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
    next()
  }
})

export default router
