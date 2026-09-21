<template>
  <div class="bottom-nav">
    <div
        class="nav-item"
        :class="{ active: activeTab === 'home' }"
        @click="switchTab('home')"
    >
      <span class="nav-icon">⊞</span>
    </div>

    <div
        class="nav-item"
        :class="{ active: activeTab === 'upload' }"
        @click="switchTab('upload')"
    >
      <span class="nav-icon">⛶</span>
    </div>

    <div
        class="nav-item"
        :class="{ active: activeTab === 'profile' }"
        @click="switchTab('profile')"
    >
      <span class="nav-icon">👤</span>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

// 记录当前激活的标签，默认选中 'home'
const activeTab = ref('home');

// 定义向父组件发射的自定义事件
const emit = defineEmits(['change']);

// 点击切换标签的逻辑
const switchTab = (tabName) => {
  if (activeTab.value === tabName) return; // 如果点击的是当前高亮的，就不做处理

  activeTab.value = tabName; // 更新高亮状态
  emit('change', tabName);   // 通知父组件（比如 App.vue）用户点击了哪个标签，方便后续做路由跳转
};
</script>

<style scoped>
/* 底部导航栏容器 */
/* 替换 TabBar.vue 中的 .bottom-nav 样式 */
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 400px;
  background-color: #eaddd3; /* 完全贴合 App 整体的暖灰背景色 */
  display: flex;
  justify-content: space-around;
  /* 底部增加内边距，为了适配现代手机底部的操作横条 (Safe Area) */
  padding: 16px 0 36px 0;
  z-index: 999;

  /* 💡 核心视觉魔法：
     1. 去掉所有圆角，填满两侧空隙。
     2. 使用同色系的向上阴影，创造出一个“渐变羽化”的边缘。
     这样当页面向上滑动时，内容会像是柔和地消失在底部，而不是被生硬地切断。
  */
  border-radius: 0;
  box-shadow: 0 -24px 32px #eaddd3;
}

/* 单个图标的默认样式 */
.nav-item {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #a0a0a0;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease; /* 添加一点点击过渡动画让交互更平滑 */
}

/* 选中后的高亮样式 */
.nav-item.active {
  background-color: white;
  color: #2c2c2c;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
</style>