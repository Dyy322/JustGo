<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActivityDetail, updateActivity } from '@/api/activity'
import { getErrorMessage } from '@/api/error'
import type { ActivityDetailResponse, UpdateActivityRequest } from '@/types/api'
import ActivityForm from '@/components/ActivityForm.vue'
import ActivityImageManager from '@/components/ActivityImageManager.vue'

const route = useRoute()
const router = useRouter()

const activity = ref<ActivityDetailResponse | null>(null)
const loading = ref(true)
const error = ref('')
const submitting = ref(false)
const activityId = Number(route.params.id)

async function loadActivity() {
  loading.value = true
  try {
    const { data } = await getActivityDetail(activityId)
    if (data.code === 0) activity.value = data.data
    else error.value = data.message
  } catch (err) {
    error.value = getErrorMessage(err, '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleSubmit(data: UpdateActivityRequest) {
  submitting.value = true
  try {
    const { data: res } = await updateActivity(activityId, data)
    if (res.code === 0) {
      ElMessage.success('修改已保存')
      router.push(`/activities/${activityId}`)
    } else ElMessage.error(res.message)
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '更新失败'))
  } finally {
    submitting.value = false
  }
}

onMounted(loadActivity)
</script>

<template>
  <div class="ev">
    <div class="ev-hero">
      <a class="ev-back" @click="router.back()">返回</a>
      <h1 class="ev-title">编辑活动</h1>
    </div>

    <div v-if="loading" class="ev-state">加载中…</div>
    <div v-else-if="error" class="ev-state">
      <p>{{ error }}</p>
      <el-button type="primary" @click="loadActivity">重试</el-button>
    </div>
    <template v-else>
      <div class="ev-images-card">
        <div class="ev-images-label">活动图片</div>
        <ActivityImageManager :activity-id="activityId" />
      </div>

      <ActivityForm
        :initial="activity"
        submit-label="保存修改"
        :loading="submitting"
        @submit="handleSubmit"
      />
    </template>
  </div>
</template>

<style scoped>
.ev {
  max-width: 920px;
  margin: 0 auto;
  padding: 0 24px 72px;
}

.ev-hero {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 42px 0 28px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--jg-line);
  animation: edit-in 420ms var(--jg-ease) both;
}
.ev-back {
  font-size: 13px;
  color: var(--jg-muted);
  cursor: pointer;
  text-decoration: none;
  padding-top: 10px;
  flex-shrink: 0;
  font-weight: 800;
}
.ev-back:hover {
  color: var(--jg-accent-deep);
}
.ev-title {
  font-size: clamp(34px, 5vw, 54px);
  font-weight: 900;
  color: var(--jg-ink);
  letter-spacing: 0;
  line-height: 1;
  margin: 0;
}

.ev-state {
  text-align: center;
  padding: 80px 20px;
  color: var(--jg-muted);
}
.ev-state .el-button {
  margin-top: 12px;
}

.ev-images-card {
  max-width: 820px;
  margin: 0 auto 18px;
  background: rgba(252, 251, 247, 0.82);
  border-radius: var(--jg-radius-lg);
  padding: 28px;
  border: 1px solid var(--jg-line);
  box-shadow: var(--jg-shadow-card);
}
.ev-images-label {
  font-size: 14px;
  font-weight: 900;
  color: var(--jg-ink);
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--jg-line);
}
@keyframes edit-in {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
}
@media (max-width: 680px) {
  .ev {
    padding: 0 16px 60px;
  }
  .ev-hero {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
