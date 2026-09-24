<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { account, avatarUrl, roles, refreshAccount, logoutAccount } from '../services/realAccount.js';
const route=useRoute(), router=useRouter(), open=ref(false), root=ref(null), trigger=ref(null), error=ref(''), busy=ref(false);
function close(event) { if (!root.value?.contains(event.target)) open.value=false; }
function escape() { open.value=false; trigger.value?.focus(); }
async function logout() { busy.value=true; error.value=''; try { await logoutAccount(); open.value=false; await router.replace('/login'); } catch(e) { error.value=e.message; } finally { busy.value=false; } }
watch(()=>route.fullPath,()=>{open.value=false;error.value='';refreshAccount().catch(()=>{});},{immediate:true});
onMounted(()=>document.addEventListener('pointerdown',close));
onUnmounted(()=>document.removeEventListener('pointerdown',close));
</script>
<template><div ref="root" class="user-menu" @keydown.esc="escape" @focusout="event=>{if(!root?.contains(event.relatedTarget))open=false}">
<template v-if="account"><button ref="trigger" class="user-trigger" :aria-expanded="open" aria-controls="account-dropdown" @click="open=!open"><img v-if="avatarUrl" :src="avatarUrl" alt=""/><span v-else class="user-initial">{{account.nickname?.slice(0,1)||'我'}}</span><span class="user-name">{{account.nickname}}</span><span aria-hidden="true">⌄</span></button>
<div v-if="open" id="account-dropdown" class="user-dropdown"><strong>{{account.nickname}}</strong><p class="small muted">{{roles[account.roleId]}} · {{account.level}}</p><hr/><RouterLink to="/profile">个人资料</RouterLink><RouterLink to="/account">账户与安全</RouterLink><hr/><button :disabled="busy" @click="logout">{{busy?'正在退出…':'退出登录'}}</button><p v-if="error" class="error" role="alert">{{error}}</p></div></template>
<div v-else class="inline"><RouterLink to="/login" class="text-link">登录</RouterLink><RouterLink to="/register" class="button secondary">注册账号</RouterLink></div>
</div></template>
<style scoped>
.user-menu{position:relative}.user-trigger{display:flex;align-items:center;gap:10px;border:0;background:transparent;min-height:44px;padding:4px 8px;border-radius:10px}.user-trigger img,.user-initial{width:36px;height:36px;border-radius:50%;object-fit:cover}.user-initial{display:grid;place-items:center;background:var(--sage);font-weight:700}.user-name{max-width:140px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.user-dropdown{position:absolute;right:0;top:calc(100% + 8px);width:250px;background:white;border:1px solid var(--line);box-shadow:0 12px 36px #292c271a;border-radius:14px;padding:18px;z-index:100}.user-dropdown a,.user-dropdown button{display:block;width:100%;padding:10px;text-align:left;border:0;background:none;border-radius:8px}.user-dropdown a:hover,.user-dropdown button:hover{background:var(--sage)}.user-dropdown hr{margin:10px 0}.user-dropdown strong{overflow-wrap:anywhere}@media(max-width:767px){.user-name{max-width:90px}.user-trigger{gap:6px}}
</style>
