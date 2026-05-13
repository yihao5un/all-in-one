<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="login-header">
        <h2>Uno HR System</h2>
        <p>Enterprise Resource Management</p>
      </div>
      <el-form :model="loginForm" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input 
            v-model="loginForm.username" 
            placeholder="Username" 
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item>
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="Password" 
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button 
            type="primary" 
            class="login-button" 
            :loading="loading"
            @click="handleLogin"
          >
            Login
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <span>Forgot password?</span>
        <span>Contact IT Support</span>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const loginForm = ref({
  username: 'admin',
  password: '123456'
})

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('Please enter credentials')
    return
  }

  loading.value = true
  try {
    // ⚡️ 联调核心：调用后端的真实登录接口
    const params = new URLSearchParams()
    params.append('username', loginForm.value.username)
    params.append('password', loginForm.value.password)
    
    const response = await request.post('/auth/login', params)
    
    // 兼容网关/后端两种返回方式：响应体 access_token 或响应头 Authorization
    let authHeader = response.data?.data?.access_token || response.headers['authorization']
    if (authHeader?.startsWith('Bearer ')) {
      authHeader = authHeader.substring(7)
    }
    
    if (authHeader) {
      userStore.setToken(authHeader)
      const backendData = response.data?.data || {}
      userStore.setUserInfo({ 
        username: backendData.username || loginForm.value.username, 
        realName: backendData.real_name || loginForm.value.username,
        role: backendData.role || 'EMPLOYEE'
      })
      ElMessage.success('Login successful')
      router.push('/dashboard')
    } else {
      throw new Error('No authorization header found')
    }
  } catch (err: any) {
    // 如果后端没开或者账号不对，这里会捕获到
    console.error('Login failed, if you are just testing UI, you can bypass this', err)
    
    // 备选方案：如果你还没准备好数据库用户，可以暂时手动设置一个 Mock Token 绕过拦截器进行后续页面开发
    /*
    const mockToken = 'mock-jwt-token'
    userStore.setToken(mockToken)
    router.push('/dashboard')
    */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
}

.login-card {
  width: 400px;
  padding: 20px;
  border-radius: 12px;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  margin: 0;
  color: #1e293b;
  font-size: 1.8rem;
}

.login-header p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 0.9rem;
}

.login-button {
  width: 100%;
  height: 44px;
  font-size: 1rem;
}

.login-footer {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
  color: #94a3b8;
  cursor: pointer;
}
</style>
