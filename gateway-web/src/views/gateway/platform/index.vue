<template>
  <div class="gw-page">
    <!-- 搜索栏 -->
    <div class="gw-card" v-show="showSearch">
      <div class="gw-card-body gw-filter-bar">
        <el-input v-model="queryParams.name" clearable placeholder="插件名称" style="width: 170px" @keyup.enter="handleQuery" />
        <el-input v-model="queryParams.code" clearable placeholder="插件代码" style="width: 150px" @keyup.enter="handleQuery" />
        <el-select v-model="queryParams.status" clearable placeholder="状态（全部）" style="width: 130px">
          <el-option v-for="dict in sys_status" :label="dict.label" :value="dict.value" :key="dict.value" />
        </el-select>
        <el-button icon="Search" type="primary" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <div class="spacer" />
        <span class="gw-muted gw-small">
          每个厂商/协议抽象为一个插件；启停与配置修改动态生效，互不影响
          <template v-if="runningSummary"> · {{ runningSummary }}</template>
        </span>
      </div>
    </div>

    <!-- 插件卡片列表 -->
    <div class="gw-plugin-grid" v-loading="loading">
      <el-empty v-if="!loading && platformList.length === 0" description="暂无插件配置" style="grid-column: 1 / -1" />
      <div
        v-for="row in platformList"
        :key="row.id"
        class="gw-plugin-card"
        :class="{ 'is-error': isAbnormal(row), 'is-disabled': row.status !== '1' }"
      >
        <div class="pc-head">
          <div class="pc-ico" :style="{ background: pluginColor(row.code) }">{{ pluginInitials(row.code) }}</div>
          <div style="flex: 1; min-width: 0">
            <div class="pc-title-row">
              <b class="pc-name" :title="row.name">{{ row.name }}</b>
              <span class="gw-badge" :class="getBadgeClass(row)">{{ getRunningLabel(row) }}</span>
            </div>
            <div class="gw-muted gw-small pc-meta">
              <el-tag size="small" type="info" effect="plain">{{ statsMap[row.code]?.protocol || row.code }}</el-tag>
              <el-tag size="small" :type="getCategoryTagType(row.code)" effect="plain">{{ getCategoryLabel(row.code) }}</el-tag>
              <el-tag v-if="statsMap[row.code] && !statsMap[row.code].registered" size="small" type="warning" effect="plain">无插件实现</el-tag>
              <span v-if="statsMap[row.code]?.lastStartTime" class="pc-start-time">
                启动于 {{ formatTime(statsMap[row.code].lastStartTime) }}
              </span>
            </div>
            <div
              v-if="statsMap[row.code]?.connectionInfo && !['已断开','运行中','推送中','未配置'].includes(statsMap[row.code]?.connectionInfo)"
              class="gw-muted gw-small pc-conn"
              :title="statsMap[row.code].connectionInfo"
            >
              连接：{{ statsMap[row.code].connectionInfo }}
            </div>
          </div>
        </div>

        <div class="pc-stats">
          <div>
            <div class="v">{{ deviceStatsMap[row.code]?.total ?? 0 }}</div>
            <div class="k">接入设备</div>
          </div>
          <div>
            <div class="v">{{ formatCount(statsMap[row.code]?.msgCount) }}</div>
            <div class="k">累计消息</div>
          </div>
          <div>
            <div class="v" :style="{ color: (statsMap[row.code]?.errCount || 0) > 0 ? '#dc2626' : '#16a34a' }">
              {{ formatCount(statsMap[row.code]?.errCount) }}
            </div>
            <div class="k">错误次数</div>
          </div>
        </div>

        <div class="pc-foot">
          <el-tooltip :content="row.status === '1' ? '点击禁用将停止插件' : '点击启用将启动插件'" placement="top">
            <el-switch
              v-model="row.status"
              active-value="1"
              inactive-value="0"
              :loading="statusLoadingMap[row.code]"
              active-text="启用"
              inactive-text="禁用"
              inline-prompt
              @change="toggleStatus(row)"
            />
          </el-tooltip>
          <div class="spacer" style="flex: 1" />
          <el-button size="small" icon="Document" @click="handleViewLogs(row)">日志 / 统计</el-button>
          <el-button size="small" icon="Edit" type="primary" plain @click="handleUpdate(row)" v-hasPermi="['sys:platform:edit']">配置</el-button>
        </div>
      </div>
    </div>

    <pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNum" :total="total" @pagination="getList" v-show="total > 0" />

    <!-- 修改配置对话框 -->
    <el-dialog v-model="open" append-to-body :title="title" width="720px">
      <el-form label-width="100px" :model="form" :rules="rules" ref="platformRef">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称">
              <el-text>{{ form.name }}</el-text>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="代码">
              <el-tag>{{ form.code }}</el-tag>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="心跳接口" prop="apis">
          <el-input v-model="form.apis" placeholder="心跳/健康检查接口路径（可选）" />
        </el-form-item>

        <!-- 扩展配置：仅展示接口返回的 configObject 中已有的键；有 schema 则按类型渲染，否则文本 -->
        <template v-if="extensionConfigEntries.length > 0">
          <el-divider content-position="left"><span class="divider-label">扩展配置</span></el-divider>
          <el-alert
            v-if="currentSchema.length === 0"
            type="info"
            :closable="false"
            show-icon
            class="mb8"
            title="当前插件未提供参数元数据（configSchema），以下字段以文本方式编辑并写入配置 JSON。"
          />
          <el-form-item
            v-for="entry in extensionConfigEntries"
            :key="entry.key"
            :prop="'configObject.' + entry.key"
          >
            <template #label>
              <span v-if="entry.field" class="param-label">
                <span>{{ paramName(entry.field) }}</span>
                <el-tooltip
                  v-if="paramDesc(entry.field)"
                  :content="paramDesc(entry.field)"
                  placement="top"
                  effect="dark"
                  :show-after="200"
                >
                  <el-icon class="param-desc-icon" tabindex="-1" aria-label="参数说明"><QuestionFilled /></el-icon>
                </el-tooltip>
              </span>
              <span v-else>{{ entry.key }}</span>
            </template>

            <template v-if="entry.field">
              <el-select
                v-if="normalizedParamType(entry.field) === 'select'"
                v-model="form.configObject[entry.key]"
                :placeholder="entry.field.placeholder || '请选择' + paramName(entry.field)"
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="opt in (entry.field.options || [])"
                  :key="String(opt.value)"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>

              <el-switch
                v-else-if="normalizedParamType(entry.field) === 'boolean'"
                v-model="form.configObject[entry.key]"
              />

              <el-input-number
                v-else-if="normalizedParamType(entry.field) === 'integer'"
                v-model="form.configObject[entry.key]"
                :min="entry.field.min != null ? Number(entry.field.min) : undefined"
                :max="entry.field.max != null ? Number(entry.field.max) : undefined"
                :step="entry.field.step != null ? Number(entry.field.step) : 1"
                :precision="0"
                style="width: 100%"
              />

              <el-input-number
                v-else-if="normalizedParamType(entry.field) === 'number'"
                v-model="form.configObject[entry.key]"
                :min="entry.field.min != null ? Number(entry.field.min) : undefined"
                :max="entry.field.max != null ? Number(entry.field.max) : undefined"
                :step="entry.field.step != null ? Number(entry.field.step) : undefined"
                style="width: 100%"
              />

              <el-date-picker
                v-else-if="normalizedParamType(entry.field) === 'date'"
                v-model="form.configObject[entry.key]"
                type="date"
                :value-format="entry.field.valueFormat || entry.field.format || 'YYYY-MM-DD'"
                :placeholder="entry.field.placeholder || '选择日期'"
                style="width: 100%"
              />

              <el-date-picker
                v-else-if="normalizedParamType(entry.field) === 'datetime'"
                v-model="form.configObject[entry.key]"
                type="datetime"
                :value-format="entry.field.valueFormat || entry.field.format || 'YYYY-MM-DD HH:mm:ss'"
                :placeholder="entry.field.placeholder || '选择日期时间'"
                style="width: 100%"
              />

              <el-input
                v-else-if="normalizedParamType(entry.field) === 'textarea'"
                v-model="form.configObject[entry.key]"
                type="textarea"
                :rows="entry.field.rows != null ? Number(entry.field.rows) : 3"
                :placeholder="entry.field.placeholder || ''"
              />

              <el-input
                v-else-if="normalizedParamType(entry.field) === 'password'"
                v-model="form.configObject[entry.key]"
                type="password"
                show-password
                :placeholder="entry.field.placeholder || ''"
              />

              <el-input
                v-else
                v-model="form.configObject[entry.key]"
                :placeholder="entry.field.placeholder || ''"
              />

              <div v-if="subLabelHint(entry.field)" class="field-remark">{{ subLabelHint(entry.field) }}</div>
            </template>

            <el-input
              v-else
              v-model="form.configObject[entry.key]"
              :placeholder="'请输入 ' + entry.key"
            />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="扩展配置">
            <el-text type="info" size="small">暂无配置项</el-text>
          </el-form-item>
        </template>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">保 存</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 平台日志抽屉 -->
    <el-drawer v-model="logDrawerOpen" :title="logDrawerTitle" direction="rtl" size="600px">
      <div class="log-drawer-content">
        <el-descriptions v-if="currentStats" :column="2" border size="small" class="mb16">
          <el-descriptions-item label="协议">{{ currentStats.protocol || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ currentStats.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="运行状态">
            <el-tag :type="currentStats.alive ? 'success' : 'danger'" size="small">
              {{ currentStats.alive ? '正常运行' : '已断开' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="连接信息">{{ currentStats.connectionInfo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近启动">{{ formatTime(currentStats.lastStartTime) }}</el-descriptions-item>
          <el-descriptions-item label="最近停止">{{ formatTime(currentStats.lastStopTime) }}</el-descriptions-item>
          <el-descriptions-item label="处理消息数">
            <el-text type="success" size="large">{{ currentStats.msgCount }}</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="错误次数">
            <el-text :type="currentStats.errCount > 0 ? 'danger' : 'success'" size="large">{{ currentStats.errCount }}</el-text>
          </el-descriptions-item>
        </el-descriptions>

        <div class="log-header">
          <span class="log-title">运行日志</span>
          <div class="log-header-actions">
            <el-button size="small" icon="Refresh" @click="loadPlatformLogs(currentPlatformCode, 1)" :loading="logLoading">刷新</el-button>
            <el-button
              size="small"
              type="danger"
              plain
              icon="Delete"
              :loading="logClearLoading"
              v-hasPermi="['sys:platform:edit']"
              @click="handleClearPlatformLogs"
            >清除</el-button>
          </div>
        </div>
        <div v-loading="logLoading">
          <el-empty v-if="!logLoading && platformLogs.length === 0" description="暂无日志" :image-size="80" />
          <div v-for="log in platformLogs" :key="log.id" class="log-item" :class="getLogClass(log)">
            <div class="log-item-header">
              <el-tag :type="getLogTagType(log)" size="small">{{ getLogTypeLabel(log) }}</el-tag>
              <span class="log-time">{{ log.createTime }}</span>
            </div>
            <div class="log-item-title">
              {{ log.title }}
              <div v-if="log.content" class="log-item-actions">
                <el-button size="small" link type="primary" @click="openLogDetail(log)">查看详细</el-button>
              </div>
            </div>
<!--            <div v-if="log.content" class="log-item-preview">{{ logContentPreview(log.content) }}</div>-->

          </div>
        </div>
        <div class="msg-pagination" v-if="logTotal > 0">
          <el-pagination
            v-model:current-page="logPageNum"
            :page-size="logPageSize"
            :total="logTotal"
            layout="total, prev, pager, next"
            small
            @current-change="onLogPageChange"
          />
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="logDetailOpen" :title="logDetailTitle" width="680px" append-to-body destroy-on-close>
      <pre class="log-detail-pre">{{ logDetailContent }}</pre>
    </el-dialog>

    <!-- 平台消息记录抽屉 -->
    <el-drawer v-model="msgDrawerOpen" :title="msgDrawerTitle" direction="rtl" size="700px">
      <div class="msg-drawer-content">
        <div class="msg-header">
          <el-text size="small" type="info">共 {{ msgTotal }} 条消息记录</el-text>
          <el-button size="small" icon="Refresh" @click="loadPlatformMessages(currentPlatformCode)" :loading="msgLoading">刷新</el-button>
        </div>
        <div v-loading="msgLoading">
          <el-empty v-if="!msgLoading && msgList.length === 0" description="暂无消息记录" :image-size="80" />
          <div v-for="msg in msgList" :key="msg.id" class="msg-item" :class="'msg-type-' + (msg.type || 'default')">
            <div class="msg-item-header">
              <div class="msg-tags">
                <el-tag size="small" :type="getMsgTypeTag(msg.type)" effect="light">{{ msg.type || '未知' }}</el-tag>
                <el-tag size="small" v-if="msg.sendStatus" :type="msg.sendStatus === 'sent' ? 'success' : 'danger'" effect="light">
                  {{ msg.sendStatus === 'sent' ? '已推送' : '推送失败' }}
                </el-tag>
                <el-tag size="small" v-if="msg.results === 'Y'" type="success" effect="plain">已处置</el-tag>
              </div>
              <span class="msg-time">{{ msg.createTime }}</span>
            </div>
            <div class="msg-device-row">
              <span class="msg-device-code">{{ msg.deviceCode || '-' }}</span>
              <span v-if="msg.sendTime" class="msg-send-time">推送: {{ msg.sendTime }}</span>
              <span v-if="msg.retryCount" class="msg-retry-count">重试 {{ msg.retryCount }} 次</span>
            </div>
            <div v-if="msg.sendStatus === 'failed'" class="msg-action-row">
              <el-button size="small" type="warning" plain @click="handleRetryMsg(msg)" :loading="retryingMsgMap[msg.id]">重试推送</el-button>
            </div>
          </div>
        </div>
        <div class="msg-pagination" v-if="msgTotal > 0">
          <el-pagination
            v-model:current-page="msgPageNum"
            :page-size="10"
            :total="msgTotal"
            layout="total, prev, pager, next"
            small
            @current-change="onMsgPageChange"
          />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="Platform">
import { QuestionFilled } from '@element-plus/icons-vue'
import {
  listPlatform, getPlatform, delPlatform, addPlatform, updatePlatform,
  getPlatformStats, getAllPlatformStats, getPlatformLogs, clearPlatformLogs
} from '@/api/sys/platform'
import { listMessage } from '@/api/sys/message'
import { retryMessage } from '@/api/sys/message'
import { getDeviceOnlineStats } from '@/api/sys/device'

const { proxy } = getCurrentInstance()
const { sys_status } = proxy.useDict('sys_status')

const platformList  = ref([])
const open          = ref(false)
const loading       = ref(true)
const showSearch    = ref(true)
const ids           = ref([])
const single        = ref(true)
const multiple      = ref(true)
const total         = ref(0)
const title         = ref('')

const statusLoadingMap  = ref({})
const statsMap      = ref({})
const deviceStatsMap = ref({})   // pfCode -> {total, onlineCount}
const statsLoading  = ref(false)
const currentSchema = ref([])  // 当前平台的 configSchema（每项含 code/name/desc/type/defaultValue 等）

function paramCode(field) {
  if (!field || typeof field !== 'object') return ''
  const c = field.code != null ? field.code : field.key
  return c == null ? '' : String(c)
}
function paramName(field) {
  if (!field) return ''
  const n = field.name != null ? field.name : field.label
  if (n != null && String(n) !== '') return String(n)
  const c = paramCode(field)
  return c || '参数'
}
function paramDesc(field) {
  if (!field) return ''
  const d = field.desc != null ? field.desc : field.description
  return d == null || d === '' ? '' : String(d)
}
function subLabelHint(field) {
  if (!field || paramDesc(field)) return ''
  if (field.remark != null && String(field.remark) !== '') return String(field.remark)
  return ''
}
function normalizedParamType(field) {
  let t = (field && field.type != null ? field.type : 'string').toString().toLowerCase()
  if (t === 'switch') return 'boolean'
  if (t === 'text') return 'string'
  return t || 'string'
}
function applySchemaDefaults(schema, configObject) {
  if (!schema?.length || !configObject || typeof configObject !== 'object') return
  for (const field of schema) {
    const code = paramCode(field)
    if (!code || !Object.prototype.hasOwnProperty.call(configObject, code)) continue
    const cur = configObject[code]
    const empty = cur === undefined || cur === null || cur === ''
    if (!empty || field.defaultValue === undefined || field.defaultValue === null) continue
    const dv = field.defaultValue
    configObject[code] = typeof dv === 'object' ? JSON.parse(JSON.stringify(dv)) : dv
  }
}
function rebuildFormRules() {
  const next = {}
  const co = form.value?.configObject
  if (co && typeof co === 'object') {
    for (const field of currentSchema.value || []) {
      const code = paramCode(field)
      if (!code || !field.required || !Object.prototype.hasOwnProperty.call(co, code)) continue
      next['configObject.' + code] = [
        { required: true, message: (paramName(field) || '该项') + '不能为空', trigger: 'change' }
      ]
    }
  }
  data.rules = next
}
/** 仅根据 configObject 已有键展示；与 schema 按 code 匹配决定控件类型 */
const extensionConfigEntries = computed(() => {
  const obj = form.value?.configObject
  if (!obj || typeof obj !== 'object') return []
  const schema = currentSchema.value || []
  return Object.keys(obj)
    .sort()
    .map(key => ({
      key,
      field: schema.find(f => paramCode(f) === key) || null
    }))
})

// 日志抽屉
const logDrawerOpen       = ref(false)
const logDrawerTitle      = ref('')
const currentPlatformCode = ref('')
const currentStats        = ref(null)
const platformLogs        = ref([])
const logLoading          = ref(false)
const logTotal            = ref(0)
const logPageNum          = ref(1)
const logPageSize         = ref(10)
const logClearLoading     = ref(false)
const logDetailOpen       = ref(false)
const logDetailTitle      = ref('')
const logDetailContent    = ref('')
const msgDrawerOpen  = ref(false)
const msgDrawerTitle = ref('')
const msgList        = ref([])
const msgLoading     = ref(false)
const msgTotal       = ref(0)
const msgPageNum     = ref(1)
const retryingMsgMap = ref({})

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 20,
    orderByColumn: 'id',
    isAsc: 'DESC',
    name: null,
    code: null,
    status: null
  },
  rules: {}
})
const { queryParams, form, rules } = toRefs(data)

// ==================== 分类辅助 ====================
const CATEGORY_MAP = {
  gateway:          { label: '消息推送', type: 'warning' },
  'cascade':        { label: '级联同步', type: '' },
  'cascade-server': { label: '级联服务', type: '' }
}
function getCategoryLabel(code) {
  return CATEGORY_MAP[code]?.label ?? '数据接入'
}
function getCategoryTagType(code) {
  return CATEGORY_MAP[code]?.type ?? 'success'
}

// ==================== 样式辅助 ====================
const PLUGIN_COLORS = [
  'linear-gradient(135deg, #2563eb, #06b6d4)',
  'linear-gradient(135deg, #dc2626, #f97316)',
  'linear-gradient(135deg, #16a34a, #84cc16)',
  'linear-gradient(135deg, #7c3aed, #c084fc)',
  'linear-gradient(135deg, #d97706, #facc15)',
  'linear-gradient(135deg, #0891b2, #22d3ee)',
  'linear-gradient(135deg, #db2777, #f472b6)',
  'linear-gradient(135deg, #475569, #94a3b8)'
]
function pluginInitials(code) {
  if (!code) return '?'
  const clean = String(code).replace(/[^a-zA-Z0-9]/g, '')
  return clean.slice(0, 2).toUpperCase() || '?'
}
function pluginColor(code) {
  let hash = 0
  const s = String(code || '')
  for (let i = 0; i < s.length; i++) {
    hash = (hash * 31 + s.charCodeAt(i)) >>> 0
  }
  return PLUGIN_COLORS[hash % PLUGIN_COLORS.length]
}
function isAbnormal(row) {
  return row.status === '1' && row.running !== '1'
}
function getBadgeClass(row) {
  if (row.status !== '1') return 'off'
  return row.running === '1' ? 'ok' : 'err'
}
function getRunningLabel(row) {
  if (row.status !== '1') return '已禁用'
  return row.running === '1' ? '运行中' : '异常'
}
function formatCount(v) {
  return Number(v || 0).toLocaleString('zh-CN')
}
const runningSummary = computed(() => {
  const list = platformList.value || []
  if (list.length === 0) return ''
  const running = list.filter(r => r.status === '1' && r.running === '1').length
  const abnormal = list.filter(r => isAbnormal(r)).length
  const disabled = list.filter(r => r.status !== '1').length
  return `运行中 ${running} · 异常 ${abnormal} · 已禁用 ${disabled}`
})
function normalizeLogType(t) {
  if (t === null || t === undefined) return ''
  return String(t)
}
function getLogClass(log) {
  const t = normalizeLogType(log.type)
  if (t === '1' && log.content && String(log.content).includes('启动结果：失败')) return 'log-error'
  if (t === '1' ) return 'log-event'
  if (t === '4') return 'log-error'
  return 'log-warn'
}
function getLogTagType(log) {
  const t = normalizeLogType(log.type)
  if (t === '1' && log.content && String(log.content).includes('启动结果：失败')) return 'danger'
  if (t === '1') return 'success'
  if (t === '2') return 'info'
  if (t === '4') return 'danger'
  return 'info'
}
function getLogTypeLabel(log) {
  const map = { '1': '启动', '2': '停止', '3': '配置更新', '4': '运行异常', '5': '其它' }
  return map[normalizeLogType(log.type)] || normalizeLogType(log.type) || '日志'
}
function getMsgTypeTag(type) {
  const m = { alarm: 'danger', event: 'warning', device: 'primary', heartbeat: 'info' }
  return m[type] || ''
}
function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN', { hour12: false })
}

// ==================== 数据加载 ====================
async function getList() {
  loading.value = true
  const response = await listPlatform(queryParams.value)
  platformList.value = response.rows
  total.value        = response.total
  loading.value      = false
  loadAllStats()
}

async function loadAllStats() {
  try {
    statsLoading.value = true
    const [res, devRes] = await Promise.all([
      getAllPlatformStats(),
      getDeviceOnlineStats().catch(() => ({ data: [] }))
    ])
    const map = {}
    if (Array.isArray(res.data)) {
      res.data.forEach(s => { map[s.platformCode] = s })
    }
    statsMap.value = map
    const devMap = {}
    ;(devRes.data || []).forEach(s => { devMap[s.pfCode] = s })
    deviceStatsMap.value = devMap
  } catch (e) {
  } finally {
    statsLoading.value = false
  }
}

async function refreshStats() {
  await loadAllStats()
  proxy.$modal.msgSuccess('状态已刷新')
}

async function loadPlatformLogs(code, page) {
  logLoading.value = true
  const pageNum = page || logPageNum.value
  try {
    const res = await getPlatformLogs(code, {
      pageNum,
      pageSize: logPageSize.value,
      orderByColumn: 'create_time',
      isAsc: 'desc'
    })
    platformLogs.value = res.rows || []
    logTotal.value = res.total || 0
    logPageNum.value = pageNum
  } finally {
    logLoading.value = false
  }
}

function onLogPageChange(page) {
  logPageNum.value = page
  loadPlatformLogs(currentPlatformCode.value, page)
}

function logContentPreview(content) {
  if (content == null || content === '') return ''
  const s = String(content)
  return s.length <= 200 ? s : `${s.slice(0, 200)}…`
}

function openLogDetail(log) {
  logDetailTitle.value = log.title || '日志详情'
  logDetailContent.value = log.content != null ? String(log.content) : ''
  logDetailOpen.value = true
}

async function handleClearPlatformLogs() {
  try {
    await proxy.$modal.confirm('确定清空当前插件的全部运行日志？此操作不可恢复。')
    logClearLoading.value = true
    await clearPlatformLogs(currentPlatformCode.value)
    proxy.$modal.msgSuccess('已清空')
    await loadPlatformLogs(currentPlatformCode.value, 1)
  } catch (e) {
    if (e !== 'cancel') {
      proxy.$modal.msgError('清除失败')
    }
  } finally {
    logClearLoading.value = false
  }
}

async function loadPlatformMessages(code, page) {
  msgLoading.value = true
  const pageNum = page || msgPageNum.value
  try {
    const res = await listMessage({ pfCode: code, pageNum, pageSize: 10, orderByColumn: 'id', isAsc: 'DESC' })
    msgList.value  = res.rows  || []
    msgTotal.value = res.total || 0
  } finally {
    msgLoading.value = false
  }
}

function onMsgPageChange(page) {
  msgPageNum.value = page
  loadPlatformMessages(currentPlatformCode.value, page)
}

// ==================== 启用 / 禁用（后端负责启停插件） ====================
async function toggleStatus(row) {
  const text = row.status === '1' ? '启用' : '禁用'
  try {
    await proxy.$modal.confirm(`确认${text} [${row.name}]？启用后将启动插件，禁用后将停止插件。`)
    statusLoadingMap.value[row.code] = true
    await updatePlatform({ id: row.id, code: row.code, status: row.status })
    proxy.$modal.msgSuccess(text + '成功')
    await getList()
    await loadAllStats()
  } catch (e) {
    row.status = row.status === '1' ? '0' : '1'
  } finally {
    statusLoadingMap.value[row.code] = false
  }
}

// ==================== 日志/消息抽屉 ====================
async function handleViewLogs(row) {
  currentPlatformCode.value = row.code
  logDrawerTitle.value = `${row.name} — 运行日志`
  currentStats.value = statsMap.value[row.code] || null
  platformLogs.value = []
  logTotal.value = 0
  logPageNum.value = 1
  logDrawerOpen.value = true
  await loadPlatformLogs(row.code, 1)
  const statsRes = await getPlatformStats(row.code)
  currentStats.value = statsRes.data
}

async function handleViewMessages(row) {
  currentPlatformCode.value = row.code
  msgDrawerTitle.value = `${row.name} — 消息记录`
  msgList.value  = []
  msgTotal.value = 0
  msgPageNum.value = 1
  msgDrawerOpen.value = true
  await loadPlatformMessages(row.code, 1)
}

async function handleRetryMsg(msg) {
  retryingMsgMap.value[msg.id] = true
  try {
    await retryMessage(msg.id)
    proxy.$modal.msgSuccess('重试已提交')
    msg.sendStatus = 'sent'
  } catch (e) {
    proxy.$modal.msgError('重试失败')
  } finally {
    retryingMsgMap.value[msg.id] = false
  }
}

// ==================== CRUD ====================
function cancel() {
  open.value = false
  reset()
}
function reset() {
  form.value = {
    id: null, name: null, code: null, ip: null, port: null,
    apis: null, config: null, configObject: {}, remark: null
  }
  currentSchema.value = []
  data.rules = {}
  proxy.resetForm('platformRef')
}
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}
function resetQuery() {
  queryParams.value.name = null
  queryParams.value.code = null
  queryParams.value.status = null
  handleQuery()
}
function handleSelectionChange(selection) {
  ids.value    = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getPlatform(_id).then(response => {
    const d = response.data || {}
    form.value = {
      ...d,
      configObject: d.configObject || (d.config ? safeParseJson(d.config) : {})
    }
    if (!form.value.configObject || typeof form.value.configObject !== 'object') {
      form.value.configObject = {}
    }
    const stats = statsMap.value[d.code]
    currentSchema.value = stats?.configSchema || []
    applySchemaDefaults(currentSchema.value, form.value.configObject)
    rebuildFormRules()
    open.value = true
    title.value = '修改平台配置 — ' + d.name
  })
}
function applyHostPortColumnsFromConfigObject(formVal) {
  const o = formVal.configObject || {}
  if (Object.prototype.hasOwnProperty.call(o, 'ip')) {
    const v = o.ip
    formVal.ip = v === undefined || v === null || String(v).trim() === '' ? null : String(v).trim()
    delete o.ip
  }
  if (Object.prototype.hasOwnProperty.call(o, 'port')) {
    const p = o.port
    if (p === undefined || p === null || p === '') {
      formVal.port = null
    } else {
      const n = typeof p === 'number' ? p : parseInt(String(p), 10)
      formVal.port = Number.isFinite(n) ? n : null
    }
    delete o.port
  }
}
function safeParseJson(str) {
  try { return JSON.parse(str) } catch { return {} }
}
function submitForm() {
  proxy.$refs['platformRef'].validate(valid => {
    if (!valid) return
    applyHostPortColumnsFromConfigObject(form.value)
    form.value.config = JSON.stringify(form.value.configObject || {})
    if (form.value.id != null) {
      updatePlatform(form.value).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addPlatform(form.value).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm(proxy.deleteTips)
    .then(() => delPlatform(_ids))
    .then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
    .catch(() => {})
}
function handleExport() {
  proxy.download('sys/platform/export', { ...queryParams.value }, `platform_${Date.now()}.xlsx`)
}

getList()
</script>

<style scoped>
/* 插件卡片头部 */
.pc-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.pc-name {
  font-size: 15px;
  color: #0f172a;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.pc-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.pc-start-time { font-size: 12px; }
.pc-conn {
  margin-top: 6px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 配置编辑器 */
.config-editor {
  width: 100%;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 4px;
  min-height: 120px;
}
.divider-label { font-size: 13px; font-weight: 500; color: var(--el-text-color-secondary); }
.field-remark { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 2px; }
.mb8 { margin-bottom: 8px; }

.param-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  vertical-align: middle;
}
.param-desc-icon {
  cursor: help;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
.param-desc-icon:focus { outline: none; }

/* 日志抽屉 */
.log-drawer-content { padding: 0 4px; }
.mb16 { margin-bottom: 16px; }
.log-header, .msg-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.log-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.log-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.log-item-preview {
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-top: 6px;
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.45;
  max-height: 4.6em;
  overflow: hidden;
}
.log-item-actions {
  margin-top: 4px;
}
.log-detail-pre {
  margin: 0;
  padding: 8px 4px;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 65vh;
  overflow: auto;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
.log-item {
  padding: 10px 12px;
  margin-bottom: 8px;
  border-radius: 6px;
  border-left: 4px solid transparent;
  background: var(--el-fill-color-lighter);
}
.log-item.log-event  { border-left-color: #67c23a; }
.log-item.log-error  { border-left-color: #f56c6c; background: #fff0f0; }
.log-item.log-warn   { border-left-color: #e6a23c; }
.log-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.log-time, .msg-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.log-item-title {
  font-size: 13px;
  color: var(--el-text-color-primary);
}
.log-item-error {
  font-size: 12px;
  color: #f56c6c;
  margin-top: 4px;
  word-break: break-all;
}

/* 消息抽屉 */
.msg-drawer-content { padding: 0 4px; display: flex; flex-direction: column; height: 100%; }
.msg-item {
  padding: 10px 14px;
  margin-bottom: 8px;
  border-radius: 6px;
  border-left: 4px solid var(--el-border-color);
  background: var(--el-bg-color);
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.msg-item.msg-type-alarm   { border-left-color: #f56c6c; }
.msg-item.msg-type-event   { border-left-color: #e6a23c; }
.msg-item.msg-type-device  { border-left-color: #409eff; }
.msg-item.msg-type-heartbeat { border-left-color: #909399; }
.msg-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.msg-tags { display: flex; gap: 4px; flex-wrap: wrap; }
.msg-device-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-bottom: 2px;
}
.msg-device-code { font-family: monospace; font-weight: 600; }
.msg-send-time   { color: var(--el-text-color-secondary); }
.msg-retry-count { color: #e6a23c; }
.msg-action-row { margin-top: 6px; }
.msg-pagination {
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  justify-content: center;
  margin-top: auto;
}
</style>
