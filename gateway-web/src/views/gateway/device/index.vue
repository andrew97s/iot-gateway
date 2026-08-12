<template>
  <div class="gw-page">
    <div class="page-summary">
      共 <b>{{ totalStats.total }}</b> 台 · 在线 <b class="ok">{{ totalStats.onlineCount }}</b> · 离线 <b class="err">{{ totalStats.total - totalStats.onlineCount }}</b>
    </div>

    <div class="gw-card">
      <div class="gw-card-body gw-filter-bar">
        <el-input v-model="filters.keyword" clearable placeholder="设备编码 / 名称" style="width: 200px" @keyup.enter="handleQuery" />
        <el-select v-model="filters.type" clearable filterable placeholder="全部类型" style="width: 140px">
          <el-option v-for="t in deviceTypes" :key="t.code" :label="t.name" :value="t.code" />
        </el-select>
        <el-select v-model="filters.pfCode" clearable filterable placeholder="全部插件" style="width: 160px">
          <el-option v-for="pf in platformOptions" :key="pf.code" :label="pf.name" :value="pf.code" />
        </el-select>
        <el-select v-model="filters.online" clearable placeholder="全部状态" style="width: 120px">
          <el-option label="在线" value="1" />
          <el-option label="离线" value="0" />
        </el-select>
        <el-select v-model="filters.sync" clearable placeholder="同步状态（全部）" style="width: 150px">
          <el-option label="已同步" value="synced" />
          <el-option label="未同步" value="unsynced" />
        </el-select>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <div class="spacer" />
        <el-button plain :disabled="!selectedRows.length" @click="openSync(selectedRows)">批量同步至上级平台</el-button>
        <el-button type="danger" plain :disabled="!selectedRows.length" v-hasPermi="['sys:device:remove']" @click="handleBatchDelete">批量删除</el-button>
      </div>
    </div>

    <div class="gw-card">
      <div class="gw-card-body no-pad">
        <el-table :data="filteredList" v-loading="loading" @selection-change="(s) => (selectedRows = s)">
          <el-table-column type="selection" width="42" align="center" />
          <el-table-column label="设备编码" min-width="150" show-overflow-tooltip>
            <template #default="{ row }"><span class="gw-mono" style="font-weight:600">{{ row.code }}</span></template>
          </el-table-column>
          <el-table-column label="名称" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ displayName(row) }}</template>
          </el-table-column>
          <el-table-column label="类型" width="160" align="center">
            <template #default="{ row }"><span class="gw-tag">{{ typeName(row.type) }}</span></template>
          </el-table-column>
          <el-table-column label="型号" prop="model" min-width="110" show-overflow-tooltip>
            <template #default="{ row }"><span class="gw-muted">{{ row.model || '-' }}</span></template>
          </el-table-column>
          <el-table-column label="归属插件" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ platformName(row.pfCode) }}</template>
          </el-table-column>
          <el-table-column label="位置" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">{{ row.name || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <span class="gw-badge" :class="isOnline(row) ? 'ok' : 'off'">{{ isOnline(row) ? '在线' : '离线' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="最后上报" width="150" align="center">
            <template #default="{ row }"><span class="gw-mono gw-muted gw-small">{{ formatTime(row.updateTime) }}</span></template>
          </el-table-column>
          <el-table-column label="同步状态" width="130" align="center">
            <template #default="{ row }">
              <span class="gw-badge plain" :class="syncBadgeClass(row)">{{ row.syncLabel || '未同步' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">修改</el-button>
              <el-button link type="primary" @click="openSync([row])">同步</el-button>
              <el-button link type="danger" v-hasPermi="['sys:device:remove']" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-foot">
          <span class="gw-muted gw-small">已选 {{ selectedRows.length }} 项 · 共 {{ total }} 条记录</span>
          <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
        </div>
      </div>
    </div>

    <!-- 修改设备 -->
    <el-dialog v-model="editOpen" :title="`修改设备 · ${editForm.code || ''}`" width="680px" append-to-body destroy-on-close>
      <el-form :model="editForm" label-position="top" class="edit-grid">
        <el-form-item label="设备编码" required>
          <el-input v-model="editForm.code" />
          <div class="field-hint">网关内唯一，同步上级平台时作为设备标识</div>
        </el-form-item>
        <el-form-item label="设备名称" required>
          <el-input v-model="editForm.displayName" placeholder="显示名称（写入扩展属性）" />
        </el-form-item>
        <el-form-item label="设备类型" required>
          <el-select v-model="editForm.type" filterable allow-create style="width: 100%">
            <el-option v-for="t in deviceTypes" :key="t.code" :label="t.name" :value="t.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="editForm.model" />
        </el-form-item>
        <el-form-item label="归属插件">
          <el-input :model-value="`${platformName(editForm.pfCode)} (${editForm.pfCode || '-'})`" disabled />
          <div class="field-hint">由接入来源决定，不可修改</div>
        </el-form-item>
        <el-form-item label="状态">
          <el-input :model-value="isOnline(editForm) ? '在线' : '离线'" disabled />
        </el-form-item>
        <el-form-item label="安装位置" class="full">
          <el-input v-model="editForm.name" placeholder="位置文本" />
        </el-form-item>
        <el-form-item label="经度"><el-input v-model="editForm.longitude" /></el-form-item>
        <el-form-item label="纬度"><el-input v-model="editForm.latitude" /></el-form-item>
        <el-form-item label="扩展属性（JSON）" class="full">
          <el-input v-model="editForm.remarkText" type="textarea" :rows="3" placeholder='{ "channelNo": 3 }' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editOpen = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 同步至上级平台 -->
    <el-dialog v-model="syncOpen" title="同步设备至上级平台" width="720px" append-to-body destroy-on-close>
      <p class="gw-muted">
        将所选 <b>{{ syncTargets.length }}</b> 台设备的档案信息以 <span class="gw-tag">DEVICE_EVENT</span> 统一消息推送至以下平台：
      </p>
      <el-table :data="upstreamList" size="small" class="mt12" @selection-change="(s) => (syncUpstreams = s)" ref="upstreamTableRef">
        <el-table-column type="selection" width="42" />
        <el-table-column label="平台" prop="name" min-width="140" />
        <el-table-column label="推送方式" width="110" align="center">
          <template #default="{ row }"><span class="gw-tag">{{ pushTypeLabel(row.pushType) }}</span></template>
        </el-table-column>
        <el-table-column label="连接状态" width="110" align="center">
          <template #default="{ row }">
            <span class="gw-badge" :class="upstreamAlive(row) ? 'ok' : 'warn'">{{ upstreamAlive(row) ? '已连接' : '未连接' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="该设备同步状态" min-width="140">
          <template #default>
            <span class="gw-badge plain info">将推送 DEVICE_EVENT</span>
          </template>
        </el-table-column>
      </el-table>
      <p class="field-hint mt12">平台不可达时消息将进入待补推队列；同步结果可在「消息日志」中查看。</p>
      <template #footer>
        <el-button @click="syncOpen = false">取消</el-button>
        <el-button type="primary" :loading="syncLoading" :disabled="!syncUpstreams.length" @click="confirmSync">立即同步</el-button>
      </template>
    </el-dialog>

    <!-- 删除确认 -->
    <el-dialog v-model="delOpen" title="删除设备" width="460px" append-to-body>
      <p>确认删除设备 <b class="gw-mono">{{ delTarget?.code }}（{{ displayName(delTarget) }}）</b>？</p>
      <p class="gw-muted gw-small mt8">删除后：① 通知归属插件取消该设备订阅；② 向已同步的上级平台推送 DEVICE_EVENT: deleted；③ 历史消息日志保留。</p>
      <template #footer>
        <el-button @click="delOpen = false">取消</el-button>
        <el-button type="danger" :loading="delLoading" @click="confirmDelete">确认删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Device">
import { nextTick } from 'vue'
import { listDevice, updateDevice, delDevice, pushDevice, getDeviceOnlineStats } from '@/api/sys/device'
import { selectPlatform } from '@/api/sys/platform'
import { selectUpstream, getUpstreamStatus } from '@/api/sys/upstream'
import { deviceTypeApi } from '@/api/sys/businessType'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const deviceList = ref([])
const total = ref(0)
const selectedRows = ref([])
const platformOptions = ref([])
const deviceTypes = ref([])
const onlineStatsMap = ref({})
const upstreamList = ref([])
const upstreamStatusMap = ref({})

const filters = reactive({ keyword: '', type: '', pfCode: '', online: '', sync: '' })
const queryParams = reactive({ pageNum: 1, pageSize: 10, orderByColumn: 'id', isAsc: 'DESC' })

const editOpen = ref(false)
const editLoading = ref(false)
const editForm = ref({})

const syncOpen = ref(false)
const syncLoading = ref(false)
const syncTargets = ref([])
const syncUpstreams = ref([])
const upstreamTableRef = ref()

const delOpen = ref(false)
const delLoading = ref(false)
const delTarget = ref(null)

const totalStats = computed(() => {
  let t = 0, online = 0
  Object.values(onlineStatsMap.value).forEach((s) => {
    t += Number(s.total || 0)
    online += Number(s.onlineCount || 0)
  })
  if (!t && total.value) return { total: total.value, onlineCount: deviceList.value.filter(isOnline).length }
  return { total: t, onlineCount: online }
})

const filteredList = computed(() => {
  if (!filters.sync) return deviceList.value
  return deviceList.value.filter((row) => {
    const synced = isOnline(row) && Number(row.syncSuccess || 0) > 0
    return filters.sync === 'synced' ? synced : !synced
  })
})

function isOnline(row) {
  return row && (row.online === 1 || row.online === '1' || row.online === true)
}
function platformName(code) {
  return platformOptions.value.find((p) => p.code === code)?.name || code || '-'
}
function typeName(code) {
  return deviceTypes.value.find((t) => t.code === code)?.name || code || '-'
}
function displayName(row) {
  if (!row) return '-'
  try {
    const remark = typeof row.remark === 'string' ? JSON.parse(row.remark) : row.remark
    if (remark && remark.displayName) return remark.displayName
  } catch { /* */ }
  return row.name || row.code || '-'
}
function formatTime(v) {
  if (!v) return '-'
  return proxy.parseTime(v) || String(v)
}
function syncBadgeClass(row) {
  if (!row?.syncTotal) return 'off'
  if (isOnline(row) && row.syncSuccess > 0) return 'info'
  return 'off'
}
function pushTypeLabel(t) {
  return { url: 'HTTP', redis: 'Redis', mq: 'MQ', kafka: 'Kafka', mqtt: 'MQTT' }[t] || t || '-'
}
function upstreamAlive(row) {
  const st = upstreamStatusMap.value[row.code] || upstreamStatusMap.value[row.id]
  if (st && typeof st.alive === 'boolean') return st.alive
  return row.status === '1'
}

async function init() {
  const [pf, types, ups, st] = await Promise.all([
    selectPlatform().catch(() => ({ data: [] })),
    deviceTypeApi.select().catch(() => ({ data: [] })),
    selectUpstream({ status: '1' }).catch(() => ({ data: [] })),
    getUpstreamStatus().catch(() => ({ data: [] }))
  ])
  platformOptions.value = pf.data || []
  deviceTypes.value = types.data || []
  upstreamList.value = ups.data || []
  const map = {}
  ;(st.data || []).forEach((s) => {
    if (s.code) map[s.code] = s
    if (s.id) map[s.id] = s
  })
  upstreamStatusMap.value = map
  await loadOnlineStats()
  getList()
}

async function loadOnlineStats() {
  try {
    const res = await getDeviceOnlineStats()
    const map = {}
    ;(res.data || []).forEach((s) => { map[s.pfCode] = s })
    onlineStatsMap.value = map
  } catch { /* */ }
}

function getList() {
  loading.value = true
  const params = {
    ...queryParams,
    type: filters.type || null,
    pfCode: filters.pfCode || null,
    online: filters.online || null,
    params: {
      searchValue: filters.keyword || null
    }
  }
  listDevice(params).then((res) => {
    deviceList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}
function resetQuery() {
  Object.assign(filters, { keyword: '', type: '', pfCode: '', online: '', sync: '' })
  handleQuery()
}

function parseRemark(raw) {
  if (!raw) return {}
  if (typeof raw === 'object') return { ...raw }
  try { return JSON.parse(raw) || {} } catch { return { _raw: String(raw) } }
}

function openEdit(row) {
  const remark = parseRemark(row.remark)
  editForm.value = {
    id: row.id,
    code: row.code,
    name: row.name,
    displayName: remark.displayName || row.name,
    type: row.type,
    model: row.model,
    pfCode: row.pfCode,
    online: row.online,
    longitude: row.longitude,
    latitude: row.latitude,
    remarkText: JSON.stringify(remark, null, 2)
  }
  editOpen.value = true
}

async function submitEdit() {
  editLoading.value = true
  try {
    let remarkObj = parseRemark(editForm.value.remarkText)
    if (editForm.value.displayName) remarkObj.displayName = editForm.value.displayName
    await updateDevice({
      id: editForm.value.id,
      code: editForm.value.code,
      name: editForm.value.name,
      type: editForm.value.type,
      model: editForm.value.model,
      pfCode: editForm.value.pfCode,
      longitude: editForm.value.longitude,
      latitude: editForm.value.latitude,
      remark: JSON.stringify(remarkObj)
    })
    proxy.$modal.msgSuccess('保存成功')
    editOpen.value = false
    getList()
  } finally {
    editLoading.value = false
  }
}

async function openSync(rows) {
  syncTargets.value = rows || []
  syncOpen.value = true
  await nextTick()
  upstreamTableRef.value?.clearSelection?.()
  upstreamList.value.forEach((row) => upstreamTableRef.value?.toggleRowSelection?.(row, true))
}

async function confirmSync() {
  if (!syncTargets.value.length) return
  syncLoading.value = true
  try {
    for (const d of syncTargets.value) {
      await pushDevice({ code: d.code, pfCode: d.pfCode })
    }
    proxy.$modal.msgSuccess(`已提交 ${syncTargets.value.length} 台设备同步`)
    syncOpen.value = false
  } finally {
    syncLoading.value = false
  }
}

function handleDelete(row) {
  delTarget.value = row
  delOpen.value = true
}
async function confirmDelete() {
  delLoading.value = true
  try {
    await delDevice(delTarget.value.id)
    proxy.$modal.msgSuccess('已删除')
    delOpen.value = false
    getList()
    loadOnlineStats()
  } finally {
    delLoading.value = false
  }
}
async function handleBatchDelete() {
  try {
    await proxy.$modal.confirm(`确认删除选中的 ${selectedRows.value.length} 台设备？`)
    await delDevice(selectedRows.value.map((r) => r.id).join(','))
    proxy.$modal.msgSuccess('已删除')
    getList()
    loadOnlineStats()
  } catch { /* */ }
}

init()
</script>

<style scoped>
.page-summary { color: #64748b; font-size: 13px; }
.page-summary b { color: #0f172a; font-weight: 700; }
.page-summary b.ok { color: #16a34a; }
.page-summary b.err { color: #dc2626; }
.gw-tag {
  display: inline-block; padding: 1px 8px; border-radius: 5px; font-size: 12px;
  background: #f1f5f9; color: #475569; border: 1px solid #e2e8f0;
}
.table-foot {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-top: 1px solid #e2e8f0; gap: 12px; flex-wrap: wrap;
}
.table-foot :deep(.pagination-container) {
  margin: 0 !important; padding: 0 !important; border: none !important; height: auto !important;
  box-shadow: none !important;
}
.edit-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 4px 20px;
}
.edit-grid :deep(.el-form-item) { margin-bottom: 14px; min-width: 0; }
.edit-grid :deep(.el-form-item__content) { min-width: 0; }
.edit-grid :deep(.el-input),
.edit-grid :deep(.el-select),
.edit-grid :deep(.el-textarea) { width: 100%; }
.edit-grid :deep(.full), .edit-grid .full { grid-column: 1 / -1; }
.field-hint { margin-top: 4px; font-size: 12px; color: #94a3b8; line-height: 1.4; }
.mt8 { margin-top: 8px; }
.mt12 { margin-top: 12px; }
@media (max-width: 900px) { .edit-grid { grid-template-columns: 1fr; } }
</style>
