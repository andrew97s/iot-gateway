// vite.config.js
import { defineConfig, loadEnv } from "file:///D:/projects/history/zhian-gateway/gateway-web/node_modules/vite/dist/node/index.js";

// vite/plugins/index.js
import vue from "file:///D:/projects/history/zhian-gateway/gateway-web/node_modules/@vitejs/plugin-vue/dist/index.mjs";

// vite/plugins/auto-import.js
import autoImport from "file:///D:/projects/history/zhian-gateway/gateway-web/node_modules/unplugin-auto-import/dist/vite.js";
function createAutoImport() {
  return autoImport({
    imports: ["vue", "vue-router", "pinia"],
    // eslint报错解决
    eslintrc: {
      enabled: true,
      // Default `false`
      filepath: "./.eslintrc-auto-import.json",
      // Default `./.eslintrc-auto-import.json`
      globalsPropValue: true
      // Default `true`, (true | false | 'readonly' | 'readable' | 'writable' | 'writeable')
    },
    dts: "types/auto-imports.d.ts"
  });
}

// vite/plugins/svg-icon.js
import { createSvgIconsPlugin } from "file:///D:/projects/history/zhian-gateway/gateway-web/node_modules/vite-plugin-svg-icons/dist/index.mjs";
import path from "path";
function createSvgIcon(isBuild) {
  return createSvgIconsPlugin({
    iconDirs: [path.resolve(process.cwd(), "src/assets/icons/svg")],
    symbolId: "icon-[dir]-[name]",
    svgoOptions: isBuild
  });
}

// vite/plugins/compression.js
import compression from "file:///D:/projects/history/zhian-gateway/gateway-web/node_modules/vite-plugin-compression/dist/index.mjs";
function createCompression(env) {
  const { VITE_BUILD_COMPRESS } = env;
  const plugin = [];
  if (VITE_BUILD_COMPRESS) {
    const compressList = VITE_BUILD_COMPRESS.split(",");
    if (compressList.includes("gzip")) {
      plugin.push(
        compression({
          ext: ".gz",
          deleteOriginFile: false
        })
      );
    }
    if (compressList.includes("brotli")) {
      plugin.push(
        compression({
          ext: ".br",
          algorithm: "brotliCompress",
          deleteOriginFile: false
        })
      );
    }
  }
  return plugin;
}

// vite/plugins/setup-extend.js
import setupExtend from "file:///D:/projects/history/zhian-gateway/gateway-web/node_modules/unplugin-vue-setup-extend-plus/dist/vite.js";
function createSetupExtend() {
  return setupExtend();
}

// vite/plugins/get-proxy-target.js
function getProxyTarget() {
  return {
    name: "vite-plugin-get-proxy-target",
    configureServer({ config }) {
      const info = config.server?.proxy;
      if (!info) return;
      console.info("\n\x1B[35m%s\x1B[37m", ` websocket\u5730\u5740: ${config.env.VITE_APP_WEBSOCKET} `);
      for (let key in info) {
        const value = info[key];
        console.info("\n\x1B[35m%s\x1B[37m", ` \u63A5\u53E3\u4EE3\u7406\u5730\u5740\u3010${key}\u3011: ${value.target}`);
      }
    }
  };
}

// vite/plugins/version-plugin.js
import fs from "fs";
import path2 from "path";
import { execSync } from "child_process";
function charWidth(code) {
  if (code === 0) return 0;
  if (code < 32 || code >= 127 && code < 160) return 0;
  const ranges = [
    [4352, 4447],
    [9001, 9002],
    [11904, 42191],
    [44032, 55203],
    [63744, 64255],
    [65040, 65049],
    [65072, 65135],
    [65280, 65376],
    [65504, 65510],
    [131072, 196605],
    [196608, 262141]
  ];
  for (const [start, end] of ranges) {
    if (code >= start && code <= end) return 2;
  }
  return 1;
}
function displayLength(str) {
  let len = 0;
  for (const ch of [...String(str)]) {
    len += charWidth(ch.codePointAt(0));
  }
  return len;
}
function truncateToWidth(str, maxWidth) {
  let out = "";
  let cur = 0;
  for (const ch of [...String(str)]) {
    const w = charWidth(ch.codePointAt(0));
    if (cur + w > maxWidth - 1) break;
    out += ch;
    cur += w;
  }
  return out;
}
function padText(text, innerWidth) {
  let t = String(text);
  let tLen = displayLength(t);
  if (tLen > innerWidth) {
    t = truncateToWidth(t, innerWidth) + "\u2026";
    tLen = displayLength(t);
  }
  const totalSpaces = innerWidth - tLen;
  const leftSpaces = Math.floor(totalSpaces / 2);
  const rightSpaces = totalSpaces - leftSpaces;
  return "\u2551" + " ".repeat(leftSpaces) + t + " ".repeat(rightSpaces) + "\u2551";
}
function createVersionPlugin() {
  return {
    name: "vite-plugin-version",
    // 使用closeBundle钩子，这是构建过程的最后一步
    // 添加延迟以确保所有压缩文件都已完全写入
    closeBundle: async () => {
      try {
        const versionPath = path2.resolve(process.cwd(), "version.json");
        const webVersionPath = path2.resolve(process.cwd(), "web", "version.json");
        if (fs.existsSync(versionPath)) {
          const versionContent = fs.readFileSync(versionPath, "utf-8");
          const { version, projectVersion } = JSON.parse(versionContent);
          const webDir = path2.resolve(process.cwd(), "web");
          if (!fs.existsSync(webDir)) {
            fs.mkdirSync(webDir, { recursive: true });
          }
          fs.writeFileSync(webVersionPath, versionContent, "utf-8");
          let zipVersion;
          try {
            const gitRemote = execSync("git remote get-url origin", { encoding: "utf8", cwd: process.cwd() }).trim();
            const productRepos = ["fire-base.git", "fire-getway.git", "fire-robot.git", "fire-robot.git", "fire-pension.git"];
            if (productRepos.some((repo) => gitRemote.includes(repo))) {
              zipVersion = version;
            } else {
              zipVersion = projectVersion;
            }
          } catch (error) {
            console.warn("\u83B7\u53D6git\u4ED3\u5E93\u4FE1\u606F\u5931\u8D25\uFF0C\u4F7F\u7528\u9ED8\u8BA4\u7248\u672C:", error.message);
            zipVersion = projectVersion;
          }
          const zipFileName = `web_v${zipVersion}.zip`;
          try {
            await new Promise((resolve) => setTimeout(resolve, 2e3));
            if (process.platform === "win32") {
              const webDir2 = path2.resolve(process.cwd(), "web");
              const zipPath = path2.resolve(process.cwd(), zipFileName);
              const command = `powershell -Command "Compress-Archive -Path '${webDir2}' -DestinationPath '${zipPath}' -Force"`;
              execSync(command, {
                stdio: "pipe",
                shell: true
              });
            } else {
              execSync(`zip -r '${zipFileName}' 'web'`, { stdio: "inherit" });
            }
            console.log(`\x1B[32m\u2713 \u6784\u5EFA\u538B\u7F29\u5305: ${zipFileName}\x1B[0m`);
          } catch (err) {
            console.warn("\x1B[33m\u8B66\u544A: \u521B\u5EFA\u538B\u7F29\u6587\u4EF6\u5931\u8D25\uFF0C\u8BF7\u786E\u4FDD\u7CFB\u7EDF\u652F\u6301\u538B\u7F29\u547D\u4EE4:\x1B[0m", err.message);
          }
          const boxWidth = 40;
          const innerWidth = boxWidth - 2;
          console.log("\n\x1B[32m\u2554" + "\u2550".repeat(innerWidth) + "\u2557");
          console.log(padText("\u2713 \u6784\u5EFA\u5B8C\u6210", innerWidth));
          console.log(padText(`\u57FA\u7EBF\u7248\u672C: ${version}`, innerWidth));
          console.log(padText(`\u9879\u76EE\u7248\u672C: ${projectVersion}`, innerWidth));
          console.log("\u255A" + "\u2550".repeat(innerWidth) + "\u255D\x1B[0m\n");
        }
      } catch (error) {
        console.error("\u5904\u7406\u7248\u672C\u4FE1\u606F\u65F6\u51FA\u9519:", error);
      }
    }
  };
}

// vite/plugins/index.js
function createVitePlugins(viteEnv, isBuild = false) {
  const vitePlugins = [vue()];
  vitePlugins.push(createAutoImport());
  vitePlugins.push(createSvgIcon(isBuild));
  if (isBuild) {
    vitePlugins.push(createSetupExtend());
    vitePlugins.push(...createCompression(viteEnv));
    vitePlugins.push(createVersionPlugin());
  } else {
    vitePlugins.push(getProxyTarget());
  }
  return vitePlugins;
}

// vite/build.js
var build_default = {
  // 打包后的文件夹名称
  outDir: "web",
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
        if (id.includes("element-plus")) {
          return "chunk-element-plus";
        }
        if (id.includes("node_modules")) {
          return id.toString()?.split("node_modules/")[1]?.split("/")[0].toString();
        }
        if (id.includes("api/") || id.includes("assets/") || id.includes("utils/") || id.includes("stores/")) {
          return "chunk-static";
        }
        if (id.includes("views/components") || id.includes("src/components")) {
          return;
        }
        if (id.includes("src/views")) {
          return "views";
        }
        if (id.includes("src")) {
          return "chunk-src";
        }
      },
      // 用于从入口点创建的块的打包输出格式[name]表示文件名,[hash]表示该文件内容hash值
      entryFileNames: "assets/js/[name].[hash].js",
      // 用于命名代码拆分时创建的共享块的输出命名
      chunkFileNames: `assets/js/[name].[hash].js`,
      assetFileNames: (assetInfo) => {
        var info = assetInfo.name.split(".");
        var extType = info[info.length - 1];
        if (/\.(mp4|webm|ogg|mp3|wav|flac|aac)(\?.*)?$/i.test(assetInfo.name)) {
          extType = "media";
        } else if (/\.(png|jpe?g|gif)(\?.*)?$/.test(assetInfo.name)) {
          extType = "img";
          return `assets/${extType}/[name].[ext]`;
        } else if (/\.(svg)(\?.*)?$/.test(assetInfo.name)) {
          extType = "svg";
        } else if (/\.(woff2?|eot|ttf|otf)(\?.*)?$/i.test(assetInfo.name)) {
          extType = "fonts";
        } else if (/\.(css)(\?.*)?$/i.test(assetInfo.name)) {
          extType = "css";
        }
        return `assets/${extType}/[name].[hash].[ext]`;
      }
    }
  }
};

// vite.config.js
import path3 from "path";
var __vite_injected_original_dirname = "D:\\projects\\history\\zhian-gateway\\gateway-web";
var vite_config_default = defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd());
  const { VITE_APP_BASE_API, VITE_APP_PROXY_API } = env;
  return {
    // 部署生产环境和开发环境下的URL。
    // 默认情况下，vite 会假设你的应用是被部署在一个域名的根路径上
    // 例如 https://www.ruoyi.vip/。如果应用被部署在一个子路径上，你就需要用这个选项指定这个子路径。例如，如果你的应用被部署在 https://www.ruoyi.vip/admin/，则设置 baseUrl 为 /admin/。
    base: command === "build" ? "/" : "/",
    plugins: createVitePlugins(env, command === "build"),
    resolve: {
      // https://cn.vitejs.dev/config/#resolve-alias
      alias: {
        // 设置别名
        "@": path3.resolve(__vite_injected_original_dirname, "./src"),
        "@public": path3.resolve(__vite_injected_original_dirname, "./public")
      },
      // https://cn.vitejs.dev/config/#resolve-extensions
      extensions: [".mjs", ".js", ".ts", ".jsx", ".tsx", ".json", ".vue"]
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
          rewrite: (p) => p.replace(new RegExp("^" + VITE_APP_BASE_API), "")
        }
      }
    },
    define: {
      // 启用生产环境构建下激活不匹配的详细警告
      __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: false
    },
    // 打包配置
    build: build_default
  };
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcuanMiLCAidml0ZS9wbHVnaW5zL2luZGV4LmpzIiwgInZpdGUvcGx1Z2lucy9hdXRvLWltcG9ydC5qcyIsICJ2aXRlL3BsdWdpbnMvc3ZnLWljb24uanMiLCAidml0ZS9wbHVnaW5zL2NvbXByZXNzaW9uLmpzIiwgInZpdGUvcGx1Z2lucy9zZXR1cC1leHRlbmQuanMiLCAidml0ZS9wbHVnaW5zL2dldC1wcm94eS10YXJnZXQuanMiLCAidml0ZS9wbHVnaW5zL3ZlcnNpb24tcGx1Z2luLmpzIiwgInZpdGUvYnVpbGQuanMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJEOlxcXFxwcm9qZWN0c1xcXFxoaXN0b3J5XFxcXHpoaWFuLWdhdGV3YXlcXFxcZ2F0ZXdheS13ZWJcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkQ6XFxcXHByb2plY3RzXFxcXGhpc3RvcnlcXFxcemhpYW4tZ2F0ZXdheVxcXFxnYXRld2F5LXdlYlxcXFx2aXRlLmNvbmZpZy5qc1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vRDovcHJvamVjdHMvaGlzdG9yeS96aGlhbi1nYXRld2F5L2dhdGV3YXktd2ViL3ZpdGUuY29uZmlnLmpzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnLCBsb2FkRW52IH0gZnJvbSAndml0ZSdcbmltcG9ydCBjcmVhdGVWaXRlUGx1Z2lucyBmcm9tICcuL3ZpdGUvcGx1Z2lucydcbmltcG9ydCBidWlsZCBmcm9tICcuL3ZpdGUvYnVpbGQnXG5pbXBvcnQgcGF0aCBmcm9tICdwYXRoJ1xuXG4vLyBodHRwczovL3ZpdGVqcy5kZXYvY29uZmlnL1xuZXhwb3J0IGRlZmF1bHQgZGVmaW5lQ29uZmlnKCh7IG1vZGUsIGNvbW1hbmQgfSkgPT4ge1xuICBjb25zdCBlbnYgPSBsb2FkRW52KG1vZGUsIHByb2Nlc3MuY3dkKCkpXG4gIGNvbnN0IHsgVklURV9BUFBfQkFTRV9BUEksIFZJVEVfQVBQX1BST1hZX0FQSSB9ID0gZW52XG4gIHJldHVybiB7XG4gICAgLy8gXHU5MEU4XHU3RjcyXHU3NTFGXHU0RUE3XHU3M0FGXHU1ODgzXHU1NDhDXHU1RjAwXHU1M0QxXHU3M0FGXHU1ODgzXHU0RTBCXHU3Njg0VVJMXHUzMDAyXG4gICAgLy8gXHU5RUQ4XHU4QkE0XHU2MEM1XHU1MUI1XHU0RTBCXHVGRjBDdml0ZSBcdTRGMUFcdTUwNDdcdThCQkVcdTRGNjBcdTc2ODRcdTVFOTRcdTc1MjhcdTY2MkZcdTg4QUJcdTkwRThcdTdGNzJcdTU3MjhcdTRFMDBcdTRFMkFcdTU3REZcdTU0MERcdTc2ODRcdTY4MzlcdThERUZcdTVGODRcdTRFMEFcbiAgICAvLyBcdTRGOEJcdTU5ODIgaHR0cHM6Ly93d3cucnVveWkudmlwL1x1MzAwMlx1NTk4Mlx1Njc5Q1x1NUU5NFx1NzUyOFx1ODhBQlx1OTBFOFx1N0Y3Mlx1NTcyOFx1NEUwMFx1NEUyQVx1NUI1MFx1OERFRlx1NUY4NFx1NEUwQVx1RkYwQ1x1NEY2MFx1NUMzMVx1OTcwMFx1ODk4MVx1NzUyOFx1OEZEOVx1NEUyQVx1OTAwOVx1OTg3OVx1NjMwN1x1NUI5QVx1OEZEOVx1NEUyQVx1NUI1MFx1OERFRlx1NUY4NFx1MzAwMlx1NEY4Qlx1NTk4Mlx1RkYwQ1x1NTk4Mlx1Njc5Q1x1NEY2MFx1NzY4NFx1NUU5NFx1NzUyOFx1ODhBQlx1OTBFOFx1N0Y3Mlx1NTcyOCBodHRwczovL3d3dy5ydW95aS52aXAvYWRtaW4vXHVGRjBDXHU1MjE5XHU4QkJFXHU3RjZFIGJhc2VVcmwgXHU0RTNBIC9hZG1pbi9cdTMwMDJcbiAgICBiYXNlOiBjb21tYW5kID09PSAnYnVpbGQnID8gJy8nIDogJy8nLFxuICAgIHBsdWdpbnM6IGNyZWF0ZVZpdGVQbHVnaW5zKGVudiwgY29tbWFuZCA9PT0gJ2J1aWxkJyksXG4gICAgcmVzb2x2ZToge1xuICAgICAgLy8gaHR0cHM6Ly9jbi52aXRlanMuZGV2L2NvbmZpZy8jcmVzb2x2ZS1hbGlhc1xuICAgICAgYWxpYXM6IHtcbiAgICAgICAgLy8gXHU4QkJFXHU3RjZFXHU1MjJCXHU1NDBEXG4gICAgICAgICdAJzogcGF0aC5yZXNvbHZlKF9fZGlybmFtZSwgJy4vc3JjJyksXG4gICAgICAgICdAcHVibGljJzogcGF0aC5yZXNvbHZlKF9fZGlybmFtZSwgJy4vcHVibGljJylcbiAgICAgIH0sXG4gICAgICAvLyBodHRwczovL2NuLnZpdGVqcy5kZXYvY29uZmlnLyNyZXNvbHZlLWV4dGVuc2lvbnNcbiAgICAgIGV4dGVuc2lvbnM6IFsnLm1qcycsICcuanMnLCAnLnRzJywgJy5qc3gnLCAnLnRzeCcsICcuanNvbicsICcudnVlJ11cbiAgICB9LFxuICAgIGNzczoge1xuICAgICAgcHJlcHJvY2Vzc29yT3B0aW9uczoge1xuICAgICAgICBzY3NzOiB7XG4gICAgICAgICAgLy8gXHU1MTY4XHU1QzQwXHU1RjE1XHU1MTY1XG4gICAgICAgICAgYWRkaXRpb25hbERhdGE6ICdAaW1wb3J0IFwiQC9hc3NldHMvc3R5bGVzL21peGluLnNjc3NcIjsnXG4gICAgICAgIH1cbiAgICAgIH1cbiAgICB9LFxuICAgIC8vIFx1NUYwMFx1NTNEMVx1OTE0RFx1N0Y2RVxuICAgIHNlcnZlcjoge1xuICAgICAgcG9ydDogOTEwMyxcbiAgICAgIGhvc3Q6IHRydWUsXG4gICAgICBvcGVuOiBmYWxzZSxcbiAgICAgIHByb3h5OiB7XG4gICAgICAgIC8vIGh0dHBzOi8vY24udml0ZWpzLmRldi9jb25maWcvI3NlcnZlci1wcm94eVxuICAgICAgICBbVklURV9BUFBfQkFTRV9BUEldOiB7XG4gICAgICAgICAgdGFyZ2V0OiBWSVRFX0FQUF9QUk9YWV9BUEksXG4gICAgICAgICAgY2hhbmdlT3JpZ2luOiB0cnVlLFxuICAgICAgICAgIHJld3JpdGU6IChwKSA9PiBwLnJlcGxhY2UobmV3IFJlZ0V4cCgnXicgKyBWSVRFX0FQUF9CQVNFX0FQSSksICcnKVxuICAgICAgICB9XG4gICAgICB9XG4gICAgfSxcbiAgICBkZWZpbmU6IHtcbiAgICAgIC8vIFx1NTQyRlx1NzUyOFx1NzUxRlx1NEVBN1x1NzNBRlx1NTg4M1x1Njc4NFx1NUVGQVx1NEUwQlx1NkZDMFx1NkQzQlx1NEUwRFx1NTMzOVx1OTE0RFx1NzY4NFx1OEJFNlx1N0VDNlx1OEI2Nlx1NTQ0QVxuICAgICAgX19WVUVfUFJPRF9IWURSQVRJT05fTUlTTUFUQ0hfREVUQUlMU19fOiBmYWxzZVxuICAgIH0sXG4gICAgLy8gXHU2MjUzXHU1MzA1XHU5MTREXHU3RjZFXG4gICAgYnVpbGRcbiAgfVxufSlcbiIsICJjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZGlybmFtZSA9IFwiRDpcXFxccHJvamVjdHNcXFxcaGlzdG9yeVxcXFx6aGlhbi1nYXRld2F5XFxcXGdhdGV3YXktd2ViXFxcXHZpdGVcXFxccGx1Z2luc1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiRDpcXFxccHJvamVjdHNcXFxcaGlzdG9yeVxcXFx6aGlhbi1nYXRld2F5XFxcXGdhdGV3YXktd2ViXFxcXHZpdGVcXFxccGx1Z2luc1xcXFxpbmRleC5qc1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vRDovcHJvamVjdHMvaGlzdG9yeS96aGlhbi1nYXRld2F5L2dhdGV3YXktd2ViL3ZpdGUvcGx1Z2lucy9pbmRleC5qc1wiO2ltcG9ydCB2dWUgZnJvbSAnQHZpdGVqcy9wbHVnaW4tdnVlJ1xuXG5pbXBvcnQgY3JlYXRlQXV0b0ltcG9ydCBmcm9tICcuL2F1dG8taW1wb3J0J1xuaW1wb3J0IGNyZWF0ZVN2Z0ljb24gZnJvbSAnLi9zdmctaWNvbidcbmltcG9ydCBjcmVhdGVDb21wcmVzc2lvbiBmcm9tICcuL2NvbXByZXNzaW9uJ1xuaW1wb3J0IGNyZWF0ZVNldHVwRXh0ZW5kIGZyb20gJy4vc2V0dXAtZXh0ZW5kJ1xuaW1wb3J0IGdldFByb3h5VGFyZ2V0IGZyb20gJy4vZ2V0LXByb3h5LXRhcmdldCdcbmltcG9ydCBjcmVhdGVWZXJzaW9uUGx1Z2luIGZyb20gJy4vdmVyc2lvbi1wbHVnaW4nXG5cbmV4cG9ydCBkZWZhdWx0IGZ1bmN0aW9uIGNyZWF0ZVZpdGVQbHVnaW5zKHZpdGVFbnYsIGlzQnVpbGQgPSBmYWxzZSkge1xuICBjb25zdCB2aXRlUGx1Z2lucyA9IFt2dWUoKV1cbiAgdml0ZVBsdWdpbnMucHVzaChjcmVhdGVBdXRvSW1wb3J0KCkpXG4gIHZpdGVQbHVnaW5zLnB1c2goY3JlYXRlU3ZnSWNvbihpc0J1aWxkKSlcbiAgaWYgKGlzQnVpbGQpIHtcbiAgICB2aXRlUGx1Z2lucy5wdXNoKGNyZWF0ZVNldHVwRXh0ZW5kKCkpXG4gICAgdml0ZVBsdWdpbnMucHVzaCguLi5jcmVhdGVDb21wcmVzc2lvbih2aXRlRW52KSlcbiAgICB2aXRlUGx1Z2lucy5wdXNoKGNyZWF0ZVZlcnNpb25QbHVnaW4oKSlcbiAgfSBlbHNlIHtcbiAgICB2aXRlUGx1Z2lucy5wdXNoKGdldFByb3h5VGFyZ2V0KCkpXG4gIH1cbiAgcmV0dXJuIHZpdGVQbHVnaW5zXG59XG4iLCAiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIkQ6XFxcXHByb2plY3RzXFxcXGhpc3RvcnlcXFxcemhpYW4tZ2F0ZXdheVxcXFxnYXRld2F5LXdlYlxcXFx2aXRlXFxcXHBsdWdpbnNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkQ6XFxcXHByb2plY3RzXFxcXGhpc3RvcnlcXFxcemhpYW4tZ2F0ZXdheVxcXFxnYXRld2F5LXdlYlxcXFx2aXRlXFxcXHBsdWdpbnNcXFxcYXV0by1pbXBvcnQuanNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL0Q6L3Byb2plY3RzL2hpc3RvcnkvemhpYW4tZ2F0ZXdheS9nYXRld2F5LXdlYi92aXRlL3BsdWdpbnMvYXV0by1pbXBvcnQuanNcIjtpbXBvcnQgYXV0b0ltcG9ydCBmcm9tICd1bnBsdWdpbi1hdXRvLWltcG9ydC92aXRlJ1xuXG5leHBvcnQgZGVmYXVsdCBmdW5jdGlvbiBjcmVhdGVBdXRvSW1wb3J0KCkge1xuICByZXR1cm4gYXV0b0ltcG9ydCh7XG4gICAgaW1wb3J0czogWyd2dWUnLCAndnVlLXJvdXRlcicsICdwaW5pYSddLFxuICAgIC8vIGVzbGludFx1NjJBNVx1OTUxOVx1ODlFM1x1NTFCM1xuICAgIGVzbGludHJjOiB7XG4gICAgICBlbmFibGVkOiB0cnVlLCAvLyBEZWZhdWx0IGBmYWxzZWBcbiAgICAgIGZpbGVwYXRoOiAnLi8uZXNsaW50cmMtYXV0by1pbXBvcnQuanNvbicsIC8vIERlZmF1bHQgYC4vLmVzbGludHJjLWF1dG8taW1wb3J0Lmpzb25gXG4gICAgICBnbG9iYWxzUHJvcFZhbHVlOiB0cnVlIC8vIERlZmF1bHQgYHRydWVgLCAodHJ1ZSB8IGZhbHNlIHwgJ3JlYWRvbmx5JyB8ICdyZWFkYWJsZScgfCAnd3JpdGFibGUnIHwgJ3dyaXRlYWJsZScpXG4gICAgfSxcbiAgICBkdHM6ICd0eXBlcy9hdXRvLWltcG9ydHMuZC50cydcbiAgfSlcbn1cbiIsICJjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZGlybmFtZSA9IFwiRDpcXFxccHJvamVjdHNcXFxcaGlzdG9yeVxcXFx6aGlhbi1nYXRld2F5XFxcXGdhdGV3YXktd2ViXFxcXHZpdGVcXFxccGx1Z2luc1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiRDpcXFxccHJvamVjdHNcXFxcaGlzdG9yeVxcXFx6aGlhbi1nYXRld2F5XFxcXGdhdGV3YXktd2ViXFxcXHZpdGVcXFxccGx1Z2luc1xcXFxzdmctaWNvbi5qc1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vRDovcHJvamVjdHMvaGlzdG9yeS96aGlhbi1nYXRld2F5L2dhdGV3YXktd2ViL3ZpdGUvcGx1Z2lucy9zdmctaWNvbi5qc1wiO2ltcG9ydCB7IGNyZWF0ZVN2Z0ljb25zUGx1Z2luIH0gZnJvbSAndml0ZS1wbHVnaW4tc3ZnLWljb25zJ1xuaW1wb3J0IHBhdGggZnJvbSAncGF0aCdcblxuZXhwb3J0IGRlZmF1bHQgZnVuY3Rpb24gY3JlYXRlU3ZnSWNvbihpc0J1aWxkKSB7XG4gIHJldHVybiBjcmVhdGVTdmdJY29uc1BsdWdpbih7XG4gICAgaWNvbkRpcnM6IFtwYXRoLnJlc29sdmUocHJvY2Vzcy5jd2QoKSwgJ3NyYy9hc3NldHMvaWNvbnMvc3ZnJyldLFxuICAgIHN5bWJvbElkOiAnaWNvbi1bZGlyXS1bbmFtZV0nLFxuICAgIHN2Z29PcHRpb25zOiBpc0J1aWxkXG4gIH0pXG59XG4iLCAiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIkQ6XFxcXHByb2plY3RzXFxcXGhpc3RvcnlcXFxcemhpYW4tZ2F0ZXdheVxcXFxnYXRld2F5LXdlYlxcXFx2aXRlXFxcXHBsdWdpbnNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkQ6XFxcXHByb2plY3RzXFxcXGhpc3RvcnlcXFxcemhpYW4tZ2F0ZXdheVxcXFxnYXRld2F5LXdlYlxcXFx2aXRlXFxcXHBsdWdpbnNcXFxcY29tcHJlc3Npb24uanNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL0Q6L3Byb2plY3RzL2hpc3RvcnkvemhpYW4tZ2F0ZXdheS9nYXRld2F5LXdlYi92aXRlL3BsdWdpbnMvY29tcHJlc3Npb24uanNcIjtpbXBvcnQgY29tcHJlc3Npb24gZnJvbSAndml0ZS1wbHVnaW4tY29tcHJlc3Npb24nXG5cbmV4cG9ydCBkZWZhdWx0IGZ1bmN0aW9uIGNyZWF0ZUNvbXByZXNzaW9uKGVudikge1xuICBjb25zdCB7IFZJVEVfQlVJTERfQ09NUFJFU1MgfSA9IGVudlxuICBjb25zdCBwbHVnaW4gPSBbXVxuICBpZiAoVklURV9CVUlMRF9DT01QUkVTUykge1xuICAgIGNvbnN0IGNvbXByZXNzTGlzdCA9IFZJVEVfQlVJTERfQ09NUFJFU1Muc3BsaXQoJywnKVxuICAgIGlmIChjb21wcmVzc0xpc3QuaW5jbHVkZXMoJ2d6aXAnKSkge1xuICAgICAgLy8gaHR0cDovL2RvYy5ydW95aS52aXAvcnVveWktdnVlL290aGVyL2ZhcS5odG1sI1x1NEY3Rlx1NzUyOGd6aXBcdTg5RTNcdTUzOEJcdTdGMjlcdTk3NTlcdTYwMDFcdTY1ODdcdTRFRjZcbiAgICAgIHBsdWdpbi5wdXNoKFxuICAgICAgICBjb21wcmVzc2lvbih7XG4gICAgICAgICAgZXh0OiAnLmd6JyxcbiAgICAgICAgICBkZWxldGVPcmlnaW5GaWxlOiBmYWxzZVxuICAgICAgICB9KVxuICAgICAgKVxuICAgIH1cbiAgICBpZiAoY29tcHJlc3NMaXN0LmluY2x1ZGVzKCdicm90bGknKSkge1xuICAgICAgcGx1Z2luLnB1c2goXG4gICAgICAgIGNvbXByZXNzaW9uKHtcbiAgICAgICAgICBleHQ6ICcuYnInLFxuICAgICAgICAgIGFsZ29yaXRobTogJ2Jyb3RsaUNvbXByZXNzJyxcbiAgICAgICAgICBkZWxldGVPcmlnaW5GaWxlOiBmYWxzZVxuICAgICAgICB9KVxuICAgICAgKVxuICAgIH1cbiAgfVxuICByZXR1cm4gcGx1Z2luXG59XG4iLCAiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIkQ6XFxcXHByb2plY3RzXFxcXGhpc3RvcnlcXFxcemhpYW4tZ2F0ZXdheVxcXFxnYXRld2F5LXdlYlxcXFx2aXRlXFxcXHBsdWdpbnNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkQ6XFxcXHByb2plY3RzXFxcXGhpc3RvcnlcXFxcemhpYW4tZ2F0ZXdheVxcXFxnYXRld2F5LXdlYlxcXFx2aXRlXFxcXHBsdWdpbnNcXFxcc2V0dXAtZXh0ZW5kLmpzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9EOi9wcm9qZWN0cy9oaXN0b3J5L3poaWFuLWdhdGV3YXkvZ2F0ZXdheS13ZWIvdml0ZS9wbHVnaW5zL3NldHVwLWV4dGVuZC5qc1wiO2ltcG9ydCBzZXR1cEV4dGVuZCBmcm9tICd1bnBsdWdpbi12dWUtc2V0dXAtZXh0ZW5kLXBsdXMvdml0ZSdcblxuZXhwb3J0IGRlZmF1bHQgZnVuY3Rpb24gY3JlYXRlU2V0dXBFeHRlbmQoKSB7XG4gIHJldHVybiBzZXR1cEV4dGVuZCgpXG59XG4iLCAiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIkQ6XFxcXHByb2plY3RzXFxcXGhpc3RvcnlcXFxcemhpYW4tZ2F0ZXdheVxcXFxnYXRld2F5LXdlYlxcXFx2aXRlXFxcXHBsdWdpbnNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkQ6XFxcXHByb2plY3RzXFxcXGhpc3RvcnlcXFxcemhpYW4tZ2F0ZXdheVxcXFxnYXRld2F5LXdlYlxcXFx2aXRlXFxcXHBsdWdpbnNcXFxcZ2V0LXByb3h5LXRhcmdldC5qc1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vRDovcHJvamVjdHMvaGlzdG9yeS96aGlhbi1nYXRld2F5L2dhdGV3YXktd2ViL3ZpdGUvcGx1Z2lucy9nZXQtcHJveHktdGFyZ2V0LmpzXCI7LyoqXG4gKiBAcGFyYW0geyp9IG9wdGlvbnNcbiAqIEByZXR1cm5zIHsgaW1wb3J0KCd2aXRlJykuUGx1Z2luIH1cbiAqL1xuZXhwb3J0IGRlZmF1bHQgZnVuY3Rpb24gZ2V0UHJveHlUYXJnZXQoKSB7XG4gIHJldHVybiB7XG4gICAgbmFtZTogJ3ZpdGUtcGx1Z2luLWdldC1wcm94eS10YXJnZXQnLFxuICAgIGNvbmZpZ3VyZVNlcnZlcih7IGNvbmZpZyB9KSB7XG4gICAgICBjb25zdCBpbmZvID0gY29uZmlnLnNlcnZlcj8ucHJveHlcbiAgICAgIGlmICghaW5mbykgcmV0dXJuXG4gICAgICBjb25zb2xlLmluZm8oJ1xcblxceDFiWzM1bSVzXFx4MWJbMzdtJywgYCB3ZWJzb2NrZXRcdTU3MzBcdTU3NDA6ICR7Y29uZmlnLmVudi5WSVRFX0FQUF9XRUJTT0NLRVR9IGApXG4gICAgICBmb3IgKGxldCBrZXkgaW4gaW5mbykge1xuICAgICAgICBjb25zdCB2YWx1ZSA9IGluZm9ba2V5XVxuICAgICAgICBjb25zb2xlLmluZm8oJ1xcblxceDFiWzM1bSVzXFx4MWJbMzdtJywgYCBcdTYzQTVcdTUzRTNcdTRFRTNcdTc0MDZcdTU3MzBcdTU3NDBcdTMwMTAke2tleX1cdTMwMTE6ICR7dmFsdWUudGFyZ2V0fWApXG4gICAgICB9XG4gICAgfVxuICB9XG59XG4iLCAiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIkQ6XFxcXHByb2plY3RzXFxcXGhpc3RvcnlcXFxcemhpYW4tZ2F0ZXdheVxcXFxnYXRld2F5LXdlYlxcXFx2aXRlXFxcXHBsdWdpbnNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkQ6XFxcXHByb2plY3RzXFxcXGhpc3RvcnlcXFxcemhpYW4tZ2F0ZXdheVxcXFxnYXRld2F5LXdlYlxcXFx2aXRlXFxcXHBsdWdpbnNcXFxcdmVyc2lvbi1wbHVnaW4uanNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL0Q6L3Byb2plY3RzL2hpc3RvcnkvemhpYW4tZ2F0ZXdheS9nYXRld2F5LXdlYi92aXRlL3BsdWdpbnMvdmVyc2lvbi1wbHVnaW4uanNcIjtpbXBvcnQgZnMgZnJvbSAnZnMnXG5pbXBvcnQgcGF0aCBmcm9tICdwYXRoJ1xuaW1wb3J0IHsgZXhlY1N5bmMgfSBmcm9tICdjaGlsZF9wcm9jZXNzJ1xuXG4vLyBcdTY2RjRcdTUzRUZcdTk3NjBcdTc2ODRcdTY2M0VcdTc5M0FcdTVCQkRcdTVFQTZcdTRGMzBcdTdCOTdcdUZGMDhcdTdCODBcdTUzMTZcdTc2ODQgd2N3aWR0aCBcdTVCOUVcdTczQjBcdUZGMDlcbmZ1bmN0aW9uIGNoYXJXaWR0aChjb2RlKSB7XG4gIC8vIFx1NjNBN1x1NTIzNlx1NUI1N1x1N0IyNlxuICBpZiAoY29kZSA9PT0gMCkgcmV0dXJuIDBcbiAgaWYgKGNvZGUgPCAzMiB8fCAoY29kZSA+PSAweDdmICYmIGNvZGUgPCAweGEwKSkgcmV0dXJuIDBcblxuICAvLyBcdTVFMzhcdTg5QzFcdTc2ODRcdTUxNjhcdTVCQkQvXHU1QkJEXHU1QjU3XHU3QjI2XHU1MzNBXHU5NUY0XHVGRjA4Q0pLXHUzMDAxXHU4ODY4XHU2MEM1XHU3QjQ5XHVGRjA5XG4gIGNvbnN0IHJhbmdlcyA9IFtcbiAgICBbMHgxMTAwLCAweDExNWZdLFxuICAgIFsweDIzMjksIDB4MjMyYV0sXG4gICAgWzB4MmU4MCwgMHhhNGNmXSxcbiAgICBbMHhhYzAwLCAweGQ3YTNdLFxuICAgIFsweGY5MDAsIDB4ZmFmZl0sXG4gICAgWzB4ZmUxMCwgMHhmZTE5XSxcbiAgICBbMHhmZTMwLCAweGZlNmZdLFxuICAgIFsweGZmMDAsIDB4ZmY2MF0sXG4gICAgWzB4ZmZlMCwgMHhmZmU2XSxcbiAgICBbMHgyMDAwMCwgMHgyZmZmZF0sXG4gICAgWzB4MzAwMDAsIDB4M2ZmZmRdXG4gIF1cblxuICBmb3IgKGNvbnN0IFtzdGFydCwgZW5kXSBvZiByYW5nZXMpIHtcbiAgICBpZiAoY29kZSA+PSBzdGFydCAmJiBjb2RlIDw9IGVuZCkgcmV0dXJuIDJcbiAgfVxuXG4gIHJldHVybiAxXG59XG5cbmZ1bmN0aW9uIGRpc3BsYXlMZW5ndGgoc3RyKSB7XG4gIGxldCBsZW4gPSAwXG4gIGZvciAoY29uc3QgY2ggb2YgWy4uLlN0cmluZyhzdHIpXSkge1xuICAgIGxlbiArPSBjaGFyV2lkdGgoY2guY29kZVBvaW50QXQoMCkpXG4gIH1cbiAgcmV0dXJuIGxlblxufVxuXG5mdW5jdGlvbiB0cnVuY2F0ZVRvV2lkdGgoc3RyLCBtYXhXaWR0aCkge1xuICBsZXQgb3V0ID0gJydcbiAgbGV0IGN1ciA9IDBcbiAgZm9yIChjb25zdCBjaCBvZiBbLi4uU3RyaW5nKHN0cildKSB7XG4gICAgY29uc3QgdyA9IGNoYXJXaWR0aChjaC5jb2RlUG9pbnRBdCgwKSlcbiAgICBpZiAoY3VyICsgdyA+IG1heFdpZHRoIC0gMSkgYnJlYWsgLy8gXHU0RTNBXHU3NzAxXHU3NTY1XHU1M0Y3XHU0RkREXHU3NTU5XHU4MUYzXHU1QzExMVx1NTIxN1xuICAgIG91dCArPSBjaFxuICAgIGN1ciArPSB3XG4gIH1cbiAgcmV0dXJuIG91dFxufVxuXG5mdW5jdGlvbiBwYWRUZXh0KHRleHQsIGlubmVyV2lkdGgpIHtcbiAgbGV0IHQgPSBTdHJpbmcodGV4dClcbiAgbGV0IHRMZW4gPSBkaXNwbGF5TGVuZ3RoKHQpXG4gIGlmICh0TGVuID4gaW5uZXJXaWR0aCkge1xuICAgIHQgPSB0cnVuY2F0ZVRvV2lkdGgodCwgaW5uZXJXaWR0aCkgKyAnXHUyMDI2J1xuICAgIHRMZW4gPSBkaXNwbGF5TGVuZ3RoKHQpXG4gIH1cbiAgY29uc3QgdG90YWxTcGFjZXMgPSBpbm5lcldpZHRoIC0gdExlblxuICBjb25zdCBsZWZ0U3BhY2VzID0gTWF0aC5mbG9vcih0b3RhbFNwYWNlcyAvIDIpXG4gIGNvbnN0IHJpZ2h0U3BhY2VzID0gdG90YWxTcGFjZXMgLSBsZWZ0U3BhY2VzXG4gIHJldHVybiAnXHUyNTUxJyArICcgJy5yZXBlYXQobGVmdFNwYWNlcykgKyB0ICsgJyAnLnJlcGVhdChyaWdodFNwYWNlcykgKyAnXHUyNTUxJ1xufVxuZXhwb3J0IGRlZmF1bHQgZnVuY3Rpb24gY3JlYXRlVmVyc2lvblBsdWdpbigpIHtcbiAgcmV0dXJuIHtcbiAgICBuYW1lOiAndml0ZS1wbHVnaW4tdmVyc2lvbicsXG4gICAgLy8gXHU0RjdGXHU3NTI4Y2xvc2VCdW5kbGVcdTk0QTlcdTVCNTBcdUZGMENcdThGRDlcdTY2MkZcdTY3ODRcdTVFRkFcdThGQzdcdTdBMEJcdTc2ODRcdTY3MDBcdTU0MEVcdTRFMDBcdTZCNjVcbiAgICAvLyBcdTZERkJcdTUyQTBcdTVFRjZcdThGREZcdTRFRTVcdTc4NkVcdTRGRERcdTYyNDBcdTY3MDlcdTUzOEJcdTdGMjlcdTY1ODdcdTRFRjZcdTkwRkRcdTVERjJcdTVCOENcdTUxNjhcdTUxOTlcdTUxNjVcbiAgICBjbG9zZUJ1bmRsZTogYXN5bmMgKCkgPT4ge1xuICAgICAgdHJ5IHtcbiAgICAgICAgY29uc3QgdmVyc2lvblBhdGggPSBwYXRoLnJlc29sdmUocHJvY2Vzcy5jd2QoKSwgJ3ZlcnNpb24uanNvbicpXG4gICAgICAgIGNvbnN0IHdlYlZlcnNpb25QYXRoID0gcGF0aC5yZXNvbHZlKHByb2Nlc3MuY3dkKCksICd3ZWInLCAndmVyc2lvbi5qc29uJylcblxuICAgICAgICBpZiAoZnMuZXhpc3RzU3luYyh2ZXJzaW9uUGF0aCkpIHtcbiAgICAgICAgICBjb25zdCB2ZXJzaW9uQ29udGVudCA9IGZzLnJlYWRGaWxlU3luYyh2ZXJzaW9uUGF0aCwgJ3V0Zi04JylcbiAgICAgICAgICBjb25zdCB7IHZlcnNpb24sIHByb2plY3RWZXJzaW9uIH0gPSBKU09OLnBhcnNlKHZlcnNpb25Db250ZW50KVxuXG4gICAgICAgICAgLy8gXHU3ODZFXHU0RkREd2ViXHU3NkVFXHU1RjU1XHU1QjU4XHU1NzI4XG4gICAgICAgICAgY29uc3Qgd2ViRGlyID0gcGF0aC5yZXNvbHZlKHByb2Nlc3MuY3dkKCksICd3ZWInKVxuICAgICAgICAgIGlmICghZnMuZXhpc3RzU3luYyh3ZWJEaXIpKSB7XG4gICAgICAgICAgICBmcy5ta2RpclN5bmMod2ViRGlyLCB7IHJlY3Vyc2l2ZTogdHJ1ZSB9KVxuICAgICAgICAgIH1cblxuICAgICAgICAgIC8vIFx1NTkwRFx1NTIzNlx1NzI0OFx1NjcyQ1x1NjU4N1x1NEVGNlx1NTIzMHdlYlx1NzZFRVx1NUY1NVxuICAgICAgICAgIGZzLndyaXRlRmlsZVN5bmMod2ViVmVyc2lvblBhdGgsIHZlcnNpb25Db250ZW50LCAndXRmLTgnKVxuXG4gICAgICAgICAgLy8gXHU2ODM5XHU2MzZFZ2l0XHU0RUQzXHU1RTkzXHU1NDBEXHU1MUIzXHU1QjlBXHU0RjdGXHU3NTI4XHU1NEVBXHU0RTJBXHU3MjQ4XHU2NzJDXHU1M0Y3XG4gICAgICAgICAgbGV0IHppcFZlcnNpb25cbiAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgLy8gXHU4M0I3XHU1M0Q2XHU1RjUzXHU1MjREZ2l0XHU0RUQzXHU1RTkzXHU3Njg0XHU4RkRDXHU3QTBCVVJMXG4gICAgICAgICAgICBjb25zdCBnaXRSZW1vdGUgPSBleGVjU3luYygnZ2l0IHJlbW90ZSBnZXQtdXJsIG9yaWdpbicsIHsgZW5jb2Rpbmc6ICd1dGY4JywgY3dkOiBwcm9jZXNzLmN3ZCgpIH0pLnRyaW0oKVxuICAgICAgICAgICAgLy8gXHU2OEMwXHU2N0U1XHU2NjJGXHU1NDI2XHU1MzA1XHU1NDJCXHU2MzA3XHU1QjlBXHU0RUE3XHU1NEMxXHU0RUQzXHU1RTkzXG4gICAgICAgICAgICBjb25zdCBwcm9kdWN0UmVwb3MgPSBbJ2ZpcmUtYmFzZS5naXQnLCAnZmlyZS1nZXR3YXkuZ2l0JywgJ2ZpcmUtcm9ib3QuZ2l0JywgJ2ZpcmUtcm9ib3QuZ2l0JywgJ2ZpcmUtcGVuc2lvbi5naXQnXVxuICAgICAgICAgICAgaWYgKHByb2R1Y3RSZXBvcy5zb21lKChyZXBvKSA9PiBnaXRSZW1vdGUuaW5jbHVkZXMocmVwbykpKSB7XG4gICAgICAgICAgICAgIHppcFZlcnNpb24gPSB2ZXJzaW9uXG4gICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICB6aXBWZXJzaW9uID0gcHJvamVjdFZlcnNpb25cbiAgICAgICAgICAgIH1cbiAgICAgICAgICB9IGNhdGNoIChlcnJvcikge1xuICAgICAgICAgICAgLy8gXHU1OTgyXHU2NzlDXHU4M0I3XHU1M0Q2Z2l0XHU0RkUxXHU2MDZGXHU1OTMxXHU4RDI1XHVGRjBDXHU5RUQ4XHU4QkE0XHU0RjdGXHU3NTI4dmVyc2lvblxuICAgICAgICAgICAgY29uc29sZS53YXJuKCdcdTgzQjdcdTUzRDZnaXRcdTRFRDNcdTVFOTNcdTRGRTFcdTYwNkZcdTU5MzFcdThEMjVcdUZGMENcdTRGN0ZcdTc1MjhcdTlFRDhcdThCQTRcdTcyNDhcdTY3MkM6JywgZXJyb3IubWVzc2FnZSlcbiAgICAgICAgICAgIHppcFZlcnNpb24gPSBwcm9qZWN0VmVyc2lvblxuICAgICAgICAgIH1cblxuICAgICAgICAgIGNvbnN0IHppcEZpbGVOYW1lID0gYHdlYl92JHt6aXBWZXJzaW9ufS56aXBgXG5cbiAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgLy8gXHU2REZCXHU1MkEwXHU1RUY2XHU4RkRGXHU0RUU1XHU3ODZFXHU0RkREXHU2MjQwXHU2NzA5XHU1MzhCXHU3RjI5XHU2NTg3XHU0RUY2XHU5MEZEXHU1REYyXHU1QjhDXHU1MTY4XHU1MTk5XHU1MTY1XG4gICAgICAgICAgICBhd2FpdCBuZXcgUHJvbWlzZSgocmVzb2x2ZSkgPT4gc2V0VGltZW91dChyZXNvbHZlLCAyMDAwKSlcblxuICAgICAgICAgICAgLy8gXHU1MzhCXHU3RjI5d2ViXHU3NkVFXHU1RjU1XHVGRjBDXHU3ODZFXHU0RkREXHU4OUUzXHU1MzhCXHU1NDBFXHU2NjJGd2ViXHU3NkVFXHU1RjU1XG4gICAgICAgICAgICBpZiAocHJvY2Vzcy5wbGF0Zm9ybSA9PT0gJ3dpbjMyJykge1xuICAgICAgICAgICAgICAvLyBXaW5kb3dzXHU3Q0ZCXHU3RURGXHU0RjdGXHU3NTI4UG93ZXJTaGVsbFx1NTQ3RFx1NEVFNFx1RkYwQ1x1NEY3Rlx1NzUyOFx1NUI4Q1x1NjU3NFx1OERFRlx1NUY4NFxuICAgICAgICAgICAgICBjb25zdCB3ZWJEaXIgPSBwYXRoLnJlc29sdmUocHJvY2Vzcy5jd2QoKSwgJ3dlYicpXG4gICAgICAgICAgICAgIGNvbnN0IHppcFBhdGggPSBwYXRoLnJlc29sdmUocHJvY2Vzcy5jd2QoKSwgemlwRmlsZU5hbWUpXG4gICAgICAgICAgICAgIGNvbnN0IGNvbW1hbmQgPSBgcG93ZXJzaGVsbCAtQ29tbWFuZCBcIkNvbXByZXNzLUFyY2hpdmUgLVBhdGggJyR7d2ViRGlyfScgLURlc3RpbmF0aW9uUGF0aCAnJHt6aXBQYXRofScgLUZvcmNlXCJgXG4gICAgICAgICAgICAgIGV4ZWNTeW5jKGNvbW1hbmQsIHtcbiAgICAgICAgICAgICAgICBzdGRpbzogJ3BpcGUnLFxuICAgICAgICAgICAgICAgIHNoZWxsOiB0cnVlXG4gICAgICAgICAgICAgIH0pXG4gICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAvLyBMaW51eC9NYWNcdTdDRkJcdTdFREZcdTRGN0ZcdTc1Mjh6aXBcdTU0N0RcdTRFRTRcbiAgICAgICAgICAgICAgZXhlY1N5bmMoYHppcCAtciAnJHt6aXBGaWxlTmFtZX0nICd3ZWInYCwgeyBzdGRpbzogJ2luaGVyaXQnIH0pXG4gICAgICAgICAgICB9XG5cbiAgICAgICAgICAgIGNvbnNvbGUubG9nKGBcXHgxYlszMm1cdTI3MTMgXHU2Nzg0XHU1RUZBXHU1MzhCXHU3RjI5XHU1MzA1OiAke3ppcEZpbGVOYW1lfVxceDFiWzBtYClcbiAgICAgICAgICB9IGNhdGNoIChlcnIpIHtcbiAgICAgICAgICAgIGNvbnNvbGUud2FybignXFx4MWJbMzNtXHU4QjY2XHU1NDRBOiBcdTUyMUJcdTVFRkFcdTUzOEJcdTdGMjlcdTY1ODdcdTRFRjZcdTU5MzFcdThEMjVcdUZGMENcdThCRjdcdTc4NkVcdTRGRERcdTdDRkJcdTdFREZcdTY1MkZcdTYzMDFcdTUzOEJcdTdGMjlcdTU0N0RcdTRFRTQ6XFx4MWJbMG0nLCBlcnIubWVzc2FnZSlcbiAgICAgICAgICB9XG5cbiAgICAgICAgICAvLyBcdTRGN0ZcdTc1MjhcdTc3RTlcdTVGNjJcdTY4NDZcdTY2M0VcdTc5M0FcdTcyNDhcdTY3MkNcdTRGRTFcdTYwNkZcdUZGMDhcdTY2RjRcdTdEMjdcdTUxRDFcdUZGMENcdTY1MkZcdTYzMDFcdTY1ODdcdTY3MkNcdTYyMkFcdTY1QURcdTkwN0ZcdTUxNERcdTk1MTlcdTRGNERcdUZGMDlcbiAgICAgICAgICBjb25zdCBib3hXaWR0aCA9IDQwIC8vIFx1NjAzQlx1NUJCRFx1NUVBNlx1RkYwOFx1NTQyQlx1NEUyNFx1NEZBN1x1OEZCOVx1Njg0Nlx1RkYwOVxuICAgICAgICAgIGNvbnN0IGlubmVyV2lkdGggPSBib3hXaWR0aCAtIDIgLy8gXHU1M0VGXHU3NTI4XHU2NTg3XHU2NzJDXHU1QkJEXHU1RUE2XG5cbiAgICAgICAgICAvLyBcdTYyNTNcdTUzNzBcdTY2RjRcdTdEMjdcdTUxRDFcdTc2ODRcdTY4NDZcdUZGMDhcdTUxQ0ZcdTVDMTFcdTU3ODJcdTc2RjRcdTdBN0FcdTc2N0RcdTg4NENcdUZGMDlcbiAgICAgICAgICBjb25zb2xlLmxvZygnXFxuXFx4MWJbMzJtXHUyNTU0JyArICdcdTI1NTAnLnJlcGVhdChpbm5lcldpZHRoKSArICdcdTI1NTcnKVxuICAgICAgICAgIGNvbnNvbGUubG9nKHBhZFRleHQoJ1x1MjcxMyBcdTY3ODRcdTVFRkFcdTVCOENcdTYyMTAnLCBpbm5lcldpZHRoKSlcbiAgICAgICAgICBjb25zb2xlLmxvZyhwYWRUZXh0KGBcdTU3RkFcdTdFQkZcdTcyNDhcdTY3MkM6ICR7dmVyc2lvbn1gLCBpbm5lcldpZHRoKSlcbiAgICAgICAgICBjb25zb2xlLmxvZyhwYWRUZXh0KGBcdTk4NzlcdTc2RUVcdTcyNDhcdTY3MkM6ICR7cHJvamVjdFZlcnNpb259YCwgaW5uZXJXaWR0aCkpXG4gICAgICAgICAgY29uc29sZS5sb2coJ1x1MjU1QScgKyAnXHUyNTUwJy5yZXBlYXQoaW5uZXJXaWR0aCkgKyAnXHUyNTVEXFx4MWJbMG1cXG4nKVxuICAgICAgICB9XG4gICAgICB9IGNhdGNoIChlcnJvcikge1xuICAgICAgICBjb25zb2xlLmVycm9yKCdcdTU5MDRcdTc0MDZcdTcyNDhcdTY3MkNcdTRGRTFcdTYwNkZcdTY1RjZcdTUxRkFcdTk1MTk6JywgZXJyb3IpXG4gICAgICB9XG4gICAgfVxuICB9XG59XG4iLCAiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIkQ6XFxcXHByb2plY3RzXFxcXGhpc3RvcnlcXFxcemhpYW4tZ2F0ZXdheVxcXFxnYXRld2F5LXdlYlxcXFx2aXRlXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ZpbGVuYW1lID0gXCJEOlxcXFxwcm9qZWN0c1xcXFxoaXN0b3J5XFxcXHpoaWFuLWdhdGV3YXlcXFxcZ2F0ZXdheS13ZWJcXFxcdml0ZVxcXFxidWlsZC5qc1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vRDovcHJvamVjdHMvaGlzdG9yeS96aGlhbi1nYXRld2F5L2dhdGV3YXktd2ViL3ZpdGUvYnVpbGQuanNcIjsvKiogQHR5cGUge2ltcG9ydCgndml0ZScpLkJ1aWxkT3B0aW9uc30gKi9cbmV4cG9ydCBkZWZhdWx0IHtcbiAgLy8gXHU2MjUzXHU1MzA1XHU1NDBFXHU3Njg0XHU2NTg3XHU0RUY2XHU1OTM5XHU1NDBEXHU3OUYwXG4gIG91dERpcjogJ3dlYicsXG4gIC8vIHNvdXJjZW1hcDogdHJ1ZSxcbiAgLy8gXHU0RjdGXHU3NTI4dGVyc2VyXHU0RjFBXHU1RjcxXHU1NENEXHU2MjUzXHU1MzA1XHU5MDFGXHU1RUE2XG4gIC8vIG1pbmlmeTogJ3RlcnNlcicsXG4gIC8vIHRlcnNlck9wdGlvbnM6IHtcbiAgLy8gICBjb21wcmVzczoge1xuICAvLyAgICAgLy9cdTc1MUZcdTRFQTdcdTczQUZcdTU4ODNcdTY1RjZcdTc5RkJcdTk2NjRjb25zb2xlLmxvZygpXG4gIC8vICAgICBkcm9wX2NvbnNvbGU6IHRydWUsXG4gIC8vICAgICBkcm9wX2RlYnVnZ2VyOiB0cnVlXG4gIC8vICAgfVxuICAvLyB9LFxuICBjaHVua1NpemVXYXJuaW5nTGltaXQ6IDE5MDAsXG4gIHJvbGx1cE9wdGlvbnM6IHtcbiAgICBvdXRwdXQ6IHtcbiAgICAgIC8vIFx1NjcwMFx1NUMwRlx1NTMxNlx1NjJDNlx1NTIwNlx1NTMwNVxuICAgICAgbWFudWFsQ2h1bmtzOiAoaWQpID0+IHtcbiAgICAgICAgaWYgKGlkLmluY2x1ZGVzKCdlbGVtZW50LXBsdXMnKSkge1xuICAgICAgICAgIHJldHVybiAnY2h1bmstZWxlbWVudC1wbHVzJ1xuICAgICAgICB9XG4gICAgICAgIGlmIChpZC5pbmNsdWRlcygnbm9kZV9tb2R1bGVzJykpIHtcbiAgICAgICAgICByZXR1cm4gaWQudG9TdHJpbmcoKT8uc3BsaXQoJ25vZGVfbW9kdWxlcy8nKVsxXT8uc3BsaXQoJy8nKVswXS50b1N0cmluZygpXG4gICAgICAgIH1cbiAgICAgICAgaWYgKGlkLmluY2x1ZGVzKCdhcGkvJykgfHwgaWQuaW5jbHVkZXMoJ2Fzc2V0cy8nKSB8fCBpZC5pbmNsdWRlcygndXRpbHMvJykgfHwgaWQuaW5jbHVkZXMoJ3N0b3Jlcy8nKSkge1xuICAgICAgICAgIHJldHVybiAnY2h1bmstc3RhdGljJ1xuICAgICAgICB9XG4gICAgICAgIGlmIChpZC5pbmNsdWRlcygndmlld3MvY29tcG9uZW50cycpIHx8IGlkLmluY2x1ZGVzKCdzcmMvY29tcG9uZW50cycpKSB7XG4gICAgICAgICAgcmV0dXJuXG4gICAgICAgIH1cbiAgICAgICAgaWYgKGlkLmluY2x1ZGVzKCdzcmMvdmlld3MnKSkge1xuICAgICAgICAgIHJldHVybiAndmlld3MnXG4gICAgICAgIH1cbiAgICAgICAgaWYgKGlkLmluY2x1ZGVzKCdzcmMnKSkge1xuICAgICAgICAgIHJldHVybiAnY2h1bmstc3JjJ1xuICAgICAgICB9XG4gICAgICB9LFxuICAgICAgLy8gXHU3NTI4XHU0RThFXHU0RUNFXHU1MTY1XHU1M0UzXHU3MEI5XHU1MjFCXHU1RUZBXHU3Njg0XHU1NzU3XHU3Njg0XHU2MjUzXHU1MzA1XHU4RjkzXHU1MUZBXHU2ODNDXHU1RjBGW25hbWVdXHU4ODY4XHU3OTNBXHU2NTg3XHU0RUY2XHU1NDBELFtoYXNoXVx1ODg2OFx1NzkzQVx1OEJFNVx1NjU4N1x1NEVGNlx1NTE4NVx1NUJCOWhhc2hcdTUwM0NcbiAgICAgIGVudHJ5RmlsZU5hbWVzOiAnYXNzZXRzL2pzL1tuYW1lXS5baGFzaF0uanMnLFxuICAgICAgLy8gXHU3NTI4XHU0RThFXHU1NDdEXHU1NDBEXHU0RUUzXHU3ODAxXHU2MkM2XHU1MjA2XHU2NUY2XHU1MjFCXHU1RUZBXHU3Njg0XHU1MTcxXHU0RUFCXHU1NzU3XHU3Njg0XHU4RjkzXHU1MUZBXHU1NDdEXHU1NDBEXG4gICAgICBjaHVua0ZpbGVOYW1lczogYGFzc2V0cy9qcy9bbmFtZV0uW2hhc2hdLmpzYCxcbiAgICAgIGFzc2V0RmlsZU5hbWVzOiAoYXNzZXRJbmZvKSA9PiB7XG4gICAgICAgIHZhciBpbmZvID0gYXNzZXRJbmZvLm5hbWUuc3BsaXQoJy4nKVxuICAgICAgICB2YXIgZXh0VHlwZSA9IGluZm9baW5mby5sZW5ndGggLSAxXVxuICAgICAgICBpZiAoL1xcLihtcDR8d2VibXxvZ2d8bXAzfHdhdnxmbGFjfGFhYykoXFw/LiopPyQvaS50ZXN0KGFzc2V0SW5mby5uYW1lKSkge1xuICAgICAgICAgIGV4dFR5cGUgPSAnbWVkaWEnXG4gICAgICAgIH0gZWxzZSBpZiAoL1xcLihwbmd8anBlP2d8Z2lmKShcXD8uKik/JC8udGVzdChhc3NldEluZm8ubmFtZSkpIHtcbiAgICAgICAgICBleHRUeXBlID0gJ2ltZydcbiAgICAgICAgICAvLyBpbWdcdTRFMERcdTk3MDBcdTg5ODFoYXNoXHVGRjBDXHU2NUI5XHU0RkJGXHU2NkZGXHU2MzYyXHU2NTg3XHU0RUY2XG4gICAgICAgICAgcmV0dXJuIGBhc3NldHMvJHtleHRUeXBlfS9bbmFtZV0uW2V4dF1gXG4gICAgICAgIH0gZWxzZSBpZiAoL1xcLihzdmcpKFxcPy4qKT8kLy50ZXN0KGFzc2V0SW5mby5uYW1lKSkge1xuICAgICAgICAgIGV4dFR5cGUgPSAnc3ZnJ1xuICAgICAgICB9IGVsc2UgaWYgKC9cXC4od29mZjI/fGVvdHx0dGZ8b3RmKShcXD8uKik/JC9pLnRlc3QoYXNzZXRJbmZvLm5hbWUpKSB7XG4gICAgICAgICAgZXh0VHlwZSA9ICdmb250cydcbiAgICAgICAgfSBlbHNlIGlmICgvXFwuKGNzcykoXFw/LiopPyQvaS50ZXN0KGFzc2V0SW5mby5uYW1lKSkge1xuICAgICAgICAgIGV4dFR5cGUgPSAnY3NzJ1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiBgYXNzZXRzLyR7ZXh0VHlwZX0vW25hbWVdLltoYXNoXS5bZXh0XWBcbiAgICAgIH1cbiAgICB9XG4gIH1cbn1cbiJdLAogICJtYXBwaW5ncyI6ICI7QUFBbVUsU0FBUyxjQUFjLGVBQWU7OztBQ0FQLE9BQU8sU0FBUzs7O0FDQUosT0FBTyxnQkFBZ0I7QUFFdFgsU0FBUixtQkFBb0M7QUFDekMsU0FBTyxXQUFXO0FBQUEsSUFDaEIsU0FBUyxDQUFDLE9BQU8sY0FBYyxPQUFPO0FBQUE7QUFBQSxJQUV0QyxVQUFVO0FBQUEsTUFDUixTQUFTO0FBQUE7QUFBQSxNQUNULFVBQVU7QUFBQTtBQUFBLE1BQ1Ysa0JBQWtCO0FBQUE7QUFBQSxJQUNwQjtBQUFBLElBQ0EsS0FBSztBQUFBLEVBQ1AsQ0FBQztBQUNIOzs7QUNid1csU0FBUyw0QkFBNEI7QUFDN1ksT0FBTyxVQUFVO0FBRUYsU0FBUixjQUErQixTQUFTO0FBQzdDLFNBQU8scUJBQXFCO0FBQUEsSUFDMUIsVUFBVSxDQUFDLEtBQUssUUFBUSxRQUFRLElBQUksR0FBRyxzQkFBc0IsQ0FBQztBQUFBLElBQzlELFVBQVU7QUFBQSxJQUNWLGFBQWE7QUFBQSxFQUNmLENBQUM7QUFDSDs7O0FDVDhXLE9BQU8saUJBQWlCO0FBRXZYLFNBQVIsa0JBQW1DLEtBQUs7QUFDN0MsUUFBTSxFQUFFLG9CQUFvQixJQUFJO0FBQ2hDLFFBQU0sU0FBUyxDQUFDO0FBQ2hCLE1BQUkscUJBQXFCO0FBQ3ZCLFVBQU0sZUFBZSxvQkFBb0IsTUFBTSxHQUFHO0FBQ2xELFFBQUksYUFBYSxTQUFTLE1BQU0sR0FBRztBQUVqQyxhQUFPO0FBQUEsUUFDTCxZQUFZO0FB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LHFCQUFxQjtBQUFBLFFBQ25CLE1BQU07QUFBQTtBQUFBLFVBRUosZ0JBQWdCO0FBQUEsUUFDbEI7QUFBQSxNQUNGO0FBQUEsSUFDRjtBQUFBO0FBQUEsSUFFQSxRQUFRO0FBQUEsTUFDTixNQUFNO0FBQUEsTUFDTixNQUFNO0FBQUEsTUFDTixNQUFNO0FBQUEsTUFDTixPQUFPO0FBQUE7QUFBQSxRQUVMLENBQUMsaUJBQWlCLEdBQUc7QUFBQSxVQUNuQixRQUFRO0FBQUEsVUFDUixjQUFjO0FBQUEsVUFDZCxTQUFTLENBQUMsTUFBTSxFQUFFLFFBQVEsSUFBSSxPQUFPLE1BQU0saUJBQWlCLEdBQUcsRUFBRTtBQUFBLFFBQ25FO0FBQUEsTUFDRjtBQUFBLElBQ0Y7QUFBQSxJQUNBLFFBQVE7QUFBQTtBQUFBLE1BRU4seUNBQXlDO0FBQUEsSUFDM0M7QUFBQTtBQUFBLElBRUE7QUFBQSxFQUNGO0FBQ0YsQ0FBQzsiLAogICJuYW1lcyI6IFsicGF0aCIsICJwYXRoIiwgIndlYkRpciIsICJwYXRoIiwgInBhdGgiXQp9Cg==
