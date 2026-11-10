import request from "@/api/api";

export type BookingAdminQuery = {
  pageNum: number;
  pageSize: number;
  bookingNo?: string;
  status?: string;
  contactName?: string;
  startDate?: string;
  endDate?: string;
};

export type BookingAdminRow = {
  id: number;
  bookingNo: string;
  status: string;
  source: string;
  stayStatus: "not_checked_in" | "checked_in" | "checked_out";
  checkedInAt: string | null;
  checkedOutAt: string | null;
  operationVersion: number;
  paymentDueAt: string | null;
  expiredAt: string | null;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  itemName: string;
  checkInDate: string;
  checkOutDate: string;
  nights: number;
  guestCount: number;
  vehicleCount: number;
  subtotalAmount: number;
  adjustmentAmount: number;
  totalAmount: number;
  paidAmount: number;
  bookingNote?: string;
  adminNote?: string;
  confirmNote?: string;
  cancelReason?: string;
  arrivalNote?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  createdTime: string;
  modifiedTime?: string;
};

export type BookingStatusUpdate = {
  expectedVersion: number;
  bookingId: number;
  status: string;
};

export type BookingAdminUpdate = {
  expectedVersion: number;
  bookingId: number;
  paidAmount: number;
  adjustmentAmount: number;
  adminNote?: string;
  confirmNote?: string;
  cancelReason?: string;
};

export type ManualPaymentCreate = {
  bookingId: number;
  amount: number;
  paymentMethod?: string;
  note?: string;
};

export type BookingDetailItem = {
  id: number;
  bookingId: number;
  campUnitId?: number | null;
  ratePlanId?: number | null;
  itemName: string;
  campUnitName?: string;
  ratePlanName?: string;
  quantity: number;
  unitPrice: number;
  nights: number;
  lineTotal: number;
  createdTime?: string;
  modifiedTime?: string;
};

export type BookingTimelineEntry = {
  id: number;
  bookingId: number;
  actionType: string;
  actionLabel: string;
  actorUserId?: number | null;
  actorName?: string;
  detail?: string;
  createdTime?: string;
};

export type PaymentRecord = {
  refundedAmount?: number;
  id: number;
  bookingId: number;
  provider: string;
  paymentMethod: string;
  status: string;
  amount: number;
  currency: string;
  merchantOrderNo: string;
  providerTransactionId?: string;
  paymentNo?: string;
  paymentUrl?: string;
  callbackPayload?: string;
  paidTime?: string;
  createdTime?: string;
  modifiedTime?: string;
};

export type PaymentAdminQuery = {
  pageNum: number;
  pageSize: number;
  bookingNo?: string;
  provider?: string;
  status?: string;
  merchantOrderNo?: string;
  startDate?: string;
  endDate?: string;
};

export type PaymentAdminRow = PaymentRecord & {
  bookingNo: string;
  bookingStatus: string;
  contactName?: string;
  contactPhone?: string;
};

export type PaymentAdminSummary = {
  status: string;
  recordCount: number;
  totalAmount: number;
};

export type PaymentMethodConfig = {
  id?: number;
  provider: "linepay" | "ecpay";
  displayName: string;
  description: string;
  paymentMethod: string;
  sortOrder: number;
  isActive: boolean;
  createdTime?: string;
  modifiedTime?: string;
};

export type NotificationAdminQuery = {
  pageNum: number;
  pageSize: number;
  bookingNo?: string;
  eventType?: string;
  channel?: string;
  status?: string;
};

export type NotificationQueueStatus = {
  dueCount: number; waitingCount: number; processingCount: number; failedCount: number;
  uncertainCount: number; expiredLeaseCount: number; overdueCount: number; blockedCount: number;
  observedAt: string; oldestDueAt?: string; oldestExpiredLeaseAt?: string;
  oldestDueAgeSeconds: number; overdueThresholdSeconds: number;
  health: "healthy" | "info" | "warning" | "critical";
  alerts: {code: string; severity: "info" | "warning" | "critical"; count: number; message: string; action: string}[];
  deliveryMode: "disabled" | "simulate" | "smtp"; workerEnabled: boolean;
};
export type NotificationAttempt = {
  id: number; attemptNumber: number; deliveryMode: string; status: string;
  errorCode?: string; startedAt: string; finishedAt?: string; actorName: string;
};
export type NotificationAdminRow = {
  queueManaged: boolean;
  attemptCount: number; attemptLimit: number; operationVersion: number;
  nextAttemptAt?: string; leaseUntil?: string; deliveryMode?: string; errorCode?: string;
  id: number;
  bookingId: number;
  bookingNo: string;
  bookingStatus: string;
  contactName?: string;
  contactPhone?: string;
  channel: string;
  eventType: string;
  recipient: string;
  recipientName?: string;
  subject: string;
  message: string;
  status: string;
  provider: string;
  errorMessage?: string;
  payload?: string;
  sentTime?: string;
  createdTime?: string;
  modifiedTime?: string;
};

export type NotificationStatusUpdate = {
  notificationId: number;
  status: string;
  expectedVersion: number;
  reason: string;
};

export type NotificationSendQueuedPayload = {
  limit?: number;
  bookingNo?: string;
};

export type NotificationReminderCreatePayload = {
  targetDate?: string;
  daysBefore?: number;
};

export type NotificationSendResult = {
  simulatedCount: number; retryCount: number; unresolvedCount: number; createdCount: number;
  processedCount: number;
  sentCount: number;
  failedCount: number;
  skippedCount: number;
};

export type BookingDetailResponse = {
  booking: {
    id: number;
    bookingNo: string;
    status: string;
    source: string;
    stayStatus: "not_checked_in" | "checked_in" | "checked_out";
    checkedInAt: string | null;
    checkedOutAt: string | null;
    operationVersion: number;
    paymentDueAt: string | null;
    expiredAt: string | null;
    checkInDate: string;
    checkOutDate: string;
    nights: number;
    guestCount: number;
    vehicleCount: number;
    subtotalAmount: number;
    adjustmentAmount: number;
    totalAmount: number;
    paidAmount: number;
    note?: string;
    adminNote?: string;
    confirmNote?: string;
    cancelReason?: string;
    createdTime?: string;
    modifiedTime?: string;
  };
  guest?: {
    contactName?: string;
    contactPhone?: string;
    contactEmail?: string;
    emergencyContactName?: string;
    emergencyContactPhone?: string;
    arrivalNote?: string;
  } | null;
  items: BookingDetailItem[];
  timeline: BookingTimelineEntry[];
  payments: PaymentRecord[];
};

export const getBookingListService = (payload: BookingAdminQuery) =>
  request.post("/booking/list", payload);

export const expireUnpaidBookingsService = () => request.post("/booking/expire-unpaid");

export const getBookingDetailService = (bookingId: number) =>
  request.get(`/booking/detail/${bookingId}`);

export const updateBookingStatusService = (payload: BookingStatusUpdate) =>
  request.patch("/booking/status", payload);

export const updateBookingDetailService = (payload: BookingAdminUpdate) =>
  request.patch("/booking/detail", payload);

export const getPaymentListService = (payload: PaymentAdminQuery) =>
  request.post("/payment/list", payload);

export const getPaymentSummaryService = (payload: PaymentAdminQuery) =>
  request.post("/payment/summary", payload);

export const createManualPaymentService = (payload: ManualPaymentCreate) =>
  request.post("/payment/manual", payload);

export const getPaymentMethodConfigListService = () =>
  request.get("/payment/methods");

export const updatePaymentMethodConfigListService = (payload: PaymentMethodConfig[]) =>
  request.put("/payment/methods", payload);

export const getNotificationListService = (payload: NotificationAdminQuery) =>
  request.post("/notification/list", payload);

export const updateNotificationStatusService = (payload: NotificationStatusUpdate) =>
  request.patch("/notification/status", payload);

export const sendQueuedNotificationService = (payload: NotificationSendQueuedPayload = {}) =>
  request.post("/notification/send-queued", payload);

export const createDepartureReminderService = (payload: NotificationReminderCreatePayload = {}) =>
  request.post("/notification/departure-reminders", payload);

// 交接備註獨立於金額更新，現場人員不需取得收款權限。
export const updateBookingNoteService = (payload: { bookingId: number; adminNote: string }) =>
  request.patch("/booking/note", payload);

export const updateBookingStayService = (payload: { bookingId: number; action: 'check_in' | 'check_out'; expectedVersion: number }) =>
  request.patch('/booking/stay', payload);

// 日期範圍由後端篩選，完整讀取分頁，避免第一批 500 筆被誤當成完整營運名單。
export const getBookingOperationRowsService = async (startDate: string, endDate: string): Promise<BookingAdminRow[]> => {
  const rows: BookingAdminRow[] = [];
  const seen = new Set<number>();
  let expectedTotal: number | undefined;
  for (let pageNum = 1; ; pageNum++) {
    const response = await getBookingListService({ pageNum, pageSize: 200, startDate, endDate });
    const items: BookingAdminRow[] = response.data.items || [];
    if (expectedTotal === undefined) expectedTotal = response.data.total;
    if (response.data.total !== expectedTotal || items.some(item => seen.has(item.id))) throw new Error('預約資料已變動，請重新載入');
    for (const item of items) seen.add(item.id);
    rows.push(...items);
    if (rows.length >= response.data.total) return rows;
    if (!items.length) throw new Error('預約資料已變動，請重新載入');
  }
};

export const getNotificationQueueService = () => request.get("/notification/queue");
export const getNotificationAttemptsService = (id: number) => request.get(`/notification/${id}/attempts`);
export const simulateQueuedNotificationService = (payload: NotificationSendQueuedPayload = {}) => request.post("/notification/simulate-queued", payload);
