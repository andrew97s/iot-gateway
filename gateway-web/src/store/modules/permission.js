import auth from '@/plugins/auth'
import router, { constantRoutes, dynamicRoutes } from '@/router'
import { getRouters } from '@/api/menu'
import Layout from '@/layout/index'
import ParentView from '@/components/system/ParentView'
import InnerLink from '@/layout/components/InnerLink'

// 匹配views里面所有的.vue文件
const modules = import.meta.glob(['@/views/**/*.vue', '!@/views/**/components/**/*.vue'])

const usePermissionStore = defineStore('permission', {
    state: () => ({
        routes: [],
        addRoutes: [],
        defaultRoutes: [],
        topbarRouters: [],
        sidebarRouters: []
    }),
    actions: {
        setRoutes(routes) {
            this.addRoutes = routes
            this.routes = constantRoutes.concat(routes)
        },
        setDefaultRoutes(routes) {
            this.defaultRoutes = constantRoutes.concat(routes)
        },
        setTopbarRoutes(routes) {
            this.topbarRouters = routes
        },
        setSidebarRouters(routes) {
            this.sidebarRouters = routes
        },
        generateRoutes(roles) {
            return new Promise((resolve) => {
                // 向后端请求路由数据
                getRouters().then((res) => {
                    const sdata = JSON.parse(JSON.stringify(res.data))
                    const rdata = JSON.parse(JSON.stringify(res.data))
                    const defaultData = JSON.parse(JSON.stringify(res.data))
                    const sidebarRoutes = filterAsyncRouter(sdata)
                    const rewriteRoutes = filterAsyncRouter(rdata, false, true)
                    const defaultRoutes = filterAsyncRouter(defaultData)
                    const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
                    asyncRoutes.forEach((route) => {
                        router.addRoute(route)
                    })
                    placeLogMenus(sidebarRoutes, rewriteRoutes)
                    this.setRoutes(rewriteRoutes)
                    this.setSidebarRouters(constantRoutes.concat(sidebarRoutes))
                    this.setDefaultRoutes(sidebarRoutes)
                    this.setTopbarRoutes(defaultRoutes)
                    resolve(rewriteRoutes)
                })
            })
        }
    }
})

// 遍历后台传来的路由字符串，转换为组件对象
function filterAsyncRouter(asyncRouterMap, lastRouter = false, type = false) {
    return asyncRouterMap.filter((route) => {
        if (type && route.children) {
            route.children = filterChildren(route.children)
        }
        if (route.component) {
            // Layout ParentView 组件特殊处理
            if (route.component === 'Layout') {
                route.component = Layout
            } else if (route.component === 'ParentView') {
                route.component = ParentView
            } else if (route.component === 'InnerLink') {
                route.component = InnerLink
            } else {
                route.component = loadView(route.component)
            }
        }
        if (route.children != null && route.children && route.children.length) {
            route.children = filterAsyncRouter(route.children, route, type)
        } else {
            delete route['children']
            delete route['redirect']
        }
        return true
    })
}

function filterChildren(childrenMap, lastRouter = false) {
    var children = []
    childrenMap.forEach((el, index) => {
        if (el.children && el.children.length) {
            if (el.component === 'ParentView' && !lastRouter) {
                el.children.forEach((c) => {
                    c.path = el.path + '/' + c.path
                    if (c.children && c.children.length) {
                        children = children.concat(filterChildren(c.children, c))
                        return
                    }
                    children.push(c)
                })
                return
            }
        }
        if (lastRouter) {
            el.path = lastRouter.path + '/' + el.path
        }
        children = children.concat(el)
    })
    return children
}

// 动态路由遍历，验证是否具备权限
export function filterDynamicRoutes(routes) {
    const res = []
    routes.forEach((route) => {
        if (route.permissions) {
            if (auth.hasPermiOr(route.permissions)) {
                res.push(route)
            }
        } else if (route.roles) {
            if (auth.hasRoleOr(route.roles)) {
                res.push(route)
            }
        }
    })
    return res
}

/** 历史日志 / 实时日志固定放在「系统管理 → 日志管理」下。 */
function placeLogMenus(sidebarRoutes, rewriteRoutes) {
    const sidebarTaken = takeLogMenus(sidebarRoutes)
    const rewriteTaken = takeLogMenus(rewriteRoutes)
    const allowed = sidebarTaken.length > 0 || rewriteTaken.length > 0
        || auth.hasPermi('monitor:logfile:list') || auth.hasRole('admin')
    if (!allowed) {
        return
    }
    const sidebarEntries = (sidebarTaken.length ? sidebarTaken : createLogSources())
        .sort(byLogMenuOrder)
        .map((route) => toLogRoute(route, true))
    const rewriteEntries = (rewriteTaken.length ? rewriteTaken : createLogSources())
        .sort(byLogMenuOrder)
        .map((route) => toLogRoute(route, false))
    if (!mountSidebarLogMenus(sidebarRoutes, sidebarEntries)) {
        sidebarRoutes.push(logMenuGroup(sidebarEntries))
    }
    if (!mountRewriteLogMenus(rewriteRoutes, rewriteEntries)) {
        rewriteRoutes.push(logMenuGroup(rewriteEntries.map((route) => ({
            ...route,
            path: route.path.replace(/^log\//, '')
        }))))
    }
}

function createLogSources() {
    return [
        {
            name: 'HistoryLog',
            component: () => import('@/views/monitor/logfile/history.vue'),
            meta: { title: '历史日志', icon: 'log' }
        },
        {
            name: 'RealtimeLog',
            component: () => import('@/views/monitor/logfile/realtime.vue'),
            meta: { title: '实时日志', icon: 'online' }
        }
    ]
}

function byLogMenuOrder(a, b) {
    return logMenuRank(a) - logMenuRank(b)
}

function logMenuRank(route) {
    const title = route.meta && route.meta.title
    return title === '历史日志' ? 0 : 1
}

function toLogRoute(route, nested) {
    const title = route.meta && route.meta.title
    const history = title === '历史日志'
    return {
        path: nested ? (history ? 'history' : 'realtime') : (history ? 'log/history' : 'log/realtime'),
        name: route.name || (history ? 'HistoryLog' : 'RealtimeLog'),
        component: route.component || (history
            ? () => import('@/views/monitor/logfile/history.vue')
            : () => import('@/views/monitor/logfile/realtime.vue')),
        meta: route.meta || { title, icon: history ? 'log' : 'online' }
    }
}

function logMenuGroup(children) {
    return {
        path: '/ops',
        component: Layout,
        redirect: '/ops/history',
        alwaysShow: true,
        meta: { title: '运维中心', icon: 'monitor' },
        children
    }
}

function takeLogMenus(routes) {
    const taken = []
    walk(routes)
    return taken

    function walk(list) {
        if (!list) {
            return
        }
        for (let i = list.length - 1; i >= 0; i--) {
            const route = list[i]
            const title = route.meta && route.meta.title
            if (title === '历史日志' || title === '实时日志') {
                taken.push(list.splice(i, 1)[0])
                continue
            }
            walk(route.children)
            if (title === '运维中心' && (!route.children || route.children.length === 0)) {
                list.splice(i, 1)
            }
        }
    }
}

function findByTitle(routes, title) {
    for (const route of routes || []) {
        if (route.meta && route.meta.title === title) {
            return route
        }
        const found = findByTitle(route.children, title)
        if (found) {
            return found
        }
    }
    return null
}

function mountSidebarLogMenus(routes, entries) {
    let dir = findByTitle(routes, '日志管理')
    if (!dir) {
        const system = findByTitle(routes, '系统管理')
        if (!system) {
            return false
        }
        if (!system.children) {
            system.children = []
        }
        dir = {
            path: 'log',
            component: ParentView,
            alwaysShow: true,
            redirect: 'noRedirect',
            meta: { title: '日志管理', icon: 'log' },
            children: []
        }
        system.children.push(dir)
    }
    if (!dir.children) {
        dir.children = []
    }
    entries.forEach((entry) => dir.children.push(entry))
    return true
}

function mountRewriteLogMenus(routes, entries) {
    let parent = findRewriteLogParent(routes)
    if (!parent) {
        parent = findByTitle(routes, '系统管理')
    }
    if (!parent) {
        return false
    }
    if (!parent.children) {
        parent.children = []
    }
    entries.forEach((entry) => parent.children.push(entry))
    return true
}

function findRewriteLogParent(routes) {
    for (const route of routes || []) {
        const children = route.children || []
        if (children.some((child) => child.path === 'log' || (typeof child.path === 'string' && child.path.indexOf('log/') === 0))) {
            return route
        }
        const nested = findRewriteLogParent(children)
        if (nested) {
            return nested
        }
    }
    return null
}

export const loadView = (view) => {
    let res
    for (const path in modules) {
        const dir = path.split('views/')[1].split('.vue')[0]
        if (dir === view) {
            res = () => modules[path]()
        }
    }
    return res
}

export default usePermissionStore