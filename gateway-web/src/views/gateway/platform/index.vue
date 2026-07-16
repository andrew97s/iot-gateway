<template>
  <div class="gw-page plugin-page">
    <!-- 顶部摘要（对齐原型 topbar meta） -->
    <div class="plugin-summary">
      <div class="plugin-summary-text">
        运行中 <b class="ok">{{ summary.running }}</b>
        · 已停止 <b>{{ summary.stopped }}</b>
        · 异常 <b class="err">{{ summary.abnormal }}</b>
      </div>
      <el-button type="primary" size="small" icon="Plus" @click="openInstall" v-hasPermi="['sys:platform:add']">
        安装插件包
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="gw-card">
      <div class="gw-card-body gw-filter-bar">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="插件名称 / 厂商"
          style="width: 200px"
          @keyup.enter="applyFilter"
        />
        <el-select v-model="filters.state" clearable placeholder="全部状态" style="width: 130px">
          <el-option label="运行中" value="running" />
          <el-option label="已停止" value="stopped" />
          <el-option label="异常" value="abnormal" />
        </el-select>
        <el-select v-model="filters.protocol" clearable placeholder="全部协议" style="width: 150px">
          <el-option v-for="p in protocolOptions" :key="p" :label="p" :value="p" />
        </el-select>
        <el-button type="primary" icon="Search" @click="applyFilter">查询</el-button>
        <el-button icon="Refresh" @click="refreshAll" :loading="loading">刷新</el-button>
        <div class="spacer" />
        <span class="gw-muted gw-small">
          插件为厂商接入的最小单元，同一插件可创建多个实例；启停/改配置均动态生效，不影响其他插件
        </span>
      </div>
    </div>

    <!-- 插件卡片网格 -->
    <div class="gw-plugin-grid" v-loading="loading">
      <el-empty v-if="!loading && filteredList.length === 0" description="暂无匹配的插件" style="grid-column: 1 / -1" />

      <div
        v-for="row in filteredList"
        :key="row.id"
        class="gw-plugin-card"
        :class="{
          'is-error': pluginState(row) === 'abnormal',
          'is-disabled': pluginState(row) === 'stopped'
        }"
      >
        <div class="pc-head">
          <div class="pc-ico" :style="{ background: pluginColor(row.code) }">{{ pluginInitials(row.code) }}</div>
          <div class="pc-main">
            <div class="pc-title-row">
              <b class="pc-name" :title="row.name">{{ row.name }}</b>
              <span class="gw-badge" :class="badgeClass(row)">{{ stateLabel(row) }}</span>
            </div>
            <div class="pc-meta gw-muted gw-small">
              <span class="gw-tag">{{ protocolOf(row) }}</span>
              <span class="gw-tag">{{ versionOf(row) }}</span>
              <span>· 实例 {{ row.code }}</span>
              <span v-if="capsOf(row)">· 能力：{{ capsOf(row) }}</span>
              <span v-else-if="pluginState(row) === 'abnormal' && restartHint(row)">· {{ restartHint(row) }}</span>
            </div>
          </div>
        </div>

        <div class="pc-stats">
          <div>
            <div class="v">{{ fmt(deviceCount(row.code)) }}</div>
            <div class="k">接入设备</div>
          </div>
          <div>
            <div class="v">{{ fmt(todayMsg(row.code)) }}</div>
            <div class="k">今日消息</div>
          </div>
          <div>
            <div
              class="v"
              :style="{ color: failCount(row) > 0 ? '#dc2626' : (pluginState(row) === 'stopped' ? '#64748b' : '#16a34a') }"
            >
              {{ pluginState(row) === 'stopped' && todayMsg(row.code) === 0 ? '—' : fmt(failCount(row)) }}
            </div>
            <div class="k">转换失败</div>
          </div>
        </div>

        <div class="pc-foot">
          <el-button size="small" @click="openDrawer(row)">日志 / 统计</el-button>
          <el-button size="small" @click="openConfig(row)" v-hasPermi="['sys:platform:edit']">配置</el-button>

          <template v-if="pluginState(row) === 'running'">
            <el-button size="small" type="danger" plain :loading="actionLoading[row.code]" @click="handleStop(row)">停止</el-button>
          </template>
          <template v-else-if="pluginState(row) === 'stopped'">
            <el-button size="small" type="primary" :loading="actionLoading[row.code]" @click="handleStart(row)">启动</el-button>
            <el-button size="small" type="danger" plain v-hasPermi="['sys:platform:remove']" @click="handleUninstall(row)">卸载</el-button>
          </template>
          <template v-else>
            <el-button size="small" type="primary" :loading="actionLoading[row.code]" @click="handleRestart(row)">重启</el-button>
            <el-button size="small" type="danger" plain :loading="actionLoading[row.code]" @click="handleStop(row)">停止</el-button>
          </template>
        </div>
      </div>
    </div>

    <!-- 配置弹窗（动态 schema / 通用字段） -->
    <el-dialog v-model="cfgOpen" :title="cfgTitle" width="720px" append-to-body destroy-on-close>
      <p class="cfg-hint">
        以下表单由插件 <b>config-schema</b> 动态渲染（无 schema 时展示通用接入参数）；保存后配置热生效（插件自动重载）。
      </p>
      <el-form ref="cfgFormRef" :model="cfgForm" :rules="cfgRules" label-position="top" class="cfg-form">
        <div class="cfg-grid">
          <el-form-item label="平台/设备地址" prop="ip">
            <el-input v-model="cfgForm.ip" placeholder="如 192.168.1.64" />
          </el-form-item>
          <el-form-item label="端口" prop="port">
            <el-input-number v-model="cfgForm.port" :min="1" :max="65535" controls-position="right" style="width: 100%" />
          </el-form-item>

          <template v-if="schemaFields.length">
            <el-form-item
              v-for="field in schemaFields"
              :key="field.code"
              :label="field.name"
              :prop="'configObject.' + field.code"
              :required="!!field.required"
            >
              <el-select
                v-if="field.type === 'select'"
                v-model="cfgForm.configObject[field.code]"
                clearable
                style="width: 100%"
              >
                <el-option v-for="opt in field.options || []" :key="String(opt.value)" :label="opt.label" :value="opt.value" />
              </el-select>
              <el-switch v-else-if="field.type === 'boolean'" v-model="cfgForm.configObject[field.code]" />
              <el-input-number
                v-else-if="field.type === 'integer' || field.type === 'number'"
                v-model="cfgForm.configObject[field.code]"
                controls-position="right"
                style="width: 100%"
              />
              <el-input
                v-else-if="field.type === 'password'"
                v-model="cfgForm.configObject[field.code]"
                type="password"
                show-password
              />
              <el-input
                v-else-if="field.type === 'textarea'"
                v-model="cfgForm.configObject[field.code]"
                type="textarea"
                :rows="3"
              />
              <el-input v-else v-model="cfgForm.configObject[field.code]" />
              <div v-if="field.desc" class="field-hint">{{ field.desc }}</div>
            </el-form-item>
          </template>

          <template v-else>
            <el-form-item label="用户名">
              <el-input v-model="cfgForm.configObject.username" placeholder="admin" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="cfgForm.configObject.password" type="password" show-password placeholder="********" />
            </el-form-item>
            <el-form-item label="心跳间隔（秒）">
              <el-input-number v-model="cfgForm.configObject.heartbeatInterval" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="断线重连间隔（秒）">
              <el-input-number v-model="cfgForm.configObject.reconnectInterval" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="日志级别">
              <el-select v-model="cfgForm.configObject.logLevel" style="width: 100%">
                <el-option label="DEBUG" value="DEBUG" />
                <el-option label="INFO" value="INFO" />
                <el-option label="WARN" value="WARN" />
                <el-option label="ERROR" value="ERROR" />
              </el-select>
            </el-form-item>
            <el-form-item label="异常自动重启" class="full">
              <div class="auto-restart">
                <el-switch v-model="cfgForm.configObject.autoRestart" />
                <span class="gw-muted gw-small">
                  最多重启
                  <el-input-number v-model="cfgForm.configObject.maxRestart" :min="0" :max="99" size="small" controls-position="right" style="width: 90px; margin: 0 6px" />
                  次，退避间隔
                  <el-input-number v-model="cfgForm.configObject.restartBackoff" :min="1" size="small" controls-position="right" style="width: 90px; margin: 0 6px" />
                  秒起指数递增
                </span>
              </div>
            </el-form-item>
          </template>

          <el-form-item label="心跳接口" class="full">
            <el-input v-model="cfgForm.apis" placeholder="可选，健康检查路径" />
          </el-form-item>
          <el-form-item label="备注" class="full">
            <el-input v-model="cfgForm.remark" type="textarea" :rows="2" placeholder="备注说明（可选）" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="testConnection" :loading="testing">测试连接</el-button>
        <el-button @click="cfgOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveConfig">保存并生效</el-button>
      </template>
    </el-dialog>

    <!-- 安装插件 -->
    <el-dialog v-model="installOpen" title="安装插件包" width="520px" append-to-body destroy-on-close>
      <el-form ref="installRef" :model="installForm" :rules="installRules" label-width="90px">
        <el-form-item label="插件名称" prop="name">
          <el-input v-model="installForm.name" placeholder="如：海康威视接入插件" />
        </el-form-item>
        <el-form-item label="实例代码" prop="code">
          <el-input v-model="installForm.code" placeholder="如：hik-01" />
        </el-form-item>
        <el-form-item label="协议">
          <el-input v-model="installForm.protocol" placeholder="如：ISAPI/SDK" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="installForm.ip" placeholder="可选" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="installForm.port" :min="1" :max="65535" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="installOpen = false">取消</el-button>
        <el-button type="primary" :loading="installing" @click="submitInstall">安装</el-button>
      </template>
    </el-dialog>

    <!-- 日志 / 统计抽屉 -->
    <el-drawer v-model="drawerOpen" :title="drawerTitle" direction="rtl" size="700px" destroy-on-close>
      <el-tabs v-model="drawerTab">
        <el-tab-pane label="消息统计" name="stats">
          <div v-loading="drawerStatsLoading" class="drawer-stats">
            <div class="stat-row">
              <div class="mini-stat">
                <div class="label">累计接入消息</div>
                <div class="value">{{ fmt(drawerStats.total) }}</div>
              </div>
              <div class="mini-stat">
                <div class="label">今日接入消息</div>
                <div class="value">{{ fmt(drawerStats.todayTotal) }}</div>
              </div>
              <div class="mini-stat">
                <div class="label">今日转换失败</div>
                <div class="value" :style="{ color: drawerStats.todayFailed > 0 ? '#dc2626' : '#16a34a' }">
                  {{ fmt(drawerStats.todayFailed) }}
                </div>
              </div>
            </div>

            <div class="gw-card mt16">
              <div class="gw-card-head"><h2>今日消息类型分布</h2></div>
              <div class="gw-card-body">
                <el-empty v-if="typeDist.length === 0" description="今日暂无消息" :image-size="60" />
                <div v-else class="gw-hbar">
                  <div v-for="item in typeDist" :key="item.type" class="row">
                    <span>{{ typeLabel(item.type) }}</span>
                    <div class="bar-bg">
                      <div class="bar" :style="{ width: item.percent + '%', background: typeColor(item.type) }" />
                    </div>
                    <span class="num">{{ fmt(item.total) }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="gw-card mt16">
              <div class="gw-card-head"><h2>近 24 小时消息量</h2></div>
              <div class="gw-card-body">
                <div ref="trendChartRef" class="trend-chart" />
                <el-empty v-if="!drawerStatsLoading && hourlyTrend.length === 0" description="暂无趋势数据" :image-size="50" />
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="运行日志" name="logs">
          <div class="gw-filter-bar" style="margin-bottom: 12px">
            <el-select v-model="logLevel" clearable placeholder="全部级别" style="width: 120px" @change="reloadLogs">
              <el-option label="启动" value="1" />
              <el-option label="停止" value="2" />
              <el-option label="配置" value="3" />
              <el-option label="异常" value="4" />
            </el-select>
            <el-input v-model="logKeyword" clearable placeholder="关键字过滤" style="flex: 1" @keyup.enter="reloadLogs" />
            <el-button size="small" icon="Refresh" :loading="logLoading" @click="reloadLogs">刷新</el-button>
            <el-button size="small" type="danger" plain icon="Delete" v-hasPermi="['sys:platform:edit']" @click="clearLogs">清空</el-button>
          </div>
          <div v-loading="logLoading" class="gw-log-console">
            <template v-if="displayLogs.length">
              <div v-for="log in displayLogs" :key="log.id" class="log-line">
                <span class="t">{{ log.createTime || '' }}</span>
                <span :class="logLevelClass(log)">[{{ logTypeLabel(log) }}]</span>
                <span> [{{ currentCode }}] {{ log.title }}</span>
                <el-button v-if="log.content" link type="primary" size="small" @click="showLogDetail(log)">详情</el-button>
              </div>
            </template>
            <div v-else class="gw-muted">暂无日志</div>
          </div>
          <div class="log-pager" v-if="logTotal > 0">
            <el-pagination
              v-model:current-page="logPage"
              :page-size="logPageSize"
              :total="logTotal"
              layout="total, prev, pager, next"
              small
              @current-change="loadLogs"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <el-dialog v-model="logDetailOpen" :title="logDetailTitle" width="680px" append-to-body destroy-on-close>
      <pre class="log-detail-pre">{{ logDetailContent }}</pre>
    </el-dialog>
  </div>
</template>

<script setup name="Platform">
import * as echarts from 'echarts'
import {
  listPlatform,
  getPlatform,
  addPlatform,
  updatePlatform,
  delPlatform,
  getAllPlatformStats,
  getTodayMsgStats,
  getPluginMsgStats,
  getPlatformLogs,
  clearPlatformLogs
} from '@/api/sys/platform'
import { getDeviceOnlineStats } from '@/api/sys/device'

const { proxy } = getCurrentInstance()

const TYPE_META = {
  alarm: { label: '告警事件', color: '#dc2626' },
  business: { label: '监测数据', color: '#2563eb' },
  monitor: { label: '监测数据', color: '#2563eb' },
  device: { label: '设备事件', color: '#d97706' },
  control: { label: '指令回执', color: '#0891b2' },
  event: { label: '事件', color: '#0891b2' },
  heartbeat: { label: '心跳', color: '#64748b' }
}

const PLUGIN_COLORS = [
  'linear-gradient(135deg,#dc2626,#f97316)',
  'linear-gradient(135deg,#2563eb,#06b6d4)',
  'linear-gradient(135deg,#16a34a,#84cc16)',
  'linear-gradient(135deg,#7c3aed,#c084fc)',
  'linear-gradient(135deg,#64748b,#94a3b8)',
  'linear-gradient(135deg,#d97706,#facc15)',
  'linear-gradient(135deg,#0891b2,#22d3ee)',
  'linear-gradient(135deg,#db2777,#f472b6)'
]

const loading = ref(false)
const platformList = ref([])
const statsMap = ref({})
const deviceMap = ref({})
const todayMap = ref({})
const actionLoading = ref({})

const filters = reactive({ keyword: '', state: '', protocol: '' })
const applied = reactive({ keyword: '', state: '', protocol: '' })

const cfgOpen = ref(false)
const cfgTitle = ref('')
const cfgForm = ref(emptyCfg())
const cfgRules = {
  ip: [{ required: false, message: '请输入地址', trigger: 'blur' }]
}
const schemaFields = ref([])
const saving = ref(false)
const testing = ref(false)
const cfgFormRef = ref()

const installOpen = ref(false)
const installing = ref(false)
const installForm = ref({ name: '', code: '', protocol: '', ip: '', port: undefined })
const installRules = {
  name: [{ required: true, message: '请输入插件名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入实例代码', trigger: 'blur' }]
}
const installRef = ref()

const drawerOpen = ref(false)
const drawerTitle = ref('')
const drawerTab = ref('stats')
const drawerStatsLoading = ref(false)
const drawerStats = ref({ total: 0, todayTotal: 0, todayFailed: 0 })
const typeDist = ref([])
const hourlyTrend = ref([])
const trendChartRef = ref(null)
let trendChart = null

const currentCode = ref('')
const logLoading = ref(false)
const platformLogs = ref([])
const logTotal = ref(0)
const logPage = ref(1)
const logPageSize = 20
const logLevel = ref('')
const logKeyword = ref('')
const logDetailOpen = ref(false)
const logDetailTitle = ref('')
const logDetailContent = ref('')

function emptyCfg() {
  return {
    id: null,
    name: '',
    code: '',
    ip: '',
    port: undefined,
    apis: '',
    remark: '',
    status: '1',
    configObject: {
      username: '',
      password: '',
      heartbeatInterval: 30,
      reconnectInterval: 10,
      logLevel: 'INFO',
      autoRestart: true,
      maxRestart: 5,
      restartBackoff: 10
    }
  }
}

function fmt(v) {
  return Number(v || 0).toLocaleString('zh-CN')
}
function pluginInitials(code) {
  const clean = String(code || '').replace(/[^a-zA-Z0-9]/g, '')
  return (clean.slice(0, 2) || '?').toUpperCase()
}
function pluginColor(code) {
  let hash = 0
  const s = String(code || '')
  for (let i = 0; i < s.length; i++) hash = (hash * 31 + s.charCodeAt(i)) >>> 0
  return PLUGIN_COLORS[hash % PLUGIN_COLORS.length]
}
function pluginState(row) {
  if (!row || row.status !== '1') return 'stopped'
  return row.running === '1' ? 'running' : 'abnormal'
}
function stateLabel(row) {
  const s = pluginState(row)
  return s === 'running' ? '运行中' : s === 'abnormal' ? '异常' : '已停止'
}
function badgeClass(row) {
  const s = pluginState(row)
  return s === 'running' ? 'ok' : s === 'abnormal' ? 'err' : 'off'
}
function protocolOf(row) {
  return statsMap.value[row.code]?.protocol || row.configObject?.protocol || row.code || '-'
}
function versionOf(row) {
  const v = statsMap.value[row.code]?.version || row.configObject?.version
  return v ? `v${String(v).replace(/^v/i, '')}` : 'v—'
}
function capsOf(row) {
  const caps = statsMap.value[row.code]?.capabilities || row.configObject?.capabilities
  if (Array.isArray(caps)) return caps.join(' / ')
  if (typeof caps === 'string' && caps) return caps
  return ''
}
function restartHint(row) {
  const n = statsMap.value[row.code]?.restartCount
  return n ? `已自动重启 ${n} 次` : ''
}
function deviceCount(code) {
  return deviceMap.value[code]?.total || 0
}
function todayMsg(code) {
  return todayMap.value[code]?.total || 0
}
function failCount(row) {
  const today = todayMap.value[row.code]
  if (today && today.failedCount != null) return Number(today.failedCount || 0)
  return Number(statsMap.value[row.code]?.errCount || 0)
}
function typeLabel(t) {
  return TYPE_META[t]?.label || t || '未知'
}
function typeColor(t) {
  return TYPE_META[t]?.color || '#94a3b8'
}

const summary = computed(() => {
  const list = platformList.value || []
  return {
    running: list.filter((r) => pluginState(r) === 'running').length,
    stopped: list.filter((r) => pluginState(r) === 'stopped').length,
    abnormal: list.filter((r) => pluginState(r) === 'abnormal').length
  }
})

const protocolOptions = computed(() => {
  const set = new Set()
  ;(platformList.value || []).forEach((r) => {
    const p = protocolOf(r)
    if (p && p !== '-') set.add(p)
  })
  return Array.from(set)
})

const filteredList = computed(() => {
  return (platformList.value || []).filter((row) => {
    if (applied.state && pluginState(row) !== applied.state) return false
    if (applied.protocol && protocolOf(row) !== applied.protocol) return false
    if (applied.keyword) {
      const kw = applied.keyword.toLowerCase()
      const blob = `${row.name || ''} ${row.code || ''} ${protocolOf(row)}`.toLowerCase()
      if (!blob.includes(kw)) return false
    }
    return true
  })
})

const displayLogs = computed(() => {
  let list = platformLogs.value || []
  if (logLevel.value) list = list.filter((l) => String(l.type) === String(logLevel.value))
  if (logKeyword.value) {
    const kw = logKeyword.value.toLowerCase()
    list = list.filter((l) => `${l.title || ''} ${l.content || ''}`.toLowerCase().includes(kw))
  }
  return list
})

function applyFilter() {
  applied.keyword = filters.keyword
  applied.state = filters.state
  applied.protocol = filters.protocol
}

async function refreshAll() {
  await loadList()
}

async function loadList() {
  loading.value = true
  try {
    const [listRes, statsRes, deviceRes, todayRes] = await Promise.all([
      listPlatform({ pageNum: 1, pageSize: 200, orderByColumn: 'id', isAsc: 'DESC' }),
      getAllPlatformStats().catch(() => ({ data: [] })),
      getDeviceOnlineStats().catch(() => ({ data: [] })),
      getTodayMsgStats().catch(() => ({ data: [] }))
    ])
    platformList.value = (listRes.rows || []).map((r) => ({
      ...r,
      configObject: r.configObject || safeParse(r.config)
    }))
    const sm = {}
    ;(statsRes.data || []).forEach((s) => {
      sm[s.platformCode] = s
    })
    statsMap.value = sm
    const dm = {}
    ;(deviceRes.data || []).forEach((s) => {
      dm[s.pfCode] = s
    })
    deviceMap.value = dm
    const tm = {}
    ;(todayRes.data || []).forEach((s) => {
      tm[s.pfCode] = s
    })
    todayMap.value = tm
  } finally {
    loading.value = false
  }
}

function safeParse(str) {
  try {
    return str ? JSON.parse(str) : {}
  } catch {
    return {}
  }
}

function setLoading(code, v) {
  actionLoading.value = { ...actionLoading.value, [code]: v }
}

/** 启停只改 status，其余字段原样带回，避免 mapper 无条件写空 port */
function statusPayload(row, status) {
  return {
    id: row.id,
    name: row.name,
    code: row.code,
    ip: row.ip,
    port: row.port,
    apis: row.apis,
    config: row.config,
    remark: row.remark,
    status
  }
}

async function handleStart(row) {
  try {
    await proxy.$modal.confirm(`确认启动插件「${row.name}」？`)
    setLoading(row.code, true)
    // 后端 edit：status=启用时会自动停止再启动
    await updatePlatform(statusPayload(row, '1'))
    proxy.$modal.msgSuccess('已启动')
    await loadList()
  } catch (e) {
    /* cancel */
  } finally {
    setLoading(row.code, false)
  }
}

async function handleStop(row) {
  try {
    await proxy.$modal.confirm(`确认停止插件「${row.name}」？`)
    setLoading(row.code, true)
    await updatePlatform(statusPayload(row, '0'))
    proxy.$modal.msgSuccess('已停止')
    await loadList()
  } catch (e) {
    /* cancel */
  } finally {
    setLoading(row.code, false)
  }
}

async function handleRestart(row) {
  try {
    await proxy.$modal.confirm(`确认重启插件「${row.name}」？`)
    setLoading(row.code, true)
    await updatePlatform(statusPayload(row, '1'))
    proxy.$modal.msgSuccess('已重启')
    await loadList()
  } catch (e) {
    /* cancel */
  } finally {
    setLoading(row.code, false)
  }
}

async function handleUninstall(row) {
  try {
    await proxy.$modal.confirm(`确认卸载插件「${row.name}」？此操作将删除插件配置。`)
    await delPlatform(row.id)
    proxy.$modal.msgSuccess('已卸载')
    await loadList()
  } catch (e) {
    /* cancel */
  }
}

function openInstall() {
  installForm.value = { name: '', code: '', protocol: '', ip: '', port: undefined }
  installOpen.value = true
}

function submitInstall() {
  installRef.value?.validate(async (ok) => {
    if (!ok) return
    installing.value = true
    try {
      const f = installForm.value
      const configObject = {
        protocol: f.protocol || undefined,
        version: '1.0.0',
        capabilities: '告警 / 监测 / 设备'
      }
      await addPlatform({
        name: f.name,
        code: f.code,
        ip: f.ip || null,
        port: f.port || null,
        status: '0',
        running: '0',
        config: JSON.stringify(configObject),
        configObject
      })
      proxy.$modal.msgSuccess('安装成功，请配置后启动')
      installOpen.value = false
      await loadList()
    } finally {
      installing.value = false
    }
  })
}

function normalizeSchema(schema) {
  if (!Array.isArray(schema)) return []
  return schema
    .map((f) => {
      const code = f.code != null ? String(f.code) : f.key != null ? String(f.key) : ''
      if (!code || code === 'ip' || code === 'port') return null
      let type = String(f.type || 'string').toLowerCase()
      if (type === 'switch') type = 'boolean'
      if (type === 'text') type = 'string'
      return {
        code,
        name: f.name || f.label || code,
        desc: f.desc || f.description || '',
        type,
        required: !!f.required,
        options: f.options || [],
        defaultValue: f.defaultValue
      }
    })
    .filter(Boolean)
}

async function openConfig(row) {
  const res = await getPlatform(row.id)
  const d = res.data || {}
  const configObject = d.configObject || safeParse(d.config)
  const schema = normalizeSchema(statsMap.value[d.code]?.configSchema)
  schema.forEach((f) => {
    if (configObject[f.code] === undefined || configObject[f.code] === null || configObject[f.code] === '') {
      if (f.defaultValue !== undefined) configObject[f.code] = f.defaultValue
    }
  })
  if (!schema.length) {
    configObject.username = configObject.username ?? ''
    configObject.password = configObject.password ?? ''
    configObject.heartbeatInterval = configObject.heartbeatInterval ?? 30
    configObject.reconnectInterval = configObject.reconnectInterval ?? 10
    configObject.logLevel = configObject.logLevel ?? 'INFO'
    configObject.autoRestart = configObject.autoRestart ?? true
    configObject.maxRestart = configObject.maxRestart ?? 5
    configObject.restartBackoff = configObject.restartBackoff ?? 10
  }
  schemaFields.value = schema
  cfgForm.value = {
    id: d.id,
    name: d.name,
    code: d.code,
    ip: d.ip || configObject.ip || '',
    port: d.port != null ? Number(d.port) : configObject.port != null ? Number(configObject.port) : undefined,
    apis: d.apis || '',
    remark: d.remark || '',
    status: d.status || '1',
    configObject: { ...configObject }
  }
  delete cfgForm.value.configObject.ip
  delete cfgForm.value.configObject.port
  cfgTitle.value = `接入参数配置 · ${d.name}（实例 ${d.code}）`
  cfgOpen.value = true
}

function testConnection() {
  testing.value = true
  setTimeout(() => {
    testing.value = false
    const alive = statsMap.value[cfgForm.value.code]?.alive
    if (alive) proxy.$modal.msgSuccess('连接正常')
    else proxy.$modal.msgWarning('当前插件未在运行或连接不可达，请保存后启动再试')
  }, 400)
}

function saveConfig() {
  saving.value = true
  const form = cfgForm.value
  const payload = {
    id: form.id,
    name: form.name,
    code: form.code,
    ip: form.ip || null,
    port: form.port || null,
    apis: form.apis,
    remark: form.remark,
    status: form.status || '1',
    configObject: form.configObject || {},
    config: JSON.stringify(form.configObject || {})
  }
  updatePlatform(payload)
    .then(() => {
      proxy.$modal.msgSuccess('已保存并生效')
      cfgOpen.value = false
      return loadList()
    })
    .finally(() => {
      saving.value = false
    })
}

async function openDrawer(row) {
  currentCode.value = row.code
  drawerTitle.value = `${row.name} · 实例 ${row.code}`
  drawerTab.value = 'stats'
  drawerOpen.value = true
  drawerStats.value = { total: 0, todayTotal: 0, todayFailed: 0 }
  typeDist.value = []
  hourlyTrend.value = []
  platformLogs.value = []
  logTotal.value = 0
  logPage.value = 1
  logLevel.value = ''
  logKeyword.value = ''
  await Promise.all([loadDrawerStats(row.code), loadLogs()])
}

async function loadDrawerStats(code) {
  drawerStatsLoading.value = true
  try {
    const res = await getPluginMsgStats(code)
    const d = res.data || {}
    drawerStats.value = {
      total: Number(d.total || 0),
      todayTotal: Number(d.todayTotal || 0),
      todayFailed: Number(d.todayFailed || 0)
    }
    const rows = d.typeDist || []
    const max = Math.max(1, ...rows.map((r) => Number(r.total || 0)))
    typeDist.value = rows
      .slice()
      .sort((a, b) => Number(b.total) - Number(a.total))
      .map((r) => ({ ...r, percent: Math.round((Number(r.total) / max) * 100) }))
    hourlyTrend.value = d.hourlyTrend || []
    nextTick(() => renderTrend())
  } finally {
    drawerStatsLoading.value = false
  }
}

function renderTrend() {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  const points = hourlyTrend.value || []
  if (!points.length) {
    trendChart.clear()
    return
  }
  trendChart.setOption({
    grid: { left: 36, right: 12, top: 16, bottom: 28 },
    xAxis: {
      type: 'category',
      data: points.map((p) => String(p.timePoint || '').slice(11, 16)),
      axisLabel: { color: '#94a3b8', fontSize: 11 },
      axisLine: { lineStyle: { color: '#e2e8f0' } }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 }
    },
    tooltip: { trigger: 'axis' },
    series: [
      {
        type: 'bar',
        data: points.map((p) => Number(p.total || 0)),
        itemStyle: { color: '#2563eb', borderRadius: [3, 3, 0, 0] },
        barMaxWidth: 16
      }
    ]
  })
}

async function loadLogs() {
  if (!currentCode.value) return
  logLoading.value = true
  try {
    const res = await getPlatformLogs(currentCode.value, {
      pageNum: logPage.value,
      pageSize: logPageSize,
      orderByColumn: 'create_time',
      isAsc: 'desc'
    })
    platformLogs.value = res.rows || []
    logTotal.value = res.total || 0
  } finally {
    logLoading.value = false
  }
}

function reloadLogs() {
  logPage.value = 1
  loadLogs()
}

async function clearLogs() {
  try {
    await proxy.$modal.confirm('确定清空当前插件的全部运行日志？')
    await clearPlatformLogs(currentCode.value)
    proxy.$modal.msgSuccess('已清空')
    await reloadLogs()
  } catch (e) {
    /* cancel */
  }
}

function logTypeLabel(log) {
  const map = { 1: 'INFO ', 2: 'INFO ', 3: 'INFO ', 4: 'ERROR', 5: 'DEBUG' }
  if (log.content && String(log.content).includes('失败')) return 'ERROR'
  return map[String(log.type)] || 'INFO '
}
function logLevelClass(log) {
  const label = logTypeLabel(log)
  if (label.includes('ERROR')) return 'lv-error'
  if (String(log.type) === '4') return 'lv-error'
  if (String(log.type) === '5') return 'lv-debug'
  return 'lv-info'
}
function showLogDetail(log) {
  logDetailTitle.value = log.title || '日志详情'
  logDetailContent.value = log.content != null ? String(log.content) : ''
  logDetailOpen.value = true
}

watch(drawerTab, (t) => {
  if (t === 'stats') nextTick(() => renderTrend())
})

onBeforeUnmount(() => {
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
})

loadList()
</script>

<style scoped>
.plugin-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.plugin-summary-text {
  color: #64748b;
  font-size: 13px;
}
.plugin-summary-text b {
  color: #0f172a;
  font-weight: 700;
}
.plugin-summary-text b.ok { color: #16a34a; }
.plugin-summary-text b.err { color: #dc2626; }

.pc-main { flex: 1; min-width: 0; }
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
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  line-height: 1.5;
}
.gw-tag {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 5px;
  font-size: 12px;
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.cfg-hint {
  margin: 0 0 14px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}
.cfg-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 20px;
}
.cfg-grid :deep(.el-form-item.full),
.cfg-grid .full {
  grid-column: 1 / -1;
}
.field-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}
.auto-restart {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.drawer-stats { padding-right: 4px; }
.stat-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.mini-stat {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 14px 16px;
}
.mini-stat .label { color: #64748b; font-size: 13px; margin-bottom: 8px; }
.mini-stat .value { font-size: 24px; font-weight: 700; color: #0f172a; }
.mt16 { margin-top: 16px; }
.trend-chart { width: 100%; height: 180px; }

.log-line { word-break: break-all; }
.log-line .t { color: #64748b; margin-right: 6px; }
.log-line .lv-info { color: #38bdf8; }
.log-line .lv-debug { color: #94a3b8; }
.log-line .lv-warn { color: #facc15; }
.log-line .lv-error { color: #f87171; }
.log-pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
.log-detail-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13px;
  line-height: 1.5;
  max-height: 65vh;
  overflow: auto;
}

@media (max-width: 900px) {
  .cfg-grid { grid-template-columns: 1fr; }
  .stat-row { grid-template-columns: 1fr; }
}
</style>
