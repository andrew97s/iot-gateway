<template>
  <div class="app-container">
    <el-form :inline="true" :label-position="defaultQueryLabelPosition" label-width="90px" :model="queryParams" ref="queryRef" v-show="showSearch">
      <el-form-item label="名称" prop="name">
        <el-input v-model="queryParams.name" clearable placeholder="请输入名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="代码" prop="code">
        <el-input v-model="queryParams.code" clearable placeholder="请输入代码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="客户端类型" prop="type">
        <el-select v-model="queryParams.type" clearable placeholder="请选择客户端类型">
          <el-option v-for="dict in client_type" :label="dict.label" :value="dict.value" :key="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="请选择状态">
          <el-option v-for="dict in sys_status" :label="dict.label" :value="dict.value" :key="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="在线状态" prop="online">
        <el-select v-model="queryParams.online" clearable placeholder="请选择在线状态">
          <el-option v-for="dict in sys_boolean" :label="dict.label" :value="dict.value" :key="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" type="primary" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row class="mb8" :gutter="10">
      <el-col :span="1.5">
        <el-button icon="Plus" plain type="primary" v-hasPermi="['sys:client:add']" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button :disabled="multiple" icon="Delete" plain type="danger" v-hasPermi="['sys:client:remove']" @click="handleDelete">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="Download" plain type="warning" v-hasPermi="['sys:client:export']" @click="handleExport">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table :data="clientList" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column align="center" type="selection" width="55" />
      <el-table-column align="center" label="名称" prop="name" />
      <el-table-column align="center" label="代码" prop="code" />
      <el-table-column align="center" label="客户端类型" prop="type">
        <template #default="scope">
          <dict-tag :options="client_type" :value="scope.row.type" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="状态" prop="status">
        <template #default="scope">
          <dict-tag :options="sys_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="在线状态" prop="online">
        <template #default="scope">
          <dict-tag :options="sys_boolean" :value="scope.row.online" />
        </template>
      </el-table-column>
      <!-- <el-table-column align="center" label="参数配置" prop="configs" /> -->
      <el-table-column align="center" label="IP" prop="ip" />
      <el-table-column align="center" label="备注" prop="remark" />
      <el-table-column align="center" class-name="small-padding fixed-width" label="操作">
        <template #default="scope">
          <el-button icon="Edit" link type="primary" v-hasPermi="['sys:client:edit']" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button icon="View" link type="primary" v-hasPermi="['sys:client:edit']" @click="handleView(scope.row)">查看状态</el-button>
          <el-button icon="Delete" link type="primary" v-hasPermi="['sys:client:remove']" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNum" :total="total" @pagination="getList" v-show="total > 0" />

    <!-- 添加或修改客户对话框 -->
    <el-dialog v-model="open" append-to-body :title="title" :width="defaultModalWidth + 'px'">
      <el-form :label-position="defaultModalLabelPosition" label-width="100px" :model="form" :rules="rules" ref="clientRef">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="代码" prop="code">
          <el-input v-model="form.code" :disabled="!!form.id" placeholder="请输入代码" />
        </el-form-item>
        <el-form-item label="客户端类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择客户端类型">
            <el-option v-for="dict in client_type" :label="dict.label" :value="dict.value" :key="dict.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_status" :label="dict.value" :key="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="在线状态" prop="online">
          <el-radio-group v-model="form.online">
            <el-radio v-for="dict in sys_boolean" :label="dict.value" :key="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item label="参数配置" prop="configs">
          <el-input v-model="form.configs" placeholder="请输入内容" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="open2" append-to-body class="server-modal long-dialog" :title="'服务状态'" :width="defaultModalWidth + 'px'">
      <ServerStatus class="server-data" :server="serverData" v-if="serverData" />
    </el-dialog>
  </div>
</template>

<script setup>
import { listClient, getClient, delClient, addClient, updateClient } from '@/api/sys/client'
import ServerStatus from '@/components/business/serverStatus.vue'

defineOptions({
  name: 'Client'
})

const { proxy } = getCurrentInstance()
const { client_type, sys_status, sys_boolean } = proxy.useDict('client_type', 'sys_status', 'sys_boolean')

const clientList = ref([])
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
    type: null,
    status: null,
    online: null
  },
  rules: {
    name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
    code: [{ required: true, message: '代码不能为空', trigger: 'blur' }],
    type: [{ required: true, message: '客户端类型不能为空', trigger: 'change' }],
    status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
    online: [{ required: true, message: '在线状态不能为空', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询客户列表 */
function getList() {
  loading.value = true
  listClient(queryParams.value).then((response) => {
    clientList.value = response.rows
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
    type: null,
    status: null,
    online: '0',
    configs: null,
    remark: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null
  }
  proxy.resetForm('clientRef')
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
  title.value = '添加客户'
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getClient(_id).then((response) => {
    form.value = response.data
    open.value = true
    title.value = '修改客户'
  })
}

const open2 = ref(false)
const serverData = ref(null)
/** 查看按钮操作 */
function handleView(row) {
  reset()
  const _id = row.id || ids.value
  getClient(_id).then((response) => {
    serverData.value = response.data?.state
    if (serverData.value) {
      open2.value = true
    } else {
      proxy.$modal.msgWarning('暂无状态信息')
    }
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs['clientRef'].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateClient(form.value).then((response) => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addClient(form.value).then((response) => {
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
      return delClient(_ids)
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
    'sys/client/export',
    {
      ...queryParams.value
    },
    `client_${new Date().getTime()}.xlsx`
  )
}

getList()
</script>
<style lang="scss">
.server-modal {
  width: 100%;
  height: 100%;
  .el-dialog__body {
    max-height: calc(100vh - 70px);
  }
  .server-data {
    height: 100%;
  }
}
</style>
