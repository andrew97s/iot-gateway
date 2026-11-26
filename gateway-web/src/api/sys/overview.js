import request from '@/utils/request'

// 首页概览聚合数据
export function getOverview() {
  return request({
    url: '/sys/overview',
    method: 'get'
  })
}
