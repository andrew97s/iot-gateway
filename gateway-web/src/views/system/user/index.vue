<template>
  <div class="app-container">
    <el-form :inline="true" label-width="68px" :model="queryParams" ref="queryRef" v-show="showSearch">
      <el-form-item label="用户名称" prop="userName">
        <el-input v-model="queryParams.userName" clearable placeholder="请输入用户名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号码" prop="phonenumber">
        <el-input v-model="queryParams.phonenumber" clearable placeholder="请输入手机号码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="用户状态">
          <el-option v-for="dict in sys_normal_disable" :label="dict.label" :value="dict.value" :key="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间">
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
        <el-button icon="Plus" plain type="primary" v-hasPermi="['system:user:add']" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button :disabled="multiple" icon="Delete" plain type="danger" v-hasPermi="['system:user:remove']" @click="handleDelete"> 删除 </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="Upload" plain type="info" v-hasPermi="['system:user:import']" @click="importTemplate?.handleImport">导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="Download" plain type="warning" v-hasPermi="['system:user:export']" @click="handleExport">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table :data="userList" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column align="center" type="selection" width="50" />
      <el-table-column align="center" label="用户编号" prop="userId" key="userId" v-if="columns[0].visible" />
      <el-table-column align="center" label="用户名称" prop="userName" :show-overflow-tooltip="true" key="userName" v-if="columns[1].visible" />
      <el-table-column align="center" label="用户昵称" prop="nickName" :show-overflow-tooltip="true" key="nickName" v-if="columns[2].visible" />
      <el-table-column align="center" label="部门" prop="dept.deptName" :show-overflow-tooltip="true" key="deptName" v-if="columns[3].visible" />
      <el-table-column align="center" label="手机号码" prop="phonenumber" width="120" key="phonenumber" v-if="columns[4].visible" />
      <el-table-column align="center" label="状态" key="status" v-if="columns[5].visible">
        <template #default="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)"></el-switch>
        </template>
      </el-table-column>
      <el-table-column align="center" label="创建时间" prop="createTime" width="160" v-if="columns[6].visible">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column align="center" class-name="small-padding fixed-width" label="操作" width="150">
        <template #default="scope">
          <el-tooltip content="修改" placement="top" v-if="scope.row.userId !== 1">
            <el-button icon="Edit" link type="primary" v-hasPermi="['system:user:edit']" @click="handleUpdate(scope.row)"></el-button>
          </el-tooltip>
          <el-tooltip content="删除" placement="top" v-if="scope.row.userId !== 1">
            <el-button icon="Delete" link type="primary" v-hasPermi="['system:user:remove']" @click="handleDelete(scope.row)"></el-button>
          </el-tooltip>
          <el-tooltip content="重置密码" placement="top" v-if="scope.row.userId !== 1">
            <el-button icon="Key" link type="primary" v-hasPermi="['system:user:resetPwd']" @click="handleResetPwd(scope.row)"></el-button>
          </el-tooltip>
          <el-tooltip content="分配角色" placement="top" v-if="scope.row.userId !== 1">
            <el-button icon="CircleCheck" link type="primary" v-hasPermi="['system:user:edit']" @click="handleAuthRole(scope.row)"></el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNum" :total="total" @pagination="getList" v-show="total > 0" />

    <!-- 添加或修改用户配置对话框 -->
    <el-dialog v-model="open" append-to-body class="user-dialog" :title="title" width="620px">
      <el-form label-width="80px" :model="form" :rules="rules" ref="userRef">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="form.nickName" maxlength="30" placeholder="请输入用户昵称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-select v-model="form.roleIds" multiple placeholder="请选择">
                <el-option
                  v-for="item in roleOptions"
                  :disabled="item.status == 1"
                  :label="item.roleName"
                  :value="item.roleId"
                  :key="item.roleId"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phonenumber">
              <el-input v-model="form.phonenumber" maxlength="11" placeholder="请输入手机号码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" maxlength="50" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户名称" prop="userName" v-if="form.userId == undefined">
              <el-input v-model="form.userName" maxlength="30" placeholder="请输入用户名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户密码" prop="password" v-if="form.userId == undefined">
              <el-input v-model="form.password" maxlength="20" placeholder="请输入用户密码" show-password type="password" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户性别">
              <el-select v-model="form.sex" placeholder="请选择">
                <el-option v-for="dict in sys_user_sex" :label="dict.label" :value="dict.value" :key="dict.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in sys_normal_disable" :label="dict.value" :key="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="请输入内容" type="textarea"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 用户导入对话框 -->
    <ImportTemplate
      download-url="system/user/importTemplate"
      file-name="user"
      title="用户"
      upload-url="system/user/importData"
      ref="importTemplate"
      @uploadSuccess="getList"
    />
  </div>
</template>

<script setup name="User">
import { getToken } from '@/utils/auth'
import { changeUserStatus, listUser, resetUserPwd, delUser, getUser, updateUser, addUser, deptTreeSelect } from '@/api/system/user'

const router = useRouter()
const { proxy } = getCurrentInstance()
const { sys_normal_disable, sys_user_sex } = proxy.useDict('sys_normal_disable', 'sys_user_sex')

const userList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const dateRange = ref([])
const deptName = ref('')
const deptOptions = ref(undefined)
const initPassword = ref(undefined)
const postOptions = ref([])
const roleOptions = ref([])
const importTemplate = ref(null)

// 列显隐信息
const columns = ref([
  { key: 0, label: `用户编号`, visible: true },
  { key: 1, label: `用户名称`, visible: true },
  { key: 2, label: `用户昵称`, visible: true },
  { key: 3, label: `部门`, visible: true },
  { key: 4, label: `手机号码`, visible: true },
  { key: 5, label: `状态`, visible: true },
  { key: 6, label: `创建时间`, visible: true }
])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userName: undefined,
    phonenumber: undefined,
    status: undefined
  },
  rules: {
    userName: [
      { required: true, message: '用户名称不能为空', trigger: 'blur' },
      { min: 2, max: 20, message: '用户名称长度必须介于 2 和 20 之间', trigger: 'blur' }
    ],
    nickName: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
    password: [
      { required: true, message: '用户密码不能为空', trigger: 'blur' },
      { min: 5, max: 20, message: '用户密码长度必须介于 5 和 20 之间', trigger: 'blur' },
      { pattern: /^[^<>"'|\\]+$/, message: '不能包含非法字符：< > " \' \\ |', trigger: 'blur' }
    ],
    email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }],
    phonenumber: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: '请输入正确的手机号码', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 通过条件过滤节点  */
const filterNode = (value, data) => {
  if (!value) return true
  return data.label.indexOf(value) !== -1
}
/** 根据名称筛选部门树 */
watch(deptName, (val) => {
  proxy.$refs['deptTreeRef'].filter(val)
})
/** 查询部门下拉树结构 */
function getDeptTree() {
  deptTreeSelect().then((response) => {
    deptOptions.value = response.data
  })
}
/** 查询用户列表 */
function getList() {
  loading.value = true
  listUser(proxy.addDateRange(queryParams.value, dateRange.value)).then((res) => {
    loading.value = false
    userList.value = res.rows
    total.value = res.total
  })
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
  proxy.$refs.deptTreeRef.setCurrentKey(null)
  handleQuery()
}
/** 删除按钮操作 */
function handleDelete(row) {
  const userIds = row.userId || ids.value
  proxy.$modal
    .confirm('是否确认删除用户编号为"' + userIds + '"的数据项？')
    .then(function () {
      return delUser(userIds)
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
    'system/user/export',
    {
      ...queryParams.value
    },
    `user_${new Date().getTime()}.xlsx`
  )
}
/** 用户状态修改  */
function handleStatusChange(row) {
  let text = row.status === '0' ? '启用' : '停用'
  proxy.$modal
    .confirm('确认要"' + text + '""' + row.userName + '"用户吗?')
    .then(function () {
      return changeUserStatus(row.userId, row.status)
    })
    .then(() => {
      proxy.$modal.msgSuccess(text + '成功')
    })
    .catch(function () {
      row.status = row.status === '0' ? '1' : '0'
    })
}
/** 更多操作 */
function handleCommand(command, row) {
  switch (command) {
    case 'handleResetPwd':
      handleResetPwd(row)
      break
    case 'handleAuthRole':
      handleAuthRole(row)
      break
    default:
      break
  }
}
/** 跳转角色分配 */
function handleAuthRole(row) {
  const userId = row.userId
  router.push('/system/user-auth/role/' + userId)
}
/** 重置密码按钮操作 */
function handleResetPwd(row) {
  proxy
    .$prompt('请输入"' + row.userName + '"的新密码', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      closeOnClickModal: false,
      inputPattern: /^.{5,20}$/,
      inputErrorMessage: '用户密码长度必须介于 5 和 20 之间',
      inputValidator: (value) => {
        if (/<|>|"|'|\||\\/.test(value)) {
          return '不能包含非法字符：< > " \' \\ |'
        }
      }
    })
    .then(({ value }) => {
      resetUserPwd(row.userId, value).then((response) => {
        proxy.$modal.msgSuccess('修改成功，新密码是：' + value)
      })
    })
    .catch(() => {})
}
/** 选择条数  */
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.userId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}
/** 重置操作表单 */
function reset() {
  form.value = {
    userId: undefined,
    deptId: 100,
    userName: undefined,
    nickName: undefined,
    password: undefined,
    phonenumber: undefined,
    email: undefined,
    sex: undefined,
    status: '0',
    remark: undefined,
    postIds: [],
    roleIds: []
  }
  proxy.resetForm('userRef')
}
/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}
/** 新增按钮操作 */
function handleAdd() {
  reset()
  getUser().then((response) => {
    postOptions.value = response.posts
    roleOptions.value = response.roles
    open.value = true
    title.value = '添加用户'
    form.value.password = initPassword.value
  })
}
/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const userId = row.userId || ids.value
  getUser(userId).then((response) => {
    form.value = response.data
    postOptions.value = response.posts
    roleOptions.value = response.roles
    form.value.postIds = response.postIds
    form.value.roleIds = response.roleIds
    open.value = true
    title.value = '修改用户'
    form.value.password = ''
  })
}
/** 提交按钮 */
function submitForm() {
  proxy.$refs['userRef'].validate((valid) => {
    if (valid) {
      if (form.value.userId != undefined) {
        updateUser(form.value).then((response) => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addUser(form.value).then((response) => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

getDeptTree()
getList()
</script>
<style lang="scss">
.user-dialog {
  .el-form {
    .el-select,
    .el-input-number {
      .el-input {
        min-width: auto;
      }
    }
  }
}
</style>
