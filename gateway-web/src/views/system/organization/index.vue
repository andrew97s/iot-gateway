<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" ref="queryRef" v-show="showSearch">
      <el-form-item label="名称" prop="name">
        <el-input v-model="queryParams.name" clearable placeholder="请输入名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" type="primary" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row class="mb8" :gutter="10">
      <el-col :span="1.5">
        <el-button icon="Plus" plain type="primary" v-hasPermi="['system:organization:add']" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button :disabled="multiple" icon="Delete" plain type="danger" v-hasPermi="['system:organization:remove']" @click="handleDelete">
          删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="Download" plain type="warning" v-hasPermi="['system:organization:export']" @click="handleExport">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table :data="organizationList" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column align="center" type="selection" width="55" />
      <el-table-column align="center" label="名称" prop="name" />
      <el-table-column align="center" label="类型" prop="type">
        <template #default="scope">
          <dict-tag :options="system_organization_type" :value="scope.row.type" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="位置" prop="location" />
      <el-table-column align="center" class-name="small-padding fixed-width" label="操作">
        <template #default="scope">
          <el-button icon="Edit" link type="primary" v-hasPermi="['system:organization:edit']" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button icon="Delete" link type="primary" v-hasPermi="['system:organization:remove']" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNum" :total="total" @pagination="getList" v-show="total > 0" />

    <!-- 添加或修改系统组织对话框 -->
    <el-dialog v-model="open" append-to-body :title="title" width="600px">
      <el-form :model="form" :rules="rules" ref="organizationRef">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入组织名称" />
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="form.location" placeholder="请输入位置" />
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

<script setup name="Organization">
import { listOrganization, getOrganization, delOrganization, addOrganization, updateOrganization } from '@/api/system/organization'

const { proxy } = getCurrentInstance()
const { system_organization_type } = proxy.useDict('system_organization_type')

const organizationList = ref([])
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
    pid: null,
    name: null,
    type: null,
    code: null,
    location: null,
    industry: null,
    peoples: null
  },
  rules: {
    name: [{ required: true, message: '组织名称不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询系统组织列表 */
function getList() {
  loading.value = true
  listOrganization(queryParams.value).then((response) => {
    organizationList.value = response.rows
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
    pid: null,
    name: null,
    type: null,
    code: null,
    location: null,
    industry: null,
    peoples: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null
  }
  proxy.resetForm('organizationRef')
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
  title.value = '添加系统组织'
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getOrganization(_id).then((response) => {
    form.value = response.data
    open.value = true
    title.value = '修改系统组织'
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs['organizationRef'].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateOrganization(form.value).then((response) => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addOrganization(form.value).then((response) => {
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
      return delOrganization(_ids)
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
    'system/organization/export',
    {
      ...queryParams.value
    },
    `organization_${new Date().getTime()}.xlsx`
  )
}

getList()
</script>
