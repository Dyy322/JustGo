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
  nickname: '',
  email: '',
  password: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度 3-50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 72, message: '密码长度 6-72 个字符', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }],
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const ok = await auth.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
      email: form.email || undefined,
    })
    if (ok) router.push('/login')
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '注册失败')
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
      <p class="brand-desc">加入我们，一起探索</p>
      <div class="brand-emoji">🤝</div>
      <p class="brand-footer">已有账号？快去登录吧</p>
    </div>
    <div class="auth-form">
      <h2 class="form-title">创建账号</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username" required>
          <el-input v-model="form.username" placeholder="3-50 个字符" size="large" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="选填" size="large" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="选填" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password" required>
          <el-input v-model="form.password" type="password" placeholder="6-72 个字符" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleRegister">注 册</el-button>
      </el-form>
      <p class="switch-link">已有账号？<router-link to="/login">去登录</router-link></p>
    </div>
  </div>
</template>

<style scoped>
.auth-page { display: flex; width: 840px; min-height: 560px; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 32px rgba(0,0,0,0.1); }
.auth-brand { flex: 1; background: linear-gradient(135deg, #ff6b35 0%, #f7931e 50%, #ffc107 100%); display: flex; flex-direction: column; align-items: center; justify-content: center; color: #fff; padding: 48px 32px; }
.brand-name { font-size: 36px; font-weight: 800; }
.brand-sub { font-size: 20px; margin-bottom: 12px; }
.brand-desc { font-size: 14px; opacity: 0.9; }
.brand-emoji { font-size: 64px; margin: 24px 0 16px; }
.brand-footer { font-size: 12px; opacity: 0.7; }
.auth-form { flex: 1; background: #fff; padding: 36px 40px; display: flex; flex-direction: column; justify-content: center; }
.form-title { font-size: 22px; font-weight: 700; color: #333; margin-bottom: 20px; }
.submit-btn { width: 100%; margin-top: 8px; background: #ff6b35; border-color: #ff6b35; }
.submit-btn:hover { background: #e55a2b; border-color: #e55a2b; }
.switch-link { text-align: center; margin-top: 20px; font-size: 13px; color: #999; }
.switch-link a { color: #ff6b35; font-weight: 600; text-decoration: none; }
</style>
