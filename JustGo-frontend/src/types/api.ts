// Generic API response wrapper
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

// Auth
export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
  nickname?: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface LoginResponse {
  tokenType: string
  accessToken: string
  expiresIn: number
  refreshToken: string
  refreshExpiresIn: number
  user: UserProfile
}

// User
export interface UserProfile {
  id: number
  username: string
  email: string | null
  phone: string | null
  nickname: string | null
  avatar: string | null
  gender: number
  status: number
  accountType: number
}

export interface UserPublicProfile {
  id: number
  username: string
  nickname: string | null
  avatar: string | null
  gender: number
}

export interface UpdateProfileRequest {
  nickname?: string
  avatar?: string
  gender?: number
  email?: string
  phone?: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

// Social
export interface FollowStats {
  followingCount: number
  followerCount: number
}

export interface FollowRelation {
  following: boolean
  followsYou: boolean
}

export interface FollowUserItem {
  userId: number
  username: string
  nickname: string | null
  avatar: string | null
  gender: number
  followedAt: string
}

export interface CursorPage<T> {
  items: T[]
  nextCursor: string | null
  hasMore: boolean
}

// File
export interface UploadTokenRequest {
  ext: string
  prefix: string
}

export interface UploadTokenResponse {
  uploadUrl: string
  fileUrl: string
  objectKey: string
  expireSeconds: number
}

export interface AccessUrlRequest {
  objectKey: string
}

export interface AccessUrlResponse {
  presignedUrl: string
}

// Gender enum
export const GenderLabels: Record<number, string> = {
  0: '保密',
  1: '男',
  2: '女',
}

// Activity
export interface ActivityPageResponse<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface ActivityListItemResponse {
  id: number
  title: string
  coverImage: string | null
  locationName: string
  startTime: string
  endTime: string | null
  maxParticipants: number
  currentParticipants: number
  status: number
  categoryId: number
  categoryName: string
  tags: string[]
  creator: CreatorInfo
  createdAt: string
}

export interface CreatorInfo {
  id: number
  nickname: string | null
  avatar: string | null
}

export interface ActivityDetailResponse {
  id: number
  title: string
  description: string | null
  coverImage: string | null
  locationName: string
  latitude: number | null
  longitude: number | null
  address: string | null
  startTime: string
  endTime: string | null
  maxParticipants: number
  currentParticipants: number
  status: number
  categoryId: number
  categoryName: string
  images: ImageInfo[]
  tags: string[]
  creator: CreatorInfo
  viewCount: number
  createdAt: string
  updatedAt: string
}

export interface ImageInfo {
  id: number
  url: string
  sortOrder: number
}

export interface CategoryResponse {
  id: number
  name: string
  icon: string | null
  sortOrder: number
}

export interface TagResponse {
  id: number
  name: string
}

export interface CreateActivityRequest {
  title: string
  description?: string
  categoryId: number
  locationName: string
  latitude?: number
  longitude?: number
  address?: string
  startTime: string
  endTime?: string
  maxParticipants?: number
  tagIds?: number[]
  coverImage?: string
}

export interface UpdateActivityRequest {
  title?: string
  description?: string
  categoryId?: number
  locationName?: string
  latitude?: number
  longitude?: number
  address?: string
  startTime?: string
  endTime?: string
  maxParticipants?: number
  tagIds?: number[]
  coverImage?: string
}

export interface ActivityPageQuery {
  categoryId?: number
  status?: number
  keyword?: string
  size?: number
  page?: number
}

export interface AddActivityImageRequest {
  objectKey: string
}

export const ActivityStatusLabels: Record<number, string> = {
  1: '招募中',
  2: '已满员',
  3: '进行中',
  4: '已结束',
  5: '已取消',
}

export const ActivityStatusColors: Record<number, string> = {
  1: '#54746a',
  2: '#8a734a',
  3: '#3f695c',
  4: '#7a7d72',
  5: '#a84f45',
}
