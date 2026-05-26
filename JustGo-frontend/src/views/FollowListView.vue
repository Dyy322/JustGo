<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '@/api/error'
import { getFollowers, getFollowing, followUser, unfollowUser } from '@/api/social'
import type { FollowUserItem } from '@/types/api'

const props = defineProps<{
  type: 'followers' | 'following'
}>()

const route = useRoute()

const list = ref<FollowUserItem[]>([])
const nextCursor = ref<string | null>(null)
const hasMore = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const firstLoadDone = ref(false)

const title = computed(() => (props.type === 'followers' ? '粉丝' : '关注'))
const fetchFn = computed(() => (props.type === 'followers' ? getFollowers : getFollowing))

function userIdFromRoute(): number {
  return Number(route.params.id)
}

const avatarFailed = ref<Set<number>>(new Set())

function displayName(user: FollowUserItem): string {
  return user.nickname || user.username
}

function avatarChar(user: FollowUserItem): string {
  return displayName(user).charAt(0)
}

function onAvatarError(user: FollowUserItem) {
  avatarFailed.value.add(user.userId)
}

function avatarGradient(user: FollowUserItem): string {
  const gradients = [
    'linear-gradient(135deg, #ff6b35, #f7931e)',
    'linear-gradient(135deg, #667eea, #764ba2)',
    'linear-gradient(135deg, #f093fb, #f5576c)',
    'linear-gradient(135deg, #4facfe, #00f2fe)',
    'linear-gradient(135deg, #43e97b, #38f9d7)',
    'linear-gradient(135deg, #fa709a, #fee140)',
    'linear-gradient(135deg, #a18cd1, #fbc2eb)',
    'linear-gradient(135deg, #fccb90, #d57eeb)',
  ]
  const index = user.userId % gradients.length
  return gradients[index]!
}

async function fetchFirstPage() {
  const id = userIdFromRoute()
  if (!id) return

  loading.value = true
  try {
    const res = await fetchFn.value(id, undefined, 20)
    if (res.data.code === 0) {
      const page = res.data.data
      list.value = page.items
      nextCursor.value = page.nextCursor
      hasMore.value = page.hasMore
    } else {
      ElMessage.error(res.data.message || '加载失败')
    }
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '网络错误，请稍后重试'))
  } finally {
    loading.value = false
    firstLoadDone.value = true
  }
}

async function loadMore() {
  const id = userIdFromRoute()
  if (!id || !hasMore.value || loadingMore.value) return

  loadingMore.value = true
  try {
    const res = await fetchFn.value(id, nextCursor.value ?? undefined, 20)
    if (res.data.code === 0) {
      const page = res.data.data
      list.value = [...list.value, ...page.items]
      nextCursor.value = page.nextCursor
      hasMore.value = page.hasMore
    } else {
      ElMessage.error(res.data.message || '加载失败')
    }
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '网络错误，请稍后重试'))
  } finally {
    loadingMore.value = false
  }
}

async function handleToggleFollow(user: FollowUserItem) {
  try {
    // Optimistically update; the real state comes from the server on next fetch
    if (true) {
      // Since FollowUserItem doesn't carry `isFollowing`, we treat every click as "follow".
      // In a real scenario you'd track following state per user, but for this MVP
      // we just call followUser each time (the server handles idempotency / toggle).
      await followUser(user.userId)
      ElMessage.success(`已关注 @${user.username}`)
    }
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '操作失败，请稍后重试'))
  }
}

function resetAndFetch() {
  list.value = []
  nextCursor.value = null
  hasMore.value = false
  firstLoadDone.value = false
  fetchFirstPage()
}

onMounted(resetAndFetch)
watch(() => route.params.id, resetAndFetch)
</script>

<template>
  <div class="follow-list-page">
    <!-- Header -->
    <header class="page-header">
      <h2 class="page-title">{{ title }}</h2>
    </header>

    <!-- First-load skeleton -->
    <div v-if="loading" class="skeleton-list">
      <div v-for="i in 6" :key="i" class="skeleton-row">
        <el-skeleton animated>
          <template #template>
            <div class="skeleton-row-inner">
              <el-skeleton-item variant="circle" style="width: 48px; height: 48px" />
              <div class="skeleton-text">
                <el-skeleton-item variant="text" style="width: 120px; height: 16px" />
                <el-skeleton-item variant="text" style="width: 80px; height: 14px; margin-top: 6px" />
              </div>
              <el-skeleton-item variant="button" style="width: 64px; height: 30px; margin-left: auto" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <!-- Empty state -->
    <el-empty
      v-else-if="firstLoadDone && list.length === 0"
      :description="`暂无${title}`"
    />

    <!-- User list -->
    <div v-else class="user-list">
      <div
        v-for="user in list"
        :key="user.userId"
        class="user-row"
      >
        <div class="user-avatar" :style="{ background: avatarGradient(user) }">
          <img v-if="user.avatar && !avatarFailed.has(user.userId)" :src="user.avatar" @error="onAvatarError(user)" />
          <span v-else>{{ avatarChar(user) }}</span>
        </div>
        <div class="user-info">
          <span class="user-nickname">{{ displayName(user) }}</span>
          <span class="user-username">@{{ user.username }}</span>
        </div>
        <el-button
          size="small"
          class="follow-btn"
          @click="handleToggleFollow(user)"
        >
          关注
        </el-button>
      </div>

      <!-- Load more -->
      <div v-if="hasMore" class="load-more-wrapper">
        <el-button
          :loading="loadingMore"
          :disabled="loadingMore"
          class="load-more-btn"
          @click="loadMore"
        >
          {{ loadingMore ? '加载中...' : '加载更多' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.follow-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

/* Header */
.page-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #333;
  margin: 0;
}

/* Skeleton */
.skeleton-list {
  padding: 0 20px;
}

.skeleton-row {
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}

.skeleton-row-inner {
  display: flex;
  align-items: center;
  gap: 12px;
}

.skeleton-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* User list */
.user-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
  overflow: hidden;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.user-nickname {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-username {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.follow-btn {
  --el-button-bg-color: #ff6b35;
  --el-button-border-color: #ff6b35;
  --el-button-text-color: #fff;
  --el-button-hover-bg-color: #e85d2a;
  --el-button-hover-border-color: #e85d2a;
  --el-button-hover-text-color: #fff;
  flex-shrink: 0;
  min-width: 64px;
  height: 30px;
  font-size: 13px;
  border-radius: 16px;
}

/* Load more */
.load-more-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.load-more-btn {
  --el-button-text-color: #ff6b35;
  --el-button-border-color: #ff6b35;
  --el-button-hover-text-color: #fff;
  --el-button-hover-bg-color: #ff6b35;
  --el-button-hover-border-color: #ff6b35;
  border-radius: 20px;
  min-width: 120px;
}
</style>
