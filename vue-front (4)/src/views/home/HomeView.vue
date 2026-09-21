<template>
  <div class="home-container">
    <FeatureGrid
      @open-jd-input="emit('open-jd-input')"
      @open-resume-upload="emit('open-resume-upload')"
      @open-resume-library="emit('open-resume-library')"
    />

    <AssistantCard @start="emit('start-interview')" />
  </div>
</template>

<script setup>
import FeatureGrid from './components/FeatureGrid.vue';
import AssistantCard from './components/AssistantCard.vue';

const emit = defineEmits([
  'start-interview',
  'open-jd-input',
  'open-resume-upload',
  'open-resume-library',
]);
</script>

<style scoped>
.home-container {
  width: 100%;
  max-width: 1200px;
  margin-inline: auto;
  padding: clamp(16px, 3vw, 32px);
  box-sizing: border-box;
  position: relative;
  background-image: radial-gradient(circle at 1px 1px, rgba(0, 0, 0, 0.04) 1px, transparent 0);
  background-size: 15px 15px;
  background-position: 0 0;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

@media (min-width: 1200px) {
  .home-container {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr);
    gap: 24px;
    align-items: stretch;
  }
}
</style>
