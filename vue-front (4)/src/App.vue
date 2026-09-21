<template>
  <div id="app-wrapper" :style="{ '--shell-background': shellBackgroundColor }">
    <main class="main-content">
      <RouterView v-slot="{ Component }">
        <component :is="Component" v-on="routeListeners" />
      </RouterView>
    </main>

    <TabBar
      v-if="showTabBar"
      :current-tab="currentTab"
      @change="handleTabChange"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { RouterView, useRoute, useRouter } from 'vue-router';
import TabBar from '@/components/common/TabBar.vue';
import { useJdStore } from '@/stores/jd';
import { useInterviewStore } from '@/stores/interview';
import { useResumeStore } from '@/stores/resume';

const router = useRouter();
const route = useRoute();
const jdStore = useJdStore();
const interviewStore = useInterviewStore();
const resumeStore = useResumeStore();

const shellBackgroundColor = computed(() => route.meta.shellBackground || '#eaddd3');
const showTabBar = computed(() => route.meta.showTabBar === true);

const currentTab = computed(() => {
  if (route.meta.tabKey) {
    return route.meta.tabKey;
  }

  if (route.path === '/upload' || route.path.startsWith('/resume')) {
    return 'upload';
  }

  if (route.path.startsWith('/profile')) {
    return 'profile';
  }

  return 'home';
});

const getRedirectQuery = () => {
  const redirect =
    typeof route.query.redirect === 'string' && route.query.redirect
      ? route.query.redirect
      : '';

  return redirect ? { redirect } : undefined;
};

const buildRouteQuery = ({ preview = false, source = '' } = {}) => {
  const query = {};
  const redirectQuery = getRedirectQuery();

  if (redirectQuery) {
    Object.assign(query, redirectQuery);
  }

  if (preview) {
    query.preview = '1';
  }

  if (source) {
    query.source = source;
  }

  return Object.keys(query).length ? query : undefined;
};

const getPostAuthPath = () => {
  if (typeof route.query.redirect === 'string' && route.query.redirect) {
    return route.query.redirect;
  }

  return '/account/security';
};

const getSourceBackTarget = () => {
  const source = typeof route.query.source === 'string' ? route.query.source : '';

  if (source === 'security') {
    return { name: 'account-security' };
  }

  return null;
};

const goBack = () => {
  const sourceBackTarget = getSourceBackTarget();

  if (sourceBackTarget) {
    router.push(sourceBackTarget);
    return;
  }

  const backTo = route.meta.backTo;

  if (typeof backTo === 'string' && backTo) {
    router.push(backTo);
    return;
  }

  router.back();
};

const openInterview = () => {
  interviewStore.reset();
  router.push({ name: 'interview' });
};

const openJdInput = () => {
  jdStore.resetAnalysis();
  jdStore.clearDraftText();
  router.push({ name: 'jd-input' });
};

const openResumeUpload = () => {
  resumeStore.resetUploadState();
  router.push({ name: 'resume-upload' });
};

const openResumeLibrary = () => {
  router.push({ name: 'resume-library' });
};

const openInterviewHistory = () => {
  router.push({ name: 'profile-interview-history' });
};

const openJdHistory = () => {
  router.push({ name: 'profile-jd-history' });
};

const openUserInfo = () => {
  router.push({ name: 'profile-user-info' });
};

const openSettings = () => {
  router.push({ name: 'profile-settings' });
};

const openAccountSecurity = () => {
  router.push({ name: 'account-security' });
};

const openLogin = () => {
  const fromSecurity = route.name === 'account-security';

  router.push({
    name: 'account-login',
    query: buildRouteQuery({
      preview: fromSecurity,
      source: fromSecurity ? 'security' : '',
    }),
  });
};

const openRegister = () => {
  router.push({
    name: 'account-register',
    query: buildRouteQuery(),
  });
};

const openResetPassword = () => {
  const fromSecurity = route.name === 'account-security';

  router.push({
    name: 'account-reset-password',
    query: buildRouteQuery({
      source: fromSecurity ? 'security' : '',
    }),
  });
};

const handleStartParse = (jdText) => {
  jdStore.parseCurrentJd(jdText);
  jdStore.clearDraftText();
  router.push({ name: 'jd-result' });
};

const handleStartResumeAnalysis = async (targetJdText) => {
  try {
    await resumeStore.uploadAndAnalyzeCurrentResume(targetJdText);
    router.push({ name: 'resume-ai-result' });
  } catch (error) {
    console.error('简历上传或分析失败', error);
  }
};

const handleLoggedIn = () => {
  router.replace(getPostAuthPath());
};

const handleRegistered = () => {
  router.replace(getPostAuthPath());
};

const handleLoggedOut = () => {
  router.replace({ name: 'account-login' });
};

const handleResetSuccess = () => {
  const sourceBackTarget = getSourceBackTarget();

  if (sourceBackTarget) {
    router.replace(sourceBackTarget);
    return;
  }

  router.replace({
    name: 'account-login',
    query: getRedirectQuery(),
  });
};

const handleTabChange = (tabName) => {
  if (tabName === 'home') {
    router.push({ name: 'home' });
    return;
  }

  if (tabName === 'upload') {
    router.push({ name: 'upload' });
    return;
  }

  if (tabName === 'profile') {
    router.push({ name: 'profile' });
    return;
  }

  router.push({ name: 'home' });
};

const closeInterviewSummary = () => {
  interviewStore.reset();
  router.push({ name: 'home' });
};

const routeListeners = computed(() => {
  switch (route.name) {
    case 'home':
      return {
        'start-interview': openInterview,
        'open-jd-input': openJdInput,
        'open-resume-upload': openResumeUpload,
        'open-resume-library': openResumeLibrary,
      };

    case 'interview':
      return {
        close: goBack,
      };

    case 'interview-summary':
      return {
        close: closeInterviewSummary,
        'restart-interview': openInterview,
      };

    case 'profile':
      return {
        'open-user-info': openUserInfo,
        'open-settings': openSettings,
        'open-interview-history': openInterviewHistory,
        'open-jd-history': openJdHistory,
        'open-resume-library': openResumeLibrary,
      };

    case 'profile-user-info':
      return {
        back: goBack,
      };

    case 'profile-settings':
      return {
        back: goBack,
        'open-account-security': openAccountSecurity,
      };

    case 'profile-interview-history':
      return {
        back: goBack,
      };

    case 'profile-jd-history':
      return {
        back: goBack,
      };

    case 'jd-input':
      return {
        back: goBack,
        'start-parse': handleStartParse,
      };

    case 'jd-result':
      return {
        back: goBack,
        'start-interview': openInterview,
      };

    case 'resume-upload':
      return {
        back: goBack,
        'start-analysis': handleStartResumeAnalysis,
        'open-history': openResumeLibrary,
      };

    case 'resume-ai-result':
      return {
        back: goBack,
      };

    case 'resume-library':
      return {
        back: goBack,
        'open-upload': openResumeUpload,
      };

    case 'account-security':
      return {
        back: goBack,
        'open-reset': openResetPassword,
        'open-login': openLogin,
        'logged-out': handleLoggedOut,
      };

    case 'account-login':
      return {
        back: goBack,
        'open-register': openRegister,
        'open-reset': openResetPassword,
        'logged-in': handleLoggedIn,
      };

    case 'account-register':
      return {
        back: goBack,
        'open-login': openLogin,
        registered: handleRegistered,
      };

    case 'account-reset-password':
      return {
        back: goBack,
        'open-login': openLogin,
        'reset-success': handleResetSuccess,
      };

    default:
      return {};
  }
});
</script>

<style>
html,
body {
  margin: 0;
  padding: 0;
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
}

#app {
  width: 100%;
  min-height: 100dvh;
}

#app-wrapper {
  width: 100%;
  height: 100dvh;
  background-color: var(--shell-background, #eaddd3);
  position: relative;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: auto;
}

.main-content > * {
  flex-shrink: 0;
  min-width: 0;
}
</style>
