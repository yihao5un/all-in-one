<template>
  <div class="order-management">
    <el-card shadow="never">
      <template #header>
        <div class="header-actions">
          <h3>{{ $t('order.title') }}</h3>
          <el-button type="primary" @click="openCreateDialog">{{ $t('order.create') }}</el-button>
        </div>
      </template>

      <el-table :data="orders" style="width: 100%" v-loading="loading">
        <el-table-column prop="orderNo" :label="$t('order.orderNo')" width="180" />
        <el-table-column prop="orderType" :label="$t('order.type')">
          <template #default="{ row }">
            <el-tag :type="typeStyle(row.orderType)">{{ $t(`order.${row.orderType.toLowerCase()}`) || row.orderType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="employeeId" :label="$t('order.employeeId')" />
        <el-table-column :label="$t('order.product')" min-width="150">
          <template #default="{ row }">
            {{ productName(row.productId) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('order.status')">
          <template #default="{ row }">
            <el-tag :type="statusStyle(row.status)" effect="dark">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('order.createTime')" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('order.actions')" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetails(row)">{{ $t('order.details') }}</el-button>
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
            v-model="newOrder.productId"
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
        <el-descriptions-item :label="$t('order.employeeId')">{{ selectedOrder?.employeeId }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.product')">{{ productName(selectedOrder?.productId) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.status')">
          <el-tag :type="statusStyle(selectedOrder?.status)">{{ statusLabel(selectedOrder?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('order.createTime')">{{ formatDate(selectedOrder?.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBusinessStore } from '@/store/business'
import { useI18n } from 'vue-i18n'
import { formatDate } from '@/utils/format'

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

const newOrder = ref({
  type: 'ONBOARD',
  employeeId: '',
  productId: '',
  notes: ''
})

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

const openCreateDialog = () => {
  newOrder.value = {
    type: 'ONBOARD',
    employeeId: '',
    productId: '',
    notes: ''
  }
  if (businessStore.products.length === 0) {
    businessStore.fetchProducts()
  }
  createDialogVisible.value = true
}

const handleCreate = async () => {
  try {
    if (!newOrder.value.employeeId || !newOrder.value.productId) {
      ElMessage.warning(t('order.createValidation'))
      return
    }
    const result = await businessStore.createOrder(newOrder.value)
    const orderNo = result?.data?.orderNo
    ElMessage.success(orderNo ? `${t('common.success')}: ${orderNo}` : t('common.success'))
    createDialogVisible.value = false
    loadOrders()
  } catch (err: any) {
    ElMessage.error(err?.message || t('common.failed'))
  }
}

const loadOrders = async () => {
  total.value = await businessStore.fetchOrders(currentPage.value, pageSize.value)
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

onMounted(() => {
  loadOrders()
  businessStore.fetchProducts()
  businessStore.fetchUsers()
})
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
</style>
