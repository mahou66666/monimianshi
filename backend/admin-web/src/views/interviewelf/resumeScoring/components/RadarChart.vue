<template>
  <div :class="className" :style="{ height: height, width: width }" />
</template>

<script>
import echarts from 'echarts'
require('echarts/theme/macarons')

import resize from '@/components/Charts/mixins/resize'

export default {
  name: 'ResumeRadarChart',
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
      default: '420px'
    },
    dimensions: {
      type: Array,
      default: () => ['A', 'B', 'C', 'D', 'E', 'F']
    },
    scores: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      chart: null
    }
  },
  watch: {
    scores: {
      deep: true,
      handler() {
        this.setOptions()
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initChart()
    })
  },
  beforeDestroy() {
    if (!this.chart) return
    this.chart.dispose()
    this.chart = null
  },
  methods: {
    initChart() {
      this.chart = echarts.init(this.$el, 'macarons')
      this.setOptions()
    },
    setOptions() {
      if (!this.chart) return

      const indicator = (this.dimensions || []).map((name) => ({ name, max: 100 }))
      const values = (this.dimensions || []).map((d) => {
        const v = this.scores && this.scores[d]
        return typeof v === 'number' ? v : 0
      })

      const axisName = { color: '#303133', fontSize: 12 }
      const borderLineStyle = { color: '#e5e7eb' }
      const splitLine = { lineStyle: borderLineStyle }
      const splitAreaStyle = { color: ['#f9fafb', '#ffffff'] }
      const splitArea = { areaStyle: splitAreaStyle }
      const axisLine = { lineStyle: borderLineStyle }

      this.chart.setOption({
        tooltip: { show: true },
        radar: {
          indicator,
          radius: '65%',
          splitNumber: 5,
          axisName,
          splitLine,
          splitArea,
          axisLine
        },
        series: [
          {
            type: 'radar',
            data: [
              {
                value: values,
                name: '评分',
                areaStyle: { color: 'rgba(64, 158, 255, 0.18)' },
                lineStyle: { color: '#409EFF', width: 2 },
                itemStyle: { color: '#409EFF' }
              }
            ]
          }
        ]
      })
    }
  }
}
</script>
