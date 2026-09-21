<template>
  <canvas ref="canvasRef" class="sprite-canvas"></canvas>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue';

const canvasRef = ref(null);

let animationFrameId = null;
let mouseX = 0;
let mouseY = 0;
let isMouseInside = false;
let targetPupilX = 0;
let targetPupilY = 0;
let currentPupilX = 0;
let currentPupilY = 0;

const handleMove = (event) => {
  isMouseInside = true;

  if (event.touches?.length) {
    mouseX = event.touches[0].clientX;
    mouseY = event.touches[0].clientY;
    return;
  }

  mouseX = event.clientX;
  mouseY = event.clientY;
};

const handleLeave = () => {
  targetPupilX = 0;
  targetPupilY = 0;
  isMouseInside = false;
};

const resizeCanvas = () => {
  const canvas = canvasRef.value;

  if (!canvas) {
    return;
  }

  const context = canvas.getContext('2d');
  const dpr = window.devicePixelRatio || 1;
  const rect = canvas.getBoundingClientRect();

  canvas.width = rect.width * dpr;
  canvas.height = rect.height * dpr;
  context.setTransform(dpr, 0, 0, dpr, 0, 0);
};

const drawSprite = (context, canvasWidth, canvasHeight) => {
  context.clearRect(0, 0, canvasWidth, canvasHeight);

  const centerX = canvasWidth / 2;
  const centerY = canvasHeight / 2;

  context.save();
  context.beginPath();

  const topY = centerY - 58;
  const bottomY = centerY + 56;
  const flatTop = 40;
  const flatBottom = 80;
  const bulgeX = 84;
  const bulgeY = centerY + 12;

  context.moveTo(centerX - flatTop / 2, topY);
  context.lineTo(centerX + flatTop / 2, topY);
  context.bezierCurveTo(
    centerX + flatTop / 2 + 35,
    topY,
    centerX + bulgeX,
    bulgeY - 45,
    centerX + bulgeX,
    bulgeY,
  );
  context.bezierCurveTo(
    centerX + bulgeX,
    bulgeY + 30,
    centerX + flatBottom / 2 + 30,
    bottomY,
    centerX + flatBottom / 2,
    bottomY,
  );
  context.lineTo(centerX - flatBottom / 2, bottomY);
  context.bezierCurveTo(
    centerX - flatBottom / 2 - 30,
    bottomY,
    centerX - bulgeX,
    bulgeY + 30,
    centerX - bulgeX,
    bulgeY,
  );
  context.bezierCurveTo(
    centerX - bulgeX,
    bulgeY - 45,
    centerX - flatTop / 2 - 35,
    topY,
    centerX - flatTop / 2,
    topY,
  );
  context.closePath();

  context.shadowColor = 'rgba(232, 206, 104, 0.4)';
  context.shadowBlur = 16;
  context.shadowOffsetY = 8;
  context.fillStyle = '#e8ce68';
  context.fill();
  context.restore();

  const eyeRadius = 26;
  const eyeOffset = 36;
  const eyeY = centerY - 2;

  context.fillStyle = '#ffffff';
  context.beginPath();
  context.arc(centerX - eyeOffset, eyeY, eyeRadius, 0, Math.PI * 2);
  context.fill();
  context.beginPath();
  context.arc(centerX + eyeOffset, eyeY, eyeRadius, 0, Math.PI * 2);
  context.fill();

  if (canvasRef.value && isMouseInside) {
    const rect = canvasRef.value.getBoundingClientRect();
    const globalCenterX = rect.left + rect.width / 2;
    const globalCenterY = rect.top + rect.height / 2;
    const deltaX = mouseX - globalCenterX;
    const deltaY = mouseY - globalCenterY;
    const distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    const angle = Math.atan2(deltaY, deltaX);
    const maxMoveRadius = 10;
    const sensitivity = 0.02;
    const moveDistance = Math.min(distance * sensitivity, maxMoveRadius);

    targetPupilX = Math.cos(angle) * moveDistance;
    targetPupilY = Math.sin(angle) * moveDistance;
  }

  currentPupilX += (targetPupilX - currentPupilX) * 0.15;
  currentPupilY += (targetPupilY - currentPupilY) * 0.15;

  const pupilRadius = 11;
  context.fillStyle = '#2c2c2c';
  context.beginPath();
  context.arc(centerX - eyeOffset + currentPupilX, eyeY + currentPupilY, pupilRadius, 0, Math.PI * 2);
  context.fill();
  context.beginPath();
  context.arc(centerX + eyeOffset + currentPupilX, eyeY + currentPupilY, pupilRadius, 0, Math.PI * 2);
  context.fill();
};

const renderLoop = () => {
  const canvas = canvasRef.value;

  if (canvas) {
    const context = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    drawSprite(context, canvas.width / dpr, canvas.height / dpr);
  }

  animationFrameId = requestAnimationFrame(renderLoop);
};

onMounted(() => {
  resizeCanvas();
  window.addEventListener('resize', resizeCanvas);
  window.addEventListener('mousemove', handleMove);
  window.addEventListener('touchmove', handleMove);
  window.addEventListener('mouseleave', handleLeave);
  renderLoop();
});

onUnmounted(() => {
  window.removeEventListener('resize', resizeCanvas);
  window.removeEventListener('mousemove', handleMove);
  window.removeEventListener('touchmove', handleMove);
  window.removeEventListener('mouseleave', handleLeave);

  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId);
  }
});
</script>

<style scoped>
.sprite-canvas {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}
</style>
