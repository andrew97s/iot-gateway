<template>
  <div class="app-container">
    <el-form :inline="true" label-width="68px" :model="queryParams" ref="queryRef" v-show="showSearch">
      <el-form-item label="系统模块" prop="title">
        <el-input v-model="queryParams.title" clearable placeholder="请输入系统模块" style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="操作人员" prop="operName">
        <el-input v-model="queryParams.operName" clearable placeholder="请输入操作人员" style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="businessType">
        <el-select v-model="queryParams.businessType" clearable placeholder="操作类型" style="width: 240px">
          <el-option v-for="dict in sys_oper_type" :label="dict.label" :value="dict.value" :key="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="操作状态" style="width: 240px">
          <el-option v-for="dict in sys_common_status" :label="dict.label" :value="dict.value" :key="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作时间" style="width: 308px">
        <el-date-picker
          v-model="dateRange"
          end-placeholder="结束日期"
          range-separator="-"
          start-placeholder="开始日期"
          type="daterange"
          value-format="YYYY-MM-DD"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" type="primary" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row class="mb8" :gutter="10">
      <el-col :span="1.5">
        <el-button :disabled="multiple" icon="Delete" plain type="danger" v-hasPermi="['monitor:operlog:remove']" @click="handleDelete">
          删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="Delete" plain type="danger" v-hasPermi="['monitor:operlog:remove']" @click="handleClean">清空</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="Download" plain type="warning" v-hasPermi="['monitor:operlog:export']" @click="handleExport">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      :data="operlogList"
      :default-sort="defaultSort"
      ref="operlogRef"
      v-loading="loading"
      @selection-change="handleSelectionChange"
      @sort-change="handleSortChange"
    >
      <el-table-column align="center" type="selection" width="55" />
      <el-table-column align="center" label="日志编号" prop="operId" />
      <el-table-column align="center" label="系统模块" prop="title" />
      <el-table-column align="center" label="操作类型" prop="businessType">
        <template #default="scope">
          <dict-tag :options="sys_oper_type" :value="scope.row.businessType" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="请求方式" prop="requestMethod" />
      <el-table-column
        align="center"
        label="操作人员"
        prop="operName"
        :show-overflow-tooltip="true"
        :sort-orders="['descending', 'ascending']"
        sortable="custom"
        width="100"
      />
      <el-table-column align="center" label="主机" prop="operIp" :show-overflow-tooltip="true" width="130" />
      <el-table-column align="center" label="操作状态" prop="status">
        <template #default="scope">
          <dict-tag :options="sys_common_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="操作日期" prop="operTime" :sort-orders="['descending', 'ascending']" sortable="custom" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.operTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column align="center" class-name="small-padding fixed-width" label="操作">
        <template #default="scope">
          <el-button icon="View" link type="primary" v-hasPermi="['monitor:operlog:query']" @click="handleView(scope.row, scope.index)">
            详细
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNum" :total="total" @pagination="getList" v-show="total > 0" />

    <!-- 操作日志详细 -->
    <el-dialog v-model="open" append-to-body title="操作日志详细" width="700px">
      <el-form label-position="top" label-width="100px" :model="form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="操作模块：">{{ form.title }} / {{ typeFormat(form) }}</el-form-item>
            <el-form-item label="登录信息：">{{ form.operName }} / {{ form.operIp }} / {{ form.operLocation }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="请求地址：">{{ form.operUrl }}</el-form-item>
            <el-form-item label="请求方式：">{{ form.requestMethod }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="操作方法：">{{ form.method }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="请求参数：">{{ form.operParam }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="返回参数：">{{ form.jsonResult }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作状态：">
              <div v-if="form.status === 0">正常</div>
              <div v-else-if="form.status === 1">失败</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作时间：">{{ parseTime(form.operTime) }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="异常信息：" v-if="form.status === 1">{{ form.errorMsg }}</el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="open = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Operlog">
import { list, delOperlog, cleanOperlog } from '@/api/monitor/operlog'

const { proxy } = getCurrentInstance()
const { sys_oper_type, sys_common_status } = proxy.useDict('sys_oper_type', 'sys_common_status')

const operlogList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const dateRange = ref([])
const defaultSort = ref({ prop: 'operTime', order: 'descending' })

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: undefined,
    operName: undefined,
    businessType: undefined,
    status: undefined
  }
})

const { queryParams, form } = toRefs(data)

/** 查询登录日志 */
function getList() {
  loading.value = true
  list(proxy.addDateRange(queryParams.value, dateRange.value)).then((response) => {
    operlogList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
/** 操作日志类型字典翻译 */
function typeFormat(row, column) {
  return proxy.selectDictLabel(sys_oper_type.value, row.businessType)
}
/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}
/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = []
  proxy.resetForm('queryRef')
  queryParams.value.pageNum = 1
  proxy.$refs['operlogRef'].sort(defaultSort.value.prop, defaultSort.value.order)
}
/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.operId)
  multiple.value = !selection.length
}
/** 排序触发事件 */
function handleSortChange(column, prop, order) {
  queryParams.value.orderByColumn = column.prop
  queryParams.value.isAsc = column.order
  getList()
}
/** 详细按钮操作 */
function handleView(row) {
  open.value = true
  form.value = row
}
/** 删除按钮操作 */
function handleDelete(row) {
  const operIds = row.operId || ids.value
  proxy.$modal
    .confirm(proxy.deleteTips)
    .then(function () {
      return delOperlog(operIds)
    })
    .then(() => {
      getList()
      proxy.$modal.msgSuccess('删除成功')
    })
    .catch(() => {})
}
/** 清空按钮操作 */
function handleClean() {
  proxy.$modal
    .confirm('是否确认清空所有操作日志数据项?')
    .then(function () {
      return cleanOperlog()
    })
    .then(() => {
      getList()
      proxy.$modal.msgSuccess('清空成功')
    })
    .catch(() => {})
}
/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    'monitor/operlog/export',
    {
      ...queryParams.value
    },
    `config_${new Date().getTime()}.xlsx`
  )
}

getList()
</script>
