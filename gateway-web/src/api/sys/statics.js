import request from '@/utils/request'

/**
 * 根据数据源动态获取取数据信息
 * @param {String} dataSource 数据集类型，平台对应模块路由 /system/dataset
 * @param {*} query
 * @returns
 */
export function getListByDataSource(dataSource, query) {
  return request({
    url: '/sys/statics/ds/' + dataSource,
    method: 'get',
    params: query
  })
}

// 设备分类统计
export function getFacilityTypeCount(query) {
  return request({
    url: '/facility/statics/mapTypeCount',
    method: 'get',
    params: query
  })
}

// 设备分布统计
export function getFacilityOrgCount(query) {
  return request({
    url: '/facility/statics/mapOrgCount',
    method: 'get',
    params: query
  })
}

// 设备运行状态统计
export function getFacilityRunCount(query) {
  return request({
    url: '/facility/statics/mapRunCount',
    method: 'get',
    params: query
  })
}

// 告警统计(近七天火警、预警、故障、事件)
export function getAlarmDay7(query) {
  return request({
    url: '/alarm/statistics/day7',
    method: 'get',
    params: query
  })
}

// 预警记录排名
export function getWarningRank(query) {
  return request({
    url: '/alarm/warning/rank',
    method: 'get',
    params: query
  })
}

// 事件记录排名
export function getEventRank(query) {
  return request({
    url: '/alarm/event/rank',
    method: 'get',
    params: query
  })
}

// 告警总数统计
export function getAlarmCount(query) {
  return request({
    url: '/alarm/statistics/alarmCount',
    method: 'get',
    params: query
  })
}

// 故障小类统计
export function getFaultTypeCount(query) {
  return request({
    url: '/alarm/statistics/faultTypeCount',
    method: 'get',
    params: query
  })
}

// 预警小类统计
export function getWarningTypeCount(query) {
  return request({
    url: '/alarm/statistics/warningTypeCount',
    method: 'get',
    params: query
  })
}

// 安全运行时长
export function getRunSafeTime(query) {
  return request({
    url: '/alarm/statistics/runSafe',
    method: 'get',
    params: query
  })
}

// 防火门状态
export function getDoorStatus(query) {
  return request({
    url: '/alarm/statistics/doorStatus',
    method: 'get',
    params: query
  })
}
