<template>
  <div class="page">
    <div v-if="algoTaskBar && algoTaskBar.visible" class="task-progress">
      <div class="task-progress-row">
        <div class="task-progress-title">JD题库生成</div>
        <div class="task-progress-meta">
          <span>{{ algoTaskBar.statusText }}</span>
          <span v-if="algoTaskBar.total"> {{ algoTaskBar.done }}/{{ algoTaskBar.total }}</span>
          <span v-if="algoTaskBar.elapsedSeconds != null"> {{ algoTaskBar.elapsedSeconds }}s</span>
        </div>
      </div>
      <el-progress :percentage="algoTaskBar.percent" :status="algoTaskBar.progressStatus" :stroke-width="10" />
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="openResumeDialog">选择简历</el-button>
        <span class="resume-chip">{{ selectedResumeName }}</span>
        <el-button type="primary" :loading="generating" :disabled="!canGenerate" @click="onClickGenerate">生成</el-button>
        <el-input-number v-model="count" :min="1" :max="30" :step="1" controls-position="right" size="small" />
        <span class="tip">（生成数量）</span>
      </div>

      <div class="toolbar-right">
        <el-button size="small" @click="fetchList">刷新</el-button>
      </div>
    </div>

    <el-table
      ref="table"
      v-loading="loading"
      :data="list"
      border
      style="width: 100%"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="name" label="JD题库名称" min-width="200" />
      <el-table-column prop="keywords" label="关键词" min-width="220">
        <template slot-scope="scope">
          <span>{{ scope.row.keywords || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="questionCount" label="题目个数" width="110">
        <template slot-scope="scope">
          <span>{{ Number(scope.row.questionCount || 0) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="题目预览" min-width="360">
        <template slot-scope="scope">
          <div v-if="getPreviewQuestions(scope.row).length > 0" class="preview">
            <div class="preview-actions">
              <span class="cell-link" @click.stop="openDetail(scope.row)">题库详情（点击查看）</span>
            </div>
            <div v-for="(q, idx) in getPreviewQuestions(scope.row)" :key="idx" class="preview-line">
              <span class="preview-idx">{{ idx + 1 }}.</span>
              <span class="preview-text">{{ q || '-' }}</span>
            </div>
          </div>
          <span v-else>-</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="120">
        <template slot-scope="scope">
          <el-button type="text" size="mini" :disabled="!selectedResumeId" @click="onClickGenerateOne(scope.row)">重新生成</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && (!list || list.length === 0)" class="empty">暂无数据</div>

    <el-pagination
      class="pager"
      :current-page="pageNo"
      :page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      @size-change="onSizeChange"
      @current-change="onCurrentChange"
    />

    <el-dialog :title="detailTitle" :visible.sync="detailVisible" width="760px" append-to-body>
      <div v-if="detailLoading" class="dialog-loading">
        <i class="el-icon-loading" />
        <span style="margin-left: 8px">加载中...</span>
      </div>
      <div v-else>
        <div v-if="detailQuestions.length === 0" class="dialog-empty">暂无详情</div>
        <div v-else class="qa-list">
          <div
            v-for="(qa, idx) in detailQuestions"
            :key="idx"
            class="qa-row"
            @click="openQaPreview(qa, idx)"
          >
            <div class="qa-left">
              <span class="qa-idx">Q{{ idx + 1 }}:</span>
              <span class="qa-text">{{ getDetailQuestion(qa) }}</span>
            </div>
            <i class="el-icon-arrow-right qa-arrow" />
          </div>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="detailVisible = false">关闭</el-button>
      </span>
    </el-dialog>

    <el-dialog :title="previewTitle" :visible.sync="previewVisible" width="760px" append-to-body>
      <div class="preview-body">{{ previewContent }}</div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="previewVisible = false">关闭</el-button>
      </span>
    </el-dialog>

    <el-dialog :title="resumeDialogTitle" :visible.sync="resumeDialogVisible" width="760px" append-to-body>
      <div class="resume-dialog-toolbar">
        <el-input
          v-model="resumeDialogQuery"
          placeholder="搜索：简历名"
          clearable
          style="width: 280px"
        />
        <div class="spacer" />
        <el-button size="small" @click="loadResumeOptions">刷新</el-button>
      </div>

      <el-table v-loading="resumeDialogLoading" :data="filteredResumeOptions" border style="width: 100%">
        <el-table-column prop="resumeId" label="简历ID" width="120" />
        <el-table-column prop="resumeName" label="简历名" min-width="220" />
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="120">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="selectResume(scope.row)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!resumeDialogLoading && (!filteredResumeOptions || filteredResumeOptions.length === 0)" class="empty">暂无可用简历（请先解析简历）</div>

      <span slot="footer" class="dialog-footer">
        <el-button @click="resumeDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { fetchJdList } from '@/api/interviewelf/jd'
import { fetchImportedResumes, fetchResumeContentBatch } from '@/api/interviewelf/resumeImport'
import { getAlgoTask } from '@/api/interviewelf/algoTask'

export default {
  name: 'JdInterviewQuestion',
  data() {
    return {
      list: [],
      loading: false,
      generating: false,
      selectedRows: [],
      pageNo: 1,
      pageSize: 50,
      total: 0,
      query: {
        keyword: '',
        jobType: ''
      },
      count: 10,
      selectedResumeId: null,
      selectedResumeName: '未选择简历',

      algoTaskBar: {
        visible: false,
        userId: null,
        taskId: '',
        total: 0,
        startedAt: 0,
        status: '',
        statusText: '排队中',
        progressStatus: null,
        percent: 0,
        done: 0,
        elapsedSeconds: 0
      },
      algoTaskPollToken: 0,
      algoTaskPollTimer: null,

      resumeDialogVisible: false,
      resumeDialogTitle: '选择简历（仅展示已解析简历）',
      resumeDialogLoading: false,
      resumeDialogQuery: '',
      resumeOptions: [],

      detailVisible: false,
      detailLoading: false,
      detailTitle: '详情',
      detailQuestions: [],
      previewVisible: false,
      previewTitle: '',
      previewContent: ''
    }
  },
  computed: {
    canGenerate() {
      return Boolean(this.selectedResumeId) && this.selectedRows.length > 0
    },
    filteredResumeOptions() {
      const q = String(this.resumeDialogQuery || '').trim().toLowerCase()
      if (!q) return this.resumeOptions
      return (this.resumeOptions || []).filter((r) => {
        const name = String(r.resumeName || '').toLowerCase()
        const id = String(r.resumeId || '')
        return name.includes(q) || id.includes(q)
      })
    }
  },
  created() {
    this.restoreAlgoTaskBar()
    this.fetchList()
  },
  methods: {
    getCurrentUserId() {
      try {
        const raw = window.localStorage.getItem('UserInfo')
        const obj = raw ? JSON.parse(raw) : null
        return obj && obj.userId ? Number(obj.userId) : null
      } catch (e) {
        return null
      }
    },
    getAlgoTaskBarStorageKey() {
      return 'JdQuestionBank-ActiveAlgoTask'
    },
    saveAlgoTaskBarToLocalStorage(payload) {
      try {
        window.localStorage.setItem(this.getAlgoTaskBarStorageKey(), JSON.stringify(payload || {}))
      } catch (e) {
        void e
      }
    },
    loadAlgoTaskBarFromLocalStorage() {
      try {
        const raw = window.localStorage.getItem(this.getAlgoTaskBarStorageKey())
        return raw ? JSON.parse(raw) : null
      } catch (e) {
        return null
      }
    },
    clearAlgoTaskBarLocalStorage() {
      try {
        window.localStorage.removeItem(this.getAlgoTaskBarStorageKey())
      } catch (e) {
        void e
      }
    },
    stopAlgoTaskPolling() {
      this.algoTaskPollToken++
      if (this.algoTaskPollTimer) {
        clearTimeout(this.algoTaskPollTimer)
        this.algoTaskPollTimer = null
      }
    },
    restoreAlgoTaskBar() {
      const snap = this.loadAlgoTaskBarFromLocalStorage()
      if (!snap || !snap.taskId) {
        return
      }
      this.clearAlgoTaskBarLocalStorage()
      this.algoTaskBar.visible = false
    },
    applyAlgoTaskBarFromTask(task) {
      const status = task && task.status ? String(task.status) : ''
      const processedCount = task && task.processedCount != null ? Number(task.processedCount) : null
      const result = task && task.result ? task.result : null
      const total = result && result.total != null ? Number(result.total) : (this.algoTaskBar.total || 0)
      const done = processedCount != null ? processedCount : (total || 0)

      this.algoTaskBar.status = status
      this.algoTaskBar.total = total || 0
      this.algoTaskBar.done = done || 0
      this.algoTaskBar.percent = task && task.progress != null ? Number(task.progress) : 0
      this.algoTaskBar.percent = Math.max(0, Math.min(100, this.algoTaskBar.percent))
      const elapsedSeconds = Math.floor((Date.now() - (this.algoTaskBar.startedAt || Date.now())) / 1000)
      this.algoTaskBar.elapsedSeconds = elapsedSeconds

      if (status === 'SUCCESS') {
        this.algoTaskBar.statusText = '成功'
        this.algoTaskBar.progressStatus = 'success'
      } else if (status === 'FAILED') {
        this.algoTaskBar.statusText = '失败'
        this.algoTaskBar.progressStatus = 'exception'
      } else if (status === 'CANCELED') {
        this.algoTaskBar.statusText = '已停止'
        this.algoTaskBar.progressStatus = 'exception'
      } else if (status === 'RUNNING') {
        this.algoTaskBar.statusText = '生成中'
        this.algoTaskBar.progressStatus = null
      } else {
        this.algoTaskBar.statusText = '排队中'
        this.algoTaskBar.progressStatus = null
      }
    },
    pollAlgoTask() {
      const token = ++this.algoTaskPollToken
      const userId = this.algoTaskBar.userId
      const taskId = this.algoTaskBar.taskId
      if (!userId || !taskId) {
        return
      }
      getAlgoTask(userId, taskId).then((resp) => {
        if (token !== this.algoTaskPollToken) return
        if (!resp || resp.code !== 20000) {
          return
        }
        const task = resp.data
        this.applyAlgoTaskBarFromTask(task)
        const status = task && task.status ? String(task.status) : ''
        if (status === 'SUCCESS' || status === 'FAILED' || status === 'CANCELED') {
          this.clearAlgoTaskBarLocalStorage()
          this.fetchStats()
          return
        }
        this.algoTaskPollTimer = setTimeout(() => this.pollAlgoTask(), 1200)
      })
    },

    openResumeDialog() {
      this.resumeDialogVisible = true
      this.loadResumeOptions()
    },
    loadResumeOptions() {
      const userId = this.getCurrentUserId()
      if (!userId) {
        this.$message.error('未登录')
        return
      }
      this.resumeDialogLoading = true
      fetchImportedResumes(userId)
        .then((resp) => {
          if (!resp || resp.code !== 20000) {
            throw new Error(resp && resp.message ? resp.message : '获取简历列表失败')
          }
          const items = resp.data && resp.data.items ? resp.data.items : []
          const resumeIds = (items || []).map((r) => r && r.resumeId).filter((x) => x)
          return fetchResumeContentBatch(userId, resumeIds).then((resp2) => {
            if (!resp2 || resp2.code !== 20000) {
              throw new Error(resp2 && resp2.message ? resp2.message : '获取简历内容失败')
            }
            const contentItems = resp2.data && resp2.data.items ? resp2.data.items : []
            const okIds = new Set(
              (contentItems || [])
                .filter((it) => it && it.ok && it.content)
                .map((it) => it.resumeId)
            )

            const out = []
            ;(items || []).forEach((r) => {
              if (!r || !okIds.has(r.resumeId)) return
              const files = r.files || []
              const resumeName = files && files.length > 0 ? (files[0].fileName || r.title || '') : (r.title || '')
              out.push({
                resumeId: r.resumeId,
                resumeName: resumeName || `简历${r.resumeId}`,
                updateTime: r.updateTime || r.createTime
              })
            })
            this.resumeOptions = out
          })
        })
        .catch((e) => {
          this.$message.error(e && e.message ? e.message : '加载失败')
        })
        .finally(() => {
          this.resumeDialogLoading = false
        })
    },
    selectResume(row) {
      this.selectedResumeId = row.resumeId
      this.selectedResumeName = row.resumeName || `简历${row.resumeId}`
      this.resumeDialogVisible = false
      this.fetchStats()
    },

    fetchList() {
      this.loading = true
      fetchJdList({
        pageNo: this.pageNo,
        pageSize: this.pageSize,
        keyword: this.query.keyword,
        jobType: this.query.jobType
      })
        .then((resp) => {
          if (!resp || resp.code !== 20000) {
            throw new Error(resp && resp.message ? resp.message : '加载失败')
          }
          const data = resp.data || {}
          const items = data.items || []
          this.total = Number(data.total || 0)
          this.list = (items || []).map((it) => {
            return {
              id: it.id,
              name: it.jobTitle,
              keywords: '',
              questionCount: 0,
              previewQuestions: []
            }
          })
          this.fetchStats()
        })
        .catch((e) => {
          this.$message.error(e && e.message ? e.message : '加载失败')
        })
        .finally(() => {
          this.loading = false
        })
    },
    fetchStats() {
      return
    },
    onSelectionChange(rows) {
      this.selectedRows = rows || []
    },
    onSizeChange(n) {
      this.pageSize = n
      this.pageNo = 1
      this.fetchList()
    },
    onCurrentChange(n) {
      this.pageNo = n
      this.fetchList()
    },
    onClickGenerate() {
      this.$message.warning('方案已调整：JD题库生成能力已取消/下线')
    },
    onClickGenerateOne(row) {
      if (!row) return
      this.selectedRows = [row]
      this.onClickGenerate()
    },
    getPreviewQuestions(row) {
      const arr = row && row.previewQuestions ? row.previewQuestions : []
      const out = Array.isArray(arr) ? arr.filter((x) => x) : []
      const two = out.slice(0, 2)
      if (two.length <= 1) return two
      const narrow = window && window.innerWidth ? window.innerWidth : 1200
      return narrow < 1280 ? two.slice(0, 1) : two
    },
    openDetail(row) {
      this.$message.info('方案已调整：暂无题库详情')
    },
    getDetailQuestion(qa) {
      if (!qa) return '-'
      return qa.question || qa.questionContent || '-'
    },
    openQaPreview(qa, idx) {
      const q = this.getDetailQuestion(qa)
      const a = qa && (qa.answer || qa.answerContent) ? (qa.answer || qa.answerContent) : '-'
      this.previewTitle = `Q${idx + 1}: ${q}`
      this.previewContent = a
      this.previewVisible = true
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
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.resume-chip {
  color: #606266;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tip {
  color: #909399;
}

.task-progress {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
}

.task-progress-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.task-progress-title {
  font-weight: 600;
}

.task-progress-meta {
  display: flex;
  gap: 10px;
  color: #606266;
}

.preview-actions {
  margin-bottom: 6px;
}

.cell-link {
  color: #409eff;
  cursor: pointer;
}

.preview-line {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
}

.preview-idx {
  color: #909399;
}

.preview-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pager {
  margin-top: 12px;
  text-align: right;
}

.qa-list {
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.qa-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
}

.qa-row:last-child {
  border-bottom: none;
}

.qa-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.qa-idx {
  color: #909399;
  flex: 0 0 auto;
}

.qa-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qa-arrow {
  color: #c0c4cc;
}

.preview-body {
  white-space: pre-wrap;
  line-height: 20px;
}

.resume-dialog-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.spacer {
  flex: 1;
}

.empty {
  text-align: center;
  color: #909399;
  margin-top: 12px;
}

.dialog-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  color: #606266;
}

.dialog-empty {
  text-align: center;
  color: #909399;
  padding: 24px 0;
}
</style>
