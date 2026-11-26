<template>
  <el-tree-select
    :style="{ width }"
    :value="value"
    check-strictly
    :props="options"
    :data="treeData"
    :default-expand-all="isExpandAll"
    clearable
    filterable
    @change="selectChange"
  />
</template>

<script setup>
import { deptTreeSelect } from '@/api/system/user'

const props = defineProps({
  value: {
    type: [Number, String],
    default: ''
  },
  // 选择框提示语
  placeholder: {
    type: String,
    default: '请选择'
  },
  // 是否回显单位
  orgName: {
    type: String,
    default: null
  },
  width: {
    type: String,
    default: 'unset'
  },
  isExpandAll: {
    type: Boolean,
    default: true
  },
  clearable: {
    type: Boolean,
    default: true
  },
  query: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['selectChange'])

const treeData = ref([])
const options = ref({
  label: 'label',
  value: 'id',
  children: 'children'
})

// 点击选择单位，触发事件
function selectChange(nodeId) {
  emit('selectChange', nodeId)
}

// 处理组织单位名称
function getTreeData() {
  deptTreeSelect(props.query).then((res) => {
    treeData.value = res.data
  })
}
getTreeData()
</script>

<style lang="scss" scoped></style>
