import client from './client'
import type {
  ApiResponse,
  UserProfile,
  UserPublicProfile,
  UpdateProfileRequest,
  ChangePasswordRequest,
} from '@/types/api'

export function getMyProfile() {
  return client.get<ApiResponse<UserProfile>>('/users/me')
}

export function updateProfile(data: UpdateProfileRequest) {
  return client.patch<ApiResponse<UserProfile>>('/users/me', data)
}

export function changePassword(data: ChangePasswordRequest) {
  return client.put<ApiResponse<null>>('/users/me/password', data)
}

export function getUserPublic(id: number) {
  return client.get<ApiResponse<UserPublicProfile>>(`/users/${id}`)
}
