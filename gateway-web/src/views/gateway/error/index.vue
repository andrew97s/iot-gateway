<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :inline="true" label-width="68px" :model="queryParams" ref="queryRef" v-show="showSearch">
      <el-form-item label="平台" prop="pfCode">
        <el-select v-model="queryParams.pfCode" clearable placeholder="选择平台">
          <el-option v-for="p in platformOptions" :label="p.name" :value="p.code" :key="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="queryParams.type" clearable placeholder="选择类型">
          <el-option v-for="dict in sys_error_type" :label="dict.label" :value="dict.value" :key="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词" prop="title">
        <el-input v-model="queryParams.title" clearable placeholder="标题关键词" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="记录时间" style="width: 308px">
        <el-date-picker
          v-model="daterangeLogTime"
          end-placeholder="结束日期"
          range-separator="-"
          start-placeholder="开始日期"
          type="daterange"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" type="primary" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row class="mb8" :gutter="10">
      <el-col :span="1.5">
        <el-button :disabled="multiple" icon="Delete" plain type="danger" v-hasPermi="['sys:error:remove']" @click="handleDelete">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="WarnTriangleFilled" plain type="danger" v-hasPermi="['sys:message:remove']" @click="handleClear">清空</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="Download" plain type="warning" v-hasPermi="['sys:error:export']" @click="handleExport">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      :data="errorList"
      v-loading="loading"
      @selection-change="handleSelectionChange"
    >
      <el-table-column align="center" type="selection" width="40" />
      <el-table-column align="center" label="平台" prop="pfCode" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.pfCode" size="small" type="info">{{ scope.row.pfCode }}</el-tag>
          <span v-else class="text-secondary">-</span>
        </template>
      </el-table-column>
      <el-table-column align="center" label="类型" prop="type" width="100">
        <template #default="scope">
          <el-tag :type="getTypeTag(scope.row.type)" size="small" :effect="scope.row.type === '6' ? 'plain' : 'light'">
            {{ getTypeName(scope.row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
      <el-table-column label="错误说明" prop="error" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          <span :class="scope.row.error ? 'error-text' : 'text-secondary'">{{ scope.row.error || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column align="center" label="记录时间" prop="logTime" width="165" />
      <el-table-column align="center" label="操作" min-width="120">
        <template #default="scope">
          <el-button icon="View" link type="primary" v-hasPermi="['sys:error:list']" @click="handleView(scope.row)">查看</el-button>
          <el-button icon="Delete" link type="danger" v-hasPermi="['sys:error:remove']" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNum" :total="total" @pagination="getList" v-show="total > 0" />

    <!-- 详情对话框 -->
    <el-dialog v-model="open" append-to-body :title="title" width="820px">
      <div class="dialog-scroll-body">
        <el-descriptions :column="2" border size="small" class="mb12">
          <el-descriptions-item label="平台">
            <el-tag size="small" type="info">{{ form.pfCode || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag :type="getTypeTag(form.type)" size="small">{{ getTypeName(form.type) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="记录时间" :span="2">{{ form.logTime }}</el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ form.title }}</el-descriptions-item>
          <el-descriptions-item label="处理内容" :span="2">
            <div class="detail-content">{{ form.content }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="错误说明" :span="2">
            <div class="error-detail">{{ form.error }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="cancel">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ErrorLog">
import { listError, getError, delError, clearError } from '@/api/sys/error'
import { selectPlatform } from '@/api/sys/platform'

const { proxy } = getCurrentInstance()
const { sys_error_type } = proxy.useDict('sys_error_type')

const errorList    = ref([])
const open         = ref(false)
const loading      = ref(true)
const showSearch   = ref(true)
const ids          = ref([])
const single       = ref(true)
const multiple     = ref(true)
const total        = ref(0)
const title        = ref('')
const daterangeLogTime = ref([])

const platformOptions = ref([])
selectPlatform().then(res => { platformOptions.value = res.data })

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderByColumn: 'id',
    isAsc: 'DESC',
    pfCode: null,
    type:   null,
    title:  null
  }
})
const { queryParams, form } = toRefs(data)

// 类型映射
const TYPE_MAP = {
  '1': { name: '接口超时', tag: 'warning' },
  '2': { name: '数据异常', tag: 'warning' },
  '3': { name: '系统错误', tag: 'danger'  },
  '4': { name: '其它错误', tag: 'info'    },
  '5': { name: '接口错误', tag: 'danger'  },
  '6': { name: '生命周期', tag: 'success' },
  '9': { name: 'MQ消息',   tag: 'primary' }
}
function getTypeName(t) { return TYPE_MAP[t]?.name || t || '-' }
function getTypeTag(t) { return TYPE_MAP[t]?.tag || '' }
function getRowClass({ row }) {
  if (row.type === '6') return 'row-lifecycle'
  if (['3', '5'].includes(row.type)) return 'row-error'
  if (['1', '2'].includes(row.type)) return 'row-warn'
  return ''
}

function getList() {
  loading.value = true
  queryParams.value.params = {}
  if (daterangeLogTime.value?.length === 2) {
    queryParams.value.params['beginLogTime'] = daterangeLogTime.value[0]
    queryParams.value.params['endLogTime']   = daterangeLogTime.value[1]
  }
  listError(queryParams.value).then(res => {
    errorList.value = res.rows
    total.value     = res.total
    loading.value   = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = { id: null, pfCode: null, type: null, title: null, content: null, error: null, logTime: null }
  proxy.resetForm('errorRef')
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  daterangeLogTime.value = []
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value    = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleView(row) {
  reset()
  getError(row.id || ids.value).then(res => {
    form.value  = res.data
    open.value  = true
    title.value = '异常诊断详情'
  })
}

function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm(proxy.deleteTips)
    .then(() => delError(_ids))
    .then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
    .catch(() => {})
}

function handleClear() {
  proxy.$modal.confirm('是否确认清空所有异常记录?')
    .then(() => clearError())
    .then(() => { getList(); proxy.$modal.msgSuccess('清空成功') })
    .catch(() => {})
}

function handleExport() {
  proxy.download('sys/error/export', { ...queryParams.value }, `error_${Date.now()}.xlsx`)
}

getList()
</script>

<style scoped>
.text-secondary { color: var(--el-text-color-secondary); font-size: 12px; }
.error-text     { color: var(--el-color-danger); font-size: 12px; }
.mb12 { margin-bottom: 12px; }
.dialog-scroll-body {
  max-height: 65vh;
  overflow-y: auto;
  padding-right: 4px;
}
.detail-content {
  word-break: break-all;
  white-space: pre-wrap;
  font-size: 13px;
}
.error-detail {
  color: var(--el-color-danger);
  word-break: break-all;
  white-space: pre-wrap;
  font-size: 12px;
}
</style>
