import request from '@/utils/request'

// 不分页查询客户列表
export function selectClient(query) {
  return request({
    url: '/sys/client/select',
    method: 'get',
    params: query
  })
}

// 分页查询客户列表
export function listClient(query) {
  return request({
    url: '/sys/client/list',
    method: 'get',
    params: query
  })
}

// 查询客户详细
export function getClient(id) {
  return request({
    url: '/sys/client/' + id,
    method: 'get'
  })
}

// 新增客户
export function addClient(data) {
  return request({
    url: '/sys/client',
    method: 'post',
    data: data
  })
}

// 修改客户
export function updateClient(data) {
  return request({
    url: '/sys/client',
    method: 'put',
    data: data
  })
}

// 删除客户
export function delClient(id) {
  return request({
    url: '/sys/client/' + id,
    method: 'delete'
  })
}
