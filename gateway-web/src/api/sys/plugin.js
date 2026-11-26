import request from '@/utils/request'

/** 运行摘要 */
export function pluginSummary() {
  return request({ url: '/sys/plugin/summary', method: 'get' })
}

/** 插件类型目录 */
export function listPluginTypes() {
  return request({ url: '/sys/plugin/types', method: 'get' })
}

export function getPluginType(pluginId) {
  return request({ url: '/sys/plugin/types/' + pluginId, method: 'get' })
}

/** 上传安装插件包 zip */
export function installPluginPackage(file) {
  const form = new FormData()
  form.append('file', file)
  return request({
    url: '/sys/plugin/packages',
    method: 'post',
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 实例列表 */
export function listPluginInstances(query) {
  return request({ url: '/sys/plugin/instances', method: 'get', params: query })
}

export function getPluginInstance(instanceId) {
  return request({ url: '/sys/plugin/instances/' + instanceId, method: 'get' })
}

/** 从类型创建实例 */
export function createPluginInstance(data) {
  return request({ url: '/sys/plugin/instances', method: 'post', data })
}

export function updatePluginConfig(instanceId, data) {
  return request({ url: `/sys/plugin/instances/${instanceId}/config`, method: 'put', data })
}

export function startPluginInstance(instanceId) {
  return request({ url: `/sys/plugin/instances/${instanceId}/start`, method: 'post' })
}

export function stopPluginInstance(instanceId) {
  return request({ url: `/sys/plugin/instances/${instanceId}/stop`, method: 'post' })
}

export function restartPluginInstance(instanceId) {
  return request({ url: `/sys/plugin/instances/${instanceId}/restart`, method: 'post' })
}

export function uninstallPluginInstance(instanceId) {
  return request({ url: `/sys/plugin/instances/${instanceId}`, method: 'delete' })
}

export function testPluginConnection(instanceId) {
  return request({ url: `/sys/plugin/instances/${instanceId}/test`, method: 'post' })
}

export function getPluginInstanceStats(instanceId) {
  return request({ url: `/sys/plugin/instances/${instanceId}/stats`, method: 'get' })
}

export function getPluginInstanceLogs(instanceId, query) {
  return request({ url: `/sys/plugin/instances/${instanceId}/logs`, method: 'get', params: query })
}

export function clearPluginInstanceLogs(instanceId) {
  return request({ url: `/sys/plugin/instances/${instanceId}/logs`, method: 'delete' })
}
