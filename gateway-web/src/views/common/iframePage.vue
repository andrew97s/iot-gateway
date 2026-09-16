<template>
  <div class="app-container">
    <!-- <iframe v-if="src" :src="src" frameborder="0" width="100%" height="100%" ></iframe> -->
    <iframe
      allowfullscreen
      frameborder="0"
      height="100%"
      sandbox="allow-scripts allow-top-navigation allow-same-origin"
      :src="src"
      width="100%"
      v-if="src"
    ></iframe>
  </div>
</template>

<script setup>
const route = useRoute()
const location = window.location
const src = ref('')

let pathName = route.name
if (pathName) {
  pathName = pathName[0].toLowerCase() + pathName.slice(1)
}

if (import.meta.env.PROD) {
  src.value = `${location.protocol}//${hostConfig.videoGatewayIp || location.hostname}:${hostConfig.videoGatewayPort}/agapi/iframelogin?LX_TOKEN=32342342&rpath=${pathName}`
} else {
  src.value = `${import.meta.env.VITE_VIDEO__PROXY_API}/agapi/iframelogin?LX_TOKEN=32342342&rpath=${pathName}`
}
</script>

<style lang="scss" scoped>
iframe {
  height: calc(100vh - 130px);
}
</style>
