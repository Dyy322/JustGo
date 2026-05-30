import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserProfile, LoginRequest, RegisterRequest } from '@/types/api'
import * as authApi from '@/api/auth'
import { ElMessage } from 'element-plus'

export const useAuthStore = defineStore('auth', () => {
  const currentUser = ref<UserProfile | null>(null)
  const initialized = ref(false)
  let authRevision = 0

  const isLoggedIn = computed(() => currentUser.value !== null)

  async function initAuth() {
    const revision = authRevision
    try {
      const res = await authApi.getMe()
      if (res.data.code === 0 && revision === authRevision) {
        currentUser.value = res.data.data
      }
    } catch {
      if (revision === authRevision) {
        currentUser.value = null
      }
    } finally {
      initialized.value = true
    }
  }

  async function login(data: LoginRequest) {
    const res = await authApi.login(data)
    if (res.data.code === 0) {
      authRevision++
      currentUser.value = res.data.data.user
      ElMessage.success('登录成功')
      return true
    }
    return false
  }

  async function register(data: RegisterRequest) {
    const res = await authApi.register(data)
    if (res.data.code === 0) {
      ElMessage.success('注册成功，请登录')
      return true
    }
    return false
  }

  async function fetchUser() {
    const revision = authRevision
    try {
      const res = await authApi.getMe()
      if (res.data.code === 0 && revision === authRevision) {
        currentUser.value = res.data.data
      }
    } catch {
      if (revision === authRevision) {
        currentUser.value = null
      }
    }
  }

  async function logoutAction() {
    try {
      await authApi.logout()
    } finally {
      authRevision++
      currentUser.value = null
      ElMessage.success('已退出登录')
    }
  }

  return {
    currentUser,
    initialized,
    isLoggedIn,
    initAuth,
    login,
    register,
    fetchUser,
    logoutAction,
  }
})
