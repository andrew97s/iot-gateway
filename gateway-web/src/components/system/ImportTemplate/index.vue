<template>
  <el-dialog v-model="upload.open" append-to-body :title="upload.title" width="500px">
    <el-upload
      accept=".xlsx, .xls"
      :action="upload.url + '?updateSupport=' + upload.updateSupport"
      :auto-upload="false"
      :disabled="upload.isUploading"
      drag
      :headers="upload.headers"
      :limit="1"
      :on-progress="handleFileUploadProgress"
      :on-success="handleFileSuccess"
      ref="uploadRef"
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip text-center">
          <div class="el-upload__tip">
            <el-checkbox v-model="upload.updateSupport" style="vertical-align: middle; margin: -2px 5px 0 0" />是否更新已经存在的{{ title }}数据
          </div>
          <span>仅允许导入xls、xlsx格式文件。</span>
          <el-link style="font-size: 12px; vertical-align: baseline" type="primary" :underline="false" @click="importTemplate">下载模板</el-link>
        </div>
      </template>
    </el-upload>
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>
<script setup>
import { getBaseApi } from '@/utils'
import { getToken } from '@/utils/auth'

const props = defineProps({
  /**
   * 弹窗标题
   */
  title: { type: String, default: '' },
  /**
   * 文件名称，英文
   */
  fileName: { type: String, default: '' },
  /**
   * 上传url
   */
  uploadUrl: { type: String, default: '' },
  /**
   * 模板下载url
   */
  downloadUrl: { type: String, default: '' }
})

const emits = defineEmits(['uploadSuccess'])

const { proxy } = getCurrentInstance()

const upload = reactive({
  // 是否显示弹出层（用户导入）
  open: false,
  // 弹出层标题（用户导入）
  title: '',
  // 是否禁用上传
  isUploading: false,
  // 是否更新已经存在的用户数据
  updateSupport: 0,
  // 设置上传的请求头部
  headers: { Authorization: 'Bearer ' + getToken() },
  // 上传的地址
  url: getBaseApi() + props.uploadUrl
})

/** 导入按钮操作 */
function handleImport() {
  upload.title = props.title + '导入'
  upload.open = true
}
/** 下载模板操作 */
function importTemplate() {
  proxy.download(props.downloadUrl, {}, `${props.fileName}_template_${new Date().getTime()}.xlsx`)
}
/**文件上传中处理 */
const handleFileUploadProgress = (event, file, fileList) => {
  upload.isUploading = true
}
/** 文件上传成功处理 */
const handleFileSuccess = (response, file, fileList) => {
  upload.open = false
  upload.isUploading = false
  proxy.$refs['uploadRef'].handleRemove(file)
  proxy.$modal.alert(`<div class="import-result-box">${response.msg}</div>`, '导入结果', {
    dangerouslyUseHTMLString: true
  })
  emits('uploadSuccess')
}
/** 提交上传文件 */
function submitFileForm() {
  proxy.$refs['uploadRef'].submit()
}

defineExpose({
  handleImport
})
</script>
<style lang="scss">
.import-result-box {
  max-height: 650px;
  margin-right: 5px;
  overflow-y: scroll;
  @include scrollBar(5px, rgba(0, 0, 0, 0.3), transparent, 5px);
  padding: 10px 20px 0;
}
</style>
