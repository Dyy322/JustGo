<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'
import AppHeader from '@/components/AppHeader.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isAuthPage = () => route.path === '/login' || route.path === '/register'

onMounted(async () => {
  await auth.initAuth()
  if (!auth.isLoggedIn && !isAuthPage()) {
    router.replace('/login')
  }
  if (auth.isLoggedIn && isAuthPage()) {
    router.replace('/')
  }
})
</script>

<template>
  <div v-if="isAuthPage()" class="auth-layout">
    <router-view />
  </div>

  <div v-else class="app-layout">
    <AppSidebar />
    <div class="main-area">
      <AppHeader />
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  color: #333;
  background: #faf8f6;
}

.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  background: #faf8f6;
}

.auth-layout {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f0eb;
}
</style>
