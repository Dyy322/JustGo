import client from './client'
import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  UserProfile,
} from '@/types/api'

export function login(data: LoginRequest) {
  return client.post<ApiResponse<LoginResponse>>('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return client.post<ApiResponse<UserProfile>>('/auth/register', data)
}

export function refresh() {
  return client.post<ApiResponse<LoginResponse>>('/auth/refresh', {})
}

export function logout() {
  return client.post<ApiResponse<null>>('/auth/logout')
}

export function getMe() {
  return client.get<ApiResponse<UserProfile>>('/auth/me')
}
