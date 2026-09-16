<template>
  <div class="app-container">
    <el-icon class="icon">
      <Setting />
    </el-icon>
    <p class="tips-text">正在开发中...</p>
  </div>
</template>

<script>
export default {
  name: 'Developing',
  data() {
    return {}
  },
  mounted() {},
  methods: {}
}
</script>

<style lang="scss" scoped>
.app-container {
  text-align: center;
  padding-top: 200px;
  font-size: 14px;
  color: #999;
  .icon {
    font-size: 36px;
    margin-bottom: 5px;
    transform-origin: center;
    animation: rotate 10s infinite linear;
  }
}
@keyframes rotate {
  to {
    transform: rotate(360deg);
  }
}
</style>
