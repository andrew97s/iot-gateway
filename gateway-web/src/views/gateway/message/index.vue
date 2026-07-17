<template>
  <div class="gw-page">
    <div class="page-summary">
      今日 <b>{{ fmt(today.total) }}</b> 条 · 同步成功率 <b class="ok">{{ today.successRate ?? 100 }}%</b> · 待补推 <b class="warn">{{ fmt(today.pendingCount) }}</b>
    </div>

    <div class="gw-card">
      <div class="gw-card-body gw-filter-bar">
        <el-input v-model="filters.keyword" clearable placeholder="消息ID / 设备编码" style="width: 200px" @keyup.enter="handleQuery" />
        <el-select v-model="filters.type" clearable placeholder="全部类型" style="width: 160px">
          <el-option v-for="(meta, key) in MSG_TYPES" :key="key" :label="meta.label" :value="key" />
        </el-select>
        <el-select v-model="filters.pfCode" clearable filterable placeholder="全部插件" style="width: 160px">
          <el-option v-for="pf in platformOptions" :key="pf.code" :label="pf.name" :value="pf.code" />
        </el-select>
        <el-select v-model="filters.sendStatus" clearable placeholder="同步状态（全部）" style="width: 150px">
          <el-option label="全部成功" value="sent" />
          <el-option label="失败" value="failed" />
          <el-option label="待同步" value="pending" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="~"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 340px"
        />
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <div class="spacer" />
        <el-button plain :loading="batchRetryLoading" v-hasPermi="['sys:message:edit']" @click="batchRetryFailed">失败消息批量重推</el-button>
        <el-button plain icon="Download" v-hasPermi="['sys:message:export']" @click="handleExport">导出</el-button>
      </div>
    </div>

    <div class="gw-card">
      <div class="gw-card-body no-pad">
        <el-table :data="messageList" v-loading="loading">
          <el-table-column label="时间" width="160" align="center">
            <template #default="{ row }"><span class="gw-mono gw-muted gw-small">{{ row.createTime }}</span></template>
          </el-table-column>
          <el-table-column label="消息ID" width="120" show-overflow-tooltip>
            <template #default="{ row }"><span class="gw-mono gw-small">{{ shortId(row.messageId || row.id) }}</span></template>
          </el-table-column>
          <el-table-column label="类型" width="100" align="center">
            <template #default="{ row }">
              <span class="gw-tag" :class="typeTagClass(row.type)">{{ getMsgTypeLabel(row.type) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="设备编码" min-width="140" show-overflow-tooltip>
            <template #default="{ row }"><span class="gw-mono" style="font-weight:600">{{ row.deviceCode || '-' }}</span></template>
          </el-table-column>
          <el-table-column label="来源插件" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ platformName(row.pfCode) }}</template>
          </el-table-column>
          <el-table-column label="摘要" min-width="200" show-overflow-tooltip>
            <template #default="{ row }"><span class="gw-muted">{{ row.summary || '-' }}</span></template>
          </el-table-column>
          <el-table-column label="上级平台同步" width="150" align="center">
            <template #default="{ row }">
              <span class="gw-badge plain" :class="syncClass(row)">{{ row.syncLabel || syncFallback(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button
                v-if="!isControlType(row.type) && row.sendStatus === 'failed'"
                link
                type="primary"
                :loading="pushingMap[row.id]"
                v-hasPermi="['sys:message:edit']"
                @click="handlePush(row)"
              >重推</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-foot">
          <span class="gw-muted gw-small">共 {{ total }} 条记录</span>
          <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
        </div>
      </div>
    </div>

    <!-- 消息详情 -->
    <el-dialog v-model="detailOpen" :title="`消息详情 · ${shortId(detail.messageId || detail.id)}`" width="960px" append-to-body destroy-on-close class="msg-detail-dialog">
      <div class="detail-grid">
        <div>
          <div class="detail-label">
            <span>统一消息格式（推送上级平台）</span>
            <el-button link type="primary" icon="DocumentCopy" @click="copyJson(detail.unifiedContent || detail.content, '统一消息')">复制</el-button>
          </div>
          <div class="json-view"><JsonPretty :data="detail.unifiedContent || detail.content" show-icon /></div>
        </div>
        <div>
          <div class="detail-label">
            <span>厂商原始报文（转换前）</span>
            <el-button link type="primary" icon="DocumentCopy" @click="copyJson(detail.content, '原始报文')">复制</el-button>
          </div>
          <div class="json-view"><JsonPretty :data="detail.content" show-icon /></div>
        </div>
      </div>

      <div class="detail-label" style="margin-top: 16px">上级平台同步记录</div>
      <div class="sync-table-wrap">
        <el-table :data="pushLogs" size="small" v-loading="pushLogLoading" border max-height="220">
          <el-table-column label="目标平台" min-width="140">
            <template #default="{ row }">{{ platformFromTarget(row.target) }}</template>
          </el-table-column>
          <el-table-column label="推送方式" width="100" align="center">
            <template #default="{ row }"><span class="gw-tag">{{ pushModeLabel(row.type) }}</span></template>
          </el-table-column>
          <el-table-column label="结果" width="90" align="center">
            <template #default="{ row }">
              <span class="gw-badge" :class="row.status === 'success' ? 'ok' : 'err'">{{ row.status === 'success' ? '成功' : '失败' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="重试次数" width="90" align="center">
            <template #default>-</template>
          </el-table-column>
          <el-table-column label="耗时" width="90" align="center">
            <template #default>-</template>
          </el-table-column>
          <el-table-column label="时间" width="160" align="center">
            <template #default="{ row }"><span class="gw-mono gw-small">{{ row.time }}</span></template>
          </el-table-column>
          <el-table-column label="失败原因" min-width="160" show-overflow-tooltip>
            <template #default="{ row }"><span class="gw-muted gw-small">{{ row.failReason || '—' }}</span></template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button
          v-if="!isControlType(detail.type)"
          :loading="pushingMap[detail.id]"
          v-hasPermi="['sys:message:edit']"
          @click="handlePush(detail)"
        >重推所选平台</el-button>
        <el-button type="primary" @click="detailOpen = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Message">
import JsonPretty from 'vue-json-pretty'
import {
  listMessage, getMessage, pushMessage, listMessagePushLogs,
  messageTodayStats, retryMessageBatch
} from '@/api/sys/message'
import { selectPlatform } from '@/api/sys/platform'

const { proxy } = getCurrentInstance()

const MSG_TYPES = {
  alarm: { label: '告警 ALARM', short: '告警', cls: 'red' },
  business: { label: '监测 TELEMETRY', short: '监测', cls: 'blue' },
  monitor: { label: '监测 TELEMETRY', short: '监测', cls: 'blue' },
  device: { label: '设备事件 DEVICE_EVENT', short: '设备事件', cls: 'orange' },
  control: { label: '反控指令 COMMAND', short: '反控指令', cls: 'purple' },
  event: { label: '事件', short: '事件', cls: 'orange' },
  heartbeat: { label: '心跳', short: '心跳', cls: '' }
}

const loading = ref(false)
const messageList = ref([])
const total = ref(0)
const platformOptions = ref([])
const today = ref({ total: 0, successRate: 100, pendingCount: 0 })
const filters = reactive({ keyword: '', type: '', pfCode: '', sendStatus: '' })
const dateRange = ref([])
const queryParams = reactive({ pageNum: 1, pageSize: 10, orderByColumn: 'id', isAsc: 'DESC' })
const pushingMap = reactive({})
const batchRetryLoading = ref(false)

const detailOpen = ref(false)
const detail = ref({})
const pushLogs = ref([])
const pushLogLoading = ref(false)

function fmt(v) {
  return Number(v || 0).toLocaleString('zh-CN')
}
function toCopyText(data) {
  if (data == null || data === '') return ''
  if (typeof data === 'string') {
    try {
      return JSON.stringify(JSON.parse(data), null, 2)
    } catch {
      return data
    }
  }
  try {
    return JSON.stringify(data, null, 2)
  } catch {
    return String(data)
  }
}
async function copyJson(data, label) {
  const text = toCopyText(data)
  if (!text) {
    proxy.$modal.msgWarning(`${label || '内容'}为空，无法复制`)
    return
  }
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.left = '-9999px'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
    }
    proxy.$modal.msgSuccess(`${label || '内容'}已复制`)
  } catch {
    proxy.$modal.msgError('复制失败，请手动选择复制')
  }
}
function shortId(id) {
  const s = String(id || '')
  if (s.length <= 12) return s
  return s.slice(0, 4) + '…' + s.slice(-4)
}
function isControlType(type) {
  return type === 'control'
}
function getMsgTypeLabel(type) {
  return MSG_TYPES[type]?.short || type || '-'
}
function typeTagClass(type) {
  return MSG_TYPES[type]?.cls || ''
}
function platformName(code) {
  return platformOptions.value.find((p) => p.code === code)?.name || code || '-'
}
function syncFallback(row) {
  if (isControlType(row.type)) return '— 下行消息'
  if (row.sendStatus === 'sent') return '已推送'
  if (row.sendStatus === 'failed') return '失败'
  return '待同步'
}
function syncClass(row) {
  if (isControlType(row.type)) return 'off'
  if (row.sendStatus === 'sent') return 'ok'
  if (row.sendStatus === 'failed') return 'err'
  if (row.syncLabel && String(row.syncLabel).includes('部分')) return 'warn'
  return 'off'
}
function pushModeLabel(mode) {
  return { mq: 'MQ', url: 'HTTP', redis: 'Redis', kafka: 'Kafka', mqtt: 'MQTT', unknown: '未知' }[mode] || mode || '-'
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
  } catch { /* */ }
}

function getList() {
  loading.value = true
  const params = {
    ...queryParams,
    type: filters.type || null,
    pfCode: filters.pfCode || null,
    deviceCode: filters.keyword || null,
    sendStatus: filters.sendStatus === 'pending' ? null : (filters.sendStatus || null),
    params: {}
  }
  if (dateRange.value?.length === 2) {
    params.params.beginCreateTime = dateRange.value[0]
    params.params.endCreateTime = dateRange.value[1]
  }
  listMessage(params).then((res) => {
    let rows = res.rows || []
    if (filters.sendStatus === 'pending') {
      rows = rows.filter((r) => !r.sendStatus && !isControlType(r.type))
    }
    if (filters.keyword) {
      const kw = filters.keyword.toLowerCase()
      rows = rows.filter((r) =>
        String(r.deviceCode || '').toLowerCase().includes(kw) ||
        String(r.messageId || '').toLowerCase().includes(kw) ||
        String(r.id || '').includes(kw)
      )
    }
    messageList.value = rows
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
  loadToday()
}
function resetQuery() {
  Object.assign(filters, { keyword: '', type: '', pfCode: '', sendStatus: '' })
  dateRange.value = []
  handleQuery()
}

async function openDetail(row) {
  detailOpen.value = true
  detail.value = { ...row }
  pushLogs.value = []
  pushLogLoading.value = true
  try {
    const res = await getMessage(row.id)
    const d = res.data || {}
    try { d.content = d.content ? (typeof d.content === 'string' ? JSON.parse(d.content) : d.content) : d.content } catch { /* */ }
    try { d.unifiedContent = d.unifiedContent ? (typeof d.unifiedContent === 'string' ? JSON.parse(d.unifiedContent) : d.unifiedContent) : null } catch { /* */ }
    detail.value = d
    const logs = await listMessagePushLogs(row.id)
    pushLogs.value = logs.data || []
  } finally {
    pushLogLoading.value = false
  }
}

async function handlePush(row) {
  if (!row?.id) return
  pushingMap[row.id] = true
  try {
    await pushMessage(row.id)
    proxy.$modal.msgSuccess('推送成功')
    getList()
    loadToday()
    if (detailOpen.value && detail.value.id === row.id) {
      openDetail(row)
    }
  } catch {
    proxy.$modal.msgError('推送失败')
    getList()
  } finally {
    delete pushingMap[row.id]
  }
}

async function batchRetryFailed() {
  try {
    await proxy.$modal.confirm('确认批量重推最近失败的消息？（最多 50 条）')
    batchRetryLoading.value = true
    const res = await retryMessageBatch({})
    const d = res.data || {}
    proxy.$modal.msgSuccess(`重推完成：成功 ${d.success || 0}，失败 ${d.failed || 0}`)
    getList()
    loadToday()
  } catch { /* */ } finally {
    batchRetryLoading.value = false
  }
}

function handleExport() {
  proxy.download('sys/message/export', {
    ...queryParams,
    type: filters.type,
    pfCode: filters.pfCode,
    deviceCode: filters.keyword
  }, `message_${Date.now()}.xlsx`)
}

selectPlatform().then((r) => { platformOptions.value = r.data || [] })
loadToday()
getList()
</script>

<style scoped>
.page-summary { color: #64748b; font-size: 13px; }
.page-summary b { color: #0f172a; font-weight: 700; }
.page-summary b.ok { color: #16a34a; }
.page-summary b.warn { color: #d97706; }
.gw-tag {
  display: inline-block; padding: 1px 8px; border-radius: 5px; font-size: 12px;
  background: #f1f5f9; color: #475569; border: 1px solid #e2e8f0;
}
.gw-tag.red { background: #fef2f2; color: #dc2626; border-color: #fecaca; }
.gw-tag.blue { background: #eff6ff; color: #2563eb; border-color: #bfdbfe; }
.gw-tag.orange { background: #fffbeb; color: #d97706; border-color: #fde68a; }
.gw-tag.purple { background: #f5f3ff; color: #7c3aed; border-color: #ddd6fe; }
.table-foot {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-top: 1px solid #e2e8f0; gap: 12px; flex-wrap: wrap;
}
.table-foot :deep(.pagination-container) {
  margin: 0 !important; padding: 0 !important; border: none !important; height: auto !important;
  box-shadow: none !important;
}
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.detail-label {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  font-size: 13px; color: #64748b; margin-bottom: 8px;
}
.json-view {
  background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px;
  padding: 12px; max-height: 280px; overflow: auto;
}
.sync-table-wrap {
  max-height: 240px;
  overflow: hidden;
  border-radius: 8px;
}
@media (max-width: 900px) { .detail-grid { grid-template-columns: 1fr; } }
</style>
