<template>
  <div
    class="sidebar-logo-container"
    :class="{ collapse: collapse }"
    :style="{ backgroundColor: sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground }"
  >
    <transition name="sidebarLogoFade">
      <router-link class="sidebar-logo-link" to="/" key="collapse" v-if="collapse">
        <img class="sidebar-logo" :src="logo" v-if="logo && showAsideLogo" />
        <h1 class="sidebar-title" :style="{ color: sideTheme === 'theme-dark' ? variables.logoTitleColor : variables.logoLightTitleColor }" v-else>
          {{ title }}
        </h1>
      </router-link>
      <router-link class="sidebar-logo-link" to="/" key="expand" v-else>
        <img class="sidebar-logo" :src="logo" v-if="logo && showAsideLogo" />
        <h1 class="sidebar-title" :style="{ color: sideTheme === 'theme-dark' ? variables.logoTitleColor : variables.logoLightTitleColor }">
          {{ title }}
        </h1>
      </router-link>
    </transition>
  </div>
</template>

<script setup>
import variables from '@/assets/styles/variables.module.scss'
import logo from '/resource/aside-logo.png'
import useSettingsStore from '@/store/modules/settings'
import defaultSettings from '@/settings'

defineProps({
  collapse: {
    type: Boolean,
    required: true
  }
})
const title = ref(defaultSettings.sidebarTitle)
const showAsideLogo = ref(defaultSettings.showAsideLogo)
const settingsStore = useSettingsStore()
const sideTheme = computed(() => settingsStore.sideTheme)
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  position: relative;
  width: 100%;
  height: 50px;
  line-height: 50px;
  background: #2b2f3a;
  text-align: center;
  overflow: hidden;

  & .sidebar-logo-link {
    height: 100%;
    width: 100%;

    & .sidebar-logo {
      width: 32px;
      height: 32px;
      vertical-align: middle;
      margin-right: 12px;
    }

    & .sidebar-title {
      display: inline-block;
      margin: 0;
      color: #fff;
      font-weight: 600;
      line-height: 50px;
      font-size: 14px;
      font-family:
        Avenir,
        Helvetica Neue,
        Arial,
        Helvetica,
        sans-serif;
      vertical-align: middle;
    }
  }

  &.collapse {
    .sidebar-logo {
      margin-right: 0px;
    }
  }
}
</style>
