<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { updateProfile, changePassword } from '@/api/user'
import { getUploadToken } from '@/api/file'
import { getErrorMessage } from '@/api/error'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import axios from 'axios'

const auth = useAuthStore()
const activeTab = ref<'profile' | 'password'>('profile')

const profileFormRef = ref<FormInstance>()
const profileLoading = ref(false)
const profileForm = reactive({ nickname: '', email: '', phone: '', gender: 0 })
const profileRules: FormRules = {
  email: [{ type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }],
}

onMounted(() => {
  if (auth.currentUser) {
    profileForm.nickname = auth.currentUser.nickname || ''
    profileForm.email = auth.currentUser.email || ''
    profileForm.phone = auth.currentUser.phone || ''
    profileForm.gender = auth.currentUser.gender || 0
  }
})

async function handleSaveProfile() {
  const valid = await profileFormRef.value?.validate().catch(() => false)
  if (!valid) return
  profileLoading.value = true
  try {
    const res = await updateProfile({
      nickname: profileForm.nickname || undefined,
      email: profileForm.email || undefined,
      phone: profileForm.phone || undefined,
      gender: profileForm.gender,
    })
    if (res.data.code === 0) {
      auth.currentUser = res.data.data
      ElMessage.success('资料更新成功')
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '更新失败')
  } finally {
    profileLoading.value = false
  }
}

const avatarUploading = ref(false)
async function handleAvatarChange(file: File) {
  const ext = file.name.split('.').pop()?.toLowerCase() || 'jpg'
  if (!['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext)) {
    ElMessage.error('仅支持 JPG/PNG/GIF/WebP 格式')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 5MB')
    return
  }
  avatarUploading.value = true
  try {
    const tokenRes = await getUploadToken({ ext, prefix: 'avatar' })
    if (tokenRes.data.code !== 0) {
      ElMessage.error(tokenRes.data.message || '获取上传凭证失败')
      return
    }
    const { uploadUrl, fileUrl } = tokenRes.data.data
    const mimeType = { jpg: 'image/jpeg', jpeg: 'image/jpeg', png: 'image/png', gif: 'image/gif', webp: 'image/webp' }[ext] || 'application/octet-stream'
    await axios.put(uploadUrl, file, { headers: { 'Content-Type': mimeType } })
    const updateRes = await updateProfile({ avatar: fileUrl })
    if (updateRes.data.code === 0) {
      auth.currentUser = updateRes.data.data
      avatarFailed.value = false
      ElMessage.success('头像更新成功')
    } else {
      ElMessage.error(updateRes.data.message || '保存头像失败')
    }
  } catch (err) {
    console.error('头像上传失败', err)
    ElMessage.error(getErrorMessage(err, '头像上传失败'))
  } finally {
    avatarUploading.value = false
  }
}

function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) handleAvatarChange(file)
  input.value = ''
}

const passwordFormRef = ref<FormInstance>()
const passwordLoading = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '' })
const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 72, message: '密码长度 6-72 个字符', trigger: 'blur' },
  ],
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return
  passwordLoading.value = true
  try {
    const res = await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    if (res.data.code === 0) {
      ElMessage.success('密码修改成功，请重新登录')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      setTimeout(() => auth.logoutAction(), 1500)
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '密码修改失败')
  } finally {
    passwordLoading.value = false
  }
}

const avatarFailed = ref(false)

const avatarChar = computed(() => {
  const u = auth.currentUser
  if (!u) return '?'
  const name = u.nickname || u.username
  return name.charAt(0)
})
</script>

<template>
  <div class="settings-page">
    <div class="settings-sidebar">
      <div
        class="sidebar-tab"
        :class="{ active: activeTab === 'profile' }"
        @click="activeTab = 'profile'"
      >
        编辑资料
      </div>
      <div
        class="sidebar-tab"
        :class="{ active: activeTab === 'password' }"
        @click="activeTab = 'password'"
      >
        修改密码
      </div>
    </div>
    <div class="settings-content">
      <template v-if="activeTab === 'profile'">
        <h3 class="content-title">编辑资料</h3>
        <div class="avatar-upload">
          <div class="avatar-preview">
            <img v-if="auth.currentUser?.avatar && !avatarFailed" :src="auth.currentUser.avatar" @error="avatarFailed = true" />
            <span v-else>{{ avatarChar }}</span>
          </div>
          <div class="avatar-action">
            <label class="upload-label">
              更换头像
              <input
                type="file"
                accept="image/jpeg,image/png,image/gif,image/webp"
                hidden
                @change="onFileSelected"
              />
            </label>
            <p class="upload-hint">支持 JPG/PNG/GIF/WebP，最大 5MB</p>
            <p v-if="avatarUploading" class="upload-hint" style="color: #ff6b35">上传中...</p>
          </div>
        </div>
        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-position="top"
          class="settings-form"
        >
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="profileForm.nickname" placeholder="输入昵称" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="profileForm.email" placeholder="输入邮箱" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="profileForm.phone" placeholder="输入手机号" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="profileForm.gender">
              <el-radio-button :value="1">男</el-radio-button>
              <el-radio-button :value="2">女</el-radio-button>
              <el-radio-button :value="0">保密</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-button type="primary" :loading="profileLoading" @click="handleSaveProfile">
            保存修改
          </el-button>
        </el-form>
      </template>
      <template v-if="activeTab === 'password'">
        <h3 class="content-title">修改密码</h3>
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-position="top"
          class="settings-form"
        >
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="输入旧密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="6-72 个字符"
              show-password
            />
          </el-form-item>
          <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">
            修改密码
          </el-button>
        </el-form>
      </template>
    </div>
  </div>
</template>

<style scoped>
.settings-page {
  display: flex;
  height: 100%;
  background: #fff;
}
.settings-sidebar {
  width: 160px;
  border-right: 1px solid #f0f0f0;
  padding: 24px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.sidebar-tab {
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
}
.sidebar-tab:hover {
  background: #f5f5f5;
}
.sidebar-tab.active {
  background: #fff3ed;
  color: #ff6b35;
  font-weight: 600;
}
.settings-content {
  flex: 1;
  padding: 32px 48px;
  overflow-y: auto;
}
.content-title {
  font-size: 18px;
  font-weight: 700;
  color: #333;
  margin-bottom: 24px;
}
.avatar-upload {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 28px;
}
.avatar-preview {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b35, #f7931e);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  overflow: hidden;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}
.upload-label {
  color: #ff6b35;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.upload-hint {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}
.settings-form {
  max-width: 400px;
}
</style>
