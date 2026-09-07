import request from '@/utils/request'

// ==================== 核心参数 ====================

// 查询网关核心参数
export function getCoreParams() {
  return request({
    url: '/sys/setting/params',
    method: 'get'
  })
}

// 保存网关核心参数
export function saveCoreParams(data) {
  return request({
    url: '/sys/setting/params',
    method: 'put',
    data
  })
}

// ==================== 网络 / IP ====================

// 枚举本机网卡
export function listNetworkInterfaces() {
  return request({
    url: '/sys/network/interfaces',
    method: 'get'
  })
}

// 查询网络配置意图
export function getNetworkConfig() {
  return request({
    url: '/sys/network/config',
    method: 'get'
  })
}

// 保存网络配置意图
export function saveNetworkConfig(data) {
  return request({
    url: '/sys/network/config',
    method: 'put',
    headers: { 'Content-Type': 'application/json' },
    data
  })
}
