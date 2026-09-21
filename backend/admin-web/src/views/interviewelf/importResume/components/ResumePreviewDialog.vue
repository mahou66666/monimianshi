<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="720px"
    append-to-body
    @update:visible="$emit('update:visible', $event)"
    @close="$emit('update:visible', false)"
  >
    <div class="preview-body">{{ content }}</div>
  </el-dialog>
</template>

<script>
export default {
  name: 'ResumePreviewDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: ''
    },
    content: {
      type: String,
      default: ''
    }
  }
}
</script>

<style scoped>
.preview-body {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
