<script setup lang="ts">
import { watch } from 'vue'

const props = defineProps<{
  src: string | null
}>()

const emit = defineEmits<{
  close: []
}>()

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') emit('close')
}

watch(
  () => props.src,
  (val) => {
    if (val) document.addEventListener('keydown', onKeydown)
    else document.removeEventListener('keydown', onKeydown)
  },
)
</script>

<template>
  <Teleport to="body">
    <div v-if="src" class="viewer-overlay" @click="emit('close')">
      <img :src="src" class="viewer-img" @click.stop />
      <span class="viewer-close" @click="emit('close')">×</span>
    </div>
  </Teleport>
</template>

<style scoped>
.viewer-overlay {
  position: fixed;
  inset: 0;
  z-index: 4000;
  background: rgba(0, 0, 0, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: zoom-out;
}
.viewer-img {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 4px;
}
.viewer-close {
  position: absolute;
  top: 16px;
  right: 20px;
  font-size: 32px;
  color: #fff;
  cursor: pointer;
  opacity: 0.7;
  line-height: 1;
}
.viewer-close:hover {
  opacity: 1;
}
</style>
