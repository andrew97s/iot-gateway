import request from '@/utils/request'

// 查询安全报告列表
export function listSafetyReportLog(query) {
  return request({
    url: '/monitor/safetyReportLog/list',
    method: 'get',
    params: query
  })
}

// 查询安全报告详细
export function getSafetyReportLog(id) {
  return request({
    url: '/monitor/safetyReportLog/' + id,
    method: 'get'
  })
}

// 新增安全报告
export function addSafetyReportLog(data) {
  return request({
    url: '/monitor/safetyReportLog',
    method: 'post',
    data: data
  })
}

// 修改安全报告
export function updateSafetyReportLog(data) {
  return request({
    url: '/monitor/safetyReportLog',
    method: 'put',
    data: data
  })
}

// 删除安全报告
export function delSafetyReportLog(id) {
  return request({
    url: '/monitor/safetyReportLog/' + id,
    method: 'delete'
  })
}
