<template>
  <div class="video-box" :class="{ 'show-head': showHead && showPlayer }" :style="{ width: boxWidth }">
    <h6 class="video-head" v-if="showHead && showPlayer">
      <p class="video-custom-title">
        {{ videoTitle }}
      </p>
      <span class="video-close" @click="closeVideo(false, 1)" />
    </h6>
    <div class="video-player-container" v-if="showPlayer">
      <div class="video-player" ref="container" v-if="isLive()">
        <div class="btn-play" v-if="!playing">
          <div class="icon-video-play" @click="togglePlay()">
            <svg
              class="icon"
              height="64"
              p-id="2360"
              t="1686103536937"
              version="1.1"
              viewBox="0 0 1024 1024"
              width="64"
              xmlns="http://www.w3.org/2000/svg"
              xmlns:xlink="http://www.w3.org/1999/xlink"
            >
              <path
                d="M512 0C230.4 0 0 230.4 0 512s230.4 512 512 512 512-230.4 512-512S793.6 0 512 0z m0 981.333333C253.866667 981.333333 42.666667 770.133333 42.666667 512S253.866667 42.666667 512 42.666667s469.333333 211.2 469.333333 469.333333-211.2 469.333333-469.333333 469.333333z"
                fill="#ffffff"
                p-id="2361"
              />
              <path
                d="M672 441.6l-170.666667-113.066667c-57.6-38.4-106.666667-12.8-106.666666 57.6v256c0 70.4 46.933333 96 106.666666 57.6l170.666667-113.066666c57.6-42.666667 57.6-106.666667 0-145.066667z"
                fill="#ffffff"
                p-id="2362"
              />
            </svg>
          </div>
          <span class="icon-title-tips"><span class="icon-title">播放</span></span>
        </div>
        <div class="btn-play" v-else>
          <div class="icon-video-pause" @click="togglePlay()">
            <svg
              height="64"
              p-id="3518"
              t="1686103592369"
              version="1.1"
              viewBox="0 0 1024 1024"
              width="64"
              xmlns="http://www.w3.org/2000/svg"
              xmlns:xlink="http://www.w3.org/1999/xlink"
            >
              <path
                d="M512 1024C228.266667 1024 0 795.733333 0 512S228.266667 0 512 0s512 228.266667 512 512-228.266667 512-512 512z m0-42.666667c260.266667 0 469.333333-209.066667 469.333333-469.333333S772.266667 42.666667 512 42.666667 42.666667 251.733333 42.666667 512s209.066667 469.333333 469.333333 469.333333z m-106.666667-682.666666c12.8 0 21.333333 8.533333 21.333334 21.333333v384c0 12.8-8.533333 21.333333-21.333334 21.333333s-21.333333-8.533333-21.333333-21.333333V320c0-12.8 8.533333-21.333333 21.333333-21.333333z m213.333334 0c12.8 0 21.333333 8.533333 21.333333 21.333333v384c0 12.8-8.533333 21.333333-21.333333 21.333333s-21.333333-8.533333-21.333334-21.333333V320c0-12.8 8.533333-21.333333 21.333334-21.333333z"
                fill="#666666"
                fill-opacity=".9"
                p-id="3519"
              />
            </svg>
          </div>
          <span class="icon-title-tips"><span class="icon-title">暂停</span></span>
        </div>
      </div>
      <div class="video-player" ref="video" v-else>
        <video autoplay controls :src="videoUrl" />
      </div>
    </div>
    <div class="icon-play-box" v-if="!showPlayer">
      <slot v-if="$slots.default" />
      <template v-else>
        <span class="icon-play" />
        <p class="icon-text">视频播放器<br /></p>
      </template>
    </div>
  </div>
</template>

<script>
let timer = null
export default {
  name: 'VideoPlayer',
  props: {
    videoTitle: {
      type: String,
      default: ''
    },
    videoUrl: {
      type: String,
      default: ''
    },
    index: {
      type: Number,
      default: 0
    },
    showHead: {
      type: Boolean,
      default: false
    },
    id: {
      type: String,
      default: ''
    },
    autoplay: {
      type: Boolean,
      default: true
    }
  },
  emits: ['closeVideo'],
  data() {
    return {
      player: null,
      showPlayer: false,
      /**
       * @type { Jessibuca }
       */
      jessibuca: null,
      playing: false,
      boxWidth: '100%'
    }
  },
  watch: {
    videoUrl: {
      handler(n, o) {
        if (n) {
          if (this.jessibuca) {
            this.jessibuca.pause()
            this.playing = false
            setTimeout(() => {
              this.togglePlay()
            }, 300)
          } else {
            this.showPlayer = true
            this.$nextTick(() => {
              this.initPlayer()
            })
          }
        }
        if (n === '' && o && this.jessibuca) {
          this.closeVideo()
        }
      },
      immediate: true
    }
  },
  beforeUnmount() {
    this.closeVideo(true)
  },
  mounted() {},
  methods: {
    initPlayer() {
      this.boxWidth = '100.5%'
      if (this.jessibuca) {
        this.jessibuca.destroy()
        this.jessibuca = null
      }
      if (this.isLive()) {
        this.jessibuca = new Jessibuca({
          timeout: 10,
          loadingTimeout: 5,
          container: this.$refs.container,
          videoBuffer: 3, // 缓存时长
          // decoder路径
          decoder: '/cdn/jessibuca/decoder.js',
          isResize: true,
          // background: "bg.jpg",
          loadingText: '加载中',
          // hasAudio:false,
          debug: false,
          showBandwidth: false, // 显示网速
          operateBtns: {
            fullscreen: true,
            screenshot: false,
            play: false,
            audio: true
          },
          isNotMute: false
        })
        this.webGlError()
        this.jessibuca.on('error', (e) => {
          if (this.playing) {
            this.closeVideo()
            this.$modal.msgError(this.videoTitle + '视频加载出错')
          }
        })
        if (this.autoplay) {
          this.togglePlay()
        }
      }
    },
    togglePlay() {
      const url = this.videoUrl

      if (this.playing) {
        this.jessibuca.pause()
      } else {
        if (this.jessibuca.hasLoaded()) {
          this.jessibuca.play(url)
        } else {
          this.jessibuca.on('load', () => {
            this.jessibuca.play(url)
            if (timer) {
              clearTimeout(timer)
            }
            timer = setTimeout(() => {
              this.boxWidth = '100%'
            }, 1000)
          })
        }
      }
      this.playing = !this.playing
    },
    webGlError() {
      const canvas = document.querySelector('.video-player canvas')
      const gl = canvas.getContext('webgl')
      canvas.addEventListener(
        'webglcontextlost',
        (e) => {
          console.log(e)
        },
        false
      )
      // setTimeout(() => {
      //   gl.getExtension('WEBGL_lose_context').loseContext()
      //   // console.log('触发lost')
      // }, 3000)
    },
    isLive() {
      const ext = ['.mp4']
      return !ext.some((v) => (this.videoUrl + '').includes(v))
    },
    /**
     *
     * @param {bool} isDestroy 是否销毁
     * @param {bool} isOperate 是否手动操作
     */
    closeVideo(isDestroy, isOperate) {
      if (this.jessibuca) {
        this.jessibuca.destroy()
        this.jessibuca = null
        this.playing = false
        this.showPlayer = false
        if (!isDestroy) {
          this.$emit('closeVideo', this.index, isOperate)
        }
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.video-box {
  position: relative;
  width: 100%;
  height: 100%;
  background: #000;
  overflow: hidden;
  z-index: 2;
  border-radius: 10px;
  &.show-head {
    padding-top: 30px;
  }
  .video-head {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    height: 30px;
    display: flex;
    align-items: center;
    background: #333;
    padding: 0 10px;
    z-index: 5;
    .video-custom-title {
      color: #fff;
      font-size: 14px;
      width: 90%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .video-close {
      position: absolute;
      right: 15px;
      top: 50%;
      transform: translateY(-50%);
      width: 20px;
      height: 20px;
      cursor: pointer;
      background: url(./assets/icon-close.png) no-repeat center/auto 100%;
    }
  }

  :deep(.video-player-container) {
    height: 100%;
    .btn-play {
      position: absolute;
      left: 20px;
      bottom: 9px;
      z-index: 999;
      font-size: 18px;
      cursor: pointer;
      color: #ccc;
      display: flex;
      align-items: center;
      .icon-title-tips {
        opacity: 0;
        visibility: hidden;
      }
      &:hover .icon-title-tips {
        visibility: visible;
        opacity: 1;
      }
      .icon-video-play,
      .icon-video-pause {
        display: inline-block;
        width: 20px;
        height: 20px;
        svg {
          width: 100%;
          height: 100%;
          path {
            fill: #fff;
          }
        }
      }
    }
    .jessibuca-container .jessibuca-loading {
      padding-bottom: 20px !important;
    }
    .video-player {
      position: relative;
      height: 100%;
      background: #000;
      video {
        position: absolute;
        width: 100%;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        border: none;
      }
    }
  }
  .icon-play-box {
    position: absolute;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%);
    width: 160px;
    height: 50%;
    max-height: 160px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    .icon-play {
      display: inline-block;
      width: 50%;
      height: 50%;
      max-width: 100px;
      max-height: 100px;
      background: url(./assets/icon-play-logo.png) no-repeat center/auto 100%;
    }
    .icon-text {
      font-size: 14px;
      color: #fff;
      font-weight: bold;
      margin-top: 10px;
    }
  }
}
</style>
