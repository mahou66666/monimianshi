<template>
  <div id="app-wrapper">
    <main class="main-content">
      <HomeView v-if="currentView === 'home'" />

      <div v-else-if="currentView === 'upload'" class="placeholder-page">
        <h2>🚀 简历上传页面开发中...<br>(搭档同学B的主战场)</h2>
      </div>

      <div v-else class="placeholder-page">
        <h2>👤 个人中心开发中...</h2>
      </div>
    </main>

    <TabBar @change="handleTabChange" />
  </div>
</template>

<script setup>
import { ref } from 'vue';

// 引入我们刚才写好的两个组件 (请确保路径与你创建的文件夹完全一致)
import HomeView from './views/home/index.vue';
import TabBar from './components/layout/TabBar.vue';

// 记录当前显示哪个页面，默认显示首页 'home'
const currentView = ref('home');

// 接收 TabBar 组件传上来的 'change' 事件，并更新当前视图
const handleTabChange = (tabName) => {
  currentView.value = tabName;
};
</script>

<style>
/* 1. 全局样式重置 */
html, body {
  margin: 0;
  padding: 0;
  /* 如果你在电脑浏览器上看，外围背景是浅灰色的，方便区分出手机屏幕范围 */
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

/* 2. 模拟移动端 App 的外壳容器 */
#app-wrapper {
  max-width: 400px; /* 限制最大宽度，模拟手机屏幕 */
  margin: 0 auto;   /* 在电脑屏幕上居中显示 */
  min-height: 100vh;
  background-color: #eaddd3; /* 原型图的暖灰底色 */
  position: relative;
  overflow-x: hidden;
  box-shadow: 0 0 20px rgba(0,0,0,0.1); /* 加一点阴影更有立体感 */
}

/* 3. 主体内容区占满高度 */
.main-content {
  min-height: 100vh;
}

/* 4. 临时占位页面的样式 (搭档写好上传页后就可以删掉了) */
.placeholder-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  text-align: center;
  color: #888;
  font-size: 16px;
  line-height: 1.6;
}
</style>