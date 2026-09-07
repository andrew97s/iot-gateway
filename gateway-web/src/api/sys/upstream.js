import request from '@/utils/request'

// 分页查询上级平台
export function listUpstream(query) {
  return request({
    url: '/sys/upstream/list',
    method: 'get',
    params: query
  })
}

// 不分页查询上级平台
export function selectUpstream(query) {
  return request({
    url: '/sys/upstream/select',
    method: 'get',
    params: query
  })
}

// 上级平台运行时状态（推送通道是否在线）
export function getUpstreamStatus() {
  return request({
    url: '/sys/upstream/status',
    method: 'get'
  })
}

// 上级平台详情
export function getUpstream(id) {
  return request({
    url: '/sys/upstream/' + id,
    method: 'get'
  })
}

// 新增上级平台
export function addUpstream(data) {
  return request({
    url: '/sys/upstream',
    method: 'post',
    data
  })
}

// 修改上级平台
export function updateUpstream(data) {
  return request({
    url: '/sys/upstream',
    method: 'put',
    data
  })
}

// 删除上级平台
export function delUpstream(ids) {
  return request({
    url: '/sys/upstream/' + ids,
    method: 'delete'
  })
}

// 测试上级平台连通性
export function testUpstream(data) {
  return request({
    url: '/sys/upstream/test',
    method: 'post',
    data
  })
}
