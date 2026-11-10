import type { BookingAdminRow } from '@/api/booking';

export type BookingOperation = Pick<BookingAdminRow, 'id' | 'bookingNo' | 'status' | 'stayStatus' | 'checkedInAt' | 'checkedOutAt' | 'operationVersion' | 'checkInDate' | 'checkOutDate'>;
export const operationDate = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Taipei', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
export const stayLabel = (booking: BookingOperation) => {
  if (booking.stayStatus === 'checked_out') return '已退房';
  if (booking.stayStatus === 'checked_in') return '已入住';
  if (booking.status === 'completed') return '歷史完成／未登記';
  if (booking.status === 'cancelled') return '已取消／未入住';
  return '未入住';
};
export const formatStayTime = (value: string | null) => value ? new Intl.DateTimeFormat('zh-TW', { timeZone: 'Asia/Taipei', dateStyle: 'short', timeStyle: 'short', hour12: false }).format(new Date(value)) : '尚未登記';
export const canCheckIn = (booking: BookingOperation) => booking.status === 'confirmed' && booking.stayStatus === 'not_checked_in' && booking.checkInDate <= operationDate() && operationDate() < booking.checkOutDate;
export const canCheckOut = (booking: BookingOperation) => booking.status === 'confirmed' && booking.stayStatus === 'checked_in';
export const editableBookingStatuses = (booking: BookingOperation, isAdmin: boolean) => {
  const transitions: Record<string, string[]> = { pending: ['confirmed', 'cancelled'], confirmed: ['cancelled'], cancelled: ['pending', 'confirmed'], completed: [] };
  return [booking.status, ...(isAdmin && booking.stayStatus === 'not_checked_in' ? transitions[booking.status] || [] : [])];
};

export const stayDate = (value: string) => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Taipei', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(value));
export const overlapsOperationDate = (booking: BookingOperation, date: string) =>
  (booking.checkInDate <= date && date <= booking.checkOutDate) ||
  Boolean(booking.checkedInAt && stayDate(booking.checkedInAt) <= date && date <= (booking.checkedOutAt ? stayDate(booking.checkedOutAt) : operationDate()));
export const isOverdueStay = (booking: BookingOperation, date: string) => booking.stayStatus === 'checked_in' && booking.checkOutDate < date && date <= operationDate();
