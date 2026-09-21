const Mock = require('mockjs')
const { param2Obj } = require('./utils')

function randomFrom(arr) {
  return arr[Math.floor(Math.random() * arr.length)]
}

function toBool(val) {
  if (val === true || val === false) return val
  if (val === 'true') return true
  if (val === 'false') return false
  return undefined
}

function includesIgnoreCase(str, kw) {
  if (!kw) return true
  return String(str || '').toLowerCase().includes(String(kw).toLowerCase())
}

const paidServicePool = ['简历优化', '面试指导', '题库生成', '模拟面试', '职业规划']

const userDb = Array.from({ length: 57 }).map((_, idx) => {
  const id = idx + 1
  const phone = Mock.mock(/^1[3-9]\d{9}$/)
  const userName = Mock.mock('@cname')
  const hasPaidService = Math.random() < 0.35
  const hasResumeFile = Math.random() < 0.55
  const isAbnormal = Math.random() < 0.08
  const status = Math.random() < 0.12 ? 2 : 1

  const paidServiceNames = hasPaidService
    ? Array.from(new Set(Array.from({ length: 1 + Math.floor(Math.random() * 3) }).map(() => randomFrom(paidServicePool))))
    : []

  const tags = []
  if (hasPaidService) tags.push('已购服务')
  if (hasResumeFile) tags.push('已上传简历')
  if (isAbnormal) tags.push('异常')
  if (status === 2) tags.push('已禁用')

  const createTime = Mock.mock('@datetime')
  const updateTime = Mock.mock('@datetime')

  return {
    id,
    phone,
    userName,
    status,
    createTime,
    updateTime,
    tags,
    paidServiceNames,
    _flags: { hasPaidService, hasResumeFile, isAbnormal }
  }
})

function buildStatusPanel(user) {
  const paidDone = user._flags.hasPaidService
  const resumeDone = user._flags.hasResumeFile
  const disabledDone = user.status === 2
  const abnormalDone = user._flags.isAbnormal

  const steps = [
    {
      index: 1,
      name: '已购买服务',
      status: paidDone ? 'done' : 'todo',
      details: paidDone ? user.paidServiceNames : []
    },
    {
      index: 2,
      name: '已上传简历',
      status: resumeDone ? 'done' : 'todo'
    },
    {
      index: 3,
      name: '简历修改中(系统)',
      status: 'todo'
    },
    {
      index: 4,
      name: '已生成面试指导意见',
      status: 'todo'
    },
    {
      index: 5,
      name: '已生成面试题库',
      status: 'todo'
    },
    {
      index: 6,
      name: '客户禁用',
      status: disabledDone ? 'done' : 'todo'
    },
    {
      index: 7,
      name: '异常',
      status: abnormalDone ? 'done' : 'todo'
    }
  ]

  return {
    userId: user.id,
    phone: user.phone,
    userName: user.userName,
    status: user.status,
    steps
  }
}

module.exports = [
  {
    url: '/interview/admin/users',
    type: 'get',
    response: config => {
      const query = config.query || param2Obj(config.url)
      const pageNo = Number(query.pageNo || 1)
      const pageSize = Number(query.pageSize || 10)
      const keyword = query.keyword || ''
      const status = query.status !== undefined && query.status !== '' ? Number(query.status) : undefined
      const hasPaidService = toBool(query.hasPaidService)
      const hasResumeFile = toBool(query.hasResumeFile)
      const isAbnormal = toBool(query.isAbnormal)
      const registerStart = query.registerStart ? new Date(query.registerStart) : null
      const registerEnd = query.registerEnd ? new Date(query.registerEnd) : null

      let items = userDb.slice()

      if (keyword) {
        items = items.filter(u => includesIgnoreCase(u.phone, keyword) || includesIgnoreCase(u.userName, keyword))
      }
      if (status === 1 || status === 2) {
        items = items.filter(u => u.status === status)
      }
      if (hasPaidService !== undefined) {
        items = items.filter(u => u._flags.hasPaidService === hasPaidService)
      }
      if (hasResumeFile !== undefined) {
        items = items.filter(u => u._flags.hasResumeFile === hasResumeFile)
      }
      if (isAbnormal !== undefined) {
        items = items.filter(u => u._flags.isAbnormal === isAbnormal)
      }

      if (registerStart && !isNaN(registerStart.getTime())) {
        items = items.filter(u => new Date(u.createTime).getTime() >= registerStart.getTime())
      }
      if (registerEnd && !isNaN(registerEnd.getTime())) {
        items = items.filter(u => new Date(u.createTime).getTime() <= registerEnd.getTime())
      }

      const total = items.length
      const start = (pageNo - 1) * pageSize
      const end = start + pageSize
      const pageItems = items.slice(start, end).map(u => {
        const { _flags, ...rest } = u
        return rest
      })

      return {
        code: 20000,
        message: 'success',
        data: {
          pageNo,
          pageSize,
          total,
          items: pageItems
        }
      }
    }
  },
  {
    url: '/interview/admin/users/\\d+/status-panel',
    type: 'get',
    response: config => {
      const m = /\/interview\/admin\/users\/(\d+)\/status-panel/.exec(config.url)
      const userId = Number((m || [])[1])
      const user = userDb.find(u => u.id === userId)

      if (!user) {
        return {
          code: 50000,
          message: '用户不存在',
          data: null
        }
      }

      return {
        code: 20000,
        message: 'success',
        data: buildStatusPanel(user)
      }
    }
  },
  {
    url: '/interview/admin/users/\\d+/disable',
    type: 'post',
    response: config => {
      const m = /\/interview\/admin\/users\/(\d+)\/disable/.exec(config.url)
      const userId = Number((m || [])[1])
      const user = userDb.find(u => u.id === userId)

      if (!user) {
        return {
          code: 50000,
          message: '用户不存在',
          data: null
        }
      }

      user.status = 2
      if (!user.tags.includes('已禁用')) user.tags.push('已禁用')

      return {
        code: 20000,
        message: 'success',
        data: {
          userId,
          disabled: true,
          success: true
        }
      }
    }
  },
  {
    url: '/interview/admin/users/\\d+/enable',
    type: 'post',
    response: config => {
      const m = /\/interview\/admin\/users\/(\d+)\/enable/.exec(config.url)
      const userId = Number((m || [])[1])
      const user = userDb.find(u => u.id === userId)

      if (!user) {
        return {
          code: 50000,
          message: '用户不存在',
          data: null
        }
      }

      user.status = 1
      user.tags = user.tags.filter(t => t !== '已禁用')

      return {
        code: 20000,
        message: 'success',
        data: {
          userId,
          disabled: false,
          success: true
        }
      }
    }
  }
]
