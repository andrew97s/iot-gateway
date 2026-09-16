import request from '@/utils/request'

// 获取部门文档列表
export function listDocument(params) {
  return request({
    url: '/system/dd/list',
    method: 'get',
    params
  })
}

// 新增部门文档
export function addDocument(data) {
  return request({
    url: '/system/dd',
    method: 'post',
    data
  })
}

// 修改部门文档
export function updateDocument(data) {
  return request({
    url: '/system/dd',
    method: 'put',
    data
  })
}

// 查询部门文档
export function getDocument(id) {
  return request({
    url: '/system/dd/' + id,
    method: 'get'
  })
}

// 导出部门文档
export function exportDocument(data) {
  return request({
    url: '/system/dd/export',
    method: 'post',
    data
  })
}

// 删除部门文档列表
export function delDocument(ids) {
  return request({
    url: `/system/dd/${ids}`,
    method: 'delete'
  })
}

// 部门文档列表下拉
export function delectDocument(params) {
  return request({
    url: `/system/dd/select`,
    method: 'delete',
    params
  })
}

// 获取部门文档列表
export function selectDocument(params) {
  return request({
    url: '/system/dd/select',
    method: 'get',
    params
  })
}

// 检查部门文档上传统计信息
export function checkDocument(params) {
  return request({
    url: `/system/dd/check`,
    method: 'get',
    params
  })
}
