<template>
  <div class="order-management">
    <el-card shadow="never">
      <template #header>
        <div class="header-actions">
          <h3>{{ $t('order.title') }}</h3>
          <div class="header-right">
            <el-button :icon="Refresh" :loading="loading" @click="refreshPage">{{ $t('common.refresh') }}</el-button>
            <el-button type="primary" @click="openCreateDialog">{{ $t('order.create') }}</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索过滤栏 -->
      <div class="filter-bar">
        <el-form :inline="true" :model="queryParams" class="demo-form-inline">
          <el-form-item :label="$t('order.searchKeywordLabel')">
            <el-input
              v-model="queryParams.keyword"
              :placeholder="$t('order.searchKeywordPlaceholder')"
              clearable
              style="width: 280px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item :label="$t('order.status')">
            <el-select
              v-model="queryParams.status"
              :placeholder="$t('order.selectStatusPlaceholder')"
              clearable
              style="width: 160px"
              @change="handleSearch"
            >
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">{{ $t('common.search') }}</el-button>
            <el-button @click="resetQuery">{{ $t('common.reset') }}</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="orders" style="width: 100%" v-loading="loading">
        <el-table-column prop="orderNo" :label="$t('order.orderNo')" width="180" />
        <el-table-column prop="orderType" :label="$t('order.type')">
          <template #default="{ row }">
            <el-tag :type="typeStyle(row.orderType)">{{ $t(`order.${row.orderType.toLowerCase()}`) || row.orderType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('order.employee')" min-width="140">
          <template #default="{ row }">
            <!-- ES doc has employeeName directly; MySQL DTO needs lookup by employeeId -->
            <span v-if="row.employeeName">{{ row.employeeName }}</span>
            <span v-else>{{ employeeName(row.employeeId) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('order.product')" min-width="180">
          <template #default="{ row }">
            <!-- ES doc has productNames as a string; MySQL DTO has productIds array -->
            <span v-if="row.productNames">{{ row.productNames }}</span>
            <span v-else-if="row.productIds && row.productIds.length > 0">
              {{ row.productIds.map((id: any) => productName(id)).join(', ') }}
            </span>
            <span v-else>{{ productName(row.productId) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('order.status')" min-width="150">
          <template #default="{ row }">
            <el-tag class="status-tag" :type="statusStyle(row.status)" effect="dark">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="thirdSyncStatus" :label="$t('order.thirdSyncStatus')" min-width="130">
          <template #default="{ row }">
            <el-tag class="status-tag" :type="thirdSyncStyle(row.thirdSyncStatus)">{{ thirdSyncLabel(row.thirdSyncStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('order.createTime')" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('order.actions')" width="260">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetails(row)">{{ $t('order.details') }}</el-button>
            <el-button link type="warning" v-if="row.status === 'WAIT_EXTERNAL_SYNC'" :loading="processingIds.includes(row.id)" @click="triggerSync(row, 'EXTERNAL_SYNC_REQUESTED')">同步三方</el-button>
            <el-button link type="success" v-if="row.status === 'PENDING_PAYMENT' && !row.billSyncSent" :loading="processingIds.includes(row.id)" @click="triggerSync(row, 'SETTLEMENT_CREATED')">同步账单</el-button>
            <el-tag v-if="row.status === 'PENDING_PAYMENT' && row.billSyncSent" type="info" size="small" style="margin-left: 5px;">账单已生成</el-tag>
            <el-button link type="danger" v-if="row.status === 'PENDING'" @click="handleCancel(row)">{{ $t('order.cancel') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination 
          background 
          layout="prev, pager, next, sizes, total" 
          :total="total" 
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- Create Order Dialog -->
    <el-dialog v-model="createDialogVisible" :title="$t('order.create')" width="500px">
      <el-form :model="newOrder" label-width="120px">
        <el-form-item :label="$t('order.type')">
          <el-select v-model="newOrder.type" style="width: 100%" disabled>
            <el-option :label="$t('order.onboarding')" value="ONBOARD" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('order.employeeId')">
          <el-select
            v-model="newOrder.employeeId"
            filterable
            remote
            reserve-keyword
            :placeholder="$t('order.searchEmployeePlaceholder')"
            :remote-method="searchEmployees"
            :loading="userLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in employeeOptions"
              :key="item.id"
              :label="`${item.name} (ID: ${item.id})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('order.product')">
          <el-select
            v-model="newOrder.productIds"
            multiple
            :placeholder="$t('order.selectProductPlaceholder')"
            style="width: 100%"
          >
            <el-option
              v-for="product in availableProducts"
              :key="product.id"
              :label="`${product.productName} / ${$t('product.remaining')}: ${product.totalQuota - product.usedQuota}`"
              :value="product.id"
              :disabled="product.status !== 1 || product.totalQuota <= product.usedQuota"
            />
          </el-select>
          <!-- 数量选择器 -->
          <div v-if="newOrder.productIds && newOrder.productIds.length > 0" style="margin-top: 10px;">
            <div v-for="id in newOrder.productIds" :key="id" style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 5px;">
              <span style="font-size: 14px; color: #606266;">{{ productName(id) }}</span>
              <el-input-number v-model="productCounts[id]" :min="1" size="small" />
            </div>
          </div>
        </el-form-item>
        <el-form-item :label="$t('order.notes')">
          <el-input v-model="newOrder.notes" type="textarea" :placeholder="$t('order.notes')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="createDialogVisible = false">{{ $t('common.cancel') }}</el-button>
          <el-button type="primary" :loading="loading" @click="handleCreate">{{ $t('common.confirm') }}</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Order Detail Dialog -->
    <el-dialog v-model="detailVisible" :title="$t('order.details')" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="$t('order.orderNo')">{{ selectedOrder?.orderNo }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.type')">{{ selectedOrder?.orderType ? $t(`order.${selectedOrder.orderType.toLowerCase()}`) : '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.employee')">
          <span v-if="selectedOrder?.employeeName">{{ selectedOrder?.employeeName }}</span>
          <span v-else>{{ employeeName(selectedOrder?.employeeId) }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('order.product')">
          <span v-if="selectedOrder?.productNames">{{ selectedOrder?.productNames }}</span>
          <span v-else-if="selectedOrder?.productIds && selectedOrder?.productIds.length > 0">
            {{ selectedOrder?.productIds.map((id: any) => productName(id)).join(', ') }}
          </span>
          <span v-else>{{ productName(selectedOrder?.productId) }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('order.status')">
          <el-tag class="status-tag" :type="statusStyle(selectedOrder?.status)">{{ statusLabel(selectedOrder?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('order.thirdSyncStatus')">
          <el-tag class="status-tag" :type="thirdSyncStyle(selectedOrder?.thirdSyncStatus)">{{ thirdSyncLabel(selectedOrder?.thirdSyncStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('order.createTime')">{{ formatDate(selectedOrder?.createTime) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.thirdSyncMsg')" :span="2">{{ selectedOrder?.thirdSyncMsg || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBusinessStore } from '@/store/business'
import { useI18n } from 'vue-i18n'
import { formatDate } from '@/utils/format'
import { Refresh } from '@element-plus/icons-vue'

const { t } = useI18n()
const businessStore = useBusinessStore()
const loading = computed(() => businessStore.loading)
const userLoading = ref(false)
const employeeOptions = ref<any[]>([])
const createDialogVisible = ref(false)
const detailVisible = ref(false)
const selectedOrder = ref<any>(null)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

const queryParams = ref({
  keyword: '',
  status: ''
})

const statusOptions = computed(() => [
  { value: 'CREATED', label: t('order.created') },
  { value: 'PROCESSING', label: t('order.processing') },
  { value: 'WAIT_EXTERNAL_SYNC', label: t('order.wait_external_sync') },
  { value: 'SYNC_FAILED', label: t('order.sync_failed') },
  { value: 'PENDING_PAYMENT', label: t('order.pending_payment') },
  { value: 'SETTLED', label: t('order.settled') },
  { value: 'CLOSED', label: t('order.closed') }
])

const handleSearch = () => {
  currentPage.value = 1
  loadOrders()
}

const resetQuery = () => {
  queryParams.value.keyword = ''
  queryParams.value.status = ''
  handleSearch()
}

const searchEmployees = async (query: string) => {
  if (query) {
    userLoading.value = true
    try {
      await businessStore.fetchUsers(query)
      employeeOptions.value = businessStore.users.map((u: any) => ({
        id: u.id,
        name: u.realName || u.username
      }))
    } finally {
      userLoading.value = false
    }
  } else {
    employeeOptions.value = []
  }
}

const orders = computed(() => businessStore.orders)
const availableProducts = computed(() => businessStore.products)

const productName = (productId: number | string) => {
  const product = businessStore.products.find((item: any) => String(item.id) === String(productId))
  return product?.productName || '-'
}

const employeeName = (employeeId: number | string) => {
  const employee = businessStore.users.find((item: any) => String(item.id) === String(employeeId))
  return employee?.realName || employee?.username || (employeeId ? `ID: ${employeeId}` : '-')
}

const newOrder = ref({
  type: 'ONBOARD',
  employeeId: '',
  productIds: [],
  notes: ''
})

const productCounts = ref<Record<string, number>>({})

watch(() => newOrder.value.productIds, (newIds) => {
  newIds.forEach(id => {
    if (productCounts.value[id] === undefined) {
      productCounts.value[id] = 1
    }
  })
}, { deep: true })

const typeStyle = (type: string) => {
  switch (type) {
    case 'ONBOARD': return 'success'
    case 'TRANSFER': return 'warning'
    case 'RESIGNATION': return 'danger'
    default: return ''
  }
}

const statusStyle = (status: string) => {
  switch (status) {
    case 'Completed': case 'COMPLETED': return 'success'
    case 'SETTLED': case 'CLOSED': return 'success'
    case 'PENDING_PAYMENT': return 'warning'
    case 'WAIT_EXTERNAL_SYNC': return 'primary'
    case 'SYNC_FAILED': return 'danger'
    case 'Processing': case 'PROCESSING': return 'primary'
    case 'CREATED': return 'warning'
    case 'Pending': case 'PENDING': return 'info'
    default: return ''
  }
}

const statusLabel = (status: string) => {
  if (!status) return '-'
  const key = status.toLowerCase()
  return t(`order.${key}`) || status
}

const thirdSyncStyle = (status: string) => {
  switch (status) {
    case 'SUCCESS': return 'success'
    case 'FAILED': return 'danger'
    case 'SYNCING': return 'primary'
    case 'NOT_SYNCED': return 'info'
    default: return 'info'
  }
}

const thirdSyncLabel = (status: string) => {
  if (!status) return '-'
  return t(`order.third_${status.toLowerCase()}`) || status
}

const openCreateDialog = () => {
  newOrder.value = {
    type: 'ONBOARD',
    employeeId: '',
    productIds: [],
    notes: ''
  }
  if (businessStore.products.length === 0) {
    businessStore.fetchProducts()
  }
  createDialogVisible.value = true
}

const handleCreate = async () => {
  try {
    if (!newOrder.value.employeeId || !newOrder.value.productIds || newOrder.value.productIds.length === 0) {
      ElMessage.warning(t('order.createValidation'))
      return
    }
    const payload = {
      employeeId: newOrder.value.employeeId,
      products: newOrder.value.productIds.map(id => ({
        productId: id,
        count: productCounts.value[id] || 1
      }))
    }
    const result = await businessStore.createOrder(payload)
    const orderNo = result?.data?.orderNo
    ElMessage.success(orderNo ? `${t('common.success')}: ${orderNo}` : t('common.success'))
    createDialogVisible.value = false
    loadOrders()
  } catch (err: any) {
    ElMessage.error(err?.message || t('common.failed'))
  }
}

const loadOrders = async () => {
  total.value = await businessStore.fetchOrders(currentPage.value, pageSize.value, queryParams.value)
}

const refreshPage = async () => {
  await Promise.all([
    loadOrders(),
    businessStore.fetchProducts(),
    businessStore.fetchUsers(),
    businessStore.fetchBills()
  ])
}

const handlePageChange = (val: number) => {
  currentPage.value = val
  loadOrders()
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  loadOrders()
}

const viewDetails = (row: any) => {
  selectedOrder.value = row
  detailVisible.value = true
}

const processingIds = ref<number[]>([])

const triggerSync = async (row: any, eventType: string) => {
  try {
    processingIds.value.push(row.id)
    await businessStore.triggerOutboxPublish(eventType)
    ElMessage.success('请求已发送，正在后台处理...')
    
    // 开始轮询
    startPolling(row.id, row.status)
  } catch (err: any) {
    ElMessage.error(err?.message || '同步失败')
    processingIds.value = processingIds.value.filter(id => id !== row.id)
  }
}

const startPolling = (rowId: number, oldStatus: string) => {
  const timer = setInterval(async () => {
    await loadOrders() // 刷新列表
    
    const updatedRow = orders.value.find((item: any) => item.id === rowId)
    if (!updatedRow) {
      clearInterval(timer)
      processingIds.value = processingIds.value.filter(id => id !== rowId)
      return
    }
    
    const statusChanged = updatedRow.status !== oldStatus
    const billSent = updatedRow.billSyncSent
    
    if (statusChanged || (oldStatus === 'PENDING_PAYMENT' && billSent)) {
      clearInterval(timer)
      processingIds.value = processingIds.value.filter(id => id !== rowId)
      ElMessage.success('同步完成')
    }
  }, 1000)
  
  // 10秒超时
  setTimeout(() => {
    clearInterval(timer)
    processingIds.value = processingIds.value.filter(id => id !== rowId)
  }, 10000)
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(t('order.deleteConfirm'), t('common.delete'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
    await businessStore.deleteOrder(row.id)
    ElMessage.success(t('common.success'))
    loadOrders()
  } catch (e) {}
}

const handleCancel = (row: any) => {
  ElMessage.info('Cancel logic to be implemented')
}

onMounted(refreshPage)
</script>

<style scoped>
.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions h3 { margin: 0; }
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.status-tag {
  display: inline-flex;
  width: max-content;
  max-width: 100%;
  min-width: max-content;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  padding-inline: 10px;
  line-height: 22px;
}
.filter-bar {
  margin-bottom: 20px;
  background-color: #fcfcfc;
  padding: 18px 18px 2px 18px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}
</style>
