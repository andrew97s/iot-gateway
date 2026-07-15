<template>
  <div class="gw-page">
    <!-- 筛选栏 -->
    <div class="gw-card" v-show="showSearch">
      <div class="gw-card-body gw-filter-bar">
        <el-input
          v-model="queryParams.deviceCode"
          clearable
          :placeholder="exactMatch ? '设备编码或位置（精准）' : '设备编码或位置（模糊）'"
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
        <el-switch v-model="exactMatch" active-text="精准" inactive-text="模糊" inline-prompt />
        <el-select v-model="queryParams.type" clearable placeholder="消息类型（全部）" style="width: 150px" @change="handleQuery">
          <el-option v-for="(meta, key) in MSG_TYPES" :key="key" :label="meta.label" :value="key" />
        </el-select>
        <el-select v-model="queryParams.pfCode" clearable filterable placeholder="来源插件（全部）" style="width: 160px" @change="handleQuery">
          <el-option v-for="pf in platformOptions" :key="pf.code" :label="pf.name" :value="pf.code" />
        </el-select>
        <el-select v-model="queryParams.sendStatus" clearable placeholder="推送状态（全部）" style="width: 140px" @change="handleQuery">
          <el-option label="已推送" value="sent" />
          <el-option label="推送失败" value="failed" />
        </el-select>
        <el-date-picker
          v-model="daterangeCreateTime"
          end-placeholder="结束日期"
          range-separator="-"
          start-placeholder="开始日期"
          type="daterange"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
        <el-button icon="Search" type="primary" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <div class="spacer" />
        <el-button :disabled="multiple" icon="Delete" plain type="danger" v-hasPermi="['sys:message:remove']" @click="handleDelete">删除</el-button>
        <el-button icon="WarnTriangleFilled" plain type="danger" v-hasPermi="['sys:message:remove']" @click="handleClear">清空</el-button>
        <el-button icon="Download" plain v-hasPermi="['sys:message:export']" @click="handleExport">导出</el-button>
      </div>
    </div>

    <!-- 消息表格 -->
    <div class="gw-card">
      <div class="gw-card-body no-pad">
        <el-table :data="messageList" v-loading="loading" @selection-change="handleSelectionChange">
          <el-table-column align="center" type="selection" width="40" />
          <el-table-column align="center" label="接收时间" prop="createTime" width="165">
            <template #default="scope">
              <span class="gw-mono gw-muted">{{ scope.row.createTime }}</span>
            </template>
          </el-table-column>
          <el-table-column align="center" label="消息类型" prop="type" width="110">
            <template #default="scope">
              <el-tooltip v-if="isControlType(scope.row.type)" placement="top" content="上级平台经网关对设备发起的反控指令；不产生对上级平台的对外推送。">
                <el-tag type="warning" size="small">{{ getMsgTypeLabel(scope.row.type) }}</el-tag>
              </el-tooltip>
              <el-tag v-else :type="getMsgTypeTag(scope.row.type)" size="small">{{ getMsgTypeLabel(scope.row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="设备编码" prop="deviceCode" min-width="170">
            <template #default="scope">
              <span class="gw-mono" style="font-weight: 600">{{ scope.row.deviceCode || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column align="center" label="来源插件" prop="pfCode" width="130">
            <template #default="scope">
              <el-tag size="small" type="info" effect="plain">{{ platformName(scope.row.pfCode) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column align="center" label="处置完成" prop="results" width="90">
            <template #default="scope">
              <dict-tag :options="sys_yes_no" :value="scope.row.results" />
            </template>
          </el-table-column>
          <el-table-column align="center" label="同步上级平台" prop="sendStatus" width="120">
            <template #default="scope">
              <template v-if="isControlType(scope.row.type)">
                <el-tooltip content="反控指令不经网关对外推送" placement="top">
                  <span class="text-secondary">— 下行消息</span>
                </el-tooltip>
              </template>
              <template v-else>
                <span v-if="!scope.row.sendStatus" class="gw-badge off plain">未推送</span>
                <span v-else-if="scope.row.sendStatus === 'sent'" class="gw-badge ok">已推送</span>
                <span v-else-if="scope.row.sendStatus === 'failed'" class="gw-badge err">
                  失败{{ scope.row.retryCount ? ` (重试${scope.row.retryCount})` : '' }}
                </span>
                <span v-else class="gw-badge info plain">{{ scope.row.sendStatus }}</span>
              </template>
            </template>
          </el-table-column>
          <el-table-column align="center" label="操作" min-width="230" fixed="right">
            <template #default="scope">
              <el-button link type="primary" v-hasPermi="['sys:message:list']" @click="handleView(scope.row)">详情</el-button>
              <el-button
                v-if="!isControlType(scope.row.type)"
                link
                type="primary"
                v-hasPermi="['sys:message:edit']"
                :loading="pushingMap[scope.row.id]"
                @click="handlePush(scope.row)"
              >重推</el-button>
              <el-button
                v-if="!isControlType(scope.row.type)"
                link
                type="info"
                v-hasPermi="['sys:message:list']"
                @click="openPushLogs(scope.row)"
              >推送记录</el-button>
              <el-button link type="danger" v-hasPermi="['sys:message:remove']" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNum" :total="total" @pagination="getList" v-show="total > 0" />
      </div>
    </div>

    <!-- 消息详情对话框 -->
    <el-dialog v-model="open" append-to-body :title="title" width="720px">
      <el-descriptions :column="2" border size="small" class="mb12">
        <el-descriptions-item label="平台">{{ form.pfCode }}</el-descriptions-item>
        <el-descriptions-item label="设备编号">{{ form.deviceCode }}</el-descriptions-item>
        <el-descriptions-item label="消息类型">
          <div class="msg-type-cell">
            <el-tag :type="getMsgTypeTag(form.type)" size="small">{{ getMsgTypeLabel(form.type) }}</el-tag>
<!--            <el-text v-if="isControlType(form.type)" type="info" size="small" class="msg-type-hint">-->
<!--              上级经网关下发至设备的反控，无对外推送-->
<!--            </el-text>-->
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="处置完成">
          <dict-tag :options="sys_yes_no" :value="form.results" />
        </el-descriptions-item>
        <template v-if="!isControlType(form.type)">
          <el-descriptions-item label="推送状态">
            <el-tag v-if="form.sendStatus === 'sent'" type="success" size="small">已推送</el-tag>
            <el-tag v-else-if="form.sendStatus === 'failed'" type="danger" size="small">失败</el-tag>
            <span v-else class="text-secondary">-</span>
          </el-descriptions-item>
          <el-descriptions-item label="重试次数">{{ form.retryCount || 0 }}</el-descriptions-item>
        </template>
<!--        <el-descriptions-item v-else label="推送说明" :span="2">-->
<!--          <el-text type="info" size="small">反控为上级平台经全息网关下发至设备的指令，不产生对业务侧的对外推送，亦无推送状态与推送记录。</el-text>-->
<!--        </el-descriptions-item>-->
        <el-descriptions-item label="接收时间" :span="2">{{ form.createTime }}</el-descriptions-item>
        <el-descriptions-item v-if="!isControlType(form.type)" label="发送时间" :span="2">
          <span v-if="form.sendTime">{{ form.sendTime }}</span>
          <span v-else class="text-secondary">-</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="form.retryTime && !isControlType(form.type)" label="最后重试" :span="2">{{ form.retryTime }}</el-descriptions-item>
        <el-descriptions-item v-if="form.pushUrl && !isControlType(form.type)" label="推送地址" :span="2">
          <span class="push-url">{{ form.pushUrl }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <div class="content-toolbar">
        <span class="content-label">消息内容</span>
        <el-button type="primary" link size="small" icon="DocumentCopy" @click="copyMessageContent">复制</el-button>
      </div>
      <div class="content-body">
        <JsonPretty :data="form.content" show-icon />
      </div>
      <template #footer>
        <el-button
          v-if="!isControlType(form.type)"
          type="primary"
          :loading="pushingMap[form.id]"
          v-hasPermi="['sys:message:edit']"
          @click="handlePush(form)"
        >推送</el-button>
        <el-button @click="cancel">关 闭</el-button>
      </template>
    </el-dialog>

    <!-- 推送记录 -->
    <el-dialog v-model="pushLogOpen" :title="pushLogTitle" width="920px" append-to-body destroy-on-close>
      <el-table :data="pushLogList" v-loading="pushLogLoading" max-height="420" size="small" border stripe>
        <template #empty>
          <el-empty description="暂无推送记录（每次点击「推送」会生成一条）" :image-size="72" />
        </template>
        <el-table-column label="推送方式" prop="type" width="120" align="center">
          <template #default="scope">
            {{ pushModeLabel(scope.row.type) }}
          </template>
        </el-table-column>
        <el-table-column label="接收地址" prop="target" min-width="220" show-overflow-tooltip />
        <el-table-column label="推送时间" prop="time" width="170" align="center" />
        <el-table-column label="状态" prop="status" width="90" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 'success'" type="success" size="small">成功</el-tag>
            <el-tag v-else type="danger" size="small">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="失败原因" prop="failReason" min-width="140" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.failReason">{{ scope.row.failReason }}</span>
            <span v-else class="text-secondary">-</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button type="primary" plain @click="reloadPushLogs" :loading="pushLogLoading">刷 新</el-button>
        <el-button @click="pushLogOpen = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Message">
import JsonPretty from 'vue-json-pretty'
import { watchDebounced } from '@vueuse/core'
import { listMessage, getMessage, delMessage, clearMessage, pushMessage, listMessagePushLogs } from '@/api/sys/message'
import { selectPlatform } from '@/api/sys/platform'

const { proxy } = getCurrentInstance()
const { sys_yes_no } = proxy.useDict('sys_yes_no')

const messageList = ref([])
const open        = ref(false)
const loading     = ref(true)
const showSearch  = ref(true)
const showMore    = ref(false)
const exactMatch  = ref(false)
const ids         = ref([])
const single      = ref(true)
const multiple    = ref(true)
const total       = ref(0)
const title       = ref('')
const daterangeCreateTime = ref([])
const pushingMap  = reactive({})

const pushLogOpen = ref(false)
const pushLogTitle = ref('')
const pushLogList = ref([])
const pushLogLoading = ref(false)
const pushLogMessageId = ref(null)

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderByColumn: 'id',
    isAsc: 'DESC',
    pfCode:     null,
    deviceCode: null,
    name:       null,
    type:       null,
    sendStatus: null,
    results:    null
  }
})
const { queryParams, form } = toRefs(data)

const platformOptions = ref([])
selectPlatform().then(res => { platformOptions.value = res.data })

const MSG_TYPES = {
  alarm:     { label: '告警', tag: 'danger' },
  business:  { label: '监测/业务', tag: 'success' },
  device:    { label: '设备', tag: 'primary' },
  control:   { label: '反控', tag: 'warning' },
  event:     { label: '事件', tag: 'warning' },
  heartbeat: { label: '心跳', tag: 'info' }
}

function isControlType(type) {
  return type === 'control'
}

function getMsgTypeLabel(type) {
  if (!type) return '-'
  return MSG_TYPES[type]?.label || type
}

function getMsgTypeTag(type) {
  return MSG_TYPES[type]?.tag || ''
}

function platformName(pfCode) {
  if (!pfCode) return '-'
  return platformOptions.value.find(p => p.code === pfCode)?.name || pfCode
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

function copyMessageContent() {
  copyPlainText(stringifyForCopy(form.value.content))
}

function getList() {
  loading.value = true
  queryParams.value.params = {}
  if (daterangeCreateTime.value?.length === 2) {
    queryParams.value.params['beginCreateTime'] = daterangeCreateTime.value[0]
    queryParams.value.params['endCreateTime']   = daterangeCreateTime.value[1]
  }
  // 精准匹配标志
  queryParams.value.params['exactMatch'] = exactMatch.value ? '1' : null
  listMessage(queryParams.value).then(res => {
    messageList.value = res.rows
    total.value       = res.total
    loading.value     = false
  })
}

watchDebounced(
  [
    () => queryParams.value.deviceCode,
    () => exactMatch.value,
    () => JSON.stringify(daterangeCreateTime.value ?? [])
  ],
  () => {
    queryParams.value.pageNum = 1
    getList()
  },
  { debounce: 450, maxWait: 2000 }
)

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = { id: null, pfCode: null, deviceCode: null, type: null, content: null, results: null, sendStatus: null, retryCount: 0, pushUrl: null }
  proxy.resetForm('messageRef')
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  daterangeCreateTime.value = []
  exactMatch.value = false
  queryParams.value.deviceCode = null
  queryParams.value.type = null
  queryParams.value.pfCode = null
  queryParams.value.sendStatus = null
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value    = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleView(row) {
  reset()
  getMessage(row.id || ids.value).then(res => {
    form.value = res.data
    try { form.value.content = JSON.parse(form.value.content) } catch {}
    open.value = true
    title.value = '查看消息详情'
  })
}

function pushModeLabel(mode) {
  const m = { mq: 'MQ消息队列', url: 'HTTP(URL)', redis: 'Redis队列', unknown: '未知' }
  return m[mode] || mode || '-'
}

function openPushLogs(row) {
  pushLogMessageId.value = row.id
  pushLogTitle.value = `推送记录 — ${row.deviceCode || row.id}`
  pushLogOpen.value = true
  loadPushLogs(row.id)
}

async function loadPushLogs(messageId) {
  if (!messageId) return
  pushLogLoading.value = true
  try {
    const body = await listMessagePushLogs(messageId)
    pushLogList.value = body.data || []
  } finally {
    pushLogLoading.value = false
  }
}

function reloadPushLogs() {
  if (pushLogMessageId.value) loadPushLogs(pushLogMessageId.value)
}

async function handlePush(row) {
  if (!row?.id) return
  pushingMap[row.id] = true
  try {
    await pushMessage(row.id)
    proxy.$modal.msgSuccess('推送成功')
    row.sendStatus = 'sent'
    row.retryCount = (row.retryCount || 0) + 1
    if (form.value.id === row.id) {
      form.value.sendStatus = 'sent'
      form.value.retryCount = row.retryCount
    }
    if (pushLogOpen.value && pushLogMessageId.value === row.id) {
      await loadPushLogs(row.id)
    }
    getList()
  } catch (e) {
    proxy.$modal.msgError('推送失败，请检查网关推送配置')
    row.sendStatus = 'failed'
    if (form.value.id === row.id) form.value.sendStatus = 'failed'
    if (pushLogOpen.value && pushLogMessageId.value === row.id) {
      await loadPushLogs(row.id)
    }
    getList()
  } finally {
    delete pushingMap[row.id]
  }
}

function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm(proxy.deleteTips)
    .then(() => delMessage(_ids))
    .then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
    .catch(() => {})
}

function handleClear() {
  proxy.$modal.confirm('是否确认清空所有消息记录?')
    .then(() => clearMessage())
    .then(() => { getList(); proxy.$modal.msgSuccess('清空成功') })
    .catch(() => {})
}

function handleExport() {
  proxy.download('sys/message/export', { ...queryParams.value }, `message_${Date.now()}.xlsx`)
}

getList()
</script>

<style scoped>
.text-secondary { color: var(--el-text-color-secondary); font-size: 12px; }
.mb12 { margin-bottom: 12px; }

/* 更多参数展开/收起 */
.more-params-wrap {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  overflow: hidden;
  max-width: 0;
  max-height: 60px;
  opacity: 0;
  vertical-align: middle;
  pointer-events: none;
  transition: max-width 0.35s ease, opacity 0.25s ease;
}
.more-params-wrap.is-open {
  max-width: 800px;
  opacity: 1;
  pointer-events: auto;
}
.more-toggle {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  cursor: pointer;
  font-size: 13px;
  color: var(--el-color-primary);
  user-select: none;
  margin-left: 6px;
  transition: color 0.2s;
}
.more-toggle:hover { color: var(--el-color-primary-light-3); }
.toggle-arrow {
  transition: transform 0.3s ease;
  font-size: 12px;
}
.toggle-arrow.is-open { transform: rotate(180deg); }
.content-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
  margin-bottom: 8px;
}
.content-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
}
.msg-type-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}
.msg-type-hint {
  line-height: 1.4;
  max-width: 100%;
}
.content-body {
  max-height: 240px;
  overflow-y: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  padding: 8px;
  background: var(--el-fill-color-lighter);
}
.push-url {
  font-family: monospace;
  font-size: 12px;
  word-break: break-all;
  color: var(--el-text-color-regular);
}
</style>
