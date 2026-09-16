import request from '@/utils/request'

// 查询系统组织列表
export function listOrganization(query) {
  return request({
    url: '/system/organization/list',
    method: 'get',
    params: query
  })
}

// 查询系统组织详细
export function getOrganization(id) {
  return request({
    url: '/system/organization/' + id,
    method: 'get'
  })
}

// 新增系统组织
export function addOrganization(data) {
  return request({
    url: '/system/organization',
    method: 'post',
    data: data
  })
}

// 修改系统组织
export function updateOrganization(data) {
  return request({
    url: '/system/organization',
    method: 'put',
    data: data
  })
}

// 删除系统组织
export function delOrganization(id) {
  return request({
    url: '/system/organization/' + id,
    method: 'delete'
  })
}
