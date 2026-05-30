<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { Close, Minus, Plus, Picture, Rank } from '@element-plus/icons-vue'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'

const props = withDefaults(
  defineProps<{
    src: string
    aspectRatio?: number
    cropShape?: 'rect' | 'round'
  }>(),
  {
    aspectRatio: 16 / 9,
    cropShape: 'rect',
  },
)

const emit = defineEmits<{
  confirm: [blob: Blob]
  cancel: []
}>()

const imgRef = ref<HTMLImageElement>()
const previewRef = ref<HTMLDivElement>()
let cropper: Cropper | null = null
const zoom = ref(0)
const zoomMin = ref(0)
const zoomMax = ref(100)
let zoomRatio = 0

function formatAspectRatio() {
  const ratio = props.aspectRatio
  if (Math.abs(ratio - 16 / 9) < 0.01) return '16:9'
  if (Math.abs(ratio - 4 / 3) < 0.01) return '4:3'
  if (Math.abs(ratio - 1) < 0.01) return '1:1'
  return ratio.toFixed(2)
}

function clampZoomRatio(ratio: number) {
  return Math.min(zoomMax.value / 100, Math.max(zoomMin.value / 100, ratio))
}

function updateZoomState(ratio: number) {
  zoomRatio = clampZoomRatio(ratio)
  zoom.value = Math.round(zoomRatio * 100)
}

function initCropper() {
  if (!imgRef.value || !previewRef.value) return
  cropper = new Cropper(imgRef.value, {
    viewMode: 1,
    dragMode: 'move',
    aspectRatio: props.aspectRatio,
    autoCropArea: 1,
    cropBoxMovable: false,
    cropBoxResizable: false,
    background: false,
    guides: true,
    center: true,
    highlight: true,
    preview: previewRef.value,
    toggleDragModeOnDblclick: false,
    ready() {
      const cropperInstance = cropper!
      const imageData = cropperInstance.getImageData()
      const containerData = cropperInstance.getContainerData()
      const naturalWidth = imageData.naturalWidth
      const fitRatio = Math.min(
        containerData.width / naturalWidth,
        containerData.height / naturalWidth / (imageData.height / naturalWidth),
      )
      zoomMin.value = Math.max(5, Math.round(fitRatio * 100))
      zoomMax.value = Math.max(zoomMin.value + 120, Math.round(fitRatio * 280))
      cropperInstance.zoomTo(fitRatio)
      updateZoomState(fitRatio)
    },
    zoom(event: Cropper.ZoomEvent) {
      updateZoomState(event.detail.ratio)
    },
  })
}

function handleZoomIn() {
  if (!cropper) return
  cropper.zoomTo(clampZoomRatio(zoomRatio + 0.1))
}

function handleZoomOut() {
  if (!cropper) return
  cropper.zoomTo(clampZoomRatio(zoomRatio - 0.1))
}

function handleZoomSlider(event: Event) {
  if (!cropper) return
  const nextZoom = Number((event.target as HTMLInputElement).value)
  cropper.zoomTo(nextZoom / 100)
}

function handleConfirm() {
  if (!cropper) return
  const canvas = cropper.getCroppedCanvas()
  canvas.toBlob(
    (blob) => {
      if (blob) emit('confirm', blob)
    },
    'image/jpeg',
    0.92,
  )
}

function handleCancel() {
  emit('cancel')
}

onMounted(() => {
  nextTick(initCropper)
})

onUnmounted(() => {
  cropper?.destroy()
  cropper = null
})
</script>

<template>
  <Teleport to="body">
    <div class="cropper-overlay" @click.self="handleCancel">
      <div class="cropper-modal">
        <div class="cropper-header">
          <div>
            <span class="cropper-eyebrow">图片编辑器</span>
            <h2>调整活动图片</h2>
            <p>拖动画面选择展示区域，确认后将自动压缩上传。</p>
          </div>
          <button class="cropper-close" aria-label="关闭裁剪弹窗" @click="handleCancel">
            <el-icon><Close /></el-icon>
          </button>
        </div>

        <div class="cropper-body">
          <div class="cropper-main">
            <div class="cropper-stage-label">
              <el-icon><Rank /></el-icon>
              <span>拖动图片调整构图</span>
            </div>
            <img ref="imgRef" :src="src" class="cropper-source" />
          </div>

          <div class="cropper-sidebar">
            <div class="preview-heading">
              <span class="preview-icon"><el-icon><Picture /></el-icon></span>
              <div>
                <strong>裁剪预览</strong>
                <small>{{ formatAspectRatio() }} 比例</small>
              </div>
            </div>

            <div class="preview-shell">
              <div ref="previewRef" class="preview-box" :class="cropShape" />
            </div>

            <div class="zoom-panel">
              <div class="zoom-label">
                <span>画面缩放</span>
                <strong>{{ zoom }}%</strong>
              </div>
              <div class="zoom-controls">
                <button class="zoom-btn" aria-label="缩小图片" @click="handleZoomOut">
                  <el-icon><Minus /></el-icon>
                </button>
                <div class="zoom-slider-wrap">
                  <input
                    type="range"
                    :min="zoomMin"
                    :max="zoomMax"
                    :value="zoom"
                    class="zoom-slider"
                    @input="handleZoomSlider"
                  />
                </div>
                <button class="zoom-btn" aria-label="放大图片" @click="handleZoomIn">
                  <el-icon><Plus /></el-icon>
                </button>
              </div>
            </div>

            <p class="preview-note">建议主体保留在中央区域，避免列表卡片裁切。</p>
          </div>
        </div>

        <div class="cropper-footer">
          <span>输出格式 JPG · 自动优化文件体积</span>
          <div class="footer-actions">
            <button class="btn-cancel" @click="handleCancel">取消</button>
            <button class="btn-confirm" @click="handleConfirm">使用此构图</button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.cropper-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  padding: 40px 24px;
  background: rgba(238, 236, 231, 0.7);
  display: grid;
  place-items: center;
  overflow: hidden;
  overscroll-behavior: contain;
  backdrop-filter: blur(6px);
  animation: overlay-in 180ms var(--jg-ease) both;
}
.cropper-modal {
  width: min(940px, calc(100vw - 96px));
  max-height: min(720px, calc(100dvh - 120px));
  margin: auto;
  background: #fdfcf9;
  border: 1px solid var(--jg-line);
  border-radius: 24px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 28px 90px rgba(12, 18, 15, 0.32);
  animation: modal-in 240ms var(--jg-ease) both;
}
.cropper-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 24px;
  color: var(--jg-ink);
  border-bottom: 1px solid var(--jg-line);
}
.cropper-eyebrow {
  display: block;
  margin-bottom: 5px;
  color: var(--jg-accent-deep);
  font-size: 11px;
  font-weight: 800;
}
.cropper-header h2 {
  font-size: 21px;
  font-weight: 900;
  line-height: 1.2;
}
.cropper-header p {
  margin-top: 5px;
  color: var(--jg-muted);
  font-size: 12px;
  line-height: 1.6;
}
.cropper-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 12px;
  background: var(--jg-surface-muted);
  color: var(--jg-muted);
  cursor: pointer;
}
.cropper-close:hover {
  background: var(--jg-accent-soft);
  color: var(--jg-accent-deep);
}
.cropper-body {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  overflow: hidden;
}
.cropper-main {
  position: relative;
  min-width: 0;
  min-height: 0;
  background: #242824;
  touch-action: none;
  overscroll-behavior: contain;
}
.cropper-stage-label {
  position: absolute;
  top: 14px;
  left: 14px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 8px 10px;
  border-radius: 999px;
  color: #fff;
  background: rgba(23, 24, 21, 0.94);
  font-size: 11px;
  font-weight: 700;
}
.cropper-source {
  display: block;
  max-width: 100%;
}
.cropper-main :deep(.cropper-container),
.cropper-main :deep(.cropper-wrap-box),
.cropper-main :deep(.cropper-canvas),
.cropper-main :deep(.cropper-drag-box),
.cropper-main :deep(.cropper-crop-box) {
  touch-action: none;
}
.cropper-sidebar {
  min-height: 0;
  padding: 20px;
  border-left: 1px solid var(--jg-line);
  display: flex;
  flex-direction: column;
  gap: 18px;
  background: #f6f4ee;
}
.preview-heading {
  display: flex;
  align-items: center;
  gap: 10px;
}
.preview-heading div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.preview-heading strong {
  color: var(--jg-ink);
  font-size: 13px;
  font-weight: 900;
}
.preview-heading small {
  color: var(--jg-muted);
  font-size: 11px;
}
.preview-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 12px;
  color: var(--jg-accent-deep);
  background: var(--jg-accent-soft);
}
.preview-shell {
  padding: 5px;
  border: 1px solid var(--jg-line);
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(44, 49, 38, 0.08);
}
.preview-box {
  width: 100%;
  aspect-ratio: v-bind(aspectRatio);
  overflow: hidden;
  border-radius: 12px;
  background: #e7e9e3;
}
.preview-box.round {
  border-radius: 50%;
}
.preview-box :deep(.cropper-preview) {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
.preview-box :deep(img) {
  display: block;
  max-width: none;
}
.zoom-panel {
  padding-top: 2px;
}
.zoom-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: var(--jg-muted);
  font-size: 12px;
}
.zoom-label strong {
  color: var(--jg-accent-deep);
  font-weight: 900;
}
.zoom-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}
.zoom-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border: 1px solid var(--jg-line);
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  color: var(--jg-ink-soft);
}
.zoom-btn:hover {
  border-color: var(--jg-accent);
  background: var(--jg-accent-soft);
  color: var(--jg-accent-deep);
}
.zoom-slider-wrap {
  flex: 1;
  min-width: 0;
}
.zoom-slider {
  display: block;
  width: 100%;
  accent-color: var(--jg-accent);
}
.preview-note {
  color: var(--jg-muted);
  font-size: 11px;
  line-height: 1.7;
}
.cropper-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid var(--jg-line);
  background: #fdfcf9;
}
.cropper-footer > span {
  margin-right: auto;
  color: var(--jg-muted);
  font-size: 11px;
}
.footer-actions {
  display: flex;
  gap: 10px;
}
.btn-cancel {
  padding: 10px 18px;
  border: 1px solid var(--jg-line);
  border-radius: 999px;
  background: transparent;
  color: var(--jg-ink-soft);
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
}
.btn-cancel:hover {
  border-color: var(--jg-line-strong);
  background: var(--jg-surface-muted);
}
.btn-confirm {
  padding: 10px 20px;
  border: none;
  border-radius: 999px;
  background: var(--jg-ink);
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 800;
  box-shadow: 0 12px 24px rgba(23, 24, 21, 0.16);
}
.btn-confirm:hover {
  background: var(--jg-accent-deep);
  transform: translateY(-1px);
}

@keyframes overlay-in {
  from {
    opacity: 0;
  }
}

@keyframes modal-in {
  from {
    opacity: 0;
    transform: translateY(14px) scale(0.98);
  }
}

@media (max-width: 720px) {
  .cropper-overlay {
    padding: 14px;
  }

  .cropper-modal {
    width: min(720px, calc(100vw - 28px));
    max-height: calc(100dvh - 56px);
    border-radius: 22px;
  }

  .cropper-header {
    padding: 18px 18px 16px;
  }

  .cropper-header h2 {
    font-size: 19px;
  }

  .cropper-body {
    display: block;
    min-height: 0;
    overflow: auto;
  }

  .cropper-main {
    min-height: 330px;
    height: 46dvh;
  }

  .cropper-sidebar {
    display: grid;
    grid-template-columns: 108px minmax(0, 1fr);
    gap: 14px;
    padding: 16px 18px;
    border-top: 1px solid var(--jg-line);
    border-left: 0;
  }

  .preview-heading {
    grid-column: 1 / -1;
  }

  .preview-shell {
    align-self: start;
  }

  .preview-note {
    grid-column: 2;
  }

  .cropper-footer {
    padding: 14px 18px max(14px, env(safe-area-inset-bottom));
  }

  .cropper-footer > span {
    display: none;
  }

  .footer-actions {
    width: 100%;
  }

  .btn-cancel,
  .btn-confirm {
    flex: 1;
  }
}
</style>
