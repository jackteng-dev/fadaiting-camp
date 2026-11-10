<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { ElDialog, ElMessage } from "element-plus";
import { confirmAdminAction } from "@/utils/confirmAction";
import { formatStayTime } from "@/utils/bookingOperations";
import { getPaymentFinancialService, createRefundService, updateRefundService, updatePaymentReconciliationService, type PaymentFinancialResponse, type RefundRecord } from "@/api/paymentFinance";

const props = defineProps<{ paymentId: number | null }>();
const emit = defineEmits<{ close: []; updated: [] }>();
const financial = ref<PaymentFinancialResponse | null>(null), loading = ref(false), saving = ref(false), error = ref("");
const amount = ref(0), reason = ref(""), note = ref(""), reference = ref("");
const makeKey = () => globalThis.crypto?.randomUUID?.() ?? "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, code => { const value = Math.floor(Math.random() * 16); return (code === "x" ? value : (value & 3) | 8).toString(16); });
const requestKey = ref(makeKey());
const statusLabels: Record<string, string> = { requested: "待審核", approved: "已核准", rejected: "已駁回", completed: "人工退款完成", simulated: "模擬完成（未退เงินจริง）" };
const canReconcile = computed(() => financial.value?.paymentStatus === "paid_review" && financial.value.reconciliationStatus === "unreviewed");
let generation = 0;
const load = async () => {
  if (!props.paymentId) return;
  const current = ++generation; loading.value = true; error.value = "";
  try {
    const result = await getPaymentFinancialService(props.paymentId);
    if (current === generation) financial.value = result.data;
  } catch (failure: any) { if (current === generation) error.value = failure?.message || "退款資料載入失敗"; }
  finally { if (current === generation) loading.value = false; }
};
watch(() => props.paymentId, async id => {
  ++generation; financial.value = null; error.value = ""; amount.value = 0; reason.value = ""; note.value = ""; reference.value = ""; requestKey.value = makeKey();
  if (id) { await load(); amount.value = Number((financial.value as PaymentFinancialResponse | null)?.availableRefundAmount || 0); }
}, { immediate: true });
watch([amount, reason], () => { requestKey.value = makeKey(); });
const execute = async (operation: () => Promise<unknown>, message: string) => {
  saving.value = true;
  try { await operation(); ElMessage.success(message); emit("updated"); await load(); }
  catch (failure: any) { const message = failure?.message || "操作失敗，請重新載入核對"; await load(); error.value = message; }
  finally { saving.value = false; }
};
const create = async () => {
  if (!financial.value || !props.paymentId || !reason.value.trim() || amount.value <= 0) { ElMessage.warning("請填寫退款金額與原因"); return; }
  const payload = { paymentRecordId: props.paymentId, amount: amount.value, reason: reason.value, idempotencyKey: requestKey.value, expectedVersion: financial.value.operationVersion };
  if (!(await confirmAdminAction({ title: "申請退款", message: `申請 NT$ ${amount.value} 退款，送出後會保留額度，仍需審核。`, confirmButtonText: "送出申請" }))) return;
  await execute(() => createRefundService(payload), "退款申請已建立");
  if (!error.value) { reason.value = ""; amount.value = Number(financial.value.availableRefundAmount); }
};
const reconcile = async (resolution: string) => {
  if (!financial.value || !props.paymentId || !note.value.trim() || !reference.value.trim()) { ElMessage.warning("請填寫查單憑證編號與核對說明"); return; }
  const paymentId = props.paymentId;
  const payload = { resolution, expectedVersion: financial.value.operationVersion, expectedBookingVersion: financial.value.bookingOperationVersion, note: note.value, externalReference: reference.value };
  if (!(await confirmAdminAction({ title: "核對收款", message: resolution === "sync_ledger" ? "已根據憑證核對既有收退款紀錄，確認以付款淨額同步訂單已收金額？" : resolution === "accept" ? "確認此款項已實際收到並歸帳？此操作不會恢復已取消預約。" : "確認此款項應安排退款？仍需另外申請及審核退款。", confirmButtonText: "確認核對" }))) return;
  await execute(() => updatePaymentReconciliationService(paymentId, payload), "收款核對已保存");
};
const action = async (refund: RefundRecord, next: string) => {
  if (!note.value.trim()) { ElMessage.warning("請填寫處理說明"); return; }
  if (next === "complete_manual" && !reference.value.trim()) { ElMessage.warning("請填寫實際退款憑證編號"); return; }
  const labels: Record<string, string> = { approve: "核准退款", reject: "駁回退款", simulate: "模擬退款", complete_manual: "登記人工完成" };
  const message = next === "simulate" ? "僅演練退款流程，不轉移資金、不減少實際已收金額，之後仍可重新申請退款。" : next === "complete_manual" ? "請確認已在系統外完成退款；保存憑證後會扣減已收金額，且不能再次完成同一筆退款。" : `確認${labels[next]} NT$ ${refund.amount}？`;
  if (!(await confirmAdminAction({ title: labels[next], message, confirmButtonText: "確認處理" }))) return;
  await execute(() => updateRefundService(refund.id, { action: next, expectedVersion: refund.operationVersion, note: note.value, externalReference: next === "complete_manual" ? reference.value : undefined }), labels[next] + "已保存");
};
</script>

<template>
  <ElDialog :model-value="paymentId !== null" title="退款與人工對帳" width="min(960px, 95vw)" :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" @close="emit('close')">
    <p>一般收款須先取消或完成預約才可申請退款。這裡不連線銀行或金流轉帳。</p>
    <p v-if="loading">資料載入中...</p>
    <p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p>
    <template v-if="financial">
      <div class="finance-summary">
        <span>原收款 NT$ {{ financial.amount }}</span><span>已實退 NT$ {{ financial.completedRefundAmount }}</span>
        <span>申請保留 NT$ {{ financial.reservedRefundAmount }}</span><span>可申請 NT$ {{ financial.availableRefundAmount }}</span>
      </div>
      <p v-if="financial.ledgerPaidAmount !== financial.bookingPaidAmount" class="alert alert-warning">訂單已收金額與付款淨額不一致，請先核對舊帳，再辦理歸帳或人工退款。</p>
      <p v-if="financial.reconciledAt">核對：{{ financial.reconciliationStatus === 'accepted' ? '確認歸帳' : financial.reconciliationStatus === 'ledger_checked' ? '帳本已核對' : '安排退款' }}，管理員 #{{ financial.reconciledBy }}，{{ formatStayTime(financial.reconciledAt) }}<br />憑證 {{ financial.reconciliationReference }}；{{ financial.reconciliationNote }}</p>
      <div class="processing-fields">
        <label>查單／實際退款憑證編號<input v-model="reference" class="form-control" maxlength="100" :disabled="saving" /></label>
        <label>核對／處理說明<textarea v-model="note" class="form-control" maxlength="1000" :disabled="saving" /></label>
      </div>
      <button v-if="financial.paymentStatus === 'paid' && financial.ledgerPaidAmount !== financial.bookingPaidAmount" class="btn btn-outline-warning my-3" :disabled="saving" @click="reconcile('sync_ledger')">核對並同步帳本</button>
      <div v-if="canReconcile" class="d-flex gap-2 my-3">
        <button class="btn btn-primary" :disabled="saving" @click="reconcile('accept')">確認歸帳</button>
        <button class="btn btn-outline-danger" :disabled="saving" @click="reconcile('refund_required')">安排退款</button>
      </div>
      <form v-if="financial.canRequestRefund && financial.availableRefundAmount > 0" class="refund-form" @submit.prevent="create">
        <label>退款金額<input v-model.number="amount" class="form-control" type="number" min="0.01" step="0.01" :max="financial.availableRefundAmount" :disabled="saving" required /></label>
        <label>退款原因<textarea v-model="reason" class="form-control" maxlength="1000" :disabled="saving" required /></label>
        <button class="btn btn-outline-primary" :disabled="saving">申請退款</button>
      </form>
      <div class="table-responsive mt-3">
        <table class="table"><thead><tr><th>退款</th><th>金額／狀態</th><th>原因／紀錄</th><th>操作</th></tr></thead><tbody>
          <tr v-if="!financial.refunds.length"><td colspan="4">尚無退款申請</td></tr>
          <tr v-for="refund in financial.refunds" :key="refund.id">
            <td>#{{ refund.id }}<br />{{ formatStayTime(refund.requestedAt) }}<br />申請者 #{{ refund.requestedBy }}</td>
            <td>NT$ {{ refund.amount }}<br />{{ statusLabels[refund.status] }}</td>
            <td>{{ refund.reason }}<br />{{ refund.decisionNote }}<br /><span v-if="refund.decidedBy">審核者 #{{ refund.decidedBy }}，{{ formatStayTime(refund.decidedAt) }}</span><br /><span v-if="refund.completedBy">處理者 #{{ refund.completedBy }}，{{ formatStayTime(refund.completedAt) }}</span><br />{{ refund.externalReference }}</td>
            <td class="refund-actions">
              <template v-if="refund.status === 'requested'"><button class="btn btn-sm btn-outline-primary" :disabled="saving" @click="action(refund, 'approve')">核准</button><button class="btn btn-sm btn-outline-danger" :disabled="saving" @click="action(refund, 'reject')">駁回</button></template>
              <template v-if="refund.status === 'approved'"><button class="btn btn-sm btn-outline-secondary" :disabled="saving" @click="action(refund, 'simulate')">模擬退款</button><button class="btn btn-sm btn-primary" :disabled="saving" @click="action(refund, 'complete_manual')">登記人工完成</button></template>
            </td>
          </tr>
        </tbody></table>
      </div>
      <button class="btn btn-outline-secondary" :disabled="saving || loading" @click="load">重新載入</button>
    </template>
  </ElDialog>
</template>

<style scoped>
.finance-summary, .processing-fields, .refund-form { display: grid; gap: 12px; grid-template-columns: repeat(2, minmax(0, 1fr)); margin-bottom: 16px; }
.finance-summary { padding: 16px; background: #f6f7f8; border-radius: 8px; }
.refund-actions { min-width: 140px; }
.refund-actions button { margin: 0 4px 6px 0; }
@media (max-width: 600px) { .processing-fields, .refund-form { grid-template-columns: 1fr; } }
</style>
