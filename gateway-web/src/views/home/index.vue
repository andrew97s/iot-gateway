<template>
  <div class="gw-page home-page" v-loading="loading">
    <!-- 顶部指标卡 -->
    <div class="gw-stat-grid">
      <div class="gw-stat">
        <div class="label">设备总数</div>
        <div class="value">{{ fmt(device.total) }} <small>台</small></div>
        <div class="sub">
          在线率 <span :style="{ color: onlineRateColor }">{{ onlineRate }}%</span> · 离线 {{ fmt(device.offline) }} 台
        </div>
      </div>
      <div class="gw-stat">
        <div class="label">今日接入消息</div>
        <div class="value">{{ fmt(today.total) }} <small>条</small></div>
        <div class="sub">推送成功 {{ fmt(today.sentCount) }} 条</div>
      </div>
      <div class="gw-stat">
        <div class="label">今日告警</div>
        <div class="value" style="color: #dc2626">{{ fmt(today.alarmCount) }} <small>条</small></div>
        <div class="sub">
          未处置 <b style="color: #d97706">{{ fmt(today.unhandledAlarmCount) }}</b> 条
        </div>
      </div>
      <div class="gw-stat">
        <div class="label">插件运行状态</div>
        <div class="value">
          {{ plugin.running || 0 }} <small>/ {{ plugin.total || 0 }} 运行中</small>
        </div>
        <div class="sub">
          已停止 {{ plugin.stopped || 0 }}
          <span style="color: #dc2626" v-if="abnormalCount > 0"> · 异常 {{ abnormalCount }}</span>
        </div>
      </div>
      <div class="gw-stat">
        <div class="label">今日推送成功率</div>
        <div class="value" :style="{ color: pushRateColor }">{{ pushSuccessRate }}%</div>
        <div class="sub">
          失败 <b :style="today.failedCount > 0 ? 'color:#dc2626' : ''">{{ fmt(today.failedCount) }}</b> 条 · 待同步
          {{ fmt(pendingCount) }} 条
        </div>
      </div>
    </div>

    <!-- 趋势 + 分布/资源 -->
    <el-row class="home-mid" :gutter="14">
      <el-col :lg="16" :xs="24">
        <div class="gw-card home-mid-card">
          <div class="gw-card-head">
            <h2>近 24 小时消息接入趋势</h2>
            <span class="refresh-box">
              <span class="gw-muted gw-small" v-if="lastUpdated">更新于 {{ lastUpdated }} · 每 {{ AUTO_REFRESH_SECONDS }} 秒自动刷新</span>
              <el-button icon="Refresh" :loading="loading" size="small" text @click="loadAll()">刷新</el-button>
            </span>
          </div>
          <div class="gw-card-body chart-body">
            <div class="chart-box" ref="chartRef" />
          </div>
        </div>
      </el-col>
      <el-col :lg="8" :xs="24">
        <div class="gw-card home-mid-card">
          <div class="gw-card-head"><h2>今日消息类型分布</h2></div>
          <div class="gw-card-body">
            <el-empty description="今日暂无消息" :image-size="60" v-if="typeDist.length === 0" />
            <div class="gw-hbar" v-else>
              <div v-for="t in typeDist" class="row" :key="t.type">
                <span>{{ typeLabel(t.type) }}</span>
                <div class="bar-bg">
                  <div class="bar" :style="{ width: t.percent + '%', background: typeColor(t.type) }" />
                </div>
                <span class="num">{{ fmt(t.total) }}</span>
              </div>
            </div>
            <div class="res-divider">
              <h2>系统资源</h2>
              <div class="gw-hbar">
                <div class="row">
                  <span>CPU</span>
                  <div class="bar-bg"><div class="bar" :style="barStyle(server.cpu?.used)" /></div>
                  <span class="num">{{ server.cpu?.used ?? '-' }}%</span>
                </div>
                <div class="row">
                  <span>内存</span>
                  <div class="bar-bg"><div class="bar" :style="barStyle(server.mem?.usage)" /></div>
                  <span class="num">{{ server.mem?.usage ?? '-' }}%</span>
                </div>
                <div class="row">
                  <span>JVM</span>
                  <div class="bar-bg"><div class="bar" :style="barStyle(server.jvm?.usage)" /></div>
                  <span class="num">{{ server.jvm?.usage ?? '-' }}%</span>
                </div>
                <div class="row">
                  <span>磁盘</span>
                  <div class="bar-bg"><div class="bar" :style="barStyle(mainDiskUsage)" /></div>
                  <span class="num">{{ mainDiskUsage }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 插件状态 + 推送统计/最新告警 -->
    <el-row class="home-bottom" :gutter="14">
      <el-col :lg="12" :xs="24">
        <div class="gw-card home-side-card">
          <div class="gw-card-head">
            <h2>插件运行状态</h2>
            <button class="gw-link-btn" type="button" @click="navTo('platform')">管理插件 →</button>
          </div>
          <div class="gw-card-body plugin-table-wrap">
            <el-table class="home-table" :data="plugin.list || []" empty-text="暂无运行中的插件" height="100%" :show-header="true" size="small">
              <el-table-column label="插件" min-width="130" prop="name" show-overflow-tooltip />
              <el-table-column align="center" label="协议" width="120">
                <template #default="scope">
                  <span class="gw-tag">{{ scope.row.protocol || '—' }}</span>
                </template>
              </el-table-column>
              <el-table-column align="center" label="状态" width="96">
                <template #default="scope">
                  <span class="gw-badge" :class="pluginBadge(scope.row)" :title="scope.row.healthReason || pluginLabel(scope.row)">
                    {{ pluginLabel(scope.row) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column align="center" label="接入设备" width="88">
                <template #default="scope">{{ fmt(scope.row.deviceCount) }}</template>
              </el-table-column>
              <el-table-column align="center" label="今日消息" width="96">
                <template #default="scope">{{ fmt(scope.row.todayMsgCount ?? scope.row.msgCount) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-col>
      <el-col :lg="12" :xs="24">
        <div class="gw-card home-side-card">
          <div class="gw-card-head">
            <h2>最新告警</h2>
            <button class="gw-link-btn" type="button" @click="navTo('message')">消息日志 →</button>
          </div>
          <div class="gw-card-body alarm-body">
            <el-empty description="暂无告警" :image-size="50" v-if="latestAlarms.length === 0" />
            <button v-for="alarm in latestAlarms" class="alarm-row" type="button" :key="alarm.id" @click="openAlarm(alarm)">
              <span class="gw-mono gw-muted gw-small">{{ shortTime(alarm.createTime) }}</span>
              <span class="gw-tag red">告警</span>
              <span class="alarm-text" :title="alarmTitle(alarm)">
                <span class="alarm-summary" v-if="alarm.summary">{{ alarm.summary }}</span>
                <span class="gw-mono" :class="{ 'gw-muted': !!alarm.summary }">{{ alarm.summary ? ' · ' : '' }}{{ alarm.deviceCode || '-' }}</span>
                <span class="gw-muted"> · {{ pluginName(alarm.pfCode) }}</span>
              </span>
              <span class="gw-badge" :class="alarm.sendStatus === 'sent' ? 'ok' : alarm.sendStatus === 'failed' ? 'err' : 'off'">
                {{ alarm.sendStatus === 'sent' ? '已推送' : alarm.sendStatus === 'failed' ? '推送失败' : '未推送' }}
              </span>
            </button>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Home">
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getOverview } from '@/api/sys/overview'
import { getServer } from '@/api/monitor/server'
import { msgTypeLabel as typeLabel, msgTypeColor as typeColor, pluginStateLabel, pluginStateBadge, fmtNumber as fmt } from '@/utils/gateway'

const AUTO_REFRESH_SECONDS = 30
const router = useRouter()

/** 菜单由后端动态下发，路径不固定。优先按菜单标题匹配，其次按路径分段匹配。 */
const NAV_HINTS = {
  platform: { titles: ['插件'], paths: ['platform', 'plugin'] },
  message: { titles: ['消息'], paths: ['message'] }
}
function findRoute(keyword) {
  const spec = NAV_HINTS[keyword] || { titles: [], paths: [keyword] }
  const ranked = router
    .getRoutes()
    .filter((r) => r.components?.default && r.path && !r.path.includes(':'))
    .map((r) => {
      const title = String(r.meta?.title || '')
      const segments = r.path.toLowerCase().split('/')
      let score = 0
      if (spec.titles.some((item) => title.includes(item))) score += 5
      if (spec.paths.some((item) => segments.includes(item))) score += 4
      return { route: r, score }
    })
    .filter((item) => item.score > 0)
    .sort((a, b) => b.score - a.score || a.route.path.length - b.route.path.length)
  return ranked[0]?.route
}
function navTo(keyword, query) {
  const target = findRoute(keyword)
  if (!target) {
    ElMessage.warning('当前账号没有对应菜单')
    return
  }
  router.push({ path: target.path, query: query || {} })
}
function openAlarm(alarm) {
  navTo('message', {
    keyword: alarm?.deviceCode || '',
    type: 'alarm'
  })
}

const loading = ref(false)
const device = ref({})
const today = ref({})
const plugin = ref({})
const typeDistRaw = ref([])
const trend = ref([])
const latestAlarms = ref([])
const server = ref({})
const lastUpdated = ref('')
const chartRef = ref(null)
let chartInst = null
let refreshTimer = null
let loadRequestId = 0
let pageActive = true

const onlineRate = computed(() => {
  const t = Number(device.value.total || 0)
  return t === 0 ? 0 : Math.round((Number(device.value.online || 0) / t) * 1000) / 10
})
const onlineRateColor = computed(() => (onlineRate.value >= 90 ? '#16a34a' : onlineRate.value >= 70 ? '#d97706' : '#dc2626'))
const pendingCount = computed(() => {
  const total = Number(today.value.total || 0)
  const sent = Number(today.value.sentCount || 0)
  const failed = Number(today.value.failedCount || 0)
  return Math.max(0, total - sent - failed)
})
const pushSuccessRate = computed(() => {
  const sent = Number(today.value.sentCount || 0)
  const failed = Number(today.value.failedCount || 0)
  const attempted = sent + failed
  if (attempted === 0) return 100
  return Math.round((sent / attempted) * 1000) / 10
})
const pushRateColor = computed(() => (pushSuccessRate.value >= 95 ? '#16a34a' : pushSuccessRate.value >= 80 ? '#d97706' : '#dc2626'))
const abnormalCount = computed(() => Number(plugin.value.abnormal || 0))
const typeDist = computed(() => {
  const rows = typeDistRaw.value || []
  const max = Math.max(1, ...rows.map((r) => Number(r.total || 0)))
  return rows
    .slice()
    .sort((a, b) => Number(b.total) - Number(a.total))
    .map((r) => ({ ...r, percent: Math.round((Number(r.total) / max) * 100) }))
})
const mainDiskUsage = computed(() => {
  const f = server.value.sysFiles
  if (!f?.length) return 0
  return f.reduce((a, b) => (Number(a.usage) > Number(b.usage) ? a : b)).usage
})

function shortTime(t) {
  if (!t) return '-'
  return String(t).slice(5, 16)
}
function barStyle(v) {
  const n = Math.min(100, Math.max(0, Number(v) || 0))
  const color = n >= 85 ? '#dc2626' : n >= 65 ? '#d97706' : '#16a34a'
  return { width: n + '%', background: color }
}
function pluginBadge(row) {
  return pluginStateBadge(row.state)
}
function pluginLabel(row) {
  return pluginStateLabel(row.state)
}
/** 告警只带插件编码，用插件列表换成名称 */
function pluginName(pfCode) {
  const hit = (plugin.value.list || []).find((p) => p.instanceId === pfCode || p.pluginId === pfCode)
  return hit?.name || pfCode || '-'
}
function alarmTitle(alarm) {
  return [alarm.summary, alarm.deviceCode, pluginName(alarm.pfCode), alarm.createTime].filter(Boolean).join(' · ')
}

// ==================== 趋势图 ====================
function buildHours() {
  const arr = []
  const now = new Date()
  for (let i = 23; i >= 0; i--) {
    const d = new Date(now.getTime() - i * 3600 * 1000)
    const pad = (n) => String(n).padStart(2, '0')
    arr.push(`${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:00`)
  }
  return arr
}

function renderChart() {
  if (!chartInst) return
  const hours = buildHours()
  const map = {}
  ;(trend.value || []).forEach((r) => {
    map[r.timePoint] = r
  })
  const totalData = hours.map((h) => Number(map[h]?.total || 0))
  const alarmData = hours.map((h) => Number(map[h]?.alarmCount || 0))
  const sentData = hours.map((h) => Number(map[h]?.sentCount || 0))
  const labels = hours.map((h) => h.slice(11))
  chartInst.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['接入消息', '推送成功', '告警'], bottom: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10 },
    grid: { left: '2%', right: '3%', top: '8%', bottom: '14%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#94a3b8', interval: 3 }
    },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { type: 'dashed', color: '#f1f5f9' } }, axisLabel: { color: '#94a3b8' } },
    series: [
      {
        name: '接入消息',
        type: 'line',
        smooth: true,
        data: totalData,
        symbol: 'none',
        areaStyle: { color: 'rgba(37, 99, 235, 0.10)' },
        lineStyle: { color: '#2563eb', width: 2.5 },
        itemStyle: { color: '#2563eb' }
      },
      {
        name: '推送成功',
        type: 'line',
        smooth: true,
        data: sentData,
        symbol: 'none',
        lineStyle: { color: '#16a34a', width: 1.5 },
        itemStyle: { color: '#16a34a' }
      },
      {
        name: '告警',
        type: 'line',
        smooth: true,
        data: alarmData,
        symbol: 'none',
        lineStyle: { color: '#dc2626', width: 1.5 },
        itemStyle: { color: '#dc2626' }
      }
    ]
  })
}

function onResize() {
  chartInst?.resize()
}

// ==================== 自动刷新 ====================
function stopAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}
function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer = setInterval(() => {
    // 浏览器标签页在后台时不刷新，切回来由 visibilitychange 补一次
    if (document.hidden || loading.value) return
    loadAll({ silent: true })
  }, AUTO_REFRESH_SECONDS * 1000)
}
function onVisibilityChange() {
  if (!document.hidden && pageActive && refreshTimer) loadAll({ silent: true })
}

onMounted(() => {
  chartInst = echarts.init(chartRef.value)
  window.addEventListener('resize', onResize)
  document.addEventListener('visibilitychange', onVisibilityChange)
  loadAll()
  nextTick(onResize)
  startAutoRefresh()
})
// 首页被 keep-alive 缓存：离开时停止轮询，回来时立即刷新一次并校正图表尺寸
let firstActivation = true
onActivated(() => {
  pageActive = true
  if (firstActivation) {
    firstActivation = false
    return
  }
  loadAll({ silent: true })
  startAutoRefresh()
  nextTick(onResize)
})
onDeactivated(() => {
  pageActive = false
  stopAutoRefresh()
})

onBeforeUnmount(() => {
  pageActive = false
  stopAutoRefresh()
  window.removeEventListener('resize', onResize)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  chartInst?.dispose()
  chartInst = null
})

// ==================== 数据加载 ====================
/** silent：自动刷新时不出现整页 loading，失败也不打断当前展示 */
async function loadAll({ silent = false } = {}) {
  const requestId = ++loadRequestId
  if (!silent) loading.value = true
  try {
    const [ovRes, srvRes] = await Promise.all([getOverview(), getServer().catch(() => null)])
    if (requestId !== loadRequestId) return
    const d = ovRes.data || {}
    device.value = d.device || {}
    today.value = d.today || {}
    plugin.value = d.plugin || {}
    typeDistRaw.value = d.typeDistribution || []
    trend.value = d.trend || []
    latestAlarms.value = d.latestAlarms || []
    // 服务器监控接口偶发失败时保留上一次的数值，不把资源条清零
    if (srvRes?.data) server.value = srvRes.data
    lastUpdated.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
    // 之前因失败暂停过轮询的话，成功一次后恢复
    if (pageActive && !refreshTimer) startAutoRefresh()
  } catch {
    // 错误提示由请求拦截器统一弹出，保留上一次的数据。
    // 自动刷新失败（后端重启、断网等）就先停掉轮询，避免每 30 秒弹一次错误；手动刷新成功后会恢复
    if (silent) stopAutoRefresh()
  } finally {
    if (requestId === loadRequestId) loading.value = false
  }
  renderChart()
  nextTick(onResize)
}
</script>

<style scoped>
.home-page {
  height: calc(100vh - 50px);
  max-height: calc(100vh - 50px);
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
  padding: 16px 20px 24px;
  gap: 12px;
}

:global(.hasTagsView) .home-page {
  height: calc(100vh - 84px);
  max-height: calc(100vh - 84px);
}

.home-page :deep(.home-mid),
.home-page :deep(.home-bottom) {
  flex: 1 1 0;
  min-height: 0;
  width: 100%;
}

.home-page :deep(.home-mid) {
  flex-grow: 1.05;
}

.home-page :deep(.home-bottom) {
  flex-grow: 0.9;
  max-height: 400px;
}

.home-page :deep(.home-mid > .el-col),
.home-page :deep(.home-bottom > .el-col) {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow-y: auto;
  max-height: 100%;
}

.home-mid-card,
.home-side-card {
  flex: 1;
  min-height: 0;
  height: auto;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.home-mid-card .gw-card-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.chart-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.chart-box {
  width: 100%;
  flex: 1;
  min-height: 0;
  height: auto;
}
.res-divider {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #e2e8f0;
}
.res-divider h2 {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 12px;
  color: #0f172a;
}
.home-side-card .plugin-table-wrap {
  flex: 1;
  min-height: 0;
  padding: 12px 14px 14px;
  box-sizing: border-box;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.home-side-card .plugin-table-wrap :deep(.el-table) {
  height: 100%;
}

.refresh-box {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
/* 原型 card-head 右侧按钮：边框小按钮 */
.gw-link-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  font-size: 12px;
  line-height: 1.2;
  color: #0f172a;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}
.gw-link-btn:hover {
  border-color: #94a3b8;
  background: #f8fafc;
}
.home-table :deep(th.el-table__cell) {
  background: #f8fafc !important;
  color: #64748b;
  font-weight: 600;
  font-size: 13px;
}
.home-table :deep(td.el-table__cell) {
  font-size: 13px;
  padding: 7px 0;
}
.alarm-summary {
  color: #0f172a;
}
.home-side-card .alarm-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 8px 14px 12px;
  box-sizing: border-box;
}
.alarm-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  margin: 0;
  padding: 8px 6px;
  border: none;
  border-bottom: 1px solid #f1f5f9;
  border-radius: 6px;
  background: transparent;
  font: inherit;
  font-size: 13px;
  color: inherit;
  text-align: left;
  cursor: pointer;
}
.alarm-row:hover {
  background: #f8fafc;
}
.alarm-row:last-child {
  border-bottom: none;
}
.alarm-text {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

@media (max-width: 1199px) {
  .home-page {
    height: auto;
    max-height: none;
    overflow: visible;
    min-height: calc(100vh - 50px);
  }

  .home-page :deep(.home-mid),
  .home-page :deep(.home-bottom) {
    flex: none;
  }

  .chart-box {
    height: 280px;
    flex: none;
  }

  .home-side-card .plugin-table-wrap {
    flex: none;
    height: 320px;
  }
}
</style>
