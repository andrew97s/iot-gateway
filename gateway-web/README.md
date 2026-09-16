## 开发

```bash
# 安装依赖
pnpm install

# 启动服务
npm run dev
```

## 发布

```bash
# 构建测试环境
npm run build:stage

# 构建生产环境
npm run build

# 生产环境api和资源路径需在 public/config.js文件配置
```


## 目录结构说明

```
├─admin // 打包后的目录                    
│  ├─assets // 静态资源目录
│  │    ├─css
│  │    ├─fonts
│  │    ├─img
│  │    ├─svg
│  │    └─js
│  ├─config.js // 配置文件，包含接口ip端口等
│  ├─favicon.ico // 网站标签栏logo
│  └─robots.txt // 防爬虫
├─node_modules // 开发依赖包
├─public
│  ├─config.js // 前端配置文件
│  ├─cdn // 通过html引入的插件资源
│  ├─lib // 自定义纯净插件库，与业务无关
│  ├─resource // 全局资源目录，用于存放后期可动态替换的资源
│  └─favicon.icon // 网页tab栏图标  
├─src
│  ├─api // 接口集合目录
│  │  ├─system // 系统管理模块
│  │  └─tool // 构建工具模块
│  ├─assets // 
│  │  ├─401_images
│  │  ├─404_images
│  │  ├─fonts
│  │  ├─icons
│  │  ├─images // 图片集合目录
│  │  ├─logo
│  │  └─styles // 全局样式目录
│  ├─components // 全局组件目录
│  │  ├─business // 业务组件
│  │  ├─common // 通用组件，可以脱离业务使用
│  │  └─system // 系统组件
│  ├─directive // 全局指令 
│  ├─layout // 框架布局
│  ├─plugins // 全局插件工具
│  ├─router // 路由目录
│  ├─store // 状态管理
│  ├─utils // 工具方法目录
│  └─views // 页面模块集合目录
│      ├─dashboard // 图表集合目录
│      ├─error
│      ├─system // 系统管理
│      │  └─components // 模块组件目录
│      └─tool // 构建工具
├─vite
│  ├─plugins // vite插件集合
│  └─build.js // 打包配置
├─.editorconfig // 编辑器代码格式配置
├─.env.development // 开发环境相关配置
├─.env.staging // 测试环境相关配置
├─.gitignore // git忽略文件配置
├─.eslintrc.cjs // eslint相关配置
├─.prettierrc.js // 代码格式化配置
├─jsconfig.json // 文件引入提示跳转(使用@别名后无法跳转至具体文件)
├─package.json // 项目依赖配置
├─README.md
├─index.html // 站点默认首页
└─vite.config.js // 开发/打包配置
```