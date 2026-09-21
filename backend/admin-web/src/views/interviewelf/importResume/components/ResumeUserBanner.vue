<template>
  <div class="user-banner">
    <div class="user-banner-text">
      <div class="user-banner-title">{{ title }}</div>
      <div class="user-banner-desc">{{ description }}</div>
    </div>
    <div class="user-banner-actions">
      <el-button
        v-if="showBackButton"
        size="mini"
        plain
        @click="$emit('switch-current-user')"
      >返回我的简历</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ResumeUserBanner',
  props: {
    title: {
      type: String,
      default: ''
    },
    description: {
      type: String,
      default: ''
    },
    showBackButton: {
      type: Boolean,
      default: false
    }
  }
}
</script>

<style scoped>
.user-banner {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  padding: 12px 14px;
  background: #f4f8ff;
  border: 1px solid #d9e8ff;
  border-radius: 4px;
}

.user-banner-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.user-banner-desc {
  font-size: 12px;
  line-height: 1.6;
  color: #606266;
}

.user-banner-actions {
  flex-shrink: 0;
}
</style>
