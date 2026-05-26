<script setup lang="ts">
import { ref } from 'vue'
import ActivityCard from '@/components/ActivityCard.vue'

const categories = ['全部', '展览', '运动', '市集', '音乐', '美食', '户外']
const activeCategory = ref('全部')

const activities = [
  { title: '周末城市骑行 · 滨江路线', date: '5.31 周六 9:00', location: '浦东滨江大道', imageGradient: 'linear-gradient(135deg,#ffecd2,#fcb69f)', count: '12 人想去' },
  { title: '安藤忠雄建筑展 · 早鸟票', date: '6.1-8.31', location: '上海当代艺术博物馆', imageGradient: 'linear-gradient(135deg,#a1c4fd,#c2e9fb)', count: '38 人想去' },
  { title: '周末飞盘 · 新手友好局', date: '5.31 周六 15:00', location: '世纪公园', imageGradient: 'linear-gradient(135deg,#d4fc79,#96e6a1)', count: '8/20 人' },
  { title: '深夜食堂 · 居酒屋探店', date: '5.30 周五 19:30', location: '古北', imageGradient: 'linear-gradient(135deg,#fbc2eb,#a6c1ee)', count: '4/6 人' },
]

const hotTopics = ['#周末去哪儿', '#骑行', '#看展搭子', '#飞盘']
const recommendUsers = [
  { name: '骑行达人小李', fans: '128 粉丝', color: '#ffe0d0' },
  { name: '看展小能手', fans: '256 粉丝', color: '#d0e8ff' },
]
</script>

<template>
  <div class="home-page">
    <div class="home-feed">
      <div class="category-tabs">
        <span v-for="cat in categories" :key="cat" class="category-tab" :class="{ active: activeCategory === cat }" @click="activeCategory = cat">{{ cat }}</span>
      </div>
      <div class="activity-list">
        <ActivityCard v-for="a in activities" :key="a.title" v-bind="a" />
      </div>
    </div>
    <div class="home-map">
      <div class="map-placeholder">
        <div class="map-emoji">🗺️</div>
        <div class="map-title">地图视图</div>
        <div class="map-desc">活动点位将在此展示</div>
        <div class="map-note">（后续集成高德/百度/Mapbox 地图组件）</div>
        <div class="map-pin" style="top:25%;left:30%">📍</div>
        <div class="map-pin" style="top:45%;left:55%">📍</div>
        <div class="map-pin" style="top:35%;left:70%">📍</div>
        <div class="map-pin" style="top:60%;left:40%">📍</div>
        <div class="map-pin" style="top:20%;left:65%">📍</div>
      </div>
      <div class="map-sidebar">
        <div class="sidebar-section">
          <h4>🔥 热门话题</h4>
          <div class="topics"><span v-for="t in hotTopics" :key="t" class="topic-tag">{{ t }}</span></div>
        </div>
        <div class="sidebar-section">
          <h4>👤 推荐用户</h4>
          <div class="user-list">
            <div v-for="u in recommendUsers" :key="u.name" class="user-item">
              <div class="user-avatar" :style="{ background: u.color }"></div>
              <div class="user-info"><div class="user-name">{{ u.name }}</div><div class="user-fans">{{ u.fans }}</div></div>
              <span class="follow-link">+关注</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page { display: flex; height: 100%; }
.home-feed { flex: 0 0 440px; padding: 16px; overflow-y: auto; border-right: 1px solid #f0f0f0; }
.category-tabs { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.category-tab { padding: 5px 14px; border-radius: 20px; background: #fff; color: #666; font-size: 12px; cursor: pointer; transition: all 0.15s; }
.category-tab:hover { background: #fff3ed; color: #ff6b35; }
.category-tab.active { background: #ff6b35; color: #fff; font-weight: 600; }
.activity-list { display: flex; flex-direction: column; gap: 12px; }
.home-map { flex: 1; display: flex; }
.map-placeholder { flex: 1; position: relative; background: #e8f4e8; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #999; }
.map-emoji { font-size: 48px; margin-bottom: 8px; }
.map-title { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.map-desc { font-size: 13px; }
.map-note { font-size: 11px; margin-top: 8px; color: #bbb; }
.map-pin { position: absolute; font-size: 20px; }
.map-sidebar { width: 220px; background: #fff; padding: 16px 12px; border-left: 1px solid #f0f0f0; overflow-y: auto; }
.sidebar-section { margin-bottom: 24px; }
.sidebar-section h4 { font-size: 13px; font-weight: 600; color: #333; margin-bottom: 10px; }
.topics { display: flex; flex-wrap: wrap; gap: 6px; }
.topic-tag { padding: 3px 10px; border-radius: 12px; background: #f5f5f5; color: #666; font-size: 11px; }
.user-list { display: flex; flex-direction: column; gap: 10px; }
.user-item { display: flex; align-items: center; gap: 8px; }
.user-avatar { width: 32px; height: 32px; border-radius: 50%; flex-shrink: 0; }
.user-info { flex: 1; }
.user-name { font-size: 12px; font-weight: 600; color: #333; }
.user-fans { font-size: 10px; color: #999; }
.follow-link { font-size: 11px; color: #ff6b35; font-weight: 600; cursor: pointer; }
</style>
