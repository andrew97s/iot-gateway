import request from '@/utils/request'

// 不分页查询接入消息列表
export function selectMessage(query) {
  return request({
    url: '/sys/message/select',
    method: 'get',
    params: query
  })
}

// 分页查询接入消息列表
export function listMessage(query) {
  return request({
    url: '/sys/message/list',
    method: 'get',
    params: query
  })
}

// 查询接入消息详细
export function getMessage(id) {
  return request({
    url: '/sys/message/' + id,
    method: 'get'
  })
}

// 新增接入消息
export function addMessage(data) {
  return request({
    url: '/sys/message',
    method: 'post',
    data: data
  })
}

// 修改接入消息
export function updateMessage(data) {
  return request({
    url: '/sys/message',
    method: 'put',
    data: data
  })
}

// 删除接入消息
export function delMessage(id) {
  return request({
    url: '/sys/message/' + id,
    method: 'delete'
  })
}

// 清空接入消息
export function clearMessage() {
  return request({
    url: '/sys/message/purge',
    method: 'delete'
  })
}

// 重试/再次推送（同一接口）
export function retryMessage(id) {
  return request({
    url: '/sys/message/retry/' + id,
    method: 'post'
  })
}

/** 与 retryMessage 相同，语义为「再次向下游推送一次」 */
export function pushMessage(id) {
  return retryMessage(id)
}

// 消息推送记录
export function listMessagePushLogs(messageId) {
  return request({
    url: '/sys/message/' + messageId + '/push-logs',
    method: 'get'
  })
}

// 按平台统计消息数量
export function messagePlatformStats() {
  return request({
    url: '/sys/message/platform-stats',
    method: 'get'
  })
}

/** 今日消息同步摘要 */
export function messageTodayStats() {
  return request({
    url: '/sys/message/today-stats',
    method: 'get'
  })
}

/** 批量重推（传 ids 或空对象表示重推最近失败） */
export function retryMessageBatch(data) {
  return request({
    url: '/sys/message/retry/batch',
    method: 'post',
    data: data || {}
  })
}
