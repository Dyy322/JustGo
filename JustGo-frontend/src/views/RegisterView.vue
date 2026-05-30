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
      <div class="brand-orbit"><span /><span /><span /></div>
      <p class="brand-footer">创建你的城市活动身份</p>
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
          <el-input
            v-model="form.password"
            type="password"
            placeholder="6-72 个字符"
            size="large"
            show-password
          />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="loading"
          @click="handleRegister"
          >注 册</el-button
        >
      </el-form>
      <p class="switch-link">已有账号？<router-link to="/login">去登录</router-link></p>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 420px);
  width: min(960px, 100%);
  min-height: 590px;
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
  padding: 38px 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.form-title {
  font-size: 28px;
  font-weight: 900;
  color: var(--jg-ink);
  margin-bottom: 22px;
  text-wrap: balance;
}
.submit-btn {
  width: 100%;
  min-height: 46px;
  margin-top: 8px;
  border-radius: 999px;
  font-weight: 800;
}
.switch-link {
  text-align: center;
  margin-top: 20px;
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
    min-height: auto;
  }
  .auth-brand {
    min-height: 280px;
    padding: 38px;
  }
  .brand-name {
    font-size: 42px;
  }
  .brand-orbit {
    display: none;
  }
  .auth-form {
    padding: 32px 24px;
  }
}

@media (max-width: 780px) {
  .auth-page {
    grid-template-columns: 1fr;
    min-height: 0;
    height: auto;
  }
  .auth-brand {
    min-height: 260px;
    padding: 34px;
  }
  .brand-name { font-size: 38px; }
  .brand-orbit { display: none; }
  .auth-form { padding: 28px 22px; }
}</style>
