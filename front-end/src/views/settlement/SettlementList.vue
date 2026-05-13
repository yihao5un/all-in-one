<template>
  <div class="settlement-management">
    <el-card shadow="never">
      <template #header>
        <div class="header-actions">
          <h3>{{ $t('settlement.title') }}</h3>
          <el-button type="warning" @click="openCreateDialog">{{ $t('settlement.createBill') }}</el-button>
        </div>
      </template>

      <el-table :data="bills" style="width: 100%" v-loading="businessStore.loading">
        <el-table-column prop="billNo" :label="$t('settlement.billNo')" width="190" />
        <el-table-column prop="orderNo" :label="$t('order.orderNo')" width="180" />
        <el-table-column :label="$t('employee.name')" min-width="120">
          <template #default="{ row }">
            {{ employeeName(row.employeeId) }}
          </template>
        </el-table-column>
        <el-table-column prop="amount" :label="$t('settlement.amount')" width="140" align="right">
          <template #default="{ row }">
            <span class="amount">¥ {{ formatAmount(row.amount || 0) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="billType" :label="$t('settlement.type')" width="130">
          <template #default="{ row }">
            {{ billTypeLabel(row.billType) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('settlement.status')" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PAID' ? 'success' : 'warning'">{{ billStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('settlement.createdAt')" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="280">
          <template #default="{ row }">
            <el-button link type="primary" @click="downloadReport(row)">{{ $t('settlement.downloadReport') }}</el-button>
            <el-button link type="primary" @click="showDetails(row)">{{ $t('common.details') }}</el-button>
            <el-button v-if="row.status !== 'PAID'" link type="success" @click="payBill(row)">{{ $t('settlement.markPaid') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="mt-20" shadow="never">
      <template #header>{{ $t('settlement.summary') }}</template>
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="summary-item">
            <span class="label">{{ $t('settlement.totalBills') }}</span>
            <strong>{{ bills.length }}</strong>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="summary-item">
            <span class="label">{{ $t('settlement.pendingAmount') }}</span>
            <strong>¥ {{ formatAmount(pendingAmount) }}</strong>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="summary-item">
            <span class="label">{{ $t('settlement.paidAmount') }}</span>
            <strong>¥ {{ formatAmount(paidAmount) }}</strong>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-dialog v-model="createDialogVisible" :title="$t('settlement.createBill')" width="520px">
      <el-form label-width="120px">
        <el-form-item :label="$t('common.orderMgmt')">
          <el-select v-model="selectedOrderNo" :placeholder="$t('common.search')" style="width: 100%">
            <el-option
              v-for="order in billableOrders"
              :key="order.orderNo"
              :label="`${order.orderNo} / ${employeeName(order.employeeId)} / ${billableOrderStatus(order.status)}`"
              :value="order.orderNo"
            />
          </el-select>
        </el-form-item>
        <el-empty v-if="billableOrders.length === 0" :description="$t('settlement.noRebuildableOrders')" />
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :disabled="!selectedOrder" @click="createBill">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- Bill Detail Dialog -->
    <el-dialog v-model="detailVisible" :title="$t('common.details')" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="$t('settlement.billNo')">{{ selectedBill?.billNo }}</el-descriptions-item>
        <el-descriptions-item :label="$t('order.orderNo')">{{ selectedBill?.orderNo }}</el-descriptions-item>
        <el-descriptions-item :label="$t('employee.name')">{{ employeeName(selectedBill?.employeeId) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('settlement.amount')">¥ {{ formatAmount(selectedBill?.amount || 0) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('settlement.type')">{{ billTypeLabel(selectedBill?.billType) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('settlement.status')">
          <el-tag :type="selectedBill?.status === 'PAID' ? 'success' : 'warning'">{{ billStatusLabel(selectedBill?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('settlement.createdAt')">{{ formatDate(selectedBill?.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBusinessStore } from '@/store/business'
import { useI18n } from 'vue-i18n'
import { formatDate } from '@/utils/format'

const { t } = useI18n()
const businessStore = useBusinessStore()
const createDialogVisible = ref(false)
const detailVisible = ref(false)
const selectedBill = ref<any>(null)
const selectedOrderNo = ref('')

const bills = computed(() => businessStore.bills)
const billOrderNos = computed(() => new Set(bills.value.map((bill: any) => bill.orderNo)))
const billableOrders = computed(() => businessStore.orders.filter((order: any) =>
  order.orderType === 'ONBOARD' &&
  order.status === 'PENDING_PAYMENT' &&
  !billOrderNos.value.has(order.orderNo)
))
const selectedOrder = computed(() => businessStore.orders.find((order: any) => order.orderNo === selectedOrderNo.value))

const employeeName = (employeeId: number | string) => {
  const user = businessStore.users.find((item: any) => String(item.id) === String(employeeId))
  return user?.realName || user?.username || '-'
}

const billTypeLabel = (type: string) => {
  if (!type) return '-'
  return t(`settlement.${type.toLowerCase()}`) || type
}

const billStatusLabel = (status: string) => {
  if (!status) return '-'
  return t(`settlement.${status.toLowerCase()}`) || status
}

const billableOrderStatus = (status: string) => {
  if (!status) return '-'
  return t(`order.${status.toLowerCase()}`) || status
}

const pendingAmount = computed(() => bills.value
  .filter((bill: any) => bill.status !== 'PAID')
  .reduce((sum: number, bill: any) => sum + Number(bill.amount || 0), 0))
const paidAmount = computed(() => bills.value
  .filter((bill: any) => bill.status === 'PAID')
  .reduce((sum: number, bill: any) => sum + Number(bill.amount || 0), 0))

const formatAmount = (val: number) => {
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

const openCreateDialog = async () => {
  await businessStore.fetchOrders()
  await businessStore.fetchBills()
  selectedOrderNo.value = billableOrders.value[0]?.orderNo || ''
  createDialogVisible.value = true
}

const createBill = async () => {
  if (!selectedOrder.value) return
  await businessStore.rebuildBill(selectedOrder.value.orderNo)
  ElMessage.success(t('common.success'))
  createDialogVisible.value = false
}

const payBill = async (row: any) => {
  await ElMessageBox.confirm(`${t('settlement.markPaid')} ${row.billNo}?`, t('settlement.markPaid'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    type: 'warning',
  })
  await businessStore.payBill(row.billNo)
  ElMessage.success(t('common.success'))
}

const showDetails = (row: any) => {
  selectedBill.value = row
  detailVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(t('settlement.deleteConfirm'), t('common.delete'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
    await businessStore.deleteBill(row.id)
    ElMessage.success(t('common.success'))
  } catch (e) {}
}

const downloadReport = (row: any) => {
  const report = [
    'Uno Settlement Report',
    `Bill No,${row.billNo}`,
    `Order No,${row.orderNo}`,
    `Employee ID,${row.employeeId}`,
    `Amount,${row.amount || 0}`,
    `Status,${row.status}`,
    `Created At,${formatDate(row.createTime)}`,
  ].join('\n')
  const blob = new Blob([report], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${row.billNo}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

onMounted(() => {
  businessStore.fetchBills()
  businessStore.fetchOrders()
  businessStore.fetchUsers()
})
</script>

<style scoped>
.mt-20 { margin-top: 20px; }
.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.amount { font-family: 'Courier New', Courier, monospace; font-weight: bold; }
.summary-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 0;
}
.summary-item .label { color: #64748b; font-size: 0.875rem; }
.summary-item strong { color: #1e293b; font-size: 1.4rem; }
</style>
