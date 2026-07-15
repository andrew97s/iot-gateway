let hostInfo = {}
try {
  hostInfo = hostConfig
} catch {}
export default {
  // 网页标题
  title: hostInfo.title || '',
  loginTitle: hostInfo.loginTitle || '',
  sidebarTitle: hostInfo.sidebarTitle || '',
  // 是否显示版权logo（首页、登录页底部）
  showCopyrightLogo: hostInfo.showCopyrightLogo,
  // 是否显示后台侧边栏logo
  showAsideLogo: hostInfo.showAsideLogo,
  needVerify: hostInfo.needVerify,
  // 根据路由首页设置对应首页path和name
  homePath: '/status',
  homeName: 'Status',
  /**
   * 侧边栏主题 深色主题theme-dark，浅色主题theme-light
   */
  sideTheme: 'theme-dark',
  /**
   * 是否系统布局配置
   */
  showSettings: false,

  /**
   * 是否显示顶部导航
   */
  topNav: false,

  /**
   * 是否显示 tagsView
   */
  tagsView: true,

  /**
   * 是否固定头部
   */
  fixedHeader: true,

  /**
   * 是否显示logo
   */
  sidebarLogo: true,

  /**
   * 是否显示动态标题
   */
  dynamicTitle: false,

  /**
   * @type {string | array} 'production' | ['production', 'development']
   * @description Need show err logs component.
   * The default is only used in the production env
   * If you want to also use it in dev, you can pass ['production', 'development']
   */
  errorLog: 'production'
}
