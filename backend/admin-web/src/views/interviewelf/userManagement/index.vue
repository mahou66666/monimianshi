<template>
  <div class="app-container">
    <el-form :inline="true" size="small" class="filter-form" @submit.native.prevent>
      <el-form-item label="手机号">
        <el-input v-model="query.phone" clearable style="width: 180px" />
      </el-form-item>

      <el-form-item label="用户名">
        <el-input v-model="query.keyword" clearable style="width: 220px" />
      </el-form-item>

      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="2" />
        </el-select>
      </el-form-item>

      <el-form-item label="已购服务">
        <el-select v-model="query.paidServiceId" placeholder="全部" clearable style="width: 200px">
          <el-option label="未购买" :value="-1" />
          <el-option label="已购买任意" :value="0" />
          <el-option
            v-for="s in paidServiceOptions"
            :key="s.id"
            :label="s.serviceName"
            :value="s.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="已传简历">
        <el-select v-model="query.hasResumeFile" placeholder="全部" clearable style="width: 120px">
          <el-option label="是" :value="true" />
          <el-option label="否" :value="false" />
        </el-select>
      </el-form-item>

      <el-form-item label="异常">
        <el-select v-model="query.isAbnormal" placeholder="全部" clearable style="width: 120px">
          <el-option label="是" :value="true" />
          <el-option label="否" :value="false" />
        </el-select>
      </el-form-item>

      <el-form-item label="活跃时间">
        <el-date-picker
          v-model="activeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          align="right"
          value-format="yyyy-MM-ddTHH:mm:ss"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
        <el-button v-if="canManageUsers" type="success" @click="openCreateDialog">新增用户</el-button>
      </el-form-item>
    </el-form>

    <div class="summary-grid">
      <div v-for="card in summaryCards" :key="card.label" class="summary-card">
        <div class="summary-label">{{ card.label }}</div>
        <div class="summary-value">{{ card.value }}</div>
        <div class="summary-sub">{{ card.sub }}</div>
      </div>
    </div>

    <el-table
      v-loading="listLoading"
      :data="list"
      border
      highlight-current-row
      style="width: 100%"
    >
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="phone" label="手机号" min-width="140" />
      <el-table-column prop="userName" label="用户名" min-width="140" />
      <el-table-column label="状态" width="90">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="mini">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标签" min-width="220">
        <template slot-scope="scope">
          <el-tag
            v-for="t in (scope.row.tags || [])"
            :key="t"
            :type="tagType(t)"
            size="mini"
            style="margin-right: 6px; margin-bottom: 4px"
          >
            {{ t }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="权限" min-width="260">
        <template slot-scope="scope">
          <div v-if="userPermissionOf(scope.row).loading" class="perm-loading">加载中...</div>
          <template v-else-if="userPermissionOf(scope.row).names.length > 0">
            <el-tag
              v-for="permName in userPermissionOf(scope.row).names"
              :key="permName"
              size="mini"
              type="warning"
              effect="plain"
              class="perm-tag"
            >
              {{ permName }}
            </el-tag>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" min-width="170" />
      <el-table-column prop="lastActiveTime" label="最近活跃" min-width="170" />
      <el-table-column label="操作" width="400" fixed="right">
        <template slot-scope="scope">
          <el-button
            v-if="canManageUsers && scope.row.status === 1"
            type="text"
            size="mini"
            @click.stop="onDisable(scope.row)"
          >禁用</el-button>
          <el-button
            v-else-if="canManageUsers"
            type="text"
            size="mini"
            @click.stop="onEnable(scope.row)"
          >启用</el-button>
          <el-button v-if="canManageUsers" type="text" size="mini" @click.stop="openEditDialog(scope.row)">编辑</el-button>
          <el-button v-if="canManageUsers" type="text" size="mini" @click.stop="openPermissionDialog(scope.row)">权限</el-button>
          <el-button type="text" size="mini" @click.stop="openUserResumes(scope.row)">简历</el-button>
          <el-button v-if="canViewResumeScoring" type="text" size="mini" @click.stop="openUserScores(scope.row)">评分</el-button>
          <el-button type="text" size="mini" @click.stop="openDetailDialog(scope.row)">详情</el-button>
          <el-button type="text" size="mini" @click.stop="openPanel(scope.row)">状态</el-button>
          <el-button v-if="canManageUsers" type="text" size="mini" style="color: #f56c6c" @click.stop="onDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        :current-page="query.pageNo"
        :page-size="query.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="onSizeChange"
        @current-change="onCurrentChange"
      />
    </div>

    <el-dialog
      :title="userDialogTitle"
      :visible.sync="userDialogVisible"
      width="560px"
      append-to-body
    >
      <div v-loading="userDialogLoading">
        <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="100px">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="userForm.phone" maxlength="20" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="用户名" prop="userName">
            <el-input v-model="userForm.userName" maxlength="50" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="userForm.password"
              type="password"
              show-password
              :placeholder="userDialogMode === 'create' ? '请输入初始密码' : '不修改请留空'"
            />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="userForm.status">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="2">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button v-if="canManageUsers" type="primary" :loading="userSubmitting" @click="submitUser">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog
      :title="`权限配置 - ${permissionForm.userName || ''}`"
      :visible.sync="permissionDialogVisible"
      width="620px"
      append-to-body
    >
      <div v-loading="permissionLoading" class="permission-body">
        <div v-if="permissionOptions.length === 0" class="permission-empty">暂无可分配权限</div>
        <el-checkbox-group v-else v-model="permissionForm.permissionIds" class="permission-group">
          <el-checkbox
            v-for="p in permissionOptions"
            :key="p.id"
            :label="p.id"
            class="permission-item"
          >
            {{ p.serviceName }}（{{ p.permCode }}）
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button v-if="canManageUsers" type="primary" :loading="permissionSubmitting" @click="submitPermissions">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog
      title="用户详情"
      :visible.sync="detailDialogVisible"
      width="640px"
      append-to-body
    >
      <div v-loading="detailLoading">
        <div v-if="detailData" class="detail-wrap">
          <div class="detail-item">
            <span class="detail-label">用户ID：</span>
            <span class="detail-value">{{ detailData.id }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">手机号：</span>
            <span class="detail-value">{{ detailData.phone || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">用户名：</span>
            <span class="detail-value">{{ detailData.userName || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">账号状态：</span>
            <span class="detail-value">{{ detailData.status === 1 ? '启用' : '禁用' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">注册时间：</span>
            <span class="detail-value">{{ detailData.createTime || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">更新时间：</span>
            <span class="detail-value">{{ detailData.updateTime || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">最近活跃：</span>
            <span class="detail-value">{{ detailData.lastActiveTime || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">是否有简历：</span>
            <span class="detail-value">{{ detailData.hasResumeFile ? '是' : '否' }}</span>
          </div>
          <div class="detail-item detail-item-full">
            <span class="detail-label">权限标签：</span>
            <div class="detail-perm-list">
              <el-tag
                v-for="permName in (detailData.permissionNames || [])"
                :key="permName"
                size="mini"
                type="warning"
                effect="plain"
                class="perm-tag"
              >
                {{ permName }}
              </el-tag>
              <span v-if="!detailData.permissionNames || detailData.permissionNames.length === 0" class="detail-empty">-</span>
            </div>
          </div>
        </div>
        <div v-else class="detail-empty">暂无数据</div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button v-if="detailData && detailData.id" type="primary" plain @click="openUserResumes(detailData)">查看该用户简历</el-button>
        <el-button v-if="detailData && detailData.id && canViewResumeScoring" type="primary" plain @click="openUserScores(detailData)">查看评分结果</el-button>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>

    <el-drawer
      title="用户状态面板"
      :visible.sync="panelVisible"
      size="520px"
      direction="rtl"
      append-to-body
    >
      <div v-loading="panelLoading" class="panel-body">
        <div v-if="panelData" class="panel-header">
          <div class="panel-title">{{ panelData.userName || '-' }}</div>
          <div class="panel-sub">{{ panelData.phone || '-' }}</div>
          <div style="margin-top: 8px">
            <el-tag :type="panelData.status === 1 ? 'success' : 'info'" size="mini">
              {{ panelData.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </div>
        </div>

        <div v-if="panelData" class="panel-steps">
          <el-steps direction="vertical" :space="44">
            <el-step
              v-for="s in (panelData.steps || [])"
              :key="s.index"
              :title="s.name"
              :status="stepStatus(s.status)"
            >
              <template slot="description">
                <div v-if="s.details && s.details.length" class="step-details">
                  <div v-for="(d, idx) in s.details" :key="idx" class="step-detail-item">{{ d }}</div>
                </div>
              </template>
            </el-step>
          </el-steps>
        </div>

        <div v-if="!panelData && !panelLoading" class="panel-empty">
          暂无数据
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { MessageBox, Message } from 'element-ui'
import {
  assignUserPermissions,
  createAdminUser,
  deleteAdminUser,
  disableUser,
  enableUser,
  fetchPaidServiceOptions,
  fetchPermissionOptions,
  fetchUserDetail,
  fetchUserPermissionsBatch,
  fetchUserPermissions,
  fetchUsers,
  fetchUserStatusPanel,
  updateAdminUser
} from '@/api/interviewelf/adminUsers'

export default {
  name: 'UserManagement',
  data() {
    const validatePhone = (rule, value, callback) => {
      const v = value == null ? '' : String(value).trim()
      if (!v) {
        callback(new Error('请输入手机号'))
        return
      }
      if (!/^\d{11,20}$/.test(v)) {
        callback(new Error('手机号格式不正确'))
        return
      }
      callback()
    }

    const validatePassword = (rule, value, callback) => {
      const v = value == null ? '' : String(value).trim()
      if (this.userDialogMode === 'create' && !v) {
        callback(new Error('请输入初始密码'))
        return
      }
      callback()
    }

    return {
      listLoading: false,
      list: [],
      total: 0,
      query: {
        pageNo: 1,
        pageSize: 10,
        phone: '',
        keyword: '',
        status: undefined,
        paidServiceId: undefined,
        hasResumeFile: undefined,
        isAbnormal: undefined,
        activeStart: undefined,
        activeEnd: undefined
      },
      paidServiceOptions: [],
      permissionOptions: [],
      permissionTagMap: {},
      permissionTagReqNo: 0,
      activeRange: [],

      userDialogVisible: false,
      userDialogMode: 'create',
      userDialogLoading: false,
      userSubmitting: false,
      userForm: {
        id: null,
        phone: '',
        userName: '',
        password: '',
        status: 1
      },
      userRules: {
        phone: [{ validator: validatePhone, trigger: 'blur' }],
        userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ validator: validatePassword, trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      },

      permissionDialogVisible: false,
      permissionLoading: false,
      permissionSubmitting: false,
      permissionForm: {
        userId: null,
        userName: '',
        permissionIds: []
      },

      detailDialogVisible: false,
      detailLoading: false,
      detailData: null,

      panelVisible: false,
      panelLoading: false,
      panelData: null,
      panelUserId: null
    }
  },
  computed: {
    userDialogTitle() {
      return this.userDialogMode === 'create' ? '新增用户' : '编辑用户'
    },
    permissionCodes() {
      const codes = this.$store && this.$store.getters ? this.$store.getters.permission_codes : []
      return Array.isArray(codes) ? codes : []
    },
    userRoles() {
      const roles = this.$store && this.$store.getters ? this.$store.getters.roles : []
      return Array.isArray(roles) ? roles : []
    },
    canManageUsers() {
      return this.userRoles.includes('admin') || this.permissionCodes.includes('company:manage')
    },
    canViewResumeScoring() {
      return this.userRoles.includes('admin') || this.permissionCodes.includes('resume:view') || this.permissionCodes.includes('resume:manage')
    },
    enabledUserCount() {
      return (this.list || []).filter(row => row && Number(row.status) === 1).length
    },
    disabledUserCount() {
      return (this.list || []).filter(row => row && Number(row.status) === 2).length
    },
    uploadedResumeCount() {
      return (this.list || []).filter(row => row && row.hasResumeFile).length
    },
    usersWithPermissionsCount() {
      return (this.list || []).filter(row => this.userPermissionOf(row).names.length > 0).length
    },
    summaryCards() {
      return [
        {
          label: '当前页用户',
          value: this.list.length,
          sub: `列表总计 ${this.total}`
        },
        {
          label: '启用账号',
          value: this.enabledUserCount,
          sub: '当前页可正常登录'
        },
        {
          label: '已传简历',
          value: this.uploadedResumeCount,
          sub: '可跳转查看简历'
        },
        {
          label: '已分配权限',
          value: this.usersWithPermissionsCount,
          sub: '适合演示权限管理'
        }
      ]
    }
  },
  created() {
    this.loadPaidServiceOptions()
    this.loadPermissionOptions()
    this.getList()
  },
  methods: {
    ensureManageUsers(actionText) {
      if (this.canManageUsers) return true
      Message.warning(actionText || '当前账号无用户管理权限')
      return false
    },
    async loadPaidServiceOptions() {
      try {
        const res = await fetchPaidServiceOptions()
        this.paidServiceOptions = Array.isArray(res) ? res : []
      } catch (e) {
        this.paidServiceOptions = []
      }
    },
    async loadPermissionOptions() {
      try {
        const res = await fetchPermissionOptions()
        this.permissionOptions = Array.isArray(res) ? res : []
      } catch (e) {
        this.permissionOptions = []
      }
    },
    tagType(t) {
      if (!t) return ''
      if (String(t).includes('异常')) return 'danger'
      if (String(t).includes('已禁用')) return 'info'
      if (String(t).includes('已购')) return 'warning'
      if (String(t).includes('已上传')) return 'success'
      return ''
    },
    stepStatus(s) {
      if (s === 'done') return 'success'
      if (s === 'todo') return 'wait'
      return 'wait'
    },
    buildQuery() {
      const q = { ...this.query }

      if (q.paidServiceId === 0) {
        q.hasPaidService = true
        q.paidServiceId = undefined
      } else if (q.paidServiceId === -1) {
        q.hasPaidService = false
        q.paidServiceId = undefined
      } else if (q.paidServiceId && Number(q.paidServiceId) > 0) {
        q.hasPaidService = undefined
      } else {
        q.hasPaidService = undefined
        q.paidServiceId = undefined
      }

      if (q.phone && String(q.phone).trim()) {
        q.phone = String(q.phone).trim()
      } else {
        q.phone = undefined
      }

      if (this.activeRange && this.activeRange.length === 2) {
        q.activeStart = this.activeRange[0]
        q.activeEnd = this.activeRange[1]
      } else {
        q.activeStart = undefined
        q.activeEnd = undefined
      }
      return q
    },
    async getList() {
      this.listLoading = true
      try {
        const res = await fetchUsers(this.buildQuery())
        this.list = res && res.items ? res.items : []
        this.total = Number(res && res.total != null ? res.total : 0)
        this.loadPermissionTagsForCurrentList()
      } catch (e) {
        this.list = []
        this.total = 0
        this.permissionTagMap = {}
        Message.error('获取用户列表失败')
      } finally {
        this.listLoading = false
      }
    },
    extractPermissionNames(payload) {
      const perms = payload && Array.isArray(payload.permissions) ? payload.permissions : []
      return perms
        .map(p => {
          if (!p) return ''
          if (p.permName) return String(p.permName)
          if (p.serviceName) return String(p.serviceName)
          if (p.permCode) return String(p.permCode)
          return ''
        })
        .filter(Boolean)
    },
    async loadPermissionTagsForCurrentList() {
      const rows = Array.isArray(this.list) ? this.list : []
      const reqNo = this.permissionTagReqNo + 1
      this.permissionTagReqNo = reqNo

      const nextMap = {}
      rows.forEach(row => {
        if (!row || row.id == null) return
        nextMap[String(row.id)] = { loading: true, names: [] }
      })
      this.permissionTagMap = nextMap

      const userIds = rows
        .filter(row => row && row.id != null)
        .map(row => Number(row.id))
        .filter(v => !Number.isNaN(v) && v > 0)

      if (userIds.length === 0) {
        return
      }

      try {
        const batch = await fetchUserPermissionsBatch(userIds)
        if (this.permissionTagReqNo !== reqNo) return

        const batchItems = batch && Array.isArray(batch.items) ? batch.items : []
        const resultMap = {}
        batchItems.forEach(item => {
          if (!item || item.userId == null) return
          const key = String(item.userId)
          resultMap[key] = {
            loading: false,
            names: this.extractPermissionNames(item)
          }
        })

        rows.forEach(row => {
          if (!row || row.id == null) return
          const key = String(row.id)
          if (!resultMap[key]) {
            resultMap[key] = { loading: false, names: [] }
          }
          this.$set(this.permissionTagMap, key, resultMap[key])
        })
      } catch (e) {
        if (this.permissionTagReqNo !== reqNo) return
        rows.forEach(row => {
          if (!row || row.id == null) return
          this.$set(this.permissionTagMap, String(row.id), { loading: false, names: [] })
        })
      }
    },
    userPermissionOf(row) {
      const key = row && row.id != null ? String(row.id) : ''
      if (!key) {
        return { loading: false, names: [] }
      }
      const item = this.permissionTagMap[key]
      if (!item) {
        return { loading: true, names: [] }
      }
      return {
        loading: Boolean(item.loading),
        names: Array.isArray(item.names) ? item.names : []
      }
    },
    onSearch() {
      this.query.pageNo = 1
      this.getList()
    },
    onReset() {
      this.query = {
        pageNo: 1,
        pageSize: 10,
        phone: '',
        keyword: '',
        status: undefined,
        paidServiceId: undefined,
        hasResumeFile: undefined,
        isAbnormal: undefined,
        activeStart: undefined,
        activeEnd: undefined
      }
      this.activeRange = []
      this.getList()
    },
    onSizeChange(size) {
      this.query.pageSize = size
      this.query.pageNo = 1
      this.getList()
    },
    onCurrentChange(page) {
      this.query.pageNo = page
      this.getList()
    },
    openCreateDialog() {
      if (!this.ensureManageUsers('当前账号无新增用户权限')) return
      this.userDialogMode = 'create'
      this.userDialogVisible = true
      this.userDialogLoading = false
      this.userForm = {
        id: null,
        phone: '',
        userName: '',
        password: '',
        status: 1
      }
      this.$nextTick(() => {
        if (this.$refs.userFormRef && this.$refs.userFormRef.clearValidate) {
          this.$refs.userFormRef.clearValidate()
        }
      })
    },
    async openEditDialog(row) {
      if (!this.ensureManageUsers('当前账号无编辑用户权限')) return
      if (!row || !row.id) return
      this.userDialogMode = 'edit'
      this.userDialogVisible = true
      this.userDialogLoading = true
      this.userForm = {
        id: row.id,
        phone: row.phone || '',
        userName: row.userName || '',
        password: '',
        status: row.status === 2 ? 2 : 1
      }
      this.$nextTick(() => {
        if (this.$refs.userFormRef && this.$refs.userFormRef.clearValidate) {
          this.$refs.userFormRef.clearValidate()
        }
      })

      try {
        const detail = await fetchUserDetail(row.id)
        this.userForm.phone = detail && detail.phone ? detail.phone : this.userForm.phone
        this.userForm.userName = detail && detail.userName ? detail.userName : this.userForm.userName
        this.userForm.status = detail && detail.status === 2 ? 2 : 1
      } catch (e) {
        Message.error('获取用户详情失败')
      } finally {
        this.userDialogLoading = false
      }
    },
    async validateUserForm() {
      return new Promise(resolve => {
        if (!this.$refs.userFormRef || !this.$refs.userFormRef.validate) {
          resolve(false)
          return
        }
        this.$refs.userFormRef.validate(valid => {
          resolve(valid)
        })
      })
    },
    async submitUser() {
      if (!this.ensureManageUsers('当前账号无保存用户权限')) return
      if (this.userSubmitting) return
      const valid = await this.validateUserForm()
      if (!valid) return

      const phone = String(this.userForm.phone || '').trim()
      const userName = String(this.userForm.userName || '').trim()
      const password = String(this.userForm.password || '').trim()
      const payload = {
        phone,
        userName,
        status: this.userForm.status
      }
      if (this.userDialogMode === 'create' || password) {
        payload.password = password
      }

      this.userSubmitting = true
      try {
        if (this.userDialogMode === 'create') {
          await createAdminUser(payload)
          Message.success('新增成功')
        } else {
          await updateAdminUser(this.userForm.id, payload)
          Message.success('更新成功')
        }
        this.userDialogVisible = false
        await this.getList()
        if (this.panelVisible && this.panelUserId === this.userForm.id) {
          await this.getPanel(this.userForm.id)
        }
      } catch (e) {
        Message.error(e && e.message ? e.message : '保存失败')
      } finally {
        this.userSubmitting = false
      }
    },
    async onDelete(row) {
      if (!this.ensureManageUsers('当前账号无删除用户权限')) return
      if (!row || !row.id) return
      try {
        await MessageBox.confirm(`确认删除用户 ${row.userName || row.phone || ''}？`, '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      try {
        const res = await deleteAdminUser(row.id)
        if (res && res.success === false) {
          Message.error('删除失败')
          return
        }
        Message.success('删除成功')
        if (this.panelVisible && this.panelUserId === row.id) {
          this.panelVisible = false
          this.panelData = null
          this.panelUserId = null
        }
        await this.getList()
      } catch (e) {
        Message.error(e && e.message ? e.message : '删除失败')
      }
    },
    async openPermissionDialog(row) {
      if (!this.ensureManageUsers('当前账号无配置权限的权限')) return
      if (!row || !row.id) return
      this.permissionDialogVisible = true
      this.permissionLoading = true
      this.permissionForm = {
        userId: row.id,
        userName: row.userName || row.phone || '',
        permissionIds: []
      }

      try {
        if (!this.permissionOptions || this.permissionOptions.length === 0) {
          await this.loadPermissionOptions()
        }
        const data = await fetchUserPermissions(row.id)
        const ids = data && Array.isArray(data.permissionIds) ? data.permissionIds : []
        this.permissionForm.permissionIds = ids.map(v => Number(v)).filter(v => !Number.isNaN(v))
      } catch (e) {
        Message.error('获取权限数据失败')
      } finally {
        this.permissionLoading = false
      }
    },
    async openDetailDialog(row) {
      if (!row || !row.id) return
      this.detailDialogVisible = true
      this.detailLoading = true
      this.detailData = null
      try {
        const detail = await fetchUserDetail(row.id)
        const permissionNames = this.extractPermissionNames(detail)
        this.detailData = {
          ...detail,
          permissionNames
        }
        this.$set(this.permissionTagMap, String(row.id), { loading: false, names: permissionNames })
      } catch (e) {
        this.detailData = null
        Message.error('获取用户详情失败')
      } finally {
        this.detailLoading = false
      }
    },
    openUserResumes(row) {
      if (!row || !row.id) return
      this.$router.push({
        path: '/interviewelf/importResume',
        query: {
          userId: String(row.id),
          userName: row.userName || ''
        }
      })
    },
    openUserScores(row) {
      if (!row || !row.id) return
      if (!this.canViewResumeScoring) {
        Message.warning('当前账号无查看评分结果权限')
        return
      }
      this.$router.push({
        path: '/interviewelf/resumeScoring',
        query: {
          userId: String(row.id),
          userName: row.userName || ''
        }
      })
    },
    async submitPermissions() {
      if (!this.ensureManageUsers('当前账号无权限分配能力')) return
      if (this.permissionSubmitting || !this.permissionForm.userId) return
      this.permissionSubmitting = true
      try {
        const ids = Array.from(new Set((this.permissionForm.permissionIds || [])
          .map(v => Number(v))
          .filter(v => !Number.isNaN(v) && v > 0)))
        await assignUserPermissions(this.permissionForm.userId, ids)
        Message.success('权限保存成功')
        this.permissionDialogVisible = false
        await this.getList()
        if (this.panelVisible && this.panelUserId === this.permissionForm.userId) {
          await this.getPanel(this.permissionForm.userId)
        }
      } catch (e) {
        Message.error(e && e.message ? e.message : '权限保存失败')
      } finally {
        this.permissionSubmitting = false
      }
    },
    openPanel(row) {
      if (!row || !row.id) return
      this.panelVisible = true
      this.panelUserId = row.id
      this.getPanel(row.id)
    },
    async getPanel(userId) {
      this.panelLoading = true
      try {
        const res = await fetchUserStatusPanel(userId)
        this.panelData = res
      } catch (e) {
        this.panelData = null
      } finally {
        this.panelLoading = false
      }
    },
    async onDisable(row) {
      if (!this.ensureManageUsers('当前账号无禁用用户权限')) return
      try {
        await MessageBox.confirm(`确认禁用用户 ${row.userName || row.phone || ''}？`, '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      const res = await disableUser(row.id)
      if (res && res.success === false) {
        Message.error('禁用失败')
        return
      }
      Message.success('已禁用')
      await this.getList()
      if (this.panelVisible && this.panelUserId === row.id) {
        await this.getPanel(row.id)
      }
    },
    async onEnable(row) {
      if (!this.ensureManageUsers('当前账号无启用用户权限')) return
      try {
        await MessageBox.confirm(`确认启用用户 ${row.userName || row.phone || ''}？`, '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      const res = await enableUser(row.id)
      if (res && res.success === false) {
        Message.error('启用失败')
        return
      }
      Message.success('已启用')
      await this.getList()
      if (this.panelVisible && this.panelUserId === row.id) {
        await this.getPanel(row.id)
      }
    }
  }
}
</script>

<style scoped>
.filter-form {
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

.pager {
  padding: 16px 0;
  text-align: right;
}

.permission-body {
  min-height: 120px;
}

.permission-group {
  display: flex;
  flex-direction: column;
}

.permission-item {
  margin: 0 0 10px 0;
}

.permission-empty {
  color: #909399;
  text-align: center;
  padding: 30px 0;
}

.perm-loading {
  color: #909399;
  font-size: 12px;
}

.perm-tag {
  margin-right: 6px;
  margin-bottom: 4px;
}

.detail-wrap {
  display: flex;
  flex-wrap: wrap;
}

.detail-item {
  width: 50%;
  margin-bottom: 12px;
  padding-right: 12px;
  box-sizing: border-box;
}

.detail-item-full {
  width: 100%;
}

.detail-label {
  color: #909399;
}

.detail-value {
  color: #303133;
}

.detail-perm-list {
  display: inline-block;
  vertical-align: top;
  margin-left: 2px;
}

.detail-empty {
  color: #909399;
}

.panel-body {
  padding: 16px;
}

.panel-header {
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 12px;
  margin-bottom: 12px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
}

.panel-sub {
  margin-top: 4px;
  color: #909399;
}

.step-details {
  margin-top: 4px;
}

.step-detail-item {
  font-size: 12px;
  color: #606266;
  line-height: 18px;
}

.panel-empty {
  color: #909399;
  text-align: center;
  padding: 20px 0;
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
