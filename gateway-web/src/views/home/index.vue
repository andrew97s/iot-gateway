<template>
  <div class="home-container" v-loading="loading">
    <!-- 顶部硬件指标卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="metric-card">
          <div class="metric-inner">
            <div class="metric-icon cpu-bg"><el-icon :size="24"><Cpu /></el-icon></div>
            <div class="metric-body">
              <div class="metric-label">CPU 使用率</div>
              <div class="metric-val">{{ server.cpu?.used ?? '-' }}%</div>
              <el-progress :percentage="num(server.cpu?.used)" :stroke-width="6" :color="barColor(server.cpu?.used)" :show-text="false" class="metric-bar" />
              <div class="metric-sub">{{ server.cpu?.cpuNum }} 核心 · 空闲 {{ server.cpu?.free }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="metric-card">
          <div class="metric-inner">
            <div class="metric-icon mem-bg"><el-icon :size="24"><DataBoard /></el-icon></div>
            <div class="metric-body">
              <div class="metric-label">内存使用率</div>
              <div class="metric-val">{{ server.mem?.usage ?? '-' }}%</div>
              <el-progress :percentage="num(server.mem?.usage)" :stroke-width="6" :color="barColor(server.mem?.usage)" :show-text="false" class="metric-bar" />
              <div class="metric-sub">{{ server.mem?.used }}G / {{ server.mem?.total }}G</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="metric-card">
          <div class="metric-inner">
            <div class="metric-icon jvm-bg"><el-icon :size="24"><Monitor /></el-icon></div>
            <div class="metric-body">
              <div class="metric-label">JVM 内存</div>
              <div class="metric-val">{{ server.jvm?.usage ?? '-' }}%</div>
              <el-progress :percentage="num(server.jvm?.usage)" :stroke-width="6" :color="barColor(server.jvm?.usage)" :show-text="false" class="metric-bar" />
              <div class="metric-sub">{{ server.jvm?.used }}M / {{ server.jvm?.total }}M</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="metric-card">
          <div class="metric-inner">
            <div class="metric-icon disk-bg"><el-icon :size="24"><FolderOpened /></el-icon></div>
            <div class="metric-body">
              <div class="metric-label">磁盘使用率</div>
              <div class="metric-val">{{ mainDiskUsage }}%</div>
              <el-progress :percentage="num(mainDiskUsage)" :stroke-width="6" :color="barColor(mainDiskUsage)" :show-text="false" class="metric-bar" />
              <div class="metric-sub">{{ mainDisk?.used }} / {{ mainDisk?.total }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 设备 / 平台计数 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6">
        <el-card class="count-card">
          <div class="count-num" style="color: var(--el-color-success)">{{ deviceStats.onlineCount }}</div>
          <div class="count-label">在线设备</div>
          <div class="count-sub">共 {{ deviceStats.total }} 台</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="count-card">
          <div class="count-num" style="color: var(--el-color-danger)">{{ deviceStats.total - deviceStats.onlineCount }}</div>
          <div class="count-label">离线设备</div>
          <div class="count-sub">共 {{ deviceStats.total }} 台</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="count-card">
          <div class="count-num" style="color: var(--el-color-primary)">{{ runningCount }}</div>
          <div class="count-label">运行平台</div>
          <div class="count-sub">共 {{ platformStats.length }} 个</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="count-card">
          <div class="count-num" style="color: var(--el-color-warning)">{{ totalMsgCount }}</div>
          <div class="count-label">累计消息</div>
          <div class="count-sub" :style="totalErrCount > 0 ? 'color:var(--el-color-danger)' : ''">
            错误 {{ totalErrCount }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 消息接收推送趋势图 -->
    <el-row>
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-hd">
              <el-icon><DataLine /></el-icon>
              <span>消息接收推送趋势</span>
              <el-radio-group v-model="chartDays" size="small" @change="loadMsgChart">
                <el-radio-button :value="7">近7天</el-radio-button>
                <el-radio-button :value="14">近14天</el-radio-button>
                <el-radio-button :value="30">近30天</el-radio-button>
              </el-radio-group>
              <el-button size="small" text icon="Refresh" @click="loadAll" :loading="loading">刷新</el-button>
            </div>
          </template>
          <div v-loading="chartLoading">
            <div ref="chartRef" class="chart-box" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Home">
import * as echarts from 'echarts'
import { Cpu, DataBoard, Monitor, FolderOpened, DataLine } from '@element-plus/icons-vue'
import { getServer } from '@/api/monitor/server'
import { getAllPlatformStats } from '@/api/sys/platform'
import { getDeviceOnlineStats } from '@/api/sys/device'
import { listMessage } from '@/api/sys/message'

const loading      = ref(false)
const chartLoading = ref(false)
const chartDays    = ref(7)
const server       = ref({})
const platformStats = ref([])
const onlineStats  = ref([])
const chartRef     = ref(null)
let   chartInst    = null

const deviceStats   = computed(() => {
  let total = 0, online = 0
  onlineStats.value.forEach(s => { total += Number(s.total || 0); online += Number(s.onlineCount || 0) })
  return { total, onlineCount: online }
})
const runningCount  = computed(() => platformStats.value.filter(p => p.alive).length)
const totalMsgCount = computed(() => platformStats.value.reduce((s, p) => s + (p.msgCount || 0), 0))
const totalErrCount = computed(() => platformStats.value.reduce((s, p) => s + (p.errCount || 0), 0))
const mainDisk      = computed(() => {
  const f = server.value.sysFiles
  if (!f?.length) return null
  return f.reduce((a, b) => (Number(a.usage) > Number(b.usage) ? a : b))
})
const mainDiskUsage = computed(() => mainDisk.value?.usage ?? 0)

function num(v) { return Math.min(100, Math.max(0, Number(v) || 0)) }
function barColor(v) {
  const n = num(v)
  if (n >= 85) return '#f56c6c'
  if (n >= 65) return '#e6a23c'
  return '#67c23a'
}

// ==================== 图表 ====================
function buildDays(n) {
  const arr = []
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    arr.push(d.toISOString().slice(0, 10))
  }
  return arr
}

async function loadMsgChart() {
  if (!chartInst) return
  chartLoading.value = true
  const days = buildDays(chartDays.value)
  try {
    const res = await listMessage({
      pageNum: 1, pageSize: 3000,
      orderByColumn: 'id', isAsc: 'DESC',
      params: { beginCreateTime: days[0], endCreateTime: days[days.length - 1] }
    })
    const rows     = res.rows || []
    const recvMap  = {}
    const sentMap  = {}
    const failMap  = {}
    days.forEach(d => { recvMap[d] = 0; sentMap[d] = 0; failMap[d] = 0 })
    rows.forEach(msg => {
      const day = msg.createTime?.slice(0, 10)
      if (day && recvMap[day] !== undefined) {
        recvMap[day]++
        if (msg.sendStatus === 'sent')   sentMap[day]++
        if (msg.sendStatus === 'failed') failMap[day]++
      }
    })
    const labels = days.map(d => d.slice(5))
    chartInst.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
      legend: { data: ['接收消息', '推送成功', '推送失败'], bottom: 0 },
      grid: { left: '3%', right: '4%', top: '10%', bottom: '12%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: labels, axisTick: { show: false } },
      yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { type: 'dashed' } } },
      series: [
        {
          name: '接收消息', type: 'line', smooth: true, data: days.map(d => recvMap[d]),
          symbol: 'circle', symbolSize: 6,
          areaStyle: { color: 'rgba(64,158,255,0.12)' },
          lineStyle: { color: '#409eff' }, itemStyle: { color: '#409eff' }
        },
        {
          name: '推送成功', type: 'line', smooth: true, data: days.map(d => sentMap[d]),
          symbol: 'circle', symbolSize: 6,
          areaStyle: { color: 'rgba(103,194,58,0.12)' },
          lineStyle: { color: '#67c23a' }, itemStyle: { color: '#67c23a' }
        },
        {
          name: '推送失败', type: 'line', smooth: true, data: days.map(d => failMap[d]),
          symbol: 'circle', symbolSize: 6,
          areaStyle: { color: 'rgba(245,108,108,0.10)' },
          lineStyle: { color: '#f56c6c' }, itemStyle: { color: '#f56c6c' }
        }
      ]
    })
  } finally {
    chartLoading.value = false
  }
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
    const [srvRes, pfRes, devRes] = await Promise.all([
      getServer(),
      getAllPlatformStats(),
      getDeviceOnlineStats()
    ])
    server.value        = srvRes.data || {}
    platformStats.value = Array.isArray(pfRes.data)  ? pfRes.data  : []
    onlineStats.value   = Array.isArray(devRes.data) ? devRes.data : []
  } finally {
    loading.value = false
  }
  loadMsgChart()
}
</script>

<style scoped>
.home-container { padding: 4px 0; margin-left: 10px;margin-right: 10px; }
.stat-row { margin-bottom: 16px; }

/* 硬件指标卡片 */
.metric-card { height: 100%; }
.metric-inner { display: flex; align-items: flex-start; gap: 14px; }
.metric-icon { width: 52px; height: 52px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; color: #fff; }
.cpu-bg  { background: linear-gradient(135deg, #667eea, #764ba2); }
.mem-bg  { background: linear-gradient(135deg, #43e97b, #38f9d7); }
.jvm-bg  { background: linear-gradient(135deg, #fa709a, #fee140); }
.disk-bg { background: linear-gradient(135deg, #4facfe, #00f2fe); }
.metric-body  { flex: 1; min-width: 0; }
.metric-label { font-size: 12px; color: var(--el-text-color-secondary); margin-bottom: 2px; }
.metric-val   { font-size: 26px; font-weight: 700; line-height: 1.2; margin-bottom: 6px; }
.metric-bar   { margin-bottom: 4px; }
.metric-sub   { font-size: 12px; color: var(--el-text-color-secondary); }

/* 计数卡片 */
.count-card  { text-align: center; padding: 4px 0; }
.count-num   { font-size: 36px; font-weight: 800; line-height: 1.2; }
.count-label { font-size: 13px; color: var(--el-text-color-regular); margin-top: 4px; }
.count-sub   { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 2px; }

/* 趋势图 */
.chart-box { width: 100%; height: 340px; }
.card-hd { display: flex; align-items: center; gap: 10px; }
.card-hd span { flex: 1; font-weight: 600; }
</style>
