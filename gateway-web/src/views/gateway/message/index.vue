<template>
  <div class="gw-page">
    <div class="gw-page-summary">
      <span>
        今日 <b>{{ fmtNumber(today.total) }}</b> 条 · 推送成功率 <b :class="rateClass">{{ today.successRate ?? 100 }}%</b> · 待同步
        <b :class="Number(today.pendingCount) > 0 ? 'warn' : ''">{{ fmtNumber(today.pendingCount) }}</b> · 失败
        <b :class="Number(today.failedCount) > 0 ? 'err' : ''">{{ fmtNumber(today.failedCount) }}</b>
      </span>
      <span class="auto-refresh">
        <el-switch v-model="autoRefresh" size="small" />
        <span>自动刷新</span>
        <span class="gw-muted" v-if="autoRefresh">每 {{ AUTO_REFRESH_SECONDS }} 秒 · 仅第 1 页</span>
      </span>
    </div>

    <div class="gw-card">
      <div class="gw-card-body gw-filter-bar">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="消息ID / 设备编码"
          style="width: 220px"
          @clear="handleQuery"
          @keyup.enter="handleQuery"
        />
        <el-select-v2
          v-model="filters.type"
          clearable
          :options="MSG_TYPE_OPTIONS"
          placeholder="全部类型"
          style="width: 130px"
          @change="handleQuery"
        />
        <el-select-v2
          v-model="filters.pfCode"
          clearable
          filterable
          :options="platformSelectOptions"
          placeholder="全部插件"
          style="width: 150px"
          @change="handleQuery"
        />
        <el-select-v2
          v-model="filters.sendStatus"
          clearable
          :options="SEND_STATUS_OPTIONS"
          placeholder="同步状态"
          style="width: 130px"
          @change="handleQuery"
        />
        <el-date-picker
          v-model="dateRange"
          :default-time="DEFAULT_RANGE_TIME"
          end-placeholder="结束时间"
          range-separator="~"
          :shortcuts="DATE_SHORTCUTS"
          start-placeholder="开始时间"
          style="width: 360px; flex: 0 0 360px"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          @change="handleQuery"
        />
        <el-button icon="Search" type="primary" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <div class="spacer" />
        <el-button :loading="batchRetryLoading" plain v-hasPermi="['sys:message:edit']" @click="batchRetryFailed">失败消息批量重推</el-button>
        <el-button icon="Download" plain v-hasPermi="['sys:message:export']" @click="handleExport">导出</el-button>
      </div>
    </div>

    <div class="gw-card">
      <div class="gw-card-body no-pad">
        <el-table :data="messageList" v-loading="loading">
          <el-table-column label="消息ID" show-overflow-tooltip width="170">
            <template #default="{ row }">
              <span class="gw-mono">{{ row.messageId || row.id }}</span>
            </template>
          </el-table-column>
          <el-table-column class-name="gw-cell-wrap" label="类型" min-width="170">
            <template #default="{ row }">
              <div class="gw-tag-group">
                <span v-for="item in groupMsgTypes(row.type)" class="gw-tag" :class="msgTypeClass(item.type)" :key="item.type">
                  {{ typeTagText(item) }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="设备编码" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="gw-mono" style="font-weight: 600">{{ row.deviceCode || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="来源插件" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ platformName(row.pfCode) }}</template>
          </el-table-column>
          <el-table-column label="摘要" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="gw-muted">{{ row.summary || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column align="right" label="处理耗时" width="100">
            <template #default="{ row }">
              <span class="gw-mono" :class="costClass(row.costTime)">{{ costText(row.costTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column align="center" label="时间" width="165">
            <template #default="{ row }">
              <span class="gw-mono gw-muted">{{ row.createTime }}</span>
            </template>
          </el-table-column>
          <el-table-column align="center" label="同步状态" width="140">
            <template #default="{ row }">
              <span class="gw-badge plain" :class="syncClass(row)">{{ row.syncLabel || syncFallback(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column align="center" fixed="right" label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button
                link
                :loading="pushingMap[row.id]"
                type="primary"
                v-hasPermi="['sys:message:edit']"
                @click="handlePush(row)"
                v-if="canRetry(row)"
              >
                重推
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty :description="hasFilter ? '没有符合条件的消息' : '暂无接入消息'" :image-size="70" />
          </template>
        </el-table>
        <div class="gw-table-foot">
          <span class="gw-muted gw-small">共 {{ fmtNumber(total) }} 条记录</span>
          <pagination
            v-model:limit="queryParams.pageSize"
            v-model:page="queryParams.pageNum"
            :total="total"
            @pagination="getList"
            v-show="total > 0"
          />
        </div>
      </div>
    </div>

    <!-- 消息详情 -->
    <el-dialog
      v-model="detailOpen"
      append-to-body
      class="msg-detail-dialog"
      destroy-on-close
      :title="`消息详情 · ${shortId(detail.messageId || detail.id)}`"
      width="960px"
    >
      <div v-loading="detailLoading">
        <div class="detail-meta">
          <div class="meta-item wide">
            <span class="k">消息ID</span>
            <span class="gw-mono v">{{ detail.messageId || detail.id || '-' }}</span>
            <el-button icon="DocumentCopy" link type="primary" @click="copyValue(detail.messageId || detail.id, '消息ID')" />
          </div>
          <div class="meta-item">
            <span class="k">类型</span>
            <span class="gw-tag-group">
              <span v-for="item in groupMsgTypes(detail.type)" class="gw-tag" :class="msgTypeClass(item.type)" :key="item.type">
                {{ typeTagText(item) }}
              </span>
            </span>
          </div>
          <div class="meta-item">
            <span class="k">设备编码</span>
            <span class="gw-mono v">{{ detail.deviceCode || '-' }}</span>
          </div>
          <div class="meta-item">
            <span class="k">来源插件</span>
            <span class="v">{{ platformName(detail.pfCode) }}</span>
          </div>
          <div class="meta-item">
            <span class="k">接入时间</span>
            <span class="gw-mono v">{{ detail.createTime || '-' }}</span>
          </div>
          <div class="meta-item">
            <span class="k">处理耗时</span>
            <span class="gw-mono v">{{ costText(detail.costTime) }}</span>
          </div>
          <div class="meta-item">
            <span class="k">同步状态</span>
            <span class="gw-badge plain" :class="syncClass(detail)">{{ detail.syncLabel || syncFallback(detail) }}</span>
          </div>
        </div>

        <div class="detail-grid">
          <div v-for="panel in detailPanels" :key="panel.key">
            <div class="detail-label">
              <span>{{ panel.label }}</span>
              <el-button icon="DocumentCopy" link type="primary" @click="copyValue(toPrettyJson(panel.data), panel.label)">复制</el-button>
            </div>
            <div class="gw-json-view">
              <JsonPretty :data="panel.data" show-icon v-if="panel.data != null && panel.data !== ''" />
              <span class="gw-muted" v-else>无内容</span>
            </div>
          </div>
        </div>

        <template v-if="!isControlType(detail.type)">
          <div class="detail-label" style="margin-top: 16px">
            <span>上级平台同步记录</span>
            <span class="gw-muted gw-small" v-if="detail.retryCount">已重推 {{ detail.retryCount }} 次 · 最近 {{ detail.retryTime || '-' }}</span>
          </div>
          <el-table border :data="pushLogs" max-height="220" size="small" v-loading="pushLogLoading">
            <el-table-column label="目标平台" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ platformFromTarget(row.target) }}</template>
            </el-table-column>
            <el-table-column align="center" label="推送方式" width="100">
              <template #default="{ row }">
                <span class="gw-tag">{{ pushTypeLabel(row.type) }}</span>
              </template>
            </el-table-column>
            <el-table-column align="center" label="结果" width="90">
              <template #default="{ row }">
                <span class="gw-badge" :class="row.status === 'success' ? 'ok' : 'err'">{{ row.status === 'success' ? '成功' : '失败' }}</span>
              </template>
            </el-table-column>
            <el-table-column align="center" label="时间" width="165">
              <template #default="{ row }">
                <span class="gw-mono gw-small">{{ row.time }}</span>
              </template>
            </el-table-column>
            <el-table-column label="失败原因" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="gw-muted gw-small">{{ row.failReason || '—' }}</span>
              </template>
            </el-table-column>
            <template #empty>
              <span class="gw-muted gw-small">暂无推送记录（尚未配置上级平台，或该消息还没有推送过）</span>
            </template>
          </el-table>
        </template>
      </div>

      <template #footer>
        <el-button
          :loading="pushingMap[detail.id]"
          v-hasPermi="['sys:message:edit']"
          @click="handlePush(detail)"
          v-if="detail.id && !isControlType(detail.type)"
        >
          重推到上级平台
        </el-button>
        <el-button type="primary" @click="detailOpen = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="retryOpen" append-to-body destroy-on-close title="选择重推通道" width="720px">
      <p class="retry-hint">
        将 <b>{{ retryTargets.length }}</b> 条消息重推到勾选的上级通道。未勾选的通道保持原结果，不会再次推送。
      </p>
      <el-table :data="upstreamList" size="small" ref="retryUpstreamRef" @selection-change="(rows) => (retryUpstreams = rows)">
        <el-table-column type="selection" width="48" />
        <el-table-column label="通道" min-width="160" prop="name" show-overflow-tooltip />
        <el-table-column label="编码" min-width="120" prop="code" show-overflow-tooltip />
        <el-table-column align="center" label="方式" width="100">
          <template #default="{ row }">
            <span class="gw-tag">{{ pushTypeLabel(row.pushType) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="retryOpen = false">取消</el-button>
        <el-button :disabled="!retryUpstreams.length" type="primary" @click="confirmRetry">开始重推</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="progress.open"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="!progress.running"
      :show-close="!progress.running"
      title="消息重推进度"
      width="880px"
    >
      <el-progress :percentage="progressPercent" :status="progressStatus" :stroke-width="16" />
      <div class="progress-summary">
        已处理 {{ progress.done }} / {{ progress.total }} · 成功 <b class="ok">{{ progressOk }}</b> · 仍失败 <b class="err">{{ progressFail }}</b>
      </div>
      <el-table :data="progress.items" max-height="360" size="small">
        <el-table-column label="消息ID" min-width="150" prop="messageId" show-overflow-tooltip />
        <el-table-column label="设备编码" min-width="130" prop="deviceCode" show-overflow-tooltip />
        <el-table-column label="摘要" min-width="160" prop="summary" show-overflow-tooltip />
        <el-table-column align="center" label="结果" width="90">
          <template #default="{ row }">
            <span class="gw-badge" :class="row.ok ? 'ok' : 'err'">{{ row.ok ? '成功' : '仍失败' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="200" prop="reason" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button :disabled="progress.running" type="primary" @click="progress.open = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Message">
import JsonPretty from 'vue-json-pretty'
import { useRoute } from 'vue-router'
import { listMessage, getMessage, listMessagePushLogs, messageTodayStats, retryMessage } from '@/api/sys/message'
import { selectPlatform } from '@/api/sys/platform'
import { selectUpstream } from '@/api/sys/upstream'
import { MSG_TYPE_OPTIONS, msgTypeLabel, msgTypeClass, groupMsgTypes, pushTypeLabel, fmtNumber, toPrettyJson, copyText } from '@/utils/gateway'

const { proxy } = getCurrentInstance()
const route = useRoute()

const AUTO_REFRESH_SECONDS = 10
const SEND_STATUS_OPTIONS = [
  { value: 'sent', label: '全部成功' },
  { value: 'failed', label: '失败' },
  { value: 'pending', label: '待同步' }
]
// 只选日期时，开始取当天 00:00:00、结束取 23:59:59
const DEFAULT_RANGE_TIME = [new Date(2000, 0, 1, 0, 0, 0), new Date(2000, 0, 1, 23, 59, 59)]
function recentRange(ms) {
  const end = new Date()
  return [new Date(end.getTime() - ms), end]
}
const DATE_SHORTCUTS = [
  { text: '近 1 小时', value: () => recentRange(3600 * 1000) },
  {
    text: '今天',
    value: () => {
      const start = new Date()
      start.setHours(0, 0, 0, 0)
      return [start, new Date()]
    }
  },
  { text: '近 24 小时', value: () => recentRange(24 * 3600 * 1000) },
  { text: '近 7 天', value: () => recentRange(7 * 24 * 3600 * 1000) }
]

const loading = ref(false)
const messageList = ref([])
const total = ref(0)
const platformOptions = ref([])
const platformSelectOptions = computed(() => {
  const seen = new Set()
  const options = []
  for (const pf of platformOptions.value || []) {
    if (!pf?.code || seen.has(pf.code)) continue
    seen.add(pf.code)
    options.push({ value: pf.code, label: pf.name || pf.code })
  }
  return options
})
const today = ref({ total: 0, successRate: 100, pendingCount: 0 })
const rateClass = computed(() => {
  const rate = Number(today.value.successRate ?? 100)
  return rate >= 95 ? 'ok' : rate >= 80 ? 'warn' : 'err'
})
const filters = reactive({ keyword: '', type: undefined, pfCode: undefined, sendStatus: undefined })
const dateRange = ref([])
const hasFilter = computed(() => !!(filters.keyword || filters.type || filters.pfCode || filters.sendStatus || dateRange.value?.length === 2))
const queryParams = reactive({ pageNum: 1, pageSize: 10, orderByColumn: 'id', isAsc: 'DESC' })
const pushingMap = reactive({})
const batchRetryLoading = ref(false)
const upstreamList = ref([])
const retryOpen = ref(false)
const retryTargets = ref([])
const retryUpstreams = ref([])
const retryUpstreamRef = ref()
const progress = reactive({ open: false, running: false, done: 0, total: 0, items: [] })
const progressPercent = computed(() => (progress.total ? Math.round((progress.done * 100) / progress.total) : 0))
const progressOk = computed(() => progress.items.filter((item) => item.ok).length)
const progressFail = computed(() => progress.items.filter((item) => !item.ok).length)
const progressStatus = computed(() => {
  if (progress.running || !progress.total || progress.done < progress.total) return undefined
  return progressFail.value ? 'warning' : 'success'
})

const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref({})
const pushLogs = ref([])
const pushLogLoading = ref(false)

const autoRefresh = ref(false)
let autoRefreshTimer = null
let listRequestId = 0

async function copyValue(text, label) {
  if (text == null || text === '') {
    proxy.$modal.msgWarning(`${label || '内容'}为空，无法复制`)
    return
  }
  if (await copyText(String(text))) proxy.$modal.msgSuccess(`${label || '内容'}已复制`)
  else proxy.$modal.msgError('复制失败，请手动选择复制')
}
function shortId(id) {
  const s = String(id || '')
  if (s.length <= 12) return s
  return s.slice(0, 4) + '…' + s.slice(-4)
}
function isControlType(type) {
  const parts = String(type || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
  return parts.length > 0 && parts.every((item) => item === 'control')
}
function canRetry(row) {
  return !isControlType(row.type) && row.sendStatus === 'failed'
}
const detailPanels = computed(() => {
  const d = detail.value || {}
  if (isControlType(d.type)) {
    return [
      { key: 'params', label: '控制参数', data: d.content },
      { key: 'result', label: '控制结果', data: d.unifiedContent }
    ]
  }
  return [
    { key: 'unified', label: '统一消息格式（推送上级平台）', data: d.unifiedContent || d.content },
    { key: 'raw', label: '厂商原始报文（转换前）', data: d.content }
  ]
})
function typeTagText(item) {
  const label = msgTypeLabel(item.type)
  return item.count > 1 ? `${label} × ${item.count}` : label
}
function platformName(code) {
  return platformOptions.value.find((p) => p.code === code)?.name || code || '-'
}
function costText(cost) {
  if (cost == null || cost === '') return '-'
  const ms = Number(cost)
  if (!Number.isFinite(ms)) return '-'
  return ms >= 1000 ? `${(ms / 1000).toFixed(2)} s` : `${ms} ms`
}
function costClass(cost) {
  const ms = Number(cost)
  if (cost == null || !Number.isFinite(ms)) return 'gw-muted'
  return ms >= 3000 ? 'cost-slow' : ms >= 1000 ? 'cost-warn' : 'gw-muted'
}
/** 后端未返回 syncLabel 时的兜底文案 */
function syncFallback(row) {
  if (isControlType(row.type)) return '下行指令'
  if (row.sendStatus === 'sent') return '已推送'
  if (row.sendStatus === 'failed') return '推送失败'
  return '待同步'
}
function syncClass(row) {
  if (isControlType(row.type)) return row.syncSuccess === 1 ? 'ok' : 'err'
  if (String(row.syncLabel || '').includes('部分')) return 'warn'
  if (row.sendStatus === 'sent') return 'ok'
  if (row.sendStatus === 'failed') return 'err'
  return 'off'
}
function platformFromTarget(target) {
  if (!target) return '-'
  const s = String(target)
  const idx = s.indexOf('|')
  return idx > 0 ? s.slice(0, idx).trim() : s
}

async function loadToday() {
  try {
    const res = await messageTodayStats()
    today.value = res.data || {}
  } catch {
    /* 摘要加载失败不影响列表 */
  }
}

/** 列表与导出共用的筛选条件；关键字、待同步都由后端过滤，保证分页总数准确 */
function buildFilterQuery() {
  const params = {}
  const keyword = (filters.keyword || '').trim()
  if (keyword) params.keyword = keyword
  if (filters.sendStatus === 'pending') params.pending = '1'
  if (dateRange.value?.length === 2) {
    params.beginCreateTime = dateRange.value[0]
    params.endCreateTime = dateRange.value[1]
  }
  return {
    type: filters.type || null,
    pfCode: filters.pfCode || null,
    sendStatus: filters.sendStatus && filters.sendStatus !== 'pending' ? filters.sendStatus : null,
    params
  }
}

/** silent：自动刷新时不显示 loading，避免表格闪烁 */
async function getList({ silent = false } = {}) {
  const requestId = ++listRequestId
  if (!silent) loading.value = true
  try {
    const res = await listMessage({ ...queryParams, ...buildFilterQuery() })
    if (requestId !== listRequestId) return
    messageList.value = res.rows || []
    total.value = res.total || 0
  } catch {
    // 错误提示由请求拦截器统一弹出；自动刷新失败就关掉开关，避免每 10 秒弹一次错误
    if (silent) autoRefresh.value = false
  } finally {
    if (requestId === listRequestId) loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
  loadToday()
}
function resetQuery() {
  Object.assign(filters, { keyword: '', type: undefined, pfCode: undefined, sendStatus: undefined })
  dateRange.value = []
  handleQuery()
}

async function openDetail(row) {
  detailOpen.value = true
  detail.value = { ...row }
  pushLogs.value = []
  detailLoading.value = true
  pushLogLoading.value = !isControlType(row.type)
  try {
    const [res, logs] = await Promise.all([
      getMessage(row.id),
      isControlType(row.type) ? null : listMessagePushLogs(row.id).catch(() => ({ data: [] }))
    ])
    const d = res.data || {}
    d.content = parseContent(d.content)
    d.unifiedContent = parseContent(d.unifiedContent)
    detail.value = d
    pushLogs.value = logs?.data || []
  } catch {
    /* 保留列表行数据展示 */
  } finally {
    detailLoading.value = false
    pushLogLoading.value = false
  }
}
/** 报文可能不是合法 JSON（如厂商私有文本协议），解析失败时保留原文 */
function parseContent(value) {
  if (value == null || value === '' || typeof value !== 'string') return value ?? null
  try {
    return JSON.parse(value)
  } catch {
    return value
  }
}

async function loadUpstreams() {
  if (upstreamList.value.length) return
  const res = await selectUpstream({ status: '1' }).catch(() => ({ data: [] }))
  upstreamList.value = res.data || []
}

async function failedChannelNames(row) {
  if (!row?.id) return null
  let logs = []
  if (detailOpen.value && detail.value.id === row.id && pushLogs.value.length) {
    logs = pushLogs.value
  } else {
    const res = await listMessagePushLogs(row.id).catch(() => ({ data: [] }))
    logs = res.data || []
  }
  const latest = new Map()
  logs.forEach((log) => {
    const name = platformFromTarget(log.target)
    if (!latest.has(name)) latest.set(name, log)
  })
  const failed = new Set()
  latest.forEach((log, name) => {
    if (log.status !== 'success') failed.add(name)
  })
  return failed
}

async function openRetry(rows) {
  const targets = (rows || []).filter((row) => row?.id && !isControlType(row.type))
  if (!targets.length) {
    proxy.$modal.msgWarning('没有可重推的消息')
    return
  }
  await loadUpstreams()
  if (!upstreamList.value.length) {
    proxy.$modal.msgWarning('还没有启用的上级通道，请先在「系统配置 → 上级平台配置」中添加')
    return
  }
  retryTargets.value = targets
  retryOpen.value = true
  // 单条重推默认只勾选上次失败的通道；批量时默认全选
  const failedNames = targets.length === 1 ? await failedChannelNames(targets[0]) : null
  await nextTick()
  retryUpstreamRef.value?.clearSelection?.()
  upstreamList.value.forEach((row) => {
    const checked = failedNames && failedNames.size ? failedNames.has(row.name) : true
    retryUpstreamRef.value?.toggleRowSelection?.(row, checked)
  })
}

function handlePush(row) {
  openRetry([row])
}

async function confirmRetry() {
  const codes = retryUpstreams.value.map((row) => row.code).filter(Boolean)
  const targets = retryTargets.value.slice()
  if (!codes.length || !targets.length) return
  retryOpen.value = false
  progress.open = true
  progress.running = true
  progress.done = 0
  progress.total = targets.length
  progress.items = []
  for (const row of targets) {
    pushingMap[row.id] = true
    let ok = false
    let reason = ''
    try {
      const res = await retryMessage(row.id, codes)
      const data = res.data || {}
      ok = !!data.success
      reason = data.error || (ok ? '已推送到所选通道' : '重推失败')
      if (ok && data.sendStatus === 'failed') {
        reason = '所选通道已成功，未勾选的通道仍失败'
      }
    } catch (e) {
      reason = e?.message || '请求失败'
    } finally {
      delete pushingMap[row.id]
    }
    progress.items.push({
      messageId: row.messageId || row.id,
      deviceCode: row.deviceCode || '-',
      summary: row.summary || '-',
      ok,
      reason
    })
    progress.done += 1
  }
  progress.running = false
  getList()
  loadToday()
  if (detailOpen.value && targets.some((row) => row.id === detail.value.id)) {
    openDetail(detail.value)
  }
}

async function batchRetryFailed() {
  batchRetryLoading.value = true
  try {
    const res = await listMessage({ pageNum: 1, pageSize: 50, sendStatus: 'failed', orderByColumn: 'id', isAsc: 'DESC' })
    const rows = (res.rows || []).filter((row) => !isControlType(row.type))
    if (!rows.length) {
      proxy.$modal.msgSuccess('没有推送失败的消息，无需重推')
      return
    }
    await openRetry(rows)
  } finally {
    batchRetryLoading.value = false
  }
}

function handleExport() {
  proxy.download('sys/message/export', buildFilterQuery(), `message_${Date.now()}.xlsx`)
}

// ---------- 自动刷新 ----------
function stopAutoRefresh() {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
}
function startAutoRefresh() {
  stopAutoRefresh()
  if (!autoRefresh.value) return
  autoRefreshTimer = setInterval(() => {
    // 翻到后面的页、或正在看详情/重推时不打扰
    if (queryParams.pageNum !== 1 || detailOpen.value || retryOpen.value || progress.open || loading.value) return
    getList({ silent: true })
    loadToday()
  }, AUTO_REFRESH_SECONDS * 1000)
}
watch(autoRefresh, startAutoRefresh)
// 页面被 keep-alive 缓存：切走时暂停轮询，切回来再恢复
onActivated(() => {
  if (consumeRouteQuery()) {
    getList()
    loadToday()
  }
  startAutoRefresh()
})
onDeactivated(stopAutoRefresh)
onBeforeUnmount(stopAutoRefresh)

/** 首页告警等入口会带 keyword / type，只在查询条件变化时重查 */
let appliedRouteQuery = ''
function consumeRouteQuery() {
  const keyword = route.query.keyword ? String(route.query.keyword) : ''
  const type = route.query.type ? String(route.query.type) : ''
  const sendStatus = route.query.sendStatus ? String(route.query.sendStatus) : ''
  const key = `${keyword}|${type}|${sendStatus}`
  if (key === appliedRouteQuery) return false
  appliedRouteQuery = key
  if (!keyword && !type && !sendStatus) return false
  filters.keyword = keyword
  filters.type = type || undefined
  filters.sendStatus = sendStatus || undefined
  dateRange.value = []
  queryParams.pageNum = 1
  return true
}
consumeRouteQuery()

selectPlatform()
  .then((r) => {
    platformOptions.value = r.data || []
  })
  .catch(() => {})
loadToday()
getList()
</script>

<style scoped>
.auto-refresh {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #334155;
}
.retry-hint {
  margin: 0 0 12px;
  color: #64748b;
  font-size: 13px;
}
.retry-hint b {
  color: #0f172a;
}
.progress-summary {
  margin: 12px 0;
  color: #64748b;
  font-size: 13px;
}
.progress-summary b.ok {
  color: #16a34a;
}
.progress-summary b.err {
  color: #dc2626;
}
.cost-warn {
  color: #d97706;
}
.cost-slow {
  color: #dc2626;
  font-weight: 600;
}
.detail-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px 20px;
  margin-bottom: 16px;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  font-size: 13px;
}
.meta-item.wide {
  grid-column: 1 / -1;
}
.meta-item .k {
  flex-shrink: 0;
  width: 60px;
  color: #64748b;
}
.meta-item .v {
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
}
.detail-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
}
@media (max-width: 900px) {
  .detail-grid,
  .detail-meta {
    grid-template-columns: 1fr;
  }
}
</style>
