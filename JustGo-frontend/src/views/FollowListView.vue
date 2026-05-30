<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '@/api/error'
import { getFollowers, getFollowing, followUser, unfollowUser, getFollowRelation } from '@/api/social'
import { useAuthStore } from '@/stores/auth'
import type { FollowRelation, FollowUserItem } from '@/types/api'

const props = defineProps<{
  type: 'followers' | 'following'
}>()

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const list = ref<FollowUserItem[]>([])
const nextCursor = ref<string | null>(null)
const hasMore = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const firstLoadDone = ref(false)
const relationMap = ref<Record<number, FollowRelation>>({})
const followLoadingMap = ref<Record<number, boolean>>({})

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
  const tones = ['#54746a', '#2f4f47', '#736f5d', '#4f5d62', '#715e55', '#3f4941']
  return tones[user.userId % tones.length]!
}

function goToProfile(user: FollowUserItem) {
  router.push(`/profile/${user.userId}`)
}

function isOwnUser(user: FollowUserItem): boolean {
  return auth.currentUser?.id === user.userId
}

function relationOf(userId: number): FollowRelation {
  return relationMap.value[userId] ?? { following: false, followsYou: false }
}

function followButtonLabel(user: FollowUserItem): string {
  const relation = relationOf(user.userId)
  if (relation.following && relation.followsYou) return '互相关注'
  if (relation.following) return '已关注'
  return '关注'
}

async function syncRelations(users: FollowUserItem[]) {
  const ids = users
    .map((user) => user.userId)
    .filter((userId) => userId !== auth.currentUser?.id)

  if (ids.length === 0) return

  const results = await Promise.allSettled(ids.map((userId) => getFollowRelation(userId)))
  const nextMap = { ...relationMap.value }

  results.forEach((result, index) => {
    const userId = ids[index]!
    if (result.status === 'fulfilled' && result.value.data.code === 0) {
      nextMap[userId] = result.value.data.data
    }
  })

  relationMap.value = nextMap
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
      await syncRelations(page.items)
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
      await syncRelations(page.items)
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
  if (isOwnUser(user)) return

  followLoadingMap.value = { ...followLoadingMap.value, [user.userId]: true }
  const relation = relationOf(user.userId)

  try {
    if (relation.following) {
      await unfollowUser(user.userId)
      relationMap.value = {
        ...relationMap.value,
        [user.userId]: { ...relation, following: false },
      }
      ElMessage.success(`已取消关注 @${user.username}`)
    } else {
      await followUser(user.userId)
      relationMap.value = {
        ...relationMap.value,
        [user.userId]: { ...relation, following: true },
      }
      ElMessage.success(`已关注 @${user.username}`)
    }
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '操作失败，请稍后重试'))
  } finally {
    followLoadingMap.value = { ...followLoadingMap.value, [user.userId]: false }
  }
}

function resetAndFetch() {
  list.value = []
  nextCursor.value = null
  hasMore.value = false
  firstLoadDone.value = false
  relationMap.value = {}
  followLoadingMap.value = {}
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
                <el-skeleton-item
                  variant="text"
                  style="width: 80px; height: 14px; margin-top: 6px"
                />
              </div>
              <el-skeleton-item
                variant="button"
                style="width: 64px; height: 30px; margin-left: auto"
              />
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <!-- Empty state -->
    <el-empty v-else-if="firstLoadDone && list.length === 0" :description="`暂无${title}`" />

    <!-- User list -->
    <div v-else class="user-list">
      <div
        v-for="user in list"
        :key="user.userId"
        class="user-row"
        role="button"
        tabindex="0"
        @click="goToProfile(user)"
        @keydown.enter.prevent="goToProfile(user)"
        @keydown.space.prevent="goToProfile(user)"
      >
        <div class="user-avatar" :style="{ background: avatarGradient(user) }">
          <img
            v-if="user.avatar && !avatarFailed.has(user.userId)"
            :src="user.avatar"
            @error="onAvatarError(user)"
          />
          <span v-else>{{ avatarChar(user) }}</span>
        </div>
        <div class="user-info">
          <span class="user-nickname">{{ displayName(user) }}</span>
          <span class="user-username">@{{ user.username }}</span>
        </div>
        <el-button
          v-if="!isOwnUser(user)"
          size="small"
          class="follow-btn"
          :class="{ secondary: relationOf(user.userId).following }"
          :loading="followLoadingMap[user.userId]"
          @click.stop="handleToggleFollow(user)"
        >
          {{ followButtonLabel(user) }}
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
  padding: 24px;
  background: transparent;
  animation: follow-in 420ms var(--jg-ease) both;
}

/* Header */
.page-header {
  padding: 24px 26px;
  border: 1px solid var(--jg-line);
  border-radius: var(--jg-radius-lg) var(--jg-radius-lg) 0 0;
  flex-shrink: 0;
  background: rgba(252, 251, 247, 0.82);
  box-shadow: var(--jg-shadow-card);
}

.page-title {
  font-size: 28px;
  font-weight: 900;
  color: var(--jg-ink);
  margin: 0;
}

/* Skeleton */
.skeleton-list {
  padding: 0 24px;
  background: rgba(252, 251, 247, 0.72);
  border: 1px solid var(--jg-line);
  border-top: 0;
  border-radius: 0 0 var(--jg-radius-lg) var(--jg-radius-lg);
}

.skeleton-row {
  padding: 14px 0;
  border-bottom: 1px solid var(--jg-line);
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
  padding: 0 24px 24px;
  background: rgba(252, 251, 247, 0.72);
  border: 1px solid var(--jg-line);
  border-top: 0;
  border-radius: 0 0 var(--jg-radius-lg) var(--jg-radius-lg);
}

.user-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--jg-line);
  animation: row-in 360ms var(--jg-ease) both;
  cursor: pointer;
  transition:
    transform 180ms var(--jg-ease),
    background-color 180ms var(--jg-ease);
}

.user-row:hover {
  transform: translateX(2px);
}

.user-row:active {
  transform: scale(0.995);
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 16px;
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
  border-radius: 16px;
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
  font-weight: 800;
  color: var(--jg-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-username {
  font-size: 12px;
  color: var(--jg-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.follow-btn {
  --el-button-bg-color: var(--jg-accent);
  --el-button-border-color: var(--jg-accent);
  --el-button-text-color: #fff;
  --el-button-hover-bg-color: var(--jg-accent-deep);
  --el-button-hover-border-color: var(--jg-accent-deep);
  --el-button-hover-text-color: #fff;
  flex-shrink: 0;
  min-width: 64px;
  height: 30px;
  font-size: 13px;
  border-radius: 999px;
}

.follow-btn.secondary {
  --el-button-bg-color: rgba(252, 251, 247, 0.86);
  --el-button-border-color: rgba(28, 30, 24, 0.12);
  --el-button-text-color: var(--jg-accent-deep);
  --el-button-hover-bg-color: var(--jg-accent-soft);
  --el-button-hover-border-color: rgba(84, 116, 106, 0.22);
  --el-button-hover-text-color: var(--jg-accent-deep);
}

/* Load more */
.load-more-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.load-more-btn {
  --el-button-text-color: var(--jg-accent-deep);
  --el-button-border-color: var(--jg-accent);
  --el-button-hover-text-color: #fff;
  --el-button-hover-bg-color: var(--jg-accent);
  --el-button-hover-border-color: var(--jg-accent);
  border-radius: 999px;
  min-width: 120px;
}

@keyframes follow-in {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
}

@keyframes row-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
}

@media (max-width: 680px) {
  .follow-list-page {
    padding: 16px;
  }

  .page-header,
  .user-list,
  .skeleton-list {
    padding-left: 18px;
    padding-right: 18px;
  }
}
</style>
