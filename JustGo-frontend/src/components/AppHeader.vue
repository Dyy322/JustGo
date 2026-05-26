<script setup lang="ts">
import { ref, watch } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

watch(
  () => auth.currentUser?.avatar,
  () => { avatarFailed.value = false },
)

function goProfile() {
  if (auth.currentUser) {
    router.push(`/profile/${auth.currentUser.id}`)
  }
}

function handleLogout() {
  auth.logoutAction()
  router.push('/login')
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
        📍 上海 <span style="font-size: 12px; color: #999">▾</span>
      </span>
    </div>

    <div class="header-center">
      <el-input
        placeholder="搜索活动、话题或搭子..."
        prefix-icon="Search"
        class="search-input"
        disabled
      />
      <div class="view-toggle">
        <span class="toggle-option">📋 列表</span>
        <span class="toggle-option active">🗺 地图</span>
      </div>
    </div>

    <div class="header-right">
      <el-icon :size="20"><Bell /></el-icon>
      <el-dropdown trigger="click" @command="(cmd: string) => { if (cmd==='profile') goProfile(); if (cmd==='logout') handleLogout() }">
        <div class="avatar-circle">
          <img v-if="auth.currentUser?.avatar && !avatarFailed" :src="auth.currentUser.avatar" @error="avatarFailed = true" />
          <span v-else>{{ avatarChar() }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人主页</el-dropdown-item>
            <el-dropdown-item command="settings">
              <router-link to="/settings" style="text-decoration:none;color:inherit;display:block">设置</router-link>
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
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  height: 56px;
  box-sizing: border-box;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-city {
  font-weight: 600;
  font-size: 15px;
  color: #333;
  cursor: pointer;
}

.header-center {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 0 0 auto;
}

.search-input {
  width: 360px;
}

.view-toggle {
  display: flex;
  background: #f5f5f5;
  border-radius: 6px;
  overflow: hidden;
}

.toggle-option {
  padding: 4px 10px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
}

.toggle-option.active {
  background: #fff3ed;
  color: #ff6b35;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b35, #f7931e);
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
  border-radius: 50%;
  object-fit: cover;
}
</style>
