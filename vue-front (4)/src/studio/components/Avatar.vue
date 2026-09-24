<script setup>
import { account } from '../account.js';
defineProps({ size: { default: 36 } });
</script>
<template><img class="profile-avatar" :style="{width:size+'px',height:size+'px'}" :src="account.state.value.avatar || '/images/interview-fairy-transparent.png'" :alt="account.state.value.nickname+'的头像'"/></template>
