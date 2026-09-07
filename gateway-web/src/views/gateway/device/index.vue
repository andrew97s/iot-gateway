<template>
  <div class="gw-page">
    <div class="page-summary">
      共 <b>{{ totalStats.total }}</b> 台 · 在线 <b class="ok">{{ totalStats.onlineCount }}</b> · 离线
      <b class="err">{{ totalStats.total - totalStats.onlineCount }}</b>
    </div>

    <div class="gw-card">
      <div class="gw-card-body gw-filter-bar">
        <el-input v-model="filters.keyword" clearable placeholder="设备编码 / 名称" style="width: 200px" @keyup.enter="handleQuery" />
        <el-select-v2 v-model="filters.type" :options="deviceTypeOptions" clearable filterable placeholder="全部类型" style="width: 140px" />
        <el-select-v2 v-model="filters.pfCode" :options="platformSelectOptions" clearable filterable placeholder="全部插件" style="width: 160px" />
        <el-select-v2 v-model="filters.online" :options="ONLINE_OPTIONS" clearable placeholder="全部状态" style="width: 120px" />
        <el-select-v2 v-model="filters.sync" :options="SYNC_OPTIONS" clearable placeholder="同步状态（全部）" style="width: 150px" />
        <el-button icon="Search" type="primary" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <div class="spacer" />
        <el-button :disabled="!selectedRows.length" plain @click="openSync(selectedRows)">批量同步至上级平台</el-button>
        <el-button :disabled="!selectedRows.length" plain type="danger" v-hasPermi="['sys:device:remove']" @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>
    </div>

    <div class="gw-card">
      <div class="gw-card-body no-pad">
        <el-table :data="filteredList" v-loading="loading" @selection-change="(s) => (selectedRows = s)">
          <el-table-column align="center" type="selection" width="42" />
          <el-table-column label="设备编码" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="gw-mono" style="font-weight: 600">{{ row.code }}</span>
            </template>
          </el-table-column>
          <el-table-column label="名称" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ displayName(row) }}</template>
          </el-table-column>
          <el-table-column align="center" label="类型" width="180">
            <template #default="{ row }">
              <span >{{ typeName(row.type) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="型号" min-width="110" prop="model" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="gw-muted">{{ row.model || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="归属插件" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ platformName(row.pfCode) }}</template>
          </el-table-column>
          <el-table-column align="center" label="状态" width="90">
            <template #default="{ row }">
              <span class="gw-badge" :class="isOnline(row) ? 'ok' : 'off'">{{ isOnline(row) ? '在线' : '离线' }}</span>
            </template>
          </el-table-column>
          <el-table-column align="center" label="创建时间" width="150">
            <template #default="{ row }">
              <span class="gw-mono gw-muted gw-small">{{ formatTime(row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column align="center" label="最近通讯时间" width="150">
            <template #default="{ row }">
              <el-tooltip :content="formatTimestamp(row.lastCommTime)" placement="top">
                <span class="gw-mono gw-muted gw-small">{{ formatRelativeTime(row.lastCommTime) }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column align="center" label="同步状态" width="130">
            <template #default="{ row }">
              <span class="gw-badge plain" :class="syncBadgeClass(row)">{{ row.syncLabel || '未同步' }}</span>
            </template>
          </el-table-column>
          <el-table-column align="center" fixed="right" label="操作" width="230">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button link type="primary" @click="openEdit(row)">修改</el-button>
              <el-button link type="primary" @click="openSync([row])">同步</el-button>
              <el-button link type="danger" v-hasPermi="['sys:device:remove']" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-foot">
          <span class="gw-muted gw-small">已选 {{ selectedRows.length }} 项 · 共 {{ total }} 条记录</span>
          <pagination
            v-model:limit="queryParams.pageSize"
            v-model:page="queryParams.pageNum"
            :total="total"
            @pagination="getList"
            v-show="total > 0"
          />
        </div>
      </div>
    </div>

    <!-- 修改设备 -->
    <el-dialog v-model="editOpen" append-to-body destroy-on-close :title="`修改设备 · ${editForm.code || ''}`" width="680px">
      <el-form class="edit-grid" label-position="top" :model="editForm">
        <el-form-item label="设备编码" required>
          <el-input v-model="editForm.code" />
          <div class="field-hint">网关内唯一，同步上级平台时作为设备标识</div>
        </el-form-item>
        <el-form-item label="设备名称" required>
          <el-input v-model="editForm.displayName" placeholder="显示名称（写入扩展属性）" />
        </el-form-item>
        <el-form-item label="设备类型" required>
          <el-select-v2
            v-model="editForm.type"
            :options="deviceTypeEditOptions"
            filterable
            :teleported="false"
            placeholder="请选择设备类型"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="editForm.model" />
        </el-form-item>
        <el-form-item label="归属插件">
          <el-input disabled :model-value="`${platformName(editForm.pfCode)} (${editForm.pfCode || '-'})`" />
          <div class="field-hint">由接入来源决定，不可修改</div>
        </el-form-item>
        <el-form-item label="状态">
          <el-input disabled :model-value="isOnline(editForm) ? '在线' : '离线'" />
        </el-form-item>
        <el-form-item class="full" label="扩展属性（JSON）">
          <el-input v-model="editForm.remarkText" :placeholder="jsonPlaceholder" :rows="3" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editOpen = false">取消</el-button>
        <el-button :loading="editLoading" type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 设备详情 -->
    <el-drawer
      v-model="detailOpen"
      destroy-on-close
      direction="rtl"
      size="760px"
      :title="`设备详情 · ${displayName(detailDevice)}`"
      @close="closeDetail"
    >
      <div class="detail-drawer">
        <el-tabs v-model="activeDetailTab" class="detail-tabs" @tab-change="handleDetailTabChange">
          <el-tab-pane label="设备详情" name="detail">
            <section class="detail-section" v-loading="detailLoading">
              <el-descriptions border :column="2">
                <el-descriptions-item label="设备编码">
                  <span class="gw-mono">{{ detailDevice?.code || '-' }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="网关编码">{{ detailDevice?.net || '-' }}</el-descriptions-item>
                <el-descriptions-item label="设备名称">{{ displayName(detailDevice) }}</el-descriptions-item>
                <el-descriptions-item label="设备类型">{{ typeName(detailDevice?.type) }}</el-descriptions-item>
                <el-descriptions-item label="设备型号">{{ detailDevice?.model || '-' }}</el-descriptions-item>
                <el-descriptions-item label="归属插件">{{ platformName(detailDevice?.pfCode) }}</el-descriptions-item>
                <el-descriptions-item label="在线状态">
                  <span class="gw-badge" :class="isOnline(detailDevice) ? 'ok' : 'off'">{{ isOnline(detailDevice) ? '在线' : '离线' }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="IP 地址">{{ detailDevice?.ip || '-' }}</el-descriptions-item>
                <el-descriptions-item label="业务 ID">{{ detailDevice?.bizId || '-' }}</el-descriptions-item>
                <el-descriptions-item label="无线设备">{{ detailDevice?.wireless === '1' ? '是' : '否' }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatTime(detailDevice?.createTime) }}</el-descriptions-item>
                <el-descriptions-item label="最近通讯">
                  <el-tooltip :content="formatTimestamp(detailDevice?.lastCommTime)" placement="top">
                    <span>{{ formatRelativeTime(detailDevice?.lastCommTime) }}</span>
                  </el-tooltip>
                </el-descriptions-item>
              </el-descriptions>
              <div class="extension-block">
                <div class="section-title-row">
                  <div class="section-title">设备扩展信息</div>
                  <el-button icon="DocumentCopy" link type="primary" @click="copyText(deviceExtensionJson, '设备扩展信息')">一键复制</el-button>
                </div>
                <div class="json-view"><JsonPretty :data="deviceExtensionData" show-icon /></div>
              </div>
            </section>
          </el-tab-pane>

          <el-tab-pane label="监测信息" lazy name="telemetry">
            <section class="detail-section">
              <div class="section-title-row">
                <div class="gw-muted gw-small">数据时间：{{ formatTimestamp(telemetryUpdatedAt) }} · 每 5 秒刷新</div>
                <div class="telemetry-actions">
                  <span class="gw-badge" :class="telemetryPollingError ? 'err' : 'ok'">{{ telemetryPollingError ? '刷新失败' : '自动刷新中' }}</span>
                  <el-button icon="Refresh" :loading="telemetryLoading" size="small" @click="loadTelemetry(true)">刷新</el-button>
                </div>
              </div>
              <div class="telemetry-content" v-loading="telemetryLoading && !telemetryItems.length">
                <el-empty description="该设备暂无监测数据" :image-size="72" v-if="!telemetryLoading && !telemetryItems.length" />
                <div class="telemetry-grid" v-else>
                  <div v-for="item in telemetryItems" class="telemetry-card" :class="telemetryState(item)" :key="`${item.code}-${item.channel}`">
                    <div class="telemetry-card-head">
                      <span>{{ telemetryName(item) }}</span>
                      <span class="gw-tag">通道 {{ item.channel || 1 }}</span>
                    </div>
                    <div class="telemetry-value">
                      {{ telemetryValue(item) }}<small v-if="telemetryUnit(item)">{{ telemetryUnit(item) }}</small>
                    </div>
                    <div class="telemetry-meta">
                      <span v-if="item.thresholdLow != null && item.thresholdLow !== ''">下限 {{ item.thresholdLow }}</span>
                      <span v-if="item.thresholdHigh != null && item.thresholdHigh !== ''">上限 {{ item.thresholdHigh }}</span>
                      <span :title="item.desc" v-if="item.desc">{{ item.desc }}</span>
                    </div>
                    <el-tooltip :content="formatTimestamp(item.timestamp)" placement="top">
                      <div class="telemetry-item-time gw-muted">{{ formatRelativeTime(item.timestamp) }}</div>
                    </el-tooltip>
                  </div>
                </div>
              </div>
            </section>
          </el-tab-pane>

          <el-tab-pane label="告警信息" lazy name="alarm">
            <section class="detail-section">
              <div class="section-title-row">
                <div class="gw-muted gw-small">展示该设备最近 20 条告警记录</div>
                <el-button icon="Refresh" :loading="alarmLoading" size="small" @click="loadAlarms(true)">刷新</el-button>
              </div>
              <el-table :data="alarmRecords" max-height="520" size="small" v-loading="alarmLoading && !alarmRecords.length">
                <el-table-column type="expand" width="42">
                  <template #default="{ row }">
                    <div class="alarm-raw">
                      <div class="section-title-row">
                        <span class="gw-medium">厂商原始消息</span>
                        <el-button icon="DocumentCopy" link @click="copyAlarmRaw(row)">复制</el-button>
                      </div>
                      <div class="json-view"><JsonPretty :data="parseJson(row.rawContent)" show-icon /></div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column align="center" label="告警类别" width="90">
                  <template #default="{ row }">
                    <el-tag size="small" :type="alarmCategoryTag(row.category)">{{ alarmCategoryName(row.category) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="告警名称" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.name || row.code || '-' }}</template>
                </el-table-column>

                <el-table-column label="告警描述" min-width="180" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.desc || '-' }}</template>
                </el-table-column>
                <el-table-column label="告警时间" width="160">
                  <template #default="{ row }">
                    <span class="gw-mono gw-small">{{ formatTimestamp(row.timestamp) }}</span>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty description="该设备暂无告警记录" :image-size="72" v-if="!alarmLoading && !alarmRecords.length" />
            </section>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <!-- 同步至上级平台 -->
    <el-dialog v-model="syncOpen" append-to-body destroy-on-close title="同步设备至上级平台" width="720px">
      <p class="gw-muted">
        将所选 <b>{{ syncTargets.length }}</b> 台设备的档案信息以 <span class="gw-tag">DEVICE_EVENT</span> 统一消息推送至以下平台：
      </p>
      <el-table class="mt12" :data="upstreamList" size="small" ref="upstreamTableRef" @selection-change="(s) => (syncUpstreams = s)">
        <el-table-column type="selection" width="42" />
        <el-table-column label="平台" min-width="140" prop="name" />
        <el-table-column align="center" label="推送方式" width="110">
          <template #default="{ row }">
            <span class="gw-tag">{{ pushTypeLabel(row.pushType) }}</span>
          </template>
        </el-table-column>
        <el-table-column align="center" label="连接状态" width="110">
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
        <el-button :disabled="!syncUpstreams.length" :loading="syncLoading" type="primary" @click="confirmSync">立即同步</el-button>
      </template>
    </el-dialog>

    <!-- 删除确认 -->
    <el-dialog v-model="delOpen" append-to-body title="删除设备" width="460px">
      <p>
        确认删除设备 <b class="gw-mono">{{ delTarget?.code }}（{{ displayName(delTarget) }}）</b>？
      </p>
      <p class="gw-muted gw-small mt8">删除后：① 通知归属插件取消该设备订阅；② 向已同步的上级平台推送 DEVICE_EVENT: deleted；③ 历史消息日志保留。</p>
      <template #footer>
        <el-button @click="delOpen = false">取消</el-button>
        <el-button :loading="delLoading" type="danger" @click="confirmDelete">确认删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Device">
import { nextTick, onBeforeUnmount } from 'vue'
import JsonPretty from 'vue-json-pretty'
import {
  listDevice,
  getDevice,
  updateDevice,
  delDevice,
  pushDevice,
  getDeviceOnlineStats,
  getDeviceTelemetry,
  getDeviceAlarms
} from '@/api/sys/device'
import { selectPlatform } from '@/api/sys/platform'
import { selectUpstream, getUpstreamStatus } from '@/api/sys/upstream'
import { deviceTypeApi, monitorTypeApi } from '@/api/sys/businessType'

const { proxy } = getCurrentInstance()
const jsonPlaceholder = '{ "channelNo": 3 }'

const loading = ref(false)
const deviceList = ref([])
const total = ref(0)
const selectedRows = ref([])
const platformOptions = ref([])
const deviceTypes = ref([])
const monitorTypes = ref([])
const onlineStatsMap = ref({})
const upstreamList = ref([])
const upstreamStatusMap = ref({})

const ONLINE_OPTIONS = [
  { value: '1', label: '在线' },
  { value: '0', label: '离线' }
]
const SYNC_OPTIONS = [
  { value: 'synced', label: '已同步' },
  { value: 'unsynced', label: '未同步' }
]

const filters = reactive({ keyword: '', type: undefined, pfCode: undefined, online: undefined, sync: undefined })
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

const detailOpen = ref(false)
const detailLoading = ref(false)
const detailDevice = ref(null)
const activeDetailTab = ref('detail')
const telemetryItems = ref([])
const telemetryUpdatedAt = ref(null)
const telemetryLoading = ref(false)
const telemetryPollingError = ref(false)
const alarmRecords = ref([])
const alarmLoading = ref(false)
let telemetryTimer = null
let relativeTimeTimer = null
let telemetryRequestId = 0
let alarmRequestId = 0
const relativeTimeNow = ref(Date.now())

const ALARM_CATEGORIES = {
  1: { name: '火警', tag: 'danger' },
  2: { name: '预警', tag: 'warning' },
  3: { name: '故障', tag: 'primary' },
  4: { name: '事件', tag: 'info' }
}

const deviceExtensionData = computed(() => parseJson(detailDevice.value?.remark))
const deviceExtensionJson = computed(() => prettyJson(detailDevice.value?.remark))

const totalStats = computed(() => {
  let t = 0,
    online = 0
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

function toSelectOptions(list, valueKey, labelKey) {
  const seen = new Set()
  const options = []
  for (const item of list || []) {
    const value = item?.[valueKey]
    if (value == null || value === '') continue
    const key = String(value)
    if (seen.has(key)) continue
    seen.add(key)
    options.push({ value: key, label: item[labelKey] || key })
  }
  return options
}

const deviceTypeOptions = computed(() => toSelectOptions(deviceTypes.value, 'code', 'name'))
const platformSelectOptions = computed(() => toSelectOptions(platformOptions.value, 'code', 'name'))
const deviceTypeEditOptions = computed(() => {
  const opts = deviceTypeOptions.value
  const current = editForm.value?.type
  if (current != null && current !== '' && !opts.some((o) => o.value === String(current))) {
    return [...opts, { value: String(current), label: typeName(current) }]
  }
  return opts
})

function isOnline(row) {
  return row && (row.online === 1 || row.online === '1' || row.online === true)
}
function platformName(code) {
  return platformOptions.value.find((p) => p.code === code)?.name || code || '-'
}
function typeName(code) {
  return deviceTypes.value.find((t) => String(t.code) === String(code))?.name || code || '-'
}
function displayName(row) {
  if (!row) return '-'
  try {
    const remark = typeof row.remark === 'string' ? JSON.parse(row.remark) : row.remark
    if (remark && remark.displayName) return remark.displayName
  } catch {
    /* */
  }
  return row.name || row.code || '-'
}
function formatTime(v) {
  if (!v) return '-'
  return proxy.parseTime(v) || String(v)
}
function formatTimestamp(v) {
  if (!v) return '-'
  const value = normalizeTimestamp(v)
  return proxy.parseTime(value) || String(v)
}
function normalizeTimestamp(v) {
  if (v == null || v === '') return 0
  if (typeof v === 'number') return v < 100000000000 ? v * 1000 : v
  if (/^\d+$/.test(v)) {
    const n = Number(v)
    return n < 100000000000 ? n * 1000 : n
  }
  const parsed = new Date(String(v).replace(/-/g, '/')).getTime()
  return Number.isFinite(parsed) ? parsed : 0
}
function formatRelativeTime(v) {
  const timestamp = normalizeTimestamp(v)
  if (!timestamp) return '-'
  const seconds = Math.max(0, Math.floor((relativeTimeNow.value - timestamp) / 1000))
  if (seconds < 5) return '刚刚'
  if (seconds < 60) return `${seconds} 秒前`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days} 天前`
  const months = Math.floor(days / 30)
  if (months < 12) return `${months} 个月前`
  return `${Math.floor(months / 12)} 年前`
}
function parseJson(value) {
  if (value == null || value === '') return {}
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch {
    return { raw: String(value) }
  }
}
function prettyJson(value) {
  if (value == null || value === '') return '{}'
  if (typeof value === 'string') {
    try {
      return JSON.stringify(JSON.parse(value), null, 2)
    } catch {
      return value
    }
  }
  return JSON.stringify(value, null, 2)
}
async function copyText(text, label) {
  try {
    await navigator.clipboard.writeText(text)
    proxy.$modal.msgSuccess(`${label}已复制`)
  } catch {
    proxy.$modal.msgError('复制失败，请手动复制')
  }
}
function copyAlarmRaw(row) {
  return copyText(prettyJson(row?.rawContent), '告警原始消息')
}
function alarmCategoryName(category) {
  return ALARM_CATEGORIES[category]?.name || '未分类'
}
function alarmCategoryTag(category) {
  return ALARM_CATEGORIES[category]?.tag || 'info'
}
function monitorType(code) {
  return monitorTypes.value.find((t) => t.code === code)
}
function telemetryName(item) {
  return item.name || monitorType(item.code)?.name || item.code || '未知监测项'
}
function telemetryValue(item) {
  const type = monitorType(item.code)
  if (type?.valueType !== 'enum') return item.value ?? '-'
  try {
    const options = JSON.parse(type.enumOptions || '[]')
    return options.find((option) => String(option.value) === String(item.value))?.label ?? item.value ?? '-'
  } catch {
    return item.value ?? '-'
  }
}
function telemetryUnit(item) {
  return item.unit || monitorType(item.code)?.unit || ''
}
function telemetryState(item) {
  const value = Number(item.value)
  if (!Number.isFinite(value)) return ''
  const low = item.thresholdLow === '' || item.thresholdLow == null ? null : Number(item.thresholdLow)
  const high = item.thresholdHigh === '' || item.thresholdHigh == null ? null : Number(item.thresholdHigh)
  if ((Number.isFinite(low) && value < low) || (Number.isFinite(high) && value > high)) return 'is-alarm'
  return ''
}
function stopTelemetryPolling() {
  if (telemetryTimer) {
    clearInterval(telemetryTimer)
    telemetryTimer = null
  }
  telemetryRequestId++
  telemetryLoading.value = false
}
function closeDetail() {
  stopTelemetryPolling()
  alarmRequestId++
  alarmLoading.value = false
  detailLoading.value = false
}
async function handleDetailTabChange(tab) {
  stopTelemetryPolling()
  if (!detailOpen.value) return
  if (tab === 'telemetry') {
    await loadTelemetry()
    if (detailOpen.value && activeDetailTab.value === 'telemetry') {
      telemetryTimer = setInterval(() => loadTelemetry(), 5000)
    }
  } else if (tab === 'alarm') {
    loadAlarms()
  }
}
async function loadTelemetry(manual = false) {
  const deviceId = detailDevice.value?.id
  if (!deviceId || telemetryLoading.value) return
  const requestId = ++telemetryRequestId
  telemetryLoading.value = true
  try {
    const res = await getDeviceTelemetry(deviceId)
    if (requestId !== telemetryRequestId || !detailOpen.value || detailDevice.value?.id !== deviceId) return
    telemetryItems.value = res.data?.items || []
    telemetryUpdatedAt.value = res.data?.updatedAt || null
    telemetryPollingError.value = false
    if (manual) proxy.$modal.msgSuccess('监测数据已刷新')
  } catch {
    if (requestId === telemetryRequestId && detailOpen.value && detailDevice.value?.id === deviceId) {
      telemetryPollingError.value = true
    }
  } finally {
    if (requestId === telemetryRequestId) telemetryLoading.value = false
  }
}
async function loadAlarms(manual = false) {
  const deviceId = detailDevice.value?.id
  if (!deviceId || alarmLoading.value) return
  const requestId = ++alarmRequestId
  alarmLoading.value = true
  try {
    const res = await getDeviceAlarms(deviceId)
    if (requestId !== alarmRequestId || !detailOpen.value || detailDevice.value?.id !== deviceId) return
    alarmRecords.value = res.data || []
    if (manual) proxy.$modal.msgSuccess('告警记录已刷新')
  } catch {
    if (manual) proxy.$modal.msgError('告警记录刷新失败')
  } finally {
    if (requestId === alarmRequestId) alarmLoading.value = false
  }
}
async function openDetail(row) {
  closeDetail()
  activeDetailTab.value = 'detail'
  detailDevice.value = row
  telemetryItems.value = []
  telemetryUpdatedAt.value = null
  telemetryPollingError.value = false
  alarmRecords.value = []
  detailOpen.value = true
  detailLoading.value = true
  try {
    const res = await getDevice(row.id)
    if (detailOpen.value && detailDevice.value?.id === row.id) {
      detailDevice.value = { ...row, ...(res.data || {}) }
    }
  } catch {
    /* 列表数据仍可用于详情展示 */
  }
  if (!detailOpen.value || detailDevice.value?.id !== row.id) return
  detailLoading.value = false
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
  const [pf, types, monitors, ups, st] = await Promise.all([
    selectPlatform().catch(() => ({ data: [] })),
    deviceTypeApi.select().catch(() => ({ data: [] })),
    monitorTypeApi.select({ status: '1' }).catch(() => ({ data: [] })),
    selectUpstream({ status: '1' }).catch(() => ({ data: [] })),
    getUpstreamStatus().catch(() => ({ data: [] }))
  ])
  platformOptions.value = pf.data || []
  deviceTypes.value = types.data || []
  monitorTypes.value = monitors.data || []
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
    ;(res.data || []).forEach((s) => {
      map[s.pfCode] = s
    })
    onlineStatsMap.value = map
  } catch {
    /* */
  }
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
  listDevice(params)
    .then((res) => {
      deviceList.value = res.rows || []
      total.value = res.total || 0
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}
function resetQuery() {
  Object.assign(filters, { keyword: '', type: undefined, pfCode: undefined, online: undefined, sync: undefined })
  handleQuery()
}

function parseRemark(raw) {
  if (!raw) return {}
  if (typeof raw === 'object') return { ...raw }
  try {
    return JSON.parse(raw) || {}
  } catch {
    return { _raw: String(raw) }
  }
}

function openEdit(row) {
  const remark = parseRemark(row.remark)
  editForm.value = {
    id: row.id,
    code: row.code,
    name: row.name,
    displayName: remark.displayName || row.name,
    type: row.type == null || row.type === '' ? undefined : String(row.type),
    model: row.model,
    pfCode: row.pfCode,
    online: row.online,
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
  } catch {
    /* */
  }
}

init()
relativeTimeTimer = setInterval(() => {
  relativeTimeNow.value = Date.now()
}, 1000)
onBeforeUnmount(() => {
  closeDetail()
  if (relativeTimeTimer) clearInterval(relativeTimeTimer)
})
</script>

<style scoped>
.page-summary {
  color: #64748b;
  font-size: 13px;
}
.page-summary b {
  color: #0f172a;
  font-weight: 700;
}
.page-summary b.ok {
  color: #16a34a;
}
.page-summary b.err {
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
.table-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid #e2e8f0;
  gap: 12px;
  flex-wrap: wrap;
}
.table-foot :deep(.pagination-container) {
  margin: 0 !important;
  padding: 0 !important;
  border: none !important;
  height: auto !important;
  box-shadow: none !important;
}
.edit-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 4px 20px;
}
.edit-grid :deep(.el-form-item) {
  margin-bottom: 14px;
  min-width: 0;
}
.edit-grid :deep(.el-form-item__content) {
  min-width: 0;
}
.edit-grid :deep(.el-input),
.edit-grid :deep(.el-select),
.edit-grid :deep(.el-select-v2),
.edit-grid :deep(.el-textarea) {
  width: 100%;
}
.edit-grid :deep(.full),
.edit-grid .full {
  grid-column: 1 / -1;
}
.field-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.4;
}
.mt8 {
  margin-top: 8px;
}
.mt12 {
  margin-top: 12px;
}
.detail-drawer {
  min-height: 300px;
}
.detail-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}
.detail-section {
  padding-bottom: 22px;
}
.detail-section + .detail-section {
  padding-top: 22px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.extension-block {
  margin-top: 20px;
}
.json-view {
  max-height: 260px;
  overflow: auto;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-light);
  font-size: 12px;
}
.alarm-raw {
  padding: 12px 20px 16px 62px;
}
.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}
.section-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}
.section-title-row .section-title {
  margin-bottom: 0;
}
.telemetry-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.telemetry-content {
  min-height: 180px;
}
.telemetry-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.telemetry-card {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}
.telemetry-card.is-alarm {
  border-color: var(--el-color-danger-light-5);
  background: var(--el-color-danger-light-9);
}
.telemetry-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}
.telemetry-value {
  margin: 12px 0 8px;
  overflow: hidden;
  color: #0f172a;
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.telemetry-card.is-alarm .telemetry-value {
  color: #dc2626;
}
.telemetry-value small {
  margin-left: 5px;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
}
.telemetry-meta {
  display: flex;
  min-height: 18px;
  gap: 8px;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}
.telemetry-meta span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
}
.telemetry-item-time {
  margin-top: 8px;
  font-size: 11px;
}
@media (max-width: 900px) {
  .edit-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 700px) {
  .telemetry-grid {
    grid-template-columns: 1fr;
  }
}
</style>
