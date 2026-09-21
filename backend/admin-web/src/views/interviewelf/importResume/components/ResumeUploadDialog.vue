<template>
  <el-dialog
    title="待上传文件"
    :visible="visible"
    width="520px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    append-to-body
    @update:visible="$emit('update:visible', $event)"
  >
    <el-alert
      v-if="uploadStats.total > 0"
      class="tip"
      :title="uploadStatsText"
      :type="uploadStats.duplicate > 0 ? 'warning' : 'info'"
      show-icon
      :closable="false"
    />

    <el-table :data="uploadItems" border style="width: 100%">
      <el-table-column prop="name" label="文件名" />
      <el-table-column label="状态" width="110">
        <template slot-scope="scope">
          <span>{{ getUploadItemStatusText(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="说明" />
      <el-table-column label="操作" width="90">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row && scope.row.status === 'duplicate' && (scope.row.downloadUrl || scope.row.resumeFileId)"
            type="text"
            size="mini"
            @click="$emit('download-duplicate', scope.row)"
          >下载</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="uploadItems.length === 0" class="empty">暂无数据</div>
    <span slot="footer" class="dialog-footer">
      <el-button
        v-if="canManageResume"
        :disabled="importing || uploadStats.duplicate === 0"
        @click="$emit('remove-status', 'duplicate')"
      >移除重复</el-button>
      <el-button
        v-if="canManageResume"
        :disabled="importing || uploadStats.fail === 0"
        @click="$emit('remove-status', 'fail')"
      >移除失败</el-button>
      <el-button
        v-if="canManageResume"
        :disabled="importing || uploadStats.total === 0"
        @click="$emit('clear')"
      >清空</el-button>
      <el-button :disabled="importing" type="danger" @click="$emit('cancel')">取消</el-button>
      <el-button
        v-if="canManageResume"
        type="success"
        :loading="importing"
        @click="$emit('submit')"
      >开始/继续上传</el-button>
    </span>
  </el-dialog>
</template>

<script>
export default {
  name: 'ResumeUploadDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    canManageResume: {
      type: Boolean,
      default: false
    },
    importing: {
      type: Boolean,
      default: false
    },
    uploadStats: {
      type: Object,
      default: () => ({ total: 0, duplicate: 0, fail: 0 })
    },
    uploadStatsText: {
      type: String,
      default: ''
    },
    uploadItems: {
      type: Array,
      default: () => []
    },
    getUploadItemStatusText: {
      type: Function,
      default: () => ''
    }
  }
}
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}

.empty {
  text-align: center;
  color: #909399;
  margin-top: 12px;
}
</style>
