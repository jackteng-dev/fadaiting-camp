<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { updateBookingStayService } from '@/api/booking';
import { canCheckIn, canCheckOut, formatStayTime, stayLabel, type BookingOperation } from '@/utils/bookingOperations';
const props = defineProps<{ booking: BookingOperation }>();
const emit = defineEmits<{ updated: [] }>();
const saving = ref(false);
const registerStay = async (action: 'check_in' | 'check_out') => {
  const label = action === 'check_in' ? '登記入住' : '登記退房';
  try {
    await ElMessageBox.confirm(`確定為訂單 ${props.booking.bookingNo} ${label}？${action === 'check_out' ? '退房後訂單完成，款項與原預約庫存不會自動調整。' : '請先核對旅客已實際抵達。'}`, label, { confirmButtonText: label, cancelButtonText: '取消', type: 'warning' });
  } catch { return; }
  saving.value = true;
  try {
    await updateBookingStayService({ bookingId: props.booking.id, action, expectedVersion: props.booking.operationVersion });
    ElMessage.success(`${label}完成`);
    emit('updated');
  } catch { /* API 攔截器顯示衝突或權限錯誤；保留畫面供重新載入。 */ }
  finally { saving.value = false; }
};
</script>

<template>
  <div class="stay-actions" :data-booking-id="booking.id">
    <strong>{{ stayLabel(booking) }}</strong>
    <small v-if="booking.checkedInAt">入住：{{ formatStayTime(booking.checkedInAt) }}</small>
    <small v-if="booking.checkedOutAt">退房：{{ formatStayTime(booking.checkedOutAt) }}</small>
    <div class="d-flex flex-wrap gap-2">
      <button v-if="canCheckIn(booking)" type="button" class="btn btn-outline-success btn-sm" :disabled="saving" @click="registerStay('check_in')">登記入住</button>
      <button v-if="canCheckOut(booking)" type="button" class="btn btn-outline-primary btn-sm" :disabled="saving" @click="registerStay('check_out')">登記退房</button>
    </div>
  </div>
</template>

<style scoped>
.stay-actions { display: grid; gap: 6px; min-width: 136px; }
.stay-actions small { color: #667085; white-space: normal; }
</style>
