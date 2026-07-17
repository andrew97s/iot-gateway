<template>
  <div class="alias-editor">
    <div class="alias-row alias-head" v-if="rows.length > 0">
      <span class="col-pf">接入插件</span>
      <span class="col-alias">厂商别名</span>
      <span class="col-op"></span>
    </div>
    <div class="alias-row" v-for="(row, idx) in rows" :key="idx">
      <div class="col-pf">
        <el-select v-model="row.pfCode" clearable filterable placeholder="全部插件（通用）" style="width: 100%">
          <el-option v-for="pf in platformOptions" :key="pf.code" :label="pf.name" :value="pf.code" />
        </el-select>
      </div>
      <div class="col-alias">
        <el-input v-model="row.alias" placeholder="厂商告警码 / 类型码 / 监测项码" @input="emitChange" />
      </div>
      <div class="col-op">
        <el-button link type="danger" icon="Delete" @click="removeRow(idx)" />
      </div>
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
.alias-editor {
  width: 100%;
  min-width: 0;
}
.alias-row {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  width: 100%;
  min-width: 0;
}
/* 用 margin 保证列间距（不依赖 flex gap） */
.alias-row > * + * {
  margin-left: 12px;
}
.alias-head {
  margin-bottom: 4px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.2;
}
.col-pf {
  width: 200px;
  flex: 0 0 200px;
  min-width: 0;
}
.col-alias {
  flex: 1 1 auto;
  min-width: 0;
}
.col-op {
  width: 32px;
  flex: 0 0 32px;
  display: flex;
  justify-content: center;
}
.alias-hint {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 6px;
  line-height: 1.5;
}

@media (max-width: 640px) {
  .alias-row {
    flex-wrap: wrap;
  }
  .col-pf {
    width: 100%;
    flex: 1 1 100%;
    margin-left: 0 !important;
    margin-bottom: 8px;
  }
  .col-alias {
    flex: 1 1 calc(100% - 44px);
    margin-left: 0 !important;
  }
}
</style>
