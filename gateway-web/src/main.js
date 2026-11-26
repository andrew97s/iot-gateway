import { createApp } from 'vue'
import Cookies from 'js-cookie'

import ElementPlus from 'element-plus'
import locale from 'element-plus/es/locale/lang/zh-cn'

import 'element-plus/theme-chalk/dark/css-vars.css'
import './assets/styles/index.scss' // global css

import 'vue-json-pretty/lib/styles.css'

import App from './App'
import store from './store'
import router from './router/index'
import directive from './directive' // directive

// 注册指令
import plugins from './plugins' // plugins
import { download } from '@/utils/request'

// svg图标
import 'virtual:svg-icons-register'
import SvgIcon from '@/components/system/SvgIcon/index.vue'
import elementIcons from '@/components/system/SvgIcon/svgicon'

import './permission' // permission control
import defaultSettings from './settings'
import './globalHandle'

import { useDict } from '@/utils/dict'
import { getBaseApi, getResApi, getWebSocketUrl, getAssets, setLabelWidth } from '@/utils/index'
import { parseTime, resetForm, addDateRange, handleTree, selectDictLabel, selectDictLabels } from '@/utils/ruoyi'

// 分页组件
import Pagination from '@/components/system/Pagination/index.vue'
// 自定义表格工具组件
import RightToolbar from '@/components/system/RightToolbar/index.vue'
// 富文本组件
import Editor from '@/components/system/Editor/index.vue'
// 文件上传组件
import FileUpload from '@/components/system/FileUpload/index.vue'
// 图片上传组件
import ImageUpload from '@/components/system/ImageUpload/index.vue'
// 图片预览组件
import ImagePreview from '@/components/system/ImagePreview/index.vue'
// 自定义树选择组件
import TreeSelect from '@/components/system/TreeSelect/index.vue'
// 字典标签组件
import DictTag from '@/components/system/DictTag/index.vue'
// 下拉框选择
import FormSelect from '@/components/system/FormSelect/index.vue'
import ImportTemplate from '@/components/system/ImportTemplate/index.vue'

const app = createApp(App)

document.title = defaultSettings.title

// 全局方法挂载
app.config.globalProperties.useDict = useDict
app.config.globalProperties.download = download
app.config.globalProperties.parseTime = parseTime
app.config.globalProperties.resetForm = resetForm
app.config.globalProperties.handleTree = handleTree
app.config.globalProperties.addDateRange = addDateRange
app.config.globalProperties.selectDictLabel = selectDictLabel
app.config.globalProperties.selectDictLabels = selectDictLabels
app.config.globalProperties.getAssets = getAssets
app.config.globalProperties.setLabelWidth = setLabelWidth
// url
app.config.globalProperties.BASE_API = getBaseApi()
app.config.globalProperties.RES_URL = getResApi()
app.config.globalProperties.WEBSOCKET_URL = getWebSocketUrl()

// 删除提示语
app.config.globalProperties.deleteTips = '是否确认删除选中数据项?'
// 弹窗默认宽度
app.config.globalProperties.defaultModalWidth = 700
// 查询表单文本默认排列方式
app.config.globalProperties.defaultQueryLabelPosition = 'right'
// 弹窗表单文本默认排列方式
app.config.globalProperties.defaultModalLabelPosition = 'right'

// 全局组件挂载
app.component('DictTag', DictTag)
app.component('Pagination', Pagination)
app.component('TreeSelect', TreeSelect)
app.component('FileUpload', FileUpload)
app.component('ImageUpload', ImageUpload)
app.component('ImagePreview', ImagePreview)
app.component('RightToolbar', RightToolbar)
app.component('Editor', Editor)
app.component('FormSelect', FormSelect)
app.component('ImportTemplate', ImportTemplate)

app.use(router)
app.use(store)
app.use(plugins)
app.use(elementIcons)
app.component('SvgIcon', SvgIcon)

directive(app)

// 使用element-plus 并且设置全局的大小
app.use(ElementPlus, {
  locale: locale,
  // 支持 large、default、small
  size: Cookies.get('size') || 'default'
})

const { ElDialog } = app._context.components
// 全局修改默认配置，点击空白处不能关闭弹窗
ElDialog['props'].closeOnClickModal.default = false
// 全局修改默认配置，按下ESC不能关闭弹窗
ElDialog['props'].closeOnPressEscape.default = false

app.mount('#app')
