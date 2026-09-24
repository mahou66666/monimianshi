import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_PROXY_TARGET?.trim()
  const interviewAgentTarget = env.VITE_INTERVIEW_AGENT_TARGET?.trim() || 'http://127.0.0.1:8010'
  const svGatewayTarget = env.VITE_SV_GATEWAY_TARGET?.trim() || 'http://127.0.0.1:8082'

  return {
    plugins: [
      vue(),
      ...(env.VITE_ENABLE_DEVTOOLS === 'true' ? [vueDevTools()] : []),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    server: {
      proxy: {
        '/api/studio': { target: env.VITE_STUDIO_API_TARGET?.trim() || 'http://127.0.0.1:8083', changeOrigin: true },
        ...(proxyTarget
          ? {
              '/api': {
                target: proxyTarget,
                changeOrigin: true,
              },
              '/resume': {
                target: proxyTarget,
                changeOrigin: true,
                // Browser page navigation belongs to Vue, not the Spring API.
                bypass(req) {
                  const path = req.url?.split('?')[0];
                  if (req.method === 'GET' && req.headers.accept?.includes('text/html') &&
                      ['/resume/upload', '/resume/ai-result', '/resume/library', '/resumes/real'].includes(path)) {
                    return '/index.html';
                  }
                },
              },
              '/resumeFragment': {
                target: proxyTarget,
                changeOrigin: true,
              },
              '/resumeScore': {
                target: proxyTarget,
                changeOrigin: true,
              },
            }
          : {}),
        '/interview/start': {
          target: interviewAgentTarget,
          changeOrigin: true,
          rewrite: () => '/api/interviews/start',
        },
        '/interview/answer': {
          target: interviewAgentTarget,
          changeOrigin: true,
          rewrite: () => '/api/interviews/answer',
        },
        '/interview/state': {
          target: interviewAgentTarget,
          changeOrigin: true,
          rewrite: () => '/api/interviews/state',
        },
        '/interview/asr/transcribe': {
          target: svGatewayTarget,
          changeOrigin: true,
          rewrite: () => '/api/voice/recognize',
        },
      },
    },
  }
})
