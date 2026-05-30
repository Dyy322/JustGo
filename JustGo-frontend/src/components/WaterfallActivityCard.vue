<script setup lang="ts">
import type { ActivityListItemResponse } from '@/types/api'
import { ActivityStatusLabels, ActivityStatusColors } from '@/types/api'
import { computed } from 'vue'
import { User } from '@element-plus/icons-vue'

const props = defineProps<{
  activity: ActivityListItemResponse
}>()

const aspectRatios = ['3 / 4', '4 / 5', '1 / 1', '4 / 3', '3 / 5']
const aspectRatio = computed(() => aspectRatios[props.activity.id % 5])

const creatorName = computed(
  () => props.activity.creator.nickname || `用户${props.activity.creator.id}`,
)

const participantLabel = computed(() => {
  const { currentParticipants, maxParticipants } = props.activity
  if (maxParticipants === 0) return `${currentParticipants}人`
  return `${currentParticipants}/${maxParticipants}`
})

const statusLabel = computed(() => ActivityStatusLabels[props.activity.status] || '')
const statusColor = computed(() => ActivityStatusColors[props.activity.status] || '#7a7d72')
</script>

<template>
  <div
    class="waterfall-card"
    @click="$router.push(`/activities/${activity.id}`)"
  >
    <div class="card-image" :style="{ 'aspect-ratio': aspectRatio }">
      <img
        v-if="activity.coverImage"
        :src="activity.coverImage"
        class="card-image-img"
        loading="lazy"
      />
      <span v-else class="card-image-placeholder">{{ activity.categoryName }}</span>
      <span class="participant-badge">
        <el-icon :size="12"><User /></el-icon>
        {{ participantLabel }}
      </span>
      <span class="status-badge" :style="{ background: statusColor }">{{ statusLabel }}</span>
    </div>
    <div class="card-info">
      <h4 class="card-title">{{ activity.title }}</h4>
      <div class="card-creator">
        <div class="creator-avatar">
          <img
            v-if="activity.creator.avatar"
            :src="activity.creator.avatar"
          />
          <span v-else>{{ creatorName.charAt(0) }}</span>
        </div>
        <span class="creator-name">{{ creatorName }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.waterfall-card {
  break-inside: avoid;
  margin-bottom: 10px;
  background: var(--jg-surface);
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--jg-line);
  cursor: pointer;
  animation: card-fade-up 380ms var(--jg-ease) both;
  animation-delay: calc(var(--i, 0) * 50ms);
}

.waterfall-card:active {
  transform: scale(0.97);
}

.card-image {
  position: relative;
  width: 100%;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(84, 116, 106, 0.78), rgba(23, 24, 21, 0.86)),
    var(--jg-bg-strong);
}

.card-image-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-image-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
  font-weight: 700;
}

.participant-badge {
  position: absolute;
  bottom: 6px;
  left: 6px;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 7px;
  border-radius: 999px;
  background: rgba(23, 24, 21, 0.65);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  line-height: 1.4;
}

.status-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  padding: 2px 7px;
  border-radius: 999px;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  line-height: 1.4;
}

.card-info {
  padding: 8px 10px 10px;
}

.card-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--jg-ink);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
  margin-bottom: 6px;
}

.card-creator {
  display: flex;
  align-items: center;
  gap: 6px;
}

.creator-avatar {
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: var(--jg-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 9px;
  font-weight: 600;
  overflow: hidden;
  flex-shrink: 0;
}

.creator-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 999px;
  object-fit: cover;
}

.creator-name {
  font-size: 11px;
  color: var(--jg-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@keyframes card-fade-up {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
