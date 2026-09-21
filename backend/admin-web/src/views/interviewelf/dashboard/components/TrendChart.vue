<template>
  <div :class="className" :style="{ height, width }" />
</template>

<script>
import echarts from 'echarts'
require('echarts/theme/macarons')
import resize from '@/views/dashboard/admin/components/mixins/resize'

export default {
  name: 'TrendChart',
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
      default: '340px'
    },
    chartData: {
      type: Object,
      default: () => ({ labels: [], users: [], resumes: [], jds: [] })
    }
  },
  data() {
    return {
      chart: null
    }
  },
  watch: {
    chartData: {
      deep: true,
      handler(val) {
        this.setOptions(val)
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.chart = echarts.init(this.$el, 'macarons')
      this.setOptions(this.chartData)
    })
  },
  beforeDestroy() {
    if (!this.chart) return
    this.chart.dispose()
    this.chart = null
  },
  methods: {
    setOptions(data = {}) {
      if (!this.chart) return

      const labels = Array.isArray(data.labels) ? data.labels : []
      const users = Array.isArray(data.users) ? data.users : []
      const resumes = Array.isArray(data.resumes) ? data.resumes : []
      const jds = Array.isArray(data.jds) ? data.jds : []

      this.chart.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'cross' }
        },
        legend: {
          top: 0,
          data: ['新增用户', '新增简历', '新增JD']
        },
        grid: {
          left: 18,
          right: 18,
          bottom: 18,
          top: 48,
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: labels,
          axisTick: { show: false }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          axisTick: { show: false }
        },
        series: [
          {
            name: '新增用户',
            type: 'line',
            smooth: true,
            data: users,
            itemStyle: { color: '#0f766e' },
            lineStyle: { color: '#0f766e', width: 2 },
            areaStyle: { color: 'rgba(15, 118, 110, 0.08)' }
          },
          {
            name: '新增简历',
            type: 'line',
            smooth: true,
            data: resumes,
            itemStyle: { color: '#2563eb' },
            lineStyle: { color: '#2563eb', width: 2 },
            areaStyle: { color: 'rgba(37, 99, 235, 0.10)' }
          },
          {
            name: '新增JD',
            type: 'line',
            smooth: true,
            data: jds,
            itemStyle: { color: '#f97316' },
            lineStyle: { color: '#f97316', width: 2 },
            areaStyle: { color: 'rgba(249, 115, 22, 0.10)' }
          }
        ]
      })
    }
  }
}
</script>
