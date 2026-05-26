<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const ok = await auth.login({ username: form.username, password: form.password })
    if (ok) router.push('/')
  } catch (err) {
    const error = err as { response?: { data?: { message?: string } } }
    ElMessage.error(error?.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-brand">
      <div class="brand-name">JustGo</div>
      <div class="brand-sub">即行</div>
      <p class="brand-desc">发现城市活动，找到搭子</p>
      <div class="brand-emoji">🏙️</div>
      <p class="brand-footer">探索你身边的精彩活动</p>
    </div>
    <div class="auth-form">
      <h2 class="form-title">欢迎回来</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="输入密码" size="large" show-password />
        </el-form-item>
        <div class="form-extra"><a class="forgot-link">忘记密码？</a></div>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleLogin">登 录</el-button>
      </el-form>
      <div class="divider"><span>其他方式</span></div>
      <div class="social-btns">
        <div class="social-btn">W</div><div class="social-btn">G</div><div class="social-btn">📱</div>
      </div>
      <p class="switch-link">还没有账号？<router-link to="/register">立即注册</router-link></p>
    </div>
  </div>
</template>

<style scoped>
.auth-page { display: flex; width: 840px; min-height: 520px; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 32px rgba(0,0,0,0.1); }
.auth-brand { flex: 1; background: linear-gradient(135deg, #ff6b35 0%, #f7931e 50%, #ffc107 100%); display: flex; flex-direction: column; align-items: center; justify-content: center; color: #fff; padding: 48px 32px; }
.brand-name { font-size: 36px; font-weight: 800; }
.brand-sub { font-size: 20px; margin-bottom: 12px; }
.brand-desc { font-size: 14px; opacity: 0.9; }
.brand-emoji { font-size: 64px; margin: 24px 0 16px; }
.brand-footer { font-size: 12px; opacity: 0.7; }
.auth-form { flex: 1; background: #fff; padding: 48px 40px; display: flex; flex-direction: column; justify-content: center; }
.form-title { font-size: 22px; font-weight: 700; color: #333; margin-bottom: 28px; }
.form-extra { display: flex; justify-content: flex-end; margin-bottom: 16px; margin-top: -8px; }
.forgot-link { font-size: 12px; color: #ff6b35; cursor: pointer; }
.submit-btn { width: 100%; background: #ff6b35; border-color: #ff6b35; }
.submit-btn:hover { background: #e55a2b; border-color: #e55a2b; }
.divider { display: flex; align-items: center; gap: 12px; margin: 20px 0; color: #bbb; font-size: 12px; }
.divider::before, .divider::after { content: ''; flex: 1; height: 1px; background: #eee; }
.social-btns { display: flex; gap: 12px; justify-content: center; }
.social-btn { width: 40px; height: 40px; border-radius: 50%; background: #f5f5f5; display: flex; align-items: center; justify-content: center; font-size: 16px; color: #666; cursor: pointer; }
.social-btn:hover { background: #eee; }
.switch-link { text-align: center; margin-top: 24px; font-size: 13px; color: #999; }
.switch-link a { color: #ff6b35; font-weight: 600; text-decoration: none; }
</style>
