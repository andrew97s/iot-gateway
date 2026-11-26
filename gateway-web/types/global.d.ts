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
import SvgIcon from '@/components/system/SvgIcon/index.vue'
import FormSelect from '@/components/system/FormSelect/index.vue'
import ImportTemplate from '@/components/system/ImportTemplate/index.vue'

// 自定义全局插件
import tab from '@/plugins/tab'
import auth from '@/plugins/auth'
import cache from '@/plugins/cache'
import modal from '@/plugins/modal'
import downloadTool from '@/plugins/download'

import { useDict } from '@/utils/dict'
import { download } from '@/utils/request'
import { getAssets, setLabelWidth } from '@/utils/index'
import { parseTime, resetForm, addDateRange, handleTree, selectDictLabel, selectDictLabels } from '@/utils/ruoyi'

declare module 'vue' {
  interface GlobalComponents {
    Pagination: typeof Pagination
    RightToolbar: typeof RightToolbar
    FileUpload: typeof FileUpload
    ImageUpload: typeof ImageUpload
    ImagePreview: typeof ImagePreview
    TreeSelect: typeof TreeSelect
    DictTag: typeof DictTag
    Editor: typeof Editor
    FormSelect: typeof FormSelect
    ImportTemplate: typeof ImportTemplate
  }

  interface ComponentCustomProperties {
    useDict: typeof useDict
    download: typeof download
    parseTime: typeof parseTime
    resetForm: typeof resetForm
    handleTree: typeof handleTree
    addDateRange: typeof addDateRange
    selectDictLabel: typeof selectDictLabel
    selectDictLabels: typeof selectDictLabels
    getAssets: typeof getAssets
    setLabelWidth: typeof setLabelWidth
    BASE_API: typeof string
    RES_URL: typeof string
    WEBSOCKET_URL: typeof string
    deleteTips: typeof string
    defaultModalWidth: typeof string
    defaultQueryLabelPosition: typeof string
    defaultModalLabelPosition: typeof string
    // 页签操作
    $tab: typeof tab
    // 认证对象
    $auth: typeof auth
    // 缓存对象
    $cache: typeof cache
    // 模态框对象
    $modal: typeof modal
    // 下载文件
    $download: typeof downloadTool
  }
}
