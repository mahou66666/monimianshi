<script setup>
import {computed,ref,onMounted} from 'vue';
import {realRequest} from '../services/real.js';
import Icon from '../components/Icon.vue';
const roles=[{id:'java',name:'Java 后端',icon:'code',skills:['Java / Spring','数据库','分布式系统']},{id:'web',name:'Web 前端',icon:'browser',skills:['JavaScript','Vue / React','性能与体验']},{id:'algorithm',name:'算法工程师',icon:'sparkle',skills:['机器学习','模型评估','工程落地']},{id:'testing',name:'测试工程师',icon:'check',skills:['测试设计','自动化','质量保障']}];
const roleId=ref('java'),rows=ref([]),loggedIn=ref(false),loading=ref(true),error=ref('');
const role=computed(()=>roles.find(r=>r.id===roleId.value)||roles[0]);
const active=computed(()=>rows.value.find(r=>r.status!=='completed'));
const latest=computed(()=>rows.value.find(r=>r.status==='completed'));
const roleName=id=>roles.find(r=>r.id===id)?.name||'面试';
onMounted(async()=>{try{const me=await realRequest('/me');loggedIn.value=true;if(roles.some(r=>r.id===me.roleId))roleId.value=me.roleId;rows.value=await realRequest('/interviews');}catch(e){if(e.status===401){loggedIn.value=false;rows.value=[];}else error.value=e.message;}finally{loading.value=false;}});
</script>
<template><div class="page-heading home-heading"><div><p class="eyebrow">YOUR NEXT CHAPTER</p><h1>让下一次面试，更有底气。</h1><p class="muted">从一次认真练习开始，找到自己的节奏。</p></div><span class="date-note">准备 · 表达 · 复盘 · 成长</span></div>
<section class="hero"><div class="hero-copy"><span class="eyebrow">专属于你的面试练习</span><h2>把准备，变成<br>从容的回答。</h2><p>选定岗位，带上你的经历。<br>练习、复盘，找到下一步。</p><label class="mobile-role-picker">目标岗位<select :value="role.id" @change="roleId=$event.target.value"><option v-for="item in roles" :key="item.id" :value="item.id">{{item.name}}</option></select></label><RouterLink :to="`/interviews/real?role=${role.id}`" class="button primary">开始真实面试 <Icon name="right"/></RouterLink><RouterLink v-if="!loading&&!loggedIn" to="/register" class="button secondary">注册账号</RouterLink><small>简历定向提问 · 逐题复盘 · 专项再练</small></div><img class="hero-mascot fairy-mascot" src="/images/interview-fairy-transparent.png" alt="陪伴练习的绿色光之精灵"/></section>
<div class="section-heading home-role-heading"><h2>今天，想练哪个方向？</h2><span class="muted">四种岗位，同一条成长路径</span></div><div class="role-grid home-role-grid"><button v-for="item in roles" :key="item.id" class="role-card" :class="{selected:role.id===item.id}" @click="roleId=item.id"><span class="role-icon"><Icon :name="item.icon" :size="26"/></span><strong>{{item.name}}</strong><small>{{item.skills.join(' · ')}}</small><span class="role-check" v-if="role.id===item.id"><Icon name="check" :size="18"/></span></button></div>
<p v-if="error" class="error" role="alert">{{error}}</p><p v-if="loading" class="notice">正在读取账户记录…</p>
<div class="two-col home-bottom"><section class="card sage"><div class="section-heading"><h2>接着上次，继续进步</h2><Icon name="clock"/></div>
<template v-if="active"><h3>{{roleName(active.roleId)}}</h3><p>已保存 {{active.turnCount}} 轮作答。</p><RouterLink :to="{path:'/interviews/real',query:{id:active.id}}" class="button dark">继续面试</RouterLink></template>
<template v-else><h3>{{loggedIn?'开启下一次练习':'登录后继续你的练习'}}</h3><p>每一次回答与复盘，都会保存在你的账户中。</p><RouterLink :to="loggedIn?`/interviews/real?role=${role.id}`:{path:'/login',query:{returnTo:`/interviews/real?role=${role.id}`}}" class="button dark">{{loggedIn?'开始面试':'登录账户'}}</RouterLink></template></section>
<section class="card"><div class="section-heading"><h2>最近一次复盘</h2><RouterLink to="/interviews/real/history" class="text-link">全部记录 →</RouterLink></div>
<template v-if="latest"><div class="score-row"><strong v-if="latest.overallScore!==null" class="score">{{latest.overallScore}}<small>/10</small></strong><div><h3>{{roleName(latest.roleId)}}</h3><p class="muted">{{new Date(latest.created_at).toLocaleDateString()}}</p></div></div><RouterLink :to="`/interviews/real/${latest.id}/report`" class="text-link">查看报告与逐题复盘 →</RouterLink></template><p v-else class="empty">{{loggedIn?'完成一次面试后，在这里查看报告。':'登录后查看你的面试报告。'}}</p></section></div></template>
