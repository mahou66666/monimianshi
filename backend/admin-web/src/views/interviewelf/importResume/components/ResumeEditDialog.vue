<template>
  <el-dialog
    title="修改简历"
    :visible="visible"
    width="760px"
    append-to-body
    :close-on-click-modal="false"
    @update:visible="$emit('update:visible', $event)"
    @close="$emit('cancel')"
  >
    <div v-loading="loading">
      <el-form label-width="90px" size="small">
        <el-form-item label="简历名称">
          <el-input
            :value="titleValue"
            maxlength="120"
            show-word-limit
            @input="$emit('update:title', $event)"
          />
        </el-form-item>
        <el-form-item label="简历内容">
          <el-input
            :value="contentValue"
            type="textarea"
            :rows="14"
            placeholder="可编辑简历原始内容（HTML）"
            @input="$emit('update:content', $event)"
          />
        </el-form-item>
      </el-form>
    </div>
    <span slot="footer">
      <el-button :disabled="saving" @click="$emit('cancel')">取消</el-button>
      <el-button
        v-if="canManageResume"
        type="primary"
        :loading="saving"
        @click="$emit('submit')"
      >保存</el-button>
    </span>
  </el-dialog>
</template>

<script>
export default {
  name: 'ResumeEditDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    saving: {
      type: Boolean,
      default: false
    },
    canManageResume: {
      type: Boolean,
      default: false
    },
    titleValue: {
      type: String,
      default: ''
    },
    contentValue: {
      type: String,
      default: ''
    }
  }
}
</script>
