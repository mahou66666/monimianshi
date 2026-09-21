<template>
  <div class="top-grid">
    <div
      class="card dark-card span-row clickable-card"
      role="button"
      tabindex="0"
      @click="emit('open-resume-upload')"
      @keydown.enter="emit('open-resume-upload')"
      @keydown.space.prevent="emit('open-resume-upload')"
    >
      <div class="icon-wrapper dark-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="svg-icon">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
        </svg>
      </div>
      <div class="card-text">
        <h2>简历<br />修改</h2>
        <p class="subtitle">AI 智能润色 &gt;</p>
      </div>
    </div>

    <div
      class="card light-card clickable-card"
      role="button"
      tabindex="0"
      @click="emit('open-jd-input')"
      @keydown.enter="emit('open-jd-input')"
      @keydown.space.prevent="emit('open-jd-input')"
    >
      <div class="icon-wrapper yellow-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="svg-icon">
          <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"></path>
        </svg>
      </div>
      <div class="card-info">
        <h3>JD解析</h3>
        <p>岗位匹配度分析</p>
      </div>
    </div>

    <div
      class="card light-card clickable-card"
      role="button"
      tabindex="0"
      @click="emit('open-resume-library')"
      @keydown.enter="emit('open-resume-library')"
      @keydown.space.prevent="emit('open-resume-library')"
    >
      <div class="icon-wrapper green-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="svg-icon">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
          <polyline points="14 2 14 8 20 8"></polyline>
          <line x1="16" y1="13" x2="8" y2="13"></line>
          <line x1="16" y1="17" x2="8" y2="17"></line>
          <polyline points="10 9 9 9 8 9"></polyline>
        </svg>
      </div>
      <div class="card-info">
        <h3>我的简历</h3>
        <p>多版本管理</p>
      </div>
    </div>
  </div>
</template>

<script setup>
const emit = defineEmits(['open-jd-input', 'open-resume-upload', 'open-resume-library']);
</script>

<style scoped>
.svg-icon {
  width: 22px;
  height: 22px;
}

.top-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  grid-template-rows: repeat(2, minmax(100px, auto));
  gap: 12px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.card {
  min-width: 0;
  border-radius: 24px;
  padding: 20px;
  display: flex;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.clickable-card {
  cursor: pointer;
}

.clickable-card:active {
  transform: scale(0.98);
}

.span-row {
  grid-row: span 2;
  flex-direction: column;
  justify-content: space-between;
}

.dark-card {
  background-color: #2c2c2c;
  color: white;
  position: relative;
  overflow: hidden;
}

.dark-card::before {
  content: '';
  position: absolute;
  bottom: -15%;
  right: -10%;
  width: 160px;
  height: 160px;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='white' stroke-width='1' stroke-linecap='round' stroke-linejoin='round'><path d='M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7'/><path d='M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z'/></svg>");
  background-size: contain;
  background-repeat: no-repeat;
  opacity: 0.45;
  transform: rotate(-15deg);
  mix-blend-mode: overlay;
  pointer-events: none;
  z-index: 0;
}

.card-text,
.icon-wrapper {
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.light-card {
  background-color: white;
  align-items: center;
  padding: 16px;
  gap: 12px;
}

.icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dark-icon {
  background-color: #3f3f3f;
  color: #e2cd6d;
}

.yellow-icon {
  background-color: #e2cd6d;
  color: white;
  width: 48px;
  height: 48px;
  border-radius: 50%;
}

.green-icon {
  background-color: #8da372;
  color: white;
  width: 48px;
  height: 48px;
  border-radius: 50%;
}

.card-text h2 {
  margin: 0;
  font-size: 24px;
  line-height: 1.3;
}

.subtitle {
  color: #888;
  font-size: 12px;
  margin-top: 8px;
}

.card-info h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.card-info p {
  margin: 4px 0 0;
  font-size: 11px;
  color: #888;
}

@media (max-width: 374px) {
  .light-card { flex-direction: column; align-items: flex-start; padding: 12px; gap: 8px; }
  .card { padding: 14px; }
}

@media (min-width: 768px) and (max-width: 1199px) {
  .top-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); grid-template-rows: auto; }
  .span-row { grid-row: auto; gap: 20px; }
  .light-card { flex-direction: column; align-items: flex-start; justify-content: space-between; }
}

@media (min-width: 1200px) {
  .top-grid { margin-bottom: 0; grid-template-columns: 1fr; grid-template-rows: 1.3fr 1fr 1fr; }
  .span-row { grid-row: auto; flex-direction: row; align-items: center; gap: 20px; }
  .card-text h2 br { display: none; }
  .card { min-height: 150px; }
}
@media (max-width: 319px) {
  .top-grid { grid-template-columns: minmax(0, 1fr); grid-template-rows: auto; }
  .span-row { grid-row: auto; gap: 16px; }
  .card-text { min-width: 0; overflow-wrap: anywhere; }
}
</style>
