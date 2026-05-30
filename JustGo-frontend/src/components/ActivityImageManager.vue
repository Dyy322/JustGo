<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listActivityImages, addActivityImages, deleteActivityImage } from '@/api/activity'
import { getErrorMessage } from '@/api/error'
import ActivityImageUploader from './ActivityImageUploader.vue'
import ImageViewer from './ImageViewer.vue'
import type { ImageInfo } from '@/types/api'

const previewSrc = ref<string | null>(null)

const props = defineProps<{
  activityId: number
  maxCount?: number
}>()

const max = props.maxCount ?? 9
const images = ref<ImageInfo[]>([])
const loading = ref(true)
const error = ref('')
const pendingKeys = ref<string[]>([])
const uploading = ref(false)

async function loadImages() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await listActivityImages(props.activityId)
    if (data.code === 0) {
      images.value = data.data
    } else {
      error.value = data.message
    }
  } catch (err) {
    error.value = getErrorMessage(err, '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleUpload() {
  if (pendingKeys.value.length === 0) return
  uploading.value = true
  try {
    const requests = pendingKeys.value.map((key) => ({ objectKey: key }))
    const { data } = await addActivityImages(props.activityId, requests)
    if (data.code === 0) {
      images.value = data.data
      pendingKeys.value = []
      ElMessage.success('图片已添加')
    } else {
      ElMessage.error(data.message)
    }
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '上传失败'))
  } finally {
    uploading.value = false
  }
}

async function handleDelete(imageId: number) {
  try {
    await ElMessageBox.confirm('确定删除这张图片？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    const { data } = await deleteActivityImage(props.activityId, imageId)
    if (data.code === 0) {
      images.value = images.value.filter((img) => img.id !== imageId)
      ElMessage.success('已删除')
    } else {
      ElMessage.error(data.message)
    }
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '删除失败'))
  }
}

onMounted(loadImages)
</script>

<template>
  <div class="image-manager">
    <!-- Loading -->
    <div v-if="loading" class="im-loading">加载中...</div>

    <!-- Error -->
    <div v-else-if="error" class="im-error">
      <span>{{ error }}</span>
      <el-button size="small" @click="loadImages">重试</el-button>
    </div>

    <!-- Images -->
    <template v-else>
      <div v-if="images.length > 0" class="im-list">
        <div v-for="img in images" :key="img.id" class="im-item">
          <img :src="img.url" class="im-thumb" @click="previewSrc = img.url" />
          <span class="im-remove" @click="handleDelete(img.id)">×</span>
        </div>
      </div>
      <div v-if="images.length === 0 && pendingKeys.length === 0" class="im-empty">
        暂无图片，上传活动照片让更多人了解活动内容
      </div>

      <div v-if="images.length + pendingKeys.length < max" class="im-upload">
        <ActivityImageUploader
          v-model="pendingKeys"
          :max-count="max - images.length"
          :aspect-ratio="4 / 3"
        />
        <el-button
          v-if="pendingKeys.length > 0"
          type="primary"
          size="small"
          :loading="uploading"
          @click="handleUpload"
          class="im-confirm-btn"
        >
          确认添加 {{ pendingKeys.length }} 张图片
        </el-button>
      </div>

      <div v-if="images.length + pendingKeys.length >= max" class="im-limit">
        图片数量已达上限（{{ max }} 张）
      </div>
    </template>

    <ImageViewer :src="previewSrc" @close="previewSrc = null" />
  </div>
</template>

<style scoped>
.image-manager {
  margin-top: 4px;
}
.im-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.im-item {
  width: 140px;
  height: 105px;
  border-radius: 6px;
  overflow: hidden;
  position: relative;
}
.im-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.im-remove {
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
.im-empty {
  color: #999;
  font-size: 13px;
  padding: 12px 0;
}
.im-upload {
  margin-top: 8px;
}
.im-confirm-btn {
  margin-top: 8px;
}
.im-limit {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
.im-loading,
.im-error {
  font-size: 13px;
  color: #999;
  padding: 12px 0;
}
.im-error .el-button {
  margin-left: 8px;
}
</style>
