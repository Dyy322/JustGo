<script setup lang="ts">
import { Plus } from '@element-plus/icons-vue'
</script>

<template>
  <router-link to="/activities/create" class="fab" aria-label="发布活动">
    <el-icon :size="22"><Plus /></el-icon>
  </router-link>
</template>

<style scoped>
.fab {
  position: fixed;
  bottom: calc(78px + env(safe-area-inset-bottom));
  right: 16px;
  z-index: 45;
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: var(--jg-ink);
  color: var(--jg-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 28px rgba(23, 24, 21, 0.28);
  animation: fab-in 380ms cubic-bezier(0.34, 1.56, 0.64, 1) both;
  animation-delay: 300ms;
}

.fab:active {
  transform: scale(0.94);
}

@keyframes fab-in {
  from {
    opacity: 0;
    transform: scale(0);
  }
}
</style>
