<template>
  <div class="gw-page">
    <!-- 筛选栏 -->
    <div class="gw-card">
      <div class="gw-card-body gw-filter-bar">
        <el-input v-model="queryParams.name" clearable placeholder="类型名称" style="width: 160px" @keyup.enter="handleQuery" />
        <el-input v-model="queryParams.code" clearable placeholder="类型编码" style="width: 140px" @keyup.enter="handleQuery" />
        <el-select v-model="queryParams.valueType" clearable placeholder="值类型（全部）" style="width: 130px" @change="handleQuery">
          <el-option label="枚举值" value="enum" />
          <el-option label="线性值" value="linear" />
        </el-select>
        <el-select v-model="queryParams.status" clearable placeholder="状态（全部）" style="width: 120px" @change="handleQuery">
          <el-option label="启用" value="1" />
          <el-option label="停用" value="0" />
        </el-select>
        <el-button icon="Search" type="primary" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <div class="spacer" />
        <span class="gw-muted gw-small">枚举值：在线/离线、开关量等；线性值：压力、液位、电流、电压、信号强度、电量等</span>
        <el-button type="primary" icon="Plus" v-hasPermi="['sys:monitortype:add']" @click="openDialog()">新增监测类型</el-button>
      </div>
    </div>

    <!-- 类型表格 -->
    <div class="gw-card">
      <div class="gw-card-body no-pad">
        <el-table :data="list" v-loading="loading">
          <el-table-column label="类型编码" width="150">
            <template #default="scope">
              <span class="gw-mono" style="font-weight: 600">{{ scope.row.code }}</span>
            </template>
          </el-table-column>
          <el-table-column label="类型名称" prop="name" min-width="110" />
          <el-table-column label="值类型" width="90" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.valueType === 'enum' ? 'warning' : 'primary'" size="small" effect="plain">
                {{ scope.row.valueType === 'enum' ? '枚举值' : '线性值' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="单位" width="80" align="center">
            <template #default="scope">
              <span v-if="scope.row.valueType === 'linear'">{{ scope.row.unit || '-' }}</span>
              <span v-else class="gw-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="枚举值 / 取值" min-width="170">
            <template #default="scope">
              <template v-if="scope.row.valueType === 'enum'">
                <el-tag v-for="(o, i) in parseJsonArr(scope.row.enumOptions)" :key="i" size="small" class="alias-tag" effect="plain">
                  {{ o.value }}={{ o.label }}
                </el-tag>
              </template>
              <span v-else class="gw-muted gw-small">连续数值</span>
            </template>
          </el-table-column>
          <el-table-column label="插件别名映射" min-width="200">
            <template #default="scope">
              <template v-if="parseJsonArr(scope.row.aliases).length > 0">
                <el-tag v-for="(a, i) in parseJsonArr(scope.row.aliases)" :key="i" size="small" type="info" effect="plain" class="alias-tag">
                  {{ a.pfCode || '通用' }} · {{ a.alias }}
                </el-tag>
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
          <el-table-column label="操作" width="130" align="center" fixed="right">
            <template #default="scope">
              <el-button link type="primary" v-hasPermi="['sys:monitortype:edit']" @click="openDialog(scope.row)">修改</el-button>
              <el-button link type="danger" v-hasPermi="['sys:monitortype:remove']" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNum" :total="total" @pagination="getList" v-show="total > 0" />
      </div>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog v-model="open" :title="form.id ? '修改监测类型' : '新增监测类型'" width="680px" append-to-body>
      <el-form :model="form" label-width="90px" ref="formRef" :rules="rules">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="类型编码" prop="code">
              <el-input v-model="form.code" placeholder="如 pressure、signal" :disabled="!!form.id" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型名称" prop="name">
              <el-input v-model="form.name" placeholder="如：压力" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="值类型" prop="valueType">
              <el-radio-group v-model="form.valueType">
                <el-radio label="linear">线性值</el-radio>
                <el-radio label="enum">枚举值</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.valueType === 'linear'">
            <el-form-item label="监测单位">
              <el-input v-model="form.unit" placeholder="如 MPa / V / A / % / dBm / ℃" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 枚举值编辑器 -->
        <el-form-item label="枚举值" v-if="form.valueType === 'enum'">
          <div class="enum-editor">
            <div class="enum-row" v-for="(row, idx) in enumRows" :key="idx">
              <el-input v-model="row.value" placeholder="值（如 1）" style="width: 140px" />
              <el-input v-model="row.label" placeholder="含义（如 在线）" style="width: 200px" />
              <el-button link type="danger" icon="Delete" @click="enumRows.splice(idx, 1)" />
            </div>
            <el-button size="small" plain icon="Plus" @click="enumRows.push({ value: '', label: '' })">添加枚举值</el-button>
          </div>
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

<script setup name="MonitorType">
import AliasEditor from '@/components/business/AliasEditor/index.vue'
import { monitorTypeApi } from '@/api/sys/businessType'

const { proxy } = getCurrentInstance()

const loading = ref(true)
const saving = ref(false)
const open = ref(false)
const list = ref([])
const total = ref(0)
const form = ref({})
const enumRows = ref([])

const rules = {
  code: [{ required: true, message: '请输入类型编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入类型名称', trigger: 'blur' }]
}

const queryParams = reactive({
  pageNum: 1,
  pageSize: 15,
  name: null,
  code: null,
  valueType: null,
  status: null
})

function parseJsonArr(str) {
  if (!str) return []
  try {
    const arr = JSON.parse(str)
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
}

function getList() {
  loading.value = true
  monitorTypeApi.list(queryParams).then(res => {
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
  queryParams.valueType = null
  queryParams.status = null
  handleQuery()
}

function openDialog(row) {
  form.value = row
    ? { ...row }
    : { id: null, code: '', name: '', valueType: 'linear', unit: '', enumOptions: '', aliases: '', status: '1', remark: '' }
  enumRows.value = parseJsonArr(form.value.enumOptions).map(o => ({ value: o.value ?? '', label: o.label ?? '' }))
  open.value = true
}

async function submitForm() {
  try {
    await proxy.$refs['formRef'].validate()
  } catch (e) { return }
  // 序列化枚举值
  if (form.value.valueType === 'enum') {
    const opts = enumRows.value.filter(r => r.value !== '' && r.label !== '')
    form.value.enumOptions = opts.length > 0 ? JSON.stringify(opts) : ''
    form.value.unit = ''
  } else {
    form.value.enumOptions = ''
  }
  saving.value = true
  try {
    if (form.value.id) {
      await monitorTypeApi.update(form.value)
    } else {
      await monitorTypeApi.add(form.value)
    }
    proxy.$modal.msgSuccess('保存成功')
    open.value = false
    getList()
  } finally {
    saving.value = false
  }
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除监测类型「${row.name}」？`)
    .then(() => monitorTypeApi.remove(row.id))
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
.enum-editor { width: 100%; }
.enum-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
</style>
