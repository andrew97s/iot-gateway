<template>
  <div>
    <template v-for="(item, index) in options">
      <template v-if="values.includes(item.value)">
        <span :class="item.elTagClass" :index="index" :key="item.value" v-if="item.elTagType == 'default' || item.elTagType == ''">{{
          item.label
        }}</span>
        <el-tag
          :class="item.elTagClass"
          :disable-transitions="true"
          :index="index"
          :type="item.elTagType === 'primary' ? '' : item.elTagType"
          :key="item.value + ''"
          v-else
        >
          {{ item.label }}
        </el-tag>
      </template>
    </template>
  </div>
</template>

<script setup>
const props = defineProps({
  // 数据
  options: {
    type: Array,
    default: null
  },
  // 当前的值
  value: {
    type: [Number, String, Array],
    default: () => ''
  }
})

const values = computed(() => {
  if (props.value !== null && typeof props.value !== 'undefined') {
    return Array.isArray(props.value) ? props.value : [String(props.value)]
  } else {
    return []
  }
})
</script>

<style scoped>
.el-tag + .el-tag {
  margin-left: 10px;
}
</style>
