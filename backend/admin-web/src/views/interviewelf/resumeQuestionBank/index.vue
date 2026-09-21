<template>
  <div class="page">
    <div v-if="algoTaskBar && algoTaskBar.visible" class="task-progress">
      <div class="task-progress-row">
        <div class="task-progress-title">{{ titleText }}</div>
        <div class="task-progress-meta">
          <span>{{ algoTaskBar.statusText }}</span>
          <span v-if="algoTaskBar.total"> {{ algoTaskBar.done }}/{{ algoTaskBar.total }}</span>
          <span v-if="algoTaskBar.elapsedSeconds != null"> {{ algoTaskBar.elapsedSeconds }}s</span>
        </div>
      </div>
      <div class="task-progress-actions">
        <el-button size="mini" type="warning" :disabled="!canCancelTask" @click="onClickCancelTask">暂停</el-button>
        <el-button size="mini" type="primary" :disabled="!canContinueTask" @click="onClickContinueTask">继续</el-button>
      </div>
      <el-progress :percentage="algoTaskBar.percent" :status="algoTaskBar.progressStatus" :stroke-width="10" />
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" :loading="generating" :disabled="selectedIds.length === 0" @click="onClickGenerate">
          生成
        </el-button>
        <el-input-number v-model="count" :min="1" :max="30" :step="1" controls-position="right" size="small" />
        <span class="tip">（生成数量）</span>
      </div>

      <div class="toolbar-right">
        <el-input
          v-model="searchKeyword"
          size="small"
          clearable
          placeholder="搜索简历名"
          style="width: 220px"
          @input="onSearchChange"
        />
        <el-select
          v-model="generatedFilter"
          size="small"
          placeholder="是否已生成"
          style="width: 140px"
          @change="onSearchChange"
        >
          <el-option label="全部" value="all" />
          <el-option label="已生成" value="generated" />
          <el-option label="未生成" value="not_generated" />
        </el-select>
        <el-button size="small" @click="fetchList">刷新</el-button>
      </div>
    </div>

    <el-table
      ref="table"
      v-loading="loading"
      :data="list"
      :row-style="tableRowStyle"
      :cell-style="tableCellStyle"
      border
      style="width: 100%"
      row-key="resumeId"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="50" :reserve-selection="true" :selectable="isRowSelectable" />

      <el-table-column prop="resumeId" label="简历ID" width="120">
        <template slot-scope="scope">
          <span>{{ scope.row.resumeId || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="resumeName" label="简历名" min-width="160" show-overflow-tooltip>
        <template slot-scope="scope">
          <span>{{ scope.row.resumeName || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="keywords" label="关键词" min-width="220" show-overflow-tooltip>
        <template slot-scope="scope">
          <span>{{ scope.row.keywords || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="questionCount" label="题目数" width="120">
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

      <el-table-column label="操作" width="140">
        <template slot-scope="scope">
          <el-button type="text" size="mini" :disabled="!isRowSelectable(scope.row)" @click="onClickGenerateOne(scope.row)">重新生成</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && (!list || list.length === 0)" class="empty">暂无数据</div>

    <el-pagination
      class="pager"
      :current-page="pageNo"
      :page-size="pageSize"
      :page-sizes="[50, 100, 200]"
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
  </div>
</template>

<script>
import {
  createResumeQuestionBankTask,
  cancelResumeQuestionBankTask,
  fetchResumeQuestionBankDetail,
  fetchResumeQuestionBankList,
  getPendingResumeIdsForQuestionBank,
  getResumeQuestionBankTask
} from '@/api/interviewelf/resumeQuestionBank'

export default {
  name: 'ResumeQuestionBank',
  data() {
    return {
      list: [],
      loading: false,
      generating: false,
      selectedIds: [],
      searchKeyword: '',
      generatedFilter: 'all',
      pageNo: 1,
      pageSize: 100,
      total: 0,
      count: 10,
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
    sectionKey() {
      const metaKey = this.$route && this.$route.meta ? this.$route.meta.sectionKey : ''
      const paramKey = this.$route && this.$route.params ? this.$route.params.sectionKey : ''
      return String(metaKey || paramKey || '').trim()
    },
    titleText() {
      const k = String(this.sectionKey || '').toUpperCase()
      if (k === 'EDUCATION') return '教育经历题库'
      if (k === 'INTERNSHIP_EXPERIENCE') return '实习题库'
      if (k === 'PROJECT_EXPERIENCE') return '项目题库'
      if (k === 'AWARDS') return '获奖题库'
      return '题库'
    },
    canCancelTask() {
      if (!this.algoTaskBar || !this.algoTaskBar.visible) return false
      const s = String(this.algoTaskBar.status || '')
      return s === 'PENDING' || s === 'RUNNING'
    },
    canContinueTask() {
      if (!this.algoTaskBar || !this.algoTaskBar.visible) return false
      const s = String(this.algoTaskBar.status || '')
      return s === 'CANCELED' || s === 'FAILED'
    }

  },
  watch: {
    sectionKey() {
      this.selectedIds = []
      this.pageNo = 1
      this.stopAlgoTaskPolling()
      this.algoTaskBar.visible = false
      this.fetchList()
      this.restoreAlgoTaskBar()
    }
  },
  created() {
    this.fetchList()
    this.restoreAlgoTaskBar()
  },
  methods: {
    getAlgoTaskBarStorageKey() {
      const k = String(this.sectionKey || '').toUpperCase()
      return `ResumeQuestionBank-ActiveAlgoTask-${k}`
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
    updateAlgoTaskBarBySnapshot(snapshot) {
      const userId = snapshot && snapshot.userId != null ? Number(snapshot.userId) : null
      const taskId = snapshot && snapshot.taskId ? String(snapshot.taskId) : ''
      const total = snapshot && snapshot.total != null ? Number(snapshot.total) : 0
      const startedAt = snapshot && snapshot.startedAt != null ? Number(snapshot.startedAt) : Date.now()

      this.algoTaskBar.visible = Boolean(taskId)
      this.algoTaskBar.userId = userId
      this.algoTaskBar.taskId = taskId
      this.algoTaskBar.total = Number.isFinite(total) && total > 0 ? total : 0
      this.algoTaskBar.startedAt = Number.isFinite(startedAt) && startedAt > 0 ? startedAt : Date.now()
    },
    applyAlgoTaskBarFromTask(task) {
      const status = task && task.status ? String(task.status) : ''
      const processedCount = task && task.processedCount != null ? Number(task.processedCount) : null
      const result = task && task.result ? task.result : null
      const request = task && task.request ? task.request : null
      const resultTotal = result && result.total != null ? Number(result.total) : null
      const requestTotal = request && request.options && Array.isArray(request.options.resumeIds) ? request.options.resumeIds.length : null
      const total = this.algoTaskBar.total || (Number.isFinite(resultTotal) && resultTotal > 0 ? resultTotal : 0) || (Number.isFinite(requestTotal) && requestTotal > 0 ? requestTotal : 0)
      const startedAt = this.algoTaskBar.startedAt || Date.now()

      let statusText = '排队中'
      if (status === 'RUNNING') statusText = '处理中'
      if (status === 'SUCCESS') statusText = '成功'
      if (status === 'FAILED') statusText = '失败'

      let progressStatus = null
      if (status === 'SUCCESS') progressStatus = 'success'
      if (status === 'FAILED') progressStatus = 'exception'

      let done = 0
      if (status === 'SUCCESS' || status === 'FAILED') {
        const successCount = result && result.successCount != null ? Number(result.successCount) : null
        const failCount = result && (result.failedCount != null ? result.failedCount : result.failCount) != null
          ? Number(result.failedCount != null ? result.failedCount : result.failCount)
          : null
        const skipCount = result && result.skippedCount != null ? Number(result.skippedCount) : null
        if (successCount != null && failCount != null && skipCount != null && !Number.isNaN(successCount) && !Number.isNaN(failCount) && !Number.isNaN(skipCount)) {
          done = Math.max(0, successCount + failCount + skipCount)
        } else if (processedCount != null && !Number.isNaN(processedCount)) {
          done = Math.max(0, Math.floor(processedCount))
        } else if (status === 'SUCCESS' && total > 0) {
          done = total
        }
      } else if (processedCount != null && !Number.isNaN(processedCount)) {
        done = Math.min(total, Math.max(0, Math.floor(processedCount)))
      }

      let percent = 0
      if (total > 0) {
        percent = Math.min(100, Math.max(0, Math.floor((done / total) * 100)))
      }

      this.algoTaskBar.status = status
      this.algoTaskBar.statusText = statusText
      this.algoTaskBar.progressStatus = progressStatus
      this.algoTaskBar.percent = percent
      this.algoTaskBar.done = done
      if (!this.algoTaskBar.total && total) {
        this.algoTaskBar.total = total
      }
      this.algoTaskBar.elapsedSeconds = Math.floor((Date.now() - startedAt) / 1000)
    },
    async runAlgoTaskPolling(userId, taskId, startedAt) {
      const token = ++this.algoTaskPollToken

      const loop = async() => {
        if (token !== this.algoTaskPollToken) return
        if (!this.algoTaskBar.visible || String(this.algoTaskBar.taskId || '') !== String(taskId || '')) return

        let t
        try {
          t = await getResumeQuestionBankTask(userId, taskId)
        } catch (e) {
          this.algoTaskPollTimer = setTimeout(loop, 2000)
          return
        }

        this.applyAlgoTaskBarFromTask(t)

        const status = t && t.status ? String(t.status) : ''
        if (status === 'SUCCESS') {
          this.clearAlgoTaskBarLocalStorage()
          this.stopAlgoTaskPolling()
          this.algoTaskBar.visible = false

          const result = t && t.result ? t.result : {}
          const ok = Number(result && result.successCount != null ? result.successCount : 0)
          const fail = Number(result && result.failedCount != null ? result.failedCount : 0)
          const skip = Number(result && result.skippedCount != null ? result.skippedCount : 0)
          if (fail > 0) {
            const items = result && Array.isArray(result.items) ? result.items : []
            const firstFail = items.find((it) => it && it.status === 'fail' && it.errorMessage)
            if (firstFail) {
              this.$message.error(firstFail.errorMessage)
            } else {
              this.$message.error(`生成完成：成功 ${ok}，失败 ${fail}，跳过 ${skip}`)
            }
          } else {
            this.$message.success(`生成完成：成功 ${ok}，失败 ${fail}，跳过 ${skip}`)
          }
          await this.fetchList()
          return
        }
        if (status === 'FAILED') {
          this.clearAlgoTaskBarLocalStorage()
          this.stopAlgoTaskPolling()
          this.algoTaskBar.visible = false
          const msg = t && t.errorMessage ? String(t.errorMessage) : '生成失败'
          this.$message.error(msg)
          return
        }

        if (status === 'CANCELED') {
          this.stopAlgoTaskPolling()
          this.applyAlgoTaskBarFromTask(t)
          this.$message.warning('任务已暂停，可点击“继续”处理剩余简历')
          return
        }

        const elapsedMs = Date.now() - startedAt
        const intervalMs = elapsedMs >= 60 * 1000 ? 2000 : 1000
        this.algoTaskPollTimer = setTimeout(loop, intervalMs)
      }

      await loop()
    },

    async onClickCancelTask() {
      const userId = this.getCurrentUserId()
      const taskId = this.algoTaskBar && this.algoTaskBar.taskId ? String(this.algoTaskBar.taskId) : ''
      if (!userId || !taskId) return
      try {
        await cancelResumeQuestionBankTask(userId, taskId)
        this.$message.success('已发起暂停')
      } catch (e) {
        this.$message.error('暂停失败')
      }
    },

    async onClickContinueTask() {
      const userId = this.getCurrentUserId()
      const taskId = this.algoTaskBar && this.algoTaskBar.taskId ? String(this.algoTaskBar.taskId) : ''
      if (!userId || !taskId) return

      let pending
      try {
        pending = await getPendingResumeIdsForQuestionBank(userId, taskId)
      } catch (e) {
        this.$message.error('获取待处理简历失败')
        return
      }

      const pendingIds = pending && Array.isArray(pending.pendingResumeIds) ? pending.pendingResumeIds : []
      const sectionKey = pending && pending.sectionKey ? String(pending.sectionKey) : String(this.sectionKey || '')
      const count = pending && pending.count != null ? Number(pending.count) : Number(this.count || 10)

      if (!pendingIds || pendingIds.length === 0) {
        this.$message.success('没有剩余待处理简历')
        this.clearAlgoTaskBarLocalStorage()
        this.algoTaskBar.visible = false
        await this.fetchList()
        return
      }

      this.generating = true
      try {
        const res = await createResumeQuestionBankTask(userId, sectionKey, pendingIds, count)
        const newTaskId = res && res.taskId ? String(res.taskId) : ''
        if (!newTaskId) {
          this.$message.error('创建继续任务失败')
          return
        }

        const startedAt = Date.now()
        this.updateAlgoTaskBarBySnapshot({
          userId,
          taskId: newTaskId,
          total: pendingIds.length,
          startedAt
        })
        this.saveAlgoTaskBarToLocalStorage({
          userId,
          taskId: newTaskId,
          total: pendingIds.length,
          startedAt
        })
        this.$message.success('继续任务已提交，正在生成...')
        this.runAlgoTaskPolling(userId, newTaskId, startedAt)
      } catch (e) {
        this.$message.error('创建继续任务失败')
      } finally {
        this.generating = false
      }
    },
    restoreAlgoTaskBar() {
      const snapshot = this.loadAlgoTaskBarFromLocalStorage()
      const userId = snapshot && snapshot.userId != null ? Number(snapshot.userId) : null
      const taskId = snapshot && snapshot.taskId ? String(snapshot.taskId) : ''
      const startedAt = snapshot && snapshot.startedAt != null ? Number(snapshot.startedAt) : null
      if (!userId || !taskId || !startedAt) {
        return
      }

      this.updateAlgoTaskBarBySnapshot(snapshot)
      if (this.algoTaskBar.visible) {
        this.runAlgoTaskPolling(userId, taskId, startedAt)
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
    getPreviewQuestions(row) {
      const arr = row && Array.isArray(row.previewQuestions) ? row.previewQuestions : []
      return (arr || []).slice(0, 2).map((q) => (q == null ? '' : String(q)))
    },
    async fetchList() {
      const userId = this.getCurrentUserId()
      if (!userId) {
        this.list = []
        this.total = 0
        return
      }
      if (!this.sectionKey) {
        this.list = []
        this.total = 0
        return
      }

      this.loading = true
      try {
        const res = await fetchResumeQuestionBankList(userId, this.sectionKey, {
          pageNo: this.pageNo,
          pageSize: this.pageSize,
          onlyHasFragment: true,
          keyword: (this.searchKeyword || '').trim() || undefined,
          generated: this.generatedFilter || 'all'
        })
        this.list = (res && res.items) ? res.items : []
        this.total = Number(res && res.total != null ? res.total : 0)
        this.$nextTick(() => this.restoreSelection())
      } catch (e) {
        this.list = []
        this.total = 0
        this.$message.error('加载列表失败')
      } finally {
        this.loading = false
      }
    },
    restoreSelection() {
      try {
        const table = this.$refs.table
        if (!table || !table.toggleRowSelection) return
        const set = new Set((this.selectedIds || []).map((v) => Number(v)).filter((v) => v != null && !Number.isNaN(v)))
        ;(this.list || []).forEach((r) => {
          const id = r && r.resumeId != null ? Number(r.resumeId) : null
          if (id == null || Number.isNaN(id)) return
          table.toggleRowSelection(r, set.has(id))
        })
      } catch (e) {
        void e
      }
    },
    onSelectionChange(val) {
      const currentIds = (this.list || []).map((r) => (r && r.resumeId != null ? Number(r.resumeId) : null)).filter((v) => v != null && !Number.isNaN(v))
      const selectedInPage = (val || []).map((r) => (r && r.resumeId != null ? Number(r.resumeId) : null)).filter((v) => v != null && !Number.isNaN(v))
      const currentIdSet = new Set(currentIds)
      const selectedInPageSet = new Set(selectedInPage)

      const kept = (this.selectedIds || []).map((v) => Number(v)).filter((v) => v != null && !Number.isNaN(v) && !currentIdSet.has(v))
      const merged = kept.concat(Array.from(selectedInPageSet))
      const uniq = []
      const seen = new Set()
      merged.forEach((id) => {
        if (seen.has(id)) return
        seen.add(id)
        uniq.push(id)
      })
      this.selectedIds = uniq
    },
    isRowSelectable(row) {
      if (!row) return false
      if (row.hasFragment === false) return false
      return true
    },
    onSearchChange() {
      this.selectedIds = []
      try {
        if (this.$refs.table && this.$refs.table.clearSelection) {
          this.$refs.table.clearSelection()
        }
      } catch (e) {
        void e
      }
      this.pageNo = 1
      this.fetchList()
    },
    onSizeChange(size) {
      const n = Number(size)
      if (!Number.isFinite(n) || n <= 0) return
      this.pageSize = n
      this.pageNo = 1
      this.fetchList()
    },
    onCurrentChange(page) {
      const n = Number(page)
      if (!Number.isFinite(n) || n <= 0) return
      this.pageNo = n
      this.fetchList()
    },
    tableRowStyle() {
      return { height: '64px' }
    },
    tableCellStyle() {
      return { padding: '6px 0' }
    },
    getDetailQuestion(qa) {
      if (!qa) return '-'
      return qa.questionContent || qa.question || qa.problem || '-'
    },
    getDetailAnswer(qa) {
      if (!qa) return '-'
      return qa.answerContent || qa.answer || '-'
    },
    openQaPreview(qa, idx) {
      const q = this.getDetailQuestion(qa)
      const a = this.getDetailAnswer(qa)
      this.previewTitle = `Q${Number(idx || 0) + 1} 详情`
      this.previewContent = `Q: ${q}\n\nA: ${a}`
      this.previewVisible = true
    },
    async openDetail(row) {
      const userId = this.getCurrentUserId()
      if (!userId) {
        this.$message.warning('未找到当前用户信息')
        return
      }
      if (!this.sectionKey) {
        return
      }
      const resumeId = row && row.resumeId != null ? row.resumeId : null
      if (!resumeId) {
        this.$message.warning('未获取到简历ID')
        return
      }

      this.detailTitle = `简历ID: ${resumeId} - ${this.titleText} - 详情`
      this.detailVisible = true
      this.detailLoading = true
      this.detailQuestions = []
      try {
        const res = await fetchResumeQuestionBankDetail(userId, this.sectionKey, resumeId)
        this.detailQuestions = (res && Array.isArray(res.questions)) ? res.questions : []
      } catch (e) {
        this.$message.error('加载详情失败')
      } finally {
        this.detailLoading = false
      }
    },
    async onClickGenerate() {
      const userId = this.getCurrentUserId()
      if (!userId) {
        this.$message.warning('请先选择用户（未找到当前用户信息）')
        return
      }
      if (!this.sectionKey) {
        return
      }
      if (!this.selectedIds || this.selectedIds.length === 0) {
        this.$message.warning('请先勾选要生成题库的简历')
        return
      }
      const resumeIds = (this.selectedIds || []).map((v) => (v != null ? Number(v) : null)).filter((v) => v != null && !Number.isNaN(v))
      if (resumeIds.length === 0) {
        this.$message.warning('未获取到有效简历ID')
        return
      }

      this.generating = true
      try {
        const res = await createResumeQuestionBankTask(userId, this.sectionKey, resumeIds, this.count)
        const taskId = res && res.taskId ? String(res.taskId) : ''
        if (!taskId) {
          this.$message.error('创建任务失败')
          return
        }

        const startedAt = Date.now()
        this.updateAlgoTaskBarBySnapshot({
          userId,
          taskId,
          total: resumeIds.length,
          startedAt
        })
        this.saveAlgoTaskBarToLocalStorage({
          userId,
          taskId,
          total: resumeIds.length,
          startedAt
        })
        this.$message.success('任务已提交，正在生成...')
        this.runAlgoTaskPolling(userId, taskId, startedAt)
      } catch (e) {
        this.$message.error('创建任务失败')
      } finally {
        this.generating = false
      }
    },
    async onClickGenerateOne(row) {
      if (!row || !row.resumeId) {
        return
      }
      if (!this.isRowSelectable(row)) {
        this.$message.warning('该简历缺少对应模块内容，请先解析简历或补充该模块')
        return
      }
      this.selectedIds = [Number(row.resumeId)]
      await this.onClickGenerate()
    }
  }
}
</script>

<style scoped>
.page {
  padding: 16px;
}

.preview {
  padding: 2px 0;
  max-height: 54px;
  overflow: hidden;
}

.task-progress {
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}

.task-progress-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.task-progress-title {
  font-weight: 600;
  color: #303133;
}

.task-progress-meta {
  color: #909399;
  font-size: 12px;
  display: flex;
  gap: 10px;
}

.task-progress-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin: 6px 0 8px;
}

.preview-actions {
  margin-bottom: 6px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tip {
  color: #909399;
  font-size: 12px;
}

.preview-line {
  display: flex;
  gap: 8px;
  margin-bottom: 2px;
}

.preview-idx {
  color: #909399;
  width: 20px;
  flex: 0 0 20px;
}

.preview-text {
  color: #606266;
  word-break: break-word;
  line-height: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-link {
  color: #409eff;
  cursor: pointer;
  user-select: none;
}

.empty {
  text-align: center;
  color: #909399;
  margin-top: 12px;
}

.pager {
  margin-top: 12px;
  text-align: center;
}

.answer {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  color: #606266;
}

.qa-list {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  max-height: 520px;
  overflow: auto;
}

.qa-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
}

.qa-row:last-child {
  border-bottom: none;
}

.qa-left {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.qa-idx {
  color: #303133;
  flex: 0 0 auto;
  line-height: 1.6;
}

.qa-text {
  color: #606266;
  white-space: normal;
  word-break: break-word;
  line-height: 1.6;
  flex: 1;
  min-width: 0;
}

.qa-arrow {
  color: #c0c4cc;
  line-height: 1.6;
  margin-top: 2px;
  flex: 0 0 auto;
}

.preview-body {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  color: #606266;
  max-height: 520px;
  overflow: auto;
}

.dialog-empty {
  color: #909399;
  text-align: center;
  padding: 10px 0;
}

.dialog-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
  color: #606266;
}
</style>
