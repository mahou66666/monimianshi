<template>
  <div class="upload-placeholder">
    <section class="placeholder-card">
      <div class="placeholder-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"></path>
        </svg>
      </div>
      <p class="eyebrow">Uploading Workspace</p>
      <h1>功能开发中</h1>
      <p class="description">
        底部中间导航暂时保留为开发中状态，不再直接跳转到简历智能润色。
      </p>
      <p class="hint">
        需要使用简历智能润色时，可以从首页入口进入。
      </p>
    </section>
  </div>
</template>

<style scoped>
.upload-placeholder {
  width: 100%;
  min-height: 100%;
  height: auto;
  padding: 24px 20px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: auto;
  background-image: radial-gradient(circle at 1px 1px, rgba(0, 0, 0, 0.04) 1px, transparent 0);
  background-size: 15px 15px;
}

.placeholder-card {
  margin-top: 8px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 32px;
  padding: 32px 24px;
  text-align: center;
  box-shadow: 0 18px 36px rgba(99, 73, 26, 0.08);
}

.placeholder-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 18px;
  border-radius: 22px;
  background: #ffffff;
  color: #2c2c2c;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.06);
}

.placeholder-icon svg {
  width: 28px;
  height: 28px;
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #9d9078;
  font-weight: 700;
}

.placeholder-card h1 {
  margin: 0;
  font-size: 28px;
  color: #2c2c2c;
}

.description {
  margin: 14px 0 0;
  font-size: 15px;
  line-height: 1.8;
  color: #5d564b;
}

.hint {
  margin: 18px 0 0;
  padding: 14px 16px;
  border-radius: 18px;
  background: #f7f2e8;
  color: #7a705e;
  font-size: 13px;
  line-height: 1.7;
}
</style>
