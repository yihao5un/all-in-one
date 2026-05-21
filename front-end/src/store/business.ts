import { defineStore } from 'pinia'
import request from '@/api/request'

export const useBusinessStore = defineStore('business', {
  state: () => ({
    orders: [] as any[],
    products: [] as any[],
    users: [] as any[],
    bills: [] as any[],
    loading: false,
    error: null as string | null,
  }),
  
  actions: {
    async fetchOrders(page = 1, limit = 20, queryParams: any = {}) {
      this.loading = true
      try {
        const hasFilter = queryParams.keyword || queryParams.status
        const url = hasFilter ? '/order/search' : '/order/list'
        const response = await request.get(url, {
          params: { page, limit, ...queryParams }
        })
        // Support both list and Page result
        const resultData = response.data.data
        if (resultData?.records) {
          this.orders = resultData.records
          return resultData.total
        } else {
          this.orders = resultData || []
          return this.orders.length
        }
      } catch (err: any) {
        this.error = err.message
        return 0
      } finally {
        this.loading = false
      }
    },

    async deleteOrder(id: number | string) {
      await request.delete(`/order/${id}`)
      await this.fetchOrders()
    },

    async updateOrder(order: any) {
      await request.put('/order/update', order)
      await this.fetchOrders()
    },
    
    async createOrder(orderData: any) {
      this.loading = true
      try {
        if (!orderData.employeeId || !orderData.products || orderData.products.length === 0) {
          throw new Error('Employee and products are required')
        }
        const response = await request.post('/order/onboard', {
          employeeId: orderData.employeeId,
          products: orderData.products
        })
        await Promise.all([
          this.fetchOrders(),
          this.fetchProducts(),
          this.fetchBills()
        ])
        return response.data
      } catch (err: any) {
        this.error = err.message
        throw err
      } finally {
        this.loading = false
      }
    },

    async triggerOutboxPublish(eventType?: string) {
      this.loading = true
      try {
        await request.post(`/order/outbox/publish?eventType=${eventType || ''}`)
        await this.fetchOrders()
      } catch (err: any) {
        this.error = err.message
        throw err
      } finally {
        this.loading = false
      }
    },

    async fetchProducts() {
      this.loading = true
      try {
        const response = await request.get('/product/list')
        this.products = response.data.data || []
      } catch (err: any) {
        this.error = err.message
      } finally {
        this.loading = false
      }
    },

    async createProduct(product: any) {
      await request.post('/product/add', product)
      await this.fetchProducts()
    },

    async updateProduct(product: any) {
      await request.put('/product/update', product)
      await this.fetchProducts()
    },

    async deleteProduct(id: number | string) {
      await request.delete(`/product/${id}`)
      await this.fetchProducts()
    },

    async fetchUsers(keyword = '') {
      this.loading = true
      try {
        const response = await request.get('/auth/users', {
          params: keyword ? { keyword } : {}
        })
        this.users = response.data.data || []
      } catch (err: any) {
        this.error = err.message
      } finally {
        this.loading = false
      }
    },

    async createUser(userData: any) {
      await request.post('/auth/users', userData)
      await this.fetchUsers()
    },

    async deleteUser(id: number | string) {
      await request.delete(`/auth/users/${id}`)
      await this.fetchUsers()
    },

    async updateUserStatus(id: number, status: number) {
      const params = new URLSearchParams()
      params.append('status', String(status))
      await request.post(`/auth/users/${id}/status`, params)
      await this.fetchUsers()
    },

    async fetchBills() {
      this.loading = true
      try {
        const response = await request.get('/settlement/list')
        this.bills = response.data.data || []
      } catch (err: any) {
        this.error = err.message
      } finally {
        this.loading = false
      }
    },

    async deleteBill(id: number | string) {
      await request.delete(`/settlement/${id}`)
      await this.fetchBills()
    },

    async rebuildBill(orderNo: string) {
      await request.post(`/settlement/orders/${orderNo}/rebuild-bill`)
      await this.fetchBills()
    },

    async payBill(billNo: string) {
      await request.post(`/settlement/${billNo}/pay`)
      await Promise.all([
        this.fetchBills(),
        this.fetchOrders()
      ])
    }
  }
})
