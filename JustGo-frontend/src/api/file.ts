import client from './client'
import type { ApiResponse, AccessUrlRequest, AccessUrlResponse, UploadTokenRequest, UploadTokenResponse } from '@/types/api'

export function getUploadToken(data: UploadTokenRequest) {
  return client.post<ApiResponse<UploadTokenResponse>>('/files/upload-token', data)
}

export function getAccessUrl(data: AccessUrlRequest) {
  return client.post<ApiResponse<AccessUrlResponse>>('/files/access-url', data)
}
