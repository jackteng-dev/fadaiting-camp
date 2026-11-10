<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { getBackupMonitorService, type BackupMonitorStatus } from '@/api/backup';

const status = ref<BackupMonitorStatus>();
const loading = ref(false);
const unavailable = ref(false);
let version = 0;
const date = (value?: string) => value ? new Intl.DateTimeFormat('zh-TW', {
  timeZone: 'Asia/Taipei', dateStyle: 'short', timeStyle: 'short', hour12: false,
}).format(new Date(value)) : '尚無成功紀錄';
const attemptLabels: Record<string, string> = {running:'尚未完成',success:'成功',failed:'失敗'};
const attemptText = (value?: string) => attemptLabels[value || ''] || '尚無執行紀錄';
const load = async () => {
  const requestVersion = ++version;
  loading.value = true; unavailable.value = false; status.value = undefined;
  try {
    const result = await getBackupMonitorService();
    if (requestVersion === version) status.value = result.data;
  } catch {
    if (requestVersion === version) unavailable.value = true;
  } finally { if (requestVersion === version) loading.value = false; }
};
onMounted(load);
</script>

<template>
  <section class="card backup-observation" aria-label="備份與隔離還原觀測">
    <div class="card-header backup-heading">
      <div><h4>備份與隔離還原觀測</h4><p class="mb-0 text-muted">分開核對最近成功備份與還原驗證。更新只讀紀錄，不執行備份或覆寫資料庫。</p></div>
      <button class="btn btn-outline-primary" :disabled="loading" @click="load">{{ loading ? '更新中...' : '更新觀測' }}</button>
    </div>
    <div class="card-body">
      <p v-if="loading" role="status">正在取得備份觀測...</p>
      <p v-else-if="unavailable" class="text-danger" role="alert">觀測暫時無法取得，目前不能判定備份與還原狀態。</p>
      <template v-else-if="status">
        <p>來源 {{ status.sourceKey }} · 觀測時間 {{ date(status.observedAt) }}（台北）· {{ status.recordCount }} 筆執行紀錄</p>
        <div v-if="status.journalAvailable" class="backup-metrics">
          <div><h6>最近成功備份</h6><strong>{{ date(status.lastBackupSuccess?.finishedAt) }}</strong><p>最近一次操作：{{ attemptText(status.lastBackupAttempt?.status) }}</p><small v-if="status.lastBackupSuccess?.archiveSha256">備份識別 {{ status.lastBackupSuccess.archiveSha256.slice(0, 12) }}</small></div>
          <div><h6>最近成功還原驗證</h6><strong>{{ date(status.lastRestoreSuccess?.finishedAt) }}</strong><p>最近一次操作：{{ attemptText(status.lastRestoreAttempt?.status) }}</p><small v-if="status.lastRestoreSuccess?.archiveSha256">備份識別 {{ status.lastRestoreSuccess.archiveSha256.slice(0, 12) }}</small></div>
          <div><h6>最新成功備份的驗證</h6><strong>{{ status.latestBackupVerified ? '相同備份已驗證' : '尚無相同備份的成功驗證' }}</strong><p v-if="status.latestBackupVerified">驗證時間 {{ date(status.latestBackupVerifiedAt) }}</p><p v-else>其他備份曾還原成功，不代表最新這份已驗證。</p></div>
        </div>
        <p class="small text-muted mt-3">備份提醒門檻 {{ status.maxBackupAgeSeconds / 3600 }} 小時 · 還原驗證提醒門檻 {{ status.maxRestoreAgeSeconds / 86400 }} 天。紀錄是歷史操作結果，不保證備份檔目前仍存在或已完成整體服務復原。</p>
        <p v-if="status.health === 'healthy'" class="text-success" role="status">備份紀錄檢查正常；正式恢復服務仍需核對圖片、環境設定與登入狀態。</p>
        <div v-for="alert in status.alerts" :key="alert.code" class="backup-alert" :class="`backup-alert--${alert.severity}`" :role="alert.severity === 'info' ? 'status' : 'alert'">
          <strong>{{ alert.message }}</strong><p class="mb-0">{{ alert.action }}</p>
        </div>
        <p class="small mt-3">未設定、紀錄無法完整核對或尚無成功結果時，不能判定為健康。執行中紀錄也不代表進程仍存活。</p>
      </template>
    </div>
  </section>
</template>

<style scoped>
.backup-heading { display: flex; justify-content: space-between; align-items: start; gap: 16px; }
.backup-heading button { flex-shrink: 0; }
.backup-metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(250px, 100%), 1fr)); gap: 14px; }
.backup-metrics > div { padding: 16px; border-radius: 8px; background: #f5f7f2; overflow-wrap: anywhere; }
.backup-metrics p { margin-top: 10px; }
.backup-alert { padding: 14px; margin-top: 12px; border-left: 4px solid #538097; background: #eef5f8; }
.backup-alert--warning { border-color: #997317; background: #fff7df; }
.backup-alert--critical { border-color: #b43434; background: #fff0f0; }
@media (max-width: 600px) { .backup-heading { flex-direction: column; } }
</style>
