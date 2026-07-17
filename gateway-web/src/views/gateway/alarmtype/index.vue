<template>
  <div class="gw-page">
    <!-- 筛选栏 -->
    <div class="gw-card">
      <div class="gw-card-body gw-filter-bar">
        <el-input v-model="queryParams.name" clearable placeholder="类型名称" style="width: 170px" @keyup.enter="handleQuery" />
        <el-input v-model="queryParams.code" clearable placeholder="类型编码" style="width: 150px" @keyup.enter="handleQuery" />
        <el-select v-model="queryParams.status" clearable placeholder="状态（全部）" style="width: 120px" @change="handleQuery">
          <el-option label="启用" value="1" />
          <el-option label="停用" value="0" />
        </el-select>
        <el-button icon="Search" type="primary" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <div class="spacer" />
        <span class="gw-muted gw-small">插件收到的厂商告警码经别名映射为标准告警类型后，组装统一告警消息同步上级平台</span>
        <el-button type="primary" icon="Plus" v-hasPermi="['sys:alarmtype:add']" @click="openDialog()">新增告警类型</el-button>
      </div>
    </div>

    <!-- 类型表格 -->
    <div class="gw-card">
      <div class="gw-card-body no-pad">
        <el-table :data="list" v-loading="loading">
          <el-table-column label="类型编码" width="160">
            <template #default="scope">
              <span class="gw-mono" style="font-weight: 600">{{ scope.row.code }}</span>
            </template>
          </el-table-column>
          <el-table-column label="类型名称" prop="name" min-width="130" />
          <el-table-column label="告警级别" width="110" align="center">
            <template #default="scope">
              <el-tag :type="levelTag(scope.row.level)" size="small">{{ levelLabel(scope.row.level) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="插件别名映射" min-width="260">
            <template #default="scope">
              <template v-if="parseAliases(scope.row.aliases).length > 0">
                <el-tag
                  v-for="(a, i) in parseAliases(scope.row.aliases)"
                  :key="i"
                  size="small"
                  type="info"
                  effect="plain"
                  class="alias-tag"
                >{{ a.pfCode || '通用' }} · {{ a.alias }}</el-tag>
              </template>
              <span v-else class="gw-muted gw-small">未配置</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="scope">
              <span class="gw-badge" :class="scope.row.status === '1' ? 'ok' : 'off'">
                {{ scope.row.status === '1' ? '启用' : '停用' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
          <el-table-column label="操作" width="130" align="center" fixed="right">
            <template #default="scope">
              <el-button link type="primary" v-hasPermi="['sys:alarmtype:edit']" @click="openDialog(scope.row)">修改</el-button>
              <el-button link type="danger" v-hasPermi="['sys:alarmtype:remove']" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNum" :total="total" @pagination="getList" v-show="total > 0" />
      </div>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog v-model="open" :title="form.id ? '修改告警类型' : '新增告警类型'" width="640px" append-to-body>
      <el-form :model="form" label-width="90px" ref="formRef" :rules="rules">
        <el-form-item label="类型编码" prop="code">
          <el-input v-model="form.code" placeholder="唯一编码，如 fire、smoke（同步上级平台使用）" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="类型名称" prop="name">
          <el-input v-model="form.name" placeholder="如：火警" />
        </el-form-item>
        <el-form-item label="告警级别">
          <el-select v-model="form.level" style="width: 200px">
            <el-option label="1 - 提示" :value="1" />
            <el-option label="2 - 一般" :value="2" />
            <el-option label="3 - 严重" :value="3" />
            <el-option label="4 - 紧急" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="别名映射">
          <AliasEditor v-model="form.aliases" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" active-value="1" inactive-value="0" active-text="启用" inactive-text="停用" inline-prompt />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="AlarmType">
import AliasEditor from '@/components/business/AliasEditor/index.vue'
import { alarmTypeApi } from '@/api/sys/businessType'

const { proxy } = getCurrentInstance()

const loading = ref(true)
const saving = ref(false)
const open = ref(false)
const list = ref([])
const total = ref(0)
const form = ref({})

const rules = {
  code: [{ required: true, message: '请输入类型编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入类型名称', trigger: 'blur' }]
}

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: null,
  code: null,
  status: null
})

const LEVELS = { 1: { label: '提示', tag: 'info' }, 2: { label: '一般', tag: '' }, 3: { label: '严重', tag: 'warning' }, 4: { label: '紧急', tag: 'danger' } }
function levelLabel(l) { return LEVELS[l]?.label || l || '-' }
function levelTag(l) { return LEVELS[l]?.tag || 'info' }

function parseAliases(str) {
  if (!str) return []
  try {
    const arr = JSON.parse(str)
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
}

function getList() {
  loading.value = true
  alarmTypeApi.list(queryParams).then(res => {
    list.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.name = null
  queryParams.code = null
  queryParams.status = null
  handleQuery()
}

function openDialog(row) {
  form.value = row
    ? { ...row }
    : { id: null, code: '', name: '', level: 2, aliases: '', status: '1', remark: '' }
  open.value = true
}

async function submitForm() {
  try {
    await proxy.$refs['formRef'].validate()
  } catch (e) { return }
  saving.value = true
  try {
    if (form.value.id) {
      await alarmTypeApi.update(form.value)
    } else {
      await alarmTypeApi.add(form.value)
    }
    proxy.$modal.msgSuccess('保存成功')
    open.value = false
    getList()
  } finally {
    saving.value = false
  }
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除告警类型「${row.name}」？`)
    .then(() => alarmTypeApi.remove(row.id))
    .then(() => {
      proxy.$modal.msgSuccess('删除成功')
      getList()
    })
    .catch(() => {})
}

getList()
</script>

<style scoped>
.alias-tag { margin: 2px 4px 2px 0; }
</style>
