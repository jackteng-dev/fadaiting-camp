<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { getNotificationRetentionPreviewService, type NotificationRetentionPreview } from '@/api/notificationRetention';

const preview = ref<NotificationRetentionPreview>();
const loading = ref(false);
const unavailable = ref(false);
let version = 0;
const reasons: Record<string, string> = {
  CANDIDATE: '低風險終態候選（仍需核定）', DELIVERY_UNCERTAIN: '寄送結果不明',
  UNRESOLVED_STATUS: '待處理、失敗或其他未結案狀態', QUEUE_IDENTITY: '新版佇列或事件去重鍵',
  ATTEMPT_HISTORY: '發送嘗試稽核', DELIVERY_AUDIT: 'Email、送達或供應商稽核',
  FINANCIAL_AUDIT: '金流及其他事件稽核', BOOKING_OPEN: '訂單未結束或住宿待核對',
  SCHEDULE_OR_LEASE: '仍有排程或租約資料', RECENT_ACTIVITY: '近期活動或時間不完整',
};
const displayDate = (value?: string) => value ? value.replace('T', ' ').slice(0, 16) : '—';
const load = async () => {
  const requestVersion = ++version;
  loading.value = true; unavailable.value = false; preview.value = undefined;
  try {
    const result = await getNotificationRetentionPreviewService();
    if (requestVersion === version) preview.value = result.data;
  } catch { if (requestVersion === version) unavailable.value = true; }
  finally { if (requestVersion === version) loading.value = false; }
};
onMounted(load);
</script>

<template>
  <section class="retention-preview" aria-label="通知歷史保留試算">
    <div class="retention-heading"><h5 class="mb-0">通知歷史保留試算</h5><button class="btn btn-outline-primary btn-sm" :disabled="loading" @click="load">{{ loading ? '試算中...' : '更新保留試算' }}</button></div>
    <p class="small text-muted">全站所有通知，不受列表篩選影響。僅唯讀 dry-run；候選不是刪除許可，正式保留政策仍需核定。</p>
    <p v-if="loading" role="status">正在試算保留範圍...</p>
    <p v-else-if="unavailable" class="text-danger" role="alert">保留試算暫時無法取得，目前不能判定候選數量。</p>
    <template v-else-if="preview">
      <p>草案門檻 {{ preview.previewDays }} 天 · 截止 {{ displayDate(preview.cutoffAt) }}（{{ preview.timeZone }}）· 觀測 {{ new Intl.DateTimeFormat('zh-TW', {timeZone:'Asia/Taipei',dateStyle:'short',timeStyle:'short',hour12:false}).format(new Date(preview.observedAt)) }}（台北）</p>
      <div class="retention-metrics"><div><span>全部通知</span><strong>{{ preview.totalCount }}</strong></div><div><span>評估候選</span><strong>{{ preview.candidateCount }}</strong></div><div><span>保護保留</span><strong>{{ preview.protectedCount }}</strong></div></div>
      <p class="small mt-2">候選僅限舊站內／人工終態、已結束且無付款的訂單，沒有新版佇列、去重鍵、寄送嘗試、排程或租約。保留最後活動時間相等於截止的資料。</p>
      <div class="retention-reasons"><div v-for="bucket in preview.buckets" :key="bucket.reason"><strong>{{ reasons[bucket.reason] || '其他保護條件' }}：{{ bucket.count }} 筆</strong><p v-if="bucket.reason === 'CANDIDATE'" class="small mb-0">站內已確認 {{ bucket.acknowledgedCount }} · 略過 {{ bucket.skippedCount }} · 模擬 {{ bucket.simulatedCount }}<br />活動範圍 {{ displayDate(bucket.oldestActivityAt) }} — {{ displayDate(bucket.newestActivityAt) }}</p></div></div>
      <p class="small mt-2 mb-0">每筆依第一個符合的原因分類，數量不重複。{{ preview.policyVersion }} · 不刪通知／嘗試／訂單歷程，不啟動 worker，也不寄信。結果只代表此刻資料，不是可執行的清理清單。</p>
    </template>
  </section>
</template>

<style scoped>
.retention-preview { margin: 24px 0; padding: 18px; border: 1px solid #d8e0d6; border-radius: 8px; overflow-wrap: anywhere; }
.retention-heading { display: flex; justify-content: space-between; align-items: start; gap: 12px; margin-bottom: 12px; }
.retention-heading button { flex-shrink: 0; }
.retention-metrics { display: grid; grid-template-columns: repeat(auto-fit,minmax(min(150px,100%),1fr)); gap: 12px; }
.retention-metrics > div { background: #f3f6f1; padding: 14px; border-radius: 6px; }
.retention-metrics strong { display: block; font-size: 1.5rem; }
.retention-reasons > div { border-bottom: 1px solid #e5e8e2; padding: 10px 0; }
@media (max-width:600px) { .retention-heading { flex-direction: column; } }
</style>
