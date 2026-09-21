<template>
  <div class="home-container" @mousemove="trackEyes" @touchmove="trackEyes">
    <div class="top-grid">
      <div class="card dark-card span-row">
        <div class="icon-wrapper dark-icon">
          <span class="icon">📝</span>
        </div>
        <div class="card-text">
          <h2>简历<br>修改</h2>
          <p class="subtitle">AI 智能润色 &gt;</p>
        </div>
      </div>

      <div class="card light-card">
        <div class="icon-wrapper yellow-icon">
          <span class="icon">⛶</span>
        </div>
        <div class="card-info">
          <h3>JD解析</h3>
          <p>岗位匹配度分析</p>
        </div>
      </div>

      <div class="card light-card">
        <div class="icon-wrapper green-icon">
          <span class="icon">📄</span>
        </div>
        <div class="card-info">
          <h3>我的简历</h3>
          <p>多版本管理</p>
        </div>
      </div>
    </div>

    <div class="assistant-card">
      <div class="tag">沉浸式对练模式</div>
      <h1 class="main-title">面试助手</h1>
      <h2 class="sub-title">AGENT</h2>

      <div class="character-container">
        <div class="character-body">
          <div class="eye">
            <div class="pupil" :style="{ transform: pupilTransform }"></div>
          </div>
          <div class="eye">
            <div class="pupil" :style="{ transform: pupilTransform }"></div>
          </div>
        </div>
      </div>

      <button class="action-btn">
        开始模拟面试 <span class="arrow">➔</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';

// 用于记录鼠标/手指的位置
const mouseX = ref(0);
const mouseY = ref(0);
const windowWidth = ref(375); // 默认移动端宽度
const windowHeight = ref(812);

onMounted(() => {
  windowWidth.value = window.innerWidth;
  windowHeight.value = window.innerHeight;
});

// 追踪光标或手指位置
const trackEyes = (event) => {
  if (event.touches && event.touches.length > 0) {
    mouseX.value = event.touches[0].clientX;
    mouseY.value = event.touches[0].clientY;
  } else {
    mouseX.value = event.clientX;
    mouseY.value = event.clientY;
  }
};

// 计算眼球的偏移量
const pupilTransform = computed(() => {
  const percentX = (mouseX.value / windowWidth.value) - 0.5;
  const percentY = (mouseY.value / windowHeight.value) - 0.5;

  const moveX = percentX * 12;
  const moveY = percentY * 12;

  return `translate(${moveX}px, ${moveY}px)`;
});
</script>

<style scoped>
/* 首页专属容器样式 */
.home-container {
  /* 底部留出 100px 的内边距，防止内容被全局的 TabBar 遮挡 */
  padding: 20px 16px 100px 16px;
  box-sizing: border-box;
  position: relative;
}

/* 顶部网格布局 */
.top-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 110px 110px;
  gap: 16px;
  margin-bottom: 20px;
}

.card {
  border-radius: 24px;
  padding: 20px;
  display: flex;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  box-sizing: border-box;
}

.span-row {
  grid-row: span 2;
  flex-direction: column;
  justify-content: space-between;
}

.dark-card {
  background-color: #2c2c2c;
  color: white;
}

.light-card {
  background-color: white;
  align-items: center;
  padding: 16px;
  gap: 12px;
}

/* 图标样式 */
.icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.dark-icon { background-color: #3f3f3f; color: #e2cd6d; }
.yellow-icon { background-color: #e2cd6d; color: white; width: 48px; height: 48px; border-radius: 50%;}
.green-icon { background-color: #8da372; color: white; width: 48px; height: 48px; border-radius: 50%;}

/* 卡片文本 */
.card-text h2 { margin: 0; font-size: 24px; line-height: 1.3; }
.subtitle { color: #888; font-size: 12px; margin-top: 8px; }
.card-info h3 { margin: 0; font-size: 16px; color: #333; }
.card-info p { margin: 4px 0 0; font-size: 11px; color: #888; }

/* 中间面试助手卡片 */
.assistant-card {
  background-color: white;
  border-radius: 32px;
  padding: 24px;
  text-align: left;
  box-shadow: 0 8px 24px rgba(0,0,0,0.04);
  position: relative;
}

.tag {
  display: inline-block;
  background-color: #f0f0f0;
  color: #666;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 16px;
}

.main-title { margin: 0; font-size: 28px; color: #222; font-weight: 800; }
.sub-title { margin: 4px 0 0; font-size: 20px; color: #e2cd6d; font-weight: 800; letter-spacing: 1px; }

/* 动态卡通人物 */
.character-container {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.character-body {
  width: 140px;
  height: 100px;
  background-color: #e2cd6d;
  border-radius: 50px 50px 40px 40px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  box-shadow: 0 20px 40px rgba(226, 205, 109, 0.3);
}

.eye {
  width: 32px;
  height: 32px;
  background-color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pupil {
  width: 14px;
  height: 14px;
  background-color: #2c2c2c;
  border-radius: 50%;
  transition: transform 0.1s ease-out;
}

/* 底部按钮 */
.action-btn {
  width: 100%;
  background-color: #2c2c2c;
  color: white;
  border: none;
  padding: 18px 0;
  border-radius: 20px;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.arrow {
  background-color: rgba(255,255,255,0.2);
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}
</style>