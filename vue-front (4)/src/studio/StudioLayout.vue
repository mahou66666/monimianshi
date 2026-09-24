<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import Icon from './components/Icon.vue';
import UserMenu from './components/UserMenu.vue';
import './studio.css';
import './next-pages.css';
import './phone-preview.css';
const route=useRoute();
const focused=computed(()=>route.meta.focus||(route.path==='/interviews/real'&&!!route.query.id));
const nav=[['/','home','首页'],['/interviews/real/history','interview','面试'],['/library','library','资料库'],['/growth/real','growth','成长']];
const active=(path)=>{
 if(path==='/')return route.path==='/';
 if(path==='/interviews/real/history')return route.path==='/interviews'||route.path.startsWith('/interviews/');
 if(path==='/growth/real')return route.path==='/growth'||route.path.startsWith('/growth/')||route.path.startsWith('/practice');
 if(path==='/library')return route.path==='/library'||route.path.startsWith('/library/')||route.path.startsWith('/resumes/real');
 return false;
};
</script>
<template><div class="device-preview"><div class="studio" :class="{'focus-layout':focused}">
<aside v-if="!focused" class="side-nav"><RouterLink to="/" class="brand"><span>面试精灵<small>INTERVIEW STUDIO</small></span></RouterLink><div class="nav-caption">我的工作台</div><nav><RouterLink v-for="[path,icon,label] in nav" :key="path" :to="path" :aria-label="label" :class="{active:active(path)}"><Icon :name="icon"/><span>{{label}}</span></RouterLink></nav><div class="sidebar-note"><Icon name="sparkle"/><p>每一次练习<br>都离理想 offer 更近。</p><small>循序渐进，看见成长</small></div></aside>
<div class="device-scroll"><div class="studio-main"><header v-if="!focused" class="topbar"><span>我的工作台 <span class="muted">/ {{route.meta.title}}</span></span><UserMenu/></header><main :class="focused?'room-main':'page'"><RouterView :key="route.fullPath"/></main></div></div>
<nav v-if="!focused" class="bottom-nav" aria-label="主导航"><RouterLink v-for="[path,icon,label] in nav" :key="path" :to="path" :aria-label="label" :class="{active:active(path)}"><Icon :name="icon"/><span>{{label}}</span></RouterLink></nav></div></div></template>
