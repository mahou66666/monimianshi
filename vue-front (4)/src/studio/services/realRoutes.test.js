import test from 'node:test';
import assert from 'node:assert/strict';
import {createRouter,createMemoryHistory} from 'vue-router';
import {routes} from '../../router/realRoutes.js';
const router=createRouter({history:createMemoryHistory(),routes});
const match=path=>router.resolve(path).matched.at(-1);
test('the only reachable login component is the real password login',()=>{
  assert.match(String(match('/login').components.default),/RealLogin.vue/);
  assert.equal(match('/account/login').redirect,'/login');
  for(const route of routes.filter(r=>r.component)){
    assert.equal(route.meta.real,true);
    assert.doesNotMatch(String(route.component),/\/(Auth|Profile|Library|Room|Prepare|Report|Growth|History|Settings|Practice|Device|Analysis)\.vue/);
  }
});
test('old demo URLs cannot render demo pages; real report routes still match',()=>{
  for(const path of ['/interviews/demo-1/room','/interviews/demo-1/report','/practice/seed','/settings/demo','/legacy/home'])assert.ok(match(path).redirect,path);
  assert.equal(match('/library').redirect,'/resumes/real');
  assert.equal(match('/growth').redirect,'/growth/real');
  assert.match(String(match('/interviews/real/a/report').components.default),/RealReport.vue/);
  const target=match('/interviews/new?role=testing').redirect(router.resolve('/interviews/new?role=testing'));
  assert.equal(target.path,'/interviews/real');assert.equal(target.query.role,'testing');
});
