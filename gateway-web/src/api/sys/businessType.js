import request from '@/utils/request'

// 通用的业务类型 CRUD 封装：告警类型 / 设备类型 / 监测类型
function crud(base) {
  return {
    list(query) {
      return request({ url: `${base}/list`, method: 'get', params: query })
    },
    select(query) {
      return request({ url: `${base}/select`, method: 'get', params: query })
    },
    get(id) {
      return request({ url: `${base}/${id}`, method: 'get' })
    },
    add(data) {
      return request({ url: base, method: 'post', data })
    },
    update(data) {
      return request({ url: base, method: 'put', data })
    },
    remove(ids) {
      return request({ url: `${base}/${ids}`, method: 'delete' })
    }
  }
}

// 标准告警类型
export const alarmTypeApi = crud('/sys/alarm-type')

// 标准设备类型
export const deviceTypeApi = crud('/sys/device-type')

// 标准监测类型
export const monitorTypeApi = crud('/sys/monitor-type')
