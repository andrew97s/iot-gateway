import request from '@/utils/request'

// 检测激活状态
export function getLicenseInfo(params) {
  return request({
    url: '/license/getInfo',
    method: 'get',
    headers: { isToken: false },
    params
  })
}
