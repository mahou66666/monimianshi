<template>
  <div class="storage-cell">
    <span
      v-if="storagePath || url"
      class="cell-link"
      @click="$emit('preview', '地址', storagePath || url)"
    >{{ displayText }}</span>
    <span v-else>-</span>
    <div class="storage-meta">
      <el-tag v-if="canParse" size="mini" type="success">可提取</el-tag>
      <el-tag v-else size="mini" type="danger">文件缺失</el-tag>
      <span class="storage-tip">{{ fileCheckMessage }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ResumeStorageStatus',
  props: {
    storagePath: {
      type: String,
      default: ''
    },
    url: {
      type: String,
      default: ''
    },
    canParse: {
      type: Boolean,
      default: false
    },
    fileCheckMessage: {
      type: String,
      default: ''
    },
    maxLen: {
      type: Number,
      default: 40
    }
  },
  computed: {
    displayText() {
      const raw = this.storagePath || this.url || ''
      if (raw.length <= this.maxLen) {
        return raw
      }
      return raw.slice(0, this.maxLen) + '...'
    }
  }
}
</script>

<style scoped>
.cell-link {
  color: #409eff;
  cursor: pointer;
  user-select: none;
}

.storage-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.storage-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.storage-tip {
  font-size: 12px;
  color: #909399;
}
</style>
