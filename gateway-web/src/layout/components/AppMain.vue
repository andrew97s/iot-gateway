<template>
  <section class="app-main">
    <router-view v-slot="{ Component, route }">
      <transition mode="out-in" name="fade-transform">
        <keep-alive :include="tagsViewStore.cachedViews">
          <component :is="Component" :key="route.path" v-if="!route.meta.link" />
        </keep-alive>
      </transition>
    </router-view>
    <iframe-toggle />
  </section>
</template>

<script setup>
import iframeToggle from './IframeToggle/index.vue'
import useTagsViewStore from '@/store/modules/tagsView'

const tagsViewStore = useTagsViewStore()
</script>

<style lang="scss" scoped>
.app-main {
  /* 铺满视口灰底；固定顶栏用 padding-top 让出空间 */
  min-height: 100vh;
  width: 100%;
  position: relative;
  overflow: auto;
  background: #f1f5f9;
  box-sizing: border-box;
}

.fixed-header + .app-main {
  padding-top: 50px;
}

.hasTagsView {
  .fixed-header + .app-main {
    padding-top: 84px;
  }
}
</style>
