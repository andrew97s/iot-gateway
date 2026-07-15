<template>
  <div class="alias-editor">
    <div class="alias-row alias-head" v-if="rows.length > 0">
      <span>接入插件</span>
      <span>厂商别名</span>
      <span style="width: 32px"></span>
    </div>
    <div class="alias-row" v-for="(row, idx) in rows" :key="idx">
      <el-select v-model="row.pfCode" clearable filterable placeholder="全部插件（通用）" size="default">
        <el-option v-for="pf in platformOptions" :key="pf.code" :label="pf.name" :value="pf.code" />
      </el-select>
      <el-input v-model="row.alias" placeholder="厂商告警码 / 类型码 / 监测项码" @input="emitChange" />
      <el-button link type="danger" icon="Delete" @click="removeRow(idx)" />
    </div>
    <el-button size="small" plain icon="Plus" @click="addRow">添加别名映射</el-button>
    <div class="alias-hint">同一标准类型可配置多个插件别名；插件留空表示该别名对所有插件生效</div>
  </div>
</template>

<script setup name="AliasEditor">
/**
 * 插件别名映射编辑器
 * v-model 值为 JSON 字符串：[{"pfCode":"jb","alias":"4"}, ...]
 */
import { selectPlatform } from '@/api/sys/platform'

const props = defineProps({
  modelValue: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])

const rows = ref([])
const platformOptions = ref([])

selectPlatform().then(res => {
  platformOptions.value = (res.data || []).filter(p => !['gateway', 'cascade', 'cascade-server'].includes(p.code))
})

watch(
  () => props.modelValue,
  val => {
    let parsed = []
    if (val) {
      try { parsed = JSON.parse(val) } catch {}
    }
    if (!Array.isArray(parsed)) parsed = []
    // 避免自身 emit 导致的循环重置
    if (JSON.stringify(parsed) !== JSON.stringify(currentValue())) {
      rows.value = parsed.map(r => ({ pfCode: r.pfCode || '', alias: r.alias || '' }))
    }
  },
  { immediate: true }
)

watch(rows, emitChange, { deep: true })

function currentValue() {
  return rows.value
    .filter(r => r.alias && String(r.alias).trim() !== '')
    .map(r => (r.pfCode ? { pfCode: r.pfCode, alias: String(r.alias).trim() } : { alias: String(r.alias).trim() }))
}

function emitChange() {
  const v = currentValue()
  emit('update:modelValue', v.length > 0 ? JSON.stringify(v) : '')
}

function addRow() {
  rows.value.push({ pfCode: '', alias: '' })
}

function removeRow(idx) {
  rows.value.splice(idx, 1)
}
</script>

<style scoped>
.alias-editor { width: 100%; }
.alias-row {
  display: grid;
  grid-template-columns: 200px 1fr 32px;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.alias-head {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 4px;
}
.alias-hint {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 6px;
}
</style>
