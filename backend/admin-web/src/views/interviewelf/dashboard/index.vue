<template>
  <div v-loading="loading" class="dashboard-page">
    <section class="hero">
      <div class="hero-main">
        <div class="hero-eyebrow">InterviewElf Admin</div>
        <h1 class="hero-title">运营总览</h1>
        <p class="hero-desc">
          一眼看清用户、简历、JD 和最近业务动态，录屏展示时不用来回切页面。
        </p>
      </div>
      <div class="hero-side">
        <div class="hero-date">{{ nowText }}</div>
        <div class="hero-tip">先看全局，再讲重点。</div>
      </div>
    </section>

    <section class="quick-actions">
      <el-button
        v-for="action in quickActions"
        :key="action.path"
        :type="action.type"
        @click="goTo(action.path)"
      >
        {{ action.label }}
      </el-button>
    </section>

    <section class="stats-grid">
      <div
        v-for="card in summaryCards"
        :key="card.key"
        class="stat-card"
        :style="{ '--accent': card.color }"
      >
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value">{{ card.value }}</div>
        <div class="stat-sub">{{ card.sub }}</div>
      </div>
    </section>

    <section class="chart-grid">
      <el-card shadow="never" class="panel-card">
        <div slot="header" class="panel-header">
          <span>近 7 天新增趋势</span>
          <span class="panel-note">用户 / 简历 / JD</span>
        </div>
        <trend-chart :chart-data="trends" />
      </el-card>

      <el-card shadow="never" class="panel-card">
        <div slot="header" class="panel-header">
          <span>JD 城市分布</span>
          <span class="panel-note">按当前 JD 数量统计</span>
        </div>
        <city-chart :items="cityStats" />
      </el-card>
    </section>

    <section class="table-grid">
      <el-card shadow="never" class="panel-card">
        <div slot="header" class="panel-header">
          <span>最近简历</span>
          <span class="panel-note">最近更新的 5 份简历</span>
        </div>
        <el-table :data="recentResumes" size="mini">
          <el-table-column prop="title" label="简历名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="userName" label="所属用户" width="140" show-overflow-tooltip />
          <el-table-column prop="updateTime" label="更新时间" width="180" />
        </el-table>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <div slot="header" class="panel-header">
          <span>最近 JD</span>
          <span class="panel-note">最近更新的 5 个岗位</span>
        </div>
        <el-table :data="recentJds" size="mini">
          <el-table-column prop="jobName" label="岗位" min-width="150" show-overflow-tooltip />
          <el-table-column prop="city" label="城市" width="90" />
          <el-table-column prop="salaryRange" label="薪资" width="110" />
          <el-table-column label="指导建议" width="110">
            <template slot-scope="scope">
              <el-tag size="mini" :type="scope.row.hasGuide ? 'success' : 'info'">
                {{ scope.row.hasGuide ? '已生成' : '未生成' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>

    <section class="table-grid">
      <el-card shadow="never" class="panel-card full-span">
        <div slot="header" class="panel-header">
          <span>最近活跃后台用户</span>
          <span class="panel-note">按最近活跃时间排序</span>
        </div>
        <el-table :data="recentUsers" size="mini">
          <el-table-column prop="userName" label="用户" min-width="160" show-overflow-tooltip />
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column prop="status" label="状态" width="90">
            <template slot-scope="scope">
              <el-tag size="mini" :type="scope.row.status === '启用' ? 'success' : 'danger'">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastActiveTime" label="最近活跃" width="180" />
        </el-table>
      </el-card>
    </section>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import TrendChart from './components/TrendChart'
import CityChart from './components/CityChart'
import { fetchDashboardOverview } from '@/api/interviewelf/dashboard'

export default {
  name: 'InterviewElfDashboard',
  components: {
    TrendChart,
    CityChart
  },
  data() {
    return {
      loading: false,
      summary: {
        totalUsers: 0,
        activeUsers: 0,
        totalResumes: 0,
        totalJds: 0,
        totalGuides: 0,
        totalProblems: 0,
        resumesLast7Days: 0,
        jdsLast7Days: 0,
        guideCoveragePercent: 0
      },
      trends: {
        labels: [],
        users: [],
        resumes: [],
        jds: []
      },
      cityStats: [],
      recentResumes: [],
      recentJds: [],
      recentUsers: []
    }
  },
  computed: {
    ...mapGetters(['permission_codes']),
    nowText() {
      const now = new Date()
      const year = now.getFullYear()
      const month = String(now.getMonth() + 1).padStart(2, '0')
      const day = String(now.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    summaryCards() {
      return [
        {
          key: 'users',
          label: '后台用户',
          value: this.summary.totalUsers,
          sub: `启用 ${this.summary.activeUsers} 人`,
          color: '#0f766e'
        },
        {
          key: 'resumes',
          label: '简历总数',
          value: this.summary.totalResumes,
          sub: `近 7 天新增 ${this.summary.resumesLast7Days} 份`,
          color: '#2563eb'
        },
        {
          key: 'jds',
          label: 'JD 总数',
          value: this.summary.totalJds,
          sub: `近 7 天新增 ${this.summary.jdsLast7Days} 条`,
          color: '#f97316'
        },
        {
          key: 'guides',
          label: '指导建议',
          value: this.summary.totalGuides,
          sub: `覆盖率 ${this.summary.guideCoveragePercent}%`,
          color: '#0891b2'
        },
        {
          key: 'problems',
          label: '题目记录',
          value: this.summary.totalProblems,
          sub: '当前题库沉淀量',
          color: '#7c3aed'
        },
        {
          key: 'health',
          label: '本周节奏',
          value: this.summary.resumesLast7Days + this.summary.jdsLast7Days,
          sub: '近 7 天简历 + JD 新增总量',
          color: '#dc2626'
        }
      ]
    },
    quickActions() {
      const actions = [
        { label: '上传简历', path: '/interviewelf/importResume', type: 'primary', perms: ['resume:view', 'resume:manage'] },
        { label: '修改简历', path: '/interviewelf/resumeBatchEdit', type: '', perms: ['resume:manage'] },
        { label: '上传JD', path: '/interviewelf/importJd', type: '', perms: [] },
        { label: 'JD指导建议', path: '/interviewelf/jdGuide', type: '', perms: [] },
        { label: '用户管理', path: '/interviewelf/userManagement', type: '', perms: ['company:manage'] }
      ]

      return actions.filter(action => this.canAccess(action.perms))
    }
  },
  created() {
    this.loadOverview()
  },
  methods: {
    canAccess(requiredPerms) {
      if (!Array.isArray(requiredPerms) || requiredPerms.length === 0) {
        return true
      }
      return Array.isArray(this.permission_codes) && this.permission_codes.some(code => requiredPerms.includes(code))
    },
    async loadOverview() {
      this.loading = true
      try {
        const data = await fetchDashboardOverview()
        this.summary = Object.assign({}, this.summary, data && data.summary ? data.summary : {})
        this.trends = Object.assign({}, this.trends, data && data.trends ? data.trends : {})
        this.cityStats = Array.isArray(data && data.cityStats) ? data.cityStats : []
        this.recentResumes = Array.isArray(data && data.recentResumes) ? data.recentResumes : []
        this.recentJds = Array.isArray(data && data.recentJds) ? data.recentJds : []
        this.recentUsers = Array.isArray(data && data.recentUsers) ? data.recentUsers : []
      } catch (error) {
        this.$message.error('加载仪表盘失败')
      } finally {
        this.loading = false
      }
    },
    goTo(path) {
      this.$router.push({ path })
    }
  }
}
</script>

<style scoped>
.dashboard-page {
  min-height: calc(100vh - 84px);
  padding: 20px;
  background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.1), transparent 30%),
    linear-gradient(180deg, #f7fafc 0%, #eef2f7 100%);
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 28px;
  border-radius: 20px;
  background: linear-gradient(135deg, #0f172a 0%, #1d4ed8 55%, #0891b2 100%);
  color: #fff;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.16);
}

.hero-eyebrow {
  margin-bottom: 10px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  opacity: 0.76;
}

.hero-title {
  margin: 0 0 10px;
  font-size: 34px;
  line-height: 1.1;
}

.hero-desc {
  max-width: 620px;
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  opacity: 0.92;
}

.hero-side {
  min-width: 200px;
  text-align: right;
}

.hero-date {
  margin-bottom: 10px;
  font-size: 28px;
  font-weight: 700;
}

.hero-tip {
  font-size: 13px;
  line-height: 1.8;
  opacity: 0.88;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 18px 0 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  padding: 18px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.stat-card::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 5px;
  background: var(--accent);
}

.stat-label {
  margin-bottom: 10px;
  font-size: 13px;
  color: #64748b;
}

.stat-value {
  margin-bottom: 10px;
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  color: #0f172a;
}

.stat-sub {
  font-size: 12px;
  line-height: 1.6;
  color: #94a3b8;
}

.chart-grid,
.table-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.panel-card {
  border: none;
  border-radius: 18px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #0f172a;
}

.panel-note {
  font-size: 12px;
  font-weight: 400;
  color: #94a3b8;
}

.full-span {
  grid-column: 1 / -1;
}

@media (max-width: 1440px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .hero {
    flex-direction: column;
  }

  .hero-side {
    text-align: left;
  }

  .stats-grid,
  .chart-grid,
  .table-grid {
    grid-template-columns: 1fr;
  }
}
</style>
