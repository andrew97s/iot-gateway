<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :inline="true" label-width="68px" :model="queryParams" ref="queryRef" v-show="showSearch">
      <el-form-item label="名称" prop="name">
        <el-input v-model="queryParams.name" clearable placeholder="请输入名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="代码" prop="code">
        <el-input v-model="queryParams.code" clearable placeholder="请输入代码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="请选择状态">
          <el-option v-for="dict in sys_status" :label="dict.label" :value="dict.value" :key="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" type="primary" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
<!--      <el-form-item>-->
<!--        <el-button icon="Refresh" plain @click="refreshStats" :loading="statsLoading">刷新状态</el-button>-->
<!--      </el-form-item>-->

    </el-form>

    <!-- 平台卡片列表 -->
    <div class="platform-cards" v-loading="loading">
      <el-empty v-if="!loading && platformList.length === 0" description="暂无平台配置" />
      <div v-for="row in platformList" :key="row.id" class="platform-card">
        <el-card shadow="hover" :class="['card-item', getCardClass(row)]">
          <div class="card-header">
            <div class="card-title-row">
              <span class="card-name">{{ row.name }}</span>
<!--              <el-tag class="card-code" size="small" type="info">{{ row.code }}</el-tag>-->
              <!-- 插件分类标签 -->
              <el-tag :type="getCategoryTagType(row.code)" size="small" effect="plain">
                {{ getCategoryLabel(row.code) }}
              </el-tag>
            </div>
            <div class="card-status-row">
              <!-- 运行状态 -->
              <el-tag :type="getRunningTagType(row)" size="small">
                <el-icon v-if="row.status === '1' && row.running === '1'"><CircleCheck /></el-icon>
                <el-icon v-else-if="row.status === '1' && row.running !== '1'"><CircleClose /></el-icon>
                <el-icon v-else><CircleClose /></el-icon>
                {{ getRunningLabel(row) }}
              </el-tag>
              <!-- 插件注册状态 -->
              <el-tag v-if="statsMap[row.code]" :type="statsMap[row.code].registered ? 'primary' : 'warning'" size="small" class="ml4">
                {{ statsMap[row.code].registered ? statsMap[row.code].protocol || '已注册' : '无插件' }}
              </el-tag>
            </div>
          </div>

          <div class="card-body">
<!--            <div class="info-row">-->
<!--              <el-icon><Connection /></el-icon>-->
<!--              <span class="info-label">地址</span>-->
<!--              <span class="info-value">{{ row.ip || '-' }}{{ row.port ? ':' + row.port : '' }}</span>-->
<!--            </div>-->
            <div class="info-row" v-if="statsMap[row.code]">
              <el-icon><Timer /></el-icon>
              <span class="info-label">最近启动</span>
              <span class="info-value">{{ formatTime(statsMap[row.code].lastStartTime) }}</span>
            </div>
            <div class="info-row" v-if="statsMap[row.code]">
              <el-icon><DataLine /></el-icon>
              <span class="info-label">消息/错误</span>
              <span class="info-value">
                <el-text type="success">{{ statsMap[row.code].msgCount }}</el-text>
                /
                <el-text :type="statsMap[row.code].errCount > 0 ? 'danger' : 'info'">{{ statsMap[row.code].errCount }}</el-text>
              </span>
            </div>
            <div class="info-row" v-if="statsMap[row.code]?.connectionInfo && !['已断开','运行中','推送中','未配置'].includes(statsMap[row.code]?.connectionInfo)">
              <el-icon><InfoFilled /></el-icon>
              <span class="info-label">连接</span>
              <span class="info-value text-ellipsis">{{ statsMap[row.code].connectionInfo }}</span>
            </div>
          </div>

          <div class="card-footer">
            <div class="footer-left">
              <!-- 仅启用/禁用控制插件启停；运行态以标签展示 -->
              <el-tooltip :content="row.status === '1' ? '点击禁用将停止插件' : '点击启用将启动插件'" placement="top">
                <el-switch
                  v-model="row.status"
                  active-value="1"
                  inactive-value="0"
                  :loading="statusLoadingMap[row.code]"
                  active-text="启用"
                  inactive-text="禁用"
                  inline-prompt
                  @change="toggleStatus(row)"
                />
              </el-tooltip>
            </div>
            <div class="card-actions">
              <el-button size="small" icon="Edit" @click="handleUpdate(row)" v-hasPermi="['sys:platform:edit']">配置</el-button>
<!--              <el-button size="small" icon="ChatLineSquare" @click="handleViewMessages(row)">消息</el-button>-->
              <el-button size="small" icon="Document" @click="handleViewLogs(row)">日志</el-button>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNum" :total="total" @pagination="getList" v-show="total > 0" />

    <!-- 修改配置对话框 -->
    <el-dialog v-model="open" append-to-body :title="title" width="720px">
      <el-form label-width="100px" :model="form" :rules="rules" ref="platformRef">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称">
              <el-text>{{ form.name }}</el-text>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="代码">
              <el-tag>{{ form.code }}</el-tag>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="心跳接口" prop="apis">
          <el-input v-model="form.apis" placeholder="心跳/健康检查接口路径（可选）" />
        </el-form-item>

        <!-- 扩展配置：仅展示接口返回的 configObject 中已有的键；有 schema 则按类型渲染，否则文本 -->
        <template v-if="extensionConfigEntries.length > 0">
          <el-divider content-position="left"><span class="divider-label">扩展配置</span></el-divider>
          <el-alert
            v-if="currentSchema.length === 0"
            type="info"
            :closable="false"
            show-icon
            class="mb8"
            title="当前插件未提供参数元数据（configSchema），以下字段以文本方式编辑并写入配置 JSON。"
          />
          <el-form-item
            v-for="entry in extensionConfigEntries"
            :key="entry.key"
            :prop="'configObject.' + entry.key"
          >
            <template #label>
              <span v-if="entry.field" class="param-label">
                <span>{{ paramName(entry.field) }}</span>
                <el-tooltip
                  v-if="paramDesc(entry.field)"
                  :content="paramDesc(entry.field)"
                  placement="top"
                  effect="dark"
                  :show-after="200"
                >
                  <el-icon class="param-desc-icon" tabindex="-1" aria-label="参数说明"><QuestionFilled /></el-icon>
                </el-tooltip>
              </span>
              <span v-else>{{ entry.key }}</span>
            </template>

            <template v-if="entry.field">
              <el-select
                v-if="normalizedParamType(entry.field) === 'select'"
                v-model="form.configObject[entry.key]"
                :placeholder="entry.field.placeholder || '请选择' + paramName(entry.field)"
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="opt in (entry.field.options || [])"
                  :key="String(opt.value)"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>

              <el-switch
                v-else-if="normalizedParamType(entry.field) === 'boolean'"
                v-model="form.configObject[entry.key]"
              />

              <el-input-number
                v-else-if="normalizedParamType(entry.field) === 'integer'"
                v-model="form.configObject[entry.key]"
                :min="entry.field.min != null ? Number(entry.field.min) : undefined"
                :max="entry.field.max != null ? Number(entry.field.max) : undefined"
                :step="entry.field.step != null ? Number(entry.field.step) : 1"
                :precision="0"
                style="width: 100%"
              />

              <el-input-number
                v-else-if="normalizedParamType(entry.field) === 'number'"
                v-model="form.configObject[entry.key]"
                :min="entry.field.min != null ? Number(entry.field.min) : undefined"
                :max="entry.field.max != null ? Number(entry.field.max) : undefined"
                :step="entry.field.step != null ? Number(entry.field.step) : undefined"
                style="width: 100%"
              />

              <el-date-picker
                v-else-if="normalizedParamType(entry.field) === 'date'"
                v-model="form.configObject[entry.key]"
                type="date"
                :value-format="entry.field.valueFormat || entry.field.format || 'YYYY-MM-DD'"
                :placeholder="entry.field.placeholder || '选择日期'"
                style="width: 100%"
              />

              <el-date-picker
                v-else-if="normalizedParamType(entry.field) === 'datetime'"
                v-model="form.configObject[entry.key]"
                type="datetime"
                :value-format="entry.field.valueFormat || entry.field.format || 'YYYY-MM-DD HH:mm:ss'"
                :placeholder="entry.field.placeholder || '选择日期时间'"
                style="width: 100%"
              />

              <el-input
                v-else-if="normalizedParamType(entry.field) === 'textarea'"
                v-model="form.configObject[entry.key]"
                type="textarea"
                :rows="entry.field.rows != null ? Number(entry.field.rows) : 3"
                :placeholder="entry.field.placeholder || ''"
              />

              <el-input
                v-else-if="normalizedParamType(entry.field) === 'password'"
                v-model="form.configObject[entry.key]"
                type="password"
                show-password
                :placeholder="entry.field.placeholder || ''"
              />

              <el-input
                v-else
                v-model="form.configObject[entry.key]"
                :placeholder="entry.field.placeholder || ''"
              />

              <div v-if="subLabelHint(entry.field)" class="field-remark">{{ subLabelHint(entry.field) }}</div>
            </template>

            <el-input
              v-else
              v-model="form.configObject[entry.key]"
              :placeholder="'请输入 ' + entry.key"
            />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="扩展配置">
            <el-text type="info" size="small">暂无配置项</el-text>
          </el-form-item>
        </template>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">保 存</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 平台日志抽屉 -->
    <el-drawer v-model="logDrawerOpen" :title="logDrawerTitle" direction="rtl" size="600px">
      <div class="log-drawer-content">
        <el-descriptions v-if="currentStats" :column="2" border size="small" class="mb16">
          <el-descriptions-item label="协议">{{ currentStats.protocol || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ currentStats.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="运行状态">
            <el-tag :type="currentStats.alive ? 'success' : 'danger'" size="small">
              {{ currentStats.alive ? '正常运行' : '已断开' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="连接信息">{{ currentStats.connectionInfo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近启动">{{ formatTime(currentStats.lastStartTime) }}</el-descriptions-item>
          <el-descriptions-item label="最近停止">{{ formatTime(currentStats.lastStopTime) }}</el-descriptions-item>
          <el-descriptions-item label="处理消息数">
            <el-text type="success" size="large">{{ currentStats.msgCount }}</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="错误次数">
            <el-text :type="currentStats.errCount > 0 ? 'danger' : 'success'" size="large">{{ currentStats.errCount }}</el-text>
          </el-descriptions-item>
        </el-descriptions>

        <div class="log-header">
          <span class="log-title">运行日志</span>
          <div class="log-header-actions">
            <el-button size="small" icon="Refresh" @click="loadPlatformLogs(currentPlatformCode, 1)" :loading="logLoading">刷新</el-button>
            <el-button
              size="small"
              type="danger"
              plain
              icon="Delete"
              :loading="logClearLoading"
              v-hasPermi="['sys:platform:edit']"
              @click="handleClearPlatformLogs"
            >清除</el-button>
          </div>
        </div>
        <div v-loading="logLoading">
          <el-empty v-if="!logLoading && platformLogs.length === 0" description="暂无日志" :image-size="80" />
          <div v-for="log in platformLogs" :key="log.id" class="log-item" :class="getLogClass(log)">
            <div class="log-item-header">
              <el-tag :type="getLogTagType(log)" size="small">{{ getLogTypeLabel(log) }}</el-tag>
              <span class="log-time">{{ log.createTime }}</span>
            </div>
            <div class="log-item-title">
              {{ log.title }}
              <div v-if="log.content" class="log-item-actions">
                <el-button size="small" link type="primary" @click="openLogDetail(log)">查看详细</el-button>
              </div>
            </div>
<!--            <div v-if="log.content" class="log-item-preview">{{ logContentPreview(log.content) }}</div>-->

          </div>
        </div>
        <div class="msg-pagination" v-if="logTotal > 0">
          <el-pagination
            v-model:current-page="logPageNum"
            :page-size="logPageSize"
            :total="logTotal"
            layout="total, prev, pager, next"
            small
            @current-change="onLogPageChange"
          />
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="logDetailOpen" :title="logDetailTitle" width="680px" append-to-body destroy-on-close>
      <pre class="log-detail-pre">{{ logDetailContent }}</pre>
    </el-dialog>

    <!-- 平台消息记录抽屉 -->
    <el-drawer v-model="msgDrawerOpen" :title="msgDrawerTitle" direction="rtl" size="700px">
      <div class="msg-drawer-content">
        <div class="msg-header">
          <el-text size="small" type="info">共 {{ msgTotal }} 条消息记录</el-text>
          <el-button size="small" icon="Refresh" @click="loadPlatformMessages(currentPlatformCode)" :loading="msgLoading">刷新</el-button>
        </div>
        <div v-loading="msgLoading">
          <el-empty v-if="!msgLoading && msgList.length === 0" description="暂无消息记录" :image-size="80" />
          <div v-for="msg in msgList" :key="msg.id" class="msg-item" :class="'msg-type-' + (msg.type || 'default')">
            <div class="msg-item-header">
              <div class="msg-tags">
                <el-tag size="small" :type="getMsgTypeTag(msg.type)" effect="light">{{ msg.type || '未知' }}</el-tag>
                <el-tag size="small" v-if="msg.sendStatus" :type="msg.sendStatus === 'sent' ? 'success' : 'danger'" effect="light">
                  {{ msg.sendStatus === 'sent' ? '已推送' : '推送失败' }}
                </el-tag>
                <el-tag size="small" v-if="msg.results === 'Y'" type="success" effect="plain">已处置</el-tag>
              </div>
              <span class="msg-time">{{ msg.createTime }}</span>
            </div>
            <div class="msg-device-row">
              <span class="msg-device-code">{{ msg.deviceCode || '-' }}</span>
              <span v-if="msg.sendTime" class="msg-send-time">推送: {{ msg.sendTime }}</span>
              <span v-if="msg.retryCount" class="msg-retry-count">重试 {{ msg.retryCount }} 次</span>
            </div>
            <div v-if="msg.sendStatus === 'failed'" class="msg-action-row">
              <el-button size="small" type="warning" plain @click="handleRetryMsg(msg)" :loading="retryingMsgMap[msg.id]">重试推送</el-button>
            </div>
          </div>
        </div>
        <div class="msg-pagination" v-if="msgTotal > 0">
          <el-pagination
            v-model:current-page="msgPageNum"
            :page-size="10"
            :total="msgTotal"
            layout="total, prev, pager, next"
            small
            @current-change="onMsgPageChange"
          />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="Platform">
import { CircleCheck, CircleClose, Timer, DataLine, InfoFilled, QuestionFilled } from '@element-plus/icons-vue'
import {
  listPlatform, getPlatform, delPlatform, addPlatform, updatePlatform,
  getPlatformStats, getAllPlatformStats, getPlatformLogs, clearPlatformLogs
} from '@/api/sys/platform'
import { listMessage } from '@/api/sys/message'
import { retryMessage } from '@/api/sys/message'

const { proxy } = getCurrentInstance()
const { sys_status } = proxy.useDict('sys_status')

const platformList  = ref([])
const open          = ref(false)
const loading       = ref(true)
const showSearch    = ref(true)
const ids           = ref([])
const single        = ref(true)
const multiple      = ref(true)
const total         = ref(0)
const title         = ref('')

const statusLoadingMap  = ref({})
const statsMap      = ref({})
const statsLoading  = ref(false)
const currentSchema = ref([])  // 当前平台的 configSchema（每项含 code/name/desc/type/defaultValue 等）

function paramCode(field) {
  if (!field || typeof field !== 'object') return ''
  const c = field.code != null ? field.code : field.key
  return c == null ? '' : String(c)
}
function paramName(field) {
  if (!field) return ''
  const n = field.name != null ? field.name : field.label
  if (n != null && String(n) !== '') return String(n)
  const c = paramCode(field)
  return c || '参数'
}
function paramDesc(field) {
  if (!field) return ''
  const d = field.desc != null ? field.desc : field.description
  return d == null || d === '' ? '' : String(d)
}
function subLabelHint(field) {
  if (!field || paramDesc(field)) return ''
  if (field.remark != null && String(field.remark) !== '') return String(field.remark)
  return ''
}
function normalizedParamType(field) {
  let t = (field && field.type != null ? field.type : 'string').toString().toLowerCase()
  if (t === 'switch') return 'boolean'
  if (t === 'text') return 'string'
  return t || 'string'
}
function applySchemaDefaults(schema, configObject) {
  if (!schema?.length || !configObject || typeof configObject !== 'object') return
  for (const field of schema) {
    const code = paramCode(field)
    if (!code || !Object.prototype.hasOwnProperty.call(configObject, code)) continue
    const cur = configObject[code]
    const empty = cur === undefined || cur === null || cur === ''
    if (!empty || field.defaultValue === undefined || field.defaultValue === null) continue
    const dv = field.defaultValue
    configObject[code] = typeof dv === 'object' ? JSON.parse(JSON.stringify(dv)) : dv
  }
}
function rebuildFormRules() {
  const next = {}
  const co = form.value?.configObject
  if (co && typeof co === 'object') {
    for (const field of currentSchema.value || []) {
      const code = paramCode(field)
      if (!code || !field.required || !Object.prototype.hasOwnProperty.call(co, code)) continue
      next['configObject.' + code] = [
        { required: true, message: (paramName(field) || '该项') + '不能为空', trigger: 'change' }
      ]
    }
  }
  data.rules = next
}
/** 仅根据 configObject 已有键展示；与 schema 按 code 匹配决定控件类型 */
const extensionConfigEntries = computed(() => {
  const obj = form.value?.configObject
  if (!obj || typeof obj !== 'object') return []
  const schema = currentSchema.value || []
  return Object.keys(obj)
    .sort()
    .map(key => ({
      key,
      field: schema.find(f => paramCode(f) === key) || null
    }))
})

// 日志抽屉
const logDrawerOpen       = ref(false)
const logDrawerTitle      = ref('')
const currentPlatformCode = ref('')
const currentStats        = ref(null)
const platformLogs        = ref([])
const logLoading          = ref(false)
const logTotal            = ref(0)
const logPageNum          = ref(1)
const logPageSize         = ref(10)
const logClearLoading     = ref(false)
const logDetailOpen       = ref(false)
const logDetailTitle      = ref('')
const logDetailContent    = ref('')
const msgDrawerOpen  = ref(false)
const msgDrawerTitle = ref('')
const msgList        = ref([])
const msgLoading     = ref(false)
const msgTotal       = ref(0)
const msgPageNum     = ref(1)
const retryingMsgMap = ref({})

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 20,
    orderByColumn: 'id',
    isAsc: 'DESC',
    name: null,
    code: null,
    status: null
  },
  rules: {}
})
const { queryParams, form, rules } = toRefs(data)

// ==================== 分类辅助 ====================
const CATEGORY_MAP = {
  gateway:          { label: '消息推送', type: 'warning' },
  'cascade':        { label: '级联同步', type: '' },
  'cascade-server': { label: '级联服务', type: '' }
}
function getCategoryLabel(code) {
  return CATEGORY_MAP[code]?.label ?? '数据接入'
}
function getCategoryTagType(code) {
  return CATEGORY_MAP[code]?.type ?? 'success'
}

// ==================== 样式辅助 ====================
function getCardClass(row) {
  if (row.status !== '1') return 'card-disabled'
  if (row.running === '1') return 'card-running'
  return 'card-stopped'
}
function getRunningTagType(row) {
  if (row.status !== '1') return 'info'
  return row.running === '1' ? 'success' : 'danger'
}
function getRunningLabel(row) {
  if (row.status !== '1') return '已禁用'
  return row.running === '1' ? '运行中' : '已停止'
}
function normalizeLogType(t) {
  if (t === null || t === undefined) return ''
  return String(t)
}
function getLogClass(log) {
  const t = normalizeLogType(log.type)
  if (t === '1' && log.content && String(log.content).includes('启动结果：失败')) return 'log-error'
  if (t === '1' ) return 'log-event'
  if (t === '4') return 'log-error'
  return 'log-warn'
}
function getLogTagType(log) {
  const t = normalizeLogType(log.type)
  if (t === '1' && log.content && String(log.content).includes('启动结果：失败')) return 'danger'
  if (t === '1') return 'success'
  if (t === '2') return 'info'
  if (t === '4') return 'danger'
  return 'info'
}
function getLogTypeLabel(log) {
  const map = { '1': '启动', '2': '停止', '3': '配置更新', '4': '运行异常', '5': '其它' }
  return map[normalizeLogType(log.type)] || normalizeLogType(log.type) || '日志'
}
function getMsgTypeTag(type) {
  const m = { alarm: 'danger', event: 'warning', device: 'primary', heartbeat: 'info' }
  return m[type] || ''
}
function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN', { hour12: false })
}

// ==================== 数据加载 ====================
async function getList() {
  loading.value = true
  const response = await listPlatform(queryParams.value)
  platformList.value = response.rows
  total.value        = response.total
  loading.value      = false
  loadAllStats()
}

async function loadAllStats() {
  try {
    statsLoading.value = true
    const res = await getAllPlatformStats()
    const map = {}
    if (Array.isArray(res.data)) {
      res.data.forEach(s => { map[s.platformCode] = s })
    }
    statsMap.value = map
  } catch (e) {
  } finally {
    statsLoading.value = false
  }
}

async function refreshStats() {
  await loadAllStats()
  proxy.$modal.msgSuccess('状态已刷新')
}

async function loadPlatformLogs(code, page) {
  logLoading.value = true
  const pageNum = page || logPageNum.value
  try {
    const res = await getPlatformLogs(code, {
      pageNum,
      pageSize: logPageSize.value,
      orderByColumn: 'create_time',
      isAsc: 'desc'
    })
    platformLogs.value = res.rows || []
    logTotal.value = res.total || 0
    logPageNum.value = pageNum
  } finally {
    logLoading.value = false
  }
}

function onLogPageChange(page) {
  logPageNum.value = page
  loadPlatformLogs(currentPlatformCode.value, page)
}

function logContentPreview(content) {
  if (content == null || content === '') return ''
  const s = String(content)
  return s.length <= 200 ? s : `${s.slice(0, 200)}…`
}

function openLogDetail(log) {
  logDetailTitle.value = log.title || '日志详情'
  logDetailContent.value = log.content != null ? String(log.content) : ''
  logDetailOpen.value = true
}

async function handleClearPlatformLogs() {
  try {
    await proxy.$modal.confirm('确定清空当前插件的全部运行日志？此操作不可恢复。')
    logClearLoading.value = true
    await clearPlatformLogs(currentPlatformCode.value)
    proxy.$modal.msgSuccess('已清空')
    await loadPlatformLogs(currentPlatformCode.value, 1)
  } catch (e) {
    if (e !== 'cancel') {
      proxy.$modal.msgError('清除失败')
    }
  } finally {
    logClearLoading.value = false
  }
}

async function loadPlatformMessages(code, page) {
  msgLoading.value = true
  const pageNum = page || msgPageNum.value
  try {
    const res = await listMessage({ pfCode: code, pageNum, pageSize: 10, orderByColumn: 'id', isAsc: 'DESC' })
    msgList.value  = res.rows  || []
    msgTotal.value = res.total || 0
  } finally {
    msgLoading.value = false
  }
}

function onMsgPageChange(page) {
  msgPageNum.value = page
  loadPlatformMessages(currentPlatformCode.value, page)
}

// ==================== 启用 / 禁用（后端负责启停插件） ====================
async function toggleStatus(row) {
  const text = row.status === '1' ? '启用' : '禁用'
  try {
    await proxy.$modal.confirm(`确认${text} [${row.name}]？启用后将启动插件，禁用后将停止插件。`)
    statusLoadingMap.value[row.code] = true
    await updatePlatform({ id: row.id, code: row.code, status: row.status })
    proxy.$modal.msgSuccess(text + '成功')
    await getList()
    await loadAllStats()
  } catch (e) {
    row.status = row.status === '1' ? '0' : '1'
  } finally {
    statusLoadingMap.value[row.code] = false
  }
}

// ==================== 日志/消息抽屉 ====================
async function handleViewLogs(row) {
  currentPlatformCode.value = row.code
  logDrawerTitle.value = `${row.name} — 运行日志`
  currentStats.value = statsMap.value[row.code] || null
  platformLogs.value = []
  logTotal.value = 0
  logPageNum.value = 1
  logDrawerOpen.value = true
  await loadPlatformLogs(row.code, 1)
  const statsRes = await getPlatformStats(row.code)
  currentStats.value = statsRes.data
}

async function handleViewMessages(row) {
  currentPlatformCode.value = row.code
  msgDrawerTitle.value = `${row.name} — 消息记录`
  msgList.value  = []
  msgTotal.value = 0
  msgPageNum.value = 1
  msgDrawerOpen.value = true
  await loadPlatformMessages(row.code, 1)
}

async function handleRetryMsg(msg) {
  retryingMsgMap.value[msg.id] = true
  try {
    await retryMessage(msg.id)
    proxy.$modal.msgSuccess('重试已提交')
    msg.sendStatus = 'sent'
  } catch (e) {
    proxy.$modal.msgError('重试失败')
  } finally {
    retryingMsgMap.value[msg.id] = false
  }
}

// ==================== CRUD ====================
function cancel() {
  open.value = false
  reset()
}
function reset() {
  form.value = {
    id: null, name: null, code: null, ip: null, port: null,
    apis: null, config: null, configObject: {}, remark: null
  }
  currentSchema.value = []
  data.rules = {}
  proxy.resetForm('platformRef')
}
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}
function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}
function handleSelectionChange(selection) {
  ids.value    = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getPlatform(_id).then(response => {
    const d = response.data || {}
    form.value = {
      ...d,
      configObject: d.configObject || (d.config ? safeParseJson(d.config) : {})
    }
    if (!form.value.configObject || typeof form.value.configObject !== 'object') {
      form.value.configObject = {}
    }
    const stats = statsMap.value[d.code]
    currentSchema.value = stats?.configSchema || []
    applySchemaDefaults(currentSchema.value, form.value.configObject)
    rebuildFormRules()
    open.value = true
    title.value = '修改平台配置 — ' + d.name
  })
}
function applyHostPortColumnsFromConfigObject(formVal) {
  const o = formVal.configObject || {}
  if (Object.prototype.hasOwnProperty.call(o, 'ip')) {
    const v = o.ip
    formVal.ip = v === undefined || v === null || String(v).trim() === '' ? null : String(v).trim()
    delete o.ip
  }
  if (Object.prototype.hasOwnProperty.call(o, 'port')) {
    const p = o.port
    if (p === undefined || p === null || p === '') {
      formVal.port = null
    } else {
      const n = typeof p === 'number' ? p : parseInt(String(p), 10)
      formVal.port = Number.isFinite(n) ? n : null
    }
    delete o.port
  }
}
function safeParseJson(str) {
  try { return JSON.parse(str) } catch { return {} }
}
function submitForm() {
  proxy.$refs['platformRef'].validate(valid => {
    if (!valid) return
    applyHostPortColumnsFromConfigObject(form.value)
    form.value.config = JSON.stringify(form.value.configObject || {})
    if (form.value.id != null) {
      updatePlatform(form.value).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addPlatform(form.value).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm(proxy.deleteTips)
    .then(() => delPlatform(_ids))
    .then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
    .catch(() => {})
}
function handleExport() {
  proxy.download('sys/platform/export', { ...queryParams.value }, `platform_${Date.now()}.xlsx`)
}

getList()
</script>

<style scoped>
/* 卡片网格布局 */
.platform-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
  min-height: 120px;
  align-items: stretch;
}
.platform-card { min-width: 0; height: 100%; display: flex; flex-direction: column; }
.card-item {
  border-radius: 8px;
  transition: box-shadow 0.2s;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.card-item :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  flex: 1;
}
.card-item.card-running  { border-left: 4px solid #67c23a; }
.card-item.card-stopped  { border-left: 4px solid #f56c6c; }
.card-item.card-disabled { border-left: 4px solid #909399; opacity: 0.75; }

/* 卡片头部 */
.card-header { margin-bottom: 12px; }
.card-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}
.card-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.card-code { font-family: monospace; }
.card-status-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.ml4 { margin-left: 4px; }
.ml8 { margin-left: 8px; }

/* 卡片信息行 */
.card-body { margin-bottom: 12px; flex: 1; }
.info-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin-bottom: 4px;
}
.info-label {
  color: var(--el-text-color-secondary);
  min-width: 52px;
}
.info-value {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.text-ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 卡片底部 */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.footer-left {
  display: flex;
  align-items: center;
  gap: 4px;
}
.card-actions { display: flex; gap: 6px; flex-wrap: wrap; }

/* 配置编辑器 */
.config-editor {
  width: 100%;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 4px;
  min-height: 120px;
}
.divider-label { font-size: 13px; font-weight: 500; color: var(--el-text-color-secondary); }
.field-remark { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 2px; }
.mb8 { margin-bottom: 8px; }

.param-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  vertical-align: middle;
}
.param-desc-icon {
  cursor: help;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
.param-desc-icon:focus { outline: none; }

/* 日志抽屉 */
.log-drawer-content { padding: 0 4px; }
.mb16 { margin-bottom: 16px; }
.log-header, .msg-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.log-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.log-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.log-item-preview {
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-top: 6px;
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.45;
  max-height: 4.6em;
  overflow: hidden;
}
.log-item-actions {
  margin-top: 4px;
}
.log-detail-pre {
  margin: 0;
  padding: 8px 4px;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 65vh;
  overflow: auto;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
.log-item {
  padding: 10px 12px;
  margin-bottom: 8px;
  border-radius: 6px;
  border-left: 4px solid transparent;
  background: var(--el-fill-color-lighter);
}
.log-item.log-event  { border-left-color: #67c23a; }
.log-item.log-error  { border-left-color: #f56c6c; background: #fff0f0; }
.log-item.log-warn   { border-left-color: #e6a23c; }
.log-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.log-time, .msg-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.log-item-title {
  font-size: 13px;
  color: var(--el-text-color-primary);
}
.log-item-error {
  font-size: 12px;
  color: #f56c6c;
  margin-top: 4px;
  word-break: break-all;
}

/* 消息抽屉 */
.msg-drawer-content { padding: 0 4px; display: flex; flex-direction: column; height: 100%; }
.msg-item {
  padding: 10px 14px;
  margin-bottom: 8px;
  border-radius: 6px;
  border-left: 4px solid var(--el-border-color);
  background: var(--el-bg-color);
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.msg-item.msg-type-alarm   { border-left-color: #f56c6c; }
.msg-item.msg-type-event   { border-left-color: #e6a23c; }
.msg-item.msg-type-device  { border-left-color: #409eff; }
.msg-item.msg-type-heartbeat { border-left-color: #909399; }
.msg-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.msg-tags { display: flex; gap: 4px; flex-wrap: wrap; }
.msg-device-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-bottom: 2px;
}
.msg-device-code { font-family: monospace; font-weight: 600; }
.msg-send-time   { color: var(--el-text-color-secondary); }
.msg-retry-count { color: #e6a23c; }
.msg-action-row { margin-top: 6px; }
.msg-pagination {
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  justify-content: center;
  margin-top: auto;
}
</style>
