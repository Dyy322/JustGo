<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'
import AppHeader from '@/components/AppHeader.vue'
import { useAuthStore } from '@/stores/auth'
import { useMediaQuery } from '@/composables/useMediaQuery'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isMobile = useMediaQuery('(max-width: 900px)')
const isAuthPage = () => route.path === '/login' || route.path === '/register'

// 退出登录或 token 失效时自动跳转登录页
watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (!loggedIn && !isAuthPage()) {
      router.replace('/login')
    }
  },
)

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
      <AppHeader v-if="!isMobile" />
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style>
:root {
  --jg-bg: #f3f2ef;
  --jg-bg-strong: #e8e5de;
  --jg-surface: #fcfbf7;
  --jg-surface-muted: #f7f5ef;
  --jg-ink: #171815;
  --jg-ink-soft: #464940;
  --jg-muted: #7a7d72;
  --jg-line: rgba(28, 30, 24, 0.1);
  --jg-line-strong: rgba(28, 30, 24, 0.16);
  --jg-accent: #54746a;
  --jg-accent-deep: #2f4f47;
  --jg-accent-soft: #dce6df;
  --jg-danger: #a84f45;
  --jg-radius-sm: 8px;
  --jg-radius-md: 14px;
  --jg-radius-lg: 22px;
  --jg-shadow-soft: 0 22px 70px rgba(44, 49, 38, 0.1);
  --jg-shadow-card: 0 12px 36px rgba(44, 49, 38, 0.08);
  --jg-ease: cubic-bezier(0.22, 1, 0.36, 1);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  scroll-behavior: smooth;
}

html,
body,
#app {
  height: 100%;
  font-family: 'Avenir Next', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  color: var(--jg-ink);
  background: var(--jg-bg);
  font-variant-numeric: tabular-nums;
}

body {
  min-width: 320px;
}

body::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: 100;
  pointer-events: none;
  opacity: 0.045;
  background-image:
    radial-gradient(circle at 20% 20%, rgba(23, 24, 21, 0.5) 0 1px, transparent 1px),
    radial-gradient(circle at 80% 70%, rgba(23, 24, 21, 0.35) 0 1px, transparent 1px);
  background-size:
    26px 26px,
    38px 38px;
  mix-blend-mode: multiply;
}

a {
  color: inherit;
}

button,
input,
textarea {
  font: inherit;
}

button,
a,
.el-button {
  transition:
    transform 180ms var(--jg-ease),
    background-color 180ms var(--jg-ease),
    border-color 180ms var(--jg-ease),
    color 180ms var(--jg-ease),
    box-shadow 180ms var(--jg-ease);
}

button:active,
a:active,
.el-button:active {
  transform: translateY(1px) scale(0.99);
}

*:focus-visible {
  outline: 2px solid rgba(84, 116, 106, 0.5);
  outline-offset: 3px;
}

::selection {
  background: var(--jg-accent-soft);
  color: var(--jg-accent-deep);
}

::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

::-webkit-scrollbar-thumb {
  background: rgba(84, 116, 106, 0.28);
  border: 3px solid transparent;
  border-radius: 999px;
  background-clip: padding-box;
}

::-webkit-scrollbar-track {
  background: transparent;
}

.el-button--primary {
  --el-button-bg-color: var(--jg-accent);
  --el-button-border-color: var(--jg-accent);
  --el-button-hover-bg-color: var(--jg-accent-deep);
  --el-button-hover-border-color: var(--jg-accent-deep);
  --el-button-active-bg-color: var(--jg-accent-deep);
  --el-button-active-border-color: var(--jg-accent-deep);
}

.el-input__wrapper,
.el-textarea__inner {
  border-radius: var(--jg-radius-sm) !important;
  box-shadow: 0 0 0 1px var(--jg-line) inset !important;
  background: rgba(252, 251, 247, 0.78) !important;
}

.el-input__wrapper:hover,
.el-textarea__inner:hover {
  box-shadow: 0 0 0 1px var(--jg-line-strong) inset !important;
}

.el-input__wrapper.is-focus,
.el-textarea__inner:focus {
  box-shadow: 0 0 0 1px var(--jg-accent) inset !important;
}

.page-enter-active,
.page-leave-active {
  transition:
    opacity 220ms var(--jg-ease),
    transform 220ms var(--jg-ease);
}

.page-enter-from,
.page-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
  }
}

.app-layout {
  display: flex;
  min-height: 100dvh;
  height: 100dvh;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 12%, rgba(84, 116, 106, 0.13), transparent 28%),
    linear-gradient(135deg, var(--jg-bg), #eeece5 52%, var(--jg-bg-strong));
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
  background: transparent;
}

.auth-layout {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow-y: auto;
  background:
    radial-gradient(circle at 12% 12%, rgba(84, 116, 106, 0.16), transparent 28%),
    radial-gradient(circle at 88% 78%, rgba(23, 24, 21, 0.1), transparent 24%),
    linear-gradient(135deg, #f5f3ed, #e6e2d9);
}

@media (max-width: 780px) {
  .auth-layout {
    align-items: flex-start;
    padding: 16px;
    min-height: 100dvh;
  }
}

@media (max-width: 900px) {
  .app-layout {
    display: block;
    height: 100dvh;
    overflow: hidden;
  }

  .main-area {
    display: flex;
    flex-direction: column;
    height: 100dvh;
    overflow: hidden;
  }

  .main-content {
    flex: 1;
    overflow-y: auto;
    overflow-x: hidden;
    -webkit-overflow-scrolling: touch;
    padding-bottom: 78px;
  }
}
</style>
