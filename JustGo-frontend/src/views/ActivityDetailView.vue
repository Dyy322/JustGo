<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getActivityDetail,
  cancelActivity,
  republishActivity,
  joinActivity,
  leaveActivity,
  isJoinedActivity,
} from '@/api/activity'
import { getErrorMessage } from '@/api/error'
import { useAuthStore } from '@/stores/auth'
import { ActivityStatusLabels, ActivityStatusColors } from '@/types/api'
import ImageViewer from '@/components/ImageViewer.vue'
import type { ActivityDetailResponse } from '@/types/api'
import { Calendar, Location, User, Folder, View } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activity = ref<ActivityDetailResponse | null>(null)
const loading = ref(true)
const error = ref('')
const cancelling = ref(false)
const republishing = ref(false)
const joined = ref(false)
const previewSrc = ref<string | null>(null)
const joining = ref(false)

const activityId = computed(() => Number(route.params.id))

const statusLabel = computed(() =>
  activity.value ? ActivityStatusLabels[activity.value.status] : '',
)
const statusColor = computed(() =>
  activity.value ? ActivityStatusColors[activity.value.status] : '',
)

const canCancel = computed(() =>
  activity.value ? [1, 2, 3].includes(activity.value.status) : false,
)
const canRepublish = computed(() => (activity.value ? activity.value.status === 5 : false))

const isCreator = computed(() =>
  activity.value ? auth.currentUser?.id === activity.value.creator.id : false,
)

function goToCreatorProfile() {
  if (!activity.value) return
  router.push(`/profile/${activity.value.creator.id}`)
}

function formatDate(dt: string) {
  const d = new Date(dt)
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${d.getMonth() + 1}月${d.getDate()}日 ${weekDays[d.getDay()]}`
}

function formatTime(dt: string) {
  const d = new Date(dt)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function participantText() {
  if (!activity.value) return ''
  const { currentParticipants, maxParticipants } = activity.value
  if (maxParticipants === 0) return `${currentParticipants} 人已参加`
  return `${currentParticipants} / ${maxParticipants}`
}

function progressPercent() {
  if (!activity.value) return 0
  const { currentParticipants, maxParticipants } = activity.value
  if (maxParticipants === 0) return 0
  return Math.min((currentParticipants / maxParticipants) * 100, 100)
}

async function loadDetail() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await getActivityDetail(activityId.value)
    if (data.code === 0) {
      activity.value = data.data
    } else {
      error.value = data.message
    }
  } catch (err) {
    error.value = getErrorMessage(err, '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确定要取消这个活动吗？', '取消活动', {
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想',
      type: 'warning',
    })
  } catch {
    return
  }
  cancelling.value = true
  try {
    const { data } = await cancelActivity(activityId.value)
    if (data.code === 0) {
      ElMessage.success('活动已取消')
      loadDetail()
    } else ElMessage.error(data.message)
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '操作失败'))
  } finally {
    cancelling.value = false
  }
}

async function handleRepublish() {
  republishing.value = true
  try {
    const { data } = await republishActivity(activityId.value)
    if (data.code === 0) {
      ElMessage.success('活动已重新发布')
      loadDetail()
    } else ElMessage.error(data.message)
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '操作失败'))
  } finally {
    republishing.value = false
  }
}

async function checkJoinStatus() {
  try {
    const { data } = await isJoinedActivity(activityId.value)
    if (data.code === 0) joined.value = data.data
  } catch {
    /* ignore */
  }
}

async function handleJoin() {
  joining.value = true
  try {
    const { data } = await joinActivity(activityId.value)
    if (data.code === 0) {
      ElMessage.success('报名成功')
      joined.value = true
      loadDetail()
    } else ElMessage.error(data.message)
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '操作失败'))
  } finally {
    joining.value = false
  }
}

async function handleLeave() {
  joining.value = true
  try {
    const { data } = await leaveActivity(activityId.value)
    if (data.code === 0) {
      ElMessage.success('已取消报名')
      joined.value = false
      loadDetail()
    } else ElMessage.error(data.message)
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '操作失败'))
  } finally {
    joining.value = false
  }
}

onMounted(() => {
  loadDetail()
  checkJoinStatus()
})
</script>

<template>
  <div class="detail-page">
    <!-- Loading -->
    <div v-if="loading" class="detail-loading">
      <div class="skeleton-cover" />
      <div class="skeleton-title" />
      <div class="skeleton-line" />
      <div class="skeleton-line short" />
    </div>

    <!-- Error -->
    <div v-else-if="error" class="detail-error">
      <p>{{ error }}</p>
      <el-button type="primary" @click="loadDetail">重试</el-button>
    </div>

    <!-- Detail -->
    <template v-else-if="activity">
      <!-- Cover -->
      <div
        class="cover-section"
        @click="activity.coverImage && (previewSrc = activity.coverImage)"
        :style="{ cursor: activity.coverImage ? 'pointer' : 'default' }"
      >
        <img v-if="activity.coverImage" :src="activity.coverImage" class="cover-img" />
        <div class="cover-gradient" />
        <div class="cover-overlay">
          <span class="status-chip" :style="{ background: statusColor }">{{ statusLabel }}</span>
          <h1 class="cover-title">{{ activity.title }}</h1>
        </div>
      </div>

      <!-- Creator bar -->
      <div
        class="creator-bar"
        role="button"
        tabindex="0"
        @click="goToCreatorProfile"
        @keydown.enter.prevent="goToCreatorProfile"
        @keydown.space.prevent="goToCreatorProfile"
      >
        <div class="creator-avatar">
          <img
            v-if="activity.creator.avatar"
            :src="activity.creator.avatar"
            @click.stop="previewSrc = activity.creator.avatar"
            style="cursor: pointer"
          />
          <span v-else>{{ (activity.creator.nickname || '?').charAt(0) }}</span>
        </div>
        <div class="creator-info">
          <span class="creator-name">{{ activity.creator.nickname || '用户' }}</span>
          <span class="creator-label">发起人</span>
        </div>
        <span class="view-count"
          ><el-icon><View /></el-icon>{{ activity.viewCount }}</span
        >
      </div>

      <!-- Time & Location cards -->
      <div class="info-cards">
        <div class="info-card">
          <div class="card-icon">
            <el-icon><Calendar /></el-icon>
          </div>
          <div class="card-content">
            <div class="card-value">{{ formatDate(activity.startTime) }}</div>
            <div class="card-sub">
              {{ formatTime(activity.startTime) }}
              <template v-if="activity.endTime"> — {{ formatTime(activity.endTime) }}</template>
            </div>
          </div>
        </div>
        <div class="info-card">
          <div class="card-icon">
            <el-icon><Location /></el-icon>
          </div>
          <div class="card-content">
            <div class="card-value">{{ activity.locationName }}</div>
            <div v-if="activity.address" class="card-sub">{{ activity.address }}</div>
          </div>
        </div>
        <div class="info-card">
          <div class="card-icon">
            <el-icon><User /></el-icon>
          </div>
          <div class="card-content">
            <div class="card-value">{{ participantText() }}</div>
            <div v-if="activity.maxParticipants > 0" class="progress-wrap">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: progressPercent() + '%' }" />
              </div>
            </div>
          </div>
        </div>
        <div class="info-card">
          <div class="card-icon">
            <el-icon><Folder /></el-icon>
          </div>
          <div class="card-content">
            <div class="card-value">{{ activity.categoryName }}</div>
            <div v-if="activity.tags.length" class="card-tags">
              <span v-for="tag in activity.tags.slice(0, 4)" :key="tag" class="tag-chip">{{
                tag
              }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Description -->
      <div v-if="activity.description" class="detail-section">
        <h3 class="section-title">活动详情</h3>
        <div class="description-body" v-html="activity.description" />
      </div>

      <!-- Images -->
      <div v-if="activity.images.length" class="detail-section">
        <h3 class="section-title">活动图片</h3>
        <div class="image-grid">
          <img
            v-for="img in activity.images"
            :key="img.id"
            :src="img.url"
            class="detail-image"
            @click="previewSrc = img.url"
          />
        </div>
      </div>
    </template>

    <!-- Empty -->
    <div v-else class="detail-error"><p>活动不存在</p></div>

    <!-- Bottom action bar -->
    <div v-if="activity" class="bottom-bar">
      <template v-if="isCreator">
        <el-button class="bar-btn" @click="router.push(`/activities/${activityId}/edit`)"
          >编辑活动</el-button
        >
        <el-button
          v-if="canCancel"
          class="bar-btn danger"
          :loading="cancelling"
          @click="handleCancel"
          >取消活动</el-button
        >
        <el-button
          v-if="canRepublish"
          class="bar-btn primary"
          :loading="republishing"
          @click="handleRepublish"
          >重新发布</el-button
        >
      </template>
      <template v-else>
        <el-button v-if="joined" class="bar-btn leave" :loading="joining" @click="handleLeave"
          >已报名，取消报名</el-button
        >
        <el-button v-else class="bar-btn join" :loading="joining" @click="handleJoin"
          >我要参加</el-button
        >
      </template>
    </div>

    <ImageViewer :src="previewSrc" @close="previewSrc = null" />
  </div>
</template>

<style scoped>
.detail-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px 24px 0;
  animation: detail-in 480ms var(--jg-ease) both;
}

/* Cover */
.cover-section {
  position: relative;
  width: 100%;
  height: clamp(300px, 42vw, 460px);
  overflow: hidden;
  border-radius: 28px;
  background: var(--jg-ink);
  box-shadow: var(--jg-shadow-soft);
}
.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(23, 24, 21, 0.12), rgba(23, 24, 21, 0.72));
}
.cover-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 34px;
}
.cover-title {
  max-width: 760px;
  font-size: clamp(28px, 4vw, 52px);
  font-weight: 900;
  color: #fff;
  line-height: 1.08;
  margin-top: 12px;
  text-wrap: balance;
  text-shadow: 0 12px 34px rgba(0, 0, 0, 0.28);
}
.status-chip {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
  color: #fff;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.16);
}

/* Creator bar */
.creator-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: -28px 24px 18px;
  position: relative;
  z-index: 2;
  padding: 14px 16px;
  background: rgba(252, 251, 247, 0.9);
  border: 1px solid var(--jg-line);
  border-radius: 18px;
  box-shadow: var(--jg-shadow-card);
  backdrop-filter: blur(16px);
  cursor: pointer;
  transition:
    transform 180ms var(--jg-ease),
    box-shadow 180ms var(--jg-ease),
    border-color 180ms var(--jg-ease);
}
.creator-bar:hover {
  transform: translateY(-1px);
  border-color: rgba(84, 116, 106, 0.24);
  box-shadow: 0 16px 34px rgba(44, 49, 38, 0.1);
}
.creator-bar:active {
  transform: translateY(0) scale(0.995);
}
.creator-avatar {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: var(--jg-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  flex-shrink: 0;
}
.creator-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.creator-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.creator-name {
  font-size: 14px;
  font-weight: 800;
  color: var(--jg-ink);
}
.creator-label {
  font-size: 11px;
  color: var(--jg-muted);
}
.view-count {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--jg-muted);
}

/* Info cards */
.info-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}
.info-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 18px;
  background: rgba(252, 251, 247, 0.8);
  border: 1px solid var(--jg-line);
  border-radius: var(--jg-radius-md);
  box-shadow: 0 10px 30px rgba(44, 49, 38, 0.06);
}
.card-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 12px;
  color: var(--jg-accent-deep);
  background: var(--jg-accent-soft);
  flex-shrink: 0;
  margin-top: 1px;
}
.card-content {
  flex: 1;
  min-width: 0;
}
.card-value {
  font-size: 15px;
  font-weight: 800;
  color: var(--jg-ink);
}
.card-sub {
  font-size: 12px;
  color: var(--jg-muted);
  margin-top: 4px;
}
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
}
.tag-chip {
  padding: 1px 8px;
  border-radius: 10px;
  background: var(--jg-accent-soft);
  color: var(--jg-accent-deep);
  font-size: 11px;
}
.progress-wrap {
  margin-top: 6px;
}
.progress-bar {
  height: 5px;
  background: rgba(84, 116, 106, 0.14);
  border-radius: 999px;
}
.progress-fill {
  height: 100%;
  background: var(--jg-accent);
  border-radius: 999px;
  transition: width 300ms var(--jg-ease);
}

/* Detail section */
.detail-section {
  padding: 26px;
  background: rgba(252, 251, 247, 0.82);
  border: 1px solid var(--jg-line);
  border-radius: var(--jg-radius-lg);
  margin-top: 16px;
  box-shadow: var(--jg-shadow-card);
}
.section-title {
  font-size: 17px;
  font-weight: 900;
  color: var(--jg-ink);
  margin-bottom: 14px;
}
.description-body {
  font-size: 14px;
  line-height: 1.9;
  color: var(--jg-ink-soft);
  text-wrap: pretty;
}
.description-body :deep(h2) {
  font-size: 18px;
  margin: 16px 0 8px;
}
.description-body :deep(h3) {
  font-size: 16px;
  margin: 14px 0 6px;
}
.image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.detail-image {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 12px;
  cursor: pointer;
  transition:
    transform 220ms var(--jg-ease),
    filter 220ms var(--jg-ease);
}
.detail-image:hover {
  transform: translateY(-2px);
  filter: saturate(1.05);
}

/* Loading */
.detail-loading {
  padding: 20px;
}
.skeleton-cover {
  width: 100%;
  height: 280px;
  background: #edeae2;
  border-radius: 24px;
  margin-bottom: 20px;
}
.skeleton-title {
  width: 60%;
  height: 24px;
  background: #edeae2;
  border-radius: 8px;
  margin: 0 20px 12px;
}
.skeleton-line {
  width: 80%;
  height: 14px;
  background: #edeae2;
  border-radius: 8px;
  margin: 0 20px 8px;
}
.skeleton-line.short {
  width: 40%;
}
.detail-error {
  text-align: center;
  padding: 80px 20px;
  color: var(--jg-muted);
}
.detail-error .el-button {
  margin-top: 12px;
}

/* Bottom bar */
.bottom-bar {
  position: sticky;
  bottom: 0;
  padding: 12px 20px;
  padding-bottom: max(12px, env(safe-area-inset-bottom));
  background: rgba(252, 251, 247, 0.86);
  border: 1px solid var(--jg-line);
  border-radius: 18px 18px 0 0;
  display: flex;
  gap: 10px;
  z-index: 10;
  margin-top: 20px;
  backdrop-filter: blur(16px);
}
.bar-btn {
  flex: 1;
  height: 46px;
  font-size: 15px;
  font-weight: 800;
  border-radius: 999px;
}
.bar-btn.join {
  background: var(--jg-ink);
  border-color: var(--jg-ink);
  color: #fff;
}
.bar-btn.join:hover {
  background: var(--jg-accent-deep);
  border-color: var(--jg-accent-deep);
}
.bar-btn.leave {
  border-color: var(--jg-accent);
  color: var(--jg-accent-deep);
}
.bar-btn.leave:hover {
  border-color: var(--jg-accent-deep);
  color: var(--jg-accent-deep);
}
.bar-btn.primary {
  background: var(--jg-accent);
  border-color: var(--jg-accent);
  color: #fff;
}
.bar-btn.danger {
  border-color: var(--jg-danger);
  color: var(--jg-danger);
}
.bar-btn.danger:hover {
  background: rgba(168, 79, 69, 0.08);
}

@keyframes detail-in {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
}

@media (max-width: 720px) {
  .detail-page {
    padding: 16px 16px 0;
  }

  .cover-overlay {
    padding: 24px;
  }

  .creator-bar {
    margin-left: 12px;
    margin-right: 12px;
  }

  .info-cards {
    grid-template-columns: 1fr;
  }

  .image-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
