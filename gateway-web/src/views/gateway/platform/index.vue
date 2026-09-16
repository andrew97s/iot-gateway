<template>
  <div class="gw-page plugin-page">
    <div class="plugin-summary">
      <div class="plugin-summary-text">
        运行中 <b class="ok">{{ summary.running }}</b> · 已停止 <b>{{ summary.stopped }}</b> · 已安装 <b>{{ summary.installed }}</b> · 异常
        <b class="err">{{ summary.abnormal }}</b>
        <span class="gw-muted" style="margin-left: 10px">共 {{ summary.total || 0 }} 个插件项</span>
      </div>
      <div class="plugin-summary-actions">
        <el-button icon="Upload" size="small" v-hasPermi="['sys:platform:add']" @click="openPackageUpload">安装插件包</el-button>
        <el-button icon="Plus" size="small" type="primary" v-hasPermi="['sys:platform:add']" @click="openInstall">创建实例</el-button>
      </div>
    </div>

    <div class="gw-card">
      <div class="gw-card-body gw-filter-bar">
        <el-input v-model="filters.keyword" clearable placeholder="插件名称 / 厂商" style="width: 200px" @keyup.enter="loadList" />
        <el-select-v2 v-model="filters.state" :options="STATE_OPTIONS" clearable placeholder="全部状态" style="width: 130px" />
        <el-select-v2 v-model="filters.protocol" :options="protocolSelectOptions" clearable placeholder="全部协议" style="width: 150px" />
        <el-button icon="Search" type="primary" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" :loading="loading" @click="handleQuery">刷新</el-button>
        <div class="spacer" />
        <span class="gw-muted gw-small"> 插件为厂商接入的最小单元；配置由 plugin.yaml / config-schema 驱动，启停热生效 </span>
      </div>
    </div>

    <!-- 卡片栅格直接铺在灰底上，与原型一致（不加白色容器） -->
    <div class="plugin-list">
      <div class="gw-plugin-grid" v-loading="loading">
        <el-empty description="暂无插件实例，请先创建或安装" style="grid-column: 1 / -1" v-if="!loading && instances.length === 0" />
        <div
          v-for="row in pagedInstances"
          class="gw-plugin-card"
          :class="{ 'is-error': row.state === 'abnormal', 'is-disabled': row.state === 'stopped' || row.state === 'installed' }"
          :key="row.instanceId || `installed-${row.pluginId}`"
        >
          <div class="pc-head">
            <div class="pc-ico" :style="{ background: pluginColor(row.pluginId) }">{{ pluginInitials(row.pluginId) }}</div>
            <div class="pc-main">
              <div class="pc-title-row">
                <b class="pc-name" :title="row.name">{{ row.name }}</b>
                <span class="gw-badge" :class="badgeClass(row.state)">{{ stateLabel(row.state) }}</span>
              </div>
              <div class="pc-meta gw-muted gw-small">
                <span class="gw-tag">{{ row.protocol || row.pluginId }}</span>
                <span class="gw-tag">v{{ row.version || '—' }}</span>
                <span v-if="row.instanceId">· 实例 {{ row.instanceId }}</span>
                <span v-else>· 尚未创建实例</span>
                <span v-if="capsText(row)">· 能力：{{ capsText(row) }}</span>
                <span v-if="row.state === 'abnormal' && row.restartCount">· 已自动重启 {{ row.restartCount }} 次</span>
              </div>
              <div class="pc-health err" v-if="row.state === 'abnormal'">
                {{ row.healthReason || '健康检查失败' }}
                <span v-if="row.nextRestartTime"> · 下次重试 {{ formatTime(row.nextRestartTime) }}</span>
                <span v-if="row.consecutiveFailures"> · 连续失败 {{ row.consecutiveFailures }} 次</span>
              </div>
            </div>
          </div>
          <div class="pc-stats">
            <div>
              <div class="v">{{ fmt(row.deviceCount) }}</div>
              <div class="k">接入设备</div>
            </div>
            <div>
              <div class="v">{{ fmt(row.todayMsgCount) }}</div>
              <div class="k">今日消息</div>
            </div>
            <div>
              <div class="v" :style="{ color: failColor(row) }">
                {{ (row.state === 'stopped' || row.state === 'installed') && !row.todayMsgCount ? '—' : fmt(row.todayFailCount || row.errCount) }}
              </div>
              <div class="k">转换失败</div>
            </div>
          </div>
          <div class="pc-foot">
            <template v-if="row.state === 'installed'">
              <el-button size="small" type="primary" v-hasPermi="['sys:platform:add']" @click="openInstallFor(row)">创建实例</el-button>
            </template>
            <template v-else>
              <el-button size="small" @click="openDrawer(row)">日志 / 统计</el-button>
              <el-button size="small" v-hasPermi="['sys:platform:edit']" @click="openConfig(row)">配置</el-button>
            </template>
            <template v-if="row.state === 'running'">
              <el-button :loading="busy[row.instanceId]" plain size="small" type="danger" @click="doStop(row)">停止</el-button>
            </template>
            <template v-else-if="row.state === 'abnormal'">
              <el-button :loading="busy[row.instanceId]" size="small" type="primary" @click="doRestart(row)">重启</el-button>
              <el-button :loading="busy[row.instanceId]" plain size="small" type="danger" @click="doStop(row)">停止</el-button>
            </template>
            <template v-else-if="row.state === 'stopped'">
              <el-button :loading="busy[row.instanceId]" size="small" type="primary" @click="doStart(row)">启动</el-button>
              <el-button plain size="small" type="danger" v-hasPermi="['sys:platform:remove']" @click="doUninstall(row)">卸载</el-button>
            </template>
          </div>
        </div>
      </div>
      <div class="plugin-pager" v-show="instances.length > 0">
        <pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNum" :page-sizes="[10, 20, 30]" :total="instances.length" />
      </div>
    </div>

    <!-- 创建实例：从类型目录选择 -->
    <el-dialog v-model="installOpen" append-to-body destroy-on-close title="创建插件实例" width="560px">
      <el-form label-width="100px" :model="installForm" :rules="installRules" ref="installRef">
        <el-form-item label="插件类型" prop="pluginId">
          <el-select-v2
            v-model="installForm.pluginId"
            :options="runnableTypeOptions"
            filterable
            :teleported="false"
            placeholder="选择已注册的可运行插件"
            style="width: 100%"
            @change="onTypePicked"
          >
            <template #default="{ item }">
              <span>{{ item.name }}</span>
              <span class="gw-muted gw-small" style="float: right">{{ item.protocol }} · v{{ item.version }}</span>
            </template>
          </el-select-v2>
        </el-form-item>
        <el-form-item label="实例编码">
          <el-input v-model="installForm.instanceId" disabled placeholder="与插件类型一致" />
          <div class="field-hint">当前运行时实例编码与插件类型 1:1（Handler 键）</div>
        </el-form-item>
        <el-form-item label="显示名称" prop="name">
          <el-input v-model="installForm.name" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="installForm.ip" placeholder="可选，配置时可再改" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="installForm.port" controls-position="right" :max="65535" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="installOpen = false">取消</el-button>
        <el-button :loading="installing" type="primary" @click="submitInstall">创建</el-button>
      </template>
    </el-dialog>

    <!-- 上传插件包 -->
    <el-dialog v-model="pkgOpen" append-to-body destroy-on-close title="安装插件包" width="520px">
      <p class="cfg-hint">上传 zip，需包含 <b>plugin.yaml</b> 与可选 <b>config-schema.json</b>；若类型已有内置 Handler 则可创建并启动实例。</p>
      <el-upload accept=".zip" :auto-upload="false" drag :limit="1" :on-change="onPkgChange" :on-remove="() => (pkgFile = null)">
        <div class="el-upload__text">将插件包拖到此处，或 <em>点击选择</em></div>
      </el-upload>
      <template #footer>
        <el-button @click="pkgOpen = false">取消</el-button>
        <el-button :disabled="!pkgFile" :loading="pkgLoading" type="primary" @click="submitPackage">上传安装</el-button>
      </template>
    </el-dialog>

    <!-- 配置 -->
    <el-dialog v-model="cfgOpen" append-to-body destroy-on-close :title="cfgTitle" width="720px">
      <p class="cfg-hint">表单由插件 <b>config-schema</b> 动态渲染；保存后热生效（运行中实例将自动重载）。</p>
      <el-form class="cfg-form" label-position="top" :model="cfgForm" ref="cfgFormRef">
        <div class="cfg-grid">
          <el-form-item v-for="field in schemaFields" :label="field.name" :required="!!field.required" :key="field.code">
            <el-select-v2
              v-model="cfgForm.config[field.code]"
              :options="field.options"
              clearable
              :teleported="false"
              style="width: 100%"
              v-if="field.type === 'select'"
            />
            <el-switch v-model="cfgForm.config[field.code]" v-else-if="field.type === 'boolean'" />
            <el-input-number
              v-model="cfgForm.config[field.code]"
              controls-position="right"
              style="width: 100%"
              v-else-if="field.type === 'integer' || field.type === 'number'"
            />
            <el-input v-model="cfgForm.config[field.code]" show-password type="password" v-else-if="field.type === 'password'" />
            <el-input v-model="cfgForm.config[field.code]" :rows="3" type="textarea" v-else-if="field.type === 'textarea'" />
            <el-input v-model="cfgForm.config[field.code]" v-else />
            <template #label>
              <span style="margin-right: 3px">{{ field.name }}</span>
              <!--              <el-tooltip-->
              <!--                  v-if="field.desc"-->
              <!--                  :content="field.desc"-->
              <!--                  placement="top"-->
              <!--              >-->
              <!--                <el-icon class="tip-icon">-->
              <!--                  <InfoFilled />-->
              <!--                </el-icon>-->
              <!--              </el-tooltip>-->
            </template>
            <div class="field-hint" v-if="field.desc">{{ field.desc }}</div>
          </el-form-item>
          <el-form-item class="full" label="备注">
            <el-input v-model="cfgForm.remark" :rows="2" type="textarea" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button :loading="testing" @click="doTest">测试连接</el-button>
        <el-button @click="cfgOpen = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="saveConfig">保存并生效</el-button>
      </template>
    </el-dialog>

    <!-- 日志/统计 -->
    <el-drawer
      v-model="drawerOpen"
      destroy-on-close
      direction="rtl"
      size="700px"
      class="plugin-drawer"
      custom-class="plugin-drawer"
      :title="drawerTitle"
    >
      <div class="drawer-health" :class="'is-' + (drawerHealth.state || 'stopped')">
        <div class="drawer-health-head">
          <b>健康信息</b>
          <span class="gw-badge" :class="badgeClass(drawerHealth.state)">{{ stateLabel(drawerHealth.state) }}</span>
        </div>
        <p class="drawer-health-reason">{{ healthSummary }}</p>
        <div class="drawer-health-meta">
          <span>最近检查 {{ formatTime(drawerHealth.lastHealthCheckTime) }}</span>
          <span :class="{ err: drawerHealth.consecutiveFailures > 0 }">连续失败 {{ drawerHealth.consecutiveFailures || 0 }} 次</span>
          <span>自动重启 {{ drawerHealth.restartCount || 0 }} 次</span>
          <span v-if="drawerHealth.nextRestartTime">下次重试 {{ formatTime(drawerHealth.nextRestartTime) }}</span>
          <span v-else-if="drawerHealth.lastStartTime">最近启动 {{ formatTime(drawerHealth.lastStartTime) }}</span>
        </div>
      </div>
      <el-tabs v-model="drawerTab">
        <el-tab-pane label="消息统计" name="stats">
          <div v-loading="drawerStatsLoading">
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
                <div class="value" :style="{ color: drawerStats.todayFailed > 0 ? '#dc2626' : '#16a34a' }">{{ fmt(drawerStats.todayFailed) }}</div>
              </div>
            </div>
            <div class="gw-card mt16">
              <div class="gw-card-head"><h2>今日消息类型分布</h2></div>
              <div class="gw-card-body">
                <el-empty description="今日暂无消息" :image-size="60" v-if="typeDist.length === 0" />
                <div class="gw-hbar" v-else>
                  <div v-for="item in typeDist" class="row" :key="item.type">
                    <span>{{ typeLabel(item.type) }}</span>
                    <div class="bar-bg"><div class="bar" :style="{ width: item.percent + '%', background: typeColor(item.type) }" /></div>
                    <span class="num">{{ fmt(item.total) }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div class="gw-card mt16">
              <div class="gw-card-head"><h2>近 24 小时消息量</h2></div>
              <div class="gw-card-body">
                <div class="trend-chart" ref="trendChartRef" v-show="hourlyTrend.length > 0" />
                <el-empty description="暂无趋势数据" :image-size="50" v-if="!drawerStatsLoading && hourlyTrend.length === 0" />
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="运行日志" name="logs">
          <div class="gw-filter-bar log-filter" style="margin-bottom: 12px">
            <el-select-v2 v-model="logLevel" :options="LOG_LEVEL_OPTIONS" clearable placeholder="全部级别" style="width: 130px" />
            <el-input v-model="logKeyword" clearable placeholder="关键字过滤" style="flex: 1" />
            <el-button icon="Refresh" :loading="logLoading" size="small" @click="loadLogs">刷新</el-button>
            <el-button :disabled="!displayLogs.length" icon="Download" size="small" @click="downloadLogs">下载日志</el-button>
          </div>
          <div class="gw-log-console" v-loading="logLoading">
            <template v-if="displayLogs.length">
              <div v-for="(log, idx) in displayLogs" class="log-line" :key="log.id || idx">
                <span class="t">{{ formatLogTime(log.time) }}</span>
                <span :class="'lv-' + log.level.toLowerCase()">[{{ padLevel(log.level) }}]</span>
                <span> [{{ currentId }}] {{ log.message }}</span>
              </div>
            </template>
            <div class="empty-log" v-else>暂无运行日志</div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>
  </div>
</template>

<script setup name="Platform">
import * as echarts from 'echarts'
import {
  pluginSummary,
  listPluginTypes,
  listPluginInstances,
  createPluginInstance,
  installPluginPackage,
  updatePluginConfig,
  startPluginInstance,
  stopPluginInstance,
  restartPluginInstance,
  uninstallPluginInstance,
  testPluginConnection,
  getPluginInstanceStats,
  getPluginInstanceLogs,
  getPluginInstance
} from '@/api/sys/plugin'

const { proxy } = getCurrentInstance()

const TYPE_META = {
  alarm: { label: '告警事件', color: '#dc2626' },
  telemetry: { label: '监测数据', color: '#2563eb' },
  device_add: { label: '监测数据', color: '#2563eb' },
  device_upd: { label: '设备事件', color: '#d97706' },
  device_del: { label: '设备事件', color: '#d97706' },
  device_online: { label: '设备事件', color: '#d97706' },
  device_offline: { label: '设备事件', color: '#d97706' },
  control: { label: '指令回执', color: '#0891b2' },
  event: { label: '事件', color: '#0891b2' }
}
const PLUGIN_COLORS = [
  'linear-gradient(135deg,#dc2626,#f97316)',
  'linear-gradient(135deg,#2563eb,#06b6d4)',
  'linear-gradient(135deg,#16a34a,#84cc16)',
  'linear-gradient(135deg,#7c3aed,#c084fc)',
  'linear-gradient(135deg,#64748b,#94a3b8)',
  'linear-gradient(135deg,#d97706,#facc15)'
]

const STATE_OPTIONS = [
  { value: 'running', label: '运行中' },
  { value: 'stopped', label: '已停止' },
  { value: 'installed', label: '已安装' },
  { value: 'abnormal', label: '异常' }
]
const LOG_LEVEL_OPTIONS = [
  { value: 'DEBUG', label: 'DEBUG' },
  { value: 'INFO', label: 'INFO' },
  { value: 'WARN', label: 'WARN' },
  { value: 'ERROR', label: 'ERROR' }
]

const loading = ref(false)
const instances = ref([])
const summary = ref({ running: 0, stopped: 0, installed: 0, abnormal: 0, total: 0 })
const types = ref([])
const filters = reactive({ keyword: '', state: undefined, protocol: undefined })
const queryParams = reactive({ pageNum: 1, pageSize: 9 })
const busy = ref({})

const installOpen = ref(false)
const installing = ref(false)
const installForm = ref({ pluginId: undefined, instanceId: '', name: '', ip: '', port: undefined })
const installRules = {
  pluginId: [{ required: true, message: '请选择插件类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}
const installRef = ref()

const pkgOpen = ref(false)
const pkgFile = ref(null)
const pkgLoading = ref(false)

const cfgOpen = ref(false)
const cfgTitle = ref('')
const cfgForm = ref({ instanceId: '', ip: '', port: undefined, apis: '', remark: '', config: {} })
const schemaFields = ref([])
const saving = ref(false)
const testing = ref(false)

const drawerOpen = ref(false)
const drawerTitle = ref('')
const drawerTab = ref('stats')
const drawerHealth = ref(emptyHealth())
const drawerStatsLoading = ref(false)
const drawerStats = ref({ total: 0, todayTotal: 0, todayFailed: 0 })
const typeDist = ref([])
const hourlyTrend = ref([])
const trendChartRef = ref(null)
let trendChart = null
const currentId = ref('')
const logLoading = ref(false)
const consoleLogs = ref([])
const logKeyword = ref('')
const logLevel = ref()

const runnableTypes = computed(() => (types.value || []).filter((t) => t.runnable))
const runnableTypeOptions = computed(() =>
  (runnableTypes.value || []).map((t) => ({
    value: t.id,
    label: `${t.name}（${t.id}）`,
    name: t.name,
    protocol: t.protocol,
    version: t.version
  }))
)
const protocolOptions = computed(() => {
  const set = new Set()
  instances.value.forEach((r) => r.protocol && set.add(r.protocol))
  types.value.forEach((t) => t.protocol && set.add(t.protocol))
  return Array.from(set)
})
const protocolSelectOptions = computed(() => protocolOptions.value.map((p) => ({ value: p, label: p })))
const pagedInstances = computed(() => {
  const start = (queryParams.pageNum - 1) * queryParams.pageSize
  return (instances.value || []).slice(start, start + queryParams.pageSize)
})
/** 原型控制台：级别 + 关键字过滤 */
const displayLogs = computed(() => {
  let list = consoleLogs.value || []
  if (logLevel.value) {
    list = list.filter((l) => l.level === logLevel.value)
  }
  if (logKeyword.value) {
    const kw = logKeyword.value.toLowerCase()
    list = list.filter((l) =>
      String(l.message || '')
        .toLowerCase()
        .includes(kw)
    )
  }
  return list
})

function emptyHealth() {
  return {
    state: '',
    healthReason: '',
    connectionInfo: '',
    lastHealthCheckTime: null,
    consecutiveFailures: 0,
    restartCount: 0,
    nextRestartTime: null,
    lastStartTime: null,
    lastStopTime: null
  }
}
function fillHealth(source) {
  const s = source || {}
  drawerHealth.value = {
    state: s.state || '',
    healthReason: s.healthReason || '',
    connectionInfo: s.connectionInfo || '',
    lastHealthCheckTime: s.lastHealthCheckTime || null,
    consecutiveFailures: Number(s.consecutiveFailures || 0),
    restartCount: Number(s.restartCount || 0),
    nextRestartTime: s.nextRestartTime || null,
    lastStartTime: s.lastStartTime || null,
    lastStopTime: s.lastStopTime || null
  }
}
const healthSummary = computed(() => {
  const h = drawerHealth.value || {}
  return h.healthReason || h.connectionInfo || '暂无健康检查结果'
})

function fmt(v) {
  return Number(v || 0).toLocaleString('zh-CN')
}
function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '—'
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
function stateLabel(s) {
  return { running: '运行中', stopped: '已停止', installed: '已安装', abnormal: '异常' }[s] || s
}
function badgeClass(s) {
  return { running: 'ok', abnormal: 'err', stopped: 'off', installed: 'info' }[s] || 'off'
}
function capsText(row) {
  const caps = row.capabilities || []
  if (!caps.length) return ''
  const map = { alarm: '告警', monitor: '监测', telemetry: '监测', device: '设备', device_info: '设备', control: '反控' }
  return caps.map((c) => map[c] || c).join(' / ')
}
function failColor(row) {
  const n = Number(row.todayFailCount || row.errCount || 0)
  if ((row.state === 'stopped' || row.state === 'installed') && !row.todayMsgCount) return '#64748b'
  return n > 0 ? '#dc2626' : '#16a34a'
}
function typeLabel(t) {
  return TYPE_META[t]?.label || t || '未知'
}
function typeColor(t) {
  return TYPE_META[t]?.color || '#94a3b8'
}
function setBusy(id, v) {
  busy.value = { ...busy.value, [id]: v }
}

function handleQuery() {
  queryParams.pageNum = 1
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    const [sumRes, listRes, typeRes] = await Promise.all([
      pluginSummary().catch(() => ({ data: {} })),
      listPluginInstances({ ...filters }),
      listPluginTypes().catch(() => ({ data: [] }))
    ])
    summary.value = sumRes.data || {}
    instances.value = listRes.data || []
    types.value = typeRes.data || []
    const maxPage = Math.max(1, Math.ceil(instances.value.length / queryParams.pageSize) || 1)
    if (queryParams.pageNum > maxPage) queryParams.pageNum = maxPage
  } finally {
    loading.value = false
  }
}

function openInstall() {
  installForm.value = { pluginId: undefined, instanceId: '', name: '', ip: '', port: undefined }
  installOpen.value = true
  if (!types.value.length) listPluginTypes().then((r) => (types.value = r.data || []))
}
async function openInstallFor(row) {
  if (!types.value.length) {
    const res = await listPluginTypes()
    types.value = res.data || []
  }
  installForm.value = {
    pluginId: row.pluginId,
    instanceId: row.pluginId,
    name: row.name || '',
    ip: '',
    port: undefined
  }
  installOpen.value = true
}
function onTypePicked(id) {
  const t = types.value.find((x) => x.id === id)
  installForm.value.instanceId = id
  if (t) installForm.value.name = t.name
}
function submitInstall() {
  installRef.value?.validate(async (ok) => {
    if (!ok) return
    installing.value = true
    try {
      await createPluginInstance({
        pluginId: installForm.value.pluginId,
        instanceId: installForm.value.pluginId,
        name: installForm.value.name,
        ip: installForm.value.ip || null,
        port: installForm.value.port || null
      })
      proxy.$modal.msgSuccess('实例已创建，请配置后启动')
      installOpen.value = false
      await loadList()
    } finally {
      installing.value = false
    }
  })
}

function openPackageUpload() {
  pkgFile.value = null
  pkgOpen.value = true
}
function onPkgChange(file) {
  pkgFile.value = file?.raw || null
}
async function submitPackage() {
  if (!pkgFile.value) return
  pkgLoading.value = true
  try {
    const res = await installPluginPackage(pkgFile.value)
    const d = res.data || {}
    proxy.$modal.msgSuccess(d.runnable ? `插件包 ${d.id} 已安装，可创建实例` : `插件包 ${d.id} 元数据已安装（无内置 Handler，暂不可运行）`)
    pkgOpen.value = false
    types.value = (await listPluginTypes()).data || []
  } finally {
    pkgLoading.value = false
  }
}

async function doStart(row) {
  try {
    await proxy.$modal.confirm(`确认启动「${row.name}」？`)
    setBusy(row.instanceId, true)
    await startPluginInstance(row.instanceId)
    proxy.$modal.msgSuccess('已启动')
    await loadList()
  } catch (e) {
    /* cancel or error shown */
  } finally {
    setBusy(row.instanceId, false)
  }
}
async function doStop(row) {
  try {
    await proxy.$modal.confirm(`确认停止「${row.name}」？`)
    setBusy(row.instanceId, true)
    await stopPluginInstance(row.instanceId)
    proxy.$modal.msgSuccess('已停止')
    await loadList()
  } catch (e) {
    /* */
  } finally {
    setBusy(row.instanceId, false)
  }
}
async function doRestart(row) {
  try {
    await proxy.$modal.confirm(`确认重启「${row.name}」？`)
    setBusy(row.instanceId, true)
    await restartPluginInstance(row.instanceId)
    proxy.$modal.msgSuccess('已重启')
    await loadList()
  } catch (e) {
    /* */
  } finally {
    setBusy(row.instanceId, false)
  }
}
async function doUninstall(row) {
  try {
    await proxy.$modal.confirm(`确认卸载「${row.name}」？将删除实例配置。`)
    await uninstallPluginInstance(row.instanceId)
    proxy.$modal.msgSuccess('已卸载')
    await loadList()
  } catch (e) {
    /* */
  }
}

function normalizeSchema(schema) {
  if (!Array.isArray(schema)) return []
  return schema
    .map((f) => {
      const code = f.code != null ? String(f.code) : f.key != null ? String(f.key) : ''
      let type = String(f.type || 'string').toLowerCase()
      if (type === 'switch') type = 'boolean'
      if (type === 'text') type = 'string'
      return {
        code,
        name: f.name || f.label || code,
        desc: f.desc || f.description || '',
        type,
        required: !!f.required,
        options: (f.options || []).map((opt) => ({
          value: opt.value,
          label: opt.label ?? String(opt.value ?? '')
        })),
        defaultValue: f.defaultValue
      }
    })
    .filter(Boolean)
}

async function openConfig(row) {
  const res = await getPluginInstance(row.instanceId)
  const d = res.data || {}
  const config = { ...(d.config || {}) }
  const schema = normalizeSchema(d.configSchema)
  schema.forEach((f) => {
    if (config[f.code] === undefined || config[f.code] === null || config[f.code] === '') {
      if (f.defaultValue !== undefined) config[f.code] = f.defaultValue
    }
    if (f.type === 'select' && (config[f.code] === '' || config[f.code] === null)) {
      config[f.code] = undefined
    }
  })
  Object.keys(config).forEach((k) => {
    if (k.startsWith('_')) delete config[k]
  })
  schemaFields.value = schema
  cfgForm.value = {
    instanceId: d.instanceId,
    ip: d.ip || '',
    port: d.port,
    apis: d.apis || '',
    remark: d.remark || '',
    config
  }
  cfgTitle.value = `接入参数配置 · ${d.name}（实例 ${d.instanceId}）`
  cfgOpen.value = true
}

async function doTest() {
  testing.value = true
  try {
    const res = await testPluginConnection(cfgForm.value.instanceId)
    const d = res.data || {}
    if (d.success) proxy.$modal.msgSuccess(d.message || '连接正常')
    else proxy.$modal.msgWarning(d.message || '连接失败')
  } finally {
    testing.value = false
  }
}

async function saveConfig() {
  saving.value = true
  try {
    await updatePluginConfig(cfgForm.value.instanceId, {
      ip: cfgForm.value.ip,
      port: cfgForm.value.port,
      apis: cfgForm.value.apis,
      remark: cfgForm.value.remark,
      config: cfgForm.value.config
    })
    proxy.$modal.msgSuccess('已保存并生效')
    cfgOpen.value = false
    await loadList()
  } finally {
    saving.value = false
  }
}

async function openDrawer(row) {
  currentId.value = row.instanceId
  drawerTitle.value = `${row.name} · 实例 ${row.instanceId}`
  drawerTab.value = 'stats'
  fillHealth(row)
  drawerOpen.value = true
  logKeyword.value = ''
  logLevel.value = undefined
  await Promise.all([loadDrawerStats(row.instanceId), loadLogs()])
}

async function loadDrawerStats(id) {
  drawerStatsLoading.value = true
  try {
    const res = await getPluginInstanceStats(id)
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
    if (d.runtime) {
      fillHealth({
        ...drawerHealth.value,
        ...d.runtime,
        restartCount: d.restartCount ?? d.runtime.restartCount
      })
    }
    nextTick(() => renderTrend())
  } finally {
    drawerStatsLoading.value = false
  }
}

function disposeTrendChart() {
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
}

function renderTrend() {
  if (!trendChartRef.value) return
  // destroy-on-close 会重建 DOM，旧实例挂在已销毁节点上，需重新 init
  if (trendChart && trendChart.getDom() !== trendChartRef.value) {
    disposeTrendChart()
  }
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  const points = hourlyTrend.value || []
  if (!points.length) {
    trendChart.clear()
    return
  }
  trendChart.setOption(
    {
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
    },
    true
  )
  nextTick(() => trendChart?.resize())
}

/** 将后台日志行映射为原型控制台条目（忽略原有分类型卡片逻辑） */
function mapToConsoleLine(row) {
  const title = String(row.title || '').trim()
  const content = String(row.content || '').trim()
  const joined = [title, content].filter(Boolean).join(' · ')
  const text = joined || '—'
  let level = 'INFO'
  const lower = text.toLowerCase()
  if (String(row.type) === '4' || /失败|error|exception|超时/.test(lower)) level = 'ERROR'
  else if (/warn|警告|降级|缓慢/.test(lower)) level = 'WARN'
  else if (String(row.type) === '5' || /debug|心跳|trace/.test(lower)) level = 'DEBUG'
  return {
    id: row.id,
    time: row.createTime || '',
    level,
    message: text
  }
}

async function loadLogs() {
  if (!currentId.value) return
  logLoading.value = true
  try {
    const res = await getPluginInstanceLogs(currentId.value, {
      pageNum: 1,
      pageSize: 200,
      orderByColumn: 'create_time',
      isAsc: 'desc'
    })
    consoleLogs.value = (res.rows || []).map(mapToConsoleLine)
  } finally {
    logLoading.value = false
  }
}

function padLevel(level) {
  const s = String(level || 'INFO')
  return (s + '     ').slice(0, 5)
}
function formatLogTime(t) {
  if (!t) return '--:--:--.---'
  const s = String(t)
  // 优先展示 HH:mm:ss.SSS / HH:mm:ss
  const m = s.match(/(\d{2}:\d{2}:\d{2}(?:\.\d{1,3})?)/)
  return m ? m[1] : s
}
function downloadLogs() {
  const lines = displayLogs.value.map((l) => `${formatLogTime(l.time)} [${padLevel(l.level)}] [${currentId.value}] ${l.message}`)
  if (!lines.length) {
    proxy.$modal.msgWarning('暂无可下载日志')
    return
  }
  const blob = new Blob([lines.join('\n') + '\n'], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `plugin-${currentId.value}-runtime.log`
  a.click()
  URL.revokeObjectURL(url)
}

watch(drawerOpen, (open) => {
  if (!open) {
    disposeTrendChart()
    fillHealth(emptyHealth())
  }
})
watch(drawerTab, (t) => {
  if (t === 'stats') nextTick(() => renderTrend())
})
onBeforeUnmount(() => {
  disposeTrendChart()
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
.plugin-summary-text b.ok {
  color: #16a34a;
}
.plugin-summary-text b.err {
  color: #dc2626;
}
.plugin-summary-actions {
  display: flex;
  gap: 8px;
}

.pc-main {
  flex: 1;
  min-width: 0;
}
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
.pc-health {
  margin-top: 7px;
  font-size: 12px;
  line-height: 1.5;
}
.pc-health.err {
  color: #dc2626;
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
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 4px 20px;
}
.cfg-grid :deep(.el-form-item) {
  margin-bottom: 14px;
  min-width: 0;
}
.cfg-grid :deep(.el-form-item__content) {
  min-width: 0;
}
.cfg-grid :deep(.el-input),
.cfg-grid :deep(.el-select),
.cfg-grid :deep(.el-select-v2),
.cfg-grid :deep(.el-input-number),
.cfg-grid :deep(.el-textarea) {
  width: 100%;
}
.cfg-grid :deep(.el-form-item.full),
.cfg-grid .full {
  grid-column: 1 / -1;
}
.field-hint {
  margin-left: 4px;
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}
:deep(.el-drawer__header) {
  margin-bottom: 8px;
  padding-bottom: 0;
}
:deep(.el-drawer__body) {
  padding-top: 8px;
}
.drawer-health {
  margin: 0 0 14px;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
}
.drawer-health.is-running {
  background: #f0fdf4;
  border-color: #bbf7d0;
}
.drawer-health.is-abnormal {
  background: #fef2f2;
  border-color: #fecaca;
}
.drawer-health.is-stopped,
.drawer-health.is-installed {
  background: #f8fafc;
}
.drawer-health-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.drawer-health-head b {
  font-size: 14px;
  color: #0f172a;
}
.drawer-health-reason {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: #334155;
  word-break: break-all;
}
.drawer-health.is-abnormal .drawer-health-reason {
  color: #dc2626;
}
.drawer-health-meta {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}
.drawer-health-meta .err {
  color: #dc2626;
}
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
.mini-stat .label {
  color: #64748b;
  font-size: 13px;
  margin-bottom: 8px;
}
.mini-stat .value {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}
.mt16 {
  margin-top: 16px;
}
.trend-chart {
  width: 100%;
  height: 180px;
}
.trend-empty {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.plugin-pager {
  margin-top: 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
  display: flex;
  justify-content: flex-end;
}
.plugin-pager :deep(.pagination-container) {
  margin: 0 !important;
  padding: 10px 16px !important;
  border: none;
  background: transparent;
}
.log-filter {
  width: 100%;
}
.log-line {
  word-break: break-all;
}
.log-line .t {
  color: #64748b;
  margin-right: 6px;
}
.log-line .lv-info {
  color: #38bdf8;
}
.log-line .lv-debug {
  color: #94a3b8;
}
.log-line .lv-warn {
  color: #facc15;
}
.log-line .lv-error {
  color: #f87171;
}
.empty-log {
  color: #64748b;
}
@media (max-width: 900px) {
  .cfg-grid,
  .stat-row {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
/* 抽屉 teleport 到 body 后，scoped 选择器可能打不到标题间距 */
.plugin-drawer .el-drawer__header {
  margin-bottom: 8px;
  padding-bottom: 0;
}
.plugin-drawer .el-drawer__body {
  padding-top: 8px;
}
</style>
