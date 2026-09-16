import { defineConfig, loadEnv } from 'vite'
import createVitePlugins from './vite/plugins'
import build from './vite/build'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd())
  const { VITE_APP_BASE_API, VITE_APP_PROXY_API } = env
  return {
    // 部署生产环境和开发环境下的URL。
    // 默认情况下，vite 会假设你的应用是被部署在一个域名的根路径上
    // 例如 https://www.ruoyi.vip/。如果应用被部署在一个子路径上，你就需要用这个选项指定这个子路径。例如，如果你的应用被部署在 https://www.ruoyi.vip/admin/，则设置 baseUrl 为 /admin/。
    base: command === 'build' ? '/' : '/',
    plugins: createVitePlugins(env, command === 'build'),
    resolve: {
      // https://cn.vitejs.dev/config/#resolve-alias
      alias: {
        // 设置别名
        '@': path.resolve(__dirname, './src'),
        '@public': path.resolve(__dirname, './public')
      },
      // https://cn.vitejs.dev/config/#resolve-extensions
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
    },
    css: {
      preprocessorOptions: {
        scss: {
          // 全局引入
          additionalData: '@import "@/assets/styles/mixin.scss";'
        }
      }
    },
    // 开发配置
    server: {
      port: 9103,
      host: true,
      open: false,
      proxy: {
        // https://cn.vitejs.dev/config/#server-proxy
        [VITE_APP_BASE_API]: {
          target: VITE_APP_PROXY_API,
          changeOrigin: true,
          rewrite: (p) => p.replace(new RegExp('^' + VITE_APP_BASE_API), '')
        }
      }
    },
    define: {
      // 启用生产环境构建下激活不匹配的详细警告
      __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: false
    },
    // 打包配置
    build
  }
})
