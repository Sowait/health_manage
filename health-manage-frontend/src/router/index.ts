import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    redirect: '/dashboard',
    component: () => import('../layout/Layout.vue'),
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue')
      },
      {
        path: 'health-data',
        name: 'HealthData',
        component: () => import('../views/HealthData.vue')
      },
      {
        path: 'check-in',
        name: 'CheckIn',
        component: () => import('../views/CheckIn.vue')
      },
      {
        path: 'diet',
        name: 'Diet',
        component: () => import('../views/Diet.vue')
      },
      {
        path: 'admin',
        name: 'AdminDashboard',
        component: () => import('../views/admin/AdminDashboard.vue')
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('../views/admin/AdminUsers.vue')
      },
      {
        path: 'admin/templates',
        name: 'AdminTemplates',
        component: () => import('../views/admin/AdminTemplates.vue')
      }
      ,{
        path: 'admin/health-records',
        name: 'AdminHealthRecords',
        component: () => import('../views/admin/AdminHealthRecords.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const user = localStorage.getItem('user')
  if (to.path !== '/login' && !user) {
    next('/login')
  } else {
    const u = user ? JSON.parse(user) : {}
    if (to.path.startsWith('/admin') && u.role !== 'ADMIN') {
      next('/dashboard')
    } else {
      next()
    }
  }
})

export default router
