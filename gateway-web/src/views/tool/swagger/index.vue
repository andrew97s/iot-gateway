<template>
  <i-frame v-model:src="url"></i-frame>
</template>

<script setup>
import iFrame from '@/components/system/iFrame/index.vue'
import { getBaseApi } from '@/utils'

const url = ref(getBaseApi() + '/swagger-ui/index.html')
</script>
