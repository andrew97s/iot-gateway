<template>
  <div class="app-container">
    <el-form :inline="true" label-width="68px" :model="queryParams" ref="queryRef" v-show="showSearch">
      <el-form-item label="字典名称" prop="dictName">
        <el-input v-model="queryParams.dictName" clearable placeholder="请输入字典名称" style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="字典类型" prop="dictType">
        <el-input v-model="queryParams.dictType" clearable placeholder="请输入字典类型" style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="字典状态" style="width: 240px">
          <el-option v-for="dict in sys_normal_disable" :label="dict.label" :value="dict.value" :key="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间" style="width: 308px">
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
        <el-button icon="Plus" plain type="primary" v-hasPermi="['system:dict:add']" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button :disabled="multiple" icon="Delete" plain type="danger" v-hasPermi="['system:dict:remove']" @click="handleDelete">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="Download" plain type="warning" v-hasPermi="['system:dict:export']" @click="handleExport">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="Refresh" plain type="danger" v-hasPermi="['system:dict:remove']" @click="handleRefreshCache">刷新缓存</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table :data="typeList" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column align="center" type="selection" width="55" />
      <el-table-column align="center" label="字典编号" prop="dictId" />
      <el-table-column align="center" label="字典名称" prop="dictName" :show-overflow-tooltip="true" />
      <el-table-column align="center" label="字典类型" :show-overflow-tooltip="true">
        <template #default="scope">
          <router-link class="link-type" :to="'/system/dict-data/index/' + scope.row.dictId">
            <span>{{ scope.row.dictType }}</span>
          </router-link>
        </template>
      </el-table-column>
      <el-table-column align="center" label="状态" prop="status">
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="备注" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column align="center" label="创建时间" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column align="center" class-name="small-padding fixed-width" label="操作" width="160">
        <template #default="scope">
          <el-button icon="Edit" link type="primary" v-hasPermi="['system:dict:edit']" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button icon="Delete" link type="primary" v-hasPermi="['system:dict:remove']" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNum" :total="total" @pagination="getList" v-show="total > 0" />

    <!-- 添加或修改参数配置对话框 -->
    <el-dialog v-model="open" append-to-body :title="title" width="600px">
      <el-form label-width="80px" :model="form" :rules="rules" ref="dictRef">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="form.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="form.dictType" placeholder="请输入字典类型" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_normal_disable" :label="dict.value" :key="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入内容" type="textarea"></el-input>
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

<script setup name="Dict">
import useDictStore from '@/store/modules/dict'
import { listType, getType, delType, addType, updateType, refreshCache } from '@/api/system/dict/type'

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict('sys_normal_disable')

const typeList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const dateRange = ref([])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    dictName: undefined,
    dictType: undefined,
    status: undefined
  },
  rules: {
    dictName: [{ required: true, message: '字典名称不能为空', trigger: 'blur' }],
    dictType: [{ required: true, message: '字典类型不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询字典类型列表 */
function getList() {
  loading.value = true
  listType(proxy.addDateRange(queryParams.value, dateRange.value)).then((response) => {
    typeList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}
/** 表单重置 */
function reset() {
  form.value = {
    dictId: undefined,
    dictName: undefined,
    dictType: undefined,
    status: '0',
    remark: undefined
  }
  proxy.resetForm('dictRef')
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
  handleQuery()
}
/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '添加字典类型'
}
/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.dictId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}
/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const dictId = row.dictId || ids.value
  getType(dictId).then((response) => {
    form.value = response.data
    open.value = true
    title.value = '修改字典类型'
  })
}
/** 提交按钮 */
function submitForm() {
  proxy.$refs['dictRef'].validate((valid) => {
    if (valid) {
      if (form.value.dictId != undefined) {
        updateType(form.value).then((response) => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addType(form.value).then((response) => {
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
  const dictIds = row.dictId || ids.value
  proxy.$modal
    .confirm(proxy.deleteTips)
    .then(function () {
      return delType(dictIds)
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
    'system/dict/type/export',
    {
      ...queryParams.value
    },
    `dict_${new Date().getTime()}.xlsx`
  )
}
/** 刷新缓存按钮操作 */
function handleRefreshCache() {
  refreshCache().then(() => {
    proxy.$modal.msgSuccess('刷新成功')
    useDictStore().cleanDict()
  })
}

getList()
</script>
