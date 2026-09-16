<template>
  <!-- prop必须和父组件表单字段一致 -->
  <el-form-item :label="label" :prop="onlyView ? '' : propValue">
    <!-- 仅查看模式 -->
    <span class="item-text" v-if="onlyView">{{ textFormat(modelValue) }}</span>
    <el-select
      clearable
      :disabled="disable"
      :filterable="isFilter"
      :model-value="modelValue"
      :placeholder="defaultPlaceholder"
      @change="selectChange"
      v-else
    >
      <el-option v-for="dict in optionList" :label="dict[dictLabel]" :value="dict[dictValue]" :key="dict[dictValue]" />
    </el-select>
  </el-form-item>
</template>

<script setup>
const props = defineProps({
  /**
   * list模式，与modeApi二选一，优先选择该模式，api模式会增加请求量
   * @example
   * {
   *   // 列表数据变量
   *   list: list
   *   // 列表查询键名
   *   queryKey: id
   *   // 列表查询值名
   *   queryValue: '1'
   * }
   */
  modeList: {
    type: Object,
    default: () => {
      return {
        list: [],
        queryKey: null,
        queryValue: null
      }
    }
  },
  /**
   * api模式，与dictList二选一,优先级比dictList高
   * @example
   * {
   *   // api方法名
   *   apiFn: selectUser
   *   // api查询参数
   *   apiQuery: { id: 1 }
   *   // api返回数据key
   *   apiResKey: 'data'
   * }
   */
  modeApi: {
    type: Object,
    default: () => {
      return {
        apiFn: null,
        apiQuery: null,
        apiResKey: 'data'
      }
    }
  },
  // 使用value字段与父组件v-model绑定
  modelValue: {
    require: true,
    type: [String, Number, Boolean, Object, Array],
    default: ''
  },
  // 下拉框标题
  label: {
    type: String,
    default: '选项'
  },
  // 表单prop属性名
  propValue: {
    type: String,
    default: ''
  },
  // 字典名称属性名
  dictLabel: {
    type: String,
    default: 'dictLabel'
  },
  // 字典值属性名
  dictValue: {
    type: String,
    default: 'dictValue'
  },
  // 选择框提示语
  placeholder: {
    type: String,
    default: ''
  },
  // 是否可筛选
  isFilter: {
    type: Boolean,
    default: false
  },
  // 是否禁用
  disable: {
    type: Boolean,
    default: false
  },
  // 是否仅查看
  onlyView: {
    type: Boolean,
    default: false
  }
})
const emits = defineEmits(['update:modelValue', 'change'])

// 选择框默认提示语
const defaultPlaceholder = ref('请选择' + props.label)
const optionList = ref([])
const originalList = ref([])

watch(
  () => props.placeholder,
  (n, o) => {
    if (n && n !== o) {
      defaultPlaceholder.value = props.placeholder
    }
  },
  { immediate: true }
)

// watch(
//   () => props.dictList,
//   (n, o) => {
//     if (n && !props.filterApi) {
//       list.value = props.dictList.map((v) => v)
//     }
//   },
//   { immediate: true }
// )

watch(
  () => props.modeList?.list,
  (n, o) => {
    if (n) {
      getList()
    }
  },
  { immediate: true }
)

watch(
  () => props.modeApi?.apiQuery,
  (n, o) => {
    if (n && n !== o) {
      getList()
    }
  },
  { immediate: true }
)

onMounted(() => {
  getList()
})

async function getList() {
  if (props.modeApi?.apiFn) {
    const { apiFn, apiQuery, resKey } = props.modeApi
    const res = await apiFn(apiQuery)
    optionList.value = res[resKey || 'data'] || []
  } else if (props.modeList?.list) {
    const { list, queryKey, queryValue } = props.modeList
    if (originalList.value.length <= 0) {
      originalList.value = list
      optionList.value = list
    }
    if (queryKey) {
      optionList.value = originalList.value.filter((v) => v[queryKey] === queryValue)
    }
  }
}

function selectChange(value) {
  const item = optionList.value.find((v) => v[props.dictValue] === value) || {}
  emits('update:modelValue', value)
  emits('change', {
    item,
    list: optionList.value
  })
}
// 字典翻译
function textFormat(value) {
  const item = optionList.value.find((v) => v[props.dictValue] === value) || {}
  return item[props.dictLabel]
}
</script>

<style></style>
