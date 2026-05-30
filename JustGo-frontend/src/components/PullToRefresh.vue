<script setup lang="ts">
import { ref, watch } from 'vue'
import { ArrowDown, Loading } from '@element-plus/icons-vue'

const props = defineProps<{
  refreshing: boolean
}>()

const emit = defineEmits<{
  refresh: []
}>()

const containerRef = ref<HTMLElement | null>(null)
const pullDistance = ref(0)
const phase = ref<'idle' | 'pulling' | 'ready' | 'refreshing'>('idle')

let startY = 0
let scrollTopAtStart = 0

function onTouchStart(e: TouchEvent) {
  if (props.refreshing || !e.touches[0]) return
  startY = e.touches[0].clientY
  scrollTopAtStart = containerRef.value?.scrollTop ?? 0
}

function onTouchMove(e: TouchEvent) {
  if (props.refreshing || !e.touches[0]) return
  const currentY = e.touches[0].clientY
  const delta = currentY - startY

  if (scrollTopAtStart <= 0 && delta > 0) {
    pullDistance.value = Math.min(delta * 0.4, 80)
    phase.value = pullDistance.value >= 60 ? 'ready' : 'pulling'
  }
}

function onTouchEnd() {
  if (props.refreshing) return
  if (phase.value === 'ready') {
    phase.value = 'refreshing'
    pullDistance.value = 56
    emit('refresh')
  } else {
    phase.value = 'idle'
    pullDistance.value = 0
  }
}

watch(
  () => props.refreshing,
  (val) => {
    if (!val) {
      phase.value = 'idle'
      pullDistance.value = 0
    }
  },
)
</script>

<template>
  <div
    ref="containerRef"
    class="ptr-container"
    @touchstart.passive="onTouchStart"
    @touchmove.passive="onTouchMove"
    @touchend="onTouchEnd"
  >
    <div
      class="ptr-indicator"
      :class="phase"
      :style="{ height: pullDistance + 'px' }"
    >
      <span v-if="phase === 'refreshing'" class="ptr-spinner">
        <el-icon :size="18" class="spin"><Loading /></el-icon>
        刷新中...
      </span>
      <span v-else-if="phase === 'ready'">释放刷新</span>
      <span v-else-if="phase === 'pulling'">
        <el-icon :size="14"><ArrowDown /></el-icon>
        下拉刷新
      </span>
    </div>

    <div
      class="ptr-content"
      :style="{ transform: `translateY(${pullDistance}px)` }"
      :class="{ 'ptr-transition': phase === 'idle' }"
    >
      <slot />
    </div>
  </div>
</template>

<style scoped>
.ptr-container {
  position: relative;
  min-height: 100%;
  touch-action: pan-y;
  overscroll-behavior-y: contain;
}

.ptr-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-size: 12px;
  color: var(--jg-muted);
  gap: 6px;
  transition: height 200ms var(--jg-ease);
}

.ptr-indicator.ready {
  color: var(--jg-accent-deep);
  font-weight: 700;
}

.ptr-spinner {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.ptr-content {
  min-height: 100%;
}

.ptr-content.ptr-transition {
  transition: transform 280ms var(--jg-ease);
}

.spin {
  animation: spin 800ms linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
