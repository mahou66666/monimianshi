<script setup>
import { PhHouse, PhChatCircle, PhBooks, PhChartBar, PhArrowRight, PhArrowLeft, PhUserCircle, PhCode, PhBrowser, PhBrain, PhBug, PhCheckCircle, PhClock, PhMicrophone, PhPaperPlaneTilt, PhPause, PhSparkle } from '@phosphor-icons/vue';
defineProps({name:String,size:{default:22}});
const icons={home:PhHouse,interview:PhChatCircle,library:PhBooks,growth:PhChartBar,right:PhArrowRight,left:PhArrowLeft,user:PhUserCircle,code:PhCode,browser:PhBrowser,brain:PhBrain,bug:PhBug,test:PhBug,check:PhCheckCircle,clock:PhClock,mic:PhMicrophone,send:PhPaperPlaneTilt,pause:PhPause,sparkle:PhSparkle};
</script>
<template><component :is="icons[name] || PhSparkle" :size="size" aria-hidden="true"/></template>
