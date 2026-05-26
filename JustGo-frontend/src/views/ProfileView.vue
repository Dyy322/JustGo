<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getUserPublic } from '@/api/user'
import { followUser, unfollowUser, getFollowStats, getFollowRelation } from '@/api/social'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '@/api/error'
import type { UserPublicProfile, FollowStats, FollowRelation } from '@/types/api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const profile = ref<UserPublicProfile | null>(null)
const stats = ref<FollowStats>({ followingCount: 0, followerCount: 0 })
const relation = ref<FollowRelation>({ following: false, followsYou: false })
const loading = ref(false)
const followLoading = ref(false)

const isOwnProfile = computed(() => auth.currentUser?.id === profile.value?.id)
const displayName = computed(() => profile.value ? (profile.value.nickname || profile.value.username) : '')
const avatarChar = computed(() => displayName.value ? displayName.value.charAt(0) : '?')
const avatarFailed = ref(false)

const relationLabel = computed(() => {
  if (relation.value.following && relation.value.followsYou) return '互相关注'
  if (relation.value.followsYou) return '关注了你'
  return ''
})

async function fetchProfile() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const [profileRes, statsRes, relationRes] = await Promise.all([
      getUserPublic(id),
      getFollowStats(id),
      isOwnProfile.value ? Promise.resolve(null) : getFollowRelation(id),
    ])
    if (profileRes.data.code === 0) profile.value = profileRes.data.data
    if (statsRes.data.code === 0) stats.value = statsRes.data.data
    if (relationRes && relationRes.data.code === 0) relation.value = relationRes.data.data
  } catch (err) { ElMessage.error(getErrorMessage(err, '加载用户信息失败')) } finally { loading.value = false }
}

async function handleFollow() {
  if (!profile.value) return
  followLoading.value = true
  try {
    if (relation.value.following) {
      await unfollowUser(profile.value.id)
      relation.value.following = false; stats.value.followerCount--
      ElMessage.success('已取消关注')
    } else {
      await followUser(profile.value.id)
      relation.value.following = true; stats.value.followerCount++
      ElMessage.success('关注成功')
    }
  } catch (err) { ElMessage.error(getErrorMessage(err, '操作失败')) } finally { followLoading.value = false }
}

onMounted(fetchProfile)
watch(() => route.params.id, fetchProfile)
</script>

<template>
  <div class="profile-page" v-loading="loading">
    <div class="profile-banner"><div class="avatar-section"><div class="avatar-large"><img v-if="profile?.avatar && !avatarFailed" :src="profile.avatar" @error="avatarFailed = true" /><span v-else>{{ avatarChar }}</span></div></div></div>
    <div class="profile-info" v-if="profile">
      <div class="info-header">
        <div class="info-main">
          <h2 class="profile-name">{{ displayName }}<span v-if="relationLabel" class="relation-tag">{{ relationLabel }}</span></h2>
          <p class="profile-username">@{{ profile.username }}</p>
        </div>
        <div class="info-actions">
          <template v-if="isOwnProfile">
            <el-button @click="$router.push('/settings')">✏️ 编辑资料</el-button>
            <el-button type="primary">分享主页</el-button>
          </template>
          <template v-else>
            <el-button :type="relation.following ? 'default' : 'primary'" :loading="followLoading" @click="handleFollow">{{ relation.following ? '✓ 已关注' : '+ 关注' }}</el-button>
            <el-button>💬</el-button>
          </template>
        </div>
      </div>
      <div class="stats-row">
        <div class="stat-item" @click="$router.push(`/profile/${route.params.id}/following`)"><span class="stat-num">{{ stats.followingCount }}</span><span class="stat-label">关注</span></div>
        <div class="stat-item" @click="$router.push(`/profile/${route.params.id}/followers`)"><span class="stat-num">{{ stats.followerCount }}</span><span class="stat-label">粉丝</span></div>
      </div>
    </div>
    <div class="profile-tabs"><span class="tab active">发起的活动</span><span class="tab">参与的活动</span><span class="tab">收藏</span></div>
    <div class="profile-content"><el-empty description="暂无活动" /></div>
  </div>
</template>

<style scoped>
.profile-page { height: 100%; overflow-y: auto; }
.profile-banner { height: 160px; background: linear-gradient(135deg, #ff6b35 0%, #f7931e 50%, #ffc107 100%); position: relative; }
.avatar-section { position: absolute; bottom: -40px; left: 40px; }
.avatar-large { width: 100px; height: 100px; border-radius: 50%; background: #fff; border: 4px solid #fff; display: flex; align-items: center; justify-content: center; font-size: 36px; color: #ff6b35; font-weight: 700; overflow: hidden; }
.avatar-large img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.profile-info { background: #fff; padding: 24px 32px 0 160px; border-bottom: 1px solid #f0f0f0; }
.info-header { display: flex; align-items: flex-start; justify-content: space-between; }
.info-main { flex: 1; }
.profile-name { font-size: 22px; font-weight: 700; color: #333; display: flex; align-items: center; gap: 8px; }
.relation-tag { font-size: 11px; color: #999; background: #f5f5f5; padding: 2px 8px; border-radius: 4px; font-weight: 400; }
.profile-username { font-size: 13px; color: #999; margin-top: 4px; }
.info-actions { display: flex; gap: 8px; }
.stats-row { display: flex; gap: 32px; margin-top: 20px; padding-bottom: 16px; }
.stat-item { text-align: center; cursor: pointer; }
.stat-num { font-size: 20px; font-weight: 700; color: #333; display: block; }
.stat-num:hover { color: #ff6b35; }
.stat-label { font-size: 12px; color: #999; }
.profile-tabs { display: flex; gap: 0; padding: 0 32px 0 40px; background: #fff; border-bottom: 1px solid #f0f0f0; }
.tab { padding: 10px 20px; font-size: 14px; color: #666; cursor: pointer; border-bottom: 2px solid transparent; }
.tab.active { color: #ff6b35; border-bottom-color: #ff6b35; font-weight: 600; }
.profile-content { padding: 40px; display: flex; justify-content: center; }
</style>
