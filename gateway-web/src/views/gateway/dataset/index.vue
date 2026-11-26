<template>
  <div class="app-container">
    <el-form :inline="true" label-width="68px" :model="queryParams" ref="queryRef" v-show="showSearch">
      <el-form-item label="类型" prop="type">
        <el-select v-model="queryParams.type" clearable placeholder="请选择类型">
          <el-option v-for="dict in dataset_type" :label="dict.label" :value="dict.value" :key="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="queryParams.name" clearable placeholder="请输入名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="代码" prop="code">
        <el-input v-model="queryParams.code" clearable placeholder="请输入代码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="参数" prop="param">
        <el-input v-model="queryParams.param" clearable placeholder="请输入参数" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" type="primary" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row class="mb8" :gutter="10">
      <el-col :span="1.5">
        <el-button icon="Plus" plain type="primary" v-hasPermi="['sys:dataset:add']" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button :disabled="multiple" icon="Delete" plain type="danger" v-hasPermi="['sys:dataset:remove']" @click="handleDelete">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="Download" plain type="warning" v-hasPermi="['sys:dataset:export']" @click="handleExport">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table :data="datasetList" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column align="center" type="selection" width="55" />
      <el-table-column align="center" label="类型" prop="type">
        <template #default="scope">
          <dict-tag :options="dataset_type" :value="scope.row.type" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="名称" prop="name" />
      <el-table-column align="center" label="代码" prop="code" />
      <el-table-column align="center" label="参数" prop="param" />
      <el-table-column align="center" label="备注" prop="remark" />
      <el-table-column align="center" class-name="small-padding fixed-width" label="操作">
        <template #default="scope">
          <el-button icon="Edit" link type="primary" v-hasPermi="['sys:dataset:edit']" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button icon="Delete" link type="primary" v-hasPermi="['sys:dataset:remove']" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNum" :total="total" @pagination="getList" v-show="total > 0" />

    <!-- 添加或修改数据集对话框 -->
    <el-dialog v-model="open" append-to-body :title="title" width="600px">
      <el-form label-width="80px" :model="form" :rules="rules" ref="datasetRef">
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型">
            <el-option v-for="dict in dataset_type" :label="dict.label" :value="dict.value" :key="dict.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="代码" prop="code">
          <el-input v-model="form.code" placeholder="请输入代码" />
        </el-form-item>
        <el-form-item label="查询语句" prop="sqls">
          <el-input v-model="form.sqls" autosize class="sql-input" placeholder="请输入内容" type="textarea" />
        </el-form-item>
        <el-form-item label="参数" prop="param">
          <el-input v-model="form.param" placeholder="请输入参数" />
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

<script setup name="Dataset">
import { listDataset, getDataset, delDataset, addDataset, updateDataset } from '@/api/sys/dataset'

const { proxy } = getCurrentInstance()
const { dataset_type } = proxy.useDict('dataset_type')

const datasetList = ref([])
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
    type: null,
    name: null,
    code: null,
    sqls: null,
    param: null
  },
  rules: {
    type: [{ required: true, message: '类型不能为空', trigger: 'change' }],
    name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
    code: [{ required: true, message: '代码不能为空', trigger: 'blur' }],
    sqls: [{ required: true, message: '查询语句不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询数据集列表 */
function getList() {
  loading.value = true
  listDataset(queryParams.value).then((response) => {
    datasetList.value = response.rows
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
    type: null,
    name: null,
    code: null,
    sqls: null,
    param: null,
    remark: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null
  }
  proxy.resetForm('datasetRef')
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
  title.value = '添加数据集'
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getDataset(_id).then((response) => {
    form.value = response.data
    open.value = true
    title.value = '修改数据集'
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs['datasetRef'].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateDataset(form.value).then((response) => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addDataset(form.value).then((response) => {
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
      return delDataset(_ids)
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
    'sys/dataset/export',
    {
      ...queryParams.value
    },
    `dataset_${new Date().getTime()}.xlsx`
  )
}

getList()
</script>

<style lang="scss" scoped>
.sql-input {
  :deep(.el-textarea__inner) {
    max-height: 300px;
  }
}
</style>
