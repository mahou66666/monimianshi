<template>
  <div class="page">
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索：岗位/关键字"
        clearable
        style="width: 260px"
        @keyup.enter.native="onSearch"
      />

      <el-select v-model="query.status" placeholder="生成状态" clearable style="width: 140px" @change="onSearch">
        <el-option label="未生成" value="none" />
        <el-option label="已生成" value="generated" />
      </el-select>

      <el-button type="primary" :disabled="selectedIds.length === 0" @click="onClickBatchGenerate">批量生成指导建议</el-button>

      <el-popover placement="bottom-start" width="280" trigger="click" append-to-body>
        <el-form size="mini" label-width="120px">
          <el-form-item label="软超时(秒)">
            <el-input-number v-model="algoTaskConfigDraft.softTimeoutSeconds" :min="30" :max="7200" :step="30" controls-position="right" />
          </el-form-item>
          <el-form-item label="最大等待(秒)">
            <el-input-number v-model="algoTaskConfigDraft.maxWaitSeconds" :min="60" :max="21600" :step="60" controls-position="right" />
          </el-form-item>
          <el-form-item>
            <div class="algo-config-actions">
              <el-button size="mini" type="primary" @click="applyAlgoTaskConfig">保存</el-button>
              <el-button size="mini" @click="resetAlgoTaskConfig">恢复默认</el-button>
            </div>
          </el-form-item>
        </el-form>
        <el-button slot="reference" size="mini" icon="el-icon-setting" circle />
      </el-popover>

      <div class="spacer" />

      <el-button @click="onReset">重置</el-button>
    </div>

    <div class="summary-grid">
      <div v-for="card in summaryCards" :key="card.label" class="summary-card">
        <div class="summary-label">{{ card.label }}</div>
        <div class="summary-value">{{ card.value }}</div>
        <div class="summary-sub">{{ card.sub }}</div>
      </div>
    </div>

    <div v-if="algoTaskBar && algoTaskBar.visible" class="task-progress">
      <div class="task-progress-row">
        <div class="task-progress-title">JD指导建议任务</div>
        <div class="task-progress-meta">
          <span>{{ algoTaskBar.statusText }}</span>
          <span v-if="algoTaskBar.total"> {{ algoTaskBar.done }}/{{ algoTaskBar.total }}</span>
          <span v-if="algoTaskBar.elapsedSeconds != null"> {{ algoTaskBar.elapsedSeconds }}s</span>
        </div>
      </div>
      <el-progress :percentage="algoTaskBar.percent" :status="algoTaskBar.progressStatus" :stroke-width="10" />
    </div>

    <el-table
      ref="table"
      v-loading="listLoading"
      :data="list"
      border
      style="width: 100%"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="52" fixed="left" />
      <el-table-column prop="id" label="ID" width="90" fixed="left" />
      <el-table-column prop="jobTitle" label="岗位名称" min-width="180" fixed="left" />
      <el-table-column label="状态" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.currentGuideText ? 'success' : 'info'" size="mini">
            {{ scope.row.currentGuideText ? '已生成' : '待生成' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="JD摘要" min-width="220">
        <template slot-scope="scope">
          <div class="cell">
            <span
              v-if="scope.row.rawText"
              class="cell-link"
              @click.stop="openPreview('JD原文全文', scope.row.rawText)"
            >{{ truncate(scope.row.rawText, 50) }}</span>
            <span v-else>-</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="JD指导建议" min-width="220">
        <template slot-scope="scope">
          <div class="cell">
            <span
              v-if="scope.row.currentGuideText"
              class="cell-link"
              @click.stop="openPreview('JD指导建议全文', scope.row.currentGuideText)"
            >{{ truncate(scope.row.currentGuideText, 50) }}</span>
            <span v-else class="pending-text">待生成</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="140">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="onClickSingleGenerate(scope.row)">生成</el-button>
          <el-button type="text" size="mini" @click="onClearCurrent(scope.row)">清空</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!list || list.length === 0" class="empty">暂无数据</div>

    <el-dialog :title="previewTitle" :visible.sync="previewVisible" width="720px" append-to-body>
      <div class="preview-body">{{ previewContent }}</div>
    </el-dialog>
  </div>
</template>

<script>
import { fetchJdGuideList, clearJdGuide } from '@/api/interviewelf/jd'
import { createAlgoTask, getAlgoTask } from '@/api/interviewelf/algoTask'

export default {
  name: 'JdGuide',
  data() {
    return {
      query: {
        keyword: '',
        status: ''
      },
      list: [],
      listLoading: false,
      selectedIds: [],
      algoTaskConfig: {
        softTimeoutSeconds: 180,
        maxWaitSeconds: 1800
      },
      algoTaskConfigDraft: {
        softTimeoutSeconds: 180,
        maxWaitSeconds: 1800
      },
      algoTaskBar: {
        visible: false,
        taskId: '',
        status: '',
        statusText: '',
        percent: 0,
        progressStatus: null,
        done: 0,
        total: 0,
        elapsedSeconds: 0,
        startedAt: 0,
        userId: null
      },
      algoTaskPollTimer: null,
      algoTaskPollToken: 0,
      previewVisible: false,
      previewTitle: '',
      previewContent: ''
    }
  },
  computed: {
    generatedGuideCount() {
      return (this.list || []).filter((row) => {
        const text = row && row.currentGuideText != null ? String(row.currentGuideText).trim() : ''
        return !!text
      }).length
    },
    pendingGuideCount() {
      return Math.max(0, (this.list || []).length - this.generatedGuideCount)
    },
    summaryCards() {
      return [
        {
          label: '当前 JD',
          value: this.list.length,
          sub: '当前筛选结果'
        },
        {
          label: '已生成建议',
          value: this.generatedGuideCount,
          sub: '可直接预览全文'
        },
        {
          label: '待生成',
          value: this.pendingGuideCount,
          sub: '适合批量跑任务'
        },
        {
          label: '当前选中',
          value: this.selectedIds.length,
          sub: '支持批量生成'
        }
      ]
    }
  },
  created() {
    this.loadAlgoTaskConfig()
    this.loadList()
    this.restoreAlgoTaskBar()
  },
  beforeDestroy() {
    this.stopAlgoTaskPolling()
  },
  methods: {
    getDefaultAlgoTaskConfig() {
      return {
        softTimeoutSeconds: 180,
        maxWaitSeconds: 1800
      }
    },
    loadAlgoTaskConfig() {
      try {
        const raw = window.localStorage.getItem('JdGuidePage-AlgoTaskConfig')
        const cfg = raw ? JSON.parse(raw) : null
        const s = cfg && cfg.softTimeoutSeconds != null ? Number(cfg.softTimeoutSeconds) : 180
        const m = cfg && cfg.maxWaitSeconds != null ? Number(cfg.maxWaitSeconds) : 1800
        if (Number.isFinite(s) && s > 0) this.algoTaskConfig.softTimeoutSeconds = s
        if (Number.isFinite(m) && m > 0) this.algoTaskConfig.maxWaitSeconds = m
      } catch (e) {
        this.$message.warning('读取本地配置失败，将使用默认配置')
      }

      this.algoTaskConfigDraft = {
        softTimeoutSeconds: this.algoTaskConfig.softTimeoutSeconds,
        maxWaitSeconds: this.algoTaskConfig.maxWaitSeconds
      }
    },
    applyAlgoTaskConfig() {
      const softTimeoutSeconds = Number(this.algoTaskConfigDraft.softTimeoutSeconds)
      const maxWaitSeconds = Number(this.algoTaskConfigDraft.maxWaitSeconds)

      if (!Number.isFinite(softTimeoutSeconds) || softTimeoutSeconds <= 0) {
        this.$message.error('软超时必须为正数')
        return
      }
      if (!Number.isFinite(maxWaitSeconds) || maxWaitSeconds <= 0) {
        this.$message.error('最大等待必须为正数')
        return
      }
      if (maxWaitSeconds < softTimeoutSeconds) {
        this.$message.error('最大等待必须大于等于软超时')
        return
      }

      this.algoTaskConfig = { softTimeoutSeconds, maxWaitSeconds }
      try {
        window.localStorage.setItem('JdGuidePage-AlgoTaskConfig', JSON.stringify(this.algoTaskConfig))
      } catch (e) {
        this.$message.warning('保存配置失败')
      }
      this.$message.success('配置已保存')
    },
    resetAlgoTaskConfig() {
      const d = this.getDefaultAlgoTaskConfig()
      this.algoTaskConfig = { softTimeoutSeconds: d.softTimeoutSeconds, maxWaitSeconds: d.maxWaitSeconds }
      this.algoTaskConfigDraft = { softTimeoutSeconds: d.softTimeoutSeconds, maxWaitSeconds: d.maxWaitSeconds }
      try {
        window.localStorage.setItem('JdGuidePage-AlgoTaskConfig', JSON.stringify(this.algoTaskConfig))
      } catch (e) {
        this.$message.warning('保存配置失败')
        return
      }
      this.$message.success('已恢复默认配置')
    },
    getAlgoTaskBarStorageKey() {
      return 'JdGuidePage-ActiveAlgoTask'
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
      const progress = task && task.progress != null ? Number(task.progress) : null
      const processedCount = task && task.processedCount != null ? Number(task.processedCount) : null
      const result = task && task.result ? task.result : null
      const request = task && task.request ? task.request : null
      const resultTotal = result && result.total != null ? Number(result.total) : null
      const requestTotal = request && request.options && Array.isArray(request.options.jdIds) ? request.options.jdIds.length : null
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
        const failCount = result && result.failedCount != null ? Number(result.failedCount) : null
        if (successCount != null && failCount != null && !Number.isNaN(successCount) && !Number.isNaN(failCount)) {
          done = Math.max(0, successCount + failCount)
        } else if (processedCount != null && !Number.isNaN(processedCount)) {
          done = Math.max(0, Math.floor(processedCount))
        } else if (status === 'SUCCESS' && total > 0) {
          done = total
        }
      } else if (processedCount != null && !Number.isNaN(processedCount)) {
        done = Math.min(total, Math.max(0, Math.floor(processedCount)))
      }

      let percent = 0
      if (progress != null && !Number.isNaN(progress) && progress > 0) {
        percent = Math.min(100, Math.max(0, Math.floor(progress)))
      } else if (total > 0) {
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
      const softTimeoutMs = Math.max(0, Number(this.algoTaskConfig.softTimeoutSeconds || 0) * 1000)
      const maxWaitMs = Math.max(0, Number(this.algoTaskConfig.maxWaitSeconds || 0) * 1000)
      let softTimeoutNotified = false

      const loop = async() => {
        if (token !== this.algoTaskPollToken) return
        if (!this.algoTaskBar.visible || String(this.algoTaskBar.taskId || '') !== String(taskId || '')) return

        let t
        try {
          t = await getAlgoTask(userId, taskId)
        } catch (e) {
          this.algoTaskPollTimer = setTimeout(loop, 2000)
          return
        }

        this.applyAlgoTaskBarFromTask(t)

        const status = t && t.status ? String(t.status) : ''
        const elapsedMs = Date.now() - startedAt
        if (!softTimeoutNotified && softTimeoutMs > 0 && elapsedMs >= softTimeoutMs) {
          softTimeoutNotified = true
          this.$message.warning('任务执行时间较长，仍在继续处理中，请耐心等待')
        }

        if (status === 'SUCCESS') {
          this.clearAlgoTaskBarLocalStorage()
          this.stopAlgoTaskPolling()
          this.algoTaskBar.visible = false
          this.handleTaskFinishMessage(t, true)
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

        const intervalMs = (softTimeoutMs > 0 && elapsedMs >= softTimeoutMs) ? 3000 : 1000
        if (maxWaitMs > 0 && elapsedMs >= maxWaitMs) {
          this.algoTaskPollTimer = setTimeout(loop, 5000)
          return
        }
        this.algoTaskPollTimer = setTimeout(loop, intervalMs)
      }

      await loop()
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
        const id = user && user.id != null ? Number(user.id) : 0
        return Number.isFinite(id) ? id : 0
      } catch (e) {
        return 0
      }
    },
    async loadList() {
      this.listLoading = true
      try {
        const data = await fetchJdGuideList({ pageNo: 1, pageSize: 50, keyword: this.query.keyword || '' })
        const items = data && data.items ? data.items : []
        let rows = Array.isArray(items) ? items : []
        const status = String(this.query.status || '')
        if (status === 'none') {
          rows = rows.filter((r) => {
            const t = r && r.currentGuideText != null ? String(r.currentGuideText).trim() : ''
            return !t
          })
        } else if (status === 'generated') {
          rows = rows.filter((r) => {
            const t = r && r.currentGuideText != null ? String(r.currentGuideText).trim() : ''
            return !!t
          })
        }
        this.list = rows
      } catch (e) {
        this.list = []
      } finally {
        this.listLoading = false
      }
    },
    onSearch() {
      if (this.$refs.table && this.$refs.table.clearSelection) {
        this.$refs.table.clearSelection()
      }
      this.selectedIds = []
      this.loadList()
    },
    onReset() {
      this.query = { keyword: '', status: '' }
      if (this.$refs.table && this.$refs.table.clearSelection) {
        this.$refs.table.clearSelection()
      }
      this.selectedIds = []
      this.loadList()
    },
    onSelectionChange(rows) {
      this.selectedIds = (rows || []).map((r) => r && r.id).filter((id) => id != null)
    },
    truncate(text, maxLen = 48) {
      const raw = String(text || '')
      if (!raw) return ''
      if (raw.length > maxLen) return raw.slice(0, maxLen) + '...'
      return raw
    },
    openPreview(title, content) {
      this.previewTitle = title || '内容预览'
      this.previewContent = content == null ? '' : String(content)
      this.previewVisible = true
    },
    handleTaskFinishMessage(task, successReload) {
      const result = task && task.result ? task.result : null
      const successCount = result && result.successCount != null ? Number(result.successCount) : null
      const failedCount = result && result.failedCount != null ? Number(result.failedCount) : null
      const items = result && Array.isArray(result.items) ? result.items : []
      const firstFail = items.find((it) => it && (it.status === 'fail' || it.status === 'failed'))
      const firstFailMsg = firstFail && firstFail.errorMessage ? String(firstFail.errorMessage) : ''

      if (failedCount != null && failedCount > 0) {
        const s = successCount != null ? successCount : 0
        const f = failedCount
        const msg = firstFailMsg ? `生成完成：成功 ${s}，失败 ${f}，原因：${firstFailMsg}` : `生成完成：成功 ${s}，失败 ${f}`
        if (s > 0) {
          this.$message.warning(msg)
        } else {
          this.$message.error(msg)
        }
        if (successReload) {
          this.loadList()
        }
        return
      }

      this.$message.success('生成成功')
      if (successReload) {
        this.loadList()
      }
    },
    async onClickBatchGenerate() {
      const userId = this.getCurrentUserId()
      if (!userId) {
        this.$message.error('当前登录用户信息缺失，请重新登录')
        return
      }
      const jdIds = (this.selectedIds || []).filter((id) => id != null)
      if (!jdIds || jdIds.length === 0) {
        this.$message.warning('请先选择JD')
        return
      }

      try {
        const req = { options: { jdIds }}
        const data = await createAlgoTask(userId, 'jd_guide', req)
        const taskId = data && data.taskId ? data.taskId : ''
        if (!taskId) {
          this.$message.success('已提交任务')
          return
        }

        this.$message.success(`已提交任务：${taskId}`)
        const startedAt = Date.now()
        this.updateAlgoTaskBarBySnapshot({ userId, taskId, total: jdIds.length, startedAt })
        this.saveAlgoTaskBarToLocalStorage({ userId, taskId, total: jdIds.length, startedAt })
        await this.runAlgoTaskPolling(userId, taskId, startedAt)
      } catch (e) {
        this.stopAlgoTaskPolling()
        this.algoTaskBar.visible = false
        this.$message.error('提交任务失败')
      }
    },
    async onClickSingleGenerate(row) {
      const userId = this.getCurrentUserId()
      if (!userId) {
        this.$message.error('当前登录用户信息缺失，请重新登录')
        return
      }
      const jdId = row && row.id != null ? row.id : null
      if (!jdId) {
        this.$message.warning('JD不存在')
        return
      }

      try {
        const req = { options: { jdId }}
        const data = await createAlgoTask(userId, 'jd_guide', req)
        const taskId = data && data.taskId ? data.taskId : ''
        if (!taskId) {
          this.$message.success('已提交任务')
          return
        }

        this.$message.success(`已提交任务：${taskId}`)
        const startedAt = Date.now()
        this.updateAlgoTaskBarBySnapshot({ userId, taskId, total: 1, startedAt })
        this.saveAlgoTaskBarToLocalStorage({ userId, taskId, total: 1, startedAt })
        await this.runAlgoTaskPolling(userId, taskId, startedAt)
      } catch (e) {
        this.stopAlgoTaskPolling()
        this.algoTaskBar.visible = false
        this.$message.error('提交任务失败')
      }
    },
    async onClearCurrent(row) {
      const jdId = row && row.id != null ? row.id : null
      if (!jdId) {
        this.$message.warning('JD不存在')
        return
      }

      try {
        await clearJdGuide(jdId)
        this.$message.success('已清空')
        this.loadList()
      } catch (e) {
        this.$message.error('清空失败')
      }
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
  gap: 12px;
  margin-bottom: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.summary-card {
  padding: 16px 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
  border: 1px solid #e8eef6;
  border-radius: 14px;
  box-shadow: 0 8px 18px rgba(31, 45, 61, 0.05);
}

.summary-label {
  font-size: 12px;
  color: #8a94a6;
  letter-spacing: 0.5px;
}

.summary-value {
  margin-top: 8px;
  font-size: 28px;
  line-height: 1;
  font-weight: 700;
  color: #22314f;
}

.summary-sub {
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
}

.divider {
  width: 1px;
  height: 24px;
  background: #dcdfe6;
  margin: 0 4px;
}

.spacer {
  flex: 1;
}

.task-progress {
  margin: 12px 0;
  padding: 14px 16px;
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.08) 0%, rgba(103, 194, 58, 0.06) 100%);
  border: 1px solid #dbe8f8;
  border-radius: 14px;
}

 .task-progress-row {
   display: flex;
   justify-content: space-between;
   align-items: center;
   margin-bottom: 8px;
 }

.task-progress-title {
  font-weight: 600;
  color: #22314f;
}

 .task-progress-meta span {
   margin-left: 8px;
   color: #606266;
 }

.cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary {
  color: #606266;
  line-height: 18px;
}

.pending-text {
  color: #909399;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.cell-link {
  color: #409eff;
  cursor: pointer;
}

.cell-link:hover {
  text-decoration: underline;
}

.empty {
  text-align: center;
  color: #909399;
  margin-top: 12px;
}

.preview-body {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  max-height: 500px;
  overflow-y: auto;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
