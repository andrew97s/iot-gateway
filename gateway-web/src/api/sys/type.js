import request from '@/utils/request'

// 不分页查询设备类型列表
export function selectType(query) {
  return request({
    url: '/sys/type/select',
    method: 'get',
    params: query
  })
}

// 分页查询设备类型列表
export function listType(query) {
  return request({
    url: '/sys/type/list',
    method: 'get',
    params: query
  })
}

// 查询设备类型详细
export function getType(id) {
  return request({
    url: '/sys/type/' + id,
    method: 'get'
  })
}

// 新增设备类型
export function addType(data) {
  return request({
    url: '/sys/type',
    method: 'post',
    data: data
  })
}

// 修改设备类型
export function updateType(data) {
  return request({
    url: '/sys/type',
    method: 'put',
    data: data
  })
}

// 删除设备类型
export function delType(id) {
  return request({
    url: '/sys/type/' + id,
    method: 'delete'
  })
}
