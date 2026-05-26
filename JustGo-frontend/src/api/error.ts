export function getErrorMessage(err: unknown, fallback: string): string {
  const e = err as { response?: { status?: number; data?: unknown }; message?: string }
  // JSON API 响应
  const jsonMsg = (e?.response?.data as { message?: string } | undefined)?.message
  if (jsonMsg) return jsonMsg
  // OSS XML 错误响应，提取 <Message>...</Message>
  if (typeof e?.response?.data === 'string') {
    const m = e.response.data.match(/<Message>([^<]+)<\/Message>/)
    if (m?.[1]) return m[1]
  }
  if (e?.response?.status) return `请求失败 (${e.response.status})，${fallback}`
  if (e?.message === 'Network Error') return '网络连接失败，请检查 OSS CORS 跨域配置'
  return fallback
}
