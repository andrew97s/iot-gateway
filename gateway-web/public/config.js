// http://192.168.1.197:93
//        [  hostname ]:[port]
var hostConfig = {
  // api接口ip，如不填，则自动获取浏览器访问url中的hostname
  apiIp: '',
  // api接口端口
  apiPort: '',
  // api后缀，示例：/api/v1/，没有则置空
  apiSuffix: '/api',
  // websocket端口
  webSocketPort: '',
  // 文件资源ip，不填默认为api接口ip
  resourceIp: '',
  // 文件资源端口，不填默认为api接口端口
  resourcePort: '',
  // 视频网关ip
  videoGatewayIp: '',
  // 视频网关端口
  videoGatewayPort: '8080',
  // 是否为https，默认false，即http
  isHttps: false,
  // app下载链接
  appDownloadUrl: '',
  // 页面标题
  title: '全息网关',
  // 登录标题
  loginTitle: '全息网关',
  // 侧边栏标题
  sidebarTitle: '全息网关',
  // 是否显示版权logo（首页、登录页底部）
  showCopyrightLogo: false,
  // 是否显示后台侧边栏logo
  showAsideLogo: false,
  needVerify: false
}
