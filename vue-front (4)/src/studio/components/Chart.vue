<script setup>
import { onMounted,onBeforeUnmount,watch,ref } from 'vue';
import { init,use } from 'echarts/core';import { RadarChart,LineChart } from 'echarts/charts';import { RadarComponent,GridComponent,TooltipComponent } from 'echarts/components';import { CanvasRenderer } from 'echarts/renderers';
use([RadarChart,LineChart,RadarComponent,GridComponent,TooltipComponent,CanvasRenderer]);
const props=defineProps({values:Array,labels:Array,type:{default:'radar'}});const el=ref();let chart,observer;
const render=()=>{if(!chart)return;chart.setOption(props.type==='radar'?{radar:{indicator:props.labels.map(name=>({name,max:100})),radius:'62%',axisName:{color:'#696b63'},splitArea:{areaStyle:{color:['#faf9f4','#f3f2e9']}}},series:[{type:'radar',data:[{value:props.values}],symbolSize:6,lineStyle:{color:'#b69032'},itemStyle:{color:'#b69032'},areaStyle:{color:'#e4c976',opacity:.4}}]}:{grid:{left:38,right:18,top:20,bottom:32},tooltip:{trigger:'axis'},xAxis:{type:'category',data:props.labels},yAxis:{type:'value',min:0,max:100},series:[{type:'line',data:props.values,symbolSize:9,lineStyle:{color:'#b69032',width:3},itemStyle:{color:'#b69032'},areaStyle:{color:'#f1e4ba',opacity:.35}}]},true)};
onMounted(()=>{chart=init(el.value);render();observer=new ResizeObserver(()=>chart.resize());observer.observe(el.value)});watch(()=>[props.values,props.labels],render,{deep:true});onBeforeUnmount(()=>{observer?.disconnect();chart?.dispose()});
</script>
<template><div ref="el" class="chart" role="img" :aria-label="labels.map((x,i)=>`${x} ${values[i]} 分`).join('，')"></div></template>
