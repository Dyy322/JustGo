<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { House, Setting, ChatDotSquare, Calendar, Search, DArrowLeft, DArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const collapsed = ref(false)
const collapsedWidth = '68px'
const expandedWidth = '180px'

const sidebarWidth = ref(expandedWidth)

function toggleCollapse() {
  collapsed.value = !collapsed.value
  sidebarWidth.value = collapsed.value ? collapsedWidth : expandedWidth
}

interface NavItem {
  path: string
  label: string
  icon: typeof House
}

const navItems: NavItem[] = [
  { path: '/', label: '发现活动', icon: House },
  { path: '/', label: '找搭子', icon: Search },
  { path: '/', label: '我的行程', icon: Calendar },
  { path: '/', label: '消息', icon: ChatDotSquare },
]

function goProfile() {
  // trigger profile view — for now just navigate
  // The actual profile link will be built with current user id
}

function isActive(item: NavItem): boolean {
  if (item.path === '/') return route.path === '/'
  return route.path.startsWith(item.path)
}
</script>

<template>
  <aside
    class="app-sidebar"
    :style="{ width: sidebarWidth }"
  >
    <div class="sidebar-nav">
      <router-link
        v-for="item in navItems"
        :key="item.label"
        :to="item.path"
        class="nav-item"
        :class="{ active: isActive(item) }"
      >
        <el-icon :size="20"><component :is="item.icon" /></el-icon>
        <span v-if="!collapsed" class="nav-label">{{ item.label }}</span>
      </router-link>
    </div>

    <div class="sidebar-bottom">
      <router-link to="/settings" class="nav-item" :class="{ active: route.path === '/settings' }">
        <el-icon :size="20"><Setting /></el-icon>
        <span v-if="!collapsed" class="nav-label">设置</span>
      </router-link>

      <button class="collapse-btn" @click="toggleCollapse">
        <el-icon :size="16">
          <DArrowRight v-if="collapsed" />
          <DArrowLeft v-else />
        </el-icon>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.app-sidebar {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #fff;
  border-right: 1px solid #f0f0f0;
  transition: width 0.2s ease;
  overflow: hidden;
  flex-shrink: 0;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: #666;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.15s;
  white-space: nowrap;
}

.nav-item:hover {
  background: #f5f5f5;
  color: #333;
}

.nav-item.active {
  background: #fff3ed;
  color: #ff6b35;
  font-weight: 600;
}

.nav-label {
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-bottom {
  padding: 8px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  border: none;
  background: none;
  color: #999;
  cursor: pointer;
  border-radius: 8px;
  font-size: 14px;
}

.collapse-btn:hover {
  background: #f5f5f5;
  color: #666;
}
</style>
