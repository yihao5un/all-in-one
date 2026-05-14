<template>
  <div class="product-management">
    <el-card shadow="never">
      <template #header>
        <div class="header-actions">
          <h3>{{ $t('product.title') }}</h3>
          <div class="header-right">
            <el-button :icon="Refresh" :loading="businessStore.loading" @click="refreshPage">{{ $t('common.refresh') }}</el-button>
            <el-button type="success" @click="openAddProduct">{{ $t('product.add') }}</el-button>
          </div>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="8" v-for="product in products" :key="product.id">
          <el-card class="product-card" shadow="hover">
            <div class="product-info">
              <h4>{{ product.name }}</h4>
              <el-tag :type="product.status === 'Active' ? 'success' : 'info'">{{ product.status }}</el-tag>
            </div>
            <div class="quota-info">
              <div class="label">{{ $t('product.totalQuota') }}:</div>
              <div class="value">{{ product.totalQuota }}</div>
            </div>
            <div class="quota-info">
              <div class="label">{{ $t('product.remaining') }}:</div>
              <div class="value remaining">{{ product.remainingQuota }}</div>
            </div>
            <el-progress 
              :percentage="calculatePercentage(product)" 
              :status="getProgressStatus(product)"
            />
            <div class="card-footer">
              <el-button link type="primary" @click="handleManageQuota(product)">{{ $t('product.manageQuota') }}</el-button>
              <el-button link type="danger" @click="handleDelete(product)">{{ $t('common.delete') }}</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- Add Product Dialog -->
    <el-dialog v-model="addDialogVisible" :title="$t('product.add')" width="400px">
      <el-form :model="newProduct" label-width="120px">
        <el-form-item :label="$t('product.name')">
          <el-input v-model="newProduct.productName" />
        </el-form-item>
        <el-form-item :label="$t('product.totalQuota')">
          <el-input-number v-model="newProduct.totalQuota" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleAdd">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- Manage Quota Dialog -->
    <el-dialog v-model="quotaDialogVisible" :title="$t('product.manageQuota')" width="400px">
      <el-form label-width="120px">
        <el-form-item :label="$t('product.name')">
          <span>{{ selectedProduct?.name }}</span>
        </el-form-item>
        <el-form-item :label="$t('product.totalQuota')">
          <el-input-number v-model="quotaUpdateValue" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quotaDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="confirmUpdateQuota">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBusinessStore } from '@/store/business'
import { useI18n } from 'vue-i18n'
import { Refresh } from '@element-plus/icons-vue'

const { t } = useI18n()
const addDialogVisible = ref(false)
const quotaDialogVisible = ref(false)
const selectedProduct = ref<any>(null)
const quotaUpdateValue = ref(0)

const businessStore = useBusinessStore()
const products = computed(() => businessStore.products.map((product: any) => ({
  id: product.id,
  name: product.productName,
  totalQuota: product.totalQuota,
  remainingQuota: product.totalQuota - product.usedQuota,
  status: product.status === 1 ? t('product.active') : t('product.inactive'),
  raw: product
})))

const newProduct = ref({
  productName: '',
  totalQuota: 100,
  status: 1,
  usedQuota: 0
})

const calculatePercentage = (product: any) => {
  if (!product.totalQuota) return 0
  return Math.round((product.remainingQuota / product.totalQuota) * 100)
}

const getProgressStatus = (product: any) => {
  const percentage = calculatePercentage(product)
  if (percentage < 10) return 'exception'
  if (percentage < 30) return 'warning'
  return 'success'
}

const openAddProduct = () => {
  newProduct.value = { productName: '', totalQuota: 100, status: 1, usedQuota: 0 }
  addDialogVisible.value = true
}

const handleAdd = async () => {
  try {
    await businessStore.createProduct(newProduct.value)
    ElMessage.success(t('common.success'))
    addDialogVisible.value = false
  } catch (e) {
    ElMessage.error(t('common.failed'))
  }
}

const handleDelete = async (product: any) => {
  try {
    await ElMessageBox.confirm(t('product.deleteConfirm'), t('common.delete'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
    await businessStore.deleteProduct(product.id)
    ElMessage.success(t('common.success'))
  } catch (e) {}
}

const handleManageQuota = (product: any) => {
  selectedProduct.value = product
  quotaUpdateValue.value = product.totalQuota
  quotaDialogVisible.value = true
}

const confirmUpdateQuota = async () => {
  try {
    const updatedProduct = { ...selectedProduct.value.raw, totalQuota: quotaUpdateValue.value }
    await businessStore.updateProduct(updatedProduct)
    ElMessage.success(t('common.success'))
    quotaDialogVisible.value = false
  } catch (e) {
    ElMessage.error(t('common.failed'))
  }
}

const refreshPage = () => businessStore.fetchProducts()

onMounted(refreshPage)
</script>

<style scoped>
.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.product-card {
  margin-bottom: 20px;
}
.product-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}
.product-info h4 { margin: 0; }
.quota-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 0.9rem;
}
.quota-info .label { color: #64748b; }
.quota-info .value { font-weight: bold; }
.remaining { color: #4f46e5; }
.card-footer {
  margin-top: 15px;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  justify-content: flex-end;
}
</style>
