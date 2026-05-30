<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ActivityCard from '@/components/ActivityCard.vue'
import { listActivities } from '@/api/activity'
import { listCategories } from '@/api/category'
import { getErrorMessage } from '@/api/error'
import type { ActivityListItemResponse, CategoryResponse } from '@/types/api'

const categories = ref<CategoryResponse[]>([])
const activeCategory = ref(0)
const activities = ref<ActivityListItemResponse[]>([])
const loading = ref(true)
const error = ref('')

const hotTopics = ['#周末去哪儿', '#骑行', '#看展搭子', '#飞盘']

async function loadCategories() {
  try {
    const { data } = await listCategories()
    if (data.code === 0) categories.value = data.data
  } catch {
    // 分类加载失败不影响主流程
  }
}

async function loadActivities() {
  loading.value = true
  error.value = ''
  try {
    const params: Record<string, number> = { page: 1, size: 20 }
    if (activeCategory.value > 0) params.categoryId = activeCategory.value
    const { data } = await listActivities(params)
    if (data.code === 0) {
      activities.value = data.data.items
    } else {
      error.value = data.message
    }
  } catch (err) {
    error.value = getErrorMessage(err, '加载失败')
  } finally {
    loading.value = false
  }
}

function selectCategory(id: number) {
  activeCategory.value = id
  loadActivities()
}

onMounted(() => {
  loadCategories()
  loadActivities()
})
</script>

<template>
  <div class="home-page">
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
        <el-button type="primary" size="small" @click="loadActivities">重试</el-button>
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
