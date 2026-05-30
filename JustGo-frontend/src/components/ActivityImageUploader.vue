<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getUploadToken, getAccessUrl } from '@/api/file'
import { getErrorMessage } from '@/api/error'
import ImageCropper from './ImageCropper.vue'
import axios from 'axios'

const props = withDefaults(
  defineProps<{
    modelValue: string[]
    maxCount?: number
    aspectRatio?: number
  }>(),
  {
    maxCount: 9,
    aspectRatio: 16 / 9,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const uploading = ref(false)
const previewUrls = ref<Record<string, string>>({})

const cropperSrc = ref('')
const showCropper = ref(false)
const cropperFileExt = ref('jpg')

async function fetchPreviewUrls() {
  for (const key of props.modelValue) {
    if (previewUrls.value[key]) continue
    try {
      const { data } = await getAccessUrl({ objectKey: key })
      if (data.code === 0) {
        previewUrls.value[key] = data.data.presignedUrl
      }
    } catch {
      previewUrls.value[key] = ''
    }
  }
}
fetchPreviewUrls()

function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files?.length) return

  const remaining = props.maxCount - props.modelValue.length
  if (remaining <= 0) {
    ElMessage.warning(`最多上传 ${props.maxCount} 张图片`)
    input.value = ''
    return
  }

  const file = files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    input.value = ''
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 10MB')
    input.value = ''
    return
  }

  cropperFileExt.value = 'jpg' // 裁剪后统一输出 JPEG
  cropperSrc.value = URL.createObjectURL(file)
  showCropper.value = true
  input.value = ''
}

async function handleCropperConfirm(blob: Blob) {
  showCropper.value = false
  URL.revokeObjectURL(cropperSrc.value)
  cropperSrc.value = ''

  uploading.value = true
  try {
    const { data: tokenRes } = await getUploadToken({
      ext: cropperFileExt.value,
      prefix: 'activity',
    })
    if (tokenRes.code !== 0) {
      ElMessage.error(tokenRes.message)
      return
    }

    const { uploadUrl, objectKey } = tokenRes.data
    await axios.put(uploadUrl, blob, {
      headers: { 'Content-Type': blob.type || 'image/jpeg' },
      timeout: 60000,
    })

    previewUrls.value[objectKey] = URL.createObjectURL(blob)
    emit('update:modelValue', [...props.modelValue, objectKey])
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '上传失败'))
  } finally {
    uploading.value = false
  }
}

function handleCropperCancel() {
  showCropper.value = false
  URL.revokeObjectURL(cropperSrc.value)
  cropperSrc.value = ''
}

function removeImage(index: number) {
  const key = props.modelValue[index]
  if (!key) return
  const next = [...props.modelValue]
  next.splice(index, 1)
  if (previewUrls.value[key]?.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrls.value[key])
  }
  delete previewUrls.value[key]
  emit('update:modelValue', next)
}
</script>

<template>
  <div class="image-uploader">
    <div v-for="(key, idx) in modelValue" :key="key" class="image-item">
      <img :src="previewUrls[key] || ''" class="image-thumb" />
      <span class="image-remove" @click="removeImage(idx)">×</span>
    </div>

    <label
      v-if="modelValue.length < maxCount"
      class="upload-trigger"
      :class="{ disabled: uploading }"
    >
      <span v-if="uploading">⏳</span>
      <span v-else>+</span>
      <input
        type="file"
        accept="image/*"
        class="upload-input"
        :disabled="uploading"
        @change="handleFileChange"
      />
    </label>

    <ImageCropper
      v-if="showCropper"
      :src="cropperSrc"
      :aspect-ratio="aspectRatio"
      @confirm="handleCropperConfirm"
      @cancel="handleCropperCancel"
    />
  </div>
</template>

<style scoped>
.image-uploader {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.image-item {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}
.image-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.image-remove {
  position: absolute;
  top: 2px;
  right: 4px;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.6);
  line-height: 1;
}
.upload-trigger {
  width: 100px;
  height: 100px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #c0c4cc;
  cursor: pointer;
  transition: border-color 0.2s;
}
.upload-trigger:hover {
  border-color: var(--jg-accent);
  color: var(--jg-accent-deep);
}
.upload-trigger.disabled {
  pointer-events: none;
  opacity: 0.6;
}
.upload-input {
  display: none;
}
</style>
