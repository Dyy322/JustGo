<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { ActivityDetailResponse, CategoryResponse, TagResponse } from '@/types/api'
import RichTextEditor from './RichTextEditor.vue'
import ActivityImageUploader from './ActivityImageUploader.vue'

function extractObjectKey(url: string): string {
  if (!url) return ''
  try {
    const u = new URL(url)
    return u.pathname.substring(1)
  } catch {
    return url
  }
}

const props = withDefaults(
  defineProps<{
    initial?: Partial<ActivityDetailResponse> | null
    submitLabel?: string
    loading?: boolean
  }>(),
  { initial: null, submitLabel: '发布活动', loading: false },
)

const emit = defineEmits<{
  submit: [
    value: {
      title: string
      description: string
      categoryId: number
      locationName: string
      latitude: number | undefined
      longitude: number | undefined
      address: string
      startTime: string
      endTime: string
      maxParticipants: number
      tagIds: number[]
      coverImage: string
    },
  ]
}>()

const formRef = ref<FormInstance>()
const categories = ref<CategoryResponse[]>([])
const tags = ref<TagResponse[]>([])
const showGeo = ref(false)

const initialCoverUrl = computed(() => props.initial?.coverImage ?? '')
const initialCoverKey = computed(() => extractObjectKey(initialCoverUrl.value))

const form = ref({
  title: props.initial?.title ?? '',
  description: props.initial?.description ?? '',
  categoryId: props.initial?.categoryId ?? (null as number | null),
  locationName: props.initial?.locationName ?? '',
  latitude: props.initial?.latitude ?? (undefined as number | undefined),
  longitude: props.initial?.longitude ?? (undefined as number | undefined),
  address: props.initial?.address ?? '',
  startTime: props.initial?.startTime ?? '',
  endTime: props.initial?.endTime ?? '',
  maxParticipants: props.initial?.maxParticipants ?? 0,
  selectedTagNames: (props.initial?.tags ?? []) as string[],
  coverImageKeys: [] as string[],
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择活动分类', trigger: 'change' }],
  locationName: [{ required: true, message: '请输入活动地点', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
}

function handleSubmit() {
  formRef.value?.validate((valid) => {
    if (!valid) return
    const nameToId = new Map(tags.value.map((t) => [t.name, t.id]))
    const tagIds = form.value.selectedTagNames
      .map((n) => nameToId.get(n))
      .filter((id): id is number => id != null)
    const coverImage = form.value.coverImageKeys[0] || initialCoverKey.value || ''
    emit('submit', {
      title: form.value.title,
      description: form.value.description,
      categoryId: form.value.categoryId!,
      locationName: form.value.locationName,
      latitude: form.value.latitude,
      longitude: form.value.longitude,
      address: form.value.address,
      startTime: new Date(form.value.startTime).toISOString(),
      endTime: form.value.endTime ? new Date(form.value.endTime).toISOString() : '',
      maxParticipants: form.value.maxParticipants,
      tagIds,
      coverImage,
    })
  })
}

async function loadCategories() {
  try {
    const { data } = await import('@/api/category').then((m) => m.listCategories())
    if (data.code === 0) categories.value = data.data
  } catch {
    ElMessage.error('加载分类失败')
  }
}

async function loadTags() {
  try {
    const { data } = await import('@/api/category').then((m) => m.listTags())
    if (data.code === 0) tags.value = data.data
  } catch {
    ElMessage.error('加载标签失败')
  }
}

onMounted(() => {
  loadCategories()
  loadTags()
})
defineExpose({ submit: handleSubmit })
</script>

<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="af">
    <!-- ===== 标题 + 封面 双列 ===== -->
    <div class="af-hero-block">
      <div class="af-hero-left">
        <el-form-item class="af-title-field" prop="title">
          <template #label>
            <span class="af-label">活动标题</span>
          </template>
          <el-input
            v-model="form.title"
            maxlength="100"
            show-word-limit
            placeholder="给你的活动起个吸引人的名字"
            class="af-hero-input af-input-lg"
          />
        </el-form-item>

        <el-form-item prop="categoryId">
          <template #label>
            <span class="af-label">分类</span>
          </template>
          <div class="af-cat-grid">
            <button
              v-for="cat in categories"
              :key="cat.id"
              type="button"
              class="af-cat-btn"
              :class="{ on: form.categoryId === cat.id }"
              @click="form.categoryId = cat.id"
            >
              <span class="af-cat-emoji">{{ cat.icon }}</span>
              <span>{{ cat.name }}</span>
            </button>
          </div>
        </el-form-item>
      </div>

      <div class="af-hero-right">
        <div class="af-cover-label">封面</div>
        <div class="af-cover-zone">
          <img
            v-if="initialCoverUrl && form.coverImageKeys.length === 0"
            :src="initialCoverUrl"
            class="af-cover-preview-img"
          />
          <ActivityImageUploader
            v-model="form.coverImageKeys"
            :max-count="1"
            :aspect-ratio="4 / 3"
          />
        </div>
        <p v-if="initialCoverUrl && form.coverImageKeys.length === 0" class="af-hint">
          上传新图替换当前封面
        </p>
      </div>
    </div>

    <!-- ===== 时间 + 地点 双列 ===== -->
    <div class="af-card af-card--time-loc">
      <div class="af-card-col">
        <div class="af-card-label">时间 & 地点</div>
        <el-form-item label="活动地点" prop="locationName">
          <el-input
            v-model="form.locationName"
            maxlength="200"
            placeholder="如 世纪公园 · 浦东滨江大道"
            class="af-input af-input-lg"
          />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input
            v-model="form.address"
            maxlength="300"
            placeholder="补充详细地址方便参与者找到位置（选填）"
            class="af-input"
          />
        </el-form-item>
      </div>

      <div class="af-card-col">
        <div class="af-card-label">&nbsp;</div>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择日期和时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="af-date af-date-lg"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选填"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="af-date af-date-lg"
          />
        </el-form-item>

        <button type="button" class="af-geo-toggle" @click="showGeo = !showGeo">
          {{ showGeo ? '收起坐标' : '+ 经纬度' }}
        </button>
        <div v-if="showGeo" class="af-geo-row">
          <el-form-item label="纬度" class="af-geo-half">
            <el-input-number
              v-model="form.latitude"
              :min="-90"
              :max="90"
              :precision="6"
              controls-position="right"
              placeholder="纬度"
              class="af-num-sm"
            />
          </el-form-item>
          <el-form-item label="经度" class="af-geo-half">
            <el-input-number
              v-model="form.longitude"
              :min="-180"
              :max="180"
              :precision="6"
              controls-position="right"
              placeholder="经度"
              class="af-num-sm"
            />
          </el-form-item>
        </div>
      </div>
    </div>

    <!-- ===== 详情 + 标签 ===== -->
    <div class="af-card af-card--detail">
      <div class="af-card-col">
        <div class="af-card-label">人数 & 标签</div>
        <div class="af-detail-row">
          <el-form-item label="人数上限" class="af-people-field">
            <el-input-number
              v-model="form.maxParticipants"
              :min="0"
              :max="99999"
              controls-position="right"
              class="af-num-lg"
            />
            <span class="af-hint-inline">0 = 人数不限</span>
          </el-form-item>
        </div>
        <el-form-item label="标签">
          <div class="af-tags">
            <button
              v-for="t in tags"
              :key="t.id"
              type="button"
              class="af-tag-chip"
              :class="{ on: form.selectedTagNames.includes(t.name) }"
              @click="
                form.selectedTagNames.includes(t.name)
                  ? (form.selectedTagNames = form.selectedTagNames.filter((n) => n !== t.name))
                  : form.selectedTagNames.push(t.name)
              "
            >
              {{ t.name }}
            </button>
          </div>
        </el-form-item>
      </div>
    </div>

    <!-- ===== 描述 全宽 ===== -->
    <div class="af-card af-card--desc">
      <div class="af-card-label">活动描述</div>
      <el-form-item class="af-desc-field">
        <RichTextEditor v-model="form.description" />
      </el-form-item>
    </div>

    <!-- ===== Submit ===== -->
    <button type="button" class="af-submit" :disabled="loading" @click="handleSubmit">
      <span v-if="loading" class="af-spin" />
      <span v-else>{{ submitLabel }}</span>
    </button>
  </el-form>
</template>

<style scoped>
/* ===== Layout ===== */
.af {
  max-width: 820px;
  margin: 0 auto;
  animation: form-in 440ms var(--jg-ease) both;
}

/* ===== Hero Block: title + cover side by side ===== */
.af-hero-block {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 18px;
  margin-bottom: 18px;
}
.af-hero-left {
  flex: 1;
  min-width: 0;
}
.af-hero-right {
  width: 260px;
  flex-shrink: 0;
}

/* ===== Cards ===== */
.af-card {
  background: rgba(252, 251, 247, 0.82);
  border-radius: var(--jg-radius-lg);
  padding: 28px;
  margin-bottom: 18px;
  border: 1px solid var(--jg-line);
  box-shadow: var(--jg-shadow-card);
}
.af-card--time-loc {
  display: flex;
  gap: 28px;
}
.af-card--time-loc .af-card-col {
  flex: 1;
  min-width: 0;
}
.af-card--detail {
  display: flex;
  gap: 28px;
}
.af-card--detail .af-card-col {
  flex: 1;
  min-width: 0;
}
.af-card--desc {
  display: flex;
  flex-direction: column;
}

.af-card-label {
  font-size: 14px;
  font-weight: 900;
  color: var(--jg-ink);
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--jg-line);
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ===== Labels ===== */
.af-label {
  font-size: 14px;
  font-weight: 800;
  color: var(--jg-ink-soft);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* ===== Inputs ===== */
.af-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: rgba(247, 245, 239, 0.9);
  box-shadow: 0 0 0 1px var(--jg-line) inset !important;
  padding: 10px 14px;
  transition:
    background 180ms var(--jg-ease),
    box-shadow 180ms var(--jg-ease);
}
.af-input :deep(.el-input__wrapper:hover) {
  background: #fff;
}
.af-input :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px var(--jg-accent) inset !important;
}
.af-input-lg :deep(.el-input__wrapper) {
  padding: 14px 16px;
}
.af-input-lg :deep(.el-input__inner) {
  font-size: 17px;
  font-weight: 600;
}
.af-hero-input :deep(.el-input__inner) {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
}

/* Date */
.af-date {
  width: 100%;
}
.af-date :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: rgba(247, 245, 239, 0.9);
  box-shadow: 0 0 0 1px var(--jg-line) inset !important;
  padding: 10px 14px;
}
.af-date :deep(.el-input__wrapper:hover) {
  background: #fff;
}
.af-date-lg :deep(.el-input__wrapper) {
  padding: 14px 16px;
}

/* Number */
.af-num-lg {
  width: 160px;
}
.af-num-lg :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: rgba(247, 245, 239, 0.9);
  box-shadow: 0 0 0 1px var(--jg-line) inset !important;
  padding: 14px 16px;
}
.af-num-lg :deep(.el-input__wrapper:hover) {
  background: #fff;
}
.af-num-sm {
  width: 100%;
}
.af-num-sm :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: rgba(247, 245, 239, 0.9);
  box-shadow: 0 0 0 1px var(--jg-line) inset !important;
  padding: 10px 14px;
}
.af-num-sm :deep(.el-input__wrapper:hover) {
  background: #fff;
}

/* ===== Cover ===== */
.af-cover-label {
  font-size: 14px;
  font-weight: 800;
  color: var(--jg-ink-soft);
  margin-bottom: 10px;
}
.af-cover-zone {
  min-height: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.af-cover-preview-img {
  width: 100%;
  border-radius: 14px;
  object-fit: cover;
  max-height: 240px;
  border: 1px solid var(--jg-line);
}

/* ===== Category grid ===== */
.af-cat-grid {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: thin;
}
.af-cat-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 88px;
  flex: 1 0 88px;
  padding: 10px 2px 8px;
  border-radius: 14px;
  border: 1px solid var(--jg-line);
  background: rgba(247, 245, 239, 0.88);
  font-size: 11px;
  color: var(--jg-muted);
  white-space: nowrap;
  cursor: pointer;
  transition:
    transform 180ms var(--jg-ease),
    background-color 180ms var(--jg-ease),
    border-color 180ms var(--jg-ease),
    color 180ms var(--jg-ease);
}
.af-cat-btn:hover {
  border-color: rgba(84, 116, 106, 0.28);
  background: #fff;
  transform: translateY(-1px);
}
.af-cat-btn.on {
  border-color: var(--jg-accent);
  background: var(--jg-accent-soft);
  color: var(--jg-accent-deep);
  font-weight: 800;
}
.af-cat-emoji {
  font-size: 22px;
}

.af-desc-field {
  margin-bottom: 0;
}
.af-desc-field :deep(.el-form-item__content) {
  display: block;
  width: 100%;
}

/* ===== Tags ===== */
.af-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.af-tag-chip {
  padding: 7px 14px;
  border-radius: 999px;
  border: 1px solid var(--jg-line);
  background: rgba(252, 251, 247, 0.9);
  font-size: 13px;
  color: var(--jg-muted);
  cursor: pointer;
  transition:
    transform 180ms var(--jg-ease),
    background-color 180ms var(--jg-ease),
    color 180ms var(--jg-ease);
}
.af-tag-chip:hover {
  border-color: var(--jg-accent);
  color: var(--jg-accent-deep);
  transform: translateY(-1px);
}
.af-tag-chip.on {
  background: var(--jg-ink);
  border-color: var(--jg-ink);
  color: #fff;
  font-weight: 800;
}

/* ===== People field ===== */
.af-people-field :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

/* ===== Geo ===== */
.af-geo-toggle {
  border: none;
  background: none;
  font-size: 12px;
  color: var(--jg-muted);
  cursor: pointer;
  padding: 4px 0;
  display: block;
}
.af-geo-toggle:hover {
  color: var(--jg-accent-deep);
}
.af-geo-row {
  display: flex;
  gap: 8px;
}
.af-geo-half {
  flex: 1;
}

/* ===== Hint ===== */
.af-hint {
  font-size: 12px;
  color: var(--jg-muted);
  margin: 6px 0 0;
}
.af-hint-inline {
  font-size: 12px;
  color: var(--jg-muted);
}

/* ===== Submit ===== */
.af-submit {
  width: 100%;
  padding: 18px 0;
  font-size: 17px;
  font-weight: 900;
  color: #fff;
  background: var(--jg-ink);
  border: none;
  border-radius: 999px;
  cursor: pointer;
  letter-spacing: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 16px 36px rgba(23, 24, 21, 0.16);
}
.af-submit:hover {
  background: var(--jg-accent-deep);
  transform: translateY(-1px);
}
.af-submit:active {
  transform: scale(0.99);
}
.af-submit:disabled {
  opacity: 0.6;
  cursor: default;
}
.af-spin {
  width: 22px;
  height: 22px;
  border: 2px solid rgba(255, 255, 255, 0.25);
  border-top-color: #fff;
  border-radius: 50%;
  animation: af-spin 0.6s linear infinite;
}
@keyframes af-spin {
  to {
    transform: rotate(360deg);
  }
}
@keyframes form-in {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
}

@media (max-width: 820px) {
  .af-hero-block,
  .af-card--time-loc {
    grid-template-columns: 1fr;
    display: grid;
  }

  .af-hero-right {
    width: auto;
  }

  .af-card {
    padding: 22px;
  }

  .af-cat-grid {
    margin-right: -2px;
  }
}
</style>
