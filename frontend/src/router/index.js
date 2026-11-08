import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import EventListView from '../views/EventListView.vue'
import EventDetailView from '../views/EventDetailView.vue'
import MyRegistrationsView from '../views/MyRegistrationsView.vue'
import AdminEventListView from '../views/AdminEventListView.vue'
import AdminEventDetailView from '../views/AdminEventDetailView.vue'
import CheckInView from '../views/CheckInView.vue'

const routes = [
  { path: '/', redirect: '/events' },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { guestOnly: true, title: '登录' },
  },
  {
    path: '/events',
    name: 'events',
    component: EventListView,
    meta: { requiresAuth: true, studentOnly: true, title: '活动广场' },
  },
  {
    path: '/events/:id',
    name: 'event-detail',
    component: EventDetailView,
    meta: { requiresAuth: true, studentOnly: true, title: '活动详情' },
  },
  {
    path: '/me/registrations',
    name: 'my-registrations',
    component: MyRegistrationsView,
    meta: { requiresAuth: true, studentOnly: true, title: '我的票夹' },
  },
  { path: '/admin', redirect: '/admin/events' },
  {
    path: '/admin/events',
    name: 'admin-events',
    component: AdminEventListView,
    meta: { requiresAuth: true, admin: true, title: '活动管理' },
  },
  {
    path: '/admin/events/:id',
    name: 'admin-event-detail',
    component: AdminEventDetailView,
    meta: { requiresAuth: true, admin: true, title: '活动详情' },
  },
  {
    path: '/check-in',
    name: 'check-in',
    component: CheckInView,
    meta: { requiresAuth: true, studentOnly: true, title: '现场签到' },
  },
  { path: '/:pathMatch(.*)*', redirect: '/events' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (auth.token && !auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      auth.logout()
    }
  }

  if (to.meta.requiresAuth && !auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && auth.token) {
    return auth.isAdmin ? '/admin/events' : '/events'
  }
  if (to.meta.studentOnly && auth.isAdmin) {
    return '/admin/events'
  }
  if (to.meta.admin && !auth.isAdmin) {
    return '/events'
  }
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · CampusFlow` : 'CampusFlow · 青禾活动'
})

export default router
