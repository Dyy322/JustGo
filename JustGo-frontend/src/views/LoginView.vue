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
      <div class="brand-orbit"><span /><span /><span /></div>
      <p class="brand-footer">上海 · 周末 · 同频的人</p>
    </div>
    <div class="auth-form">
      <h2 class="form-title">欢迎回来</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="输入密码"
            size="large"
            show-password
          />
        </el-form-item>
        <div class="form-extra"><a class="forgot-link">忘记密码？</a></div>
        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="loading"
          @click="handleLogin"
          >登 录</el-button
        >
      </el-form>
      <div class="divider"><span>其他方式</span></div>
      <div class="social-btns">
        <div class="social-btn">W</div>
        <div class="social-btn">G</div>
        <div class="social-btn">M</div>
      </div>
      <p class="switch-link">还没有账号？<router-link to="/register">立即注册</router-link></p>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 420px);
  width: min(960px, 100%);
  min-height: 560px;
  border-radius: 28px;
  overflow: hidden;
  background: rgba(252, 251, 247, 0.72);
  border: 1px solid var(--jg-line);
  box-shadow: var(--jg-shadow-soft);
  animation: auth-rise 520ms var(--jg-ease) both;
}
.auth-brand {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  background: #171815;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  color: #fff;
  padding: 56px;
}
.auth-brand::before {
  content: '';
  position: absolute;
  inset: 18px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 22px;
  pointer-events: none;
}
.auth-brand::after {
  content: '';
  position: absolute;
  width: 360px;
  height: 360px;
  left: -130px;
  bottom: -160px;
  background: rgba(84, 116, 106, 0.42);
  border-radius: 999px;
  filter: blur(2px);
  z-index: -1;
}
.brand-name {
  font-size: 52px;
  line-height: 0.95;
  font-weight: 900;
  letter-spacing: 0;
}
.brand-sub {
  font-size: 20px;
  margin: 10px 0 18px;
  color: rgba(255, 255, 255, 0.78);
}
.brand-desc {
  max-width: 18em;
  font-size: 15px;
  line-height: 1.8;
  opacity: 0.82;
  text-wrap: pretty;
}
.brand-orbit {
  position: relative;
  width: 180px;
  height: 180px;
  margin: 42px 0 22px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  animation: orbit 9s linear infinite;
}
.brand-orbit span {
  position: absolute;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: #dce6df;
  box-shadow: 0 16px 42px rgba(220, 230, 223, 0.22);
}
.brand-orbit span:nth-child(1) {
  top: 20px;
  left: 28px;
}
.brand-orbit span:nth-child(2) {
  right: 18px;
  top: 72px;
  background: #8fa99f;
}
.brand-orbit span:nth-child(3) {
  left: 74px;
  bottom: 18px;
  background: #fff;
}
.brand-footer {
  font-size: 12px;
  opacity: 0.68;
}
.auth-form {
  flex: 1;
  background: rgba(252, 251, 247, 0.96);
  padding: 54px 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.form-title {
  font-size: 28px;
  font-weight: 900;
  color: var(--jg-ink);
  margin-bottom: 30px;
  text-wrap: balance;
}
.form-extra {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
  margin-top: -8px;
}
.forgot-link {
  font-size: 12px;
  color: var(--jg-accent-deep);
  cursor: pointer;
  font-weight: 700;
}
.submit-btn {
  width: 100%;
  min-height: 46px;
  border-radius: 999px;
  font-weight: 800;
}
.divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 22px 0;
  color: var(--jg-muted);
  font-size: 12px;
}
.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--jg-line);
}
.social-btns {
  display: flex;
  gap: 12px;
  justify-content: center;
}
.social-btn {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: var(--jg-surface-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  color: var(--jg-ink-soft);
  cursor: pointer;
  box-shadow: 0 0 0 1px var(--jg-line) inset;
}
.social-btn:hover {
  background: var(--jg-accent-soft);
  color: var(--jg-accent-deep);
  transform: translateY(-2px);
}
.switch-link {
  text-align: center;
  margin-top: 24px;
  font-size: 13px;
  color: var(--jg-muted);
}
.switch-link a {
  color: var(--jg-accent-deep);
  font-weight: 800;
  text-decoration: none;
}
@keyframes auth-rise {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.98);
  }
}
@keyframes orbit {
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 780px) {
  .auth-page {
    grid-template-columns: 1fr;
    min-height: 0;
    height: auto;
  }
  .auth-brand {
    min-height: 300px;
    padding: 38px;
  }
  .brand-name {
    font-size: 42px;
  }
  .brand-orbit {
    display: none;
  }
  .auth-form {
    padding: 36px 24px;
  }
}
</style>
