import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue'),
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../views/Dashboard.vue'),
      },
      {
        path: 'order',
        name: 'order',
        component: () => import('../views/order/OrderList.vue'),
      },
      {
        path: 'product',
        name: 'product',
        component: () => import('../views/product/ProductList.vue'),
      },
      {
        path: 'settlement',
        name: 'settlement',
        component: () => import('../views/settlement/SettlementList.vue'),
      },
      {
        path: 'employee',
        name: 'employee',
        component: () => import('../views/employee/EmployeeList.vue'),
      },
      {
        path: 'ai-chat',
        name: 'ai-chat',
        component: () => import('../views/ai-chat/ChatWindow.vue'),
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('../views/Profile.vue'),
      },
    ],
  },
]

import { useUserStore } from '../store/user'

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.name !== 'login' && !userStore.isLoggedIn) {
    next({ name: 'login' })
  } else {
    next()
  }
})

export default router
