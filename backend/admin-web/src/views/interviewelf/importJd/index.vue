<template>
  <div class="page">
    <div class="toolbar search-toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索岗位名称 / 描述"
        clearable
        style="width: 280px"
        @keyup.enter.native="onSearch"
      />
      <el-select v-model="query.jobType" placeholder="岗位类型" clearable style="width: 150px">
        <el-option label="校招" :value="1" />
        <el-option label="社招" :value="2" />
        <el-option label="实习" :value="3" />
        <el-option label="其他" :value="4" />
      </el-select>
      <el-button type="primary" @click="onSearch">搜索</el-button>
      <el-button @click="onReset">重置</el-button>
      <div class="spacer" />
      <el-button type="primary" @click="openImportDialog">导入JD</el-button>
      <el-button :disabled="selectedIds.length === 0" @click="onClickBatchExtract">批量提取</el-button>
    </div>

    <el-table
      ref="table"
      v-loading="loading"
      :data="list"
      border
      style="width: 100%"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="52" fixed="left" />
      <el-table-column prop="id" label="ID" width="90" fixed="left" />
      <el-table-column prop="jobTitle" label="岗位名称" min-width="180" fixed="left" />

      <el-table-column prop="jobType" label="岗位类型" width="110">
        <template slot-scope="scope">
          <span>{{ jobTypeText(scope.row.jobType) }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="city" label="工作城市" width="120">
        <template slot-scope="scope">
          <span>{{ scope.row.city || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="salary" label="薪资" width="140">
        <template slot-scope="scope">
          <span>{{ scope.row.salary || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="岗位描述" min-width="300">
        <template slot-scope="scope">
          <span
            v-if="scope.row.jobDesc"
            class="cell-link"
            @click.stop="openPreview('岗位描述', scope.row.jobDesc)"
          >{{ truncate(scope.row.jobDesc, 50) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>

      <el-table-column label="岗位要求" min-width="300">
        <template slot-scope="scope">
          <span
            v-if="scope.row.jobReq"
            class="cell-link"
            @click.stop="openPreview('岗位要求', scope.row.jobReq)"
          >{{ truncate(scope.row.jobReq, 50) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!list || list.length === 0" class="empty">暂无数据</div>

    <div class="pager">
      <el-pagination
        background
        layout="total, prev, pager, next, sizes"
        :current-page.sync="pagination.pageNo"
        :page-size.sync="pagination.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        @current-change="fetchData"
        @size-change="onSizeChange"
      />
    </div>

    <el-dialog
      title="批量导入JD"
      :visible.sync="importDialogVisible"
      width="720px"
      append-to-body
    >
      <el-alert
        title="支持粘贴多条 JD 文本，建议一段一条，或用空行分隔。"
        type="info"
        show-icon
        :closable="false"
        style="margin-bottom: 12px"
      />
      <el-input
        v-model="importText"
        type="textarea"
        :rows="12"
        placeholder="在此粘贴 JD 文本..."
      />
      <span slot="footer" class="dialog-footer">
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onConfirmImport">确认导入</el-button>
      </span>
    </el-dialog>

    <el-dialog
      :title="previewTitle"
      :visible.sync="previewVisible"
      width="720px"
      append-to-body
    >
      <div class="preview-body">{{ previewContent }}</div>
    </el-dialog>
  </div>
</template>

<script>
import { extractJdBatch, fetchJdList, importJdTextBatch } from '@/api/interviewelf/jd'

export default {
  name: 'ImportJd',
  data() {
    return {
      query: {
        keyword: '',
        jobType: ''
      },
      list: [],
      selectedIds: [],
      importDialogVisible: false,
      importText: '',
      loading: false,
      previewVisible: false,
      previewTitle: '',
      previewContent: '',
      pagination: {
        pageNo: 1,
        pageSize: 10,
        total: 0
      }
    }
  },
  mounted() {
    this.fetchData()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const params = {
          pageNo: this.pagination.pageNo,
          pageSize: this.pagination.pageSize
        }
        if (this.query.keyword) {
          params.keyword = this.query.keyword
        }
        if (this.query.jobType) {
          params.jobType = this.query.jobType
        }
        const data = await fetchJdList(params)
        this.list = data.items || []
        this.pagination.total = data.total || 0
        this.selectedIds = []
        if (this.$refs.table && this.$refs.table.clearSelection) {
          this.$refs.table.clearSelection()
        }
      } catch (error) {
        console.error('获取JD列表失败:', error)
        this.$message.error('获取JD列表失败')
      } finally {
        this.loading = false
      }
    },
    onSearch() {
      this.pagination.pageNo = 1
      this.fetchData()
    },
    onReset() {
      this.query = { keyword: '', jobType: '' }
      this.selectedIds = []
      this.pagination.pageNo = 1
      this.fetchData()
    },
    onSizeChange(size) {
      this.pagination.pageSize = size
      this.pagination.pageNo = 1
      this.fetchData()
    },
    onSelectionChange(rows) {
      this.selectedIds = (rows || []).map(row => row && row.id).filter(id => id != null)
    },
    jobTypeText(v) {
      const value = String(v == null ? '' : v).toLowerCase()
      if (value === 'campus' || value === '1') return '校招'
      if (value === 'social' || value === '2') return '社招'
      if (value === 'internship' || value === '3') return '实习'
      if (value === '4') return '其他'
      return '-'
    },
    openImportDialog() {
      this.importDialogVisible = true
    },
    async onConfirmImport() {
      const text = String(this.importText || '').trim()
      if (!text) {
        this.$message.warning('请先粘贴 JD 文本')
        return
      }
      try {
        const data = await importJdTextBatch(text)
        const createdCount = Number(data && data.createdCount ? data.createdCount : 0)
        const duplicateCount = Number(data && data.duplicateCount ? data.duplicateCount : 0)
        const skippedCount = Number(data && data.skippedCount ? data.skippedCount : 0)
        this.$message.success(`导入成功：新增 ${createdCount} 条，重复 ${duplicateCount} 条，跳过 ${skippedCount} 条`)
        this.importDialogVisible = false
        this.importText = ''
        this.selectedIds = []
        this.pagination.pageNo = 1
        await this.fetchData()
      } catch (e) {
        this.$message.error('导入JD失败')
      }
    },
    async onClickBatchExtract() {
      if (!this.selectedIds || this.selectedIds.length === 0) {
        this.$message.warning('请先选择 JD 行')
        return
      }
      try {
        const data = await extractJdBatch(this.selectedIds)
        const updatedCount = Number(data && data.updatedCount ? data.updatedCount : 0)
        const skippedCount = Number(data && data.skippedCount ? data.skippedCount : 0)
        const missingCount = Number(data && data.missingCount ? data.missingCount : 0)
        this.$message.success(`提取完成：更新 ${updatedCount} 条，跳过 ${skippedCount} 条，缺失 ${missingCount} 条`)
        await this.fetchData()
      } catch (e) {
        this.$message.error('批量提取失败')
      }
    },
    onClickBatchRetry() {
      this.$message.info('重试失败：暂未接入接口')
    },
    truncate(text, maxLen = 50) {
      const s = String(text || '')
      if (!s) return ''
      if (s.length <= maxLen) return s
      return s.slice(0, maxLen) + '...'
    },
    openPreview(title, content) {
      this.previewTitle = title || '内容预览'
      this.previewContent = content == null ? '' : String(content)
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
  gap: 12px;
  margin-bottom: 12px;
}

.search-toolbar {
  justify-content: flex-start;
}

.spacer {
  flex: 1;
}

.empty {
  text-align: center;
  color: #909399;
  margin-top: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.cell-link {
  color: #409eff;
  cursor: pointer;
}

.cell-link:hover {
  text-decoration: underline;
}

.preview-body {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  max-height: 500px;
  overflow-y: auto;
}
</style>
