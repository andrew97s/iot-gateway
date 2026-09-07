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

      <div class="meta-item system-time">{{ systemTime }}</div>
      <span class="upstream-badge" :class="upstreamBadgeClass">{{ upstreamBadgeText }}</span>

      <screenfull id="screenfull" class="right-menu-item hover-effect" />

      <div class="avatar-container">
        <el-dropdown class="right-menu-item hover-effect" trigger="click" @command="handleCommand">
          <div class="avatar-wrapper">
            <span class="nickname">{{ userStore.userInfo.nickName }}</span>
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
import useAppStore from '@/store/modules/app'
import useUserStore from '@/store/modules/user'
import useSettingsStore from '@/store/modules/settings'
import defaultSettings from '@/settings'
import Qrcode from 'qrcode.vue'
import NoticeBar from './NoticeBar.vue'
import { useRoute } from 'vue-router'
import { getUpstreamStatus } from '@/api/sys/upstream'

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

const systemTime = ref('')
const upstreamOnline = ref(0)
const upstreamTotal = ref(0)
let clockTimer = null
let upstreamTimer = null

const upstreamBadgeText = computed(() => {
  if (!upstreamTotal.value) return '上级平台 —'
  return `上级平台 ${upstreamOnline.value}/${upstreamTotal.value} 在线`
})
const upstreamBadgeClass = computed(() => {
  if (!upstreamTotal.value) return 'off'
  return upstreamOnline.value >= upstreamTotal.value ? 'ok' : 'warn'
})

function pad(n) {
  return String(n).padStart(2, '0')
}
function tickClock() {
  const d = new Date()
  systemTime.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

async function loadUpstreamStatus() {
  try {
    const res = await getUpstreamStatus()
    const list = res.data || []
    upstreamTotal.value = list.length
    upstreamOnline.value = list.filter((s) => s.alive).length
  } catch {
    /* 忽略状态拉取失败，保留上次结果 */
  }
}

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

onMounted(() => {
  tickClock()
  clockTimer = setInterval(tickClock, 1000)
  loadUpstreamStatus()
  upstreamTimer = setInterval(loadUpstreamStatus, 30000)
})
onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (upstreamTimer) clearInterval(upstreamTimer)
})
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
    align-items: center;
    gap: 12px;
    padding-right: 8px;

    &:focus {
      outline: none;
    }

    .meta-item {
      font-size: 13px;
      color: #64748b;
      line-height: 1;
      white-space: nowrap;
      font-variant-numeric: tabular-nums;
    }

    .upstream-badge {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 4px 10px;
      font-size: 12px;
      border-radius: 999px;
      line-height: 1;
      white-space: nowrap;

      &::before {
        content: '';
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: currentColor;
      }

      &.ok {
        background: #f0fdf4;
        color: #16a34a;
      }
      &.warn {
        background: #fffbeb;
        color: #d97706;
      }
      &.off {
        background: #f1f5f9;
        color: #64748b;
      }
    }

    .right-menu-item {
      display: inline-flex;
      align-items: center;
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
      margin-right: 24px;

      .avatar-wrapper {
        position: relative;
        height: 100%;
        display: flex;
        align-items: center;
        gap: 4px;

        .nickname {
          font-size: 13px;
          color: #334155;
        }

        .user-avatar {
          cursor: pointer;
          width: 40px;
          height: 40px;
          border-radius: 10px;
        }

        i {
          cursor: pointer;
          font-size: 12px;
        }
      }
    }
    .qr-box {
      margin-right: 4px;

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
