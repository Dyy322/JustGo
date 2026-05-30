import client from './client'
import type { ApiResponse, CategoryResponse, TagResponse } from '@/types/api'

export function listCategories() {
  return client.get<ApiResponse<CategoryResponse[]>>('/categories')
}

export function listTags() {
  return client.get<ApiResponse<TagResponse[]>>('/tags')
}
