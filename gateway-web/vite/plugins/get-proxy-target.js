/**
 * @param {*} options
 * @returns { import('vite').Plugin }
 */
export default function getProxyTarget() {
  return {
    name: 'vite-plugin-get-proxy-target',
    configureServer({ config }) {
      const info = config.server?.proxy
      if (!info) return
      console.info('\n\x1b[35m%s\x1b[37m', ` websocket地址: ${config.env.VITE_APP_WEBSOCKET} `)
      for (let key in info) {
        const value = info[key]
        console.info('\n\x1b[35m%s\x1b[37m', ` 接口代理地址【${key}】: ${value.target}`)
      }
    }
  }
}
