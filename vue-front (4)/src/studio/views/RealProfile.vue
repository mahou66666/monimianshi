<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { account, avatarUrl, roles, refreshAccount } from '../services/realAccount.js';
import { realRequest } from '../services/real.js';
const router=useRouter(), loading=ref(true), busy=ref(false), error=ref(''), notice=ref(''), picker=ref(null);
const form=reactive({nickname:'',roleId:'',level:'',graduation:''});
const completeness=computed(()=>account.value?Math.round([account.value.nickname,account.value.roleId,account.value.level,account.value.graduation,account.value.hasAvatar].filter(Boolean).length/5*100):0);
async function perform(fn) { busy.value=true;error.value='';notice.value='';try { await fn(); } catch(e) { error.value=e.message;if(e.status===401)await router.replace('/login?returnTo=/profile'); } finally { busy.value=false; } }
async function load() { loading.value=true;await perform(async()=>{const p=await refreshAccount();if(p)Object.assign(form,p);});loading.value=false; }
async function save() { await perform(async()=>{account.value=await realRequest('/me',{method:'PUT',body:form});Object.assign(form,account.value);notice.value='资料已更新。';}); }
async function upload(event) {
  const file=event.target.files[0];event.target.value='';if(!file)return;
  await perform(async()=>{
    if(!['image/png','image/jpeg','image/webp'].includes(file.type))throw Error('请选择 JPG、PNG 或 WebP 图片');
    if(file.size>2*1024*1024)throw Error('头像不能超过 2MB');
    // Normalize WebP for the existing Java ImageIO PNG/JPG decoder.
    const image=await createImageBitmap(file);
    let blob;
    try { const canvas=document.createElement('canvas');canvas.width=canvas.height=256;const ctx=canvas.getContext('2d');const side=Math.min(image.width,image.height);ctx.drawImage(image,(image.width-side)/2,(image.height-side)/2,side,side,0,0,256,256);blob=await new Promise(resolve=>canvas.toBlob(resolve,'image/png')); } finally { image.close(); }
    if(!blob)throw Error('图片读取失败，请换一张图片');
    const data=new FormData();data.append('file',blob,'avatar.png');await realRequest('/me/avatar',{method:'POST',body:data});await refreshAccount();notice.value='头像已更新。';
  });
}
async function resetAvatar() { await perform(async()=>{await realRequest('/me/avatar',{method:'DELETE'});await refreshAccount();notice.value='已恢复默认头像。';}); }
onMounted(load);
</script>
<template>
<div class="page-heading"><div><p class="eyebrow">YOUR PROFILE</p><h1>个人资料</h1><p class="muted">完善求职信息，让面试内容更贴近你的目标。</p></div></div>
<p v-if="error" class="error" role="alert">{{error}} <button v-if="!account" class="text-link" @click="load">重新加载</button></p><p v-if="notice" class="notice" role="status">{{notice}}</p>
<section v-if="loading" class="card">正在读取个人资料…</section>
<div v-else-if="account" class="profile-grid">
<aside class="card profile-summary"><img v-if="avatarUrl" :src="avatarUrl" alt="个人头像" class="profile-avatar"/><div v-else class="profile-avatar profile-fallback">{{account.nickname?.slice(0,1)||'我'}}</div><h2>{{account.nickname}}</h2><p class="muted">{{roles[account.roleId]}} · {{account.level}}</p><input ref="picker" type="file" hidden accept="image/png,image/jpeg,image/webp" @change="upload"/><button class="button secondary" :disabled="busy" @click="picker.click()">更换头像</button><button v-if="account.hasAvatar" class="text-link reset-avatar" :disabled="busy" @click="resetAvatar">恢复默认</button><p class="small muted">JPG / PNG / WebP · 最大 2MB<br/>上传后自动居中裁剪并保存</p><hr/><div class="completion-label"><span>资料完整度</span><strong>{{completeness}}%</strong></div><progress :value="completeness" max="100" aria-label="资料完整度"/><p class="small muted">按头像及四项基本信息计算</p></aside>
<section class="card profile-editor"><h2>基本信息</h2><p class="muted small">为下一次面试设定清晰的方向。</p><form @submit.prevent="save"><fieldset :disabled="busy"><label>昵称<input v-model="form.nickname" maxlength="20" required autocomplete="nickname"/></label><label>目标岗位<select v-model="form.roleId" required><option v-for="(label,id) in roles" :key="id" :value="id">{{label}}</option></select></label><div class="form-row"><label>求职阶段<select v-model="form.level" required><option>校招</option><option>初级</option><option>中级</option></select></label><label>毕业年份<input v-model="form.graduation" inputmode="numeric" pattern="20[0-9]{2}" maxlength="4" required/></label></div><div class="profile-actions"><button class="button primary">{{busy?'正在保存…':'保存修改'}}</button></div></fieldset></form></section>
</div></template>
<style scoped>
.profile-grid{display:grid;grid-template-columns:minmax(240px,3fr) minmax(0,7fr);gap:24px;align-items:start}.profile-summary{text-align:center}.profile-avatar{width:104px;height:104px;object-fit:cover;border-radius:50%;margin:8px auto 20px}.profile-fallback{display:grid;place-items:center;background:var(--sage);font-size:36px}.profile-summary h2{overflow-wrap:anywhere}.profile-summary input[hidden]{display:none}.reset-avatar{display:block;margin:10px auto}.completion-label{display:flex;justify-content:space-between;font-size:13px}progress{width:100%;height:8px;accent-color:#79835c}.profile-editor fieldset{border:0;padding:0;margin:24px 0 0;min-width:0}.profile-editor label{margin-bottom:22px}.profile-actions{display:flex;justify-content:flex-end;border-top:1px solid var(--line);padding-top:20px}@media(max-width:767px){.profile-grid{grid-template-columns:1fr;gap:16px}.profile-actions .button{width:100%}.profile-editor .form-row{grid-template-columns:1fr}}
</style>
