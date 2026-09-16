import request from '@/utils/request'

// 不分页查询告警事件列表
export function selectEvent(query) {
  return request({
    url: '/sys/event/select',
    method: 'get',
    params: query
  })
}

// 分页查询告警事件列表
export function listEvent(query) {
  return request({
    url: '/sys/event/list',
    method: 'get',
    params: query
  })
}

// 查询告警事件详细
export function getEvent(id) {
  return request({
    url: '/sys/event/' + id,
    method: 'get'
  })
}

// 新增告警事件
export function addEvent(data) {
  return request({
    url: '/sys/event',
    method: 'post',
    data: data
  })
}

// 修改告警事件
export function updateEvent(data) {
  return request({
    url: '/sys/event',
    method: 'put',
    data: data
  })
}

// 删除告警事件
export function delEvent(id) {
  return request({
    url: '/sys/event/' + id,
    method: 'delete'
  })
}
