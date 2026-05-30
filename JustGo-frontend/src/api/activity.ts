import client from './client'
import type {
  ApiResponse,
  ActivityPageResponse,
  ActivityListItemResponse,
  ActivityDetailResponse,
  CreateActivityRequest,
  UpdateActivityRequest,
  ActivityPageQuery,
  AddActivityImageRequest,
  ImageInfo,
} from '@/types/api'

export function listActivities(params: ActivityPageQuery) {
  return client.get<ApiResponse<ActivityPageResponse<ActivityListItemResponse>>>('/activities', {
    params,
  })
}

export function getActivityDetail(id: number) {
  return client.get<ApiResponse<ActivityDetailResponse>>(`/activities/${id}`)
}

export function createActivity(data: CreateActivityRequest) {
  return client.post<ApiResponse<ActivityDetailResponse>>('/activities', data)
}

export function updateActivity(id: number, data: UpdateActivityRequest) {
  return client.put<ApiResponse<ActivityDetailResponse>>(`/activities/${id}`, data)
}

export function cancelActivity(id: number) {
  return client.patch<ApiResponse<null>>(`/activities/${id}/cancel`)
}

export function republishActivity(id: number) {
  return client.patch<ApiResponse<null>>(`/activities/${id}/republish`)
}

export function listActivityImages(id: number) {
  return client.get<ApiResponse<ImageInfo[]>>(`/activities/${id}/images`)
}

export function addActivityImages(id: number, data: AddActivityImageRequest[]) {
  return client.post<ApiResponse<ImageInfo[]>>(`/activities/${id}/images`, data)
}

export function deleteActivityImage(activityId: number, imageId: number) {
  return client.delete<ApiResponse<null>>(`/activities/${activityId}/images/${imageId}`)
}

export function joinActivity(id: number) {
  return client.post<ApiResponse<null>>(`/activities/${id}/join`)
}

export function leaveActivity(id: number) {
  return client.delete<ApiResponse<null>>(`/activities/${id}/join`)
}

export function isJoinedActivity(id: number) {
  return client.get<ApiResponse<boolean>>(`/activities/${id}/joined`)
}

export function listMyActivities(type: 'created' | 'joined', page = 1, size = 10) {
  return client.get<ApiResponse<ActivityPageResponse<ActivityListItemResponse>>>(
    '/users/me/activities',
    { params: { type, page, size } },
  )
}
