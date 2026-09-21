<template>
  <div :class="className" :style="{ height, width }" />
</template>

<script>
import echarts from 'echarts'
require('echarts/theme/macarons')
import resize from '@/views/dashboard/admin/components/mixins/resize'

export default {
  name: 'CityChart',
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
    items: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      chart: null
    }
  },
  watch: {
    items: {
      deep: true,
      handler(val) {
        this.setOptions(val)
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.chart = echarts.init(this.$el, 'macarons')
      this.setOptions(this.items)
    })
  },
  beforeDestroy() {
    if (!this.chart) return
    this.chart.dispose()
    this.chart = null
  },
  methods: {
    setOptions(items = []) {
      if (!this.chart) return

      const labels = items.map(item => item.name)
      const values = items.map(item => Number(item.count || 0))

      this.chart.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        grid: {
          left: 18,
          right: 18,
          bottom: 18,
          top: 24,
          containLabel: true
        },
        xAxis: {
          type: 'value',
          minInterval: 1,
          axisTick: { show: false }
        },
        yAxis: {
          type: 'category',
          data: labels,
          axisTick: { show: false }
        },
        series: [
          {
            type: 'bar',
            data: values,
            barWidth: 18,
            itemStyle: {
              color: function(params) {
                const palette = ['#2563eb', '#0f766e', '#f97316', '#0891b2', '#7c3aed', '#dc2626']
                return palette[params.dataIndex % palette.length]
              },
              borderRadius: [0, 8, 8, 0]
            },
            label: {
              show: true,
              position: 'right',
              color: '#374151'
            }
          }
        ]
      })
    }
  }
}
</script>
