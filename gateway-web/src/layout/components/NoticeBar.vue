<template>
  <div class="notice-box" v-if="false">
    <span class="icon-bell"></span>
    <CustomMarquee class="marquee-text" is-hover-pause style="color: #f00">
      今日告警总数：
      <p class="num-text">
        <span class="num" @click="handleClick()">{{ totalCount }}</span>
        条<span v-if="latestWarning?.id">，最新告警：{{ latestWarning.warningTime }}，{{ latestWarning.deptName }}-{{ latestWarning.content }}</span>
      </p>
    </CustomMarquee>
  </div>
</template>
<script setup name="NoticeBar">
import useAppStore from '@/store/modules/app'
import { MSG_TYPE } from '@/business'
import CustomMarquee from '@/components/business/customMarquee.vue'
// import { todayWarningCount } from '@/api/public'

const router = useRouter()

const latestWarning = ref(null)
useAppStore().eventBus.on(MSG_TYPE.warning_message, (res) => {
  console.log('监听结果', res)
  switch (res.actionType) {
    case 'add':
      latestWarning.value = res.data || {}
      break
    case 'clear':
      latestWarning.value = {}
      break
  }
  getTodayTotal()
})

const totalCount = ref(0)
function getTodayTotal() {
  // todayWarningCount().then((res) => {
  //   totalCount.value = res.data
  // })
}
getTodayTotal()

function handleClick() {
  router.push({
    name: 'Warning',
    query: {
      isToday: true
    }
  })
}
</script>
<style lang="scss" scoped>
.notice-box {
  float: left;
  width: 400px;
  height: 100%;
  margin-left: 120px;
  display: flex;
  align-items: center;

  .marquee-text {
    height: 100%;
    display: flex;
    align-items: center;
    margin-left: 5px;
  }
  .num-text {
    display: inline-block;
  }
  .num-text .num {
    font-weight: bold;
    font-size: 18px;
    cursor: pointer;
    padding: 0 4px;
    border-bottom: 1px solid #f00;
  }
  .icon-bell {
    display: inline-block;
    width: 24px;
    height: 24px;
    background: url(@/assets/images/icon-warning-bell.png) no-repeat center/cover;
    transform-origin: center top;
    animation: swing 1.5s infinite;
  }
  @keyframes swing {
    10% {
      transform: rotate3d(0, 0, 1, 15deg);
    }
    20% {
      transform: rotate3d(0, 0, 1, -10deg);
    }
    30% {
      transform: rotate3d(0, 0, 1, 5deg);
    }
    40% {
      transform: rotate3d(0, 0, 1, -5deg);
    }
    50% {
      transform: rotate3d(0, 0, 1, 0deg);
    }
  }
}
</style>
