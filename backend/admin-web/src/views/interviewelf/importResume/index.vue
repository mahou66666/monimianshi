<template>
  <div class="page">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-upload
          v-if="canManageResume"
          ref="upload"
          action=""
          :show-file-list="false"
          :auto-upload="false"
          :multiple="true"
          accept=".pdf,application/pdf"
          :http-request="dummyRequest"
          :on-change="onFileChange"
        >
          <el-button type="primary">上传PDF</el-button>
        </el-upload>
        <el-button v-if="canManageResume" :disabled="!selectedIds || selectedIds.length === 0" :loading="parsing" @click="onClickBatchExtract">批量提取</el-button>
        <el-popover v-if="canManageResume" placement="bottom-start" width="280" trigger="click" append-to-body>
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
      </div>

      <div class="toolbar-right">
        <el-input
          v-model="keyword"
          placeholder="搜索简历名"
          size="small"
          clearable
          style="width: 220px"
          @keyup.enter.native="onClickSearch"
        />
        <el-button size="small" @click="onClickSearch">搜索</el-button>
      </div>
    </div>

    <resume-user-banner
      :title="activeUserTitle"
      :description="activeUserDesc"
      :show-back-button="isViewingOtherUser"
      @switch-current-user="switchToCurrentUser"
    />

    <div class="summary-grid">
      <div v-for="card in summaryCards" :key="card.label" class="summary-card">
        <div class="summary-label">{{ card.label }}</div>
        <div class="summary-value">{{ card.value }}</div>
        <div class="summary-sub">{{ card.sub }}</div>
      </div>
    </div>

    <resume-task-progress
      :bar="algoTaskBar"
      :can-manage-resume="canManageResume"
      @cancel="onClickCancelAlgoTask"
      @continue="onClickContinueAlgoTask"
    />

    <el-alert
      v-if="fileTip"
      class="tip"
      :title="fileTip"
      type="warning"
      show-icon
      :closable="false"
    />

    <el-table
      ref="table"
      :data="rows"
      border
      style="width: 100%"
      :row-key="getRowKey"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="50" :reserve-selection="true" :selectable="isRowSelectable" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="简历名称" min-width="180">
        <template slot-scope="scope">
          <span>{{ scope.row.name ? truncate(scope.row.name) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="地址" min-width="220">
        <template slot-scope="scope">
          <resume-storage-status
            :storage-path="scope.row.storagePath"
            :url="scope.row.url"
            :can-parse="scope.row.canParse"
            :file-check-message="scope.row.fileCheckMessage"
            @preview="openPreview"
          />
        </template>
      </el-table-column>
      <el-table-column prop="project" label="项目" min-width="160">
        <template slot-scope="scope">
          <span
            v-if="scope.row.project"
            class="cell-link"
            @click.stop="openPreview('项目经历', scope.row.projectFull || scope.row.project)"
          >{{ truncate(scope.row.project) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="internship" label="实习" min-width="160">
        <template slot-scope="scope">
          <span
            v-if="scope.row.internship"
            class="cell-link"
            @click.stop="openPreview('实习经历', scope.row.internshipFull || scope.row.internship)"
          >{{ truncate(scope.row.internship) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="education" label="教育情况" min-width="160">
        <template slot-scope="scope">
          <span
            v-if="scope.row.education"
            class="cell-link"
            @click.stop="openPreview('教育经历', scope.row.educationFull || scope.row.education)"
          >{{ truncate(scope.row.education) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template slot-scope="scope">
          <el-button
            type="text"
            size="mini"
            class="op-btn"
            @click.stop="onClickDownloadRow(scope.row)"
          >下载</el-button>
          <el-button
            v-if="canViewResume"
            type="text"
            size="mini"
            class="op-btn"
            @click.stop="onClickViewResult(scope.row)"
          >结果</el-button>
          <el-button
            v-if="canManageResume"
            type="text"
            size="mini"
            class="op-btn"
            @click.stop="onClickEditRow(scope.row)"
          >编辑</el-button>
          <el-button
            v-if="canManageResume"
            type="text"
            size="mini"
            class="op-btn danger-text"
            :disabled="scope.row && scope.row.__deleting"
            @click.stop="onClickDeleteRow(scope.row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <resume-upload-dialog
      :visible.sync="confirmVisible"
      :can-manage-resume="canManageResume"
      :importing="importing"
      :upload-stats="uploadStats"
      :upload-stats-text="uploadStatsText"
      :upload-items="uploadItems"
      :get-upload-item-status-text="getUploadItemStatusText"
      @download-duplicate="onClickDownloadDuplicate"
      @remove-status="removeUploadItemsByStatus"
      @clear="clearUploadItems"
      @cancel="onCancelConfirm"
      @submit="submitUpload"
    />

    <resume-preview-dialog
      :visible.sync="previewVisible"
      :title="previewTitle"
      :content="previewContent"
    />

    <resume-result-dialog
      :visible.sync="resultVisible"
      :loading="resultLoading"
      :title="resultDialogTitle"
      :html="resultHtml"
    />

    <resume-edit-dialog
      :visible.sync="editVisible"
      :loading="editLoading"
      :saving="editSaving"
      :can-manage-resume="canManageResume"
      :title-value="editForm.title"
      :content-value="editForm.content"
      @update:title="updateEditTitle"
      @update:content="updateEditContent"
      @cancel="onCancelEdit"
      @submit="onSubmitEdit"
    />

    <div v-if="rows.length === 0" class="empty">暂无数据</div>

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
  </div>
</template>
<script>
import axios from 'axios'
import SparkMD5 from 'spark-md5'
import { getToken } from '@/utils/auth'
import ResumeResultDialog from './components/ResumeResultDialog'
import ResumePreviewDialog from './components/ResumePreviewDialog'
import ResumeEditDialog from './components/ResumeEditDialog'
import ResumeStorageStatus from './components/ResumeStorageStatus'
import ResumeTaskProgress from './components/ResumeTaskProgress'
import ResumeUploadDialog from './components/ResumeUploadDialog'
import ResumeUserBanner from './components/ResumeUserBanner'
import {
  cancelAlgoTask,
  createAlgoTask,
  deleteResume,
  fetchImportedResumes,
  fetchResumeContent,
  fetchResumeContentBatch,
  getAlgoTask,
  getPendingResumeFileIds,
  importPdfBatch,
  precheckImportPdfMd5Batch,
  updateResume
} from '@/api/interviewelf/resumeImport'

export default {
  name: 'ImportResume',
  components: {
    ResumeResultDialog,
    ResumePreviewDialog,
    ResumeEditDialog,
    ResumeStorageStatus,
    ResumeTaskProgress,
    ResumeUploadDialog,
    ResumeUserBanner
  },
  data() {
    return {
      rows: [],
      keyword: '',
      targetUserId: null,
      targetUserName: '',
      pageNo: 1,
      pageSize: 100,
      total: 0,
      fileTip: '',
      fileTipTimer: null,
      nextId: 1,
      uploadItems: [],
      confirmVisible: false,
      previewVisible: false,
      previewTitle: '',
      previewContent: '',
      resultVisible: false,
      resultLoading: false,
      resultDialogTitle: '查看提取结果',
      resultHtml: '',
      selectedIds: [],
      selectedRowMap: {},
      importing: false,
      parsing: false,
      loadingImported: false,
      loadingContents: false,
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
        pausedAt: 0,
        elapsedSecondsWhenPaused: 0,
        startedAt: 0,
        userId: null,
        pendingResumeFileIds: []
      },
      algoTaskPollTimer: null,
      algoTaskPollToken: 0,
      algoTaskConfig: {
        softTimeoutSeconds: 180,
        maxWaitSeconds: 1800
      },
      algoTaskConfigDraft: {
        softTimeoutSeconds: 180,
        maxWaitSeconds: 1800
      },
      editVisible: false,
      editLoading: false,
      editSaving: false,
      editForm: {
        resumeId: null,
        title: '',
        content: ''
      }
    }
  },
  computed: {
    uploadStats() {
      const items = this.uploadItems || []
      const total = items.length
      const duplicate = items.filter((it) => it && it.status === 'duplicate').length
      const success = items.filter((it) => it && it.status === 'success').length
      const fail = items.filter((it) => it && it.status === 'fail').length
      const uploading = items.filter((it) => it && it.status === 'uploading').length
      const hashing = items.filter((it) => it && it.status === 'hashing').length
      const retrying = items.filter((it) => it && it.status === 'retrying').length
      const pending = Math.max(0, total - duplicate - success - uploading - hashing - retrying)
      return { total, duplicate, success, fail, uploading, hashing, retrying, pending }
    },
    uploadStatsText() {
      const s = this.uploadStats
      const base = `共 ${s.total} 份：待处理 ${s.pending}，上传中 ${s.uploading}，排队重试 ${s.retrying}，成功 ${s.success}，重复 ${s.duplicate}，失败 ${s.fail}`
      if (s.duplicate > 0) {
        return `${base}（重复文件已按 MD5 自动跳过）`
      }
      return base
    },
    activeUserTip() {
      const userId = this.getActiveUserId()
      if (!userId) {
        return '未找到当前登录用户，请重新登录。'
      }
      const userName = this.activeUserDisplayName ? `（${this.activeUserDisplayName}）` : ''
      return `当前查看用户：${userId}${userName}`
    },
    currentUserName() {
      try {
        const raw = window.localStorage.getItem('InterviewElf-User')
        const user = raw ? JSON.parse(raw) : null
        return user && user.userName ? String(user.userName) : ''
      } catch (e) {
        return ''
      }
    },
    activeUserDisplayName() {
      return this.targetUserName || this.currentUserName || ''
    },
    isViewingOtherUser() {
      const currentUserId = this.getCurrentUserId()
      const activeUserId = this.getActiveUserId()
      return !!currentUserId && !!activeUserId && currentUserId !== activeUserId
    },
    activeUserTitle() {
      return this.activeUserTip
    },
    activeUserDesc() {
      if (!this.getActiveUserId()) {
        return '当前页面无法确定用户身份，请退出后重新登录。'
      }
      if (this.isViewingOtherUser) {
        return '你现在看到的是指定用户的简历列表。当前账号具备跨用户查看权限，所以可以从“用户管理”跳转过来看。'
      }
      return '这里默认只显示当前登录账号自己的简历。如果列表为空，可以先上传 PDF，或者从“用户管理”里点击某个用户的“简历”进入查看。'
    },
    permissionCodes() {
      const codes = this.$store && this.$store.getters ? this.$store.getters.permission_codes : []
      return Array.isArray(codes) ? codes : []
    },
    userRoles() {
      const roles = this.$store && this.$store.getters ? this.$store.getters.roles : []
      return Array.isArray(roles) ? roles : []
    },
    canViewResume() {
      return this.userRoles.includes('admin') || this.permissionCodes.includes('resume:view') || this.permissionCodes.includes('resume:manage')
    },
    canManageResume() {
      return this.userRoles.includes('admin') || this.permissionCodes.includes('resume:manage')
    },
    parseableRowCount() {
      return (this.rows || []).filter((row) => row && row.canParse).length
    },
    missingFileCount() {
      return (this.rows || []).filter((row) => row && row.canParse === false).length
    },
    summaryCards() {
      const activeUserId = this.getActiveUserId()
      return [
        {
          label: '当前用户',
          value: activeUserId || '-',
          sub: this.activeUserDisplayName || '未识别用户名'
        },
        {
          label: '简历总数',
          value: this.total,
          sub: `当前页展示 ${this.rows.length} 条`
        },
        {
          label: '本页可提取',
          value: this.parseableRowCount,
          sub: '存在本地 PDF 文件'
        },
        {
          label: '文件缺失',
          value: this.missingFileCount,
          sub: '仅保留记录，不能提取'
        }
      ]
    }
  },
  watch: {
    '$route.query.userId': function() {
      this.initTargetUserFromRoute()
      this.selectedIds = []
      this.selectedRowMap = {}
      this.pageNo = 1
      this.refreshImported()
      this.restoreAlgoTaskBar()
    }
  },
  created() {
    this.initTargetUserFromRoute()
    this.loadAlgoTaskConfig()
    this.clearLegacyAlgoTaskBarLocalStorage()
    this.refreshImported()
    this.restoreAlgoTaskBar()
  },
  beforeDestroy() {
    if (this.fileTipTimer) {
      clearTimeout(this.fileTipTimer)
      this.fileTipTimer = null
    }
    this.stopAlgoTaskPolling()
  },
  methods: {
    switchToCurrentUser() {
      this.$router.push({ path: '/interviewelf/importResume' })
    },
    ensureResumeManage(actionText) {
      if (this.canManageResume) return true
      this.$message.warning(actionText || '当前账号无简历管理权限')
      return false
    },
    dummyRequest() {
      return Promise.resolve()
    },
    getDefaultAlgoTaskConfig() {
      return {
        softTimeoutSeconds: 180,
        maxWaitSeconds: 1800
      }
    },
    loadAlgoTaskConfig() {
      try {
        const raw = window.localStorage.getItem('UploadResumePage-AlgoTaskConfig')
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
      if (!this.ensureResumeManage('当前账号无保存任务配置权限')) return
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
        window.localStorage.setItem('UploadResumePage-AlgoTaskConfig', JSON.stringify(this.algoTaskConfig))
      } catch (e) {
        this.$message.warning('保存配置失败')
      }
      this.$message.success('配置已保存')
    },
    resetAlgoTaskConfig() {
      if (!this.ensureResumeManage('当前账号无重置任务配置权限')) return
      const d = this.getDefaultAlgoTaskConfig()
      this.algoTaskConfig = { softTimeoutSeconds: d.softTimeoutSeconds, maxWaitSeconds: d.maxWaitSeconds }
      this.algoTaskConfigDraft = { softTimeoutSeconds: d.softTimeoutSeconds, maxWaitSeconds: d.maxWaitSeconds }
      try {
        window.localStorage.setItem('UploadResumePage-AlgoTaskConfig', JSON.stringify(this.algoTaskConfig))
      } catch (e) {
        this.$message.warning('保存配置失败')
        return
      }
      this.$message.success('已恢复默认配置')
    },
    getAlgoTaskBarStorageKey() {
      return 'ImportResumePage-ActiveAlgoTask-v2'
    },
    clearLegacyAlgoTaskBarLocalStorage() {
      try {
        window.localStorage.removeItem('ImportResumePage-ActiveAlgoTask')
      } catch (e) {
        return
      }
    },
    saveAlgoTaskBarToLocalStorage(payload) {
      try {
        window.localStorage.setItem(this.getAlgoTaskBarStorageKey(), JSON.stringify(payload || {}))
      } catch (e) {
        return
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
        window.localStorage.removeItem('ImportResumePage-ActiveAlgoTask')
      } catch (e) {
        return
      }
    },
    clearStaleAlgoTaskBar() {
      this.clearAlgoTaskBarLocalStorage()
      this.stopAlgoTaskPolling()
      this.algoTaskBar.visible = false
      this.algoTaskBar.taskId = ''
      this.algoTaskBar.status = ''
      this.algoTaskBar.statusText = ''
      this.algoTaskBar.percent = 0
      this.algoTaskBar.done = 0
      this.algoTaskBar.total = 0
      this.algoTaskBar.pendingResumeFileIds = []
    },
    isTaskNotFoundError(error) {
      const msg = error && error.message ? String(error.message) : ''
      return msg.includes('Task not found')
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
      const pendingResumeFileIds = snapshot && Array.isArray(snapshot.pendingResumeFileIds) ? snapshot.pendingResumeFileIds : []
      const pausedAt = snapshot && snapshot.pausedAt != null ? Number(snapshot.pausedAt) : 0
      const elapsedSecondsWhenPaused = snapshot && snapshot.elapsedSecondsWhenPaused != null ? Number(snapshot.elapsedSecondsWhenPaused) : 0

      this.algoTaskBar.visible = Boolean(taskId)
      this.algoTaskBar.userId = userId
      this.algoTaskBar.taskId = taskId
      this.algoTaskBar.total = Number.isFinite(total) && total > 0 ? total : 0
      this.algoTaskBar.startedAt = Number.isFinite(startedAt) && startedAt > 0 ? startedAt : Date.now()
      this.algoTaskBar.pendingResumeFileIds = Array.isArray(pendingResumeFileIds) ? pendingResumeFileIds : []
      this.algoTaskBar.pausedAt = Number.isFinite(pausedAt) && pausedAt > 0 ? pausedAt : 0
      this.algoTaskBar.elapsedSecondsWhenPaused = Number.isFinite(elapsedSecondsWhenPaused) && elapsedSecondsWhenPaused > 0 ? elapsedSecondsWhenPaused : 0
    },
    applyAlgoTaskBarFromTask(task) {
      const status = task && task.status ? String(task.status) : ''
      const processedCount = task && task.processedCount != null ? Number(task.processedCount) : null
      const result = task && task.result ? task.result : null
      const request = task && task.request ? task.request : null
      const resultTotal = result && result.total != null ? Number(result.total) : null
      const requestTotal = request && Array.isArray(request.resumeFileIds) ? request.resumeFileIds.length : null
      const total = this.algoTaskBar.total || (Number.isFinite(resultTotal) && resultTotal > 0 ? resultTotal : 0) || (Number.isFinite(requestTotal) && requestTotal > 0 ? requestTotal : 0)
      const startedAt = this.algoTaskBar.startedAt || Date.now()

      let statusText = '排队中'
      if (status === 'RUNNING') statusText = '处理中'
      if (status === 'SUCCESS') statusText = '成功'
      if (status === 'FAILED') statusText = '失败'
      if (status === 'CANCELED') statusText = '已取消'

      let progressStatus = null
      if (status === 'SUCCESS') progressStatus = 'success'
      if (status === 'FAILED') progressStatus = 'exception'
      if (status === 'CANCELED') progressStatus = 'warning'

      let done = 0
      if (status === 'SUCCESS' || status === 'FAILED' || status === 'CANCELED') {
        const successCount = result && result.successCount != null ? Number(result.successCount) : null
        const failCount = result && result.failCount != null ? Number(result.failCount) : null
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

      if (status === 'CANCELED') {
        const pa = this.algoTaskBar.pausedAt && this.algoTaskBar.pausedAt > 0 ? Number(this.algoTaskBar.pausedAt) : 0
        const frozen = this.algoTaskBar.elapsedSecondsWhenPaused && this.algoTaskBar.elapsedSecondsWhenPaused > 0
          ? Number(this.algoTaskBar.elapsedSecondsWhenPaused)
          : 0
        if (frozen > 0) {
          this.algoTaskBar.elapsedSeconds = frozen
        } else if (pa > 0) {
          this.algoTaskBar.elapsedSeconds = Math.max(0, Math.floor((pa - startedAt) / 1000))
        } else {
          this.algoTaskBar.elapsedSeconds = Math.max(0, Math.floor((Date.now() - startedAt) / 1000))
        }
      } else {
        this.algoTaskBar.elapsedSeconds = Math.max(0, Math.floor((Date.now() - startedAt) / 1000))
        this.algoTaskBar.pausedAt = 0
        this.algoTaskBar.elapsedSecondsWhenPaused = 0
      }
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
          if (this.isTaskNotFoundError(e)) {
            this.clearStaleAlgoTaskBar()
            return
          }
          this.algoTaskPollTimer = setTimeout(loop, 2000)
          return
        }

        if (t && t.code != null && t.code !== 20000) {
          const code = Number(t.code)
          if (code === 40400) {
            this.clearStaleAlgoTaskBar()
            return
          }
          this.algoTaskPollTimer = setTimeout(loop, 2000)
          return
        }

        if (t && t.data != null && t.status == null) {
          t = t.data
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
          await this.fillSummariesFromResumeContent()
          const result = t && t.result ? t.result : {}
          const successCount = result && result.successCount != null ? result.successCount : 0
          const failCount = result && result.failCount != null ? result.failCount : 0

          const retryableIds = this.getRetryableResumeFileIdsFromAlgoTask(t)
          const retryableCount = retryableIds.length
          if (failCount > 0 && retryableCount > 0) {
            this.$message.warning(`Parse completed: success ${successCount}, failed ${failCount}, retryable ${retryableCount}`)
            try {
              await this.$confirm(`There are ${retryableCount} retryable failed resumes. Retry now?`, 'Parse Completed', {
                confirmButtonText: 'Retry All',
                cancelButtonText: 'Close',
                type: 'warning'
              })
            } catch (e) {
              return
            }
            await this.retryResumeParseByResumeFileIds(retryableIds)
            return
          }

          if (failCount > 0) {
            const firstHint = this.getFirstFailureHintFromAlgoTask(t)
            const suffix = firstHint ? `，例如：${firstHint}` : ''
            this.$message.warning(`批量提取完成：成功 ${successCount} 份，失败 ${failCount} 份${suffix}`)
          } else {
            this.$message.success(`批量提取完成：成功 ${successCount} 份，失败 ${failCount} 份`)
          }
          return
        }
        if (status === 'FAILED') {
          this.clearAlgoTaskBarLocalStorage()
          this.stopAlgoTaskPolling()
          this.algoTaskBar.visible = false
          const msg = t && t.errorMessage ? String(t.errorMessage) : '批量提取失败'
          this.$message.error(msg)
          return
        }

        if (status === 'CANCELED') {
          this.stopAlgoTaskPolling()

          let pendingIds = null
          try {
            const data = await getPendingResumeFileIds(userId, taskId)
            const list = data && Array.isArray(data.pendingResumeFileIds) ? data.pendingResumeFileIds : []
            pendingIds = Array.from(new Set(list.map((v) => (v != null ? Number(v) : null)).filter((v) => v != null && !Number.isNaN(v))))
          } catch (e) {
            pendingIds = null
          }

          const existingPausedAt = this.algoTaskBar.pausedAt && this.algoTaskBar.pausedAt > 0 ? Number(this.algoTaskBar.pausedAt) : 0
          const existingFrozen = this.algoTaskBar.elapsedSecondsWhenPaused && this.algoTaskBar.elapsedSecondsWhenPaused > 0
            ? Number(this.algoTaskBar.elapsedSecondsWhenPaused)
            : 0

          const pauseEffectiveAt = existingPausedAt > 0 ? existingPausedAt : Date.now()
          const frozenSeconds = existingFrozen > 0
            ? existingFrozen
            : Math.max(0, Math.floor((pauseEffectiveAt - startedAt) / 1000))

          this.algoTaskBar.pausedAt = pauseEffectiveAt
          this.algoTaskBar.elapsedSecondsWhenPaused = frozenSeconds
          this.algoTaskBar.elapsedSeconds = frozenSeconds

          if (Array.isArray(pendingIds)) {
            this.algoTaskBar.pendingResumeFileIds = pendingIds

            if (pendingIds.length === 0) {
              this.clearAlgoTaskBarLocalStorage()
              this.algoTaskBar.visible = false
              await this.fillSummariesFromResumeContent()
              this.$message.info('Task finished. No pending resumes to continue.')
              return
            }

            this.saveAlgoTaskBarToLocalStorage({
              userId,
              taskId,
              total: this.algoTaskBar.total,
              startedAt,
              pendingResumeFileIds: pendingIds,
              pausedAt: this.algoTaskBar.pausedAt,
              elapsedSecondsWhenPaused: this.algoTaskBar.elapsedSecondsWhenPaused
            })
            if (pendingIds.length > 0) {
              this.$message.info(`任务已暂停，剩余 ${pendingIds.length} 份可继续`)
            } else {
              this.$message.info('Task paused')
            }
          } else {
            this.saveAlgoTaskBarToLocalStorage({
              userId,
              taskId,
              total: this.algoTaskBar.total,
              startedAt,
              pendingResumeFileIds: this.algoTaskBar.pendingResumeFileIds,
              pausedAt: this.algoTaskBar.pausedAt,
              elapsedSecondsWhenPaused: this.algoTaskBar.elapsedSecondsWhenPaused
            })
            this.$message.info('Task paused')
          }
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
    async onClickCancelAlgoTask() {
      if (!this.ensureResumeManage('当前账号无暂停提取任务权限')) return
      const userId = this.algoTaskBar && this.algoTaskBar.userId != null ? Number(this.algoTaskBar.userId) : null
      const taskId = this.algoTaskBar && this.algoTaskBar.taskId ? String(this.algoTaskBar.taskId) : ''
      if (!userId || !taskId) {
        return
      }

      try {
        await this.$confirm('Pause current task? You can continue it later.', 'Pause Task', {
          confirmButtonText: 'Pause',
          cancelButtonText: 'Cancel',
          type: 'warning'
        })
      } catch (e) {
        return
      }

      try {
        await cancelAlgoTask(userId, taskId)
      } catch (e) {
        this.$message.error('閺嗗倸浠犳径杈Е')
        return
      }

      this.stopAlgoTaskPolling()

      this.algoTaskBar.status = 'CANCELED'
      this.algoTaskBar.statusText = 'Canceled'
      this.algoTaskBar.progressStatus = 'warning'

      let pendingIds = []
      try {
        const data = await getPendingResumeFileIds(userId, taskId)
        const list = data && Array.isArray(data.pendingResumeFileIds) ? data.pendingResumeFileIds : []
        pendingIds = Array.from(new Set(list.map((v) => (v != null ? Number(v) : null)).filter((v) => v != null && !Number.isNaN(v))))
      } catch (e) {
        pendingIds = this.algoTaskBar.pendingResumeFileIds || []
      }

      const pauseEffectiveAt = Date.now()
      const startedAt = this.algoTaskBar.startedAt || pauseEffectiveAt
      this.algoTaskBar.pausedAt = pauseEffectiveAt
      this.algoTaskBar.elapsedSecondsWhenPaused = Math.max(0, Math.floor((pauseEffectiveAt - startedAt) / 1000))
      this.algoTaskBar.elapsedSeconds = this.algoTaskBar.elapsedSecondsWhenPaused

      this.algoTaskBar.pendingResumeFileIds = pendingIds
      this.saveAlgoTaskBarToLocalStorage({
        userId,
        taskId,
        total: this.algoTaskBar.total,
        startedAt: this.algoTaskBar.startedAt,
        pendingResumeFileIds: pendingIds,
        pausedAt: this.algoTaskBar.pausedAt,
        elapsedSecondsWhenPaused: this.algoTaskBar.elapsedSecondsWhenPaused
      })
      if (pendingIds.length > 0) {
        this.$message.success(`已暂停，剩余 ${pendingIds.length} 份可继续`)
      } else {
        this.$message.success('Paused')
      }
    },
    async onClickContinueAlgoTask() {
      if (!this.ensureResumeManage('当前账号无继续提取任务权限')) return
      const userId = this.algoTaskBar && this.algoTaskBar.userId != null ? Number(this.algoTaskBar.userId) : null
      const taskId = this.algoTaskBar && this.algoTaskBar.taskId ? String(this.algoTaskBar.taskId) : ''
      if (!userId || !taskId) {
        return
      }

      let ids = null
      try {
        const data = await getPendingResumeFileIds(userId, taskId)
        const list = data && Array.isArray(data.pendingResumeFileIds) ? data.pendingResumeFileIds : []
        ids = Array.from(new Set(list.map((v) => (v != null ? Number(v) : null)).filter((v) => v != null && !Number.isNaN(v))))
        this.algoTaskBar.pendingResumeFileIds = ids
        this.saveAlgoTaskBarToLocalStorage({
          userId,
          taskId,
          total: this.algoTaskBar.total,
          startedAt: this.algoTaskBar.startedAt,
          pendingResumeFileIds: ids,
          pausedAt: this.algoTaskBar.pausedAt,
          elapsedSecondsWhenPaused: this.algoTaskBar.elapsedSecondsWhenPaused
        })
      } catch (e) {
        ids = null
      }

      if (ids == null) {
        this.$message.error('获取待处理列表失败，请稍后重试')
        return
      }

      if (ids.length === 0) {
        this.clearAlgoTaskBarLocalStorage()
        this.algoTaskBar.visible = false
        await this.fillSummariesFromResumeContent()
        this.$message.info('没有可继续的未完成项')
        return
      }

      try {
        await this.$confirm(`Continue processing the remaining ${ids.length} resumes?`, 'Continue Task', {
          confirmButtonText: '继续',
          cancelButtonText: '取消',
          type: 'info'
        })
      } catch (e) {
        return
      }

      await this.retryResumeParseByResumeFileIds(ids)
    },
    restoreAlgoTaskBar() {
      const snap = this.loadAlgoTaskBarFromLocalStorage()
      const currentUserId = this.getActiveUserId()
      const snapUserId = snap && snap.userId != null ? Number(snap.userId) : null
      if (!snap || !snap.taskId || !snapUserId || !currentUserId || snapUserId !== currentUserId) {
        return
      }
      this.updateAlgoTaskBarBySnapshot(snap)
      const startedAt = snap.startedAt != null ? Number(snap.startedAt) : Date.now()
      this.runAlgoTaskPolling(currentUserId, String(snap.taskId), startedAt)
    },
    isPdf(file) {
      const name = String((file && file.name) || '').toLowerCase()
      const type = String((file && file.type) || '').toLowerCase()
      return type === 'application/pdf' || name.endsWith('.pdf')
    },
    onFileChange(file, fileList) {
      if (!this.ensureResumeManage('当前账号无上传简历权限')) return
      const raw = file && file.raw ? file.raw : file
      if (!raw) return

      const maxPdfSizeBytes = 20 * 1024 * 1024
      if (!this.isPdf(raw)) {
        this.showTip(`Only PDF files are supported: ${raw.name || ''}`)
        return
      }
      if ((raw.size || 0) > maxPdfSizeBytes) {
        this.showTip(`PDF file is too large (max 20MB): ${raw.name || ''}`)
        return
      }

      const exists = (this.uploadItems || []).some((it) => it && it.name === raw.name)
      if (exists) {
        this.$message.warning('File already exists in upload list')
        return
      }

      this.fileTip = ''
      this.uploadItems.push({
        file: raw,
        name: raw.name || '',
        size: raw.size || 0,
        md5: '',
        status: 'pending',
        message: ''
      })
      this.confirmVisible = true

      if (this.$refs.upload && this.$refs.upload.clearFiles) {
        this.$refs.upload.clearFiles()
      }
    },
    showTip(text) {
      this.fileTip = text
      if (this.fileTipTimer) clearTimeout(this.fileTipTimer)
      this.fileTipTimer = setTimeout(() => {
        this.fileTip = ''
        this.fileTipTimer = null
      }, 3000)
    },
    initTargetUserFromRoute() {
      const rawId = this.$route && this.$route.query ? this.$route.query.userId : null
      const parsedId = rawId != null ? Number(rawId) : null
      this.targetUserId = Number.isFinite(parsedId) && parsedId > 0 ? parsedId : null
      const rawName = this.$route && this.$route.query ? this.$route.query.userName : ''
      this.targetUserName = rawName == null ? '' : String(rawName)
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
    getActiveUserId() {
      if (this.targetUserId && this.targetUserId > 0) {
        return this.targetUserId
      }
      return this.getCurrentUserId()
    },
    mapImportedToRows(imported) {
      const items = (imported && imported.items) ? imported.items : []
      return items.map((it) => {
        const files = it && it.files ? it.files : []
        const first = files && files.length > 0 ? files[0] : null
        const hasLocalFile = !!(it && it.hasLocalFile)
        const canParse = !!(it && it.canParse)
        let fileCheckMessage = '缺少文件记录'
        if (first && first.id != null) {
          fileCheckMessage = hasLocalFile ? '本地文件可用，可直接提取' : '本地文件不存在，仅可查看记录'
        }
        return {
          id: it && it.resumeId != null ? it.resumeId : '',
          name: (it && it.title) ? it.title : (first && first.fileName ? first.fileName : ''),
          storagePath: first && first.storagePath ? first.storagePath : '',
          downloadUrl: first && first.downloadUrl ? first.downloadUrl : '',
          url: first && first.downloadUrl ? first.downloadUrl : '',
          resumeFileId: first && first.id != null ? first.id : null,
          hasLocalFile,
          canParse,
          fileCheckMessage,
          project: '',
          projectFull: '',
          internship: '',
          internshipFull: '',
          education: '',
          educationFull: '',
          __pending: false,
          __deleting: false
        }
      })
    },
    truncate(text, maxLen = 32) {
      const s = text == null ? '' : String(text)
      if (!s) return ''
      if (s.length <= maxLen) return s
      return s.slice(0, maxLen) + '...'
    },
    openPreview(title, content) {
      this.previewTitle = title || '閸愬懎顔愭０鍕潔'
      this.previewContent = content == null ? '' : String(content)
      this.previewVisible = true
    },
    async onClickViewResult(row) {
      if (!row || row.id == null) {
        return
      }
      const userId = this.getActiveUserId()
      if (!userId) {
        this.$message.error('未获取到当前用户ID，请重新登录')
        return
      }

      this.resultDialogTitle = `提取结果：${row.name || row.id}`
      this.resultHtml = ''
      this.resultVisible = true
      this.resultLoading = true
      try {
        const data = await fetchResumeContent(userId, row.id)
        this.resultHtml = data && data.content != null ? String(data.content) : ''
      } catch (e) {
        this.resultVisible = false
        this.$message.error('加载提取结果失败')
      } finally {
        this.resultLoading = false
      }
    },
    resetEditForm() {
      this.editForm = {
        resumeId: null,
        title: '',
        content: ''
      }
    },
    updateEditTitle(value) {
      this.$set(this.editForm, 'title', value == null ? '' : String(value))
    },
    updateEditContent(value) {
      this.$set(this.editForm, 'content', value == null ? '' : String(value))
    },
    async onClickEditRow(row) {
      if (!this.ensureResumeManage('当前账号无编辑简历权限')) return
      if (!row || row.id == null) {
        return
      }
      const userId = this.getActiveUserId()
      if (!userId) {
        this.$message.error('未获取到当前用户ID，请重新登录')
        return
      }

      this.resetEditForm()
      this.editVisible = true
      this.editLoading = true
      this.editForm.resumeId = Number(row.id)
      this.editForm.title = row.name ? String(row.name) : ''
      try {
        const data = await fetchResumeContent(userId, row.id)
        this.editForm.content = data && data.content != null ? String(data.content) : ''
      } catch (e) {
        this.$message.error('加载简历内容失败')
      } finally {
        this.editLoading = false
      }
    },
    onCancelEdit() {
      if (this.editSaving) return
      this.editVisible = false
      this.resetEditForm()
    },
    async onSubmitEdit() {
      if (!this.ensureResumeManage('当前账号无保存简历权限')) return
      if (this.editSaving) return
      const userId = this.getActiveUserId()
      if (!userId) {
        this.$message.error('未获取到当前用户ID，请重新登录')
        return
      }
      const resumeId = this.editForm && this.editForm.resumeId != null ? Number(this.editForm.resumeId) : null
      if (!resumeId || Number.isNaN(resumeId)) {
        this.$message.error('简历ID无效')
        return
      }

      const title = this.editForm && this.editForm.title != null ? String(this.editForm.title).trim() : ''
      if (!title) {
        this.$message.warning('简历名称不能为空')
        return
      }

      this.editSaving = true
      try {
        await updateResume(userId, resumeId, {
          title,
          content: this.editForm && this.editForm.content != null ? String(this.editForm.content) : ''
        })
        this.$message.success('保存成功')
        this.editVisible = false
        this.resetEditForm()
        await this.refreshImported()
      } catch (e) {
        this.$message.error('保存失败')
      } finally {
        this.editSaving = false
      }
    },
    async onClickDeleteRow(row) {
      if (!this.ensureResumeManage('当前账号无删除简历权限')) return
      if (!row || row.id == null) return
      const userId = this.getActiveUserId()
      if (!userId) {
        this.$message.error('未获取到当前用户ID，请重新登录')
        return
      }

      try {
        await this.$confirm('删除后将同步删除本地PDF文件，是否继续？', '确认删除', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch (e) {
        return
      }

      this.$set(row, '__deleting', true)
      try {
        await deleteResume(userId, row.id)
        const deletedId = Number(row.id)
        this.selectedIds = (this.selectedIds || []).filter((id) => Number(id) !== deletedId)
        const nextMap = Object.assign({}, this.selectedRowMap || {})
        delete nextMap[String(deletedId)]
        this.selectedRowMap = nextMap
        this.$message.success('删除成功')
        await this.refreshImported()
      } catch (e) {
        this.$message.error('删除失败')
      } finally {
        this.$set(row, '__deleting', false)
      }
    },
    async refreshImported() {
      const userId = this.getActiveUserId()
      if (!userId) {
        return
      }

      this.loadingImported = true
      try {
        const data = await fetchImportedResumes(userId, {
          pageNo: this.pageNo,
          pageSize: this.pageSize,
          keyword: (this.keyword || '').trim() || undefined
        })
        this.total = Number(data && data.total != null ? data.total : 0)
        this.rows = this.mapImportedToRows(data)
        this.$nextTick(() => this.restoreSelection())
        this.fillSummariesFromResumeContent()
      } catch (e) {
        this.total = 0
        this.$message.error('获取已上传简历失败')
      } finally {
        this.loadingImported = false
      }
    },

    restoreSelection() {
      try {
        const table = this.$refs.table
        if (!table || !table.toggleRowSelection) return
        const set = new Set((this.selectedIds || []).map((v) => Number(v)).filter((v) => v != null && !Number.isNaN(v)))
        ;(this.rows || []).forEach((r) => {
          if (!r || r.id == null) return
          const id = Number(r.id)
          table.toggleRowSelection(r, set.has(id))
        })
      } catch (e) {
        void e
      }
    },

    onClickSearch() {
      this.selectedIds = []
      this.selectedRowMap = {}
      try {
        if (this.$refs.table && this.$refs.table.clearSelection) {
          this.$refs.table.clearSelection()
        }
      } catch (e) {
        void e
      }
      this.pageNo = 1
      this.refreshImported()
    },

    onSizeChange(size) {
      const n = Number(size)
      if (!Number.isFinite(n) || n <= 0) return
      this.pageSize = n
      this.pageNo = 1
      this.refreshImported()
    },

    onCurrentChange(page) {
      const n = Number(page)
      if (!Number.isFinite(n) || n <= 0) return
      this.pageNo = n
      this.refreshImported()
    },

    async fillSummariesFromResumeContent() {
      if (this.loadingContents) {
        return
      }
      const userId = this.getActiveUserId()
      if (!userId) {
        return
      }
      const rows = this.rows || []
      if (rows.length === 0) {
        return
      }

      this.loadingContents = true
      try {
        const rowByResumeId = new Map()
        const needIds = []
        for (const r of rows) {
          const resumeId = r && r.id != null ? Number(r.id) : null
          if (!resumeId) continue
          rowByResumeId.set(resumeId, r)

          const alreadyHasAny = (r.education && String(r.education).trim()) ||
            (r.internship && String(r.internship).trim()) ||
            (r.project && String(r.project).trim())
          if (alreadyHasAny) {
            continue
          }
          needIds.push(resumeId)
        }

        if (needIds.length === 0) {
          return
        }

        const batchSize = 50
        for (let i = 0; i < needIds.length; i += batchSize) {
          const slice = needIds.slice(i, i + batchSize)
          let data
          try {
            data = await fetchResumeContentBatch(userId, slice)
          } catch (e) {
            continue
          }
          const items = data && data.items ? data.items : []

          items.forEach((it) => {
            if (!it || it.ok === false) {
              return
            }
            const resumeId = it.resumeId != null ? Number(it.resumeId) : null
            if (!resumeId) {
              return
            }
            const r = rowByResumeId.get(resumeId)
            if (!r) {
              return
            }
            const html = it.content ? String(it.content) : ''
            if (!html) {
              return
            }

            const educationItems = this.extractSectionItems(html, '教育经历')
            const internshipItems = this.extractSectionItems(html, '实习经历')
            const projectItems = this.extractSectionItems(html, '项目经历')

            r.education = educationItems.length > 0 ? educationItems[0] : ''
            r.internship = internshipItems.length > 0 ? internshipItems[0] : ''
            r.project = projectItems.length > 0 ? projectItems[0] : ''

            r.educationFull = educationItems.join('\n')
            r.internshipFull = internshipItems.join('\n')
            r.projectFull = projectItems.join('\n')
          })
        }
      } finally {
        this.loadingContents = false
      }
    },

    extractSectionItems(html, title) {
      const s = html == null ? '' : String(html)
      if (!s || !title) {
        return []
      }
      try {
        const parser = new DOMParser()
        const doc = parser.parseFromString(s, 'text/html')
        const hs = Array.from(doc.querySelectorAll('h2'))
        const idx = hs.findIndex((h) => (h && h.textContent ? h.textContent.trim() : '') === title)
        if (idx < 0) {
          return []
        }
        const start = hs[idx]
        const end = idx + 1 < hs.length ? hs[idx + 1] : null

        const items = []
        let node = start.nextElementSibling
        while (node && node !== end) {
          if (node.tagName === 'UL') {
            const lis = Array.from(node.querySelectorAll('li'))
            lis.forEach((li) => {
              const t = li && li.textContent ? li.textContent.trim() : ''
              if (t) items.push(t)
            })
          } else if (node.tagName === 'P') {
            const t = node.textContent ? node.textContent.trim() : ''
            if (t) items.push(t)
          } else if (node.tagName === 'TABLE') {
            const trs = Array.from(node.querySelectorAll('tr'))
            trs.forEach((tr) => {
              const t = tr && tr.textContent ? tr.textContent.trim() : ''
              if (t) items.push(t)
            })
          }
          node = node.nextElementSibling
        }
        return items
      } catch (e) {
        return []
      }
    },
    async submitUpload() {
      if (!this.ensureResumeManage('当前账号无上传简历权限')) return
      if (this.importing) return

      const userId = this.getActiveUserId()
      if (!userId) {
        this.$message.error('未获取到当前用户ID，请重新登录')
        return
      }

      if (!this.uploadItems || this.uploadItems.length === 0) {
        this.$message.warning('请先选择 PDF 文件')
        return
      }

      this.importing = true
      try {
        if (typeof navigator !== 'undefined' && navigator.onLine === false) {
          this.$message.error('当前网络不可用，请恢复网络后继续上传')
          return
        }

        const batchSize = 5
        const concurrency = 2

        const runnable = (this.uploadItems || []).filter((it) => {
          if (!it || !it.file) return false
          if (it.status === 'success' || it.status === 'duplicate') return false
          return true
        })

        if (runnable.length === 0) {
          this.$message.info('没有需要上传的文件')
          return
        }

        for (const it of runnable) {
          if (!it.md5) {
            it.status = 'hashing'
            it.message = '正在计算 MD5'
            try {
              it.md5 = await this.calcFileMd5(it.file)
              it.status = 'hashed'
              it.message = ''
            } catch (e) {
              it.status = 'fail'
              it.message = 'MD5 计算失败'
            }
          }
        }

        const md5List = runnable.map((it) => it && it.md5).filter((v) => v)
        if (md5List.length > 0) {
          let precheck
          try {
            precheck = await precheckImportPdfMd5Batch(userId, md5List)
          } catch (e) {
            precheck = null
          }

          const existedByMd5 = new Map()
          const items = precheck && precheck.items ? precheck.items : []
          items.forEach((x) => {
            if (!x || !x.md5) return
            existedByMd5.set(String(x.md5).toLowerCase(), x)
          })

          runnable.forEach((it) => {
            const md5 = it && it.md5 ? String(it.md5).toLowerCase() : ''
            const existed = md5 ? existedByMd5.get(md5) : null
            if (existed && existed.exists) {
              it.status = 'duplicate'
              it.message = '文件已存在，已跳过'
              it.resumeId = existed.resumeId
              it.resumeFileId = existed.resumeFileId
              it.storagePath = existed.storagePath
              it.downloadUrl = existed.downloadUrl
            }
          })
        }

        const toUpload = runnable.filter((it) => it && it.status !== 'duplicate' && it.status !== 'success' && it.file)
        if (toUpload.length === 0) {
          const dupCount = (this.uploadItems || []).filter((it) => it && it.status === 'duplicate').length
          this.$message.success(`批量上传完成：重复 ${dupCount} 份，无需重复上传`)
          await this.refreshImported()
          return
        }

        const batches = []
        for (let i = 0; i < toUpload.length; i += batchSize) {
          batches.push(toUpload.slice(i, i + batchSize))
        }

        const stopFlag = { value: false }
        const setStopFlag = () => { stopFlag.value = true }
        let cursor = 0

        const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, Math.max(0, ms || 0)))

        const isRateLimitedError = (e) => {
          const msg = e && e.message ? String(e.message) : ''
          return msg && msg.includes('上传过于频繁')
        }

        const isNetworkError = (e) => {
          const msg = e && e.message ? String(e.message) : ''
          if (typeof navigator !== 'undefined' && navigator.onLine === false) {
            return true
          }
          return msg && (msg.includes('Network Error') || msg.includes('timeout') || msg.includes('ECONN') || msg.includes('connect'))
        }

        const applyBatchResult = (batch, resp) => {
          const items = resp && resp.items ? resp.items : []
          for (let i = 0; i < batch.length; i++) {
            const it = batch[i]
            const r = items[i]
            const status = r && r.status ? String(r.status) : 'fail'
            if (status === 'success') {
              it.status = 'success'
              it.message = '上传成功'
              it.resumeId = r.resumeId
              it.resumeFileId = r.resumeFileId
              it.storagePath = r.storagePath
              it.md5 = it.md5 || r.md5
            } else if (status === 'duplicate') {
              it.status = 'duplicate'
              it.message = '文件已存在，已跳过'
              it.resumeId = r.resumeId
              it.resumeFileId = r.resumeFileId
              it.storagePath = r.storagePath
              it.md5 = it.md5 || r.md5
              it.downloadUrl = this.buildResumeFileDownloadUrl(userId, it.resumeFileId)
            } else {
              it.status = 'fail'
              it.message = (r && r.errorMessage) ? String(r.errorMessage) : '上传失败'
            }
          }
        }

        const markBatchFail = (batch, message) => {
          batch.forEach((it) => {
            it.status = 'fail'
            it.message = message || '上传失败'
          })
        }

        const runOneBatch = async(batch, attempt = 0) => {
          if (!batch || batch.length === 0) return
          if (stopFlag.value) return

          const maxRateLimitRetries = 6
          const maxNetworkRetries = 3
          const baseDelayMs = 800
          const maxDelayMs = 8000

          batch.forEach((it) => {
            it.status = attempt > 0 ? 'retrying' : 'uploading'
            it.message = attempt > 0 ? `进入重试队列（第 ${attempt} 次）` : '上传中'
          })

          let resp
          try {
            resp = await importPdfBatch(userId, batch.map((it) => it.file))
          } catch (e) {
            if (isRateLimitedError(e)) {
              const nextAttempt = attempt + 1
              if (batch.length > 1 && nextAttempt >= 2) {
                const mid = Math.ceil(batch.length / 2)
                const left = batch.slice(0, mid)
                const right = batch.slice(mid)
                left.forEach((it) => {
                  it.status = 'retrying'
                  it.message = '服务繁忙，已拆分为更小批次重试'
                })
                right.forEach((it) => {
                  it.status = 'retrying'
                  it.message = '服务繁忙，已拆分为更小批次重试'
                })
                await runOneBatch(left, 0)
                await runOneBatch(right, 0)
                return
              }
              if (nextAttempt > maxRateLimitRetries) {
                markBatchFail(batch, '上传排队超时，可点击继续重试')
                setStopFlag()
                return
              }
              const jitter = Math.floor(Math.random() * 200)
              const delay = Math.min(maxDelayMs, Math.floor(baseDelayMs * Math.pow(2, Math.max(0, nextAttempt - 1))) + jitter)
              batch.forEach((it) => {
                it.status = 'retrying'
                it.message = `服务繁忙，${Math.ceil(delay / 1000)} 秒后重试（第 ${nextAttempt} 次）`
              })
              await sleep(delay)
              await runOneBatch(batch, nextAttempt)
              return
            }

            if (isNetworkError(e)) {
              const nextAttempt = attempt + 1
              if (nextAttempt > maxNetworkRetries) {
                markBatchFail(batch, '网络异常，请恢复连接后重试')
                setStopFlag()
                return
              }
              const jitter = Math.floor(Math.random() * 200)
              const delay = Math.min(maxDelayMs, Math.floor(baseDelayMs * Math.pow(2, Math.max(0, nextAttempt - 1))) + jitter)
              batch.forEach((it) => {
                it.status = 'retrying'
                it.message = `网络异常，${Math.ceil(delay / 1000)} 秒后重试（第 ${nextAttempt} 次）`
              })
              await sleep(delay)
              await runOneBatch(batch, nextAttempt)
              return
            }

            markBatchFail(batch, '上传失败，可点击继续重试')
            setStopFlag()
            return
          }

          applyBatchResult(batch, resp)
        }

        const worker = async() => {
          while (cursor < batches.length) {
            if (stopFlag.value) {
              break
            }
            const idx = cursor++
            await runOneBatch(batches[idx])
          }
        }

        const workers = []
        const n = Math.max(1, concurrency)
        for (let i = 0; i < n; i++) {
          workers.push(worker())
        }
        await Promise.all(workers)

        const successCount = (this.uploadItems || []).filter((it) => it && it.status === 'success').length
        const dupCount = (this.uploadItems || []).filter((it) => it && it.status === 'duplicate').length
        const failCount = (this.uploadItems || []).filter((it) => it && it.status === 'fail').length
        if (stopFlag.value) {
          this.$message.warning(`上传中断：成功 ${successCount} 份，重复 ${dupCount} 份，失败 ${failCount} 份。可继续上传以重试失败项。`)
          await this.refreshImported()
          return
        }

        this.$message.success(`上传完成：成功 ${successCount} 份，重复 ${dupCount} 份，失败 ${failCount} 份`)
        this.uploadItems = []
        this.confirmVisible = false
        if (this.$refs.upload && this.$refs.upload.clearFiles) {
          this.$refs.upload.clearFiles()
        }
        await this.refreshImported()
      } catch (e) {
        this.$message.error('上传失败')
      } finally {
        this.importing = false
      }
    },
    getUploadItemStatusText(it) {
      const s = it && it.status ? String(it.status) : ''
      if (!s || s === 'pending') return '待处理'
      if (s === 'hashing') return '计算中'
      if (s === 'hashed') return '待处理'
      if (s === 'uploading') return '上传中'
      if (s === 'retrying') return '重试中'
      if (s === 'success') return '成功'
      if (s === 'duplicate') return '重复'
      if (s === 'fail') return '失败'
      return s
    },
    buildResumeFileDownloadUrl(userId, resumeFileId) {
      const uid = userId != null ? String(userId) : ''
      const rid = resumeFileId != null ? String(resumeFileId) : ''
      if (!uid || !rid) return ''
      return `/interview/admin/users/${uid}/resumes/files/${rid}/file`
    },
    resolveDownloadHref(url) {
      const raw = url ? String(url) : ''
      if (!raw) return ''
      if (/^https?:\/\//i.test(raw)) return raw

      const baseApi = (process && process.env && process.env.VUE_APP_BASE_API) ? String(process.env.VUE_APP_BASE_API) : ''
      if (!baseApi || baseApi === '/') return raw

      const base = baseApi.endsWith('/') ? baseApi.slice(0, -1) : baseApi
      const path = raw.startsWith('/') ? raw : `/${raw}`
      return `${base}${path}`
    },
    async downloadByUrl(url, fallbackFileName) {
      const href = this.resolveDownloadHref(url)
      if (!href) {
        this.$message.info('未获取到下载链接')
        return
      }

      let resp
      try {
        resp = await axios.get(href, {
          responseType: 'blob',
          headers: {
            'X-Token': getToken()
          }
        })
      } catch (e) {
        this.$message.error('下载失败')
        return
      }

      const blob = resp && resp.data ? resp.data : null
      if (!blob) {
        this.$message.error('下载失败：空响应')
        return
      }

      let fileName = fallbackFileName ? String(fallbackFileName) : 'resume.pdf'
      const disposition = resp && resp.headers ? (resp.headers['content-disposition'] || resp.headers['Content-Disposition']) : ''
      if (disposition) {
        const d = String(disposition)
        const matchStar = d.match(/filename\*=UTF-8''([^;]+)/i)
        if (matchStar && matchStar[1]) {
          try {
            fileName = decodeURIComponent(matchStar[1])
          } catch (e) {
            fileName = fallbackFileName ? String(fallbackFileName) : fileName
          }
        } else {
          const match = d.match(/filename="?([^";]+)"?/i)
          if (match && match[1]) {
            fileName = match[1]
          }
        }
      }

      try {
        const objectUrl = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = objectUrl
        a.download = fileName
        document.body.appendChild(a)
        a.click()
        a.remove()
        window.URL.revokeObjectURL(objectUrl)
      } catch (e) {
        this.$message.error('下载失败：当前浏览器不支持')
      }
    },
    async onClickDownloadDuplicate(it) {
      if (!it) return
      const userId = this.getActiveUserId()
      const fallback = this.buildResumeFileDownloadUrl(userId, it.resumeFileId)
      const name = it && it.name ? it.name : (it && it.fileName ? it.fileName : '')
      await this.downloadByUrl(it.downloadUrl || fallback, name)
    },
    async onClickDownloadRow(row) {
      if (!row) return
      const userId = this.getActiveUserId()
      const fallback = this.buildResumeFileDownloadUrl(userId, row.resumeFileId)
      const url = row.downloadUrl || fallback
      if (!url) {
        this.$message.info('No downloadable file')
        return
      }
      const name = row.name ? String(row.name) : 'resume.pdf'
      await this.downloadByUrl(url, name)
    },
    removeUploadItemsByStatus(status) {
      if (!this.ensureResumeManage('当前账号无维护上传列表权限')) return
      const s = String(status || '')
      if (!s) return
      this.uploadItems = (this.uploadItems || []).filter((it) => !(it && it.status === s))
      if (this.uploadItems.length === 0) {
        this.confirmVisible = false
      }
    },
    clearUploadItems() {
      if (!this.ensureResumeManage('当前账号无清空上传列表权限')) return
      this.uploadItems = []
      this.confirmVisible = false
      if (this.$refs.upload && this.$refs.upload.clearFiles) {
        this.$refs.upload.clearFiles()
      }
    },
    calcFileMd5(file) {
      return new Promise((resolve, reject) => {
        if (!file) {
          reject(new Error('文件为空'))
          return
        }
        const chunkSize = 2 * 1024 * 1024
        const chunks = Math.ceil(file.size / chunkSize)
        let currentChunk = 0
        const spark = new SparkMD5.ArrayBuffer()
        const reader = new FileReader()
        reader.onload = (e) => {
          try {
            spark.append(e.target.result)
          } catch (err) {
            reject(err)
            return
          }
          currentChunk++
          if (currentChunk < chunks) {
            loadNext()
          } else {
            resolve(spark.end())
          }
        }
        reader.onerror = () => {
          reject(new Error('读取文件失败'))
        }
        const loadNext = () => {
          const start = currentChunk * chunkSize
          const end = Math.min(file.size, start + chunkSize)
          const blobSlice = file.slice || file.mozSlice || file.webkitSlice
          reader.readAsArrayBuffer(blobSlice.call(file, start, end))
        }
        loadNext()
      })
    },
    onCancelConfirm() {
      if (this.importing) return
      this.confirmVisible = false
      this.uploadItems = []
      if (this.$refs.upload && this.$refs.upload.clearFiles) {
        this.$refs.upload.clearFiles()
      }
    },
    getRowKey(row) {
      if (!row) return ''
      if (row.resumeFileId != null) return `file:${row.resumeFileId}`
      return `row:${row.id}`
    },
    isRowSelectable(row) {
      return !!(row && row.resumeFileId != null && row.canParse)
    },
    onSelectionChange(val) {
      const currentIds = (this.rows || []).map((r) => (r && r.id != null ? Number(r.id) : null)).filter((v) => v != null && !Number.isNaN(v))
      const selectedInPage = (val || []).map((r) => (r && r.id != null ? Number(r.id) : null)).filter((v) => v != null && !Number.isNaN(v))
      const currentIdSet = new Set(currentIds)
      const selectedInPageSet = new Set(selectedInPage)

      const nextRowMap = Object.assign({}, this.selectedRowMap || {})
      ;(currentIds || []).forEach((id) => {
        if (!selectedInPageSet.has(id)) {
          delete nextRowMap[String(id)]
        }
      })
      ;(val || []).forEach((r) => {
        if (!r || r.id == null) return
        const id = Number(r.id)
        if (!Number.isFinite(id)) return
        nextRowMap[String(id)] = r
      })
      this.selectedRowMap = nextRowMap

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

    async onClickBatchExtract() {
      if (!this.ensureResumeManage('当前账号无批量提取权限')) return
      if (this.parsing) return
      const userId = this.getActiveUserId()
      if (!userId) {
        this.$message.error('未获取到当前用户ID，请重新登录')
        return
      }

      const selectedIds = (this.selectedIds || []).map((v) => (v != null ? Number(v) : null)).filter((v) => v != null && !Number.isNaN(v))
      const rowMap = this.selectedRowMap || {}
      const selectedRows = selectedIds.map((id) => rowMap[String(id)]).filter(Boolean)
      const invalidRows = selectedRows.filter((row) => !this.isRowSelectable(row))
      const validRows = selectedRows.filter((row) => this.isRowSelectable(row))
      const resumeFileIds = Array.from(new Set(validRows.map((r) => r && r.resumeFileId).filter((v) => v != null)))

      if (resumeFileIds.length === 0) {
        const message = invalidRows.length > 0
          ? '选中的简历缺少本地 PDF，仅可查看记录，不能提取'
          : '请先在表格中选中要提取的简历行'
        this.$message.warning(message)
        return
      }

      if (invalidRows.length > 0) {
        this.$message.warning(`已自动跳过 ${invalidRows.length} 条缺少本地文件的简历，仅提取可用文件`)
      }

      this.parsing = true
      try {
        const createRes = await createAlgoTask(userId, 'resume_parse', { resumeFileIds })
        const taskId = createRes && createRes.taskId ? String(createRes.taskId) : ''
        if (!taskId) {
          this.$message.error('创建提取任务失败')
          return
        }

        const startedAt = Date.now()
        this.stopAlgoTaskPolling()
        this.updateAlgoTaskBarBySnapshot({ userId, taskId, total: resumeFileIds.length, startedAt })
        this.saveAlgoTaskBarToLocalStorage({ userId, taskId, total: resumeFileIds.length, startedAt })
        await this.runAlgoTaskPolling(userId, taskId, startedAt)
      } catch (e) {
        this.$message.error('批量提取失败')
      } finally {
        this.parsing = false
      }
    },
    getRetryableResumeFileIdsFromAlgoTask(task) {
      const result = task && task.result ? task.result : null
      const items = result && Array.isArray(result.items) ? result.items : []
      const ids = []
      const seen = new Set()
      items.forEach((it) => {
        if (!it || it.ok !== false || it.retryable !== true) return

        const code = it.errorCode != null ? String(it.errorCode) : ''
        const stage = it.stage != null ? String(it.stage) : ''
        if (code === 'TASK_CANCELED' || stage === 'TASK') {
          return
        }

        const id = it.resumeFileId != null ? Number(it.resumeFileId) : null
        if (!id || Number.isNaN(id)) return
        if (seen.has(id)) return
        seen.add(id)
        ids.push(id)
      })
      return ids
    },
    getFirstFailureHintFromAlgoTask(task) {
      const result = task && task.result ? task.result : null
      const items = result && Array.isArray(result.items) ? result.items : []
      const first = items.find((it) => {
        if (!it || it.ok !== false) return false
        const code = it.errorCode != null ? String(it.errorCode) : ''
        const stage = it.stage != null ? String(it.stage) : ''
        if (code === 'TASK_CANCELED' || stage === 'TASK') {
          return false
        }
        return true
      })
      if (!first) return ''
      return first.userHint ? String(first.userHint) : (first.errorMessage ? String(first.errorMessage) : '')
    },
    async retryResumeParseByResumeFileIds(resumeFileIds) {
      if (!this.ensureResumeManage('当前账号无重试提取权限')) return
      const userId = this.getActiveUserId()
      if (!userId) {
        this.$message.error('未获取到当前用户ID，请重新登录')
        return
      }

      const ids = Array.from(new Set((resumeFileIds || []).map((v) => (v != null ? Number(v) : null)).filter((v) => v != null && !Number.isNaN(v))))
      if (ids.length === 0) {
        this.$message.info('No retryable failed items')
        return
      }

      this.parsing = true
      try {
        const createRes = await createAlgoTask(userId, 'resume_parse', { resumeFileIds: ids, options: { retry: true }})
        const taskId = createRes && createRes.taskId ? String(createRes.taskId) : ''
        if (!taskId) {
          this.$message.error('创建提取任务失败，请稍后重试')
          return
        }

        const startedAt = Date.now()
        this.stopAlgoTaskPolling()
        this.updateAlgoTaskBarBySnapshot({ userId, taskId, total: ids.length, startedAt })
        this.saveAlgoTaskBarToLocalStorage({ userId, taskId, total: ids.length, startedAt })
        await this.runAlgoTaskPolling(userId, taskId, startedAt)
      } catch (e) {
        this.$message.error('闁插秷鐦径杈Е')
      } finally {
        this.parsing = false
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
  justify-content: flex-start;
  margin-bottom: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 12px 0;
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

.tip {
  margin-bottom: 12px;
}

.pager {
  margin-top: 12px;
  text-align: center;
}

.op-btn + .op-btn {
  margin-left: 2px;
}

.danger-text {
  color: #f56c6c;
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
