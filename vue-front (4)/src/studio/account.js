import { shallowRef } from 'vue';
import { createAccountService } from './services/account.js';
let storage; try { storage = window.localStorage; } catch {}
const service = createAccountService(storage);
const state = shallowRef(service.snapshot());
export const account = { state, call(method, ...args) { try { return service[method](...args); } finally { state.value = service.snapshot(); } } };
