import { shallowRef } from 'vue';
import { createDemoService } from './services/demo.js';
export { roles, getRole, dimensions } from './services/roles.js';

let storage;
try { storage = window.localStorage; } catch { /* Demo still works in memory. */ }
const service = createDemoService({ storage });
const state = shallowRef(service.snapshot());
const call = (method, ...args) => {
  try { return service[method](...args); }
  finally { state.value = service.snapshot(); }
};

// Views consume a business service, never legacy network paths or fixture internals.
export const studio = { state, call };
export const sessionPath = (s) => `/interviews/${s.id}/${s.status === 'completed' ? (s.analysis && s.analysis.status !== 'completed' ? 'analysis' : 'report') : s.status === 'ready' ? 'plan' : 'room'}`;
export const dateLabel = (value) => new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric' }).format(new Date(value));
