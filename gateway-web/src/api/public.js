import request from '@/utils/request'

// 获取设备统计
export function getFacilityTypeCount(params) {
  return request({
    url: '/statics/mis/facilityTypeCount',
    method: 'get',
    params
  })
}

// 获取告警统计
export function getWarningTypeCount(params) {
  return request({
    url: '/statics/mis/warningTypeCount',
    method: 'get',
    params
  })
}

// 根据设备类型获取告警统计
export function getWarningTypeCountByFacilityType(params) {
  return request({
    url: '/statics/mis/facilityTypeWarningCount',
    method: 'get',
    params
  })
}

// 获取告警统计折线图
export function getWarningLine(params) {
  return request({
    url: '/statics/mis/warningLine',
    method: 'get',
    params
  })
}

// 根据设备类型获取告警统计折线图
export function getWarningLineByFacilityType(params) {
  return request({
    url: '/statics/mis/facilityTypeWarningLine',
    method: 'get',
    params
  })
}

// 获取监测记录设备
export function listMonitorFacility(ids) {
  return request({
    url: `/monitor/record/facility/${ids}`,
    method: 'get'
  })
}

// 获取监测记录列表
export function listMonitorRecord(params) {
  return request({
    url: '/monitor/record/list',
    method: 'get',
    params
  })
}

// 库存台账
export function facilityLedger(params) {
  return request({
    url: '/manage/statics/facilityLedger',
    method: 'get',
    params
  })
}

// 设备各类状态数量
export function facilityCountComposite(params) {
  return request({
    url: '/manage/statics/facilityComposite',
    method: 'get',
    params
  })
}

// 库存台账总数
export function facilityLedgerCount(params) {
  return request({
    url: '/manage/statics/facilityLedgerCount',
    method: 'get',
    params
  })
}

// 器材出入库信息
export function facilityNeedStorageCount(params) {
  return request({
    url: '/manage/statics/facilityNeedStorageCount',
    method: 'get',
    params
  })
}

// 今日告警总数
export function todayWarningCount(params) {
  return request({
    url: '/manage/statics/deviceWarningCount',
    method: 'get',
    params
  })
}
