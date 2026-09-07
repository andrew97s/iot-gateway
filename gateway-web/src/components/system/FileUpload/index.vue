<template>
  <div class="upload-file">
    <el-upload
      :accept="fileType.map((v) => '.' + v).join(',')"
      :action="uploadFileUrl"
      :before-upload="handleBeforeUpload"
      class="upload-file-uploader"
      :class="{ 'has-file': fileList.length >= limit }"
      :data="extraData"
      :file-list="fileList"
      :headers="headers"
      :limit="limit"
      multiple
      :on-error="handleUploadError"
      :on-exceed="handleExceed"
      :on-success="handleUploadSuccess"
      :show-file-list="false"
      ref="fileUpload"
      v-if="!disabled"
    >
      <!-- 上传按钮 -->
      <el-button type="primary" v-if="!disabled && fileList.length < limit"> {{ uploadTips }} </el-button>
    </el-upload>
    <!-- 上传提示 -->
    <div class="el-upload__tip" v-if="showTip && !disabled && fileList.length <= 0">
      请上传
      <template v-if="fileSize">
        大小不超过 <b style="color: #f56c6c">{{ fileSize }}MB</b>
      </template>
      <template v-if="fileType">
        格式为 <b style="color: #f56c6c">{{ fileType.join('/') }}</b>
      </template>
      的文件
      <slot name="extraTip"></slot>
    </div>
    <span v-if="fileList.length <= 0 && disabled">暂未上传</span>
    <!-- 文件列表 -->
    <transition-group
      class="upload-file-list el-upload-list el-upload-list--text"
      :class="{ 'has-file': fileList.length > 0 }"
      name="el-fade-in-linear"
      tag="ul"
    >
      <li v-for="(file, index) in fileList" class="el-upload-list__item ele-upload-list__item-content" :key="file.uid">
        <el-link :href="`${file.url ? baseUrl + file.url : ''}`" target="_blank" :underline="false">
          <span class="el-icon-document"> {{ getFileName(file.name) }} </span>
        </el-link>
        <div class="ele-upload-list__item-content-action" v-if="!disabled">
          <el-button text type="danger" @click="handleDelete(index)"> 删除 </el-button>
        </div>
      </li>
    </transition-group>
  </div>
</template>

<script setup>
import { getToken } from '@/utils/auth'
import { getBaseApi } from '@/utils/index'

const props = defineProps({
  modelValue: {
    type: [String, Object, Array],
    default: () => ''
  },
  // 数量限制
  limit: {
    type: Number,
    default: 5
  },
  // 大小限制(MB)
  fileSize: {
    type: Number,
    default: 50
  },
  // 文件类型, 例如['png', 'jpg', 'jpeg']
  fileType: {
    type: Array,
    default: () => ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'pdf', 'png', 'jpg', 'jpeg']
  },
  // 是否显示提示
  isShowTip: {
    type: Boolean,
    default: true
  },
  disabled: {
    type: Boolean,
    default: false
  },
  /**
   * 上传附带的额外参数
   */
  extraData: {
    type: Object,
    default: () => ({})
  },
  /**
   * 上传地址
   * 使用相对路径的接口请求地址
   */
  uploadUrl: {
    type: String,
    default: ''
  },
  uploadTips: {
    type: String,
    default: '选取文件'
  }
})

const { proxy } = getCurrentInstance()
const emit = defineEmits(['update:modelValue', 'onSuccess', 'onDelete'])
const number = ref(0)
const uploadList = ref([])
const baseUrl = getBaseApi()
const uploadFileUrl = ref(baseUrl + (props.uploadUrl || '/common/upload')) // 上传文件服务器地址
const headers = ref({ Authorization: 'Bearer ' + getToken() })
const fileList = ref([])
const showTip = computed(() => props.isShowTip && (props.fileType || props.fileSize))

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      let temp = 1
      // 首先将值转为数组
      const list = Array.isArray(val) ? val : props.modelValue.split(',')
      // 然后将数组转为对象数组
      fileList.value = list.map((item) => {
        if (typeof item === 'string') {
          item = { name: item, url: item + '?random=' + Math.ceil(Math.random() * 100000) }
        }
        item.uid = item.uid || new Date().getTime() + temp++
        return item
      })
    } else {
      fileList.value = []
      return []
    }
  },
  { deep: true, immediate: true }
)

// 上传前校检格式和大小
function handleBeforeUpload(file) {
  // 校检文件类型
  if (props.fileType.length) {
    const fileName = file.name.split('.')
    const fileExt = fileName[fileName.length - 1]
    const isTypeOk = props.fileType.indexOf(fileExt) >= 0
    if (!isTypeOk) {
      proxy.$modal.msgError(`文件格式不正确, 请上传${props.fileType.join('/')}格式文件!`)
      return false
    }
  }
  // 校检文件大小
  if (props.fileSize) {
    const isLt = file.size / 1024 / 1024 < props.fileSize
    if (!isLt) {
      proxy.$modal.msgError(`上传文件大小不能超过 ${props.fileSize} MB!`)
      return false
    }
  }
  proxy.$modal.loading('正在上传文件，请稍候...')
  number.value++
  return true
}

// 文件个数超出
function handleExceed() {
  proxy.$modal.msgError(`上传文件数量不能超过 ${props.limit} 个!`)
}

// 上传失败
function handleUploadError() {
  proxy.$modal.msgError('上传文件失败')
}

// 上传成功回调
function handleUploadSuccess(res, file) {
  if (res.code === 200) {
    uploadList.value.push({ name: res.fileName, url: res.fileName })
    uploadedSuccessfully()
  } else {
    number.value--
    proxy.$modal.closeLoading()
    proxy.$modal.msgError(res.msg)
    proxy.$refs.fileUpload.handleRemove(file)
    uploadedSuccessfully()
  }
}

// 删除文件
function handleDelete(index) {
  fileList.value.splice(index, 1)
  emit('update:modelValue', listToString(fileList.value))
  emit('onDelete', listToString(fileList.value))
}

// 上传结束处理
function uploadedSuccessfully() {
  if (number.value > 0 && uploadList.value.length === number.value) {
    fileList.value = fileList.value.filter((f) => f.url !== undefined).concat(uploadList.value)
    uploadList.value = []
    number.value = 0
    emit('update:modelValue', listToString(fileList.value))
    emit('onSuccess', listToString(fileList.value))
    proxy.$modal.closeLoading()
  }
}

// 获取文件名称
function getFileName(name) {
  if (name?.lastIndexOf('/') > -1) {
    return name.slice(name.lastIndexOf('/') + 1)
  } else {
    return new Date().getTime()
  }
}

// 对象转成指定字符串分隔
function listToString(list, separator) {
  let strs = ''
  separator = separator || ','
  for (let i in list) {
    if (list[i].url) {
      strs += list[i].url + separator
    }
  }
  return strs != '' ? strs.substring(0, strs.length - 1) : ''
}
</script>

<style scoped lang="scss">
.upload-file-uploader {
  display: inline-block;
  margin-bottom: 3px;
  &.has-file {
    display: none;
  }
  :deep(.el-upload--text) {
    display: block;
  }
}
.upload-file-list .el-upload-list__item {
  border: 1px solid #e4e7ed;
  line-height: 2;
  margin-bottom: 10px;
  position: relative;
  padding-right: 5px;
}
.upload-file-list .ele-upload-list__item-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: inherit;
}
.upload-file-list.has-file {
  margin-top: 0;
}
.ele-upload-list__item-content-action .el-link {
  margin-right: 10px;
  width: 35px;
  flex-shrink: 0;
}
:deep(.upload-file-list .el-link__inner) {
  padding-left: 10px;
}
</style>
