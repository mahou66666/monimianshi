<template>
  <div class="page">
    <div class="toolbar">
      <el-upload
        ref="upload"
        action=""
        :show-file-list="false"
        :auto-upload="false"
        :multiple="true"
        accept="audio/*"
        :http-request="dummyRequest"
        :on-change="onFileChange"
      >
        <el-button type="primary">选择音频</el-button>
      </el-upload>
    </div>

    <el-table :data="list" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="audioName" label="音频名称" min-width="180">
        <template slot-scope="scope">
          <span>{{ scope.row.audioName || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="文本" min-width="220">
        <template slot-scope="scope">
          <span v-if="scope.row.transcriptText" class="text-preview" @click="openTranscript(scope.row)">
            {{ truncateText(scope.row.transcriptText) }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>

      <el-table-column prop="qaCount" label="问答数" width="100">
        <template slot-scope="scope">
          <span>{{ scope.row.qaCount != null ? scope.row.qaCount : '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="durationSeconds" label="音频时长" width="110">
        <template slot-scope="scope">
          <span>{{ formatDuration(scope.row.durationSeconds) }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="wordCount" label="字数" width="90">
        <template slot-scope="scope">
          <span>{{ scope.row.wordCount != null ? scope.row.wordCount : '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="transStatus" label="转译状态" width="110">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.transStatus" size="mini" :type="statusTagType(scope.row.transStatus)">
            {{ scope.row.transStatus }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>

      <el-table-column prop="companyName" label="所属公司" min-width="140">
        <template slot-scope="scope">
          <span>{{ scope.row.companyName || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="jd" label="JD" min-width="140">
        <template slot-scope="scope">
          <span>{{ scope.row.jd || '-' }}</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      title="待上传文件"
      :visible.sync="confirmVisible"
      width="520px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      append-to-body
    >
      <el-table :data="pendingFileRows" border style="width: 100%">
        <el-table-column prop="name" label="文件名" />
      </el-table>
      <div v-if="pendingFileRows.length === 0" class="empty">暂无数据</div>
      <span slot="footer" class="dialog-footer">
        <el-button type="danger" @click="onCancelConfirm">取消</el-button>
        <el-button type="success" :loading="uploading" @click="submitUpload">提交</el-button>
      </span>
    </el-dialog>

    <div v-if="!list || list.length === 0" class="empty">暂无数据</div>

    <el-dialog
      title="转写文本"
      :visible.sync="transcriptDialogVisible"
      width="720px"
      append-to-body
    >
      <div class="transcript-content">{{ currentTranscriptText || '-' }}</div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="transcriptDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { fetchImportedAudios, importAudioBatch } from '@/api/interviewelf/audioImport'

export default {
  name: 'ImportAudio',
  data() {
    return {
      list: [],
      transcriptDialogVisible: false,
      currentTranscriptText: '',
      pendingFiles: [],
      confirmVisible: false,
      uploading: false,
      loadingImported: false
    }
  },
  computed: {
    pendingFileRows() {
      return (this.pendingFiles || []).map((f) => {
        return { name: f && f.name ? f.name : '' }
      })
    }
  },
  created() {
    this.refreshImported()
  },
  methods: {
    dummyRequest() {
      return Promise.resolve()
    },
    isAudio(file) {
      const name = String((file && file.name) || '').toLowerCase()
      const type = String((file && file.type) || '').toLowerCase()
      if (type && type.startsWith('audio/')) return true
      return name.endsWith('.mp3') || name.endsWith('.wav') || name.endsWith('.m4a') || name.endsWith('.aac')
    },
    onFileChange(file) {
      const raw = file && file.raw ? file.raw : file
      if (!raw) return

      if (!this.isAudio(raw)) {
        this.$message.warning(`仅支持音频文件（mp3/wav/m4a/aac），已忽略：${raw.name || ''}`)
        return
      }

      const exists = this.pendingFiles.some((f) => f && f.name === raw.name)
      if (!exists) {
        this.pendingFiles.push(raw)
      } else {
        this.$message.warning('该文件已在待上传列表中')
      }

      if (this.pendingFiles.length > 0) {
        this.confirmVisible = true
      }

      if (this.$refs.upload && this.$refs.upload.clearFiles) {
        this.$refs.upload.clearFiles()
      }
    },
    getCurrentUserId() {
      try {
        const raw = window.localStorage.getItem('InterviewElf-User')
        const user = raw ? JSON.parse(raw) : null
        return user && user.id ? Number(user.id) : null
      } catch (e) {
        return null
      }
    },
    mapImportedToList(imported) {
      const items = (imported && imported.items) ? imported.items : []
      return items.map((it) => {
        return {
          ...it,
          id: it && it.id != null ? it.id : '',
          audioName: (it && it.audioName) ? it.audioName : '',
          transcriptText: (it && it.transcriptText) ? it.transcriptText : '',
          durationSeconds: it && it.durationSeconds != null ? it.durationSeconds : null,
          wordCount: it && it.wordCount != null ? it.wordCount : null,
          __pending: false
        }
      })
    },
    async refreshImported() {
      const userId = this.getCurrentUserId()
      if (!userId) {
        return
      }

      this.loadingImported = true
      try {
        const data = await fetchImportedAudios(userId)
        this.list = this.mapImportedToList(data)
      } catch (e) {
        console.log(e)
      } finally {
        this.loadingImported = false
      }
    },
    async submitUpload() {
      if (this.uploading) return

      const userId = this.getCurrentUserId()
      if (!userId) {
        this.$message.error('未获取到当前用户ID，请重新登录')
        return
      }

      if (!this.pendingFiles || this.pendingFiles.length === 0) {
        this.$message.warning('请先选择音频文件')
        return
      }

      this.uploading = true
      try {
        await importAudioBatch(userId, this.pendingFiles)
        this.$message.success('上传成功')
        this.confirmVisible = false
        this.pendingFiles = []
        if (this.$refs.upload && this.$refs.upload.clearFiles) {
          this.$refs.upload.clearFiles()
        }
        await this.refreshImported()
      } catch (e) {
        console.log(e)
      } finally {
        this.uploading = false
      }
    },
    onCancelConfirm() {
      if (this.uploading) return
      this.confirmVisible = false
      this.pendingFiles = []
      if (this.$refs.upload && this.$refs.upload.clearFiles) {
        this.$refs.upload.clearFiles()
      }
    },
    truncateText(text) {
      const raw = String(text || '')
      if (!raw) return ''
      const maxLen = 28
      if (raw.length <= maxLen) return raw
      return raw.slice(0, maxLen) + '...'
    },
    openTranscript(row) {
      this.currentTranscriptText = row && row.transcriptText ? String(row.transcriptText) : ''
      this.transcriptDialogVisible = true
    },
    formatDuration(seconds) {
      const s = Number(seconds)
      if (!Number.isFinite(s) || s <= 0) return '-'
      const m = Math.floor(s / 60)
      const r = Math.floor(s % 60)
      const mm = String(m).padStart(2, '0')
      const ss = String(r).padStart(2, '0')
      return `${mm}:${ss}`
    },
    statusTagType(status) {
      if (status === '成功') return 'success'
      if (status === '失败') return 'danger'
      return 'info'
    }
  }
}
</script>

<style scoped>
.page {
  padding: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.empty {
  text-align: center;
  color: #909399;
  margin-top: 12px;
}

.text-preview {
  color: #409eff;
  cursor: pointer;
}

.transcript-content {
  white-space: pre-wrap;
  line-height: 20px;
}
</style>
