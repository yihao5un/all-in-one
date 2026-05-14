<template>
  <div class="employee-management">
    <el-card shadow="never">
      <template #header>
        <div class="header-actions">
          <h3>{{ $t('employee.title') }}</h3>
          <div class="header-right">
            <el-input
              v-model="search"
              :placeholder="$t('employee.searchPlaceholder')"
              style="width: 300px;"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button :icon="Refresh" :loading="businessStore.loading" @click="refreshPage">{{ $t('common.refresh') }}</el-button>
            <el-button type="primary" @click="openAddDialog">{{ $t('common.add') }}</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredEmployees" style="width: 100%" v-loading="businessStore.loading">
        <el-table-column :label="$t('employee.name')" width="260">
          <template #default="{ row }">
            <div class="employee-cell">
              <el-avatar :size="40">{{ row.name.charAt(0) }}</el-avatar>
              <div class="info">
                <div class="name">{{ row.name }}</div>
                <div class="id">EMP{{ row.id }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" :label="$t('employee.username')" />
        <el-table-column prop="role" :label="$t('employee.role')">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">
              {{ row.role === 'ADMIN' ? $t('employee.admin') : $t('employee.emp') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="statusText" :label="$t('employee.status')">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="joinDate" :label="$t('employee.joinDate')" width="180">
          <template #default="{ row }">
            {{ formatDate(row.joinDate) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="250">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetails(row)">{{ $t('common.details') }}</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? $t('employee.disable') : $t('employee.enable') }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add Employee Dialog -->
    <el-dialog v-model="addVisible" :title="$t('employee.addTitle')" width="450px">
      <el-form :model="newEmp" label-width="100px">
        <el-form-item :label="$t('employee.username')">
          <el-input v-model="newEmp.username" />
        </el-form-item>
        <el-form-item :label="$t('employee.password')">
          <el-input v-model="newEmp.password" type="password" show-password />
        </el-form-item>
        <el-form-item :label="$t('employee.realName')">
          <el-input v-model="newEmp.realName" />
        </el-form-item>
        <el-form-item :label="$t('employee.role')">
          <el-select v-model="newEmp.role" style="width: 100%">
            <el-option :label="$t('employee.admin')" value="ADMIN" />
            <el-option :label="$t('employee.emp')" value="EMPLOYEE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleAdd">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- Employee Detail Dialog -->
    <el-dialog v-model="detailVisible" :title="$t('common.details')" width="500px">
      <div v-if="selectedEmp" class="detail-container">
        <div class="detail-header">
          <el-avatar :size="64">{{ selectedEmp.name.charAt(0) }}</el-avatar>
          <h3>{{ selectedEmp.name }}</h3>
          <el-tag>{{ selectedEmp.role }}</el-tag>
        </div>
        <el-divider />
        <el-descriptions :column="1" border>
          <el-descriptions-item :label="$t('employee.username')">{{ selectedEmp.username }}</el-descriptions-item>
          <el-descriptions-item :label="$t('employee.status')">
            <el-tag :type="selectedEmp.status === 1 ? 'success' : 'info'">{{ selectedEmp.statusText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('employee.joinDate')">{{ formatDate(selectedEmp.joinDate) }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { useBusinessStore } from '@/store/business'
import { useI18n } from 'vue-i18n'
import { formatDate } from '@/utils/format'

const { t } = useI18n()
const search = ref('')
const businessStore = useBusinessStore()
const detailVisible = ref(false)
const addVisible = ref(false)
const selectedEmp = ref<any>(null)

const newEmp = ref({
  username: '',
  password: '',
  realName: '',
  role: 'EMPLOYEE',
  status: 1
})

const employees = computed(() => businessStore.users.map((user: any) => ({
  id: user.id,
  username: user.username,
  name: user.realName || user.username,
  role: user.role || (user.username === 'admin' ? 'ADMIN' : 'EMPLOYEE'),
  status: user.status,
  statusText: user.status === 1 ? t('employee.active') : t('employee.disabled'),
  joinDate: user.createTime,
})))

const filteredEmployees = computed(() => {
  const keyword = search.value.toLowerCase()
  return employees.value.filter(emp =>
    emp.name.toLowerCase().includes(keyword) ||
    String(emp.id).toLowerCase().includes(keyword) ||
    emp.username.toLowerCase().includes(keyword)
  )
})

const showDetails = (row: any) => {
  selectedEmp.value = row
  detailVisible.value = true
}

const openAddDialog = () => {
  newEmp.value = { username: '', password: '', realName: '', role: 'EMPLOYEE', status: 1 }
  addVisible.value = true
}

const handleAdd = async () => {
  try {
    await businessStore.createUser(newEmp.value)
    ElMessage.success(t('common.success'))
    addVisible.value = false
  } catch (e) {
    ElMessage.error(t('common.failed'))
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(t('order.deleteConfirm'), t('common.delete'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
    await businessStore.deleteUser(row.id)
    ElMessage.success(t('common.success'))
  } catch (e) {}
}

const toggleStatus = async (row: any) => {
  const nextStatus = row.status === 1 ? 0 : 1
  await businessStore.updateUserStatus(row.id, nextStatus)
  ElMessage.success(nextStatus === 1 ? t('employee.enable') : t('employee.disable'))
}

const refreshPage = () => businessStore.fetchUsers(search.value)

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
.employee-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.employee-cell .info .name { font-weight: bold; color: #1e293b; }
.employee-cell .info .id { font-size: 0.8rem; color: #64748b; }
.detail-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}
.detail-header h3 { margin: 0; }
</style>
