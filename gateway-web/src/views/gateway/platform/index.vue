<template>
  <div class="gw-page plugin-page">
    <div class="plugin-summary">
      <div class="plugin-summary-text">
        运行中 <b class="ok">{{ summary.running }}</b>
        · 已停止 <b>{{ summary.stopped }}</b>
        · 异常 <b class="err">{{ summary.abnormal }}</b>
        <span class="gw-muted" style="margin-left: 10px">共 {{ summary.total || 0 }} 个实例</span>
      </div>
      <div class="plugin-summary-actions">
        <el-button size="small" icon="Upload" @click="openPackageUpload" v-hasPermi="['sys:platform:add']">安装插件包</el-button>
        <el-button type="primary" size="small" icon="Plus" @click="openInstall" v-hasPermi="['sys:platform:add']">创建实例</el-button>
      </div>
    </div>

    <div class="gw-card">
      <div class="gw-card-body gw-filter-bar">
        <el-input v-model="filters.keyword" clearable placeholder="插件名称 / 厂商" style="width: 200px" @keyup.enter="loadList" />
        <el-select v-model="filters.state" clearable placeholder="全部状态" style="width: 130px">
          <el-option label="运行中" value="running" />
          <el-option label="已停止" value="stopped" />
          <el-option label="已安装" value="installed" />
          <el-option label="异常" value="abnormal" />
        </el-select>
        <el-select v-model="filters.protocol" clearable placeholder="全部协议" style="width: 150px">
          <el-option v-for="p in protocolOptions" :key="p" :label="p" :value="p" />
        </el-select>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" :loading="loading" @click="handleQuery">刷新</el-button>
        <div class="spacer" />
        <span class="gw-muted gw-small">
          插件为厂商接入的最小单元；配置由 plugin.yaml / config-schema 驱动，启停热生效
        </span>
      </div>
    </div>

    <!-- 卡片栅格直接铺在灰底上，与原型一致（不加白色容器） -->
    <div class="plugin-list">
      <div class="gw-plugin-grid" v-loading="loading">
        <el-empty v-if="!loading && instances.length === 0" description="暂无插件实例，请先创建或安装" style="grid-column: 1 / -1" />
        <div
          v-for="row in pagedInstances"
          :key="row.instanceId"
          class="gw-plugin-card"
          :class="{ 'is-error': row.state === 'abnormal', 'is-disabled': row.state === 'stopped' || row.state === 'installed' }"
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
                <span>· 实例 {{ row.instanceId }}</span>
                <span v-if="capsText(row)">· 能力：{{ capsText(row) }}</span>
                <span v-if="row.state === 'abnormal' && row.restartCount">· 已自动重启 {{ row.restartCount }} 次</span>
              </div>
            </div>
          </div>
          <div class="pc-stats">
            <div><div class="v">{{ fmt(row.deviceCount) }}</div><div class="k">接入设备</div></div>
            <div><div class="v">{{ fmt(row.todayMsgCount) }}</div><div class="k">今日消息</div></div>
            <div>
              <div class="v" :style="{ color: failColor(row) }">
                {{ row.state === 'stopped' && !row.todayMsgCount ? '—' : fmt(row.todayFailCount || row.errCount) }}
              </div>
              <div class="k">转换失败</div>
            </div>
          </div>
          <div class="pc-foot">
            <el-button size="small" @click="openDrawer(row)">日志 / 统计</el-button>
            <el-button size="small" @click="openConfig(row)" v-hasPermi="['sys:platform:edit']">配置</el-button>
            <template v-if="row.state === 'running'">
              <el-button size="small" type="danger" plain :loading="busy[row.instanceId]" @click="doStop(row)">停止</el-button>
            </template>
            <template v-else-if="row.state === 'abnormal'">
              <el-button size="small" type="primary" :loading="busy[row.instanceId]" @click="doRestart(row)">重启</el-button>
              <el-button size="small" type="danger" plain :loading="busy[row.instanceId]" @click="doStop(row)">停止</el-button>
            </template>
            <template v-else>
              <el-button size="small" type="primary" :loading="busy[row.instanceId]" @click="doStart(row)">启动</el-button>
              <el-button size="small" type="danger" plain v-hasPermi="['sys:platform:remove']" @click="doUninstall(row)">卸载</el-button>
            </template>
          </div>
        </div>
      </div>
      <div class="plugin-pager" v-show="instances.length > 0">
        <pagination
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          :total="instances.length"
          :page-sizes="[10, 20, 30]"
        />
      </div>
    </div>

    <!-- 创建实例：从类型目录选择 -->
    <el-dialog v-model="installOpen" title="创建插件实例" width="560px" append-to-body destroy-on-close>
      <el-form ref="installRef" :model="installForm" :rules="installRules" label-width="100px">
        <el-form-item label="插件类型" prop="pluginId">
          <el-select v-model="installForm.pluginId" filterable style="width: 100%" placeholder="选择已注册的可运行插件" @change="onTypePicked">
            <el-option
              v-for="t in runnableTypes"
              :key="t.id"
              :label="`${t.name}（${t.id}）`"
              :value="t.id"
            >
              <span>{{ t.name }}</span>
              <span class="gw-muted gw-small" style="float: right">{{ t.protocol }} · v{{ t.version }}</span>
            </el-option>
          </el-select>
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
          <el-input-number v-model="installForm.port" :min="1" :max="65535" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="installOpen = false">取消</el-button>
        <el-button type="primary" :loading="installing" @click="submitInstall">创建</el-button>
      </template>
    </el-dialog>

    <!-- 上传插件包 -->
    <el-dialog v-model="pkgOpen" title="安装插件包" width="520px" append-to-body destroy-on-close>
      <p class="cfg-hint">上传 zip，需包含 <b>plugin.yaml</b> 与可选 <b>config-schema.json</b>；若类型已有内置 Handler 则可创建并启动实例。</p>
      <el-upload drag :auto-upload="false" :limit="1" accept=".zip" :on-change="onPkgChange" :on-remove="() => (pkgFile = null)">
        <div class="el-upload__text">将插件包拖到此处，或 <em>点击选择</em></div>
      </el-upload>
      <template #footer>
        <el-button @click="pkgOpen = false">取消</el-button>
        <el-button type="primary" :loading="pkgLoading" :disabled="!pkgFile" @click="submitPackage">上传安装</el-button>
      </template>
    </el-dialog>

    <!-- 配置 -->
    <el-dialog v-model="cfgOpen" :title="cfgTitle" width="720px" append-to-body destroy-on-close>
      <p class="cfg-hint">表单由插件 <b>config-schema</b> 动态渲染；保存后热生效（运行中实例将自动重载）。</p>
      <el-form ref="cfgFormRef" :model="cfgForm" label-position="top" class="cfg-form">
        <div class="cfg-grid">
          <el-form-item v-for="field in schemaFields" :key="field.code" :label="field.name" :required="!!field.required">
            <el-select v-if="field.type === 'select'" v-model="cfgForm.config[field.code]" clearable style="width: 100%">
              <el-option v-for="opt in field.options || []" :key="String(opt.value)" :label="opt.label" :value="opt.value" />
            </el-select>
            <el-switch v-else-if="field.type === 'boolean'" v-model="cfgForm.config[field.code]" />
            <el-input-number
                v-else-if="field.type === 'integer' || field.type === 'number'"
                v-model="cfgForm.config[field.code]"
                controls-position="right"
                style="width: 100%"
            />
            <el-input v-else-if="field.type === 'password'" v-model="cfgForm.config[field.code]" type="password" show-password />
            <el-input v-else-if="field.type === 'textarea'" v-model="cfgForm.config[field.code]" type="textarea" :rows="3" />
            <el-input v-else v-model="cfgForm.config[field.code]" />
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
            <div v-if="field.desc" class="field-hint">{{ field.desc }}</div>
          </el-form-item>
          <el-form-item label="备注" class="full">
            <el-input v-model="cfgForm.remark" type="textarea" :rows="2" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button :loading="testing" @click="doTest">测试连接</el-button>
        <el-button @click="cfgOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveConfig">保存并生效</el-button>
      </template>
    </el-dialog>

    <!-- 日志/统计 -->
    <el-drawer v-model="drawerOpen" :title="drawerTitle" direction="rtl" size="700px" destroy-on-close>
      <el-tabs v-model="drawerTab">
        <el-tab-pane label="消息统计" name="stats">
          <div v-loading="drawerStatsLoading">
            <div class="stat-row">
              <div class="mini-stat"><div class="label">累计接入消息</div><div class="value">{{ fmt(drawerStats.total) }}</div></div>
              <div class="mini-stat"><div class="label">今日接入消息</div><div class="value">{{ fmt(drawerStats.todayTotal) }}</div></div>
              <div class="mini-stat">
                <div class="label">今日转换失败</div>
                <div class="value" :style="{ color: drawerStats.todayFailed > 0 ? '#dc2626' : '#16a34a' }">{{ fmt(drawerStats.todayFailed) }}</div>
              </div>
            </div>
            <div class="gw-card mt16">
              <div class="gw-card-head"><h2>今日消息类型分布</h2></div>
              <div class="gw-card-body">
                <el-empty v-if="typeDist.length === 0" description="今日暂无消息" :image-size="60" />
                <div v-else class="gw-hbar">
                  <div v-for="item in typeDist" :key="item.type" class="row">
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
                <div v-show="hourlyTrend.length > 0" ref="trendChartRef" class="trend-chart" />
                <el-empty v-if="!drawerStatsLoading && hourlyTrend.length === 0" description="暂无趋势数据" :image-size="50" />
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="运行日志" name="logs">
          <div class="gw-filter-bar log-filter" style="margin-bottom: 12px">
            <el-select v-model="logLevel" clearable placeholder="全部级别" style="width: 130px">
              <el-option label="全部级别" value="" />
              <el-option label="DEBUG" value="DEBUG" />
              <el-option label="INFO" value="INFO" />
              <el-option label="WARN" value="WARN" />
              <el-option label="ERROR" value="ERROR" />
            </el-select>
            <el-input v-model="logKeyword" clearable placeholder="关键字过滤" style="flex: 1" />
            <el-button size="small" icon="Refresh" :loading="logLoading" @click="loadLogs">刷新</el-button>
            <el-button size="small" icon="Download" :disabled="!displayLogs.length" @click="downloadLogs">下载日志</el-button>
          </div>
          <div v-loading="logLoading" class="gw-log-console">
            <template v-if="displayLogs.length">
              <div v-for="(log, idx) in displayLogs" :key="log.id || idx" class="log-line">
                <span class="t">{{ formatLogTime(log.time) }}</span>
                <span :class="'lv-' + log.level.toLowerCase()">[{{ padLevel(log.level) }}]</span>
                <span> [{{ currentId }}] {{ log.message }}</span>
              </div>
            </template>
            <div v-else class="empty-log">暂无运行日志</div>
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

const loading = ref(false)
const instances = ref([])
const summary = ref({ running: 0, stopped: 0, abnormal: 0, total: 0 })
const types = ref([])
const filters = reactive({ keyword: '', state: '', protocol: '' })
const queryParams = reactive({ pageNum: 1, pageSize: 10 })
const busy = ref({})

const installOpen = ref(false)
const installing = ref(false)
const installForm = ref({ pluginId: '', instanceId: '', name: '', ip: '', port: undefined })
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
const logLevel = ref('')

const runnableTypes = computed(() => (types.value || []).filter((t) => t.runnable))
const protocolOptions = computed(() => {
  const set = new Set()
  instances.value.forEach((r) => r.protocol && set.add(r.protocol))
  types.value.forEach((t) => t.protocol && set.add(t.protocol))
  return Array.from(set)
})
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
    list = list.filter((l) => String(l.message || '').toLowerCase().includes(kw))
  }
  return list
})

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
function stateLabel(s) {
  return { running: '运行中', stopped: '已停止', installed: '已安装', configured: '已配置', abnormal: '异常' }[s] || s
}
function badgeClass(s) {
  return { running: 'ok', abnormal: 'err', stopped: 'off', installed: 'off', configured: 'info' }[s] || 'off'
}
function capsText(row) {
  const caps = row.capabilities || []
  if (!caps.length) return ''
  const map = { alarm: '告警', monitor: '监测', telemetry: '监测', device: '设备', device_info: '设备', control: '反控' }
  return caps.map((c) => map[c] || c).join(' / ')
}
function failColor(row) {
  const n = Number(row.todayFailCount || row.errCount || 0)
  if (row.state === 'stopped' && !row.todayMsgCount) return '#64748b'
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
  installForm.value = { pluginId: '', instanceId: '', name: '', ip: '', port: undefined }
  installOpen.value = true
  if (!types.value.length) listPluginTypes().then((r) => (types.value = r.data || []))
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
  } catch (e) { /* cancel or error shown */ } finally {
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
  } catch (e) { /* */ } finally {
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
  } catch (e) { /* */ } finally {
    setBusy(row.instanceId, false)
  }
}
async function doUninstall(row) {
  try {
    await proxy.$modal.confirm(`确认卸载「${row.name}」？将删除实例配置。`)
    await uninstallPluginInstance(row.instanceId)
    proxy.$modal.msgSuccess('已卸载')
    await loadList()
  } catch (e) { /* */ }
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
        options: f.options || [],
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
  drawerOpen.value = true
  logKeyword.value = ''
  logLevel.value = ''
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
    series: [{
      type: 'bar',
      data: points.map((p) => Number(p.total || 0)),
      itemStyle: { color: '#2563eb', borderRadius: [3, 3, 0, 0] },
      barMaxWidth: 16
    }]
  }, true)
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
  if (!open) disposeTrendChart()
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
.plugin-summary-text { color: #64748b; font-size: 13px; }
.plugin-summary-text b { color: #0f172a; font-weight: 700; }
.plugin-summary-text b.ok { color: #16a34a; }
.plugin-summary-text b.err { color: #dc2626; }
.plugin-summary-actions { display: flex; gap: 8px; }

.pc-main { flex: 1; min-width: 0; }
.pc-title-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.pc-name {
  font-size: 15px; color: #0f172a;
  overflow: hidden; white-space: nowrap; text-overflow: ellipsis;
}
.pc-meta {
  margin-top: 8px; display: flex; flex-wrap: wrap; align-items: center; gap: 6px; line-height: 1.5;
}
.gw-tag {
  display: inline-block; padding: 1px 8px; border-radius: 5px; font-size: 12px;
  background: #f1f5f9; color: #475569; border: 1px solid #e2e8f0;
}
.cfg-hint { margin: 0 0 14px; color: #64748b; font-size: 13px; line-height: 1.5; }
.cfg-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 4px 20px;
}
.cfg-grid :deep(.el-form-item) { margin-bottom: 14px; min-width: 0; }
.cfg-grid :deep(.el-form-item__content) { min-width: 0; }
.cfg-grid :deep(.el-input),
.cfg-grid :deep(.el-select),
.cfg-grid :deep(.el-input-number),
.cfg-grid :deep(.el-textarea) { width: 100%; }
.cfg-grid :deep(.el-form-item.full), .cfg-grid .full { grid-column: 1 / -1; }
.field-hint { margin-left: 4px;margin-top: 4px; font-size: 12px; color: #94a3b8; }
.stat-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.mini-stat {
  background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 14px 16px;
}
.mini-stat .label { color: #64748b; font-size: 13px; margin-bottom: 8px; }
.mini-stat .value { font-size: 24px; font-weight: 700; color: #0f172a; }
.mt16 { margin-top: 16px; }
.trend-chart { width: 100%; height: 180px; }
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
.log-filter { width: 100%; }
.log-line { word-break: break-all; }
.log-line .t { color: #64748b; margin-right: 6px; }
.log-line .lv-info { color: #38bdf8; }
.log-line .lv-debug { color: #94a3b8; }
.log-line .lv-warn { color: #facc15; }
.log-line .lv-error { color: #f87171; }
.empty-log { color: #64748b; }
@media (max-width: 900px) {
  .cfg-grid, .stat-row { grid-template-columns: 1fr; }
}
</style>
