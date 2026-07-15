<template>
  <div class="app-container">
    <el-form :inline="true" :label-position="defaultQueryLabelPosition" label-width="100px" :model="queryParams" ref="queryRef" v-show="showSearch">
      <!-- <el-form-item label="父节点ID" prop="parentId">
          <el-input v-model="queryParams.parentId" clearable placeholder="请输入父节点ID" @keyup.enter="handleQuery" />
        </el-form-item> -->
      <el-form-item label="名称" prop="name">
        <el-input v-model="queryParams.name" clearable placeholder="请输入名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="编码" prop="code">
        <el-input v-model="queryParams.code" clearable placeholder="请输入编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="节点类型" prop="isLeaf">
        <el-select v-model="queryParams.isLeaf" clearable>
          <el-option v-for="dict in leafType" :label="dict.label" :value="dict.value" :key="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="技术分类" prop="techType">
        <el-select v-model="queryParams.techType" clearable>
          <el-option v-for="dict in facility_tech_type" :label="dict.label" :value="dict.value" :key="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="青鸟类型" prop="jbCode">
        <el-input v-model="queryParams.jbCode" clearable placeholder="请输入青鸟类型" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" type="primary" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table
      :data="typeList"
      :default-expand-all="isExpandAll"
      row-key="id"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      v-loading="loading"
      v-if="refreshTable"
    >
      <el-table-column align="left" label="名称" prop="name" width="330" />
      <el-table-column align="center" label="编码" prop="code" />
      <!-- <el-table-column align="center" label="简称" prop="shortName" /> -->
      <el-table-column align="center" label="子系统类型" prop="sysTypeName" />
      <el-table-column align="center" label="青鸟类型" prop="jbCode" width="120" />
      <el-table-column align="center" label="技术分类" prop="facility_tech_type" width="120">
        <template #default="scope">
          <dict-tag :options="facility_tech_type" :value="scope.row.techType" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="节点类型" prop="isLeaf" width="120">
        <template #default="scope">
          <dict-tag :options="leafType" :value="scope.row.isLeaf" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="显示排序" prop="orderNum" width="100" />
    </el-table>
  </div>
</template>

<script setup name="Type">
import { listType } from '@/api/sys/type'

const { proxy } = getCurrentInstance()
const { facility_tech_type } = proxy.useDict('facility_tech_type')

const typeList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const isExpandAll = ref(false)
const refreshTable = ref(true)
const leafType = [
  {
    label: '节点',
    value: 'Y',
    elTagType: 'success'
  },
  {
    label: '目录',
    value: 'N'
  }
]

const data = reactive({
  queryParams: {
    parentId: null,
    ancestors: null,
    code: null,
    name: null,
    sysTypeId: null,
    isTransfer: null,
    isControl: null,
    isComponent: null,
    isLeaf: null,
    jbCode: null
  }
})

const { queryParams } = toRefs(data)
/** 查询设备类型列表 */
function getList() {
  loading.value = true
  listType(queryParams.value).then((response) => {
    typeList.value = proxy.handleTree(response.rows, 'id', 'parentId')
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

/** 展开/折叠操作 */
function toggleExpandAll() {
  refreshTable.value = false
  isExpandAll.value = !isExpandAll.value
  nextTick(() => {
    refreshTable.value = true
  })
}

getList()
</script>
