<template>
  <div class="app-container">
    <el-form :inline="true" :label-position="defaultQueryLabelPosition" label-width="68px" :model="queryParams" ref="queryRef" v-show="showSearch">
      <el-form-item label="下级网关" prop="name">
        <el-input v-model="queryParams.name" clearable placeholder="请输入下级网关名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="代码" prop="code">
        <el-input v-model="queryParams.code" clearable placeholder="请输入代码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" type="primary" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="cascadeList" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column align="center" type="selection" width="55" />
      <el-table-column align="center" label="下级网关" prop="name" />
      <el-table-column align="center" label="代码" prop="code" />
      <el-table-column align="center" label="IP" prop="ip" />
      <el-table-column align="center" label="状态" prop="status">
        <template #default="scope">
          <dict-tag :options="sys_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="是否在线" prop="online">
        <template #default="scope">
          <dict-tag :options="sys_yes_no" :value="scope.row.online" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="备注" prop="remark" />
      <el-table-column align="center" class-name="small-padding fixed-width" label="操作">
        <template #default="scope">
          <el-button icon="Edit" link type="primary" v-hasPermi="['sys:device:edit']" @click="handleUpdate(scope.row)">修改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNum" :total="total" @pagination="getList" v-show="total > 0" />

    <!-- 添加或修改级联平台对话框 -->
    <el-dialog v-model="open" append-to-body :title="title" :width="defaultModalWidth + 'px'">
      <el-form :label-position="defaultModalLabelPosition" label-width="80px" :model="form" :rules="rules" ref="cascadeRef">
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_status" :label="dict.value" :key="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
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

<script setup name="Cascade">
import { listCascade, getCascade, delCascade, addCascade, updateCascade } from '@/api/sys/cascade'

const { proxy } = getCurrentInstance()
const { sys_status, sys_yes_no } = proxy.useDict('sys_status', 'sys_yes_no')

const cascadeList = ref([])
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
    name: null,
    code: null,
    ip: null,
    status: null,
    online: null
  },
  rules: {
    name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
    code: [{ required: true, message: '代码不能为空', trigger: 'blur' }],
    status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询级联平台列表 */
function getList() {
  loading.value = true
  listCascade(queryParams.value).then((response) => {
    cascadeList.value = response.rows
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
    name: null,
    code: null,
    ip: null,
    status: null,
    online: null,
    remark: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null
  }
  proxy.resetForm('cascadeRef')
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
  title.value = '添加级联平台'
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getCascade(_id).then((response) => {
    form.value = response.data
    open.value = true
    title.value = '修改级联平台'
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs['cascadeRef'].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateCascade(form.value).then((response) => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addCascade(form.value).then((response) => {
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
      return delCascade(_ids)
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
    'sys/cascade/export',
    {
      ...queryParams.value
    },
    `cascade_${new Date().getTime()}.xlsx`
  )
}

getList()
</script>
