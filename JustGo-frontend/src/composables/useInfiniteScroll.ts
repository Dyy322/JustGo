import { ref, onMounted, onUnmounted, type Ref } from 'vue'

export function useInfiniteScroll(
  loadMore: () => Promise<void>,
  options?: { enabled?: Ref<boolean> },
) {
  const sentinelRef = ref<HTMLElement | null>(null)
  let observer: IntersectionObserver | null = null

  onMounted(() => {
    if (!sentinelRef.value) return

    observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0]
        if (!entry) return
        if (entry.isIntersecting) {
          if (options?.enabled && !options.enabled.value) return
          loadMore()
        }
      },
      { threshold: 0.1 },
    )

    observer.observe(sentinelRef.value)
  })

  onUnmounted(() => {
    if (observer) {
      observer.disconnect()
      observer = null
    }
  })

  return { sentinelRef }
}
