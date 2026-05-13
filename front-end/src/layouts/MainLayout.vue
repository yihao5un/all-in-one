<template>
  <el-container class="layout-container">
    <el-aside width="240px" class="aside">
      <div class="logo">
        <span>{{ $t('common.appName') }}</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical"
        router
        background-color="#1e293b"
        text-color="#94a3b8"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Menu /></el-icon>
          <span>{{ $t('common.dashboard') }}</span>
        </el-menu-item>
        <el-menu-item index="/order">
          <el-icon><Document /></el-icon>
          <span>{{ $t('common.orderMgmt') }}</span>
        </el-menu-item>
        <el-menu-item index="/product">
          <el-icon><Box /></el-icon>
          <span>{{ $t('common.productMgmt') }}</span>
        </el-menu-item>
        <el-menu-item index="/settlement">
          <el-icon><Money /></el-icon>
          <span>{{ $t('common.settlement') }}</span>
        </el-menu-item>
        <el-menu-item index="/employee" v-if="userStore.isAdmin">
          <el-icon><User /></el-icon>
          <span>{{ $t('common.employee') }}</span>
        </el-menu-item>
        <el-menu-item index="/ai-chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>{{ $t('common.aiChat') }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">{{ $t('common.home') }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageLabel }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="changeLang">
            <span class="lang-switch">
              {{ currentLang === 'zh' ? '中文' : 'English' }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="zh">中文</el-dropdown-item>
                <el-dropdown-item command="en">English</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown @command="handleUserCommand">
            <span class="user-info">
              {{ userStore.userInfo?.realName || $t('common.admin') }} <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">{{ $t('common.profile') }}</el-dropdown-item>
                <el-dropdown-item divided command="logout">{{ $t('common.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { locale, t } = useI18n()

const currentLang = computed(() => locale.value)
const activeMenu = computed(() => route.path)
const currentPageLabel = computed(() => {
  const name = route.name as string
  const pageMap: Record<string, string> = {
    dashboard: 'common.dashboard',
    order: 'common.orderMgmt',
    product: 'common.productMgmt',
    settlement: 'common.settlement',
    employee: 'common.employee',
    aiChat: 'common.aiChat',
    profile: 'common.profile'
  }
  return pageMap[name] ? t(pageMap[name]) : name
})

const changeLang = (lang: string) => {
  locale.value = lang
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const handleUserCommand = (command: string) => {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.aside { background-color: #1e293b; color: white; }
.logo { height: 64px; display: flex; align-items: center; justify-content: center; font-size: 1.2rem; font-weight: bold; border-bottom: 1px solid #334155; }
.el-menu-vertical { border-right: none; }
.header { background-color: white; border-bottom: 1px solid #e2e8f0; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; }
.header-right { display: flex; align-items: center; gap: 20px; }
.lang-switch, .user-info { cursor: pointer; display: flex; align-items: center; color: #64748b; font-size: 0.9rem; }
.main { background-color: #f8fafc; padding: 24px; }
.fade-transform-enter-active, .fade-transform-leave-active { transition: all 0.3s; }
.fade-transform-enter-from { opacity: 0; transform: translateX(-30px); }
.fade-transform-leave-to { opacity: 0; transform: translateX(30px); }
</style>
