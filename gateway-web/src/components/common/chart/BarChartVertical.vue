<template>
  <div :class="className" :style="{ height: height, width: width }" ref="chartRef" />
</template>

<script>
import * as echarts from 'echarts'
import resize from './resize'

export default {
  mixins: [resize],
  props: {
    className: {
      type: String,
      default: 'chart'
    },
    width: {
      type: String,
      default: '100%'
    },
    height: {
      type: String,
      default: '1.5rem'
    },
    options: {
      type: Object,
      default: () => {
        return {}
      }
    }
  },
  data() {
    // vue3 + echarts5.0以上版本需放在此处，否者图表tooltips无法正常显示
    chart: null
    return {}
  },
  watch: {
    options: {
      handler(n) {
        if (n) {
          this.$nextTick(() => {
            this.initChart()
          })
        }
      },
      deep: true,
      immediate: true
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initChart()
    })
  },
  beforeUnmount() {
    if (!this.chart) {
      return
    }
    this.chart.dispose()
    this.chart = null
  },
  methods: {
    initChart() {
      if (!this.chart) {
        this.chart = echarts.init(this.$el, 'macarons')
      }
      const {
        labelX,
        data1,
        data2,
        data3,
        legends,
        tipLegends,
        legendIcon,
        legendWidth,
        legendColor,
        gridLeft,
        gridRight,
        gridTop,
        axisColor,
        axisLineX,
        axisTickX,
        axisLineY,
        axisTickY,
        splitColor,
        splitLineType,
        yMinInterval,
        data1Style,
        data2Style,
        data3Style,
        formatterAfterTextArr
      } = this.options

      const barWidth = this.options.barWidth || 12
      const borderRadius = this.options.borderRadius || [6, 6, 0, 0]
      this.chart.setOption({
        tooltip: {
          trigger: 'axis',
          formatter:
            formatterAfterTextArr && formatterAfterTextArr.length > 0
              ? function (params) {
                  var result = params[0].axisValue + '<br/>'
                  params.forEach(function (item) {
                    /**
                     * marker：图例样式；
                     * seriesName：series中每一项的name；
                     * value：data数据中value字段
                     * formatterAfterTextArr: value之后需要补充的文字，数组格式
                     * seriesIndex: series数据索引，当有多条series数据时，会自动获取当前选中索引
                     */
                    result += `${item.marker}${item.seriesName}：${item.value}${formatterAfterTextArr[item.seriesIndex]}<br/>`
                  })
                  return result
                }
              : null
        },
        legend: legends && {
          data: legends,
          icon: legendIcon || 'roundRect',
          itemWidth: legendWidth || 14,
          itemGap: 50,
          textStyle: {
            fontSize: 12,
            color: legendColor || '#323232',
            padding: [3, 0, 0, 0],
            rich: {}
          }
        },
        grid: {
          left: gridLeft || '0%',
          top: gridTop || '15%',
          right: gridRight || '5%',
          bottom: '5%',
          containLabel: true
        },

        xAxis: {
          type: 'category',
          axisTick: {
            show: axisTickX
          },
          axisLine: {
            show: axisLineX
          },
          axisLabel: {
            color: axisColor || '#333',
            fontSize: 12
          },
          boundaryGap: ['10%', '20%'],
          data: labelX || ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
        },
        yAxis: {
          type: 'value',
          minInterval: yMinInterval || 1,
          axisTick: {
            show: axisTickY
          },
          axisLabel: {
            color: axisColor || '#333',
            fontSize: 12
          },
          axisLine: {
            show: axisLineY
          },
          splitLine: {
            lineStyle: {
              // dashed | solid
              type: splitLineType || 'solid',
              color: [splitColor || '#ccc']
            }
          },
          splitArea: { show: false }
        },
        series: [
          {
            data: data1,
            type: 'bar',
            name: (legends && legends[0]) || tipLegends,
            barWidth,
            lineStyle: {
              color: '#0E9CFF'
            },
            itemStyle: {
              borderRadius,
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {
                  offset: 0,
                  color: (data1Style && data1Style.startColor) || '#85CFFA'
                },
                {
                  offset: 1,
                  color: (data1Style && data1Style.endColor) || '#75A0FF'
                }
              ])
            }
          },
          data2 && {
            data: data2,
            type: 'bar',
            name: legends && legends[1],
            barWidth,
            symbolSize: 6,
            lineStyle: {
              color: '#FEBD91'
            },
            itemStyle: {
              borderRadius,
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {
                  offset: 0,
                  color: (data2Style && data2Style.startColor) || '#FED788'
                },
                {
                  offset: 1,
                  color: (data2Style && data2Style.endColor) || '#F6B272'
                }
              ])
            }
          },
          data3 && {
            data: data3,
            type: 'bar',
            name: legends && legends[2],
            barWidth,
            symbolSize: 6,
            lineStyle: {
              color: '#FEBD91'
            },
            itemStyle: {
              borderRadius,
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {
                  offset: 0,
                  color: (data3Style && data3Style.startColor) || '#FFA97B'
                },
                {
                  offset: 1,
                  color: (data3Style && data3Style.endColor) || '#FF7C7D'
                }
              ])
            }
          }
        ]
      })
    }
  }
}
</script>
