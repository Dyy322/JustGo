import client from './client'
import type {
  ApiResponse,
  FollowStats,
  FollowRelation,
  FollowUserItem,
  CursorPage,
} from '@/types/api'

export function followUser(id: number) {
  return client.post<ApiResponse<null>>(`/users/${id}/follow`)
}

export function unfollowUser(id: number) {
  return client.delete<ApiResponse<null>>(`/users/${id}/follow`)
}

export function getFollowStats(id: number) {
  return client.get<ApiResponse<FollowStats>>(`/users/${id}/follow-stats`)
}

export function getFollowRelation(id: number) {
  return client.get<ApiResponse<FollowRelation>>(`/users/${id}/follow-relation`)
}

export function getFollowers(id: number, cursor?: string, limit = 20) {
  return client.get<ApiResponse<CursorPage<FollowUserItem>>>(
    `/users/${id}/followers`,
    { params: { cursor, limit } },
  )
}

export function getFollowing(id: number, cursor?: string, limit = 20) {
  return client.get<ApiResponse<CursorPage<FollowUserItem>>>(
    `/users/${id}/following`,
    { params: { cursor, limit } },
  )
}
