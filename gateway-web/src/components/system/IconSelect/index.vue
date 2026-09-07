<template>
  <div class="icon-body">
    <el-input v-model="iconName" clearable placeholder="请输入图标名称" style="position: relative" @clear="filterIcons" @input="filterIcons">
      <template #suffix>
        <i class="el-icon-search el-input__icon" />
      </template>
    </el-input>
    <div class="icon-list">
      <div v-for="(item, index) in iconList" :key="index" @click="selectedIcon(item)">
        <svg-icon :icon-class="item" style="height: 30px; width: 16px; margin-right: 5px" />
        <span>{{ item }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import icons from './requireIcons'

const iconName = ref('')
const iconList = ref(icons)
const emit = defineEmits(['selected'])

function filterIcons() {
  iconList.value = icons
  if (iconName.value) {
    iconList.value = icons.filter((item) => item.indexOf(iconName.value) !== -1)
  }
}

function selectedIcon(name) {
  emit('selected', name)
  document.body.click()
}

function reset() {
  iconName.value = ''
  iconList.value = icons
}

defineExpose({
  reset
})
</script>

<style lang="scss" scoped>
.icon-body {
  width: 100%;
  padding: 10px;
  .icon-list {
    margin-top: 5px;
    height: 200px;
    overflow-y: scroll;
    @include scrollBar(5px, rgba(0, 0, 0, 0.3), transparent, 5px);
    div {
      height: 30px;
      line-height: 30px;
      margin-bottom: -5px;
      cursor: pointer;
      width: 33%;
      float: left;
    }
    span {
      display: inline-block;
      vertical-align: -0.15em;
      fill: currentColor;
      overflow: hidden;
    }
  }
}
</style>
