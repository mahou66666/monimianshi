import { ref } from 'vue';
import { realRequest, realLogout } from './real.js';
export const account = ref(null);
export const avatarUrl = ref('');
export const roles = { java:'Java 后端工程师', web:'Web 前端工程师', algorithm:'算法工程师', testing:'测试工程师' };
let revision = 0;
let pending = null;
export function clearAccount() {
  revision++;
  pending = null;
  account.value = null;
  if (avatarUrl.value) URL.revokeObjectURL(avatarUrl.value);
  avatarUrl.value = '';
}
export function refreshAccount() {
  if (pending) return pending;
  const request = loadAccount();
  pending = request;
  request.finally(() => { if (pending === request) pending = null; }).catch(() => {});
  return request;
}
async function loadAccount() {
  const current = ++revision;
  try {
    const profile = await realRequest('/me');
    let image = '';
    if (profile.hasAvatar) {
      try { image = URL.createObjectURL(await realRequest('/me/avatar', {binary:true})); }
      catch (e) { if (e.status === 401) throw e; }
    }
    if (current !== revision) { if (image) URL.revokeObjectURL(image); return; }
    if (avatarUrl.value) URL.revokeObjectURL(avatarUrl.value);
    avatarUrl.value = image;
    account.value = profile;
    return profile;
  } catch (e) { if (current === revision) clearAccount(); throw e; }
}
export async function logoutAccount() {
  try { await realLogout(); } catch (e) { if (e.status !== 401) throw e; }
  clearAccount();
}
