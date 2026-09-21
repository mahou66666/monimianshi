<template>
  <div class="page">
    <el-tabs v-model="activeCategory" @tab-click="onTabChange">
      <el-tab-pane label="实习问题" name="internship" />
      <el-tab-pane label="项目问题" name="project" />
      <el-tab-pane label="教育问题" name="education" />
      <el-tab-pane label="证书问题" name="certificate" />
      <el-tab-pane label="JD问题" name="jd" />
    </el-tabs>

    <div class="toolbar">
      <el-button type="primary" size="small" @click="openGenerateDialog">生成</el-button>

      <el-select v-model="query.model" size="small" class="model-select">
        <el-option label="DS" value="ds" />
        <el-option label="SD" value="sd" />
        <el-option label="豆包" value="doubao" />
        <el-option label="Kimi" value="kimi" />
      </el-select>

      <div class="divider" />

      <el-input
        v-model="query.keyword"
        placeholder="搜索关键词"
        clearable
        size="small"
        class="search"
        @keyup.enter.native="onSearch"
      />

      <el-select v-model="query.status" placeholder="状态" clearable size="small" class="filter">
        <el-option label="有效" :value="1" />
        <el-option label="无效" :value="2" />
      </el-select>

      <el-button size="small" @click="onSearch">查询</el-button>
      <el-button size="small" @click="onReset">重置</el-button>
      <el-button size="small" @click="fetchData">刷新</el-button>
    </div>

    <div class="summary-grid">
      <div v-for="card in summaryCards" :key="card.label" class="summary-card">
        <div class="summary-label">{{ card.label }}</div>
        <div class="summary-value">{{ card.value }}</div>
        <div class="summary-sub">{{ card.sub }}</div>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="90" />

      <el-table-column label="问题内容" min-width="220">
        <template slot-scope="scope">
          <span
            v-if="scope.row.questionContent"
            class="cell-link"
            @click.stop="openPreview('问题内容', scope.row.questionContent)"
          >{{ truncate(scope.row.questionContent, 40) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>

      <el-table-column label="问题答案" min-width="240">
        <template slot-scope="scope">
          <span
            v-if="scope.row.answerContent"
            class="cell-link"
            @click.stop="openPreview('问题答案', scope.row.answerContent)"
          >{{ truncate(scope.row.answerContent, 46) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>

      <el-table-column label="属性" min-width="180">
        <template slot-scope="scope">
          <span>{{ propertyText(scope.row) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="题库" min-width="140">
        <template slot-scope="scope">
          <span>{{ scope.row.bankName || scope.row.bankId || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="关键词" min-width="160">
        <template slot-scope="scope">
          <span>{{ scope.row.keywords || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="90">
        <template slot-scope="scope">
          <el-tag :type="Number(scope.row.status) === 1 ? 'success' : 'info'" size="mini">
            {{ Number(scope.row.status) === 1 ? '有效' : '无效' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="DS详情" width="110">
        <template slot-scope="scope">
          <el-tag size="mini" :type="detailTagType(scope.row.ds)">{{ detailTagText(scope.row.ds) }}</el-tag>
          <el-button type="text" size="mini" @click="openDetail(scope.row, 'ds')">查看</el-button>
        </template>
      </el-table-column>

      <el-table-column label="SD详情" width="110">
        <template slot-scope="scope">
          <el-tag size="mini" :type="detailTagType(scope.row.sd)">{{ detailTagText(scope.row.sd) }}</el-tag>
          <el-button type="text" size="mini" @click="openDetail(scope.row, 'sd')">查看</el-button>
        </template>
      </el-table-column>

      <el-table-column label="豆包详情" width="110">
        <template slot-scope="scope">
          <el-tag size="mini" :type="detailTagType(scope.row.doubao)">{{ detailTagText(scope.row.doubao) }}</el-tag>
          <el-button type="text" size="mini" @click="openDetail(scope.row, 'doubao')">查看</el-button>
        </template>
      </el-table-column>

      <el-table-column label="Kimi详情" width="110">
        <template slot-scope="scope">
          <el-tag size="mini" :type="detailTagType(scope.row.kimi)">{{ detailTagText(scope.row.kimi) }}</el-tag>
          <el-button type="text" size="mini" @click="openDetail(scope.row, 'kimi')">查看</el-button>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="140" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="openEditDialog(scope.row)">编辑</el-button>
          <el-button type="text" size="mini" @click="onDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && list.length === 0" class="empty">暂无数据</div>

    <div class="pager">
      <el-pagination
        background
        layout="total, prev, pager, next, sizes"
        :current-page.sync="pagination.pageNo"
        :page-size.sync="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <el-dialog
      title="批量生成"
      :visible.sync="generateVisible"
      width="520px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form label-width="100px">
        <el-form-item label="模型">
          <el-radio-group v-model="generateForm.model">
            <el-radio label="ds">DS</el-radio>
            <el-radio label="sd">SD</el-radio>
            <el-radio label="doubao">豆包</el-radio>
            <el-radio label="kimi">Kimi</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="generateForm.userId" placeholder="必填" />
        </el-form-item>
        <el-form-item label="简历ID">
          <el-input v-model="generateForm.resumeId" placeholder="可选" />
        </el-form-item>
        <el-form-item :label="relatedIdLabel">
          <el-input v-model="generateForm.relatedId" :placeholder="`请输入${relatedIdLabel}`" />
        </el-form-item>
        <el-form-item label="生成数量">
          <el-input-number v-model="generateForm.count" :min="1" :max="50" controls-position="right" />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="onConfirmGenerate">开始生成</el-button>
      </span>
    </el-dialog>

    <el-dialog
      title="编辑问题"
      :visible.sync="editVisible"
      width="680px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form label-width="90px">
        <el-form-item label="问题">
          <el-input v-model="editForm.questionContent" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="答案">
          <el-input v-model="editForm.answerContent" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="属性">
          <el-input v-model="editForm.questionAttr" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="editForm.keywords" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :label="1">有效</el-radio>
            <el-radio :label="2">无效</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editing" @click="onConfirmEdit">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog :title="detailTitle" :visible.sync="detailVisible" width="720px" append-to-body>
      <div v-if="detailQuestions.length === 0" class="dialog-empty">暂无详情</div>
      <el-collapse v-else accordion>
        <el-collapse-item
          v-for="(qa, idx) in detailQuestions"
          :key="idx"
          :title="`Q${idx + 1}: ${qa.question || '-'}`"
        >
          <div class="answer">{{ qa.answer || '-' }}</div>
        </el-collapse-item>
      </el-collapse>
      <span slot="footer" class="dialog-footer">
        <el-button @click="detailVisible = false">关闭</el-button>
      </span>
    </el-dialog>

    <el-dialog :title="previewTitle" :visible.sync="previewVisible" width="720px" append-to-body>
      <div class="preview-body">{{ previewContent }}</div>
    </el-dialog>
  </div>
</template>

<script>
import {
  deleteGeneralQuestion,
  fetchGeneralQuestionList,
  generateGeneralQuestions,
  updateGeneralQuestion
} from '@/api/interviewelf/generalQuestions'

export default {
  name: 'GeneralQuestions',
  data() {
    return {
      activeCategory: 'internship',
      query: {
        keyword: '',
        status: '',
        model: 'ds'
      },
      list: [],
      loading: false,
      pagination: {
        pageNo: 1,
        pageSize: 10,
        total: 0
      },
      generateVisible: false,
      generating: false,
      generateForm: {
        model: 'ds',
        userId: '',
        resumeId: '',
        relatedId: '',
        count: 10
      },
      editVisible: false,
      editing: false,
      editForm: {
        id: null,
        questionContent: '',
        answerContent: '',
        questionAttr: '',
        keywords: '',
        status: 1
      },
      detailVisible: false,
      detailTitle: '详情',
      detailQuestions: [],
      previewVisible: false,
      previewTitle: '',
      previewContent: ''
    }
  },
  computed: {
    activeCategoryLabel() {
      const map = {
        internship: '实习问题',
        project: '项目问题',
        education: '教育问题',
        certificate: '证书问题',
        jd: 'JD问题'
      }
      return map[this.activeCategory] || '问题'
    },
    relatedIdLabel() {
      if (this.activeCategory === 'internship') return '实习ID'
      if (this.activeCategory === 'project') return '项目ID'
      if (this.activeCategory === 'education') return '教育ID'
      if (this.activeCategory === 'certificate') return '证书ID'
      if (this.activeCategory === 'jd') return 'JD ID'
      return '关联ID'
    },
    validCount() {
      return (this.list || []).filter(row => row && Number(row.status) === 1).length
    },
    answeredCount() {
      return (this.list || []).filter((row) => {
        const text = row && row.answerContent != null ? String(row.answerContent).trim() : ''
        return !!text
      }).length
    },
    summaryCards() {
      return [
        {
          label: this.activeCategoryLabel,
          value: this.list.length,
          sub: `题库总计 ${this.pagination.total}`
        },
        {
          label: '有效题目',
          value: this.validCount,
          sub: '当前页可直接展示'
        },
        {
          label: '已有答案',
          value: this.answeredCount,
          sub: '便于现场展开查看'
        },
        {
          label: '当前模型',
          value: String(this.query.model || 'ds').toUpperCase(),
          sub: '生成操作将沿用此模型'
        }
      ]
    }
  },
  mounted() {
    this.fetchData()
  },
  methods: {
    truncate(text, maxLen = 40) {
      const s = text == null ? '' : String(text)
      if (!s) return ''
      if (s.length <= maxLen) return s
      return s.slice(0, maxLen) + '...'
    },
    propertyText(row) {
      const r = row || {}
      if (r.questionAttr) return String(r.questionAttr)
      const parts = []
      if (r.questionType != null) parts.push(`题型:${r.questionType}`)
      if (r.difficulty != null) parts.push(`难度:${r.difficulty}`)
      if (r.isCommon != null) parts.push(`高频:${Number(r.isCommon) === 1 ? '是' : '否'}`)
      if (r.status != null) parts.push(`状态:${Number(r.status) === 1 ? '有效' : Number(r.status) === 2 ? '无效' : r.status}`)
      return parts.length ? parts.join(' | ') : '-'
    },
    detailTagText(detail) {
      const items = detail && detail.items ? detail.items : []
      if (!items || items.length === 0) return '暂无'
      return `${items.length}题`
    },
    detailTagType(detail) {
      const items = detail && detail.items ? detail.items : []
      return items && items.length > 0 ? 'success' : 'info'
    },
    openPreview(title, content) {
      this.previewTitle = title || '内容预览'
      this.previewContent = content == null ? '' : String(content)
      this.previewVisible = true
    },
    openDetail(row, modelKey) {
      const map = {
        ds: 'DS详情',
        sd: 'SD详情',
        doubao: '豆包详情',
        kimi: 'Kimi详情'
      }
      this.detailTitle = `${row && row.bankName ? row.bankName : ''} - ${map[modelKey] || '详情'}`
      const detail = row ? row[modelKey] : null
      this.detailQuestions = Array.isArray(detail && detail.items) ? detail.items : []
      this.detailVisible = true
      if (this.detailQuestions.length === 0) {
        this.$message.info('暂无详情数据')
      }
    },
    getCurrentUserId() {
      try {
        const raw = window.localStorage.getItem('UserInfo')
        const obj = raw ? JSON.parse(raw) : null
        return obj && obj.userId ? String(obj.userId) : ''
      } catch (e) {
        return ''
      }
    },
    openGenerateDialog() {
      this.generateForm = {
        model: this.query.model || 'ds',
        userId: this.getCurrentUserId(),
        resumeId: '',
        relatedId: '',
        count: 10
      }
      this.generateVisible = true
    },
    async onConfirmGenerate() {
      if (!this.generateForm.userId) {
        this.$message.error('请填写userId')
        return
      }
      if (!this.generateForm.relatedId) {
        this.$message.error(`请填写${this.relatedIdLabel}`)
        return
      }

      this.generating = true
      try {
        await generateGeneralQuestions(this.activeCategory, { ...this.generateForm })
        this.$message.success('生成成功')
        this.generateVisible = false
        await this.fetchData()
      } catch (e) {
        this.$message.error('生成失败')
      } finally {
        this.generating = false
      }
    },
    openEditDialog(row) {
      this.editForm = {
        id: row.id,
        questionContent: row.questionContent || '',
        answerContent: row.answerContent || '',
        questionAttr: row.questionAttr || '',
        keywords: row.keywords || '',
        status: row.status == null ? 1 : Number(row.status)
      }
      this.editVisible = true
    },
    async onConfirmEdit() {
      if (!this.editForm.id) return
      if (!this.editForm.questionContent || !String(this.editForm.questionContent).trim()) {
        this.$message.error('问题内容不能为空')
        return
      }
      this.editing = true
      try {
        await updateGeneralQuestion(this.activeCategory, this.editForm.id, {
          questionContent: this.editForm.questionContent,
          answerContent: this.editForm.answerContent,
          questionAttr: this.editForm.questionAttr,
          keywords: this.editForm.keywords,
          status: this.editForm.status
        })
        this.$message.success('保存成功')
        this.editVisible = false
        await this.fetchData()
      } catch (e) {
        this.$message.error('保存失败')
      } finally {
        this.editing = false
      }
    },
    onDelete(row) {
      if (!row || !row.id) return
      this.$confirm('确认删除该问题吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async() => {
        try {
          await deleteGeneralQuestion(this.activeCategory, row.id)
          this.$message.success('删除成功')
          await this.fetchData()
        } catch (e) {
          this.$message.error('删除失败')
        }
      }).catch(() => {})
    },
    async fetchData() {
      this.loading = true
      try {
        const params = {
          pageNo: this.pagination.pageNo,
          pageSize: this.pagination.pageSize
        }
        if (this.query.keyword) params.keyword = this.query.keyword
        if (this.query.status !== '' && this.query.status != null) params.status = this.query.status

        const data = await fetchGeneralQuestionList(this.activeCategory, params)
        this.list = data && data.items ? data.items : []
        this.pagination.total = data && data.total != null ? data.total : 0
      } catch (e) {
        this.list = []
        this.pagination.total = 0
        this.$message.error('获取问题列表失败')
      } finally {
        this.loading = false
      }
    },
    onTabChange() {
      this.pagination.pageNo = 1
      this.fetchData()
    },
    onSearch() {
      this.pagination.pageNo = 1
      this.fetchData()
    },
    onReset() {
      this.query.keyword = ''
      this.query.status = ''
      this.pagination.pageNo = 1
      this.fetchData()
    },
    onPageChange() {
      this.fetchData()
    },
    onSizeChange() {
      this.pagination.pageNo = 1
      this.fetchData()
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

.search {
  width: 260px;
}

.filter {
  width: 120px;
}

.model-select {
  width: 120px;
}

.empty {
  text-align: center;
  color: #909399;
  margin-top: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.cell-link {
  color: #409eff;
  cursor: pointer;
}

.cell-link:hover {
  text-decoration: underline;
}

.dialog-empty {
  text-align: center;
  color: #909399;
  padding: 24px 0;
}

.answer {
  white-space: pre-wrap;
  line-height: 20px;
}

.preview-body {
  white-space: pre-wrap;
  word-break: break-word;
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
