import test from 'node:test';
import assert from 'node:assert/strict';
import {account, refreshAccount, clearAccount} from './realAccount.js';

test('header and profile share a pending request and receive the same profile',async()=>{
  const previous=globalThis.fetch;let requests=0;
  globalThis.fetch=async()=>{requests++;return {ok:true,status:200,json:async()=>({nickname:'Test',hasAvatar:false})};};
  try { clearAccount();const [header,profile]=await Promise.all([refreshAccount(),refreshAccount()]);assert.equal(requests,1);assert.equal(header.nickname,'Test');assert.equal(profile.nickname,'Test'); }
  finally {globalThis.fetch=previous;clearAccount();}
});
test('a pending profile cannot restore account identity after logout',async()=>{
  const previous=globalThis.fetch;let finish;
  globalThis.fetch=()=>new Promise(resolve=>{finish=resolve;});
  try {clearAccount();const pending=refreshAccount();clearAccount();finish({ok:true,status:200,json:async()=>({nickname:'Previous user',hasAvatar:false})});await pending;assert.equal(account.value,null);}
  finally {globalThis.fetch=previous;clearAccount();}
});
