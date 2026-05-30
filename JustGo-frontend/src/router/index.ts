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
      path: '/activities/create',
      name: 'ActivityCreate',
      component: () => import('@/views/ActivityCreateView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/activities/:id',
      name: 'ActivityDetail',
      component: () => import('@/views/ActivityDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/activities/:id/edit',
      name: 'ActivityEdit',
      component: () => import('@/views/ActivityEditView.vue'),
      meta: { requiresAuth: true },
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
