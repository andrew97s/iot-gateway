export default class SocketUtil {
  // socket实例
  #ws = null
  // socket连接相关信息
  #socketData = {
    // socket连接url
    url: '',
    //  socket发送数据
    data: '',
    // 发送消息后的回调
    callback: ''
  }
  // 是否成功发送数据
  #isSendSuccess = false
  // 重连相关配置
  #reconnectInfo = {
    // 重连定时器
    timer: null,
    // 重连间隔，单位ms
    time: 30 * 1000,
    // 当前重连次数
    currentCount: 0,
    // 最大重连次数
    maxCount: Infinity
  }
  // 心跳相关配置
  #heartInfo = {
    // 心跳定时器
    timer: null,
    // 心跳间隔，单位ms
    time: 59000
  }
  //初始化webSocket长连接
  constructor(url) {
    this.#socketData.url = url
    this.#createWebSocket()
  }
  #init() {
    this.#ws.onopen = (e) => {
      this.#open(e)
    }
    this.#ws.onclose = (e) => {}
    this.#ws.onmessage = (e) => {
      this.#receive(e)
    }
    this.#ws.onerror = (e) => {
      this.#error(e)
    }
  }
  // 发送数据到服务端
  wsSend(data, callback) {
    this.#socketData.data = data
    this.#socketData.callback = callback
    // CONNECTING：值为0，表示正在连接。
    // OPEN：值为1，表示连接成功，可以通信了。
    // CLOSING：值为2，表示连接正在关闭。
    // CLOSED：值为3，表示连接已经关闭，或者打开连接失败。
    if (this.#ws && this.#ws.readyState == WebSocket.OPEN) {
      this.#isSendSuccess = true
      this.#ws.send(JSON.stringify(data))
    }
  }
  // 关闭socket连接
  wsClose(e) {
    console.log('ws close')
    this.#close()
  }
  // 创建socke的连接
  #createWebSocket() {
    if (this.#ws) {
      this.#close()
    }
    try {
      this.#ws = new WebSocket(this.#socketData.url)
      this.#init()
    } catch (e) {
      console.log('ws error', e)
      this.#reconnect(this.#socketData.url)
    }
  }
  //打开websocket
  #open(e) {
    // 如果初始化时未发送成功，在连接成功后重新发送
    if (!this.#isSendSuccess) {
      this.wsSend(this.#socketData.data, this.#socketData.callback)
    }
    //开始websocket心跳
    this.#startWsHeartbeat()
    console.log('ws success')
  }
  // 接收服务端消息
  #receive(msg) {
    // 每次接收到服务端消息后 重置websocket心跳
    this.#resetHeartbeat()
    // 服务端发送来的消息
    try {
      if (msg.data.indexOf('{') === 0 && msg.data.lastIndexOf('}') === msg.data.length - 1) {
        const data = JSON.parse(msg.data)
        if (!data.heart) {
          this.#socketData.callback && this.#socketData.callback(data)
        }
      }
    } catch (err) {
      console.log(err)
    }
  }
  #error(err) {
    console.log('ws error', err)
    this.#reconnect()
  }
  #close() {
    if (this.#ws && this.#ws.readyState == WebSocket.OPEN) {
      clearTimeout(this.#heartInfo.timer)
      this.#ws.close()
      this.#ws = null
    }
  }
  // 重连websocket
  #reconnect() {
    if (this.#reconnectInfo.currentCount >= this.#reconnectInfo.maxCount) {
      return
    }
    this.#reconnectInfo.currentCount++
    this.#reconnectInfo.timer && clearTimeout(this.#reconnectInfo.timer)
    this.#reconnectInfo.timer = setTimeout(() => {
      console.log('ws reconneting...')
      this.#createWebSocket()
      if (this.#socketData.data && this.#socketData.callback) {
        this.wsSend(this.#socketData.data, this.#socketData.callback)
      }
    }, this.#reconnectInfo.time)
  }
  // 发送心跳
  #startWsHeartbeat() {
    this.#heartInfo.timer && clearTimeout(this.#heartInfo.timer)
    this.#heartInfo.timer = setInterval(() => {
      //判断websocket当前状态
      if (this.#ws && this.#ws.readyState != WebSocket.OPEN) {
        this.#reconnect()
      } else {
        // 心跳数据，固定格式，不可修改
        this.#ws.send(`{heartBeat:1}`)
      }
    }, this.#heartInfo.time)
  }
  //重置websocket心跳
  #resetHeartbeat() {
    clearTimeout(this.#heartInfo.timer)
    this.#startWsHeartbeat()
  }
}
