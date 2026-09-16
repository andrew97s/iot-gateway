<template>
  <div class="marquee-box">
    <div class="marquee" :class="{ 'is-hover-pause': isHoverPause }" :style="{ 'animation-duration': duration + 'ms' }">
      <slot />
    </div>
  </div>
</template>

<script setup name="CustomMarquee">
const props = defineProps({
  duration: {
    type: Number,
    default: 10000
  },
  isHoverPause: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.marquee-box {
  width: 100%;
  overflow: hidden;
}
.marquee {
  width: 100%;
  white-space: nowrap;
  animation: marquee 0s linear infinite;
}
.marquee.is-hover-pause:hover {
  animation-play-state: paused;
}
@keyframes marquee {
  0% {
    transform: translateX(100%);
  }
  100% {
    transform: translateX(-120%);
  }
}
</style>
