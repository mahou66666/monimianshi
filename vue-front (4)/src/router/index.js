import { createRouter, createWebHistory } from 'vue-router';
import { AUTH_EVENTS } from '@/utils/request';

const APP_TITLE = 'AI Mock Interview';
const AUTH_TOKEN_KEY = 'app_auth_token';
const UNAUTHORIZED_LISTENER_FLAG = '__app_unauthorized_listener_installed__';

const hasToken = () => {
  if (typeof window === 'undefined') {
    return false;
  }

  return Boolean(window.localStorage.getItem(AUTH_TOKEN_KEY));
};

const isAuthenticatedPreview = (route) =>
  route.meta?.allowAuthenticatedPreview === true && route.query?.preview === '1';

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/home/HomeView.vue'),
    meta: {
      title: '首页',
      showTabBar: true,
      tabKey: 'home',
      auth: 'public',
    },
  },
  {
    path: '/upload',
    name: 'upload',
    component: () => import('@/views/upload/UploadPlaceholderView.vue'),
    meta: {
      title: '开发中',
      showTabBar: true,
      tabKey: 'upload',
      auth: 'public',
    },
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/profile/ProfileView.vue'),
    meta: {
      title: '个人中心',
      showTabBar: true,
      tabKey: 'profile',
      auth: 'user',
    },
  },
  {
    path: '/profile/user-info',
    name: 'profile-user-info',
    component: () => import('@/views/profile/UserInfoView.vue'),
    meta: {
      title: '个人信息',
      showTabBar: false,
      auth: 'user',
      backTo: '/profile',
    },
  },
  {
    path: '/profile/settings',
    name: 'profile-settings',
    component: () => import('@/views/profile/SettingsView.vue'),
    meta: {
      title: '设置',
      showTabBar: false,
      auth: 'user',
      backTo: '/profile',
    },
  },
  {
    path: '/profile/interview-history',
    name: 'profile-interview-history',
    component: () => import('@/views/profile/InterviewHistoryView.vue'),
    meta: {
      title: '历史面试记录',
      showTabBar: false,
      auth: 'user',
      backTo: '/profile',
    },
  },
  {
    path: '/profile/jd-history',
    name: 'profile-jd-history',
    component: () => import('@/views/profile/JdHistoryView.vue'),
    meta: {
      title: '历史JD记录',
      showTabBar: false,
      auth: 'user',
      backTo: '/profile',
    },
  },
  {
    path: '/jd/input',
    name: 'jd-input',
    component: () => import('@/views/jd/JdInputView.vue'),
    meta: {
      title: 'JD 输入',
      showTabBar: false,
      auth: 'public',
      backTo: '/',
    },
  },
  {
    path: '/jd/result',
    name: 'jd-result',
    component: () => import('@/views/jd/JdResultView.vue'),
    meta: {
      title: 'JD 解析结果',
      showTabBar: false,
      auth: 'public',
      backTo: '/jd/input',
    },
  },
  {
    path: '/resume/upload',
    name: 'resume-upload',
    component: () => import('@/views/resume/ResumeUploadView.vue'),
    meta: {
      title: '简历上传',
      showTabBar: false,
      auth: 'user',
      backTo: '/',
    },
  },
  {
    path: '/resume/ai-result',
    name: 'resume-ai-result',
    component: () => import('@/views/resume/AiResumeResultView.vue'),
    meta: {
      title: 'AI 简历结果',
      showTabBar: false,
      auth: 'user',
      backTo: '/resume/upload',
    },
  },
  {
    path: '/resume/library',
    name: 'resume-library',
    component: () => import('@/views/resume/ResumeLibraryView.vue'),
    meta: {
      title: '简历库',
      showTabBar: false,
      auth: 'user',
      backTo: '/',
    },
  },
  {
    path: '/interview',
    name: 'interview',
    component: () => import('@/views/interview/InterviewView.vue'),
    meta: {
      title: '模拟面试',
      showTabBar: false,
      auth: 'user',
      backTo: '/',
    },
  },
  {
    path: '/interview/summary',
    name: 'interview-summary',
    component: () => import('@/views/interview/InterviewSummaryView.vue'),
    meta: {
      title: '面试总结',
      showTabBar: false,
      auth: 'user',
      backTo: '/interview',
      shellBackground: '#111111',
    },
  },
  {
    path: '/account/security',
    name: 'account-security',
    component: () => import('@/views/account/AccountSecurityView.vue'),
    meta: {
      title: '账号安全',
      showTabBar: false,
      auth: 'user',
      backTo: '/profile',
    },
  },
  {
    path: '/account/login',
    name: 'account-login',
    component: () => import('@/views/account/LoginView.vue'),
    meta: {
      title: '登录',
      showTabBar: false,
      auth: 'guest',
      allowAuthenticatedPreview: true,
      backTo: '/profile',
    },
  },
  {
    path: '/account/register',
    name: 'account-register',
    component: () => import('@/views/account/RegisterView.vue'),
    meta: {
      title: '注册',
      showTabBar: false,
      auth: 'guest',
      backTo: '/account/login',
    },
  },
  {
    path: '/account/reset-password',
    name: 'account-reset-password',
    component: () => import('@/views/account/ResetPasswordView.vue'),
    meta: {
      title: '重置密码',
      showTabBar: false,
      auth: 'public',
      backTo: '/account/login',
    },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});

const installUnauthorizedRedirect = (routerInstance) => {
  if (typeof window === 'undefined') {
    return;
  }

  if (window[UNAUTHORIZED_LISTENER_FLAG]) {
    return;
  }

  window[UNAUTHORIZED_LISTENER_FLAG] = true;

  window.addEventListener(AUTH_EVENTS.unauthorized, () => {
    const currentRoute = routerInstance.currentRoute.value;

    if (!currentRoute || currentRoute.name === 'account-login') {
      return;
    }

    const redirect =
      currentRoute.fullPath && currentRoute.fullPath !== '/account/login'
        ? currentRoute.fullPath
        : '/';

    routerInstance.replace({
      name: 'account-login',
      query: redirect ? { redirect } : undefined,
    });
  });
};

router.beforeEach((to) => {
  const authMode = to.meta.auth || 'public';
  const loggedIn = hasToken();

  if (authMode === 'user' && !loggedIn) {
    return {
      name: 'account-login',
      query: {
        redirect: to.fullPath,
      },
    };
  }

  if (authMode === 'guest' && loggedIn && !isAuthenticatedPreview(to)) {
    return {
      name: 'account-security',
    };
  }

  return true;
});

router.afterEach((to) => {
  if (typeof document === 'undefined') {
    return;
  }

  const pageTitle = to.meta.title ? `${to.meta.title} | ${APP_TITLE}` : APP_TITLE;
  document.title = pageTitle;
});

installUnauthorizedRedirect(router);

export default router;
