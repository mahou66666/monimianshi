// This client never imports the legacy request module (which defaults to Mock).
const key = 'interview-studio:real-session';
let token = '';
try { token = sessionStorage.getItem(key) || ''; } catch { /* session stays in memory */ }
export function clearRealSession() { token = ''; try { sessionStorage.removeItem(key); } catch {} }
export async function realRequest(path, { method='GET', body, binary=false, timeout=30000 }={}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body && !(body instanceof FormData)) headers['Content-Type'] = 'application/json';
  let response;
  try { response = await fetch(`/api/studio${path}`, { method, headers, body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined, cache:'no-store', signal:AbortSignal.timeout(timeout) }); }
  catch { throw new Error('真实服务暂不可用，请检查服务连接后重试；不会切换为 Mock。'); }
  if (response.status === 401) { clearRealSession(); const e = new Error('请登录真实账户，或登录状态已过期。'); e.status=401; throw e; }
  if (binary && response.ok) return response.blob();
  const result = await response.json().catch(() => null);
  if (!response.ok || !result) throw new Error(result?.message || '真实服务返回异常，请稍后重试。');
  return result;
}
export async function realLogin(phone,password) {
  return authenticate('/auth/login',{phone,password});
}
export async function realSmsLogin(phone,code,challengeId) {
  return authenticate('/auth/sms/login',{phone,code,challengeId});
}
async function authenticate(path,body) {
  clearRealSession();
  const result=await realRequest(path,{method:'POST',body});
  token=result.token;
  try { sessionStorage.setItem(key,token); } catch { /* session stays in memory */ }
  return realRequest('/me');
}
export async function realLogout() { await realRequest('/auth/logout',{method:'POST'});clearRealSession(); }
