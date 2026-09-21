<template>
  <div class="page">
    <el-alert
      class="user-tip"
      :title="activeUserTip"
      type="info"
      :closable="false"
      show-icon
    />

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索简历名称"
        clearable
        size="small"
        class="search"
        @keyup.enter.native="onSearch"
      />
      <el-button size="small" type="primary" @click="onSearch">搜索</el-button>
      <el-button size="small" @click="onReset">重置</el-button>
      <el-button size="small" @click="fetchData">刷新</el-button>
      <el-button
        size="small"
        :disabled="selectedDownloadableCount === 0 || batchDownloading"
        :loading="batchDownloading"
        @click="onClickBatchDownload"
      >{{ batchDownloadButtonText }}</el-button>
      <span v-if="batchDownloading" class="download-progress">
        下载进度 {{ batchDownloadProgress.done }}/{{ batchDownloadProgress.total }}
      </span>
      <el-button
        v-if="canManageResume"
        size="small"
        type="danger"
        :disabled="selectedIds.length === 0 || batchDeleting"
        :loading="batchDeleting"
        @click="onClickBatchDelete"
      >{{ batchDeleteButtonText }}</el-button>
      <span v-if="batchDeleting" class="download-progress">
        删除进度 {{ batchDeleteProgress.done }}/{{ batchDeleteProgress.total }}
      </span>
    </div>

    <div class="summary-grid">
      <div v-for="card in summaryCards" :key="card.label" class="summary-card">
        <div class="summary-label">{{ card.label }}</div>
        <div class="summary-value">{{ card.value }}</div>
        <div class="summary-sub">{{ card.sub }}</div>
      </div>
    </div>

    <el-table
      ref="table"
      v-loading="loading"
      :data="rows"
      border
      style="width: 100%"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="52" />
      <el-table-column prop="id" label="ID" width="90" />

      <el-table-column prop="resumeName" label="简历名称" min-width="200">
        <template slot-scope="scope">
          <span class="title">{{ scope.row.resumeName || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="fileName" label="文件名" min-width="180">
        <template slot-scope="scope">
          <span>{{ scope.row.fileName || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="存储路径" min-width="220">
        <template slot-scope="scope">
          <span
            v-if="scope.row.storagePath"
            class="cell-link"
            @click.stop="openPreview('存储路径', scope.row.storagePath)"
          >{{ truncate(scope.row.storagePath, 40) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>

      <el-table-column label="内容摘要" min-width="320">
        <template slot-scope="scope">
          <div class="summary-cell">
            <span>{{ truncate(stripHtml(scope.row.currentText || ''), 80) || '-' }}</span>
            <el-button type="text" size="mini" @click.stop="onClickViewContent(scope.row)">查看</el-button>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="210" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click.stop="onClickDownloadRow(scope.row)">下载</el-button>
          <el-button type="text" size="mini" @click.stop="onClickEditRow(scope.row)">编辑</el-button>
          <el-button
            v-if="canManageResume"
            type="text"
            size="mini"
            class="danger-text"
            :disabled="isDeleting(scope.row.id)"
            @click.stop="onClickDeleteRow(scope.row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && rows.length === 0" class="empty">暂无数据</div>

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

    <el-dialog
      :title="previewTitle"
      :visible.sync="previewVisible"
      width="760px"
      append-to-body
    >
      <div class="preview-body">{{ previewContent }}</div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="previewVisible = false">关闭</el-button>
      </span>
    </el-dialog>

    <el-dialog
      title="编辑简历"
      :visible.sync="editVisible"
      width="820px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div v-loading="editLoading">
        <el-form label-width="100px" size="small">
          <el-form-item label="简历名称">
            <el-input v-model.trim="editForm.title" maxlength="120" show-word-limit />
          </el-form-item>
          <el-form-item label="简历内容">
            <el-input
              v-model="editForm.content"
              type="textarea"
              :rows="16"
              placeholder="可编辑的简历内容（支持 HTML）"
            />
          </el-form-item>
        </el-form>
      </div>
      <span slot="footer">
        <el-button :disabled="editSaving" @click="onCancelEdit">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="onSubmitEdit">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog
      :title="batchResultTitle"
      :visible.sync="batchResultVisible"
      width="860px"
      append-to-body
    >
      <div class="batch-result-toolbar">
        <span>失败总数：{{ batchResultItems.length }}</span>
      </div>
      <el-table
        :data="batchResultItems"
        border
        size="mini"
        max-height="420"
      >
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="name" label="简历" min-width="220" />
        <el-table-column prop="reason" label="原因" min-width="420" />
      </el-table>
      <span slot="footer">
        <el-button
          type="warning"
          :disabled="!canRetryBatchResult"
          :loading="batchResultRetrying"
          @click="onClickRetryBatchResult"
        >{{ batchResultRetryButtonText }}</el-button>
        <el-button :disabled="batchResultItems.length === 0" @click="onClickExportBatchResultCsv">导出 CSV</el-button>
        <el-button type="primary" @click="batchResultVisible = false">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios'
import { saveAs } from 'file-saver'
import JSZip from 'jszip'
import { getToken } from '@/utils/auth'
import {
  deleteResume,
  fetchImportedResumes,
  fetchResumeContent,
  fetchResumeContentBatch,
  updateResume
} from '@/api/interviewelf/resumeImport'

export default {
  name: 'ResumeBatchEdit',
  data() {
    return {
      loading: false,
      query: {
        keyword: ''
      },
      rows: [],
      pageNo: 1,
      pageSize: 20,
      total: 0,
      selectedIds: [],
      previewVisible: false,
      previewTitle: '',
      previewContent: '',
      batchDownloading: false,
      batchDownloadProgress: {
        done: 0,
        total: 0
      },
      batchDeleting: false,
      batchDeleteProgress: {
        done: 0,
        total: 0
      },
      batchResultVisible: false,
      batchResultTitle: '批量结果',
      batchResultItems: [],
      batchResultExportName: 'batch_failed_items.csv',
      batchResultAction: '',
      batchResultRetrying: false,
      editVisible: false,
      editLoading: false,
      editSaving: false,
      deletingMap: {},
      editForm: {
        resumeId: null,
        title: '',
        content: ''
      }
    }
  },
  computed: {
    permissionCodes() {
      const codes = this.$store && this.$store.getters ? this.$store.getters.permission_codes : []
      return Array.isArray(codes) ? codes : []
    },
    userRoles() {
      const roles = this.$store && this.$store.getters ? this.$store.getters.roles : []
      return Array.isArray(roles) ? roles : []
    },
    canManageResume() {
      return this.userRoles.includes('admin') || this.permissionCodes.includes('resume:manage')
    },
    selectedRows() {
      if (!Array.isArray(this.rows) || this.rows.length === 0 || !Array.isArray(this.selectedIds)) {
        return []
      }
      const idSet = new Set(this.selectedIds.map((id) => Number(id)).filter((id) => Number.isFinite(id)))
      return this.rows.filter((row) => row && Number.isFinite(Number(row.id)) && idSet.has(Number(row.id)))
    },
    selectedDownloadableCount() {
      return this.selectedRows.filter((row) => row && row.downloadUrl).length
    },
    batchDownloadButtonText() {
      if (!this.batchDownloading) return '批量下载'
      return `批量下载（${this.batchDownloadProgress.done}/${this.batchDownloadProgress.total}）`
    },
    batchDeleteButtonText() {
      if (!this.batchDeleting) return '批量删除'
      return `批量删除（${this.batchDeleteProgress.done}/${this.batchDeleteProgress.total}）`
    },
    canRetryBatchResult() {
      if (this.batchResultRetrying) return false
      if (!Array.isArray(this.batchResultItems) || this.batchResultItems.length === 0) return false
      return this.batchResultAction === 'download' || this.batchResultAction === 'delete'
    },
    batchResultRetryButtonText() {
      if (this.batchResultAction === 'download') return '重试失败下载'
      if (this.batchResultAction === 'delete') return '重试失败删除'
      return '重试失败项'
    },
    activeUserTip() {
      const userId = this.getTargetUserId()
      if (!userId) {
        return '未获取到当前用户，请重新登录后再试。'
      }
      const routeName = this.$route && this.$route.query ? this.$route.query.userName : ''
      const suffix = routeName ? ` (${routeName})` : ''
      return `当前管理用户：${userId}${suffix}。这里展示该用户已上传的简历，可直接下载、编辑和删除。`
    },
    currentUserName() {
      const routeName = this.$route && this.$route.query ? this.$route.query.userName : ''
      return routeName || '当前登录用户'
    },
    pageDownloadableCount() {
      return Array.isArray(this.rows) ? this.rows.filter((row) => row && row.downloadUrl).length : 0
    },
    summaryCards() {
      return [
        {
          label: '当前用户',
          value: this.currentUserName,
          sub: `用户 ID：${this.getTargetUserId() || '-'}`
        },
        {
          label: '简历总数',
          value: this.total,
          sub: '按当前筛选条件统计'
        },
        {
          label: '本页可下载',
          value: this.pageDownloadableCount,
          sub: '存在本地文件的简历'
        },
        {
          label: '当前选中',
          value: this.selectedIds.length,
          sub: '可用于批量操作'
        }
      ]
    }
  },
  watch: {
    '$route.query.userId': function() {
      this.pageNo = 1
      this.fetchData()
    }
  },
  mounted() {
    this.fetchData()
  },
  methods: {
    resolveUserIdFromCache(cacheKey) {
      try {
        const raw = window.localStorage.getItem(cacheKey)
        const user = raw ? JSON.parse(raw) : null
        const value = user && (user.id != null ? user.id : user.userId)
        const id = value != null ? Number(value) : null
        return Number.isFinite(id) && id > 0 ? id : null
      } catch (e) {
        return null
      }
    },
    getCurrentUserId() {
      const keys = ['InterviewElf-User', 'UserInfo']
      for (const key of keys) {
        const id = this.resolveUserIdFromCache(key)
        if (id) {
          return id
        }
      }
      return null
    },
    getActiveUserId() {
      return this.getCurrentUserId()
    },
    getTargetUserId() {
      const routeValue = this.$route && this.$route.query ? this.$route.query.userId : null
      const fromRoute = routeValue != null ? Number(routeValue) : null
      if (Number.isFinite(fromRoute) && fromRoute > 0) {
        return fromRoute
      }
      return this.getActiveUserId()
    },
    stripHtml(text) {
      const raw = text == null ? '' : String(text)
      if (!raw) return ''
      return raw
        .replace(/<style[\s\S]*?<\/style>/ig, ' ')
        .replace(/<script[\s\S]*?<\/script>/ig, ' ')
        .replace(/<[^>]+>/g, ' ')
        .replace(/&nbsp;/ig, ' ')
        .replace(/\s+/g, ' ')
        .trim()
    },
    truncate(text, maxLen = 50) {
      const raw = text == null ? '' : String(text)
      if (!raw) return ''
      return raw.length > maxLen ? `${raw.slice(0, maxLen)}...` : raw
    },
    openPreview(title, content) {
      this.previewTitle = title || '预览'
      this.previewContent = content == null ? '' : String(content)
      this.previewVisible = true
    },
    onSearch() {
      this.pageNo = 1
      this.fetchData()
    },
    onReset() {
      this.query.keyword = ''
      this.pageNo = 1
      this.fetchData()
    },
    onSelectionChange(rows) {
      this.selectedIds = (rows || []).map((r) => (r && r.id != null ? r.id : null)).filter((id) => id != null)
    },
    async fetchData() {
      const userId = this.getTargetUserId()
      if (!userId) {
        this.rows = []
        this.total = 0
        this.$message.error('未获取到当前用户，请重新登录后再试。')
        return
      }

      this.loading = true
      try {
        const data = await fetchImportedResumes(userId, {
          pageNo: this.pageNo,
          pageSize: this.pageSize,
          keyword: this.query.keyword || ''
        })

        const items = data && Array.isArray(data.items) ? data.items : []
        const rows = items.map((item) => {
          const files = item && Array.isArray(item.files) ? item.files : []
          const file = files.length > 0 ? files[0] : null
          return {
            id: item.resumeId,
            resumeName: item.title || '',
            resumeFileId: file && file.id != null ? file.id : null,
            fileName: file && file.fileName ? file.fileName : '',
            storagePath: file && file.storagePath ? file.storagePath : '',
            downloadUrl: file && file.downloadUrl ? file.downloadUrl : '',
            currentText: ''
          }
        })
        this.rows = rows
        this.total = data && data.total != null ? Number(data.total) : 0
        await this.loadContentBatch(userId, rows)
      } catch (e) {
        this.rows = []
        this.total = 0
        this.$message.error('获取简历列表失败')
      } finally {
        this.loading = false
      }
    },
    async loadContentBatch(userId, rows) {
      const ids = (rows || []).map((r) => r && r.id).filter((id) => id != null)
      if (ids.length === 0) return
      try {
        const data = await fetchResumeContentBatch(userId, ids)
        const items = data && Array.isArray(data.items) ? data.items : []
        const contentMap = {}
        items.forEach((it) => {
          if (!it || it.resumeId == null) return
          contentMap[String(it.resumeId)] = it.ok ? (it.content || '') : ''
        })
        this.rows = this.rows.map((row) => ({
          ...row,
          currentText: contentMap[String(row.id)] != null ? contentMap[String(row.id)] : row.currentText
        }))
      } catch (e) {
        this.$message.warning('批量获取简历内容失败')
      }
    },
    onSizeChange(size) {
      this.pageSize = Number(size) || 20
      this.pageNo = 1
      this.fetchData()
    },
    onCurrentChange(page) {
      this.pageNo = Number(page) || 1
      this.fetchData()
    },
    patchRowById(id, patch) {
      const targetId = Number(id)
      if (!Number.isFinite(targetId)) return
      this.rows = (this.rows || []).map((r) => {
        if (!r || Number(r.id) !== targetId) return r
        return { ...r, ...(patch || {}) }
      })
    },
    async onClickViewContent(row) {
      if (!row || row.id == null) return
      const userId = this.getTargetUserId()
      if (!userId) return
      if (row.currentText) {
        this.openPreview('简历内容', row.currentText)
        return
      }
      try {
        const data = await fetchResumeContent(userId, row.id)
        const content = data && data.content ? data.content : ''
        this.patchRowById(row.id, { currentText: content })
        this.openPreview('简历内容', content)
      } catch (e) {
        this.$message.error('获取简历内容失败')
      }
    },
    async onClickEditRow(row) {
      if (!row || row.id == null) return
      const userId = this.getTargetUserId()
      if (!userId) return
      this.editVisible = true
      this.editLoading = true
      this.editForm = {
        resumeId: row.id,
        title: row.resumeName || '',
        content: row.currentText || ''
      }
      try {
        if (!this.editForm.content) {
          const data = await fetchResumeContent(userId, row.id)
          this.editForm.content = data && data.content ? data.content : ''
        }
      } catch (e) {
        this.$message.warning('获取简历内容失败，仍可手动编辑')
      } finally {
        this.editLoading = false
      }
    },
    onCancelEdit() {
      this.editVisible = false
      this.editForm = {
        resumeId: null,
        title: '',
        content: ''
      }
    },
    async onSubmitEdit() {
      const userId = this.getTargetUserId()
      if (!userId) return
      if (!this.editForm.resumeId) return
      if (!this.editForm.title || !String(this.editForm.title).trim()) {
        this.$message.error('简历标题不能为空')
        return
      }
      this.editSaving = true
      try {
        await updateResume(userId, this.editForm.resumeId, {
          title: this.editForm.title,
          content: this.editForm.content
        })
        this.$message.success('保存成功')
        this.editVisible = false
        await this.fetchData()
      } catch (e) {
        this.$message.error('保存失败')
      } finally {
        this.editSaving = false
      }
    },
    resolveDownloadHref(url) {
      const raw = url ? String(url) : ''
      if (!raw) return ''
      if (/^https?:\/\//i.test(raw)) return raw

      const baseApi = process && process.env && process.env.VUE_APP_BASE_API ? String(process.env.VUE_APP_BASE_API) : ''
      if (!baseApi || baseApi === '/') return raw

      const base = baseApi.endsWith('/') ? baseApi.slice(0, -1) : baseApi
      const path = raw.startsWith('/') ? raw : `/${raw}`
      return `${base}${path}`
    },
    resolveFileNameFromResponse(resp, fallbackFileName) {
      const fileName = fallbackFileName ? String(fallbackFileName) : 'resume.pdf'
      const disposition = resp && resp.headers ? (resp.headers['content-disposition'] || resp.headers['Content-Disposition']) : ''
      if (!disposition) {
        return fileName
      }

      const d = String(disposition)
      const matchStar = d.match(/filename\*=UTF-8''([^;]+)/i)
      if (matchStar && matchStar[1]) {
        try {
          return decodeURIComponent(matchStar[1])
        } catch (e) {
          return fileName
        }
      }

      const match = d.match(/filename=\"?([^\";]+)\"?/i)
      if (match && match[1]) {
        return match[1]
      }
      return fileName
    },
    sanitizeFileName(fileName) {
      const raw = fileName == null ? '' : String(fileName).trim()
      const safe = raw.replace(/[\\/:*?"<>|]+/g, '_').trim()
      return safe || 'resume.pdf'
    },
    buildUniqueFileName(fileName, usedNames) {
      if (!usedNames || !(usedNames instanceof Set)) return fileName
      if (!usedNames.has(fileName)) {
        usedNames.add(fileName)
        return fileName
      }

      const dotIndex = fileName.lastIndexOf('.')
      const hasExt = dotIndex > 0
      const base = hasExt ? fileName.slice(0, dotIndex) : fileName
      const ext = hasExt ? fileName.slice(dotIndex) : ''

      let i = 1
      let candidate = `${base}(${i})${ext}`
      while (usedNames.has(candidate)) {
        i += 1
        candidate = `${base}(${i})${ext}`
      }
      usedNames.add(candidate)
      return candidate
    },
    buildBatchZipName() {
      const now = new Date()
      const pad2 = (num) => String(num).padStart(2, '0')
      return `resumes_${now.getFullYear()}${pad2(now.getMonth() + 1)}${pad2(now.getDate())}_${pad2(now.getHours())}${pad2(now.getMinutes())}${pad2(now.getSeconds())}.zip`
    },
    buildFileTimeTag() {
      const now = new Date()
      const pad2 = (num) => String(num).padStart(2, '0')
      return `${now.getFullYear()}${pad2(now.getMonth() + 1)}${pad2(now.getDate())}_${pad2(now.getHours())}${pad2(now.getMinutes())}${pad2(now.getSeconds())}`
    },
    extractErrorReason(err) {
      if (!err) return '未知错误'
      const message = err && err.message ? String(err.message) : ''
      const payload = err && err.response && err.response.data ? err.response.data : null
      const payloadMsg = payload && (payload.msg || payload.message) ? String(payload.msg || payload.message) : ''
      if (payloadMsg) return payloadMsg
      if (message === 'NO_DOWNLOAD_URL') return '没有下载地址'
      if (message === 'EMPTY_RESPONSE') return '文件响应为空'
      return message || '请求失败'
    },
    escapeCsvCell(value) {
      const text = value == null ? '' : String(value)
      const escaped = text.replace(/"/g, '""')
      return `"${escaped}"`
    },
    toFailureTarget(row) {
      return {
        id: row && row.id != null ? row.id : null,
        resumeName: (row && row.resumeName) || '',
        fileName: (row && row.fileName) || '',
        downloadUrl: (row && row.downloadUrl) || ''
      }
    },
    buildFailureItem(row, err) {
      return {
        id: row && row.id != null ? row.id : '-',
        name: (row && (row.resumeName || row.fileName)) || '-',
        reason: this.extractErrorReason(err),
        target: this.toFailureTarget(row)
      }
    },
    openBatchResultDialog(title, items, exportName, action) {
      this.batchResultTitle = title || '批量结果'
      this.batchResultItems = Array.isArray(items) ? items : []
      this.batchResultExportName = exportName || `batch_failed_items_${this.buildFileTimeTag()}.csv`
      this.batchResultAction = action || ''
      this.batchResultVisible = true
    },
    async onClickRetryBatchResult() {
      if (!this.canRetryBatchResult) return
      if (this.batchResultAction === 'download') {
        await this.retryFailedDownloads()
        return
      }
      if (this.batchResultAction === 'delete') {
        await this.retryFailedDeletes()
      }
    },
    async retryFailedDownloads() {
      const failedRows = (this.batchResultItems || [])
        .map((it) => (it && it.target ? it.target : null))
        .filter((row) => row && row.downloadUrl)
      if (failedRows.length === 0) {
        this.$message.info('没有可重试下载项')
        return
      }

      this.batchResultRetrying = true
      this.batchDownloading = true
      this.batchDownloadProgress.total = failedRows.length
      this.batchDownloadProgress.done = 0

      try {
        const zip = new JSZip()
        const usedNames = new Set()
        let successCount = 0
        let failCount = 0
        const failedItems = []

        for (let i = 0; i < failedRows.length; i++) {
          const row = failedRows[i]
          try {
            const result = await this.fetchDownloadFile(
              row.downloadUrl,
              row.fileName || `resume-${row.id}.pdf`,
              { silent: true }
            )
            const uniqueName = this.buildUniqueFileName(result.fileName, usedNames)
            zip.file(uniqueName, result.blob)
            successCount += 1
          } catch (e) {
            failCount += 1
            failedItems.push(this.buildFailureItem(row, e))
          } finally {
            this.batchDownloadProgress.done = i + 1
          }
        }

        if (successCount > 0) {
          const zipBlob = await zip.generateAsync({ type: 'blob' })
          saveAs(zipBlob, `resumes_retry_${this.buildFileTimeTag()}.zip`)
        }

        if (failCount > 0) {
          if (successCount > 0) {
            this.$message.warning(`重试完成：成功 ${successCount} 个，仍失败 ${failCount} 个`)
          } else {
            this.$message.error('重试失败：没有文件可下载')
          }
          this.openBatchResultDialog('批量下载失败项', failedItems, `batch_download_failed_retry_${this.buildFileTimeTag()}.csv`, 'download')
        } else {
          this.$message.success(`重试成功：${successCount} 个文件`)
          this.batchResultVisible = false
        }
      } finally {
        this.batchDownloading = false
        this.batchResultRetrying = false
      }
    },
    async retryFailedDeletes() {
      const userId = this.getTargetUserId()
      if (!userId) return

      const failedRows = (this.batchResultItems || [])
        .map((it) => (it && it.target ? it.target : null))
        .filter((row) => row && row.id != null)
      if (failedRows.length === 0) {
        this.$message.info('没有可重试删除项')
        return
      }

      this.batchResultRetrying = true
      this.batchDeleting = true
      this.batchDeleteProgress.total = failedRows.length
      this.batchDeleteProgress.done = 0

      try {
        let successCount = 0
        let failCount = 0
        const failedItems = []

        for (let i = 0; i < failedRows.length; i++) {
          const row = failedRows[i]
          try {
            await deleteResume(userId, row.id)
            successCount += 1
          } catch (e) {
            failCount += 1
            failedItems.push(this.buildFailureItem(row, e))
          } finally {
            this.batchDeleteProgress.done = i + 1
          }
        }

        if (successCount > 0) {
          await this.fetchData()
        }

        if (failCount > 0) {
          if (successCount > 0) {
            this.$message.warning(`重试删除：成功 ${successCount} 个，仍失败 ${failCount} 个`)
          } else {
            this.$message.error('重试删除失败')
          }
          this.openBatchResultDialog('批量删除失败项', failedItems, `batch_delete_failed_retry_${this.buildFileTimeTag()}.csv`, 'delete')
        } else {
          this.$message.success(`重试删除成功：${successCount} 份简历`)
          this.batchResultVisible = false
        }
      } finally {
        this.batchDeleting = false
        this.batchResultRetrying = false
      }
    },
    onClickExportBatchResultCsv() {
      const items = Array.isArray(this.batchResultItems) ? this.batchResultItems : []
      if (items.length === 0) {
        this.$message.info('没有失败项')
        return
      }
      const header = ['ID', '简历', '原因']
      const lines = [header]
      items.forEach((it) => {
        lines.push([it && it.id, it && it.name, it && it.reason])
      })
      const csvText = `\uFEFF${lines.map((cols) => cols.map((c) => this.escapeCsvCell(c)).join(',')).join('\r\n')}`
      const blob = new Blob([csvText], { type: 'text/csv;charset=utf-8;' })
      saveAs(blob, this.batchResultExportName || `batch_failed_items_${this.buildFileTimeTag()}.csv`)
    },
    async fetchDownloadFile(url, fallbackFileName, options) {
      const opts = options || {}
      const silent = Boolean(opts.silent)
      const href = this.resolveDownloadHref(url)
      if (!href) {
        if (!silent) this.$message.info('没有下载地址')
        throw new Error('NO_DOWNLOAD_URL')
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
        if (!silent) this.$message.error('下载失败')
        throw e
      }

      const blob = resp && resp.data ? resp.data : null
      if (!blob) {
        if (!silent) this.$message.error('下载失败：响应为空')
        throw new Error('EMPTY_RESPONSE')
      }

      const fileName = this.sanitizeFileName(this.resolveFileNameFromResponse(resp, fallbackFileName))
      return { blob, fileName }
    },
    async downloadByUrl(url, fallbackFileName) {
      let result
      try {
        result = await this.fetchDownloadFile(url, fallbackFileName)
      } catch (e) {
        return
      }

      try {
        saveAs(result.blob, result.fileName)
      } catch (e) {
        this.$message.error('下载失败：浏览器不支持')
      }
    },
    async onClickDownloadRow(row) {
      if (!row) return
      if (!row.downloadUrl) {
        this.$message.info('当前行没有可下载文件')
        return
      }
      await this.downloadByUrl(row.downloadUrl, row.fileName || 'resume.pdf')
    },
    async onClickBatchDownload() {
      const rows = this.selectedRows.filter((row) => row && row.downloadUrl)
      if (rows.length === 0) {
        this.$message.info('选中项中没有可下载文件')
        return
      }
      this.batchDownloading = true
      this.batchDownloadProgress.total = rows.length
      this.batchDownloadProgress.done = 0
      try {
        const zip = new JSZip()
        const usedNames = new Set()
        let successCount = 0
        let failCount = 0
        const failedItems = []

        for (let i = 0; i < rows.length; i++) {
          const row = rows[i]
          try {
            const result = await this.fetchDownloadFile(
              row.downloadUrl,
              row.fileName || `resume-${row.id}.pdf`,
              { silent: true }
            )
            const uniqueName = this.buildUniqueFileName(result.fileName, usedNames)
            zip.file(uniqueName, result.blob)
            successCount += 1
          } catch (e) {
            failCount += 1
            failedItems.push(this.buildFailureItem(row, e))
          } finally {
            this.batchDownloadProgress.done = i + 1
          }
        }

        if (successCount === 0) {
          this.$message.error('批量下载失败：没有文件可下载')
          if (failedItems.length > 0) {
            this.openBatchResultDialog('批量下载失败项', failedItems, `batch_download_failed_${this.buildFileTimeTag()}.csv`, 'download')
          }
          return
        }

        const zipBlob = await zip.generateAsync({ type: 'blob' })
        saveAs(zipBlob, this.buildBatchZipName())

        if (failCount > 0) {
          this.$message.warning(`批量下载完成：成功 ${successCount} 个，失败 ${failCount} 个`)
          this.openBatchResultDialog('批量下载失败项', failedItems, `batch_download_failed_${this.buildFileTimeTag()}.csv`, 'download')
        } else {
          this.$message.success(`批量下载完成：${successCount} 个文件`)
        }
      } finally {
        this.batchDownloading = false
      }
    },
    isDeleting(id) {
      const key = String(id)
      return Boolean(this.deletingMap && this.deletingMap[key])
    },
    setDeleting(id, deleting) {
      const key = String(id)
      this.$set(this.deletingMap, key, Boolean(deleting))
    },
    onClickDeleteRow(row) {
      if (!this.canManageResume) {
        this.$message.warning('没有删除简历权限')
        return
      }
      if (!row || row.id == null) return

      this.$confirm('确认删除这份简历吗？本地文件也会一并删除。', '确认操作', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async() => {
        const userId = this.getTargetUserId()
        if (!userId) return
        this.setDeleting(row.id, true)
        try {
          await deleteResume(userId, row.id)
          this.$message.success('删除成功')
          if (this.rows.length === 1 && this.pageNo > 1) {
            this.pageNo = this.pageNo - 1
          }
          await this.fetchData()
        } catch (e) {
          this.$message.error('删除失败')
        } finally {
          this.setDeleting(row.id, false)
        }
      }).catch(() => {})
    },
    async onClickBatchDelete() {
      if (!this.canManageResume) {
        this.$message.warning('没有删除简历权限')
        return
      }
      const targets = this.selectedRows
      if (targets.length === 0) {
        this.$message.info('请先选择数据')
        return
      }

      this.$confirm(`确认删除选中的 ${targets.length} 份简历吗？本地文件也会一并删除。`, '确认操作', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async() => {
        const userId = this.getTargetUserId()
        if (!userId) return
        this.batchDeleting = true
        this.batchDeleteProgress.total = targets.length
        this.batchDeleteProgress.done = 0

        let successCount = 0
        let failCount = 0
        const failedItems = []
        for (let i = 0; i < targets.length; i++) {
          const row = targets[i]
          if (!row || row.id == null) continue
          this.setDeleting(row.id, true)
          try {
            await deleteResume(userId, row.id)
            successCount++
          } catch (e) {
            failCount++
            failedItems.push(this.buildFailureItem(row, e))
          } finally {
            this.setDeleting(row.id, false)
            this.batchDeleteProgress.done = i + 1
          }
        }

        this.batchDeleting = false
        this.selectedIds = []
        if (this.$refs.table && this.$refs.table.clearSelection) {
          this.$refs.table.clearSelection()
        }

        if (successCount > 0 && failCount === 0) {
          this.$message.success(`已删除 ${successCount} 份简历`)
        } else if (successCount > 0) {
          this.$message.warning(`已删除 ${successCount} 份，失败 ${failCount} 份`)
          this.openBatchResultDialog('批量删除失败项', failedItems, `batch_delete_failed_${this.buildFileTimeTag()}.csv`, 'delete')
        } else {
          this.$message.error('批量删除失败')
          this.openBatchResultDialog('批量删除失败项', failedItems, `batch_delete_failed_${this.buildFileTimeTag()}.csv`, 'delete')
        }

        if (this.rows.length <= successCount && this.pageNo > 1) {
          this.pageNo = this.pageNo - 1
        }
        await this.fetchData()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.page {
  padding: 16px;
}

.user-tip {
  margin-bottom: 12px;
}

.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.summary-card {
  padding: 14px 16px;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.06);
  border: 1px solid #edf2f7;
}

.summary-label {
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}

.summary-value {
  margin-bottom: 6px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.summary-sub {
  font-size: 12px;
  color: #a0aec0;
}

.search {
  width: 260px;
}

.download-progress {
  color: #606266;
  font-size: 12px;
}

.batch-result-toolbar {
  margin-bottom: 8px;
  color: #606266;
  font-size: 12px;
}

.title {
  color: #303133;
  font-weight: 600;
}

.summary-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.cell-link {
  color: #409eff;
  cursor: pointer;
}

.cell-link:hover {
  text-decoration: underline;
}

.danger-text {
  color: #f56c6c;
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

.preview-body {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  max-height: 560px;
  overflow: auto;
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
