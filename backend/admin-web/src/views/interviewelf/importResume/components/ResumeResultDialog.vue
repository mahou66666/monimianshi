<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="820px"
    append-to-body
    @update:visible="$emit('update:visible', $event)"
    @close="$emit('update:visible', false)"
  >
    <div v-loading="loading">
      <div v-if="html" class="result-html" v-html="html" />
      <div v-else class="empty">暂无提取结果</div>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'ResumeResultDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: '查看提取结果'
    },
    html: {
      type: String,
      default: ''
    }
  }
}
</script>

<style scoped>
.empty {
  text-align: center;
  color: #909399;
  margin-top: 12px;
}

.result-html {
  max-height: 70vh;
  overflow-y: auto;
  line-height: 1.8;
  color: #303133;
}

.result-html ::v-deep h1,
.result-html ::v-deep h2,
.result-html ::v-deep h3 {
  color: #303133;
  margin: 0 0 12px;
}

.result-html ::v-deep ul {
  padding-left: 20px;
  margin: 0 0 14px;
}

.result-html ::v-deep li {
  margin-bottom: 8px;
}

.result-html ::v-deep p {
  margin: 0 0 12px;
}
</style>
