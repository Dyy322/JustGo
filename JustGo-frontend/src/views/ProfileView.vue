<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getUserPublic } from '@/api/user'
import { followUser, unfollowUser, getFollowStats, getFollowRelation } from '@/api/social'
import { listMyActivities } from '@/api/activity'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '@/api/error'
import ActivityCard from '@/components/ActivityCard.vue'
import WaterfallActivityCard from '@/components/WaterfallActivityCard.vue'
import ImageViewer from '@/components/ImageViewer.vue'
import { useMediaQuery } from '@/composables/useMediaQuery'
import type {
  UserPublicProfile,
  FollowStats,
  FollowRelation,
  ActivityListItemResponse,
} from '@/types/api'

const route = useRoute()
const auth = useAuthStore()
const isMobile = useMediaQuery('(max-width: 900px)')

const profile = ref<UserPublicProfile | null>(null)
const stats = ref<FollowStats>({ followingCount: 0, followerCount: 0 })
const relation = ref<FollowRelation>({ following: false, followsYou: false })
const loading = ref(false)
const followLoading = ref(false)

const activeTab = ref<'created' | 'joined'>('created')
const activities = ref<ActivityListItemResponse[]>([])
const activityLoading = ref(false)
const activityError = ref('')

const isOwnProfile = computed(() => auth.currentUser?.id === profile.value?.id)
const displayName = computed(() =>
  profile.value ? profile.value.nickname || profile.value.username : '',
)
const avatarChar = computed(() => (displayName.value ? displayName.value.charAt(0) : '?'))
const avatarFailed = ref(false)
const previewSrc = ref<string | null>(null)

const relationLabel = computed(() => {
  if (relation.value.following && relation.value.followsYou) return '互相关注'
  if (relation.value.followsYou) return '关注了你'
  return ''
})

const followButtonLabel = computed(() => {
  if (relation.value.following && relation.value.followsYou) return '互相关注'
  if (relation.value.following) return '已关注'
  return '关注'
})

async function fetchProfile() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const [profileRes, statsRes, relationRes] = await Promise.all([
      getUserPublic(id),
      getFollowStats(id),
      isOwnProfile.value ? Promise.resolve(null) : getFollowRelation(id),
    ])
    if (profileRes.data.code === 0) profile.value = profileRes.data.data
    if (statsRes.data.code === 0) stats.value = statsRes.data.data
    if (relationRes && relationRes.data.code === 0) relation.value = relationRes.data.data
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '加载用户信息失败'))
  } finally {
    loading.value = false
  }
}

async function handleFollow() {
  if (!profile.value) return
  followLoading.value = true
  try {
    if (relation.value.following) {
      await unfollowUser(profile.value.id)
      relation.value.following = false
      stats.value.followerCount--
      ElMessage.success('已取消关注')
    } else {
      await followUser(profile.value.id)
      relation.value.following = true
      stats.value.followerCount++
      ElMessage.success('关注成功')
    }
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '操作失败'))
  } finally {
    followLoading.value = false
  }
}

async function loadActivities() {
  activityLoading.value = true
  activityError.value = ''
  try {
    const { data } = await listMyActivities(activeTab.value)
    if (data.code === 0) {
      activities.value = data.data.items
    } else {
      activityError.value = data.message
    }
  } catch (err) {
    activityError.value = getErrorMessage(err, '加载失败')
  } finally {
    activityLoading.value = false
  }
}

function switchTab(tab: 'created' | 'joined') {
  activeTab.value = tab
  loadActivities()
}

onMounted(() => {
  fetchProfile()
  loadActivities()
})
watch(
  () => route.params.id,
  () => {
    fetchProfile()
    loadActivities()
  },
)
</script>

<template>
  <div class="profile-page" v-loading="loading">
    <div class="profile-banner">
      <div class="avatar-section">
        <div class="avatar-large">
          <img
            v-if="profile?.avatar && !avatarFailed"
            :src="profile.avatar"
            @error="avatarFailed = true"
            @click.stop="previewSrc = profile?.avatar ?? null"
            style="cursor: pointer"
          /><span v-else>{{ avatarChar }}</span>
        </div>
      </div>
    </div>
    <div class="profile-info" v-if="profile">
      <div class="info-header">
        <div class="info-main">
          <h2 class="profile-name">
            {{ displayName
            }}<span v-if="relationLabel" class="relation-tag">{{ relationLabel }}</span>
          </h2>
          <p class="profile-username">@{{ profile.username }}</p>
        </div>
        <div class="info-actions">
          <template v-if="isOwnProfile">
            <el-button @click="$router.push('/settings')">编辑资料</el-button>
            <el-button type="primary">分享主页</el-button>
          </template>
          <template v-else>
            <el-button
              :type="relation.following ? 'default' : 'primary'"
              :loading="followLoading"
              @click="handleFollow"
              >{{ followButtonLabel }}</el-button
            >
            <el-button>私信</el-button>
          </template>
        </div>
      </div>
      <div class="stats-row">
        <div class="stat-item" @click="$router.push(`/profile/${route.params.id}/following`)">
          <span class="stat-num">{{ stats.followingCount }}</span
          ><span class="stat-label">关注</span>
        </div>
        <div class="stat-item" @click="$router.push(`/profile/${route.params.id}/followers`)">
          <span class="stat-num">{{ stats.followerCount }}</span
          ><span class="stat-label">粉丝</span>
        </div>
      </div>
    </div>
    <div class="profile-tabs">
      <span class="tab" :class="{ active: activeTab === 'created' }" @click="switchTab('created')"
        >发起的活动</span
      >
      <span class="tab" :class="{ active: activeTab === 'joined' }" @click="switchTab('joined')"
        >参与的活动</span
      >
    </div>
    <div class="profile-content">
      <!-- Loading -->
      <div v-if="activityLoading" :class="isMobile ? 'activity-waterfall' : 'activity-list'">
        <template v-if="isMobile">
          <div v-for="i in 6" :key="i" class="skeleton-waterfall-card">
            <div class="sk-img" :style="{ aspectRatio: ['3/4', '4/5', '1/1', '4/3', '3/5', '3/4'][i - 1] }" />
            <div class="sk-line w80" />
            <div class="sk-line w40" />
          </div>
        </template>
        <template v-else>
          <div v-for="i in 3" :key="i" class="skeleton-card">
            <div class="skeleton-img" />
            <div class="skeleton-body">
              <div class="skeleton-line w80" />
              <div class="skeleton-line w60" />
              <div class="skeleton-line w40" />
            </div>
          </div>
        </template>
      </div>
      <!-- Error -->
      <div v-else-if="activityError" class="activity-empty">
        <p>{{ activityError }}</p>
        <el-button size="small" @click="loadActivities">重试</el-button>
      </div>
      <!-- Activities -->
      <div
        v-else-if="activities.length > 0"
        :class="isMobile ? 'activity-waterfall' : 'activity-list'"
      >
        <WaterfallActivityCard
          v-if="isMobile"
          v-for="a in activities"
          :key="a.id"
          :activity="a"
        />
        <ActivityCard v-else v-for="a in activities" :key="a.id" :activity="a" />
      </div>
      <!-- Empty -->
      <el-empty
        v-else
        :description="activeTab === 'created' ? '暂无发起的活动' : '暂无参与的活动'"
      />
    </div>

    <ImageViewer :src="previewSrc" @close="previewSrc = null" />
  </div>
</template>

<style scoped>
.profile-page {
  height: 100%;
  overflow-y: auto;
  padding: 24px;
  animation: profile-in 460ms var(--jg-ease) both;
}
.profile-banner {
  height: 210px;
  background:
    radial-gradient(circle at 18% 26%, rgba(220, 230, 223, 0.28), transparent 28%),
    linear-gradient(135deg, #171815, #334d45);
  position: relative;
  border-radius: 28px 28px 0 0;
  overflow: hidden;
}
.profile-banner::after {
  content: '';
  position: absolute;
  inset: 20px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
}
.avatar-section {
  position: absolute;
  bottom: -46px;
  left: 44px;
  z-index: 2;
}
.avatar-large {
  width: 112px;
  height: 112px;
  border-radius: 28px;
  background: var(--jg-surface);
  border: 6px solid var(--jg-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 38px;
  color: var(--jg-accent-deep);
  font-weight: 900;
  overflow: hidden;
  box-shadow: var(--jg-shadow-card);
}
.avatar-large img {
  width: 100%;
  height: 100%;
  border-radius: 22px;
  object-fit: cover;
}
.profile-info {
  background: rgba(252, 251, 247, 0.86);
  padding: 28px 34px 0 176px;
  border: 1px solid var(--jg-line);
  border-top: 0;
  border-radius: 0 0 28px 28px;
  box-shadow: var(--jg-shadow-card);
}
.info-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.info-main {
  flex: 1;
}
.profile-name {
  font-size: 28px;
  font-weight: 900;
  color: var(--jg-ink);
  display: flex;
  align-items: center;
  gap: 10px;
  text-wrap: balance;
}
.relation-tag {
  font-size: 11px;
  color: var(--jg-accent-deep);
  background: var(--jg-accent-soft);
  padding: 4px 9px;
  border-radius: 999px;
  font-weight: 800;
}
.profile-username {
  font-size: 13px;
  color: var(--jg-muted);
  margin-top: 5px;
}
.info-actions {
  display: flex;
  gap: 8px;
}
.stats-row {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding-bottom: 20px;
}
.stat-item {
  min-width: 88px;
  padding: 12px 16px;
  text-align: left;
  cursor: pointer;
  background: rgba(84, 116, 106, 0.08);
  border-radius: 16px;
  transition:
    transform 180ms var(--jg-ease),
    background-color 180ms var(--jg-ease);
}
.stat-item:hover {
  transform: translateY(-2px);
  background: var(--jg-accent-soft);
}
.stat-num {
  font-size: 22px;
  font-weight: 900;
  color: var(--jg-ink);
  display: block;
}
.stat-label {
  font-size: 12px;
  color: var(--jg-muted);
}
.profile-tabs {
  display: inline-flex;
  gap: 4px;
  margin: 22px 0 0;
  padding: 4px;
  background: rgba(23, 24, 21, 0.06);
  border-radius: 999px;
}
.tab {
  padding: 9px 18px;
  font-size: 13px;
  color: var(--jg-muted);
  cursor: pointer;
  border-radius: 999px;
}
.tab.active {
  color: var(--jg-accent-deep);
  background: var(--jg-surface);
  font-weight: 800;
  box-shadow: 0 2px 12px rgba(44, 49, 38, 0.08);
}
.profile-content {
  padding: 22px 0 40px;
  max-width: 780px;
}
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-bottom: 40px;
}
.activity-waterfall {
  column-count: 2;
  column-gap: 8px;
  padding-bottom: 40px;
}
.activity-empty {
  text-align: center;
  padding: 64px 20px;
  color: var(--jg-muted);
  font-size: 14px;
  background: rgba(252, 251, 247, 0.62);
  border: 1px dashed var(--jg-line-strong);
  border-radius: var(--jg-radius-lg);
}
.activity-empty .el-button {
  margin-top: 12px;
}
.activity-skeleton {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.skeleton-waterfall-card {
  break-inside: avoid;
  margin-bottom: 10px;
  background: rgba(252, 251, 247, 0.78);
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--jg-line);
}
.skeleton-card {
  display: flex;
  background: rgba(252, 251, 247, 0.8);
  border-radius: var(--jg-radius-md);
  overflow: hidden;
  box-shadow: var(--jg-shadow-card);
  border: 1px solid var(--jg-line);
}
.skeleton-img {
  width: 140px;
  min-height: 120px;
  flex-shrink: 0;
  background: linear-gradient(90deg, #edeae2, #f8f6f0, #edeae2);
  background-size: 220% 100%;
  animation: shimmer 1.2s ease-out infinite;
}
.sk-img {
  background: linear-gradient(90deg, #edeae2, #f8f6f0, #edeae2);
  background-size: 220% 100%;
  animation: shimmer 1.2s ease-out infinite;
}
.skeleton-body {
  flex: 1;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.skeleton-line {
  height: 14px;
  background: linear-gradient(90deg, #edeae2, #f8f6f0, #edeae2);
  background-size: 220% 100%;
  animation: shimmer 1.2s ease-out infinite;
  border-radius: 8px;
}
.sk-line {
  height: 12px;
  margin: 8px 10px;
  background: linear-gradient(90deg, #edeae2, #f8f6f0, #edeae2);
  background-size: 220% 100%;
  animation: shimmer 1.2s ease-out infinite;
  border-radius: 4px;
}
.skeleton-line.w80 {
  width: 80%;
}
.skeleton-line.w60 {
  width: 60%;
}
.skeleton-line.w40,
.sk-line.w40 {
  width: 40%;
}
.sk-line.w80 {
  width: 80%;
}
@keyframes profile-in {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
}
@keyframes shimmer {
  to {
    background-position: -220% 0;
  }
}
@media (max-width: 760px) {
  .profile-page {
    padding: 16px;
  }
  .profile-banner {
    height: 170px;
  }
  .avatar-section {
    left: 24px;
  }
  .profile-info {
    padding: 72px 22px 0;
  }
  .info-header {
    flex-direction: column;
    gap: 16px;
  }
  .info-actions {
    flex-wrap: wrap;
  }
  .profile-content {
    max-width: none;
  }
  .activity-list {
    gap: 12px;
  }
}
</style>
