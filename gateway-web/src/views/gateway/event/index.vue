<template>
  <div class="app-container">
    <el-form :inline="true" label-width="68px" :model="queryParams" ref="queryRef" v-show="showSearch">
      <el-form-item label="原始代码" prop="sourceCode">
        <el-input v-model="queryParams.sourceCode" clearable placeholder="请输入原始代码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="事件厂商" prop="vendorCode">
        <el-input v-model="queryParams.vendorCode" clearable placeholder="请输入事件厂商" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" type="primary" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="eventList" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column align="center" type="selection" width="55" />
      <el-table-column align="center" label="告警类型" prop="alarmType" />
      <el-table-column align="center" label="原始代码" prop="sourceCode" />
      <el-table-column align="center" label="事件厂商" prop="vendorCode" />
      <el-table-column align="center" label="事件描述" prop="comment" />
      <el-table-column align="center" label="WS操作" prop="wsOption" />
      <el-table-column align="center" label="告警状态" prop="alarmState">
        <template #default="scope">
          <dict-tag :options="sys_alarm_state" :value="scope.row.alarmState" />
        </template>
      </el-table-column>
    </el-table>

    <pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNum" :total="total" @pagination="getList" v-show="total > 0" />

    <!-- 添加或修改告警事件对话框 -->
    <el-dialog v-model="open" append-to-body :title="title" width="600px">
      <el-form label-width="80px" :model="form" :rules="rules" ref="eventRef">
        <el-form-item label="原始代码" prop="sourceCode">
          <el-input v-model="form.sourceCode" placeholder="请输入原始代码" />
        </el-form-item>
        <el-form-item label="事件厂商" prop="vendorCode">
          <el-input v-model="form.vendorCode" placeholder="请输入事件厂商" />
        </el-form-item>
        <el-form-item label="事件描述" prop="comment">
          <el-input v-model="form.comment" placeholder="请输入事件描述" />
        </el-form-item>
        <el-form-item label="WS操作" prop="wsOption">
          <el-input v-model="form.wsOption" placeholder="请输入WS操作" />
        </el-form-item>
        <el-form-item label="告警状态" prop="alarmState">
          <el-radio-group v-model="form.alarmState">
            <el-radio v-for="dict in sys_alarm_state" :label="dict.value" :key="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Event">
import { listEvent, getEvent, delEvent, addEvent, updateEvent } from '@/api/sys/event'

const { proxy } = getCurrentInstance()
const { sys_alarm_state } = proxy.useDict('sys_alarm_state')

const eventList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderByColumn: 'id',
    isAsc: 'DESC',
    alarmType: null,
    sourceCode: null,
    vendorCode: null,
    comment: null,
    wsOption: null,
    alarmState: null
  },
  rules: {
    alarmType: [{ required: true, message: '告警类型不能为空', trigger: 'change' }],
    sourceCode: [{ required: true, message: '原始代码不能为空', trigger: 'blur' }],
    vendorCode: [{ required: true, message: '事件厂商不能为空', trigger: 'blur' }],
    comment: [{ required: true, message: '事件描述不能为空', trigger: 'blur' }],
    wsOption: [{ required: true, message: 'WS操作不能为空', trigger: 'blur' }],
    alarmState: [{ required: true, message: '告警状态不能为空', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询告警事件列表 */
function getList() {
  loading.value = true
  listEvent(queryParams.value).then((response) => {
    eventList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    alarmType: null,
    sourceCode: null,
    vendorCode: null,
    comment: null,
    wsOption: null,
    alarmState: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null
  }
  proxy.resetForm('eventRef')
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '添加告警事件'
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getEvent(_id).then((response) => {
    form.value = response.data
    open.value = true
    title.value = '修改告警事件'
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs['eventRef'].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateEvent(form.value).then((response) => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addEvent(form.value).then((response) => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal
    .confirm(proxy.deleteTips)
    .then(function () {
      return delEvent(_ids)
    })
    .then(() => {
      getList()
      proxy.$modal.msgSuccess('删除成功')
    })
    .catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    'sys/event/export',
    {
      ...queryParams.value
    },
    `event_${new Date().getTime()}.xlsx`
  )
}

getList()
</script>
