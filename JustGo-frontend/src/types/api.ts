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
