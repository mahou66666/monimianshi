<template>
  <div class="assistant-card">
    <div class="header-area">
      <div class="tag">沉浸式对练模式</div>
      <h1 class="main-title">面试助手</h1>
      <h2 class="sub-title">AGENT</h2>
    </div>

    <div class="canvas-container">
      <div class="pure-glow-bg"></div>
      <AgentSprite class="sprite-canvas" />
    </div>

    <button class="action-btn" type="button" @click="emit('start')">
      开始模拟面试
      <span class="arrow">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="5" y1="12" x2="19" y2="12"></line>
          <polyline points="12 5 19 12 12 19"></polyline>
        </svg>
      </span>
    </button>
  </div>
</template>

<script setup>
import AgentSprite from '@/components/common/AgentSprite.vue';

const emit = defineEmits(['start']);
</script>

<style scoped>
.assistant-card {
  min-width: 0;
  min-height: 400px;
  box-sizing: border-box;
  background-color: white;
  border-radius: 32px;
  padding: 20px 24px;
  text-align: left;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
  position: relative;
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.header-area {
  flex-shrink: 0;
}

.tag {
  display: inline-block;
  background-color: #f0f0f0;
  color: #666;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 8px;
}

.main-title {
  margin: 0;
  font-size: 28px;
  color: #222;
  font-weight: 800;
}

.sub-title {
  margin: 4px 0 0;
  font-size: 20px;
  color: #e2cd6d;
  font-weight: 800;
  letter-spacing: 1px;
}

.canvas-container {
  display: flex;
  justify-content: center;
  align-items: center;
  flex: 1;
  position: relative;
  width: 100%;
  min-height: 160px;
}

.pure-glow-bg {
  position: absolute;
  width: 250px;
  height: 250px;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 1) 0%, rgba(255, 255, 255, 0) 65%);
  z-index: 0;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.sprite-canvas {
  position: relative;
  z-index: 1;
  width: 260px;
  max-width: 100%;
  height: 220px;
  max-height: 100%;
  object-fit: contain;
  display: block;
}

@media (min-width: 768px) {
  .assistant-card { padding: 32px; }
}

.action-btn {
  width: 100%;
  background-color: #2c2c2c;
  color: white;
  border: none;
  padding: 16px 0;
  border-radius: 20px;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  position: relative;
  z-index: 2;
  flex-shrink: 0;
}

.arrow {
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.arrow svg {
  width: 14px;
  height: 14px;
}
</style>
