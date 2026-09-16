<template>
  <inner-link
    v-for="(item, index) in tagsViewStore.iframeViews"
    :iframe-id="'iframe' + index"
    :src="iframeUrl(item.meta.link, item.query)"
    :key="item.path"
    v-show="route.path === item.path"
  ></inner-link>
</template>

<script setup>
import InnerLink from '../InnerLink/index.vue'
import useTagsViewStore from '@/store/modules/tagsView'

const route = useRoute()
const tagsViewStore = useTagsViewStore()

function iframeUrl(url, query) {
  if (Object.keys(query).length > 0) {
    let params = Object.keys(query)
      .map((key) => key + '=' + query[key])
      .join('&')
    return url + '?' + params
  }
  return url
}
</script>
