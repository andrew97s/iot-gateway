import router from './router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isHttp } from '@/utils/validate'
import { isRelogin } from '@/utils/request'
import useAppStore from '@/store/modules/app'
import useUserStore from '@/store/modules/user'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'
import { getWebSocketUrl } from '@/utils/index'
import SocketUtil from '@/utils/socket'

import { MSG_TYPE } from './business'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  NProgress.start()
  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title)
    /* has token*/
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else {
      if (userStore.roles.length === 0) {
        useAppStore().initEventBus()
        isRelogin.show = true
        // 判断当前用户是否已拉取完user_info信息
        userStore
          .getInfo()
          .then(() => {
            isRelogin.show = false
            usePermissionStore()
              .generateRoutes()
              .then((accessRoutes) => {
                // 根据roles权限生成可访问的路由表
                accessRoutes.forEach((route) => {
                  if (!isHttp(route.path)) {
                    router.addRoute(route) // 动态添加可访问路由表
                  }
                })
                next({ ...to, replace: true }) // hack方法 确保addRoutes已完成

                // socketWatchInit(userStore.userInfo)
              })
          })
          .catch((err) => {
            userStore.logOut().then(() => {
              ElMessage.error(err)
              next({ path: '/' })
            })
          })
      } else {
        next()
      }
    }
  } else {
    // 没有token
    if (whiteList.indexOf(to.path) !== -1) {
      // 在免登录白名单，直接进入
      next()
    } else {
      next(`/login?redirect=${to.fullPath}`) // 否则全部重定向到登录页
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

// socket相关
function socketWatchInit(userInfo) {
  let socket = null
  if (!userInfo?.userId || socket) return

  function dataHandle(res) {
    useAppStore().eventBus.emit(res.type, res)
  }

  socket = new SocketUtil(`ws:${getWebSocketUrl()}/ws/${userInfo?.userId}`)
  socket.wsSend(
    {
      type: 1000,
      msgTypes: Object.values(MSG_TYPE)
    },
    (res) => {
      // switch (res.type) {
      //   case MSG_TYPE.warning_message:
      //     break
      //   case MSG_TYPE.facility_state:
      //     break
      // }
      dataHandle(res)
    }
  )
}
