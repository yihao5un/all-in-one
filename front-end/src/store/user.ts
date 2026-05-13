import { defineStore } from 'pinia'
import request from '@/api/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
  }),
  
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.userInfo?.role === 'ADMIN',
    role: (state) => state.userInfo?.role || 'EMPLOYEE',
  },
  
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('token', token)
    },
    
    setUserInfo(info: any) {
      this.userInfo = info
      localStorage.setItem('userInfo', JSON.stringify(info))
    },
    
    async logout() {
      try {
        await request.post('/auth/logout')
      } catch (e) {
        console.error('Backend logout failed, continuing with local logout', e)
      }
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
