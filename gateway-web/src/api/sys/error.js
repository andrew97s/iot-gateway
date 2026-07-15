import request from '@/utils/request'

// 不分页查询错误日志列表
export function selectError(query) {
  return request({
    url: '/sys/error/select',
    method: 'get',
    params: query
  })
}

// 分页查询错误日志列表
export function listError(query) {
  return request({
    url: '/sys/error/list',
    method: 'get',
    params: query
  })
}

// 查询错误日志详细
export function getError(id) {
  return request({
    url: '/sys/error/' + id,
    method: 'get'
  })
}

// 新增错误日志
export function addError(data) {
  return request({
    url: '/sys/error',
    method: 'post',
    data: data
  })
}

// 修改错误日志
export function updateError(data) {
  return request({
    url: '/sys/error',
    method: 'put',
    data: data
  })
}

// 删除错误日志
export function delError(id) {
  return request({
    url: '/sys/error/' + id,
    method: 'delete'
  })
}

// 删除错误日志
export function clearError() {
  return request({
    url: '/sys/error/purge',
    method: 'delete'
  })
}
