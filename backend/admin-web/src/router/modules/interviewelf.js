import Layout from '@/layout'

const interviewElfRouter = {
  path: '/interviewelf',
  component: Layout,
  redirect: '/interviewelf/dashboard',
  name: 'InterviewElf',
  meta: { title: 'InterviewElf', icon: 'el-icon-s-custom' },
  children: [
    {
      path: 'dashboard',
      component: () => import('@/views/interviewelf/dashboard/index'),
      name: 'InterviewElfDashboard',
      meta: { title: '数据总览', icon: 'el-icon-s-data', affix: true }
    },
    {
      path: 'userManagement',
      component: () => import('@/views/interviewelf/userManagement/index'),
      name: 'UserManagement',
      meta: { title: '用户管理', icon: 'el-icon-user', perms: ['company:manage'] }
    },
    {
      path: 'importResume',
      component: () => import('@/views/interviewelf/importResume/index'),
      name: 'ImportResume',
      meta: { title: '上传简历', icon: 'el-icon-upload', perms: ['resume:view', 'resume:manage'] }
    },
    {
      path: 'resumeBatchEdit',
      component: () => import('@/views/interviewelf/resumeBatchEdit/index'),
      name: 'ResumeBatchEdit',
      meta: { title: '修改简历', icon: 'el-icon-edit', perms: ['resume:manage'] }
    },
    {
      path: 'resumeScoring',
      component: () => import('@/views/interviewelf/resumeScoring/index'),
      name: 'ResumeScoring',
      meta: { title: '简历评分', icon: 'el-icon-data-analysis', perms: ['resume:view', 'resume:manage'] }
    },
    {
      path: 'importJd',
      component: () => import('@/views/interviewelf/importJd/index'),
      name: 'ImportJd',
      meta: { title: '上传JD', icon: 'el-icon-document-add' }
    },
    {
      path: 'jdGuide',
      component: () => import('@/views/interviewelf/jdGuide/index'),
      name: 'JdGuide',
      meta: { title: 'JD指导建议', icon: 'el-icon-chat-dot-round' }
    },
    {
      path: 'generalQuestions',
      component: () => import('@/views/interviewelf/generalQuestions/index'),
      name: 'GeneralQuestions',
      meta: { title: '通用问题', icon: 'el-icon-s-help' }
    }
  ]
}

export default interviewElfRouter
