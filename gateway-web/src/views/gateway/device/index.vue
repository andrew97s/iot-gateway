<template>
  <div class="gw-page">
    <!-- 筛选栏 -->
    <div class="gw-card">
      <div class="gw-card-body gw-filter-bar">
        <el-input
          v-model="searchKeyword"
          clearable
          :placeholder="exactMatch ? '设备编码 / 位置（精准）' : '设备编码 / 位置（模糊）'"
          style="width: 220px"
          @input="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-tooltip content="精准匹配：完全相等；关闭后模糊查询" placement="top">
          <el-switch v-model="exactMatch" active-text="精准" inactive-text="模糊" inline-prompt @change="handleSearch" />
        </el-tooltip>
        <el-select v-model="queryParams.pfCode" clearable filterable placeholder="归属插件（全部）" style="width: 170px" @change="handlePfChange">
          <el-option v-for="pf in platformOptions" :key="pf.code" :value="pf.code">
            <span>{{ pf.name }}</span>
            <span class="gw-muted gw-small" style="float: right">
              {{ onlineStatsMap[pf.code]?.onlineCount || 0 }}/{{ onlineStatsMap[pf.code]?.total || 0 }}
            </span>
          </el-option>
        </el-select>
        <el-select v-model="filterOnline" clearable placeholder="状态（全部）" style="width: 120px" @change="handleSearch">
          <el-option label="在线" value="1" />
          <el-option label="离线" value="0" />
        </el-select>
        <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
        <div class="spacer" />
        <el-button icon="Promotion" plain type="primary" @click="handleSyncDevice">同步设备至上级</el-button>
        <el-button icon="Upload" plain @click="importTemplate?.handleImport">导入</el-button>
        <el-button icon="Download" plain @click="handleExport">导出</el-button>
        <el-button icon="WarnTriangleFilled" plain type="danger" v-hasPermi="['sys:device:remove']" @click="handleClear">清空</el-button>
      </div>
    </div>

    <!-- 设备表格 -->
    <div class="gw-card">
      <div class="gw-card-head">
        <h2>
          设备列表
          <span class="gw-muted gw-small" style="font-weight: 400; margin-left: 10px">
            共 {{ totalStats.total }} 台 · 在线 <span style="color:#16a34a">{{ totalStats.onlineCount }}</span>
            · 离线 <span style="color:#dc2626">{{ totalStats.total - totalStats.onlineCount }}</span>
          </span>
        </h2>
        <div class="gw-filter-bar">
          <template v-if="selectedRows.length > 0">
            <span class="gw-muted gw-small">已选 {{ selectedRows.length }} 台</span>
            <el-button size="small" type="primary" plain icon="Promotion" :loading="batchSyncLoading" @click="handleBatchSync">同步选中</el-button>
            <el-button size="small" type="danger" plain icon="Delete" v-hasPermi="['sys:device:remove']" @click="handleBatchDelete">删除选中</el-button>
          </template>
          <el-button size="small" circle icon="Refresh" @click="refreshAll" :loading="statsLoading" />
        </div>
      </div>
      <div class="gw-card-body no-pad">
        <el-table :data="deviceList" v-loading="loading" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="42" align="center" />
          <el-table-column label="设备编码" min-width="160">
            <template #default="scope">
              <span class="gw-mono" style="font-weight: 600">{{ scope.row.code }}</span>
            </template>
          </el-table-column>
          <el-table-column label="位置 / 名称" prop="name" min-width="150" show-overflow-tooltip />
          <el-table-column label="类型" width="110" align="center">
            <template #default="scope">
              <el-tag size="small" effect="plain">{{ scope.row.type || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="型号" prop="model" width="130" align="center" show-overflow-tooltip />
          <el-table-column label="归属插件" width="130" align="center">
            <template #default="scope">
              <el-tag size="small" type="info" effect="plain">{{ platformName(scope.row.pfCode) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="IP" width="130" align="center">
            <template #default="scope">
              <span class="gw-mono">{{ scope.row.ip || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="scope">
              <span class="gw-badge" :class="isOnline(scope.row) ? 'ok' : 'off'">
                {{ isOnline(scope.row) ? '在线' : '离线' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="最后上报" width="160" align="center">
            <template #default="scope">
              <span class="gw-mono gw-muted">{{ formatDeviceTime(scope.row.updateTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" align="center" fixed="right">
            <template #default="scope">
              <el-button size="small" link type="primary" @click="handleView(scope.row)">详情</el-button>
              <el-button size="small" link type="warning" @click="handleEdit(scope.row)">修改</el-button>
              <el-button size="small" link type="primary" :loading="rowSyncMap[scope.row.code]" @click="handleRowSync(scope.row)">同步</el-button>
              <el-button size="small" link type="danger" v-hasPermi="['sys:device:remove']" @click="handleDeleteDevice(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination
          v-model:limit="queryParams.pageSize"
          v-model:page="queryParams.pageNum"
          :total="total"
          @pagination="getList"
          v-show="total > 0"
        />
      </div>
    </div>

    <!-- 设备详情对话框 -->
    <el-dialog v-model="open" append-to-body :title="title" width="720px">
      <div class="detail-dialog-body">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="设备编码" :span="2"><span class="gw-mono">{{ form.code }}</span></el-descriptions-item>
          <el-descriptions-item label="设备位置" :span="2">{{ form.name }}</el-descriptions-item>
          <el-descriptions-item label="归属插件">{{ platformName(form.pfCode) }}</el-descriptions-item>
          <el-descriptions-item label="网关代码">{{ form.net }}</el-descriptions-item>
          <el-descriptions-item label="设备类别">{{ form.type }}</el-descriptions-item>
          <el-descriptions-item label="设备型号">{{ form.model }}</el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ form.ip || '-' }}</el-descriptions-item>
          <el-descriptions-item label="在线状态">
            <span class="gw-badge" :class="isOnline(form) ? 'ok' : 'off'">{{ isOnline(form) ? '在线' : '离线' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="无线设备">
            <dict-tag :options="sys_boolean" :value="form.wireless" />
          </el-descriptions-item>
          <el-descriptions-item label="上次通信">
            <span v-if="form.updateTime">{{ formatDeviceTime(form.updateTime) }}</span>
            <span v-else class="gw-muted">-</span>
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
            <span class="device-remark-title">扩展属性 / 备注</span>
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
    <el-dialog v-model="editOpen" append-to-body title="修改设备信息" width="520px">
      <el-form :model="editForm" label-width="90px" ref="editFormRef">
        <el-form-item label="设备编码">
          <span class="gw-mono">{{ editForm.code }}</span>
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
        <el-form-item label="经度" prop="longitude">
          <el-input v-model="editForm.longitude" placeholder="经度（可选）" />
        </el-form-item>
        <el-form-item label="纬度" prop="latitude">
          <el-input v-model="editForm.latitude" placeholder="纬度（可选）" />
        </el-form-item>
        <el-form-item label="在线状态" prop="online">
          <el-radio-group v-model="editForm.online">
            <el-radio label="1"><el-text type="success">在线</el-text></el-radio>
            <el-radio label="0"><el-text type="danger">离线</el-text></el-radio>
          </el-radio-group>
          <div class="gw-muted gw-small" style="width: 100%">状态变更将向上级平台推送设备事件</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitEdit" :loading="editLoading">保 存</el-button>
        <el-button @click="editOpen = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 批量同步设备弹窗 -->
    <el-dialog v-model="pushDeviceOpen" append-to-body title="同步设备至上级平台" width="720px" @close="resetSyncState">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="mb12"
        title="按筛选条件将设备档案以设备消息推送至上级平台（推送通道见「系统配置 - 上级平台」）"
      />
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

      <div v-if="syncQueried">
        <div class="sync-summary">
          筛选到 <el-text type="primary" tag="b">{{ syncDeviceList.length }}</el-text> 台设备
        </div>
        <el-table :data="syncDeviceList" max-height="280" size="small" v-loading="syncQueryLoading" border>
          <el-table-column label="设备编码" prop="code" min-width="150" />
          <el-table-column label="位置" prop="name" min-width="140" show-overflow-tooltip />
          <el-table-column label="插件" prop="pfCode" width="90" align="center" />
          <el-table-column label="型号" prop="model" width="110" align="center" show-overflow-tooltip />
          <el-table-column label="同步状态" width="90" align="center">
            <template #default="scope">
              <el-tag :type="getSyncStatusType(syncStatusMap[scope.row.code])" size="small">
                {{ syncStatusMap[scope.row.code] || '待同步' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

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
import { Search } from '@element-plus/icons-vue'
import { selectPlatform as fetchPlatformList } from '@/api/sys/platform'
import { listDevice, getDevice, updateDevice, clearDevice, pushDevice, getDeviceOnlineStats, updateDeviceOnline, delDevice } from '@/api/sys/device'

const { proxy } = getCurrentInstance()
const { sys_boolean } = proxy.useDict('sys_boolean')

// ==================== 状态 ====================
const loading = ref(true)
const statsLoading = ref(false)
const open = ref(false)
const total = ref(0)
const title = ref('')
const importTemplate = ref(null)

const deviceList = ref([])
const platformOptions = ref([])
const onlineStatsMap = ref({})
const searchKeyword = ref('')
const filterOnline = ref(null)
const exactMatch = ref(false)
const selectedRows = ref([])
const rowSyncMap = reactive({})
const batchSyncLoading = ref(false)
const editOpen = ref(false)
const editForm = ref({})
const editOrigOnline = ref('')
const editLoading = ref(false)
const pushDeviceOpen = ref(false)
const pushDeviceForm = ref({})
const syncQueryLoading = ref(false)
const syncLoading = ref(false)
const syncDeviceList = ref([])
const syncQueried = ref(false)
const syncStatusMap = ref({})
const syncProgress = reactive({ total: 0, done: 0, percent: 0, status: '' })
const form = ref({})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 15,
  orderByColumn: 'id',
  isAsc: 'DESC',
  params: {},
  pfCode: null,
  code: null,
  name: null,
  online: null
})

// ==================== 计算属性 ====================
const totalStats = computed(() => {
  let t = 0, online = 0
  Object.values(onlineStatsMap.value).forEach(s => {
    t += Number(s.total || 0)
    online += Number(s.onlineCount || 0)
  })
  return { total: t, onlineCount: online }
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

function isOnline(row) {
  const v = row?.online
  return v === 1 || v === '1' || v === true
}

function platformName(pfCode) {
  if (!pfCode) return '-'
  return platformOptions.value.find(p => p.code === pfCode)?.name || pfCode
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
    total.value = res.total
    loading.value = false
  })
}

function handlePfChange() {
  queryParams.pageNum = 1
  getList()
}

let searchTimer = null
function handleSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    queryParams.pageNum = 1
    queryParams.params.searchValue = searchKeyword.value || null
    queryParams.params.exactMatch = exactMatch.value ? '1' : null
    queryParams.online = filterOnline.value || null
    getList()
  }, 300)
}

function handleSelectionChange(selection) {
  selectedRows.value = selection
}

// ==================== 设备详情 ====================
function handleView(row) {
  getDevice(row.id).then(res => {
    form.value = res.data
    try { form.value.remark = JSON.parse(res.data.remark) } catch {}
    open.value = true
    title.value = `设备详情 — ${row.code}`
  })
}

// ==================== 设备编辑 ====================
function handleEdit(row) {
  const onlineStr = isOnline(row) ? '1' : '0'
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

// ==================== 删除 ====================
function handleDeleteDevice(row) {
  proxy.$modal.confirm(`是否确认删除设备「${row.code}」？删除后历史消息保留。`)
    .then(() => delDevice(row.id))
    .then(() => {
      proxy.$modal.msgSuccess('删除成功')
      getList()
      loadOnlineStats()
    })
    .catch(() => {})
}

function handleBatchDelete() {
  const rows = selectedRows.value
  if (rows.length === 0) return
  proxy.$modal.confirm(`是否确认删除选中的 ${rows.length} 台设备？`)
    .then(() => delDevice(rows.map(r => r.id).join(',')))
    .then(() => {
      proxy.$modal.msgSuccess('删除成功')
      getList()
      loadOnlineStats()
    })
    .catch(() => {})
}

// ==================== 指定设备同步 ====================
async function handleRowSync(row) {
  try {
    await proxy.$modal.confirm(`将设备「${row.code}」的档案信息同步至上级平台？`)
  } catch (e) { return }
  rowSyncMap[row.code] = true
  try {
    await pushDevice({ code: row.code, pfCode: row.pfCode })
    proxy.$modal.msgSuccess('已同步至上级平台')
  } catch (e) {
    proxy.$modal.msgError('同步失败，请检查上级平台推送配置')
  } finally {
    rowSyncMap[row.code] = false
  }
}

async function handleBatchSync() {
  const rows = selectedRows.value
  if (rows.length === 0) return
  try {
    await proxy.$modal.confirm(`将选中的 ${rows.length} 台设备同步至上级平台？`)
  } catch (e) { return }
  batchSyncLoading.value = true
  let ok = 0, fail = 0
  for (const row of rows) {
    try {
      await pushDevice({ code: row.code, pfCode: row.pfCode })
      ok++
    } catch (e) {
      fail++
    }
  }
  batchSyncLoading.value = false
  if (fail === 0) {
    proxy.$modal.msgSuccess(`同步完成，共 ${ok} 台`)
  } else {
    proxy.$modal.msgWarning(`同步完成：成功 ${ok} 台，失败 ${fail} 台`)
  }
}

// ==================== 批量同步弹窗 ====================
function handleSyncDevice() {
  pushDeviceOpen.value = true
}

function resetSyncState() {
  syncDeviceList.value = []
  syncQueried.value = false
  syncStatusMap.value = {}
  syncProgress.total = 0
  syncProgress.done = 0
  syncProgress.percent = 0
  syncProgress.status = ''
  pushDeviceForm.value = {}
}

async function querySyncDevices() {
  syncQueryLoading.value = true
  syncQueried.value = true
  syncStatusMap.value = {}
  syncProgress.total = 0
  try {
    const res = await listDevice({
      net: pushDeviceForm.value.net || null,
      code: pushDeviceForm.value.code || null,
      model: pushDeviceForm.value.model || null,
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
  syncLoading.value = true
  syncProgress.total = syncDeviceList.value.length
  syncProgress.done = 0
  syncProgress.percent = 0
  syncProgress.status = ''
  syncDeviceList.value.forEach(d => { syncStatusMap.value[d.code] = '同步中...' })
  try {
    await pushDevice(pushDeviceForm.value)
    for (let i = 0; i < syncDeviceList.value.length; i++) {
      await new Promise(r => setTimeout(r, 40))
      syncStatusMap.value[syncDeviceList.value[i].code] = '已同步'
      syncProgress.done = i + 1
      syncProgress.percent = Math.round((i + 1) / syncProgress.total * 100)
    }
    syncProgress.status = 'success'
    proxy.$modal.msgSuccess(`同步完成，共 ${syncProgress.total} 台`)
    getList()
  } catch (e) {
    syncDeviceList.value.forEach(d => { syncStatusMap.value[d.code] = '失败' })
    syncProgress.percent = 100
    syncProgress.status = 'exception'
    proxy.$modal.msgError('同步失败')
  } finally {
    syncLoading.value = false
  }
}

function getSyncStatusType(status) {
  if (status === '已同步') return 'success'
  if (status === '失败') return 'danger'
  if (status === '同步中...') return 'warning'
  return 'info'
}

// ==================== 清空 / 导出 ====================
function handleClear() {
  proxy.$modal.confirm('是否确认清空所有设备？此操作不可恢复！')
    .then(() => clearDevice())
    .then(() => {
      proxy.$modal.msgSuccess('清空成功')
      getList()
      loadOnlineStats()
    })
    .catch(() => {})
}

function handleExport() {
  proxy.download('sys/device/export', { ...queryParams }, `device_${Date.now()}.xlsx`)
}

init()
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.detail-inline-sep { margin: 0 6px; color: #cbd5e1; }
.device-remark-block {
  margin-top: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}
.device-remark-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}
.device-remark-title { font-size: 13px; font-weight: 500; color: #475569; }
.device-remark-body { padding: 10px 12px; max-height: 220px; overflow: auto; }
.sync-summary { margin-bottom: 8px; font-size: 13px; }
.sync-progress-bar { margin-top: 12px; }
.sync-progress-text { display: block; text-align: center; margin-top: 4px; }
</style>
