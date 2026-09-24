import test from 'node:test';
import assert from 'node:assert/strict';
import { realRequest, realLogin, realLogout, clearRealSession } from './real.js';

test('real client authenticates requests and revokes local session after logout', async () => {
  const previous=globalThis.fetch;const requests=[];
  globalThis.fetch=async(url,options)=>{requests.push({url,options});return {ok:true,status:200,json:async()=>url.endsWith('/login')?{token:'test-only-token'}:url.endsWith('/me')?{userId:1}:{success:true}};};
  try {
    await realLogin('19900000001','test-only-password');
    assert.equal(requests[0].options.headers.Authorization,undefined);
    assert.equal(requests[1].options.headers.Authorization,'Bearer test-only-token');
    await realLogout();await realRequest('/capabilities');
    assert.equal(requests.at(-1).options.headers.Authorization,undefined);
  } finally {globalThis.fetch=previous;clearRealSession();}
});
test('real client never falls back to mock on network or authentication failure', async () => {
  const previous=globalThis.fetch;
  try {
    globalThis.fetch=async()=>{throw Error('offline');};
    await assert.rejects(realRequest('/me'),/不会切换为 Mock/);
    globalThis.fetch=async()=>({status:401,ok:false});
    await assert.rejects(realRequest('/me'),e=>e.status===401);
    globalThis.fetch=async()=>({status:200,ok:true,json:async()=>{throw Error('HTML instead of JSON');}});
    await assert.rejects(realRequest('/me'),/返回异常/);
  } finally {globalThis.fetch=previous;clearRealSession();}
});
