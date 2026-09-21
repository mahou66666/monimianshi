<template>
  <div v-if="bar && bar.visible" class="task-progress">
    <div class="task-progress-row">
      <div class="task-progress-title">批量提取</div>
      <div class="task-progress-meta">
        <span>{{ bar.statusText }}</span>
        <span v-if="bar.total"> {{ bar.done }}/{{ bar.total }}</span>
        <span v-if="bar.elapsedSeconds != null"> {{ bar.elapsedSeconds }}s</span>
      </div>
    </div>
    <div class="task-progress-actions">
      <el-button
        v-if="canManageResume"
        size="mini"
        type="warning"
        :disabled="!(bar.status === 'RUNNING' || bar.status === 'PENDING')"
        @click="$emit('cancel')"
      >暂停</el-button>
      <el-button
        v-if="canManageResume"
        size="mini"
        type="primary"
        :disabled="bar.status !== 'CANCELED'"
        @click="$emit('continue')"
      >继续</el-button>
    </div>
    <el-progress :percentage="bar.percent" :status="bar.progressStatus" :stroke-width="10" />
  </div>
</template>

<script>
export default {
  name: 'ResumeTaskProgress',
  props: {
    bar: {
      type: Object,
      default: () => ({})
    },
    canManageResume: {
      type: Boolean,
      default: false
    }
  }
}
</script>

<style scoped>
.task-progress {
  margin-bottom: 12px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.task-progress-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.task-progress-title {
  font-weight: 600;
}

.task-progress-meta span {
  margin-left: 8px;
  color: #606266;
}

.task-progress-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin: 6px 0 8px;
}
</style>
