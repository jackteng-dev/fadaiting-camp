<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElDialog, ElPagination } from 'element-plus'
import { useUserInfoStore } from '@/stores/userInfo'
import { adminLogoutService } from '@/api/user'
import { getStaffAccountListService, createStaffAccountService, updateStaffAccountService,
  type StaffAccount, type StaffCreate, type CampRole } from '@/api/staff'

const router = useRouter()
const userInfo = useUserInfoStore()
const rows = ref<StaffAccount[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const username = ref('')
const pageNum = ref(1)
const pageSize = 20
const createVisible = ref(false)
const editing = ref<StaffAccount | null>(null)
const emptyForm = (): StaffCreate => ({ username: '', password: '', confirmPassword: '', firstName: '', lastName: '', campRole: 'STAFF' })
const form = ref(emptyForm())
const editRole = ref<CampRole>('STAFF')
const editEnabled = ref(true)
const roleText = (role: CampRole) => role === 'ADMIN' ? '營區管理員' : '現場人員'
const formatTime = (value: string | null) => value?.replace('T', ' ').slice(0, 16) || '尚未登入'

const loadData = async () => {
  loading.value = true
  try {
    const response = await getStaffAccountListService({ username: username.value.trim(), pageNum: pageNum.value, pageSize })
    rows.value = response.data.items
    total.value = response.data.total
  } catch { /* API 攔截器已顯示錯誤，保留目前列表供重新載入。 */ }
  finally { loading.value = false }
}
const search = async () => { pageNum.value = 1; await loadData() }
const openCreate = () => { form.value = emptyForm(); createVisible.value = true }
const create = async () => {
  if (!/^[a-zA-Z0-9_]{8,20}$/.test(form.value.username)) { ElMessage.error('帳號需為 8 到 20 碼英數字或底線'); return }
  if (form.value.password.length < 8 || form.value.password.length > 24) { ElMessage.error('密碼需為 8 到 24 碼'); return }
  if (form.value.password !== form.value.confirmPassword) { ElMessage.error('兩次輸入的密碼不一致'); return }
  saving.value = true
  try {
    await createStaffAccountService({ ...form.value })
    form.value = emptyForm()
    createVisible.value = false
    ElMessage.success('員工帳號已建立')
    await search()
  } catch { /* API 攔截器已顯示錯誤，保留表單供修正或重新載入。 */ }
  finally { saving.value = false }
}
const openEdit = (row: StaffAccount) => {
  editing.value = row
  editRole.value = row.campRole
  editEnabled.value = row.enabled
}
const save = async () => {
  const account = editing.value
  if (!account || (account.campRole === editRole.value && account.enabled === editEnabled.value)) return
  try {
    await ElMessageBox.confirm(`確定將 ${account.username} 設為「${roleText(editRole.value)}／${editEnabled.value ? '啟用' : '停用'}」？此帳號需重新登入。`, '調整員工帳號', { confirmButtonText: '確定', cancelButtonText: '取消', type: 'warning' })
  } catch { return }
  saving.value = true
  try {
    await updateStaffAccountService(account.id, { campRole: editRole.value, enabled: editEnabled.value, expectedVersion: account.authVersion })
    editing.value = null
    ElMessage.success('員工帳號已更新')
    if (account.id === userInfo.info?.id) {
      try { await adminLogoutService() } finally { userInfo.removeInfo(); await router.push('/admin/login') }
    } else { await loadData() }
  } catch { /* API 攔截器已顯示錯誤，保留表單供修正或重新載入。 */ }
  finally { saving.value = false }
}
onMounted(loadData)
</script>

<template>
  <div class="page-header">
    <div class="page-block">
    <h4 class="pb-2 border-bottom mb-3">員工帳號</h4>
    <p class="text-muted">管理營區管理員與現場人員。至少保留一位啟用中的管理員。</p>
    </div>
  </div>
  <div class="card">
    <div class="card-header d-flex flex-wrap align-items-center justify-content-between gap-2">
      <h5 class="mb-0">員工列表</h5>
      <button class="btn btn-primary" @click="openCreate">建立員工帳號</button>
    </div>
    <div class="card-body">
      <form class="d-flex flex-wrap gap-2 mb-3" @submit.prevent="search">
        <label class="visually-hidden" for="staff-search">搜尋帳號</label>
        <input id="staff-search" v-model="username" class="form-control search-input" placeholder="搜尋帳號" maxlength="20" />
        <button class="btn btn-outline-primary" :disabled="loading">查詢</button>
        <button type="button" class="btn btn-outline-secondary" :disabled="loading" @click="loadData">重新載入</button>
      </form>
      <div class="table-responsive">
        <table class="table align-middle">
          <thead><tr><th>帳號</th><th>姓名</th><th>角色</th><th>狀態</th><th>最近登入</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id" :data-account="row.username">
              <td>{{ row.username }} <span v-if="row.id === userInfo.info?.id" class="badge bg-light text-dark">目前帳號</span></td>
              <td>{{ row.firstName }}{{ row.lastName }}</td><td>{{ roleText(row.campRole) }}</td>
              <td><span class="badge" :class="row.enabled ? 'bg-success' : 'bg-secondary'">{{ row.enabled ? '啟用' : '停用' }}</span></td>
              <td>{{ formatTime(row.lastLoginTime) }}</td>
              <td><button class="btn btn-outline-primary btn-sm" :disabled="loading || saving" @click="openEdit(row)">調整角色與狀態</button></td>
            </tr>
            <tr v-if="!rows.length"><td colspan="6" class="text-center text-muted">{{ loading ? '載入中…' : '沒有符合的員工帳號' }}</td></tr>
          </tbody>
        </table>
      </div>
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next, total" @current-change="loadData" />
    </div>
  </div>
  <el-dialog v-model="createVisible" title="建立員工帳號" width="min(520px, 94vw)" :close-on-click-modal="!saving" :show-close="!saving" :close-on-press-escape="!saving" @closed="form = emptyForm()">
    <form @submit.prevent="create">
      <label for="staff-username" class="form-label">登入帳號</label>
      <input id="staff-username" v-model="form.username" class="form-control mb-3" autocomplete="off" maxlength="20" required />
      <div class="row g-2 mb-3">
        <div class="col"><label for="staff-first-name" class="form-label">名</label><input id="staff-first-name" v-model="form.firstName" class="form-control" maxlength="50" /></div>
        <div class="col"><label for="staff-last-name" class="form-label">姓</label><input id="staff-last-name" v-model="form.lastName" class="form-control" maxlength="50" /></div>
      </div>
      <label for="staff-password" class="form-label">初始密碼</label>
      <input id="staff-password" v-model="form.password" type="password" autocomplete="new-password" class="form-control" maxlength="24" required />
      <p class="text-muted small mt-1">8 到 24 碼，包含大小寫、數字、符號中的至少三種。</p>
      <label for="staff-confirm-password" class="form-label">確認密碼</label>
      <input id="staff-confirm-password" v-model="form.confirmPassword" type="password" autocomplete="new-password" class="form-control mb-3" maxlength="24" required />
      <label for="staff-create-role" class="form-label">角色</label>
      <select id="staff-create-role" v-model="form.campRole" class="form-select mb-3"><option value="STAFF">現場人員</option><option value="ADMIN">營區管理員</option></select>
      <button class="btn btn-primary" :disabled="saving">{{ saving ? '建立中…' : '建立帳號' }}</button>
    </form>
  </el-dialog>
  <el-dialog :model-value="Boolean(editing)" title="調整員工帳號" width="min(480px, 94vw)" :close-on-click-modal="!saving" :show-close="!saving" :close-on-press-escape="!saving" @close="editing = null">
    <p>{{ editing?.username }}</p>
    <label for="staff-edit-role" class="form-label">角色</label>
    <select id="staff-edit-role" v-model="editRole" class="form-select mb-3"><option value="STAFF">現場人員</option><option value="ADMIN">營區管理員</option></select>
    <label for="staff-enabled" class="form-label">帳號狀態</label>
    <select id="staff-enabled" v-model="editEnabled" class="form-select mb-3"><option :value="true">啟用</option><option :value="false">停用</option></select>
    <button class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? '儲存中…' : '儲存變更' }}</button>
  </el-dialog>
</template>

<style scoped>
.search-input { max-width: 300px; }
th, td { white-space: nowrap; }
</style>
