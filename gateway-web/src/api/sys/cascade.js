import request from '@/utils/request'

// 不分页查询级联平台列表
export function selectCascade(query) {
  return request({
    url: '/sys/cascade/select',
    method: 'get',
    params: query
  })
}

// 分页查询级联平台列表
export function listCascade(query) {
  return request({
    url: '/sys/cascade/list',
    method: 'get',
    params: query
  })
}

// 查询级联平台详细
export function getCascade(id) {
  return request({
    url: '/sys/cascade/' + id,
    method: 'get'
  })
}

// 新增级联平台
export function addCascade(data) {
  return request({
    url: '/sys/cascade',
    method: 'post',
    data: data
  })
}

// 修改级联平台
export function updateCascade(data) {
  return request({
    url: '/sys/cascade',
    method: 'put',
    data: data
  })
}

// 删除级联平台
export function delCascade(id) {
  return request({
    url: '/sys/cascade/' + id,
    method: 'delete'
  })
}
