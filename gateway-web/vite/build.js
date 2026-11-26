/** @type {import('vite').BuildOptions} */
export default {
  // 打包后的文件夹名称
  outDir: 'web',
  // sourcemap: true,
  // 使用terser会影响打包速度
  // minify: 'terser',
  // terserOptions: {
  //   compress: {
  //     //生产环境时移除console.log()
  //     drop_console: true,
  //     drop_debugger: true
  //   }
  // },
  chunkSizeWarningLimit: 1900,
  rollupOptions: {
    output: {
      // 最小化拆分包
      manualChunks: (id) => {
        if (id.includes('element-plus')) {
          return 'chunk-element-plus'
        }
        if (id.includes('node_modules')) {
          return id.toString()?.split('node_modules/')[1]?.split('/')[0].toString()
        }
        if (id.includes('api/') || id.includes('assets/') || id.includes('utils/') || id.includes('stores/')) {
          return 'chunk-static'
        }
        if (id.includes('views/components') || id.includes('src/components')) {
          return
        }
        if (id.includes('src/views')) {
          return 'views'
        }
        if (id.includes('src')) {
          return 'chunk-src'
        }
      },
      // 用于从入口点创建的块的打包输出格式[name]表示文件名,[hash]表示该文件内容hash值
      entryFileNames: 'assets/js/[name].[hash].js',
      // 用于命名代码拆分时创建的共享块的输出命名
      chunkFileNames: `assets/js/[name].[hash].js`,
      assetFileNames: (assetInfo) => {
        var info = assetInfo.name.split('.')
        var extType = info[info.length - 1]
        if (/\.(mp4|webm|ogg|mp3|wav|flac|aac)(\?.*)?$/i.test(assetInfo.name)) {
          extType = 'media'
        } else if (/\.(png|jpe?g|gif)(\?.*)?$/.test(assetInfo.name)) {
          extType = 'img'
          // img不需要hash，方便替换文件
          return `assets/${extType}/[name].[ext]`
        } else if (/\.(svg)(\?.*)?$/.test(assetInfo.name)) {
          extType = 'svg'
        } else if (/\.(woff2?|eot|ttf|otf)(\?.*)?$/i.test(assetInfo.name)) {
          extType = 'fonts'
        } else if (/\.(css)(\?.*)?$/i.test(assetInfo.name)) {
          extType = 'css'
        }
        return `assets/${extType}/[name].[hash].[ext]`
      }
    }
  }
}
