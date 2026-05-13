<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="user-card" shadow="hover">
          <div class="user-profile">
            <el-avatar :size="100" class="avatar">{{ userInfo?.realName?.charAt(0) }}</el-avatar>
            <h3 class="name">{{ userInfo?.realName }}</h3>
            <p class="role">{{ userInfo?.role }}</p>
            <el-divider />
            <div class="user-info">
              <div class="info-item">
                <el-icon><User /></el-icon>
                <span>{{ userInfo?.username }}</span>
              </div>
              <div class="info-item">
                <el-icon><Calendar /></el-icon>
                <span>Joined May 2026</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>{{ $t('common.profile') }}</span>
            </div>
          </template>
          <el-form :model="profileForm" label-width="120px" label-position="left">
            <el-form-item label="Username">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            <el-form-item label="Real Name">
              <el-input v-model="profileForm.realName" />
            </el-form-item>
            <el-form-item label="Email">
              <el-input v-model="profileForm.email" placeholder="Optional" />
            </el-form-item>
            <el-form-item label="Phone">
              <el-input v-model="profileForm.phone" placeholder="Optional" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdate">Update Profile</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="mt-20" shadow="never">
          <template #header>Security Settings</template>
          <el-form label-width="120px" label-position="left">
            <el-form-item label="New Password">
              <el-input type="password" placeholder="Leave blank to keep current" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="warning">Change Password</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { User, Calendar } from '@element-plus/icons-vue'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const profileForm = ref({
  username: userInfo.value?.username || '',
  realName: userInfo.value?.realName || '',
  email: '',
  phone: ''
})

const handleUpdate = () => {
  ElMessage.success('Profile updated successfully (Simulated)')
}
</script>

<style scoped>
.profile-container {
  padding: 10px;
}
.user-card {
  text-align: center;
}
.user-profile {
  padding: 20px 0;
}
.avatar {
  background-color: #4f46e5;
  font-size: 2.5rem;
  margin-bottom: 15px;
}
.name {
  margin: 10px 0 5px;
  font-size: 1.5rem;
  color: #1e293b;
}
.role {
  margin: 0;
  color: #64748b;
  font-weight: 500;
}
.user-info {
  text-align: left;
  margin-top: 20px;
}
.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  color: #475569;
}
.mt-20 {
  margin-top: 20px;
}
.card-header {
  font-weight: bold;
}
</style>
