<script setup lang="ts">
defineProps<{
  title: string
  date: string
  location: string
  imageGradient: string
  count: string
}>()
</script>

<template>
  <div class="activity-card">
    <div class="card-image" :style="{ background: imageGradient }">
      <span class="card-image-text">活动封面</span>
    </div>
    <div class="card-body">
      <h4 class="card-title">{{ title }}</h4>
      <p class="card-meta">📅 {{ date }}</p>
      <p class="card-meta">📍 {{ location }}</p>
      <div class="card-footer">
        <span class="card-count">{{ count }}</span>
        <span class="card-action">找搭子 →</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.activity-card { background: #fff; border-radius: 10px; overflow: hidden; box-shadow: 0 1px 6px rgba(0,0,0,0.06); display: flex; transition: box-shadow 0.2s; }
.activity-card:hover { box-shadow: 0 2px 16px rgba(0,0,0,0.1); }
.card-image { width: 140px; min-height: 120px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.card-image-text { color: rgba(255,255,255,0.7); font-size: 12px; }
.card-body { padding: 14px; flex: 1; display: flex; flex-direction: column; }
.card-title { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 6px; }
.card-meta { font-size: 12px; color: #999; margin-bottom: 4px; }
.card-footer { display: flex; align-items: center; justify-content: space-between; margin-top: auto; padding-top: 8px; }
.card-count { font-size: 11px; color: #999; }
.card-action { font-size: 11px; color: #ff6b35; font-weight: 600; cursor: pointer; }
</style>
