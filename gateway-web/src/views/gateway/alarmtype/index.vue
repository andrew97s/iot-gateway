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
          <el-table-column label="告警类别" width="110" align="center">
            <template #default="scope">
              <el-tag :type="levelTag(scope.row.type)" size="small">{{ levelLabel(scope.row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="类型编码" width="100">
            <template #default="scope">
              <span class="gw-mono" style="font-weight: 600">{{ scope.row.code }}</span>
            </template>
          </el-table-column>
          <el-table-column label="类型名称" prop="name" min-width="130" />
          <el-table-column label="恢复事件" min-width="160">
            <template #default="scope">
              <template v-if="parseCodes(scope.row.cancelCode).length > 0">
                <el-tag
                  v-for="code in parseCodes(scope.row.cancelCode)"
                  :key="code"
                  size="small"
                  type="success"
                  effect="plain"
                  class="alias-tag"
                >{{ codeName(code) }}</el-tag>
              </template>
              <span v-else class="gw-muted gw-small">未配置</span>
            </template>
          </el-table-column>
          <el-table-column label="被恢复事件" min-width="160">
            <template #default="scope">
              <template v-if="parseCodes(scope.row.recoveryCode).length > 0">
                <el-tag
                  v-for="code in parseCodes(scope.row.recoveryCode)"
                  :key="code"
                  size="small"
                  type="warning"
                  effect="plain"
                  class="alias-tag"
                >{{ codeName(code) }}</el-tag>
              </template>
              <span v-else class="gw-muted gw-small">未配置</span>
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
    <el-dialog v-model="open" :title="form.id ? '修改告警类型' : '新增告警类型'" width="720px" append-to-body destroy-on-close>
      <el-form :model="form" label-width="108px" ref="formRef" :rules="rules" class="type-form">
        <el-form-item label="类型编码" prop="code">
          <el-input v-model="form.code" placeholder="唯一编码，如 fire、smoke（同步上级平台使用）" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="类型名称" prop="name">
          <el-input v-model="form.name" placeholder="如：火警" />
        </el-form-item>
        <el-form-item label="告警类别">
          <el-select v-model="form.level" style="width: 200px">
            <el-option label="1 - 火警" :value="1" />
            <el-option label="2 - 预警" :value="2" />
            <el-option label="3 - 故障" :value="3" />
            <el-option label="4 - 事件" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="恢复事件">
          <el-select-v2
            v-model="form.cancelCodes"
            :options="typeSelectOptions"
            multiple
            filterable
            clearable
            placeholder="当前告警可恢复的告警事件"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="被恢复事件">
          <el-select-v2
            v-model="form.recoveryCodes"
            :options="typeSelectOptions"
            multiple
            filterable
            clearable
            placeholder="可恢复当前告警的告警事件"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="别名映射" class="alias-item">
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
const emptyForm = () => ({
  id: null,
  code: '',
  name: '',
  level: 2,
  aliases: '',
  cancelCode: '',
  recoveryCode: '',
  cancelCodes: [],
  recoveryCodes: [],
  status: '1',
  remark: ''
})
const form = ref(emptyForm())
const allTypes = ref([])

const typeMap = computed(() => {
  const map = {}
  allTypes.value.forEach(t => {
    if (t.code) map[t.code] = t.name
  })
  return map
})

const typeSelectOptions = computed(() => {
  const current = form.value.code
  return allTypes.value
    .filter(t => t.code)
    .map(t => ({
      value: t.code,
      label: t.name,
      disabled: !!current && t.code === current
    }))
})

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

const LEVELS = { 1: { label: '火警', tag: 'danger' }, 2: { label: '预警', tag: 'warning' }, 3: { label: '故障', tag: 'info' }, 4: { label: '事件', tag: 'primary' } }
function levelLabel(l) { return LEVELS[l]?.label || l || '-' }
function levelTag(l) { return LEVELS[l]?.tag || 'info' }

function parseAliases(str) {
  if (!str) return []
  try {
    const arr = JSON.parse(str)
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
}

function parseCodes(str) {
  if (!str) return []
  if (Array.isArray(str)) return str.filter(Boolean)
  try {
    const arr = JSON.parse(str)
    if (Array.isArray(arr)) {
      return arr.map(v => typeof v === 'string' ? v : v?.code).filter(Boolean)
    }
  } catch { /* 兼容逗号分隔 */ }
  return String(str).split(',').map(s => s.trim()).filter(Boolean)
}

function stringifyCodes(codes) {
  return Array.isArray(codes) && codes.length > 0 ? JSON.stringify(codes) : ''
}

function codeName(code) {
  return typeMap.value[code] || code
}

function loadAllTypes() {
  return alarmTypeApi.select().then(res => {
    allTypes.value = res.data || []
  }).catch(() => {
    allTypes.value = []
  })
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
    ? { ...emptyForm(), ...row, cancelCodes: parseCodes(row.cancelCode), recoveryCodes: parseCodes(row.recoveryCode) }
    : emptyForm()
  open.value = true
}

async function submitForm() {
  try {
    await proxy.$refs['formRef'].validate()
  } catch (e) { return }
  saving.value = true
  const payload = {
    ...form.value,
    cancelCode: stringifyCodes(form.value.cancelCodes),
    recoveryCode: stringifyCodes(form.value.recoveryCodes)
  }
  delete payload.cancelCodes
  delete payload.recoveryCodes
  try {
    if (payload.id) {
      await alarmTypeApi.update(payload)
    } else {
      await alarmTypeApi.add(payload)
    }
    proxy.$modal.msgSuccess('保存成功')
    open.value = false
    getList()
    loadAllTypes()
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
      loadAllTypes()
    })
    .catch(() => {})
}

loadAllTypes()
getList()
</script>

<style scoped>
.alias-tag { margin: 2px 4px 2px 0; }
.type-form :deep(.el-form-item__content) { min-width: 0; }
.type-form :deep(.alias-item .el-form-item__content) { display: block; width: 100%; }
</style>
