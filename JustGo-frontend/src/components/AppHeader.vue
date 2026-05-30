<script setup lang="ts">
import { ref, watch } from 'vue'
import { Bell, Location, Plus, Search, MapLocation, Tickets } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

watch(
  () => auth.currentUser?.avatar,
  () => {
    avatarFailed.value = false
  },
)

function goProfile() {
  if (auth.currentUser) {
    router.push(`/profile/${auth.currentUser.id}`)
  }
}

function handleLogout() {
  auth.logoutAction()
}

const displayName = () => {
  const u = auth.currentUser
  if (!u) return ''
  return u.nickname || u.username
}

const avatarChar = () => {
  const name = displayName()
  return name ? name.charAt(0) : '?'
}

const avatarFailed = ref(false)
</script>

<template>
  <header class="app-header">
    <div class="header-left">
      <span class="header-city">
        <el-icon><Location /></el-icon>
        上海
      </span>
    </div>

    <div class="header-center">
      <router-link to="/activities/create" class="create-btn">
        <span>发布活动</span>
        <span class="create-icon"
          ><el-icon><Plus /></el-icon
        ></span>
      </router-link>
      <el-input
        placeholder="搜索活动、话题或搭子..."
        :prefix-icon="Search"
        class="search-input"
        disabled
      />
      <div class="view-toggle">
        <span class="toggle-option"
          ><el-icon><Tickets /></el-icon>列表</span
        >
        <span class="toggle-option active"
          ><el-icon><MapLocation /></el-icon>地图</span
        >
      </div>
    </div>

    <div class="header-right">
      <el-icon :size="20"><Bell /></el-icon>
      <el-dropdown
        trigger="click"
        @command="
          (cmd: string) => {
            if (cmd === 'profile') goProfile()
            if (cmd === 'logout') handleLogout()
          }
        "
      >
        <div class="avatar-circle">
          <img
            v-if="auth.currentUser?.avatar && !avatarFailed"
            :src="auth.currentUser.avatar"
            @error="avatarFailed = true"
          />
          <span v-else>{{ avatarChar() }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人主页</el-dropdown-item>
            <el-dropdown-item command="settings">
              <router-link
                to="/settings"
                style="text-decoration: none; color: inherit; display: block"
                >设置</router-link
              >
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  background: rgba(252, 251, 247, 0.78);
  border-bottom: 1px solid var(--jg-line);
  height: 68px;
  box-sizing: border-box;
  flex-shrink: 0;
  backdrop-filter: blur(18px);
  position: sticky;
  top: 0;
  z-index: 20;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-city {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-weight: 600;
  font-size: 14px;
  color: var(--jg-ink);
  cursor: pointer;
  padding: 9px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.56);
  box-shadow: 0 0 0 1px var(--jg-line) inset;
}

.header-center {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 0 0 auto;
}

.search-input {
  width: min(32vw, 360px);
}

.create-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 7px 8px 7px 16px;
  background: var(--jg-ink);
  color: #fff;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
  box-shadow: 0 12px 28px rgba(23, 24, 21, 0.18);
}
.create-btn:hover {
  background: var(--jg-accent-deep);
  transform: translateY(-1px);
}

.create-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 999px;
  color: var(--jg-ink);
  background: #fff;
}

.view-toggle {
  display: flex;
  gap: 3px;
  background: rgba(23, 24, 21, 0.06);
  border-radius: 999px;
  padding: 3px;
  overflow: hidden;
}

.toggle-option {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 10px;
  font-size: 12px;
  color: var(--jg-muted);
  cursor: pointer;
  border-radius: 999px;
}

.toggle-option.active {
  background: var(--jg-surface);
  color: var(--jg-accent-deep);
  font-weight: 600;
  box-shadow: 0 1px 8px rgba(44, 49, 38, 0.08);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  color: var(--jg-ink-soft);
}

.avatar-circle {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: var(--jg-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  overflow: hidden;
}

.avatar-circle img {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  object-fit: cover;
}

@media (max-width: 900px) {
  .app-header {
    height: auto;
    padding: 12px 16px;
    gap: 12px;
    flex-wrap: wrap;
  }

  .header-center {
    order: 3;
    width: 100%;
    justify-content: space-between;
    overflow-x: auto;
    padding-bottom: 2px;
    scrollbar-width: none;
  }

  .header-center::-webkit-scrollbar {
    display: none;
  }

  .search-input {
    min-width: 210px;
    flex: 1;
  }
}
</style>
