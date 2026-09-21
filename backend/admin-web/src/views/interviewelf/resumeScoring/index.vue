<template>
  <div class="page">
    <el-alert
      class="user-tip"
      :title="activeUserTip"
      type="info"
      :closable="false"
      show-icon
    />

    <div class="topbar">
      <el-alert
        class="readonly-tip"
        title="当前页面用于展示该用户已经生成的评分结果，适合后台查看、对比和录屏演示。"
        type="info"
        :closable="false"
        show-icon
      />

      <div class="search">
        <el-input
          v-model="keyword"
          placeholder="输入简历 ID 或简历名"
          size="small"
          class="search-input"
          clearable
          @keyup.enter.native="onClickSearch"
        />
        <el-button size="small" type="primary" @click="onClickSearch">搜索</el-button>
        <el-button size="small" @click="onClickReset">重置</el-button>
      </div>
    </div>

    <div class="summary-grid">
      <div v-for="card in summaryCards" :key="card.label" class="summary-card">
        <div class="summary-label">{{ card.label }}</div>
        <div class="summary-value">{{ card.value }}</div>
        <div class="summary-sub">{{ card.sub }}</div>
      </div>
    </div>

    <div class="layout">
      <div class="left">
        <el-table
          ref="table"
          v-loading="listLoading"
          :data="rows"
          border
          row-key="id"
          height="520"
          highlight-current-row
          :current-row-key="activeRowId"
          empty-text="暂无评分结果"
          @row-click="onRowClick"
        >
          <el-table-column prop="id" label="简历ID" width="90" />
          <el-table-column prop="resumeName" label="简历名" min-width="180" show-overflow-tooltip />
          <el-table-column prop="totalScore" label="总分" width="90" />
          <el-table-column prop="generateCount" label="生成次数" width="100" />
          <el-table-column prop="latestScoreTime" label="最近生成时间" min-width="170" />
          <el-table-column label="五维评分" min-width="280">
            <template slot-scope="scope">
              <div class="score-tag-wrap">
                <el-tag
                  v-for="dimension in dimensions"
                  :key="dimension"
                  size="mini"
                  :type="scoreTagType(scope.row.scores[dimension])"
                  class="score-tag"
                >
                  {{ dimension }} {{ scope.row.scores[dimension] }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click.stop="openDetail(scope.row.id)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="right">
        <el-card class="chart-card">
          <div slot="header" class="card-header">
            <span>评分五维图</span>
            <span v-if="activeRow" class="card-subtitle">{{ activeRow.resumeName }}</span>
          </div>
          <div v-if="activeRow" class="active-overview">
            <div class="active-overview-item">
              <span class="overview-label">当前简历</span>
              <span class="overview-value">{{ activeRow.resumeName }}</span>
            </div>
            <div class="active-overview-item">
              <span class="overview-label">总分</span>
              <span class="overview-value">{{ activeRow.totalScore }}</span>
            </div>
            <div class="active-overview-item">
              <span class="overview-label">生成次数</span>
              <span class="overview-value">{{ activeRow.generateCount }}</span>
            </div>
          </div>
          <radar-chart :dimensions="dimensions" :scores="activeRowScores" height="420px" />
          <div v-if="!activeRow" class="chart-empty">
            暂无评分数据。可先切换到有演示数据的用户查看结果。
          </div>
        </el-card>
      </div>
    </div>

    <el-pagination
      class="pager"
      :current-page="pageNo"
      :page-size="pageSize"
      :page-sizes="[20, 50, 100, 200]"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      @size-change="onSizeChange"
      @current-change="onCurrentChange"
    />

    <el-dialog title="评分详情" :visible.sync="detailOpen" width="620px">
      <div class="detail-sub">
        <span>简历 ID：{{ detailResumeId || '-' }}</span>
        <span class="detail-total">总分：{{ detailTotalScore }}</span>
      </div>

      <el-row :gutter="12" class="detail-score-row">
        <el-col v-for="dimension in dimensions" :key="dimension" :span="8">
          <div class="detail-item">
            <div class="detail-item-label">{{ dimension }}</div>
            <div class="detail-item-value">{{ detailScores[dimension] }}</div>
          </div>
        </el-col>
      </el-row>

      <div class="detail-block">
        <div class="detail-title">分析</div>
        <div class="detail-text">{{ detailDimensionAnalysis || '暂无分析内容' }}</div>
      </div>

      <div class="detail-block">
        <div class="detail-title">问题</div>
        <div v-if="detailProblems.length > 0" class="detail-list">
          <div v-for="(item, index) in detailProblems" :key="index" class="detail-list-item">{{ index + 1 }}. {{ item }}</div>
        </div>
        <div v-else class="detail-text">暂无问题清单</div>
      </div>

      <div class="detail-block">
        <div class="detail-title">建议</div>
        <div class="detail-text">{{ detailAdvice || '暂无建议' }}</div>
      </div>

      <span slot="footer" class="dialog-footer">
        <el-button @click="detailOpen = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import RadarChart from './components/RadarChart'
import { fetchLatestResumeScore, fetchResumeScoreList } from '@/api/interviewelf/resumeScore'

const DIMENSIONS = ['教育', '工作', '项目', '技能', '获奖']

export default {
  name: 'ResumeScoringPage',
  components: {
    RadarChart
  },
  data() {
    return {
      dimensions: DIMENSIONS,
      rows: [],
      keyword: '',
      pageNo: 1,
      pageSize: 20,
      total: 0,
      activeRowId: null,
      listLoading: false,
      detailOpen: false,
      detailResumeId: null,
      detailTotalScore: 0,
      detailScores: this.buildEmptyScores(),
      detailDimensionAnalysis: '',
      detailProblems: [],
      detailAdvice: ''
    }
  },
  computed: {
    activeUserTip() {
      const userId = this.getTargetUserId()
      if (!userId) {
        return '未获取到当前用户，请重新登录后再试。'
      }
      const routeName = this.$route && this.$route.query ? this.$route.query.userName : ''
      const suffix = routeName ? `（${routeName}）` : ''
      return `当前查看用户：${userId}${suffix}。这里只展示该用户已有的评分结果。`
    },
    activeRow() {
      return this.rows.find(item => item.id === this.activeRowId) || null
    },
    activeRowScores() {
      return this.activeRow ? this.activeRow.scores : this.buildEmptyScores()
    },
    averageScore() {
      if (!Array.isArray(this.rows) || this.rows.length === 0) return 0
      const total = this.rows.reduce((sum, item) => sum + Number(item.totalScore || 0), 0)
      return Math.round(total / this.rows.length)
    },
    highestScore() {
      if (!Array.isArray(this.rows) || this.rows.length === 0) return 0
      return this.rows.reduce((max, item) => Math.max(max, Number(item.totalScore || 0)), 0)
    },
    latestScoreTime() {
      if (!Array.isArray(this.rows) || this.rows.length === 0) return '暂无'
      return this.rows[0].latestScoreTime || '暂无'
    },
    summaryCards() {
      return [
        {
          label: '评分简历数',
          value: this.total,
          sub: '当前筛选结果'
        },
        {
          label: '平均分',
          value: this.averageScore,
          sub: '便于展示整体质量'
        },
        {
          label: '最高分',
          value: this.highestScore,
          sub: '当前用户最佳结果'
        },
        {
          label: '最近生成',
          value: this.latestScoreTime,
          sub: '按时间倒序展示'
        }
      ]
    }
  },
  watch: {
    '$route.query.userId': function() {
      this.pageNo = 1
      this.fetchList()
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    buildEmptyScores() {
      return {
        教育: 0,
        工作: 0,
        项目: 0,
        技能: 0,
        获奖: 0
      }
    },
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
        if (id) return id
      }
      return null
    },
    getTargetUserId() {
      const routeValue = this.$route && this.$route.query ? this.$route.query.userId : null
      const fromRoute = routeValue != null ? Number(routeValue) : null
      if (Number.isFinite(fromRoute) && fromRoute > 0) {
        return fromRoute
      }
      return this.getCurrentUserId()
    },
    normalizeScores(source) {
      const src = source || {}
      return {
        教育: Number(src.education || src.educationScore || 0),
        工作: Number(src.work || src.workScore || 0),
        项目: Number(src.project || src.projectScore || 0),
        技能: Number(src.skill || src.skillScore || 0),
        获奖: Number(src.award || src.awardScore || 0)
      }
    },
    scoreTagType(score) {
      const value = Number(score || 0)
      if (value >= 85) return 'success'
      if (value >= 70) return 'warning'
      return 'info'
    },
    async fetchList() {
      const userId = this.getTargetUserId()
      if (!userId) {
        this.rows = []
        this.total = 0
        this.activeRowId = null
        this.$message.error('无法解析当前用户，请重新登录。')
        return
      }

      this.listLoading = true
      try {
        const res = await fetchResumeScoreList(userId, {
          pageNo: this.pageNo,
          pageSize: this.pageSize,
          keyword: this.keyword ? String(this.keyword).trim() : undefined
        })
        const items = res && Array.isArray(res.items) ? res.items : []
        this.rows = items.map(item => ({
          id: Number(item.resumeId),
          resumeName: item.resumeName || `简历 ${item.resumeId}`,
          totalScore: Number(item.totalScore || 0),
          generateCount: Number(item.generateCount || 0),
          latestScoreTime: item.latestScoreTime || '-',
          latestScoreId: item.latestScoreId || null,
          scores: this.normalizeScores(item.scores)
        }))
        this.total = Number(res && res.total != null ? res.total : 0)

        const hasActive = this.rows.some(item => item.id === this.activeRowId)
        this.activeRowId = hasActive ? this.activeRowId : (this.rows[0] ? this.rows[0].id : null)

        this.$nextTick(() => {
          if (!this.$refs.table || !this.activeRow) return
          this.$refs.table.setCurrentRow(this.activeRow)
        })
      } catch (e) {
        this.rows = []
        this.total = 0
        this.activeRowId = null
        this.$message.error('获取评分列表失败')
      } finally {
        this.listLoading = false
      }
    },
    onClickSearch() {
      this.pageNo = 1
      this.fetchList()
    },
    onClickReset() {
      this.keyword = ''
      this.pageNo = 1
      this.fetchList()
    },
    onSizeChange(size) {
      this.pageSize = size
      this.pageNo = 1
      this.fetchList()
    },
    onCurrentChange(page) {
      this.pageNo = page
      this.fetchList()
    },
    onRowClick(row) {
      if (!row || !row.id) return
      this.activeRowId = row.id
    },
    async openDetail(resumeId) {
      const userId = this.getTargetUserId()
      if (!userId || !resumeId) return

      this.detailResumeId = resumeId
      this.detailOpen = true
      this.detailScores = this.buildEmptyScores()
      this.detailTotalScore = 0
      this.detailDimensionAnalysis = ''
      this.detailProblems = []
      this.detailAdvice = ''

      try {
        const res = await fetchLatestResumeScore(userId, resumeId)
        this.detailScores = this.normalizeScores(res && res.scores)
        this.detailTotalScore = Number(res && res.totalScore != null ? res.totalScore : 0)
        this.detailDimensionAnalysis = res && (res.dimensionAnalysis || res.dimension_analysis) ? (res.dimensionAnalysis || res.dimension_analysis) : ''
        this.detailProblems = res && Array.isArray(res.problems) ? res.problems : (res && Array.isArray(res.problemList) ? res.problemList : [])
        this.detailAdvice = res && res.advice ? res.advice : ''
      } catch (e) {
        this.$message.error('获取评分详情失败')
      }
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

.topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.readonly-tip {
  flex: 1;
}

.search {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 260px;
}

.layout {
  display: flex;
  gap: 20px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card {
  padding: 14px 16px;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
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

.left {
  flex: 1.2;
  min-width: 0;
}

.right {
  flex: 0.8;
  min-width: 360px;
}

.chart-card {
  height: 100%;
}

.active-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.active-overview-item {
  padding: 10px 12px;
  border-radius: 10px;
  background: #f7f9fc;
}

.overview-label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: #909399;
}

.overview-value {
  color: #303133;
  font-weight: 600;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-subtitle {
  color: #909399;
  font-size: 12px;
}

.chart-empty {
  margin-top: 12px;
  color: #909399;
  text-align: center;
}

.score-tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.score-tag {
  margin: 0;
}

.pager {
  margin-top: 16px;
  text-align: right;
}

.detail-sub {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
  color: #606266;
}

.detail-total {
  font-weight: 600;
  color: #303133;
}

.detail-score-row {
  margin-bottom: 8px;
}

.detail-item {
  margin-bottom: 12px;
  padding: 12px;
  border-radius: 8px;
  background: #f7f9fc;
}

.detail-item-label {
  color: #909399;
  font-size: 12px;
  margin-bottom: 6px;
}

.detail-item-value {
  color: #303133;
  font-size: 22px;
  font-weight: 600;
}

.detail-block {
  margin-top: 16px;
}

.detail-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.detail-text {
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}

.detail-list-item {
  color: #606266;
  line-height: 1.8;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .layout {
    flex-direction: column;
  }

  .right {
    min-width: 0;
  }
}

@media (max-width: 768px) {
  .topbar {
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .search {
    width: 100%;
  }

  .search-input {
    width: 100%;
  }

  .active-overview {
    grid-template-columns: 1fr;
  }
}
</style>
