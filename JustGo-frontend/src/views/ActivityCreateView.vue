<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createActivity, addActivityImages } from '@/api/activity'
import { getErrorMessage } from '@/api/error'
import type { CreateActivityRequest } from '@/types/api'
import ActivityForm from '@/components/ActivityForm.vue'
import ActivityImageUploader from '@/components/ActivityImageUploader.vue'

const router = useRouter()
const submitting = ref(false)
const pendingImageKeys = ref<string[]>([])
const uploadingImages = ref(false)

async function handleSubmit(data: CreateActivityRequest) {
  submitting.value = true
  try {
    const { data: res } = await createActivity(data)
    if (res.code !== 0) {
      ElMessage.error(res.message)
      return
    }
    const activityId = res.data.id
    if (pendingImageKeys.value.length > 0) {
      uploadingImages.value = true
      try {
        const requests = pendingImageKeys.value.map((k) => ({ objectKey: k }))
        const { data: imgRes } = await addActivityImages(activityId, requests)
        if (imgRes.code !== 0) ElMessage.warning('图片上传失败：' + imgRes.message)
      } catch {
        ElMessage.warning('图片上传失败')
      } finally {
        uploadingImages.value = false
      }
    }
    ElMessage.success('发布成功')
    router.push(`/activities/${activityId}`)
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '发布失败'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="cv">
    <div class="cv-hero">
      <a class="cv-back" @click="router.back()">返回</a>
      <div class="cv-hero-text">
        <h1 class="cv-title">发布活动</h1>
        <p class="cv-sub">找到同频的人，一起去探索城市</p>
      </div>
    </div>

    <!-- 活动图片 -->
    <div class="cv-photos-card">
      <div class="cv-photos-label">活动图片 <em>选填</em></div>
      <p class="cv-photos-desc">添加现场照片让大家更直观地了解活动</p>
      <ActivityImageUploader v-model="pendingImageKeys" :max-count="9" :aspect-ratio="4 / 3" />
    </div>

    <ActivityForm
      submit-label="发布"
      :loading="submitting || uploadingImages"
      @submit="handleSubmit"
    />
  </div>
</template>

<style scoped>
.cv {
  max-width: 920px;
  margin: 0 auto;
  padding: 0 24px 72px;
}

.cv-hero {
  display: flex;
  align-items: flex-start;
  gap: 22px;
  padding: 42px 0 28px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--jg-line);
  animation: create-in 420ms var(--jg-ease) both;
}
.cv-back {
  font-size: 13px;
  color: var(--jg-muted);
  cursor: pointer;
  text-decoration: none;
  padding-top: 10px;
  flex-shrink: 0;
  font-weight: 800;
}
.cv-back:hover {
  color: var(--jg-accent-deep);
}
.cv-title {
  font-size: clamp(34px, 5vw, 54px);
  font-weight: 900;
  color: var(--jg-ink);
  letter-spacing: 0;
  line-height: 1;
  margin: 0 0 8px;
  text-wrap: balance;
}
.cv-sub {
  font-size: 15px;
  color: var(--jg-muted);
  margin: 0;
  text-wrap: pretty;
}

/* Photos card */
.cv-photos-card {
  max-width: 820px;
  margin: 0 auto 18px;
  background: rgba(252, 251, 247, 0.82);
  border-radius: var(--jg-radius-lg);
  padding: 28px;
  border: 1px solid var(--jg-line);
  box-shadow: var(--jg-shadow-card);
}
.cv-photos-label {
  font-size: 14px;
  font-weight: 900;
  color: var(--jg-ink);
  margin-bottom: 8px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--jg-line);
  display: flex;
  align-items: center;
  gap: 6px;
}
.cv-photos-label em {
  font-weight: 700;
  font-size: 12px;
  color: var(--jg-muted);
  font-style: normal;
  margin-left: auto;
}
.cv-photos-desc {
  font-size: 13px;
  color: var(--jg-muted);
  margin: 0 0 14px;
}
@keyframes create-in {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
}
@media (max-width: 680px) {
  .cv {
    padding: 0 16px 60px;
  }
  .cv-hero {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
