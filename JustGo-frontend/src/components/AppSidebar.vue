<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  House,
  Setting,
  ChatDotSquare,
  Calendar,
  Search,
  DArrowLeft,
  DArrowRight,
} from '@element-plus/icons-vue'

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

function isActive(item: NavItem): boolean {
  if (item.path === '/') return route.path === '/'
  return route.path.startsWith(item.path)
}
</script>

<template>
  <aside class="app-sidebar" :style="{ width: sidebarWidth }">
    <router-link to="/" class="brand-mark" :class="{ compact: collapsed }">
      <span class="brand-symbol">即</span>
      <span v-if="!collapsed" class="brand-copy">
        <strong>JustGo</strong>
        <small>城市活动中枢</small>
      </span>
    </router-link>

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
  height: 100dvh;
  background: rgba(252, 251, 247, 0.78);
  border-right: 1px solid var(--jg-line);
  transition: width 0.22s var(--jg-ease);
  overflow: hidden;
  flex-shrink: 0;
  backdrop-filter: blur(18px);
}

.brand-mark {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 68px;
  padding: 16px 14px;
  color: var(--jg-ink);
  text-decoration: none;
  border-bottom: 1px solid var(--jg-line);
}

.brand-mark.compact {
  justify-content: center;
}

.brand-symbol {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 14px;
  color: var(--jg-surface);
  background: var(--jg-ink);
  font-weight: 800;
  box-shadow: 0 12px 26px rgba(23, 24, 21, 0.18);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.brand-copy strong {
  font-size: 15px;
  line-height: 1.1;
}

.brand-copy small {
  margin-top: 3px;
  color: var(--jg-muted);
  font-size: 11px;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 12px;
  border-radius: 12px;
  color: var(--jg-muted);
  text-decoration: none;
  font-size: 14px;
  transition:
    transform 180ms var(--jg-ease),
    background-color 180ms var(--jg-ease),
    color 180ms var(--jg-ease);
  white-space: nowrap;
}

.nav-item:hover {
  background: rgba(84, 116, 106, 0.08);
  color: var(--jg-accent-deep);
  transform: translateX(2px);
}

.nav-item.active {
  background: var(--jg-accent-soft);
  color: var(--jg-accent-deep);
  font-weight: 600;
  box-shadow: 0 0 0 1px rgba(84, 116, 106, 0.08) inset;
}

.nav-label {
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-bottom {
  padding: 8px;
  border-top: 1px solid var(--jg-line);
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
  color: var(--jg-muted);
  cursor: pointer;
  border-radius: 12px;
  font-size: 14px;
}

.collapse-btn:hover {
  background: rgba(84, 116, 106, 0.08);
  color: var(--jg-accent-deep);
}

@media (max-width: 900px) {
  .app-sidebar {
    position: fixed;
    left: max(12px, env(safe-area-inset-left));
    right: max(12px, env(safe-area-inset-right));
    bottom: max(12px, env(safe-area-inset-bottom));
    z-index: 40;
    width: auto !important;
    height: 62px;
    border: 1px solid var(--jg-line);
    border-radius: 22px;
    box-shadow: var(--jg-shadow-soft);
  }

  .brand-mark,
  .sidebar-bottom,
  .nav-label {
    display: none;
  }

  .sidebar-nav {
    padding: 8px;
    flex-direction: row;
    justify-content: space-around;
  }

  .nav-item {
    flex: 1;
    justify-content: center;
    padding: 12px 8px;
  }

  .nav-item:hover {
    transform: translateY(-1px);
  }
}
</style>
