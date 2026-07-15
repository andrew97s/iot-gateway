<template>
  <el-select
    :value="value"
    filterable
    placeholder="请选择"
    :disabled="disabled"
    :clearable="clearable"
    :multiple="multiple"
    collapse-tags
    collapse-tags-tooltip
    @change="userSelectChange"
  >
    <el-option v-for="item in userList" :label="item.nickName" :value="item.userId" :key="item.userId">
      <span>{{ item.nickName }}</span>
      <span style="color: var(--el-text-color-secondary); font-size: 13px">（{{ item.dept?.deptName }}）</span>
    </el-option>
  </el-select>
</template>
<script setup>
import { userSelect } from '@/api/system/user'

const props = defineProps({
  value: {
    type: [Number, String],
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  },
  clearable: {
    type: Boolean,
    default: true
  },
  excludeUserIds: {
    type: Array,
    default: () => []
  },
  query: {
    type: Object,
    default: () => ({})
  },
  multiple: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['selectChange'])

const userList = ref([])
userSelect(props.query).then((res) => {
  userList.value = res.rows.filter((v) => {
    return props.excludeUserIds.every((v2) => v2 != v.userId)
  })
})
function userSelectChange(val) {
  const item = userList.value.find((v) => v.userId == val) || {}
  emit('selectChange', item)
}
</script>
