import request from '@/utils/request'

// 不分页查询设备管理列表
export function selectDevice(query) {
  return request({
    url: '/sys/device/select',
    method: 'get',
    params: query
  })
}

// 分页查询设备管理列表
export function listDevice(query) {
  return request({
    url: '/sys/device/list',
    method: 'get',
    params: query
  })
}

// 查询设备管理详细
export function getDevice(id) {
  return request({
    url: '/sys/device/' + id,
    method: 'get'
  })
}

// 新增设备管理
export function addDevice(data) {
  return request({
    url: '/sys/device',
    method: 'post',
    data: data
  })
}

// 修改设备管理
export function updateDevice(data) {
  return request({
    url: '/sys/device',
    method: 'put',
    data: data
  })
}

// 删除设备管理
export function delDevice(id) {
  return request({
    url: '/sys/device/' + id,
    method: 'delete'
  })
}

// 清除所有设备
export function clearDevice() {
  return request({
    url: '/sys/device/purge',
    method: 'delete'
  })
}

// 同步设备
export function pushDevice(query) {
  return request({
    url: '/api/device/push',
    method: 'get',
    params: query
  })
}

// 按平台统计设备在线情况
export function getDeviceOnlineStats() {
  return request({
    url: '/sys/device/online-stats',
    method: 'get'
  })
}

// 更新设备在线状态（并推送事件给下游）
export function updateDeviceOnline(data) {
  return request({
    url: '/sys/device/online',
    method: 'put',
    data: data
  })
}
