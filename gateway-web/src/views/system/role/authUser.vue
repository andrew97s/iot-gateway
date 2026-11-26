<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" ref="queryRef" v-show="showSearch">
      <el-form-item label="账号" prop="userName">
        <el-input v-model="queryParams.userName" clearable placeholder="请输入账号" style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号码" prop="phonenumber">
        <el-input v-model="queryParams.phonenumber" clearable placeholder="请输入手机号码" style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" type="primary" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row class="mb8" :gutter="10">
      <el-col :span="1.5">
        <el-button icon="Plus" plain type="primary" v-hasPermi="['system:role:add']" @click="openSelectUser">添加用户</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button :disabled="multiple" icon="CircleClose" plain type="danger" v-hasPermi="['system:role:remove']" @click="cancelAuthUserAll">
          批量取消授权
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="Close" plain type="warning" @click="handleClose">关闭</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table :data="userList" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column align="center" type="selection" width="55" />
      <el-table-column label="账号" prop="userName" :show-overflow-tooltip="true" />
      <el-table-column label="姓名" prop="nickName" :show-overflow-tooltip="true" />
      <el-table-column label="邮箱" prop="email" :show-overflow-tooltip="true" />
      <el-table-column label="手机" prop="phonenumber" :show-overflow-tooltip="true" />
      <el-table-column align="center" label="状态" prop="status">
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="创建时间" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column align="center" class-name="small-padding fixed-width" label="操作">
        <template #default="scope">
          <el-button icon="CircleClose" link type="primary" v-hasPermi="['system:role:remove']" @click="cancelAuthUser(scope.row)">
            取消授权
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNum" :total="total" @pagination="getList" v-show="total > 0" />
    <select-user :role-id="queryParams.roleId" ref="selectRef" @ok="handleQuery" />
  </div>
</template>

<script setup name="AuthUser">
import selectUser from './selectUser'
import { allocatedUserList, authUserCancel, authUserCancelAll } from '@/api/system/role'

const route = useRoute()
const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict('sys_normal_disable')

const userList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const multiple = ref(true)
const total = ref(0)
const userIds = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  roleId: route.params.roleId,
  userName: undefined,
  phonenumber: undefined
})

/** 查询授权用户列表 */
function getList() {
  loading.value = true
  allocatedUserList(queryParams).then((response) => {
    userList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
// 返回按钮
function handleClose() {
  const obj = { path: '/system/role' }
  proxy.$tab.closeOpenPage(obj)
}
/** 搜索按钮操作 */
function handleQuery() {
  queryParams.pageNum = 1
  getList()
}
/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}
// 多选框选中数据
function handleSelectionChange(selection) {
  userIds.value = selection.map((item) => item.userId)
  multiple.value = !selection.length
}
/** 打开授权用户表弹窗 */
function openSelectUser() {
  proxy.$refs['selectRef'].show()
}
/** 取消授权按钮操作 */
function cancelAuthUser(row) {
  proxy.$modal
    .confirm('确认要取消该用户"' + row.userName + '"角色吗？')
    .then(function () {
      return authUserCancel({ userId: row.userId, roleId: queryParams.roleId })
    })
    .then(() => {
      getList()
      proxy.$modal.msgSuccess('取消授权成功')
    })
    .catch(() => {})
}
/** 批量取消授权按钮操作 */
function cancelAuthUserAll(row) {
  const roleId = queryParams.roleId
  const uIds = userIds.value.join(',')
  proxy.$modal
    .confirm('是否取消选中用户授权数据项?')
    .then(function () {
      return authUserCancelAll({ roleId: roleId, userIds: uIds })
    })
    .then(() => {
      getList()
      proxy.$modal.msgSuccess('取消授权成功')
    })
    .catch(() => {})
}

getList()
</script>
