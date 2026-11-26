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
    /**
     * @type {echarts.ECharts}
     */
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
      deep: true
      // immediate: true
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
        this.chart = echarts.init(this.$refs.chartRef, 'macarons')
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
        legendLeft,
        gridLeft,
        gridRight,
        axisColor,
        splitColor,
        lineColor1,
        lineColor2,
        lineColor3,
        lineSmooth,
        formatterAfterTextArr
        // labels
      } = this.options
      this.chart.setOption({
        tooltip: {
          trigger: 'axis',
          formatter:
            formatterAfterTextArr && formatterAfterTextArr.length > 0
              ? function (params) {
                  // x轴文字
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
          left: legendLeft || 'center',
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
          top: legends ? '20%' : '10%',
          right: gridRight || '5%',
          bottom: '5%',
          containLabel: true
        },

        xAxis: {
          type: 'category',
          boundaryGap: false,
          axisTick: {
            show: false
          },
          axisLine: {
            show: true
          },
          axisLabel: {
            color: axisColor || '#333',
            fontSize: 12
          },
          data: labelX || ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          axisTick: {
            show: true
          },
          axisLabel: {
            color: axisColor || '#333',
            fontSize: 12
          },
          axisLine: {
            show: true
          },
          splitLine: {
            lineStyle: {
              type: 'dashed',
              color: [splitColor || '#ccc']
            }
          },
          splitArea: { show: false }
        },
        series: [
          {
            data: data1,
            type: 'line',
            smooth: lineSmooth || false,
            name: (legends && legends[0]) || tipLegends,
            areaStyle: {
              color: 'rgba(' + (lineColor1 || '14, 156, 255') + ', 0.2)'
            },
            symbolSize: 6,
            lineStyle: {
              color: 'rgba(' + (lineColor1 || '14, 156, 255') + ', 1)'
            },
            itemStyle: {
              color: 'rgba(' + (lineColor1 || '14, 156, 255') + ', 1)'
            }
          },
          data2 && {
            data: data2,
            type: 'line',
            smooth: lineSmooth || false,
            name: legends && legends[1],
            areaStyle: {
              color: 'rgba(' + (lineColor2 || '255, 234, 96') + ', 0.2)'
            },
            symbolSize: 6,
            lineStyle: {
              color: 'rgba(' + (lineColor2 || '255, 234, 96') + ', 1)'
            },
            itemStyle: {
              color: 'rgba(' + (lineColor2 || '255, 234, 96') + ', 1)'
            }
          },
          data3 && {
            data: data3,
            type: 'line',
            smooth: lineSmooth || false,
            name: legends && legends[2],
            areaStyle: {
              color: 'rgba(' + (lineColor3 || '102, 225, 223') + ', 0.2)'
            },
            symbolSize: 6,
            lineStyle: {
              color: 'rgba(' + (lineColor3 || '102, 225, 223') + ', 1)'
            },
            itemStyle: {
              color: 'rgba(' + (lineColor3 || '102, 225, 223') + ', 1)'
            }
          }
        ]
      })
    }
  }
}
</script>
