<template>
  <div class="app-container log-page realtime-page">
    <div class="log-switch">
      <router-link class="tab" to="/ops/history">历史日志</router-link>
      <router-link class="tab active" to="/ops/realtime">实时日志</router-link>
    </div>

    <div class="log-toolbar">
      <span class="label">过滤</span>
      <el-input v-model="keyword" clearable placeholder="请输入过滤关键字" style="width: 280px" />
      <span class="status" :class="connected ? 'on' : 'off'">{{ statusText }}</span>
      <div class="spacer" />
      <el-button icon="Download" @click="downloadCurrent">下载</el-button>
    </div>

    <div class="console" ref="consoleRef" @scroll="onScroll">
      <div class="viewer-empty" v-if="!visibleLines.length">暂无日志</div>
      <div v-for="line in visibleLines" :class="lineClass(line.text)" :key="line.id">{{ line.text }}</div>
    </div>
  </div>
</template>

<script setup name="RealtimeLog">
import { getToken } from '@/utils/auth'
import { currentLogFile, downloadLogFile } from '@/api/monitor/logfile'
import { ElMessage } from 'element-plus'

const keyword = ref('')
const lines = ref([])
const connected = ref(false)
const statusText = ref('连接中')
const consoleRef = ref(null)
const stickBottom = ref(true)
let socket = null
let reconnectTimer = null
let closedByUser = false
let seq = 0
const MAX_LINES = 3000

const visibleLines = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) return lines.value
  return lines.value.filter((line) => line.text.toLowerCase().includes(text))
})

function lineClass(text) {
  if (/\bERROR\b/.test(text)) return 'log-line is-error'
  if (/\bWARN\b/.test(text)) return 'log-line is-warn'
  if (/\bDEBUG\b/.test(text)) return 'log-line is-debug'
  return 'log-line'
}

function pushLine(text) {
  if (!text) return
  lines.value.push({ id: ++seq, text })
  if (lines.value.length > MAX_LINES) {
    lines.value.splice(0, lines.value.length - MAX_LINES)
  }
  scrollIfNeeded()
}

function scrollIfNeeded() {
  if (!stickBottom.value) return
  nextTick(() => {
    const el = consoleRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function onScroll() {
  const el = consoleRef.value
  if (!el) return
  stickBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight < 40
}

function socketUrl() {
  const token = encodeURIComponent(getToken() || '')
  if (import.meta.env.DEV && import.meta.env.VITE_APP_PROXY_API) {
    const base = String(import.meta.env.VITE_APP_PROXY_API)
      .replace(/^http/i, 'ws')
      .replace(/\/$/, '')
    return `${base}/ws/log?token=${token}`
  }
  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${proto}//${location.host}/ws/log?token=${token}`
}

function connect() {
  if (closedByUser) return
  statusText.value = '连接中'
  try {
    socket = new WebSocket(socketUrl())
  } catch (e) {
    scheduleReconnect()
    return
  }
  socket.onopen = () => {
    connected.value = true
    statusText.value = '已连接'
  }
  socket.onmessage = (event) => {
    pushLine(typeof event.data === 'string' ? event.data : '')
  }
  socket.onerror = () => {
    connected.value = false
    statusText.value = '连接异常'
  }
  socket.onclose = () => {
    connected.value = false
    if (!closedByUser) {
      statusText.value = '连接已断开，正在重连'
      scheduleReconnect()
    }
  }
}

function scheduleReconnect() {
  if (reconnectTimer || closedByUser) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    connect()
  }, 3000)
}

function downloadCurrent() {
  currentLogFile().then((res) => {
    const file = res.data
    if (!file || !file.fileName) {
      ElMessage.warning('当前还没有日志文件')
      return
    }
    downloadLogFile(file.fileName)
  })
}

onMounted(connect)

onBeforeUnmount(() => {
  closedByUser = true
  if (reconnectTimer) clearTimeout(reconnectTimer)
  if (socket) socket.close()
})
</script>

<style lang="scss" scoped>
.realtime-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 124px);
  min-height: 420px;
}

.log-switch {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;

  .tab {
    padding: 4px 14px;
    border-radius: 4px;
    color: #606266;
    background: #fff;
    border: 1px solid #dcdfe6;
    text-decoration: none;
    line-height: 22px;
  }

  .active {
    color: #fff;
    background: #67c23a;
    border-color: #67c23a;
  }
}

.log-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;

  .label {
    color: #606266;
  }

  .spacer {
    flex: 1;
  }

  .status {
    font-size: 13px;
  }

  .on {
    color: #67c23a;
  }

  .off {
    color: #e6a23c;
  }
}

.console {
  flex: 1;
  overflow: auto;
  background: #111;
  color: #d5e8c8;
  border-radius: 4px;
  padding: 12px 14px;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.55;
}

.log-line {
  white-space: pre-wrap;
  word-break: break-all;
}

.is-error {
  color: #ff8a80;
}

.is-warn {
  color: #ffe082;
}

.is-debug {
  color: #90caf9;
}

.viewer-empty {
  color: #8aa18a;
}
</style>
