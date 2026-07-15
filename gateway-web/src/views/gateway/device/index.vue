<template>
  <div class="device-container">
    <!-- 左侧：平台选择侧边栏 -->
    <div class="platform-sidebar">
      <div class="sidebar-header">
        <span class="sidebar-title">所属平台</span>
        <el-button size="small" circle icon="Refresh" @click="refreshAll" :loading="statsLoading" />
      </div>
      <!-- 全部 -->
      <div
        class="platform-item"
        :class="{ active: selectedPfCode === null }"
        @click="selectPlatform(null)"
      >
        <div class="pf-info">
          <span class="pf-name">全部设备</span>
        </div>
        <div class="pf-counts">
          <el-badge :value="totalStats.total || 0" type="primary" class="badge-total" />
          <el-badge :value="totalStats.onlineCount || 0" type="success" :hidden="!totalStats.onlineCount" />
        </div>
      </div>
      <!-- 各平台 -->
      <div
        v-for="pf in platformOptions"
        :key="pf.code"
        class="platform-item"
        :class="{ active: selectedPfCode === pf.code }"
        @click="selectPlatform(pf.code)"
      >
        <div class="pf-info">
          <span class="online-dot" :class="onlineStatsMap[pf.code]?.onlineCount > 0 ? 'dot-online' : 'dot-offline'" />
          <span class="pf-name">{{ pf.name }}</span>
        </div>
        <div class="pf-counts">
          <el-tooltip :content="`总数 ${onlineStatsMap[pf.code]?.total || 0} / 在线 ${onlineStatsMap[pf.code]?.onlineCount || 0}`" placement="right">
            <span class="count-text">
              <span class="count-online">{{ onlineStatsMap[pf.code]?.onlineCount || 0 }}</span>
              <span class="count-sep">/</span>
              <span class="count-total">{{ onlineStatsMap[pf.code]?.total || 0 }}</span>
            </span>
          </el-tooltip>
        </div>
      </div>
    </div>

    <!-- 右侧：设备内容区 -->
    <div class="device-main">
      <!-- 工具栏 -->
      <div class="device-toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="searchKeyword"
            clearable
            :placeholder="exactMatch ? '精准匹配设备编码 / 位置' : '模糊搜索设备编码 / 位置'"
            style="width: 240px"
            @input="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-tooltip content="精准匹配：完全相等；关闭后模糊查询" placement="top">
            <el-switch
              v-model="exactMatch"
              active-text="精准"
              inactive-text="模糊"
              inline-prompt
              style="--el-switch-on-color: #409eff"
              @change="handleSearch"
            />
          </el-tooltip>
          <el-select v-model="filterOnline" clearable placeholder="在线状态" style="width: 130px" @change="handleSearch">
            <el-option label="在线" value="1" />
            <el-option label="离线" value="0" />
          </el-select>
          <el-select v-model="viewMode" style="width: 100px">
            <el-option label="卡片视图" value="card" />
            <el-option label="列表视图" value="table" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button icon="Refresh" plain type="primary" @click="handleSyncDevice">同步设备</el-button>
          <el-button icon="Upload" plain type="info" @click="importTemplate?.handleImport">导入</el-button>
          <el-button icon="Download" plain type="warning" @click="handleExport">导出</el-button>
          <el-button icon="WarnTriangleFilled" plain type="danger" @click="handleClear">清空</el-button>
        </div>
      </div>

      <!-- 设备统计 -->
      <div class="device-summary" v-if="selectedPfCode">
        <el-text size="small">
          <span class="summary-name">{{ currentPlatformName }}</span>
          共 <el-text type="primary">{{ total }}</el-text> 台设备，
          在线 <el-text type="success">{{ currentOnlineCount }}</el-text> 台，
          离线 <el-text type="danger">{{ total - currentOnlineCount }}</el-text> 台
        </el-text>
      </div>

      <!-- 卡片视图 -->
      <div v-if="viewMode === 'card'" class="device-cards" v-loading="loading">
        <el-empty v-if="!loading && deviceList.length === 0" description="暂无设备数据" :image-size="100" />
        <div v-for="dev in deviceList" :key="dev.id" class="device-card-wrap">
          <el-card shadow="hover" class="device-card" :class="dev.online === '1' ? 'card-online' : 'card-offline'">
            <div class="dc-header">
              <div class="dc-status-dot" :class="dev.online === '1' ? 'dot-on' : 'dot-off'" />
              <span class="dc-code">{{ dev.code }}</span>
              <el-tag size="small" type="info" class="dc-pf-tag">{{ dev.pfCode }}</el-tag>
            </div>
            <div class="dc-name" :title="dev.name">{{ dev.name || '-' }}</div>
            <div class="dc-meta">
              <span><el-icon><Cpu /></el-icon> {{ dev.type || '-' }}</span>
              <span v-if="dev.ip"><el-icon><Connection /></el-icon> {{ dev.ip }}</span>
            </div>
            <div class="dc-last-comm" v-if="dev.updateTime">
              <el-icon><Timer /></el-icon>
              <span>上次通信：{{ formatDeviceTime(dev.updateTime) }}</span>
            </div>
            <div class="dc-model" v-if="dev.model">型号: {{ dev.model }}</div>
            <div class="dc-footer">
              <el-tag :type="dev.online === '1' ? 'success' : 'danger'" size="small">
                {{ dev.online === '1' ? '在线' : '离线' }}
              </el-tag>
              <div class="dc-actions">
                <el-tooltip content="查看详情"><el-button size="small" link type="primary" :icon="View" @click="handleView(dev)" /></el-tooltip>
                <el-tooltip content="编辑信息"><el-button size="small" link type="warning" :icon="Edit" @click="handleEdit(dev)" /></el-tooltip>
                <el-tooltip content="删除设备">
                  <el-button size="small" link type="danger" :icon="Delete" v-hasPermi="['sys:device:remove']" @click="handleDeleteDevice(dev)" />
                </el-tooltip>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 列表视图 -->
      <el-table v-if="viewMode === 'table'" :data="deviceList" v-loading="loading" style="width: 100%">
        <el-table-column label="在线" width="60" align="center">
          <template #default="scope">
            <span class="status-dot-sm" :class="scope.row.online === '1' ? 'dot-on' : 'dot-off'" />
          </template>
        </el-table-column>
        <el-table-column label="平台" prop="pfCode" width="100" align="center" />
        <el-table-column label="设备编码" prop="code" min-width="150" />
        <el-table-column label="设备位置" prop="name" min-width="150" show-overflow-tooltip />
        <el-table-column label="类别" prop="type" width="100" align="center" />
        <el-table-column label="型号" prop="model" width="120" align="center" show-overflow-tooltip />
        <el-table-column label="IP" prop="ip" width="130" align="center" />
        <el-table-column label="无线" prop="wireless" width="70" align="center">
          <template #default="scope">
            <dict-tag :options="sys_boolean" :value="scope.row.wireless" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.online === '1' ? 'success' : 'danger'" size="small">
              {{ scope.row.online === '1' ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上次通信" prop="updateTime" width="170" align="center">
          <template #default="scope">
            <span v-if="scope.row.updateTime">{{ formatDeviceTime(scope.row.updateTime) }}</span>
            <span v-else class="text-secondary">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center">
          <template #default="scope">
            <el-tooltip content="查看详情" placement="top">
              <el-button icon="View" link type="primary" @click="handleView(scope.row)" />
            </el-tooltip>
            <el-tooltip content="编辑信息" placement="top">
              <el-button icon="Edit" link type="warning" @click="handleEdit(scope.row)" />
            </el-tooltip>
            <el-tooltip content="删除设备" placement="top">
              <el-button icon="Delete" link type="danger" v-hasPermi="['sys:device:remove']" @click="handleDeleteDevice(scope.row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-model:limit="queryParams.pageSize"
        v-model:page="queryParams.pageNum"
        :total="total"
        @pagination="getList"
        v-show="total > 0"
        class="pagination-bar"
      />
    </div>

    <!-- 设备详情对话框 -->
    <el-dialog v-model="open" append-to-body :title="title" width="720px">
      <div class="detail-dialog-body">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="设备编码" :span="2">{{ form.code }}</el-descriptions-item>
          <el-descriptions-item label="设备位置" :span="2">{{ form.name }}</el-descriptions-item>
          <el-descriptions-item label="所属平台">{{ form.pfCode }}</el-descriptions-item>
          <el-descriptions-item label="网关代码">{{ form.net }}</el-descriptions-item>
          <el-descriptions-item label="设备类别">{{ form.type }}</el-descriptions-item>
          <el-descriptions-item label="设备型号">{{ form.model }}</el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ form.ip || '-' }}</el-descriptions-item>
          <el-descriptions-item label="在线状态">
            <el-tag :type="form.online === '1' ? 'success' : 'danger'" size="small">
              {{ form.online === '1' ? '在线' : '离线' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="无线设备">
            <dict-tag :options="sys_boolean" :value="form.wireless" />
          </el-descriptions-item>
          <el-descriptions-item label="上次通信">
            <span v-if="form.updateTime">{{ formatDeviceTime(form.updateTime) }}</span>
            <span v-else class="text-secondary">-</span>
          </el-descriptions-item>
          <el-descriptions-item label="经纬度 / 上报时间" :span="2">
            <span>经度 {{ form.longitude != null && form.longitude !== '' ? form.longitude : '—' }}</span>
            <span class="detail-inline-sep">·</span>
            <span>纬度 {{ form.latitude != null && form.latitude !== '' ? form.latitude : '—' }}</span>
            <span class="detail-inline-sep">·</span>
            <span>上报 {{ form.createTime || '—' }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="hasDeviceRemark" class="device-remark-block">
          <div class="device-remark-head">
            <span class="device-remark-title">备注</span>
            <el-button type="primary" link size="small" icon="DocumentCopy" @click="copyDeviceRemark">复制</el-button>
          </div>
          <div class="device-remark-body">
            <JsonPretty :data="form.remark" show-icon />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="open = false">关 闭</el-button>
      </template>
    </el-dialog>

    <!-- 设备编辑对话框 -->
    <el-dialog v-model="editOpen" append-to-body title="编辑设备信息" width="500px">
      <el-form :model="editForm" label-width="90px" ref="editFormRef">
        <el-form-item label="设备编码">
          <el-text>{{ editForm.code }}</el-text>
        </el-form-item>
        <el-form-item label="设备位置" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入设备位置" />
        </el-form-item>
        <el-form-item label="设备类别" prop="type">
          <el-input v-model="editForm.type" placeholder="请输入设备类别" />
        </el-form-item>
        <el-form-item label="设备型号" prop="model">
          <el-input v-model="editForm.model" placeholder="请输入设备型号" />
        </el-form-item>
        <el-form-item label="在线状态" prop="online">
          <el-radio-group v-model="editForm.online">
            <el-radio label="1"><el-text type="success">在线</el-text></el-radio>
            <el-radio label="0"><el-text type="danger">离线</el-text></el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitEdit" :loading="editLoading">保 存</el-button>
        <el-button @click="editOpen = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 同步设备弹窗 -->
    <el-dialog v-model="pushDeviceOpen" append-to-body title="同步设备" width="720px" @close="resetSyncState">
      <!-- 筛选条件 -->
      <el-form :inline="true" label-width="80px" :model="pushDeviceForm">
        <el-form-item label="网关代码">
          <el-input v-model="pushDeviceForm.net" clearable placeholder="不限" style="width: 140px" />
        </el-form-item>
        <el-form-item label="设备编码">
          <el-input v-model="pushDeviceForm.code" clearable placeholder="不限" style="width: 140px" />
        </el-form-item>
        <el-form-item label="设备型号">
          <el-input v-model="pushDeviceForm.model" clearable placeholder="不限" style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="querySyncDevices" :loading="syncQueryLoading">筛选设备</el-button>
        </el-form-item>
      </el-form>

      <!-- 设备列表 -->
      <div v-if="syncQueried">
        <div class="sync-summary">
          筛选到 <el-text type="primary" tag="b">{{ syncDeviceList.length }}</el-text> 台设备
        </div>
        <el-table :data="syncDeviceList" max-height="280" size="small" v-loading="syncQueryLoading" border>
          <el-table-column label="设备编码" prop="code" min-width="150" />
          <el-table-column label="位置" prop="name" min-width="140" show-overflow-tooltip />
          <el-table-column label="平台" prop="pfCode" width="90" align="center" />
          <el-table-column label="型号" prop="model" width="110" align="center" show-overflow-tooltip />
          <el-table-column label="同步状态" width="90" align="center">
            <template #default="scope">
              <el-tag :type="getSyncStatusType(syncStatusMap[scope.row.code])" size="small">
                {{ syncStatusMap[scope.row.code] || '待同步' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- 进度条 -->
        <div v-if="syncProgress.total > 0" class="sync-progress-bar">
          <el-progress
            :percentage="syncProgress.percent"
            :status="syncProgress.status || undefined"
            striped
            striped-flow
            :duration="syncProgress.status ? 0 : 6"
          />
          <el-text size="small" class="sync-progress-text">
            已同步 {{ syncProgress.done }} / {{ syncProgress.total }} 台
          </el-text>
        </div>
      </div>
      <el-empty v-else description="请先设置筛选条件，点击「筛选设备」查看待同步设备" :image-size="80" />

      <template #footer>
        <el-button
          type="primary"
          @click="confirmSyncDevice"
          :loading="syncLoading"
          :disabled="syncDeviceList.length === 0 || syncLoading"
        >开始同步</el-button>
        <el-button @click="pushDeviceOpen = false">取 消</el-button>
      </template>
    </el-dialog>

    <ImportTemplate
      :download-url="'/sys/device/importTemplate'"
      file-name="facility"
      title="设备"
      upload-url="/sys/device/importData"
      ref="importTemplate"
      @uploadSuccess="getList"
    />
  </div>
</template>

<script setup name="Device">
import JsonPretty from 'vue-json-pretty'
import { Search, Cpu, Connection, View, Edit, Delete, Timer } from '@element-plus/icons-vue'
import { selectPlatform as fetchPlatformList } from '@/api/sys/platform'
import { listDevice, getDevice, updateDevice, clearDevice, pushDevice, getDeviceOnlineStats, updateDeviceOnline, delDevice } from '@/api/sys/device'

const { proxy } = getCurrentInstance()
const { sys_boolean } = proxy.useDict('sys_boolean')

// ==================== 状态 ====================
const loading       = ref(true)
const statsLoading  = ref(false)
const open          = ref(false)
const total         = ref(0)
const title         = ref('')
const importTemplate = ref(null)

const deviceList     = ref([])
const platformOptions = ref([])    // {id, code, name}
const onlineStatsMap  = ref({})   // pfCode -> {total, onlineCount}
const selectedPfCode  = ref(null)
const searchKeyword   = ref('')
const filterOnline    = ref(null)
const viewMode        = ref('table')
const exactMatch      = ref(false)
const editOpen        = ref(false)
const editForm        = ref({})
const editOrigOnline  = ref('')
const editLoading     = ref(false)
const pushDeviceOpen  = ref(false)
const pushDeviceForm  = ref({})
const syncQueryLoading = ref(false)
const syncLoading     = ref(false)
const syncDeviceList  = ref([])
const syncQueried     = ref(false)
const syncStatusMap   = ref({})
const syncProgress    = reactive({ total: 0, done: 0, percent: 0, status: '' })
const form            = ref({})

const queryParams = reactive({
  pageNum:       1,
  pageSize:      15,
  orderByColumn: 'id',
  isAsc:         'DESC',
  params:         {},
  pfCode:   null,
  code:     null,
  name:     null,
  online:   null
})

// ==================== 计算属性 ====================
const totalStats = computed(() => {
  let total = 0, online = 0
  Object.values(onlineStatsMap.value).forEach(s => {
    total  += Number(s.total || 0)
    online += Number(s.onlineCount || 0)
  })
  return { total, onlineCount: online }
})

const currentPlatformName = computed(() => {
  if (!selectedPfCode.value) return '全部'
  return platformOptions.value.find(p => p.code === selectedPfCode.value)?.name || selectedPfCode.value
})

const currentOnlineCount = computed(() => {
  if (!selectedPfCode.value) return totalStats.value.onlineCount
  return Number(onlineStatsMap.value[selectedPfCode.value]?.onlineCount || 0)
})

const hasDeviceRemark = computed(() => {
  const r = form.value?.remark
  if (r == null) return false
  if (typeof r === 'string') return r.trim().length > 0
  if (Array.isArray(r)) return r.length > 0
  if (typeof r === 'object') return Object.keys(r).length > 0
  return true
})

// ==================== 初始化 ====================
async function init() {
  const [pfRes] = await Promise.all([fetchPlatformList(), loadOnlineStats()])
  platformOptions.value = pfRes.data || []
  getList()
}

async function loadOnlineStats() {
  statsLoading.value = true
  try {
    const res = await getDeviceOnlineStats()
    const map = {}
    ;(res.data || []).forEach(s => { map[s.pfCode] = s })
    onlineStatsMap.value = map
  } finally {
    statsLoading.value = false
  }
}

async function refreshAll() {
  await loadOnlineStats()
  getList()
}

/** 展示用：在线状态统一为 '1' / '0'（接口可能返回 0/1 或字符串） */
function normalizeDeviceOnline(v) {
  return v === 1 || v === '1' || v === true ? '1' : '0'
}

function formatDeviceTime(val) {
  if (val == null || val === '') return '-'
  const s = proxy.parseTime(val)
  return s || String(val)
}

function stringifyForCopy(val) {
  if (val == null) return ''
  if (typeof val === 'string') return val
  try {
    return JSON.stringify(val, null, 2)
  } catch {
    return String(val)
  }
}

async function copyPlainText(text) {
  if (!text) {
    proxy.$modal.msgWarning('无可复制内容')
    return
  }
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
    } else {
      const el = document.createElement('textarea')
      el.value = text
      el.setAttribute('readonly', '')
      el.style.position = 'fixed'
      el.style.left = '-9999px'
      document.body.appendChild(el)
      el.select()
      document.execCommand('copy')
      document.body.removeChild(el)
    }
    proxy.$modal.msgSuccess('已复制到剪贴板')
  } catch {
    proxy.$modal.msgError('复制失败')
  }
}

function copyDeviceRemark() {
  copyPlainText(stringifyForCopy(form.value.remark))
}

// ==================== 数据加载 ====================
function getList() {
  loading.value = true
  listDevice({ ...queryParams }).then(res => {
    deviceList.value = res.rows
    total.value      = res.total
    loading.value    = false
  })
}

function selectPlatform(code) {
  selectedPfCode.value  = code
  queryParams.pfCode    = code
  queryParams.pageNum   = 1
  getList()
}

let searchTimer = null
function handleSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    queryParams.pageNum = 1
    queryParams.params.searchValue    = searchKeyword.value || null
    queryParams.online  = filterOnline.value || null
    getList()
  }, 300)
}

// ==================== 设备详情 ====================
function handleView(row) {
  getDevice(row.id).then(res => {
    form.value = res.data
    try { form.value.remark = JSON.parse(res.data.remark) } catch {}
    open.value  = true
    title.value = `设备详情 — ${row.code}`
  })
}

// ==================== 设备编辑 ====================
function handleEdit(row) {
  const onlineStr = normalizeDeviceOnline(row.online)
  editForm.value = {
    id: row.id, code: row.code, pfCode: row.pfCode, net: row.net,
    name: row.name, type: row.type, model: row.model,
    ip: row.ip, wireless: row.wireless,
    longitude: row.longitude, latitude: row.latitude,
    online: onlineStr
  }
  editOrigOnline.value = onlineStr
  editOpen.value = true
}

async function submitEdit() {
  editLoading.value = true
  try {
    await updateDevice({
      id: editForm.value.id, code: editForm.value.code, pfCode: editForm.value.pfCode,
      net: editForm.value.net, name: editForm.value.name, type: editForm.value.type,
      model: editForm.value.model, ip: editForm.value.ip, wireless: editForm.value.wireless,
      longitude: editForm.value.longitude, latitude: editForm.value.latitude,
      online: editForm.value.online
    })
    // 如果在线状态有变化，单独调接口推送状态事件
    if (editForm.value.online !== editOrigOnline.value) {
      await updateDeviceOnline({ code: editForm.value.code, pfCode: editForm.value.pfCode, online: editForm.value.online === '1' })
    }
    proxy.$modal.msgSuccess('保存成功')
    editOpen.value = false
    getList()
    loadOnlineStats()
  } catch (e) {
    proxy.$modal.msgError('保存失败')
  } finally {
    editLoading.value = false
  }
}

function handleDeleteDevice(row) {
  proxy.$modal.confirm(`是否确认删除设备「${row.code}」？`)
    .then(() => delDevice(row.id))
    .then(() => {
      proxy.$modal.msgSuccess('删除成功')
      getList()
      loadOnlineStats()
    })
    .catch(() => {})
}

// ==================== 在线状态 ====================
async function handleOnlineAction(dev, cmd) {
  const online  = cmd === 'online'
  const onlineStr = online ? '1' : '0'
  const label   = online ? '在线' : '离线'
  try {
    await proxy.$modal.confirm(`确认将设备 [${dev.code}] 标记为${label}？此操作会推送状态变更事件给下游系统。`)
    await updateDeviceOnline({ code: dev.code, pfCode: dev.pfCode, online })
    dev.online = onlineStr
    proxy.$modal.msgSuccess(`已标记${label}`)
    // 刷新统计
    await loadOnlineStats()
  } catch (e) {
    // 用户取消
  }
}

// ==================== 同步/清空 ====================
function handleSyncDevice() {
  pushDeviceOpen.value = true
}

function resetSyncState() {
  syncDeviceList.value  = []
  syncQueried.value     = false
  syncStatusMap.value   = {}
  syncProgress.total    = 0
  syncProgress.done     = 0
  syncProgress.percent  = 0
  syncProgress.status   = ''
  pushDeviceForm.value  = {}
}

async function querySyncDevices() {
  syncQueryLoading.value = true
  syncQueried.value      = true
  syncStatusMap.value    = {}
  syncProgress.total     = 0
  try {
    const res = await listDevice({
      net:     pushDeviceForm.value.net   || null,
      code:    pushDeviceForm.value.code  || null,
      model:   pushDeviceForm.value.model || null,
      pageNum: 1,
      pageSize: 200
    })
    syncDeviceList.value = res.rows || []
  } finally {
    syncQueryLoading.value = false
  }
}

async function confirmSyncDevice() {
  if (syncDeviceList.value.length === 0) return
  syncLoading.value    = true
  syncProgress.total   = syncDeviceList.value.length
  syncProgress.done    = 0
  syncProgress.percent = 0
  syncProgress.status  = ''
  syncDeviceList.value.forEach(d => { syncStatusMap.value[d.code] = '同步中...' })
  try {
    await pushDevice(pushDeviceForm.value)
    for (let i = 0; i < syncDeviceList.value.length; i++) {
      await new Promise(r => setTimeout(r, 40))
      syncStatusMap.value[syncDeviceList.value[i].code] = '已同步'
      syncProgress.done    = i + 1
      syncProgress.percent = Math.round((i + 1) / syncProgress.total * 100)
    }
    syncProgress.status = 'success'
    proxy.$modal.msgSuccess(`同步完成，共 ${syncProgress.total} 台`)
    getList()
  } catch (e) {
    syncDeviceList.value.forEach(d => { syncStatusMap.value[d.code] = '失败' })
    syncProgress.percent = 100
    syncProgress.status  = 'exception'
    proxy.$modal.msgError('同步失败')
  } finally {
    syncLoading.value = false
  }
}

function getSyncStatusType(status) {
  if (status === '已同步')   return 'success'
  if (status === '失败')     return 'danger'
  if (status === '同步中...') return 'warning'
  return 'info'
}

function handleClear() {
  proxy.$modal.confirm('是否确认清空所有设备数据?')
    .then(() => clearDevice())
    .then(() => { getList(); loadOnlineStats(); proxy.$modal.msgSuccess('已清空') })
    .catch(() => {})
}

function handleExport() {
  proxy.download('sys/device/export', { ...queryParams }, `device_${Date.now()}.xlsx`)
}

init()
</script>

<style scoped>
/* ======================== 整体布局 ======================== */
.device-container {
  display: flex;
  height: calc(100vh - 120px);
  gap: 0;
  overflow: hidden;
  margin-left: 10px;
}

/* ======================== 左侧侧边栏 ======================== */
.platform-sidebar {
  width: 200px;
  min-width: 180px;
  flex-shrink: 0;
  border-right: 1px solid var(--el-border-color-light);
  overflow-y: auto;
  background: var(--el-bg-color);
  padding-bottom: 16px;
  scrollbar-width: none;
}
.platform-sidebar::-webkit-scrollbar { display: none; }
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  position: sticky;
  top: 0;
  background: var(--el-bg-color);
  z-index: 1;
}
.sidebar-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.platform-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 14px;
  cursor: pointer;
  transition: background 0.15s;
  border-radius: 0;
}
.platform-item:hover { background: var(--el-fill-color-light); }
.platform-item.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 500;
}
.pf-info { display: flex; align-items: center; gap: 6px; min-width: 0; }
.pf-name {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pf-counts { flex-shrink: 0; }
.count-text { font-size: 12px; }
.count-online { color: var(--el-color-success); font-weight: 600; }
.count-sep    { color: var(--el-text-color-secondary); margin: 0 2px; }
.count-total  { color: var(--el-text-color-secondary); }

/* 在线状态小点 */
.online-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot-online { background: #67c23a; }
.dot-offline { background: #ddd; }

/* ======================== 右侧主区 ======================== */
.device-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0 16px;
}
.device-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 0 8px;
}
.toolbar-left  { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.toolbar-right { display: flex; align-items: center; gap: 8px; }
.device-summary {
  padding: 6px 0 10px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 8px;
}
.summary-name { font-weight: 600; margin-right: 4px; }
.pagination-bar { margin-top: 8px; }

/* ======================== 设备卡片视图 ======================== */
.device-cards {
  flex: 1;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 12px;
  padding: 4px 0 12px;
  align-content: start;
  align-items: stretch;
}
.device-card-wrap { min-width: 0; height: 100%; display: flex; flex-direction: column; }
.device-card {
  border-radius: 8px;
  transition: box-shadow 0.2s;
  cursor: default;
  height: 100%;
  display: flex;
  flex-direction: column;
  width: 100%;
}
.device-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  flex: 1;
}
.device-card.card-online  { border-top: 3px solid #67c23a; }
.device-card.card-offline { border-top: 3px solid #ddd; }

.dc-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
.dc-status-dot {
  width: 10px; height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot-on  { background: #67c23a; box-shadow: 0 0 4px #67c23a; }
.dot-off { background: #ccc; }
.dc-code {
  font-weight: 600;
  font-size: 14px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dc-pf-tag { font-family: monospace; }

.dc-name {
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dc-meta {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}
.dc-meta span { display: flex; align-items: center; gap: 3px; }
.dc-last-comm {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}
.dc-model {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}
.dc-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
  margin-top: auto;
}
.dc-actions { display: flex; align-items: center; gap: 4px; }

/* 表格视图 */
.text-secondary { color: var(--el-text-color-secondary); font-size: 12px; }
.status-dot-sm {
  display: inline-block;
  width: 8px; height: 8px;
  border-radius: 50%;
}

/* badge */
.badge-total { margin-right: 4px; }
:deep(.el-badge__content) { font-size: 10px; }

/* 设备详情弹窗 */
.detail-dialog-body {
  max-height: 65vh;
  overflow-y: auto;
  padding-right: 4px;
}
.detail-inline-sep {
  margin: 0 10px;
  color: var(--el-text-color-placeholder);
}
.device-remark-block {
  margin-top: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  overflow: hidden;
  background: var(--el-fill-color-blank);
}
.device-remark-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
}
.device-remark-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
}
.device-remark-body {
  max-height: 220px;
  overflow: auto;
  padding: 8px 10px;
}

/* 同步弹窗 */
.sync-summary {
  margin: 8px 0 10px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.sync-progress-bar {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.sync-progress-text {
  white-space: nowrap;
  color: var(--el-text-color-secondary);
}
</style>
