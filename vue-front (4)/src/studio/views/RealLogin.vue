<script setup>
import {ref,onMounted} from 'vue';
import {useRoute,useRouter} from 'vue-router';
import {realLogin,realSmsLogin,realRequest} from '../services/real.js';
const route=useRoute(),router=useRouter(),profile=ref(null),phone=ref(''),password=ref(''),busy=ref(false),loading=ref(true),error=ref('');
const method=ref('password'),code=ref(''),challenge=ref(''),sentPhone=ref(''),notice=ref(''),smsEnabled=ref(false);
async function sendCode(){busy.value=true;error.value='';notice.value='';challenge.value='';try{const result=await realRequest('/auth/sms/code',{method:'POST',body:{phone:phone.value}});challenge.value=result.challengeId;sentPhone.value=phone.value;notice.value='验证码已就绪，5 分钟内有效。';}catch(e){error.value=e.message;}finally{busy.value=false;}}
function destination(){const value=String(route.query.returnTo||'/');return /^\/(?:interviews\/real(?:[/?]|$)|resumes\/real(?:[/?]|$)|growth\/real(?:[/?]|$)|profile(?:[/?]|$)|account(?:[/?]|$))/.test(value)?value:'/';}
async function login(){busy.value=true;error.value='';try{if(method.value==='sms'){if(!challenge.value||sentPhone.value!==phone.value)throw Error('请先获取当前手机号的验证码');profile.value=await realSmsLogin(phone.value,code.value,challenge.value);}else profile.value=await realLogin(phone.value,password.value);password.value='';code.value='';await router.replace(destination());}catch(e){error.value=e.message;}finally{busy.value=false;}}
onMounted(async()=>{try{smsEnabled.value=(await realRequest('/capabilities')).smsLogin;}catch{}try{profile.value=await realRequest('/me');await router.replace(destination());}catch(e){if(e.status!==401)error.value=e.message;}finally{loading.value=false;}});
</script>
<template>
  <div class="page-heading"><div><p class="eyebrow">WELCOME BACK</p><h1>准备好，迈出下一步。</h1><p class="muted">登录后继续你的面试、简历与成长记录。</p></div></div>
  <section class="card login-card"><p v-if="route.query.notice==='registered'" class="notice" role="status">注册成功，请使用手机号和密码登录。</p><p v-if="route.query.notice==='password-reset'" class="notice" role="status">密码已更新，旧登录已失效，请重新登录。</p><p v-if="error" class="error" role="alert">{{error}}</p><p v-if="loading">正在检查账户…</p>
    <form v-else-if="!profile" @submit.prevent="login"><h2>登录账户</h2><div v-if="smsEnabled" class="tabs"><button type="button" :class="{active:method==='password'}" :disabled="busy" @click="method='password';error='';notice=''">密码登录</button><button type="button" :class="{active:method==='sms'}" :disabled="busy" @click="method='sms';error='';notice=''">手机验证码</button></div><label>手机号<input v-model="phone" type="tel" autocomplete="username" maxlength="11" required :disabled="busy"/></label><label v-if="method==='password'">密码<input v-model="password" type="password" autocomplete="current-password" maxlength="72" required :disabled="busy"/></label><template v-if="method==='sms'"><button type="button" class="button secondary" :disabled="busy||!/^1[0-9]{10}$/.test(phone)" @click="sendCode">获取验证码</button><label>验证码<input v-model="code" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" maxlength="6" required :disabled="busy"/></label><p v-if="notice" class="notice" role="status">{{notice}}</p></template><button class="button primary" :disabled="busy">{{busy?'正在登录…':'登录'}}</button><div class="section-heading"><RouterLink to="/register" class="text-link">注册账号</RouterLink><RouterLink to="/forgot-password" class="text-link">忘记密码？</RouterLink></div></form>
  </section>
</template>
<style scoped>.login-card{max-width:540px;margin:32px auto}.login-card label{margin:20px 0}.login-card .button{margin:8px 12px 8px 0}</style>
