<template>
  <div class="navbar">
    <hamburger id="hamburger-container" class="hamburger-container" :is-active="appStore.sidebar.opened" @toggleClick="toggleSideBar" />
    <div v-if="!settingsStore.topNav" class="page-title">{{ pageTitle }}</div>
    <top-nav id="topmenu-container" class="topmenu-container" v-if="settingsStore.topNav" />

    <NoticeBar />

    <div class="right-menu">
      <div class="qr-box" v-if="appDownloadUrl">
        <el-dropdown class="right-menu-item hover-effect" trigger="click">
          <span class="text-box">
            app下载<el-icon><caret-bottom /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu class="drop-menu">
              <Qrcode :margin="3" :size="150" :value="appDownloadUrl" />
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <!-- <template v-if="appStore.device !== 'mobile'"> -->
      <!-- <el-link type="primary" style="margin-right: 20px" @click="router.push(defaultSettings.homePath)">返回首页</el-link> -->
      <header-search id="header-search" class="right-menu-item" />

      <screenfull id="screenfull" class="right-menu-item hover-effect" />

      <!-- <el-tooltip content="布局大小" effect="dark" placement="bottom">
          <size-select id="size-select" class="right-menu-item hover-effect" />
        </el-tooltip> -->
      <!-- </template> -->
      <div class="avatar-container">
        <el-dropdown class="right-menu-item hover-effect" trigger="click" @command="handleCommand">
          <div class="avatar-wrapper">
            <span class="nickname">{{ userStore.userInfo.nickName }}</span>
            <!-- <img :src="userStore.avatar" class="user-avatar" /> -->
            <el-icon><caret-bottom /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <router-link to="/user/profile">
                <el-dropdown-item>个人中心</el-dropdown-item>
              </router-link>
              <el-dropdown-item command="setLayout" v-if="settingsStore.showSettings">
                <span>布局设置</span>
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <span>退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessageBox } from 'element-plus'
import TopNav from '@/components/system/TopNav/index.vue'
import Hamburger from '@/components/system/Hamburger/index.vue'
import Screenfull from '@/components/system/Screenfull/index.vue'
import SizeSelect from '@/components/system/SizeSelect/index.vue'
import HeaderSearch from '@/components/system/HeaderSearch/index.vue'
import useAppStore from '@/store/modules/app'
import useUserStore from '@/store/modules/user'
import useSettingsStore from '@/store/modules/settings'
import defaultSettings from '@/settings'
import Qrcode from 'qrcode.vue'
import NoticeBar from './NoticeBar.vue'
import { useRoute } from 'vue-router'

let hostInfo = {}
try {
  hostInfo = hostConfig
} catch {}
const appDownloadUrl = hostInfo.appDownloadUrl

const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()
const settingsStore = useSettingsStore()
const router = useRouter()

/** 顶部展示当前页标题（对齐原型 topbar，替代面包屑） */
const pageTitle = computed(() => {
  const matched = route.matched.filter((item) => item.meta && item.meta.title)
  if (!matched.length) return settingsStore.title || ''
  return matched[matched.length - 1].meta.title
})

function toggleSideBar() {
  appStore.toggleSideBar()
}

function handleCommand(command) {
  switch (command) {
    case 'setLayout':
      setLayout()
      break
    case 'logout':
      logout()
      break
    default:
      break
  }
}

function logout() {
  ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      userStore.logOut().then(() => {
        location.href = defaultSettings.homePath
      })
    })
    .catch(() => {})
}

const emits = defineEmits(['setLayout'])
function setLayout() {
  emits('setLayout')
}
</script>
<style>
.drop-menu {
  padding: 0 !important;
  line-height: 0;
}
</style>
<style lang="scss" scoped>
.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  box-shadow: none;

  .hamburger-container {
    line-height: 46px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background 0.3s;
    -webkit-tap-highlight-color: transparent;

    &:hover {
      background: rgba(0, 0, 0, 0.025);
    }
  }

  .page-title {
    float: left;
    height: 100%;
    display: flex;
    align-items: center;
    margin-left: 4px;
    font-size: 17px;
    font-weight: 600;
    color: #0f172a;
    letter-spacing: 0.2px;
  }

  .topmenu-container {
    position: absolute;
    left: 50px;
  }

  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }

  .right-menu {
    float: right;
    height: 100%;
    line-height: 50px;
    display: flex;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: inline-block;
      padding: 0 8px;
      height: 100%;
      font-size: 18px;
      color: #5a5e66;
      vertical-align: text-bottom;

      &.hover-effect {
        cursor: pointer;
        transition: background 0.3s;

        &:hover {
          background: rgba(0, 0, 0, 0.025);
        }
      }
    }

    .avatar-container {
      margin-right: 40px;

      .avatar-wrapper {
        position: relative;
        height: 100%;
        display: flex;
        align-items: center;
        .user-avatar {
          cursor: pointer;
          width: 40px;
          height: 40px;
          border-radius: 10px;
        }

        i {
          cursor: pointer;
          position: absolute;
          right: -20px;
          top: 20px;
          font-size: 12px;
        }
      }
    }
    .qr-box {
      margin-right: 10px;

      .text-box {
        height: 100%;
        display: flex;
        align-items: center;
        i {
          font-size: 12px;
        }
      }
    }
  }
}
</style>
