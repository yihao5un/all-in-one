<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6" v-for="card in statCards" :key="card.title">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <span>{{ card.title }}</span>
              <el-tag :type="card.tagType">{{ card.tag }}</el-tag>
            </div>
          </template>
          <div class="card-body">
            <h2 class="value">{{ card.value }}</h2>
            <p class="desc">{{ card.desc }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-20">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>{{ $t('dashboard.recentOrders') }}</template>
          <el-table :data="recentOrders" style="width: 100%" v-loading="loading">
            <el-table-column prop="createTime" :label="$t('order.createTime')" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="orderType" :label="$t('order.type')" width="120">
              <template #default="{ row }">
                <el-tag>{{ $t(`order.${row.orderType.toLowerCase()}`) || row.orderType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="employeeId" :label="$t('order.employeeId')" />
            <el-table-column prop="status" :label="$t('order.status')">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)">
                  {{ $t(`order.${row.status.toLowerCase()}`) || row.status }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useBusinessStore } from '@/store/business'
import { useI18n } from 'vue-i18n'
import { formatDate } from '@/utils/format'

const { t } = useI18n()
const businessStore = useBusinessStore()
const loading = computed(() => businessStore.loading)

const pendingBills = computed(() => businessStore.bills.filter((bill: any) => bill.status === 'PENDING'))
const activeUsers = computed(() => businessStore.users.filter((user: any) => user.status === 1))
const recentOrders = computed(() => businessStore.orders.slice(0, 5))

const statCards = computed(() => [
  {
    title: t('dashboard.totalOrders'),
    value: String(businessStore.orders.length),
    tag: t('dashboard.live'),
    tagType: 'success',
    desc: t('dashboard.totalOrdersDesc'),
  },
  {
    title: t('dashboard.activeEmployees'),
    value: String(activeUsers.value.length),
    tag: t('dashboard.totalCount', { count: businessStore.users.length }),
    tagType: 'info',
    desc: t('dashboard.activeEmployeesDesc'),
  },
  {
    title: t('dashboard.pendingSettlements'),
    value: String(pendingBills.value.length),
    tag: pendingBills.value.length > 0 ? t('dashboard.actionRequired') : t('dashboard.clear'),
    tagType: pendingBills.value.length > 0 ? 'danger' : 'success',
    desc: t('dashboard.pendingSettlementsDesc'),
  },
  {
    title: t('dashboard.productQuotaUsed'),
    value: String(businessStore.products.reduce((sum: number, product: any) => sum + product.usedQuota, 0)),
    tag: t('dashboard.productCount', { count: businessStore.products.length }),
    tagType: 'warning',
    desc: t('dashboard.productQuotaUsedDesc'),
  },
])

const insight = computed(() => {
  if (pendingBills.value.length > 0) {
    return {
      title: t('dashboard.settlementAttention'),
      description: t('dashboard.settlementAttentionDesc', { count: pendingBills.value.length }),
    }
  }
  return {
    title: t('dashboard.systemHealthy'),
    description: t('dashboard.systemHealthyDesc'),
  }
})

const statusType = (status: string) => {
  switch (status) {
    case 'CLOSED':
    case 'SETTLED':
      return 'success'
    case 'PROCESSING':
      return 'primary'
    case 'CREATED':
      return 'warning'
    default:
      return 'info'
  }
}

onMounted(() => {
  businessStore.fetchOrders()
  businessStore.fetchProducts()
  businessStore.fetchUsers()
  businessStore.fetchBills()
})
</script>

<style scoped>
.mt-20 { margin-top: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-body .value { font-size: 2rem; margin: 0; color: #1e293b; }
.card-body .desc { font-size: 0.875rem; color: #64748b; margin: 4px 0 0; }
.ai-insight { padding: 10px 0; }
</style>
