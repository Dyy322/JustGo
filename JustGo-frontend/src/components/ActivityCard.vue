<script setup lang="ts">
import type { ActivityListItemResponse } from '@/types/api'
import { ActivityStatusLabels, ActivityStatusColors } from '@/types/api'
import { computed } from 'vue'
import { Calendar, Location, Right } from '@element-plus/icons-vue'

const props = defineProps<{
  activity: ActivityListItemResponse
}>()

const statusLabel = computed(() => ActivityStatusLabels[props.activity.status] || '')
const statusColor = computed(() => ActivityStatusColors[props.activity.status] || '#909399')

function formatDate(startTime: string, endTime?: string | null) {
  const start = new Date(startTime)
  const pad = (n: number) => String(n).padStart(2, '0')
  const dateStr = `${start.getMonth() + 1}.${start.getDate()}`
  const timeStr = `${pad(start.getHours())}:${pad(start.getMinutes())}`
  if (endTime) {
    const end = new Date(endTime)
    if (start.toDateString() === end.toDateString()) {
      return `${dateStr} ${timeStr} - ${pad(end.getHours())}:${pad(end.getMinutes())}`
    }
  }
  return `${dateStr} ${timeStr}`
}

function participantText() {
  const { currentParticipants, maxParticipants } = props.activity
  if (maxParticipants === 0) return `${currentParticipants} 人已参加`
  return `${currentParticipants}/${maxParticipants} 人`
}
</script>

<template>
  <div class="activity-card" @click="$router.push(`/activities/${activity.id}`)">
    <div class="card-image">
      <img v-if="activity.coverImage" :src="activity.coverImage" class="card-image-img" />
      <span v-else class="card-image-placeholder">{{ activity.categoryName }}</span>
    </div>
    <div class="card-body">
      <div class="card-header">
        <h4 class="card-title">{{ activity.title }}</h4>
        <span class="card-status" :style="{ color: statusColor }">{{ statusLabel }}</span>
      </div>
      <p class="card-meta">
        <el-icon><Calendar /></el-icon>{{ formatDate(activity.startTime, activity.endTime) }}
      </p>
      <p class="card-meta">
        <el-icon><Location /></el-icon>{{ activity.locationName }}
      </p>
      <div v-if="activity.tags.length" class="card-tags">
        <span v-for="tag in activity.tags.slice(0, 3)" :key="tag" class="tag-chip">{{ tag }}</span>
      </div>
      <div class="card-footer">
        <span class="card-count">{{ participantText() }}</span>
        <span class="card-action"
          >找搭子 <el-icon><Right /></el-icon
        ></span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.activity-card {
  background: rgba(252, 251, 247, 0.82);
  border-radius: var(--jg-radius-md);
  overflow: hidden;
  box-shadow: var(--jg-shadow-card);
  border: 1px solid var(--jg-line);
  display: flex;
  transition:
    transform 220ms var(--jg-ease),
    box-shadow 220ms var(--jg-ease),
    border-color 220ms var(--jg-ease);
  cursor: pointer;
  animation: card-rise 420ms var(--jg-ease) both;
}
.activity-card:hover {
  transform: translateY(-3px);
  border-color: rgba(84, 116, 106, 0.28);
  box-shadow: 0 18px 46px rgba(44, 49, 38, 0.13);
}
.card-image {
  width: 150px;
  min-height: 132px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(135deg, rgba(84, 116, 106, 0.78), rgba(23, 24, 21, 0.86)), var(--jg-bg-strong);
}
.card-image-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.card-image-placeholder {
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
  font-weight: 700;
}
.card-body {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}
.card-title {
  font-size: 15px;
  font-weight: 800;
  color: var(--jg-ink);
  line-height: 1.4;
  text-wrap: balance;
}
.card-status {
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
  flex-shrink: 0;
}
.card-meta {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--jg-muted);
  margin-bottom: 4px;
}
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}
.tag-chip {
  padding: 3px 8px;
  border-radius: 999px;
  background: var(--jg-accent-soft);
  color: var(--jg-accent-deep);
  font-size: 10px;
  font-weight: 700;
}
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 8px;
}
.card-count {
  font-size: 11px;
  color: var(--jg-muted);
}
.card-action {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: var(--jg-accent-deep);
  font-weight: 800;
}

@keyframes card-rise {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
}

@media (max-width: 520px) {
  .activity-card {
    flex-direction: column;
  }

  .card-image {
    width: 100%;
    min-height: 176px;
  }
}
</style>
