import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guest: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { guest: true },
    },
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/HomeView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/settings',
      name: 'Settings',
      component: () => import('@/views/SettingsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile/:id',
      name: 'Profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile/:id/followers',
      name: 'Followers',
      component: () => import('@/views/FollowListView.vue'),
      meta: { requiresAuth: true },
      props: { type: 'followers' as const },
    },
    {
      path: '/profile/:id/following',
      name: 'Following',
      component: () => import('@/views/FollowListView.vue'),
      meta: { requiresAuth: true },
      props: { type: 'following' as const },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach((to, _from) => {
  const auth = useAuthStore()

  if (to.meta.guest && auth.initialized && auth.isLoggedIn) {
    return '/'
  }

  if (to.meta.requiresAuth && auth.initialized && !auth.isLoggedIn) {
    return '/login'
  }
})

export default router
