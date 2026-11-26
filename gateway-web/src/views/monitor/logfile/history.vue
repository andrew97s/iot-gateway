<template>
  <div class="app-container log-page">
    <div class="log-toolbar">
      <span class="label">搜索</span>
      <el-input v-model="keyword" clearable placeholder="关键字" style="width: 220px" @clear="search" @keyup.enter="search" />
      <span class="label">开始时间</span>
      <el-date-picker v-model="beginTime" placeholder="选择日期时间" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" @change="search" />
      <span class="label">结束时间</span>
      <el-date-picker v-model="endTime" placeholder="选择日期时间" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" @change="search" />
      <el-button type="primary" @click="search">搜索</el-button>
      <el-button :disabled="!selected.length" type="primary" @click="downloadSelected">下载所选</el-button>
    </div>

    <el-table :data="files" v-loading="loading" @selection-change="onSelect">
      <el-table-column type="selection" width="48" />
      <el-table-column label="文件名" min-width="200" prop="fileName" show-overflow-tooltip />
      <el-table-column label="文件大小" prop="sizeText" width="300" />
      <el-table-column label="开始时间" prop="startTime" width="300" />
      <el-table-column label="结束时间" prop="endTime" width="300" />
      <el-table-column align="center" label="操作" width="160">
        <template #default="scope">
          <el-button icon="View" link type="primary" @click="openFile(scope.row)">查看</el-button>
          <el-button icon="Download" link type="primary" @click="downloadLogFile(scope.row.fileName)">下载</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNum"
      :total="total"
      @pagination="load"
    />

    <el-dialog v-model="viewer.open" append-to-body :title="viewer.fileName || '日志内容'" top="4vh" width="86%">
      <div class="viewer-bar">
        <el-input v-model="viewer.keyword" clearable placeholder="在文件中过滤关键字" style="width: 280px" @keyup.enter="loadContent" />
        <el-button type="primary" @click="loadContent">过滤</el-button>
        <span class="hint" v-if="viewer.truncated">文件较大，仅显示匹配内容的末尾 {{ viewer.lineCount }} 行</span>
        <span class="hint" v-else>共 {{ viewer.lineCount }} 行</span>
      </div>
      <div class="viewer-body" v-loading="viewer.loading">
        <div class="viewer-empty" v-if="!viewer.lines.length">没有匹配的日志</div>
        <div v-for="(line, index) in viewer.lines" :class="lineClass(line)" :key="index">{{ line }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup name="HistoryLog">
import { downloadLogFile, downloadLogFiles, listLogFiles, readLogFile } from '@/api/monitor/logfile'

const keyword = ref('')
const beginTime = ref('')
const endTime = ref('')
const loading = ref(false)
const files = ref([])
const total = ref(0)
const selected = ref([])
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10
})
const viewer = reactive({
  open: false,
  loading: false,
  fileName: '',
  keyword: '',
  lines: [],
  truncated: false,
  lineCount: 0
})

function search() {
  queryParams.pageNum = 1
  load()
}

function load() {
  loading.value = true
  listLogFiles({
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
    keyword: keyword.value,
    beginTime: beginTime.value,
    endTime: endTime.value
  })
      .then((res) => {
        if (Array.isArray(res.rows)) {
          files.value = res.rows
          total.value = res.total || 0
          return
        }
        const all = Array.isArray(res.data) ? res.data : []
        total.value = all.length
        const start = (queryParams.pageNum - 1) * queryParams.pageSize
        files.value = all.slice(start, start + queryParams.pageSize)
      })
      .finally(() => {
        loading.value = false
      })
}

function onSelect(rows) {
  selected.value = rows || []
}

function openFile(row) {
  viewer.open = true
  viewer.fileName = row.fileName
  viewer.keyword = keyword.value || ''
  loadContent()
}

function loadContent() {
  if (!viewer.fileName) return
  viewer.loading = true
  readLogFile({ fileName: viewer.fileName, keyword: viewer.keyword })
      .then((res) => {
        const data = res.data || {}
        viewer.truncated = !!data.truncated
        viewer.lineCount = data.lineCount || 0
        viewer.lines = data.content ? data.content.split(/\r?\n/) : []
      })
      .finally(() => {
        viewer.loading = false
      })
}

function downloadSelected() {
  downloadLogFiles(selected.value.map((row) => row.fileName))
}

function lineClass(line) {
  if (/\bERROR\b/.test(line)) return 'log-line is-error'
  if (/\bWARN\b/.test(line)) return 'log-line is-warn'
  return 'log-line'
}

load()
</script>

<style lang="scss" scoped>
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
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;

  .label {
    color: #606266;
  }
}

.viewer-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;

  .hint {
    color: #909399;
    font-size: 13px;
  }
}

.viewer-body {
  height: 68vh;
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

.viewer-empty {
  color: #8aa18a;
}
</style>