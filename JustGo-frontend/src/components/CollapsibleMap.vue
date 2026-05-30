<script setup lang="ts">
defineProps<{
  collapsed: boolean
}>()
</script>

<template>
  <section class="map-container" :class="{ collapsed }">
    <div class="map-area">
      <div class="map-placeholder">
        <div class="map-grid-mark" />
        <div class="map-title">城市活动地图</div>
        <div class="map-desc">附近正在发生的组局，会先在这里出现</div>
        <div class="map-pin" style="top: 25%; left: 30%" />
        <div class="map-pin" style="top: 45%; left: 55%" />
        <div class="map-pin" style="top: 35%; left: 70%" />
        <div class="map-pin" style="top: 60%; left: 40%" />
        <div class="map-pin" style="top: 20%; left: 65%" />
      </div>
    </div>
  </section>
</template>

<style scoped>
.map-container {
  background: var(--jg-bg);
  padding: 12px 12px 0;
}

.map-area {
  position: relative;
  overflow: hidden;
  border-radius: 28px;
  box-shadow: 0 18px 48px rgba(44, 49, 38, 0.12);
}

.map-container:not(.collapsed) .map-area {
  max-height: 240px;
}

.map-container.collapsed .map-area {
  max-height: 56px;
}

.map-placeholder {
  position: relative;
  height: 220px;
  overflow: hidden;
  background:
    linear-gradient(rgba(84, 116, 106, 0.12) 1px, transparent 1px),
    linear-gradient(90deg, rgba(84, 116, 106, 0.12) 1px, transparent 1px),
    radial-gradient(circle at 32% 26%, rgba(84, 116, 106, 0.22), transparent 24%),
    radial-gradient(circle at 70% 65%, rgba(23, 24, 21, 0.12), transparent 28%),
    #e9ece4;
  background-size:
    64px 64px,
    64px 64px,
    auto,
    auto,
    auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--jg-muted);
  transition:
    opacity 280ms var(--jg-ease),
    transform 280ms var(--jg-ease);
}

.map-container.collapsed .map-placeholder {
  opacity: 0.4;
  transform: translateY(-10px);
}

.map-grid-mark {
  width: 52px;
  height: 52px;
  margin-bottom: 10px;
  border-radius: 16px;
  background:
    linear-gradient(90deg, transparent 45%, rgba(255, 255, 255, 0.8) 45% 55%, transparent 55%),
    linear-gradient(transparent 45%, rgba(255, 255, 255, 0.8) 45% 55%, transparent 55%),
    var(--jg-accent);
  box-shadow: 0 14px 32px rgba(84, 116, 106, 0.22);
  transform: rotate(-8deg);
  animation: float-card 5s var(--jg-ease) infinite;
}

.map-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--jg-ink);
  margin-bottom: 2px;
}

.map-desc {
  font-size: 12px;
  max-width: 220px;
  text-align: center;
}

.map-pin {
  position: absolute;
  width: 14px;
  height: 14px;
  border-radius: 999px 999px 999px 3px;
  background: var(--jg-accent-deep);
  box-shadow: 0 8px 20px rgba(47, 79, 71, 0.24);
  transform: rotate(-45deg);
  animation: pin-rise 620ms var(--jg-ease) both;
  transition:
    opacity 280ms var(--jg-ease),
    transform 280ms var(--jg-ease);
}

.map-container.collapsed .map-pin {
  opacity: 0;
  transform: rotate(-45deg) scale(0.6);
}

.map-pin::after {
  content: '';
  position: absolute;
  inset: 4px;
  border-radius: 999px;
  background: var(--jg-surface);
}

@keyframes float-card {
  50% {
    transform: translateY(-6px) rotate(-5deg);
  }
}

@keyframes pin-rise {
  from {
    opacity: 0;
    transform: translateY(18px) rotate(-45deg) scale(0.82);
  }
}
</style>
