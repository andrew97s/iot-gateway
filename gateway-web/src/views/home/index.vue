<template>
  <div class="gw-page" v-loading="loading">
    <!-- 顶部指标卡 -->
    <div class="gw-stat-grid">
      <div class="gw-stat">
        <div class="label">设备总数</div>
        <div class="value">{{ fmt(device.total) }} <small>台</small></div>
        <div class="sub">
          在线率 <span :style="{ color: onlineRateColor }">{{ onlineRate }}%</span>
          · 离线 {{ fmt(device.offline) }} 台
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
        <div class="sub">未处置 <b style="color: #d97706">{{ fmt(today.unhandledAlarmCount) }}</b> 条</div>
      </div>
      <div class="gw-stat">
        <div class="label">插件运行状态</div>
        <div class="value">{{ plugin.running || 0 }} <small>/ {{ plugin.enabled || 0 }} 运行中</small></div>
        <div class="sub">
          共 {{ plugin.total || 0 }} 个插件
          <span v-if="abnormalCount > 0" style="color: #dc2626"> · 异常 {{ abnormalCount }}</span>
        </div>
      </div>
      <div class="gw-stat">
        <div class="label">今日推送成功率</div>
        <div class="value" :style="{ color: pushRateColor }">{{ pushSuccessRate }}%</div>
        <div class="sub">
          失败 <b :style="today.failedCount > 0 ? 'color:#dc2626' : ''">{{ fmt(today.failedCount) }}</b> 条
        </div>
      </div>
    </div>

    <!-- 趋势 + 分布/资源 -->
    <el-row :gutter="14">
      <el-col :xs="24" :lg="16">
        <div class="gw-card" style="height: 100%">
          <div class="gw-card-head">
            <h2>近 24 小时消息接入趋势</h2>
            <el-button size="small" text icon="Refresh" @click="loadAll" :loading="loading">刷新</el-button>
          </div>
          <div class="gw-card-body">
            <div ref="chartRef" class="chart-box" />
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="8">
        <div class="gw-card" style="height: 100%">
          <div class="gw-card-head"><h2>今日消息类型分布</h2></div>
          <div class="gw-card-body">
            <el-empty v-if="typeDist.length === 0" description="今日暂无消息" :image-size="60" />
            <div class="gw-hbar" v-else>
              <div class="row" v-for="t in typeDist" :key="t.type">
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
    <el-row :gutter="14">
      <el-col :xs="24" :lg="12">
        <div class="gw-card home-side-card">
          <div class="gw-card-head">
            <h2>插件运行状态</h2>
            <button type="button" class="gw-link-btn" @click="navTo('platform')">管理插件 →</button>
          </div>
          <div class="gw-card-body no-pad">
            <el-table :data="plugin.list || []" size="small" :show-header="true" class="home-table" max-height="380">
              <el-table-column label="插件" prop="name" min-width="130" show-overflow-tooltip />
              <el-table-column label="协议" width="120" align="center">
                <template #default="scope">
                  <span class="gw-tag">{{ scope.row.protocol || '—' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="96" align="center">
                <template #default="scope">
                  <span class="gw-badge" :class="pluginBadge(scope.row)">{{ pluginLabel(scope.row) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="接入设备" width="88" align="center">
                <template #default="scope">{{ fmt(scope.row.deviceCount) }}</template>
              </el-table-column>
              <el-table-column label="今日消息" width="96" align="center">
                <template #default="scope">{{ fmt(scope.row.todayMsgCount ?? scope.row.msgCount) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="gw-card home-side-card">
          <div class="gw-card-head">
            <h2>消息推送统计（按来源平台）</h2>
            <button type="button" class="gw-link-btn" @click="navTo('message')">消息日志 →</button>
          </div>
          <div class="gw-card-body no-pad">
            <el-table :data="messageByPlatform" size="small" class="home-table" max-height="200">
              <el-table-column label="来源平台" min-width="120" show-overflow-tooltip>
                <template #default="scope">
                  <span class="pf-name">{{ platformDisplay(scope.row) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="今日消息" width="100" align="center">
                <template #default="scope">{{ fmt(scope.row.total) }}</template>
              </el-table-column>
              <el-table-column label="推送成功" width="110" align="center">
                <template #default="scope">
                  <span class="ok-num">{{ fmt(scope.row.sentCount) }}</span>
                  <span class="rate-muted" v-if="Number(scope.row.total) > 0">
                    ({{ pushRate(scope.row) }}%)
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="推送失败" width="100" align="center">
                <template #default="scope">
                  <span :class="Number(scope.row.failedCount) > 0 ? 'err-num' : 'muted-num'">{{ fmt(scope.row.failedCount) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div class="gw-card-head alarm-head">
            <h2>最新告警</h2>
          </div>
          <div class="gw-card-body alarm-body">
            <el-empty v-if="latestAlarms.length === 0" description="暂无告警" :image-size="50" />
            <div v-for="alarm in latestAlarms" :key="alarm.id" class="alarm-row">
              <span class="gw-mono gw-muted gw-small">{{ shortTime(alarm.createTime) }}</span>
              <span class="gw-tag red">告警</span>
              <span class="alarm-text">
                <span class="gw-mono">{{ alarm.deviceCode || '-' }}</span>
                <span class="gw-muted"> · {{ alarm.pfCode }}</span>
              </span>
              <span class="gw-badge" :class="alarm.sendStatus === 'sent' ? 'ok' : (alarm.sendStatus === 'failed' ? 'err' : 'off')">
                {{ alarm.sendStatus === 'sent' ? '已推送' : (alarm.sendStatus === 'failed' ? '推送失败' : '未推送') }}
              </span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Home">
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'
import { getOverview } from '@/api/sys/overview'
import { getServer } from '@/api/monitor/server'

const router = useRouter()

/** 按关键字在已注册路由中定位目标页（菜单由后端动态下发，路径不固定） */
function navTo(keyword) {
  const target = router.getRoutes().find(r => r.path.toLowerCase().includes(keyword) && r.components)
  if (target) {
    router.push(target.path)
  }
}

const loading = ref(false)
const device = ref({})
const today = ref({})
const plugin = ref({})
const typeDistRaw = ref([])
const trend = ref([])
const messageByPlatform = ref([])
const latestAlarms = ref([])
const server = ref({})
const chartRef = ref(null)
let chartInst = null

const TYPE_META = {
  alarm:     { label: '告警事件', color: '#dc2626' },
  business:  { label: '监测数据', color: '#2563eb' },
  device:    { label: '设备事件', color: '#d97706' },
  control:   { label: '反控指令', color: '#7c3aed' },
  event:     { label: '事件',     color: '#0891b2' },
  heartbeat: { label: '心跳',     color: '#64748b' }
}
function typeLabel(t) { return TYPE_META[t]?.label || t || '未知' }
function typeColor(t) { return TYPE_META[t]?.color || '#94a3b8' }

const onlineRate = computed(() => {
  const t = Number(device.value.total || 0)
  return t === 0 ? 0 : Math.round((Number(device.value.online || 0) / t) * 1000) / 10
})
const onlineRateColor = computed(() => (onlineRate.value >= 90 ? '#16a34a' : onlineRate.value >= 70 ? '#d97706' : '#dc2626'))
const pushSuccessRate = computed(() => {
  const total = Number(today.value.total || 0)
  if (total === 0) return 100
  return Math.round((Number(today.value.sentCount || 0) / total) * 1000) / 10
})
const pushRateColor = computed(() => (pushSuccessRate.value >= 95 ? '#16a34a' : pushSuccessRate.value >= 80 ? '#d97706' : '#dc2626'))
const abnormalCount = computed(() =>
  (plugin.value.list || []).filter(p => p.status === '1' && !p.alive).length
)
const typeDist = computed(() => {
  const rows = typeDistRaw.value || []
  const max = Math.max(1, ...rows.map(r => Number(r.total || 0)))
  return rows
    .slice()
    .sort((a, b) => Number(b.total) - Number(a.total))
    .map(r => ({ ...r, percent: Math.round((Number(r.total) / max) * 100) }))
})
const mainDiskUsage = computed(() => {
  const f = server.value.sysFiles
  if (!f?.length) return 0
  return f.reduce((a, b) => (Number(a.usage) > Number(b.usage) ? a : b)).usage
})

function fmt(v) {
  const n = Number(v || 0)
  return n.toLocaleString('zh-CN')
}
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
  if (row.status !== '1') return 'off'
  return row.alive ? 'ok' : 'err'
}
function pluginLabel(row) {
  if (row.status !== '1') return '已停止'
  return row.alive ? '运行中' : '异常'
}
function platformDisplay(row) {
  return row.pfName || row.name || row.pfCode || '—'
}
function pushRate(row) {
  const total = Number(row.total || 0)
  if (!total) return 100
  return Math.round((Number(row.sentCount || 0) / total) * 1000) / 10
}

// ==================== 趋势图 ====================
function buildHours() {
  const arr = []
  const now = new Date()
  for (let i = 23; i >= 0; i--) {
    const d = new Date(now.getTime() - i * 3600 * 1000)
    const pad = n => String(n).padStart(2, '0')
    arr.push(`${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:00`)
  }
  return arr
}

function renderChart() {
  if (!chartInst) return
  const hours = buildHours()
  const map = {}
  ;(trend.value || []).forEach(r => { map[r.timePoint] = r })
  const totalData = hours.map(h => Number(map[h]?.total || 0))
  const alarmData = hours.map(h => Number(map[h]?.alarmCount || 0))
  const sentData = hours.map(h => Number(map[h]?.sentCount || 0))
  const labels = hours.map(h => h.slice(11))
  chartInst.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['接入消息', '推送成功', '告警'], bottom: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10 },
    grid: { left: '2%', right: '3%', top: '8%', bottom: '14%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: labels, axisTick: { show: false }, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#94a3b8', interval: 3 } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { type: 'dashed', color: '#f1f5f9' } }, axisLabel: { color: '#94a3b8' } },
    series: [
      {
        name: '接入消息', type: 'line', smooth: true, data: totalData, symbol: 'none',
        areaStyle: { color: 'rgba(37, 99, 235, 0.10)' },
        lineStyle: { color: '#2563eb', width: 2.5 }, itemStyle: { color: '#2563eb' }
      },
      {
        name: '推送成功', type: 'line', smooth: true, data: sentData, symbol: 'none',
        lineStyle: { color: '#16a34a', width: 1.5 }, itemStyle: { color: '#16a34a' }
      },
      {
        name: '告警', type: 'line', smooth: true, data: alarmData, symbol: 'none',
        lineStyle: { color: '#dc2626', width: 1.5 }, itemStyle: { color: '#dc2626' }
      }
    ]
  })
}

function onResize() { chartInst?.resize() }

onMounted(() => {
  chartInst = echarts.init(chartRef.value)
  window.addEventListener('resize', onResize)
  loadAll()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chartInst?.dispose()
  chartInst = null
})

// ==================== 数据加载 ====================
async function loadAll() {
  loading.value = true
  try {
    const [ovRes, srvRes] = await Promise.all([
      getOverview(),
      getServer().catch(() => ({ data: {} }))
    ])
    const d = ovRes.data || {}
    device.value = d.device || {}
    today.value = d.today || {}
    plugin.value = d.plugin || {}
    typeDistRaw.value = d.typeDistribution || []
    trend.value = d.trend || []
    messageByPlatform.value = d.messageByPlatform || []
    latestAlarms.value = d.latestAlarms || []
    server.value = srvRes.data || {}
  } finally {
    loading.value = false
  }
  renderChart()
}
</script>

<style scoped>
.chart-box { width: 100%; height: 330px; }
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
.home-side-card { height: 100%; }
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
  transition: all .15s;
}
.gw-link-btn:hover {
  border-color: #94a3b8;
  background: #f8fafc;
}
.gw-tag {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 5px;
  font-size: 12px;
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #e2e8f0;
  white-space: nowrap;
}
.gw-tag.red {
  background: #fef2f2;
  color: #dc2626;
  border-color: #fecaca;
}
.home-table :deep(th.el-table__cell) {
  background: #f8fafc !important;
  color: #64748b;
  font-weight: 600;
  font-size: 13px;
}
.home-table :deep(td.el-table__cell) {
  font-size: 13px;
  padding: 10px 0;
}
.pf-name { color: #0f172a; }
.ok-num { color: #16a34a; font-weight: 600; }
.err-num { color: #dc2626; font-weight: 600; }
.muted-num { color: #64748b; }
.rate-muted {
  margin-left: 4px;
  color: #94a3b8;
  font-size: 12px;
}
.alarm-head {
  border-top: 1px solid #e2e8f0;
}
.alarm-body { padding-top: 8px; }
.alarm-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
}
.alarm-row:last-child { border-bottom: none; }
.alarm-text { flex: 1; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
</style>
