<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import ActivityCard from '@/components/ActivityCard.vue'
import WaterfallActivityCard from '@/components/WaterfallActivityCard.vue'
import CollapsibleMap from '@/components/CollapsibleMap.vue'
import PullToRefresh from '@/components/PullToRefresh.vue'
import { listActivities } from '@/api/activity'
import { listCategories } from '@/api/category'
import { getErrorMessage } from '@/api/error'
import { useMediaQuery } from '@/composables/useMediaQuery'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useAuthStore } from '@/stores/auth'
import type { ActivityListItemResponse, CategoryResponse } from '@/types/api'

const isMobile = useMediaQuery('(max-width: 900px)')
const auth = useAuthStore()

const categories = ref<CategoryResponse[]>([])
const activeCategory = ref(0)
const activities = ref<ActivityListItemResponse[]>([])
const loading = ref(true)
const error = ref('')
const mapCollapsed = ref(false)
const bootstrapped = ref(false)

const page = ref(1)
const hasMore = ref(true)
const loadingMore = ref(false)
const refreshing = ref(false)
const PAGE_SIZE = 20

const hotTopics = ['#周末去哪儿', '#骑行', '#看展搭子', '#飞盘']

async function loadCategories() {
  try {
    const { data } = await listCategories()
    if (data.code === 0) categories.value = data.data
  } catch {
    // 分类加载失败不影响主流程
  }
}

async function refreshHomeFeed() {
  loading.value = true
  hasMore.value = true
  page.value = 1
  await Promise.all([loadCategories(), loadActivities(true)])
}

async function loadActivities(reset = false) {
  if (reset) {
    page.value = 1
    refreshing.value = true
  } else {
    if (!hasMore.value || loadingMore.value) return
    loadingMore.value = true
    page.value++
  }

  error.value = ''

  try {
    const params: Record<string, number> = { page: page.value, size: PAGE_SIZE }
    if (activeCategory.value > 0) params.categoryId = activeCategory.value
    const { data } = await listActivities(params)
    if (data.code === 0) {
      if (reset) {
        activities.value = data.data.items
      } else {
        activities.value = [...activities.value, ...data.data.items]
      }
      hasMore.value = data.data.items.length === PAGE_SIZE
    } else {
      error.value = data.message
    }
  } catch (err) {
    error.value = getErrorMessage(err, '加载失败')
  } finally {
    loading.value = false
    loadingMore.value = false
    refreshing.value = false
  }
}

function selectCategory(id: number) {
  activeCategory.value = id
  loadActivities(true)
}

async function handleRefresh() {
  await loadActivities(true)
}

async function handleLoadMore() {
  await loadActivities(false)
}

const infiniteEnabled = computed(
  () => hasMore.value && !loadingMore.value && !refreshing.value && !loading.value,
)

const { sentinelRef } = useInfiniteScroll(handleLoadMore, {
  enabled: infiniteEnabled,
})

watch(
  () => [auth.initialized, auth.isLoggedIn] as const,
  async ([initialized, loggedIn]) => {
    if (!initialized || !loggedIn || bootstrapped.value) return
    bootstrapped.value = true
    await refreshHomeFeed()
  },
  { immediate: true },
)
</script>

<template>
  <!-- ===== 移动端布局 ===== -->
  <div v-if="isMobile" class="home-mobile">
    <PullToRefresh :refreshing="refreshing" @refresh="handleRefresh" class="feed-wrapper">
      <CollapsibleMap v-model:collapsed="mapCollapsed" />

      <section class="mobile-sticky-panel">
        <div class="mobile-search-card">
          <div class="mobile-search-input">
            <el-icon><Search /></el-icon>
            <span>搜索活动、地点或你想找的搭子</span>
          </div>
        </div>

        <div class="mobile-topic-row">
          <span v-for="t in hotTopics" :key="t" class="mobile-topic-chip">{{ t }}</span>
        </div>

        <div class="mobile-category-scroll">
          <span
            class="mobile-category-chip"
            :class="{ active: activeCategory === 0 }"
            @click="selectCategory(0)"
          >
            全部
          </span>
          <span
            v-for="cat in categories"
            :key="cat.id"
            class="mobile-category-chip"
            :class="{ active: activeCategory === cat.id }"
            @click="selectCategory(cat.id)"
          >
            {{ cat.name }}
          </span>
        </div>
      </section>

      <!-- Loading skeleton -->
      <div v-if="loading" class="waterfall-grid">
        <div v-for="i in 6" :key="i" class="skeleton-waterfall-card">
          <div class="sk-img" :style="{ aspectRatio: ['3/4','4/5','1/1','4/3','3/5','3/4'][i-1] }" />
          <div class="sk-line w80" />
          <div class="sk-line w40" />
        </div>
      </div>

      <!-- Error -->
      <div v-else-if="error && activities.length === 0" class="feed-empty">
        <p>{{ error }}</p>
        <el-button type="primary" size="small" @click="loadActivities(true)">重试</el-button>
      </div>

      <!-- Waterfall grid -->
      <div v-else-if="activities.length > 0" class="waterfall-grid">
        <WaterfallActivityCard
          v-for="(a, i) in activities"
          :key="a.id"
          :activity="a"
          :style="{ '--i': refreshing ? i : 0 }"
        />

        <div
          v-if="hasMore"
          ref="sentinelRef"
          class="scroll-sentinel"
        >
          <span v-if="loadingMore" class="loading-more">加载中...</span>
        </div>

        <div v-if="!hasMore" class="feed-end">已加载全部活动</div>
      </div>

      <!-- Empty -->
      <div v-else class="feed-empty">
        <p>暂无可浏览的活动，去发布第一个吧</p>
      </div>
    </PullToRefresh>

  </div>

  <!-- ===== 桌面端布局（不变） ===== -->
  <div v-else class="home-page">
    <div class="home-feed">
      <div class="category-tabs">
        <span
          class="category-tab"
          :class="{ active: activeCategory === 0 }"
          @click="selectCategory(0)"
        >
          全部
        </span>
        <span
          v-for="cat in categories"
          :key="cat.id"
          class="category-tab"
          :class="{ active: activeCategory === cat.id }"
          @click="selectCategory(cat.id)"
        >
          {{ cat.name }}
        </span>
      </div>

      <!-- Loading skeleton -->
      <div v-if="loading" class="activity-list">
        <div v-for="i in 4" :key="i" class="skeleton-card">
          <div class="skeleton-img" />
          <div class="skeleton-body">
            <div class="skeleton-line w80" />
            <div class="skeleton-line w60" />
            <div class="skeleton-line w40" />
          </div>
        </div>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="feed-empty">
        <p>{{ error }}</p>
        <el-button type="primary" size="small" @click="loadActivities(true)">重试</el-button>
      </div>

      <!-- Activity list -->
      <div v-else-if="activities.length > 0" class="activity-list">
        <ActivityCard v-for="a in activities" :key="a.id" :activity="a" />
      </div>

      <!-- Empty -->
      <div v-else class="feed-empty">
        <p>暂无可浏览的活动，去发布第一个吧</p>
      </div>
    </div>

    <div class="home-map">
      <div class="map-placeholder">
        <div class="map-grid-mark" />
        <div class="map-title">地图视图</div>
        <div class="map-desc">活动点位将在此展示</div>
        <div class="map-note">（后续集成高德地图组件）</div>
        <div class="map-pin" style="top: 25%; left: 30%" />
        <div class="map-pin" style="top: 45%; left: 55%" />
        <div class="map-pin" style="top: 35%; left: 70%" />
        <div class="map-pin" style="top: 60%; left: 40%" />
        <div class="map-pin" style="top: 20%; left: 65%" />
      </div>
      <div class="map-sidebar">
        <div class="sidebar-section">
          <h4>热门话题</h4>
          <div class="topics">
            <span v-for="t in hotTopics" :key="t" class="topic-tag">{{ t }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== 桌面端 ===== */
.home-page {
  display: grid;
  grid-template-columns: minmax(360px, 440px) minmax(0, 1fr);
  height: 100%;
  min-height: 0;
}

.home-feed {
  padding: 22px;
  overflow-y: auto;
  border-right: 1px solid var(--jg-line);
  animation: feed-in 420ms var(--jg-ease) both;
}

.category-tabs {
  display: flex;
  gap: 9px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.category-tab {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(252, 251, 247, 0.72);
  color: var(--jg-muted);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 0 0 1px var(--jg-line) inset;
  transition:
    transform 180ms var(--jg-ease),
    background-color 180ms var(--jg-ease),
    color 180ms var(--jg-ease);
}

.category-tab:hover {
  background: var(--jg-accent-soft);
  color: var(--jg-accent-deep);
  transform: translateY(-1px);
}

.category-tab.active {
  background: var(--jg-ink);
  color: var(--jg-surface);
  font-weight: 600;
}

.skeleton-card {
  display: flex;
  background: rgba(252, 251, 247, 0.78);
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
  border-radius: 4px;
}

.skeleton-line.w80 {
  width: 80%;
}

.skeleton-line.w60 {
  width: 60%;
}

.skeleton-line.w40 {
  width: 40%;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feed-empty {
  text-align: center;
  padding: 64px 20px;
  color: var(--jg-muted);
  font-size: 14px;
  background: rgba(252, 251, 247, 0.62);
  border: 1px dashed var(--jg-line-strong);
  border-radius: var(--jg-radius-lg);
}

.feed-empty .el-button {
  margin-top: 12px;
}

.home-map {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
}

.map-placeholder {
  position: relative;
  min-width: 0;
  overflow: hidden;
  background:
    linear-gradient(rgba(84, 116, 106, 0.12) 1px, transparent 1px),
    linear-gradient(90deg, rgba(84, 116, 106, 0.12) 1px, transparent 1px),
    radial-gradient(circle at 32% 26%, rgba(84, 116, 106, 0.22), transparent 24%),
    radial-gradient(circle at 70% 65%, rgba(23, 24, 21, 0.12), transparent 28%), #e9ece4;
  background-size:
    64px 64px,
    64px 64px,
    auto,
    auto,
    auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--jg-muted);
}

.map-grid-mark {
  width: 86px;
  height: 86px;
  margin-bottom: 18px;
  border-radius: 28px;
  background:
    linear-gradient(90deg, transparent 45%, rgba(255, 255, 255, 0.8) 45% 55%, transparent 55%),
    linear-gradient(transparent 45%, rgba(255, 255, 255, 0.8) 45% 55%, transparent 55%),
    var(--jg-accent);
  box-shadow: 0 22px 48px rgba(84, 116, 106, 0.22);
  transform: rotate(-8deg);
  animation: float-card 5s var(--jg-ease) infinite;
}

.map-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--jg-ink);
  margin-bottom: 4px;
}

.map-desc {
  font-size: 14px;
}

.map-note {
  font-size: 11px;
  margin-top: 8px;
  color: rgba(70, 73, 64, 0.55);
}

.map-pin {
  position: absolute;
  width: 18px;
  height: 18px;
  border-radius: 999px 999px 999px 3px;
  background: var(--jg-accent-deep);
  box-shadow: 0 10px 26px rgba(47, 79, 71, 0.24);
  transform: rotate(-45deg);
  animation: pin-rise 620ms var(--jg-ease) both;
}

.map-pin::after {
  content: '';
  position: absolute;
  inset: 5px;
  border-radius: 999px;
  background: var(--jg-surface);
}

.map-sidebar {
  background: rgba(252, 251, 247, 0.78);
  padding: 22px 16px;
  border-left: 1px solid var(--jg-line);
  overflow-y: auto;
  backdrop-filter: blur(16px);
}

.sidebar-section {
  margin-bottom: 24px;
}

.sidebar-section h4 {
  font-size: 13px;
  font-weight: 800;
  color: var(--jg-ink);
  margin-bottom: 12px;
}

.topics {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.topic-tag {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(84, 116, 106, 0.1);
  color: var(--jg-accent-deep);
  font-size: 12px;
  font-weight: 600;
}

/* ===== 移动端 ===== */
.home-mobile {
  min-height: 100%;
}

.feed-wrapper {
  min-height: 100%;
  padding-bottom: 14px;
}

.mobile-sticky-panel {
  position: sticky;
  top: 0;
  z-index: 12;
  padding: 10px 12px 12px;
  background:
    linear-gradient(180deg, rgba(243, 242, 239, 0.96), rgba(243, 242, 239, 0.92) 82%, transparent);
  backdrop-filter: blur(12px);
}

.mobile-search-card {
  margin-bottom: 10px;
}

.mobile-search-input {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 16px;
  border-radius: 18px;
  background: rgba(252, 251, 247, 0.95);
  box-shadow:
    0 10px 30px rgba(44, 49, 38, 0.08),
    0 0 0 1px rgba(28, 30, 24, 0.06) inset;
  color: var(--jg-muted);
  font-size: 14px;
}

.mobile-search-input span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mobile-topic-row {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 8px;
  margin-bottom: 4px;
  scrollbar-width: none;
}

.mobile-topic-row::-webkit-scrollbar {
  display: none;
}

.mobile-topic-chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(252, 251, 247, 0.72);
  color: var(--jg-accent-deep);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  box-shadow: 0 0 0 1px rgba(28, 30, 24, 0.06) inset;
}

.mobile-category-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
}

.mobile-category-scroll::-webkit-scrollbar {
  display: none;
}

.mobile-category-chip {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(252, 251, 247, 0.86);
  color: var(--jg-muted);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  box-shadow: 0 0 0 1px var(--jg-line) inset;
  flex-shrink: 0;
}

.mobile-category-chip.active {
  background: var(--jg-ink);
  color: var(--jg-surface);
  box-shadow: none;
}

.waterfall-grid {
  column-count: 2;
  column-gap: 8px;
  padding: 2px 8px 8px;
}

.skeleton-waterfall-card {
  break-inside: avoid;
  margin-bottom: 10px;
  background: rgba(252, 251, 247, 0.78);
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--jg-line);
}

.sk-img {
  background: linear-gradient(90deg, #edeae2, #f8f6f0, #edeae2);
  background-size: 220% 100%;
  animation: shimmer 1.2s ease-out infinite;
}

.sk-line {
  height: 12px;
  margin: 8px 10px;
  background: linear-gradient(90deg, #edeae2, #f8f6f0, #edeae2);
  background-size: 220% 100%;
  animation: shimmer 1.2s ease-out infinite;
  border-radius: 4px;
}

.sk-line.w80 {
  width: 80%;
}

.sk-line.w40 {
  width: 40%;
}

.scroll-sentinel {
  column-span: all;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-more {
  font-size: 12px;
  color: var(--jg-muted);
}

.feed-end {
  column-span: all;
  text-align: center;
  padding: 20px 0 40px;
  font-size: 12px;
  color: var(--jg-muted);
}

/* ===== Keyframes ===== */
@keyframes shimmer {
  to {
    background-position: -220% 0;
  }
}

@keyframes feed-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
}

@keyframes float-card {
  50% {
    transform: translateY(-8px) rotate(-5deg);
  }
}

@keyframes pin-rise {
  from {
    opacity: 0;
    transform: translateY(18px) rotate(-45deg) scale(0.82);
  }
}

/* ===== 桌面端响应式 ===== */
@media (max-width: 1100px) {
  .home-page {
    grid-template-columns: 1fr;
    height: auto;
  }

  .home-feed {
    border-right: 0;
    overflow: visible;
  }

  .home-map {
    min-height: 520px;
    grid-template-columns: 1fr;
  }

  .map-sidebar {
    border-left: 0;
    border-top: 1px solid var(--jg-line);
  }
}

@media (max-width: 680px) {
  .home-feed {
    padding: 16px;
  }

  .home-map {
    min-height: 440px;
  }
}
</style>
