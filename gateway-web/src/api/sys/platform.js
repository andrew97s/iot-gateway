import request from '@/utils/request'

// 不分页查询平台信息列表
export function selectPlatform(query) {
  return request({
    url: '/sys/platform/select',
    method: 'get',
    params: query
  })
}

// 分页查询平台信息列表
export function listPlatform(query) {
  return request({
    url: '/sys/platform/list',
    method: 'get',
    params: query
  })
}

// 查询平台信息详细
export function getPlatform(id) {
  return request({
    url: '/sys/platform/' + id,
    method: 'get'
  })
}

// 新增平台信息
export function addPlatform(data) {
  return request({
    url: '/sys/platform',
    method: 'post',
    data: data
  })
}

// 修改平台信息
export function updatePlatform(data) {
  return request({
    url: '/sys/platform',
    method: 'put',
    data: data
  })
}

// 删除平台信息
export function delPlatform(id) {
  return request({
    url: '/sys/platform/' + id,
    method: 'delete'
  })
}

// 启动三方平台模块
export function startPlatform(code) {
  return request({
    url: '/sys/platform/start/' + code,
    method: 'get'
  })
}

// 停止三方平台模块
export function stopPlatform(code) {
  return request({
    url: '/sys/platform/stop/' + code,
    method: 'get'
  })
}

// 获取指定平台运行时统计
export function getPlatformStats(code) {
  return request({
    url: '/sys/platform/stats/' + code,
    method: 'get'
  })
}

// 获取所有平台运行时统计
export function getAllPlatformStats() {
  return request({
    url: '/sys/platform/stats',
    method: 'get'
  })
}

// 今日按平台消息统计
export function getTodayMsgStats() {
  return request({
    url: '/sys/platform/today-msg-stats',
    method: 'get'
  })
}

// 指定插件消息统计详情（抽屉）
export function getPluginMsgStats(code) {
  return request({
    url: '/sys/platform/msg-stats/' + code,
    method: 'get'
  })
}

// 分页查询指定平台运行日志（za_platform_log）
export function getPlatformLogs(code, query) {
  return request({
    url: '/sys/platform/logs/' + code,
    method: 'get',
    params: query
  })
}

// 清空指定平台运行日志
export function clearPlatformLogs(code) {
  return request({
    url: '/sys/platform/logs/' + code,
    method: 'delete'
  })
}
