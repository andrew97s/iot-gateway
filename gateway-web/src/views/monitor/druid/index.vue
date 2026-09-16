<template>
  <div>
    <i-frame v-model:src="url"></i-frame>
  </div>
</template>

<script setup>
import iFrame from '@/components/system/iFrame'
import { getBaseApi } from '@/utils'

import { ref } from 'vue'

const url = ref(getBaseApi() + '/druid/login.html')
</script>
