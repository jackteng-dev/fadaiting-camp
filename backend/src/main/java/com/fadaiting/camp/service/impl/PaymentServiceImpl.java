package com.fadaiting.camp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fadaiting.camp.config.EcpayProperties;
import com.fadaiting.camp.config.LinePayProperties;
import com.fadaiting.camp.dto.PublicPaymentConfirmRequest;
import com.fadaiting.camp.dto.PublicPaymentConfirmResponse;
import com.fadaiting.camp.dto.PublicPaymentCancelRequest;
import com.fadaiting.camp.dto.PublicPaymentFailRequest;
import com.fadaiting.camp.dto.PublicPaymentCreateRequest;
import com.fadaiting.camp.dto.PublicPaymentCreateResponse;
import com.fadaiting.camp.dto.PublicPaymentRetryRequest;
import com.fadaiting.camp.dto.ManualPaymentCreateRequest;
import com.fadaiting.camp.dto.PaymentAdminQueryDto;
import com.fadaiting.camp.dto.PaymentAdminRowDto;
import com.fadaiting.camp.dto.PaymentAdminSummaryDto;
import com.fadaiting.camp.mapper.BookingMapper;
import com.fadaiting.camp.mapper.PaymentMapper;
import com.fadaiting.camp.pojo.Booking;
import com.fadaiting.camp.pojo.BookingGuest;
import com.fadaiting.camp.pojo.BookingTimeline;
import com.fadaiting.camp.pojo.PaymentRecord;
import com.fadaiting.camp.pojo.PaymentMethodConfig;
import com.fadaiting.camp.pojo.PageBean;
import com.fadaiting.camp.service.PaymentService;
import com.fadaiting.camp.service.NotificationService;
import com.fadaiting.camp.utils.ListPageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.math.RoundingMode;

@Service
public class PaymentServiceImpl implements PaymentService {
    private static final Duration PENDING_PAYMENT_TTL = Duration.ofMinutes(30);

    @Autowired
    private BookingMapper bookingMapper;
    @Autowired private com.fadaiting.camp.service.BookingHoldService bookingHoldService;
    @Autowired private com.fadaiting.camp.service.CampAccessService campAccessService;
    @Autowired private com.fadaiting.camp.mapper.RefundMapper refundMapper;
    @Autowired
    private PaymentMapper paymentMapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private LinePayProperties linePayProperties;
    @Autowired
    private EcpayProperties ecpayProperties;
    @Autowired
    private NotificationService notificationService;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    @Transactional
    public PageBean<PaymentAdminRowDto> getAdminPayments(PaymentAdminQueryDto queryDto) {
        expireStalePendingPayments(null);
        String bookingNo = normalize(queryDto.getBookingNo());
        String provider = normalize(queryDto.getProvider());
        String status = normalize(queryDto.getStatus());
        String merchantOrderNo = normalize(queryDto.getMerchantOrderNo());
        String startDate = normalize(queryDto.getStartDate());
        String endDate = normalize(queryDto.getEndDate());
        ListPageUtil<PaymentAdminRowDto> pageUtil = new ListPageUtil<>(
                paymentMapper.findAdminPayments(bookingNo, provider, status, merchantOrderNo, startDate, endDate),
                queryDto.getPageNum(),
                queryDto.getPageSize()
        );
        return new PageBean<>((long) pageUtil.getTotalCount(), pageUtil.getPageList());
    }

    @Override
    @Transactional
    public List<PaymentAdminSummaryDto> getAdminPaymentSummary(PaymentAdminQueryDto queryDto) {
        expireStalePendingPayments(null);
        String bookingNo = normalize(queryDto.getBookingNo());
        String provider = normalize(queryDto.getProvider());
        String status = normalize(queryDto.getStatus());
        String merchantOrderNo = normalize(queryDto.getMerchantOrderNo());
        String startDate = normalize(queryDto.getStartDate());
        String endDate = normalize(queryDto.getEndDate());
        return paymentMapper.findAdminPaymentSummary(bookingNo, provider, status, merchantOrderNo, startDate, endDate);
    }

    @Override
    public List<PaymentMethodConfig> getAdminPaymentMethods() {
        return paymentMapper.findPaymentMethodConfigs();
    }

    @Override
    @Transactional
    public List<PaymentMethodConfig> updateAdminPaymentMethods(List<PaymentMethodConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            throw new IllegalArgumentException("缺少支付方式設定");
        }
        configs.forEach(config -> {
            PaymentMethodConfig normalized = normalizePaymentMethodConfig(config);
            PaymentMethodConfig existing = paymentMapper.findPaymentMethodConfigByProvider(normalized.getProvider());
            if (existing == null) {
                throw new IllegalArgumentException("不支援的支付方式：" + normalized.getProvider());
            }
            paymentMapper.updatePaymentMethodConfig(normalized);
        });
        return paymentMapper.findPaymentMethodConfigs();
    }

    @Override
    public List<PaymentMethodConfig> getPublicPaymentMethods() {
        return paymentMapper.findActivePaymentMethodConfigs();
    }

    @Override
    @Transactional
    public PaymentRecord createManualPayment(ManualPaymentCreateRequest request, Integer actorUserId, String actorName) {
        if (!"ADMIN".equals(campAccessService.currentRole(actorUserId))) throw new org.springframework.security.access.AccessDeniedException("僅管理員可補登款項");
        Booking booking = requirePayableBooking(request.getBookingId());
        if (!"ADMIN".equals(campAccessService.currentRole(actorUserId))) throw new org.springframework.security.access.AccessDeniedException("無權限補登款項");
        expireStalePendingPayments(booking.getId());

        BigDecimal remaining = calculateRemainingAmount(booking);
        BigDecimal amount = request.getAmount() == null ? BigDecimal.ZERO : request.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("付款金額需大於 0");
        }
        if (amount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException("補登金額不可超過未收金額");
        }

        LocalDateTime now = LocalDateTime.now();
        String paymentMethod = normalize(request.getPaymentMethod());
        if (!hasText(paymentMethod)) {
            paymentMethod = "manual";
        }

        PaymentRecord record = new PaymentRecord();
        record.setBookingId(booking.getId());
        record.setProvider("manual");
        record.setPaymentMethod(paymentMethod.toLowerCase(Locale.ROOT));
        record.setStatus("paid");
        record.setAmount(amount);
        record.setCurrency("TWD");
        record.setMerchantOrderNo(generateManualMerchantOrderNo(booking.getBookingNo()));
        record.setPaymentNo("MANUAL-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(now));
        record.setCallbackPayload(buildManualPaymentPayload(request.getNote(), actorName));
        record.setPaidTime(now);
        record.setCreatedTime(now);
        record.setModifiedTime(now);
        paymentMapper.insertPaymentRecord(record);

        syncBookingPaidAmount(booking.getId());
        appendManualPaymentTimeline(record, actorUserId, actorName, request.getNote());
        return record;
    }

    @Override
    @Transactional
    public PublicPaymentCreateResponse createPublicPayment(PublicPaymentCreateRequest request) {
        Booking booking = requirePayableBooking(request.getBookingId());
        expireStalePendingPayments(booking.getId());
        return createPaymentForBooking(booking, request.getProvider(), request.getPaymentMethod(), true);
    }

    @Override
    @Transactional
    public PublicPaymentCreateResponse retryPublicPayment(PublicPaymentRetryRequest request) {
        Booking booking = requirePayableBooking(requirePublicPaymentBooking(request).getId());
        expireStalePendingPayments(booking.getId());
        expirePendingPayments(booking.getId());
        return createPaymentForBooking(booking, request.getProvider(), request.getPaymentMethod(), false);
    }

    private PublicPaymentCreateResponse createPaymentForBooking(
            Booking booking,
            String providerValue,
            String paymentMethodValue,
            boolean reusePendingPayment
    ) {
        BigDecimal remaining = calculateRemainingAmount(booking);

        String provider = normalizeProvider(providerValue);
        String paymentMethod = resolveEnabledPaymentMethod(provider, paymentMethodValue);

        if (reusePendingPayment && shouldReusePendingPayment(provider)) {
            PaymentRecord existingRecord = paymentMapper.findLatestPendingByBookingIdAndProvider(
                    booking.getId(),
                    provider,
                    paymentMethod
            );
            if (canReusePendingPayment(existingRecord, remaining)) {
                return buildCreateResponse(existingRecord, buildProviderMessage(provider));
            }
        }

        String merchantOrderNo = generateMerchantOrderNo(booking.getBookingNo(), provider);
        LocalDateTime now = LocalDateTime.now();

        PaymentRecord record = new PaymentRecord();
        record.setBookingId(booking.getId());
        record.setProvider(provider);
        record.setPaymentMethod(paymentMethod);
        record.setStatus("pending");
        record.setAmount(remaining);
        record.setCurrency("TWD");
        record.setMerchantOrderNo(merchantOrderNo);
        record.setCreatedTime(now);
        record.setModifiedTime(now);
        paymentMapper.insertPaymentRecord(record);

        Map<String, String> formFields = Collections.emptyMap();
        String redirectMethod = "get";
        if ("linepay".equals(provider) && isLinePayConfigured()) {
            createLinePayRequest(record, booking);
        } else if ("ecpay".equals(provider) && isEcpayConfigured()) {
            formFields = createEcpayCheckout(record, booking);
            redirectMethod = "post";
        } else {
            applyMockPaymentRequest(record, booking);
        }
        paymentMapper.updatePaymentRecord(record);

        return buildCreateResponse(record, redirectMethod, formFields, buildProviderMessage(provider));
    }

    @Override
    @Transactional
    public PublicPaymentConfirmResponse confirmPublicPayment(PublicPaymentConfirmRequest request) {
        PaymentRecord record = resolvePaymentRecordForConfirm(request);
        if (record == null) {
            throw new IllegalArgumentException("找不到付款紀錄");
        }
        Booking booking = bookingMapper.findBookingForUpdate(record.getBookingId());
        bookingHoldService.expireLockedBooking(booking);
        expireStalePendingPayments(record.getBookingId());
        record = paymentMapper.findById(record.getId());
        if ("cancelled".equals(booking.getStatus())) return buildPaymentResponse(record, "預約已取消，請重新查詢日期");
        if (!"pending".equals(record.getStatus())) {
            if ("paid_review".equals(record.getStatus())) return buildPaymentResponse(record, "收款待營區人工核對，請聯絡營區確認；預約不會因此自動恢復");
            if ("expired".equals(record.getStatus())) {
                return buildPaymentResponse(record, "此付款紀錄已逾時，請重新建立付款");
            }
            return buildPaymentResponse(record, "此付款紀錄目前不需再次確認");
        }

        if (record.getAmount().compareTo(booking.getTotalAmount().subtract(booking.getPaidAmount())) > 0) {
            record.setStatus("expired"); paymentMapper.updatePaymentRecord(record);
            return buildPaymentResponse(record, "已收金額已更新，請重新建立付款");
        }
        if ("ecpay".equals(record.getProvider()) && isEcpayConfigured()) {
            throw new IllegalArgumentException("綠界正式付款須等待 callback 通知，不可由前台直接確認");
        }
        if ("linepay".equals(record.getProvider()) && isLinePayConfigured()) {
            confirmLinePay(record);
        } else {
            record.setStatus("paid");
            record.setPaidTime(LocalDateTime.now());
            record.setCallbackPayload(mergePayload(record.getCallbackPayload(), "{\"mode\":\"mock-confirm\"}"));
            paymentMapper.updatePaymentRecord(record);
        }
        syncBookingPaidAmount(record.getBookingId());
        appendPaymentTimeline(record);

        return buildPaymentResponse(record, "付款確認流程已完成");
    }

    @Override
    @Transactional
    public PublicPaymentConfirmResponse cancelPublicPayment(PublicPaymentCancelRequest request) {
        PaymentRecord record = paymentMapper.findById(request.getPaymentRecordId());
        if (record == null) {
            throw new IllegalArgumentException("找不到付款紀錄");
        }
        bookingMapper.findBookingForUpdate(record.getBookingId());
        record = paymentMapper.findById(record.getId());
        if ("pending".equals(record.getStatus())) {
            record.setStatus("cancelled");
            record.setCallbackPayload(mergePayload(record.getCallbackPayload(), "{\"mode\":\"payment-cancelled\"}"));
            paymentMapper.updatePaymentRecord(record);
            appendPaymentCancelledTimeline(record);
        }

        return buildPaymentResponse(record, "付款取消狀態已同步");
    }

    @Override
    @Transactional
    public PublicPaymentConfirmResponse failPublicPayment(PublicPaymentFailRequest request) {
        PaymentRecord record = paymentMapper.findById(request.getPaymentRecordId());
        if (record == null) {
            throw new IllegalArgumentException("找不到付款紀錄");
        }
        bookingMapper.findBookingForUpdate(record.getBookingId());
        record = paymentMapper.findById(record.getId());
        if ("pending".equals(record.getStatus())) {
            record.setStatus("failed");
            record.setCallbackPayload(mergePayload(record.getCallbackPayload(), "{\"mode\":\"payment-failed\"}"));
            paymentMapper.updatePaymentRecord(record);
            appendPaymentFailedTimeline(record);
        }

        return buildPaymentResponse(record, "付款失敗狀態已同步");
    }

    @Override
    @Transactional
    public String handleEcpayCallback(Map<String, String> callbackParams) {
        if (!isEcpayConfigured()) {
            throw new IllegalArgumentException("ECPay is not configured");
        }
        processEcpayResult(callbackParams);
        return "1|OK";
    }

    @Override
    public String resolveEcpayReturnRedirectUrl(Map<String, String> callbackParams) {
        try {
            if (!isEcpayConfigured()) throw new IllegalArgumentException("綠界尚未設定");
            if (!isValidEcpayCheckMacValue(callbackParams) || !ecpayProperties.getMerchantId().equals(callbackParams.get("MerchantID")))
                throw new IllegalArgumentException("綠界導回驗證失敗");
            PaymentRecord record = paymentMapper.findByMerchantOrderNo(callbackParams.get("MerchantTradeNo"));
            if (record == null || !"ecpay".equals(record.getProvider())) {
                return buildEcpayResultPageUrl("/payment/fail", null, "", null);
            }
            validateEcpayCallbackAmount(record, callbackParams.get("TradeAmt"));
            // 瀏覽器導回僅導頁，入帳只接受已驗簽的伺服器 callback。
            String path = "1".equals(callbackParams.get("RtnCode")) ? "/payment/confirm" : "/payment/fail";
            Booking booking = bookingMapper.findBookingById(record.getBookingId());
            String bookingNo = booking == null ? "" : booking.getBookingNo();
            return buildEcpayResultPageUrl(path, record.getId(), bookingNo, record.getBookingId());
        } catch (Exception e) {
            return buildEcpayResultPageUrl("/payment/fail", null, "", null);
        }
    }

    private Booking requirePayableBooking(Integer bookingId) {
        Booking booking = bookingMapper.findBookingForUpdate(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("找不到訂單資料");
        }
        if ("cancelled".equals(booking.getStatus())) {
            throw new IllegalArgumentException("此訂單已取消，無法建立付款");
        }
        if (bookingHoldService.isDue(booking)) throw new com.fadaiting.camp.expection.BookingConflictException("付款期限已到，請重新查詢預約");
        if (refundMapper.countCompletedForBooking(bookingId) > 0) throw new com.fadaiting.camp.expection.BookingConflictException("此訂單已有退款，請先人工核對或另建預約，不再建立付款連結");
        return booking;
    }

    private Booking requirePublicPaymentBooking(PublicPaymentRetryRequest request) {
        Booking booking = bookingMapper.findBookingByBookingNo(normalize(request.getBookingNo()));
        if (booking == null) {
            throw new IllegalArgumentException("查無符合資料，請確認訂單編號與聯絡電話");
        }
        BookingGuest guest = bookingMapper.findBookingGuestByBookingId(booking.getId());
        if (guest == null || !normalizePhone(guest.getContactPhone()).equals(normalizePhone(request.getContactPhone()))) {
            throw new IllegalArgumentException("查無符合資料，請確認訂單編號與聯絡電話");
        }
        if ("cancelled".equals(booking.getStatus())) {
            throw new IllegalArgumentException("此訂單已取消，無法重新付款");
        }
        return booking;
    }

    private PaymentRecord resolvePaymentRecordForConfirm(PublicPaymentConfirmRequest request) {
        if (request.getPaymentRecordId() != null) {
            return paymentMapper.findById(request.getPaymentRecordId());
        }
        String merchantOrderNo = normalize(request.getMerchantOrderNo());
        if (hasText(merchantOrderNo)) {
            return paymentMapper.findByMerchantOrderNo(merchantOrderNo);
        }
        String transactionId = normalize(request.getTransactionId());
        if (hasText(transactionId)) {
            return paymentMapper.findByProviderTransactionId(transactionId);
        }
        throw new IllegalArgumentException("缺少付款確認資訊");
    }

    private void expirePendingPayments(Integer bookingId) {
        paymentMapper.findPendingByBookingId(bookingId).forEach(record -> {
            record.setStatus("expired");
            record.setCallbackPayload(mergePayload(record.getCallbackPayload(), "{\"mode\":\"payment-expired-by-retry\"}"));
            paymentMapper.updatePaymentRecord(record);
            appendPaymentExpiredTimeline(record);
        });
    }

    private boolean canReusePendingPayment(PaymentRecord record, BigDecimal remainingAmount) {
        return record != null
                && "pending".equals(record.getStatus())
                && record.getAmount() != null
                && record.getAmount().compareTo(remainingAmount) == 0
                && hasText(record.getPaymentUrl());
    }

    private PublicPaymentCreateResponse buildCreateResponse(PaymentRecord record, String message) {
        return buildCreateResponse(record, resolveRedirectMethod(record), Collections.emptyMap(), message);
    }

    private PublicPaymentCreateResponse buildCreateResponse(
            PaymentRecord record,
            String redirectMethod,
            Map<String, String> formFields,
            String message
    ) {
        return new PublicPaymentCreateResponse(
                record.getId(),
                record.getProvider(),
                record.getPaymentMethod(),
                record.getStatus(),
                record.getMerchantOrderNo(),
                record.getAmount(),
                record.getCurrency(),
                record.getPaymentUrl(),
                redirectMethod,
                formFields == null ? Collections.emptyMap() : formFields,
                message
        );
    }

    private PublicPaymentConfirmResponse buildPaymentResponse(PaymentRecord record, String message) {
        Booking booking = bookingMapper.findBookingById(record.getBookingId());
        return new PublicPaymentConfirmResponse(
                record.getId(),
                record.getBookingId(),
                booking == null ? "" : booking.getBookingNo(),
                record.getProvider(),
                record.getStatus(),
                record.getProviderTransactionId(),
                record.getAmount(),
                message
        );
    }

    private BigDecimal calculateRemainingAmount(Booking booking) {
        BigDecimal total = booking.getTotalAmount() == null ? BigDecimal.ZERO : booking.getTotalAmount();
        BigDecimal paid = booking.getPaidAmount() == null ? BigDecimal.ZERO : booking.getPaidAmount();
        BigDecimal remaining = total.subtract(paid);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("此訂單目前無待付款金額");
        }
        return remaining;
    }

    private String normalizeProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if (!"ecpay".equals(normalized) && !"linepay".equals(normalized)) {
            throw new IllegalArgumentException("目前僅支援 ecpay 與 linepay 的付款骨架");
        }
        return normalized;
    }

    private PaymentMethodConfig normalizePaymentMethodConfig(PaymentMethodConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("支付方式設定不可為空");
        }
        String provider = normalizeProvider(config.getProvider());
        String paymentMethod = normalizePaymentMethod(provider, config.getPaymentMethod());
        PaymentMethodConfig normalized = new PaymentMethodConfig();
        normalized.setProvider(provider);
        normalized.setDisplayName(hasText(config.getDisplayName()) ? config.getDisplayName().trim() : defaultPaymentDisplayName(provider));
        normalized.setDescription(hasText(config.getDescription()) ? config.getDescription().trim() : "");
        normalized.setPaymentMethod(paymentMethod);
        normalized.setSortOrder(config.getSortOrder() == null ? defaultPaymentSortOrder(provider) : config.getSortOrder());
        normalized.setIsActive(Boolean.TRUE.equals(config.getIsActive()));
        return normalized;
    }

    private String resolveEnabledPaymentMethod(String provider, String paymentMethodValue) {
        PaymentMethodConfig config = paymentMapper.findPaymentMethodConfigByProvider(provider);
        if (config == null || !Boolean.TRUE.equals(config.getIsActive())) {
            throw new IllegalArgumentException("此付款方式目前未開放");
        }
        String requestedMethod = hasText(paymentMethodValue)
                ? paymentMethodValue.trim().toLowerCase(Locale.ROOT)
                : config.getPaymentMethod();
        String enabledMethod = normalizePaymentMethod(provider, config.getPaymentMethod());
        String normalizedRequested = normalizePaymentMethod(provider, requestedMethod);
        if (!enabledMethod.equals(normalizedRequested)) {
            throw new IllegalArgumentException("此付款方式目前未開放");
        }
        return enabledMethod;
    }

    private String normalizePaymentMethod(String provider, String paymentMethod) {
        String normalized = hasText(paymentMethod)
                ? paymentMethod.trim().toLowerCase(Locale.ROOT)
                : defaultPaymentMethod(provider);
        if ("linepay".equals(provider)
                && !"web".equals(normalized)
                && !"app".equals(normalized)
                && !"universal".equals(normalized)) {
            throw new IllegalArgumentException("LINE Pay 僅支援 web、app、universal 付款模式");
        }
        if ("ecpay".equals(provider)
                && !"all".equals(normalized)
                && !"credit".equals(normalized)
                && !"atm".equals(normalized)
                && !"cvs".equals(normalized)
                && !"barcode".equals(normalized)) {
            throw new IllegalArgumentException("綠界僅支援 all、credit、atm、cvs、barcode 付款模式");
        }
        return normalized;
    }

    private String defaultPaymentMethod(String provider) {
        return "ecpay".equals(provider) ? "all" : "web";
    }

    private String defaultPaymentDisplayName(String provider) {
        return "ecpay".equals(provider) ? "綠界科技 ECPay" : "LINE Pay";
    }

    private Integer defaultPaymentSortOrder(String provider) {
        return "ecpay".equals(provider) ? 2 : 1;
    }

    private PaymentRecord processEcpayResult(Map<String, String> callbackParams) {
        if (!isValidEcpayCheckMacValue(callbackParams)) {
            throw new IllegalArgumentException("CheckMacValue Error");
        }
        if (!ecpayProperties.getMerchantId().equals(callbackParams.get("MerchantID"))) throw new IllegalArgumentException("MerchantID mismatch");

        String merchantOrderNo = callbackParams.get("MerchantTradeNo");
        PaymentRecord record = paymentMapper.findByMerchantOrderNo(merchantOrderNo);
        if (record == null || !"ecpay".equals(record.getProvider())) {
            throw new IllegalArgumentException("Payment record not found");
        }

        validateEcpayCallbackAmount(record, callbackParams.get("TradeAmt"));
        Booking booking = bookingMapper.findBookingForUpdate(record.getBookingId());
        bookingHoldService.expireLockedBooking(booking);
        record = paymentMapper.findById(record.getId());
        record.setCallbackPayload(mergePayload(record.getCallbackPayload(), safeJson(callbackParams)));
        record.setProviderTransactionId(callbackParams.getOrDefault("TradeNo", ""));
        record.setPaymentNo(callbackParams.getOrDefault("PaymentNo", ""));

        if ("1".equals(callbackParams.get("SimulatePaid"))) {
            paymentMapper.updatePaymentRecord(record);
            return record;
        }

        if ("1".equals(callbackParams.get("RtnCode"))) {
            if ("paid_review".equals(record.getStatus())) return record;
            if (!"paid".equals(record.getStatus())) {
                BigDecimal remaining = booking.getTotalAmount().subtract(booking.getPaidAmount());
                if ("cancelled".equals(booking.getStatus()) || !"pending".equals(record.getStatus()) || record.getAmount().compareTo(remaining) > 0) {
                    record.setStatus("paid_review"); record.setPaidTime(LocalDateTime.now());
                    paymentMapper.updatePaymentRecord(record);
                    BookingTimeline timeline = new BookingTimeline(); timeline.setBookingId(booking.getId());
                    timeline.setActionType("payment_review_required"); timeline.setActionLabel("收款待人工核對");
                    timeline.setActorName("綠界"); timeline.setDetail("付款在預約取消、付款失效或金額變動後到達，請核對收款與退款；不自動恢復預約。");
                    timeline.setCreatedTime(LocalDateTime.now()); bookingMapper.insertBookingTimeline(timeline);
                    return record;
                }
                record.setStatus("paid");
                record.setPaidTime(LocalDateTime.now());
                paymentMapper.updatePaymentRecord(record);
                syncBookingPaidAmount(record.getBookingId());
                appendPaymentTimeline(record);
            } else {
                paymentMapper.updatePaymentRecord(record);
            }
            return record;
        }

        if ("pending".equals(record.getStatus())) {
            record.setStatus("failed");
            paymentMapper.updatePaymentRecord(record);
            appendPaymentFailedTimeline(record);
        } else {
            paymentMapper.updatePaymentRecord(record);
        }
        return record;
    }

    private String generateMerchantOrderNo(String bookingNo, String provider) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        if ("ecpay".equals(provider)) {
            return "EC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + suffix.substring(0, 4);
        }
        String prefix = bookingNo == null ? "BOOKING" : bookingNo.replace("-", "");
        prefix = prefix.length() > 20 ? prefix.substring(0, 20) : prefix;
        return prefix + suffix;
    }

    private String generateManualMerchantOrderNo(String bookingNo) {
        String compactBookingNo = bookingNo == null ? "BK" : bookingNo.replaceAll("[^A-Za-z0-9]", "");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        String value = "MN" + compactBookingNo + suffix;
        return value.substring(0, Math.min(50, value.length()));
    }

    private String buildManualPaymentPayload(String note, String actorName) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("mode", "manual-payment");
        payload.put("actorName", actorName == null ? "" : actorName);
        payload.put("note", note == null ? "" : note.trim());
        return safeJson(payload);
    }

    private Map<String, String> createEcpayCheckout(PaymentRecord record, Booking booking) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("MerchantID", ecpayProperties.getMerchantId());
        fields.put("MerchantTradeNo", record.getMerchantOrderNo());
        fields.put("MerchantTradeDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
        fields.put("PaymentType", "aio");
        fields.put("TotalAmount", toEcpayAmount(record.getAmount()));
        fields.put("TradeDesc", "Fa Dai Ting Camp Booking");
        fields.put("ItemName", "Fa Dai Ting Camp Booking " + booking.getBookingNo());
        fields.put("ReturnURL", ecpayProperties.getReturnUrl());
        fields.put("ChoosePayment", resolveEcpayPaymentMethod(record.getPaymentMethod()));
        fields.put("EncryptType", "1");
        if (ecpayProperties.getOrderResultUrl() != null && !ecpayProperties.getOrderResultUrl().isBlank()) {
            fields.put("OrderResultURL", ecpayProperties.getOrderResultUrl());
        }
        fields.put("CheckMacValue", generateEcpayCheckMacValue(fields));

        record.setPaymentUrl(ecpayProperties.getCheckoutUrl());
        record.setCallbackPayload(safeJson(fields));
        return fields;
    }

    private void createLinePayRequest(PaymentRecord record, Booking booking) {
        String apiPath = "/v2/payments/request";
        String body = buildLinePayRequestBody(record, booking);
        JsonNode response = linePayPost(apiPath, body);
        validateLinePaySuccess(response);

        JsonNode info = response.path("info");
        record.setProviderTransactionId(info.path("transactionId").asText(""));
        String paymentUrl = resolveLinePayPaymentUrl(info.path("paymentUrl"), record.getPaymentMethod());
        record.setPaymentUrl(paymentUrl);
        record.setCallbackPayload(safeJson(response));
    }

    private void confirmLinePay(PaymentRecord record) {
        if (record.getProviderTransactionId() == null || record.getProviderTransactionId().isBlank()) {
            throw new IllegalArgumentException("LINE Pay 付款紀錄缺少 transactionId，無法確認付款");
        }
        String apiPath = "/v2/payments/" + record.getProviderTransactionId() + "/confirm";
        String body = String.format(
                Locale.ROOT,
                "{\"amount\":%s,\"currency\":\"%s\"}",
                toLinePayAmount(record.getAmount()),
                record.getCurrency()
        );
        JsonNode response = linePayPost(apiPath, body);
        validateLinePaySuccess(response);

        record.setStatus("paid");
        record.setPaidTime(LocalDateTime.now());
        record.setCallbackPayload(mergePayload(record.getCallbackPayload(), safeJson(response)));
        paymentMapper.updatePaymentRecord(record);
    }

    private void syncBookingPaidAmount(Integer bookingId) {
        BigDecimal paidAmount = paymentMapper.sumPaidAmountByBookingId(bookingId);
        bookingMapper.updateBookingPaidAmount(bookingId, paidAmount == null ? BigDecimal.ZERO : paidAmount);
    }

    private void appendPaymentTimeline(PaymentRecord record) {
        Booking booking = bookingMapper.findBookingById(record.getBookingId());
        BookingGuest guest = bookingMapper.findBookingGuestByBookingId(record.getBookingId());
        if (booking != null) {
            notificationService.queuePaymentConfirmedNotification(booking, guest, record);
        }
        BookingTimeline timeline = new BookingTimeline();
        timeline.setBookingId(record.getBookingId());
        timeline.setActionType("payment_confirmed");
        timeline.setActionLabel("確認付款");
        timeline.setActorName(resolvePaymentActor(record.getProvider()));
        timeline.setDetail(String.format(
                "付款方式 %s，金額 %s，付款紀錄 #%d。",
                translateProvider(record.getProvider()),
                record.getAmount(),
                record.getId()
        ));
        timeline.setCreatedTime(LocalDateTime.now());
        bookingMapper.insertBookingTimeline(timeline);
    }

    private void appendPaymentCancelledTimeline(PaymentRecord record) {
        BookingTimeline timeline = new BookingTimeline();
        timeline.setBookingId(record.getBookingId());
        timeline.setActionType("payment_cancelled");
        timeline.setActionLabel("取消付款");
        timeline.setActorName("網站訪客");
        timeline.setDetail(String.format(
                "取消 %s 付款請求，金額 %s，付款紀錄 #%d。",
                translateProvider(record.getProvider()),
                record.getAmount(),
                record.getId()
        ));
        timeline.setCreatedTime(LocalDateTime.now());
        bookingMapper.insertBookingTimeline(timeline);
    }

    private void appendPaymentFailedTimeline(PaymentRecord record) {
        BookingTimeline timeline = new BookingTimeline();
        timeline.setBookingId(record.getBookingId());
        timeline.setActionType("payment_failed");
        timeline.setActionLabel("付款失敗");
        timeline.setActorName("付款系統");
        timeline.setDetail(String.format(
                "%s 付款未完成，金額 %s，付款紀錄 #%d。",
                translateProvider(record.getProvider()),
                record.getAmount(),
                record.getId()
        ));
        timeline.setCreatedTime(LocalDateTime.now());
        bookingMapper.insertBookingTimeline(timeline);
    }

    private void appendPaymentExpiredTimeline(PaymentRecord record) {
        BookingTimeline timeline = new BookingTimeline();
        timeline.setBookingId(record.getBookingId());
        timeline.setActionType("payment_expired");
        timeline.setActionLabel("付款逾時");
        timeline.setActorName("付款系統");
        timeline.setDetail(String.format(
                "%s 付款請求已逾時，金額 %s，付款紀錄 #%d。",
                translateProvider(record.getProvider()),
                record.getAmount(),
                record.getId()
        ));
        timeline.setCreatedTime(LocalDateTime.now());
        bookingMapper.insertBookingTimeline(timeline);
    }

    private void appendManualPaymentTimeline(PaymentRecord record, Integer actorUserId, String actorName, String note) {
        Booking booking = bookingMapper.findBookingById(record.getBookingId());
        BookingGuest guest = bookingMapper.findBookingGuestByBookingId(record.getBookingId());
        if (booking != null) {
            notificationService.queuePaymentConfirmedNotification(booking, guest, record);
        }
        BookingTimeline timeline = new BookingTimeline();
        timeline.setBookingId(record.getBookingId());
        timeline.setActionType("manual_payment");
        timeline.setActionLabel("補登付款");
        timeline.setActorUserId(actorUserId);
        timeline.setActorName(hasText(actorName) ? actorName : "後台管理員");
        String detail = String.format(
                "後台補登付款，方式 %s，金額 %s，付款紀錄 #%d。",
                record.getPaymentMethod(),
                record.getAmount(),
                record.getId()
        );
        if (hasText(note)) {
            detail += " 備註：" + note.trim();
        }
        timeline.setDetail(detail);
        timeline.setCreatedTime(LocalDateTime.now());
        bookingMapper.insertBookingTimeline(timeline);
    }

    private JsonNode linePayPost(String apiPath, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(trimTrailingSlash(linePayProperties.getApiBaseUrl()) + apiPath))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("X-LINE-ChannelId", linePayProperties.getChannelId())
                    .header("X-LINE-ChannelSecret", linePayProperties.getChannelSecret())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            if (linePayProperties.getDeviceProfileId() != null && !linePayProperties.getDeviceProfileId().isBlank()) {
                builder.header("X-LINE-MerchantDeviceProfileId", linePayProperties.getDeviceProfileId());
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new IllegalStateException("LINE Pay API 呼叫失敗: " + e.getMessage(), e);
        }
    }

    private void validateLinePaySuccess(JsonNode response) {
        String returnCode = response.path("returnCode").asText("");
        if (!"0000".equals(returnCode)) {
            String returnMessage = response.path("returnMessage").asText("LINE Pay 處理失敗");
            throw new IllegalArgumentException("LINE Pay 回傳失敗: " + returnCode + " " + returnMessage);
        }
    }

    private String buildLinePayRequestBody(PaymentRecord record, Booking booking) {
        String confirmUrl = trimTrailingSlash(linePayProperties.getConfirmUrlBase()) + "?paymentRecordId=" + record.getId();
        String cancelUrl = appendQueryParams(
                linePayProperties.getCancelUrl(),
                Map.of(
                        "paymentRecordId", String.valueOf(record.getId()),
                        "bookingNo", booking.getBookingNo()
                )
        );
        return String.format(
                Locale.ROOT,
                "{\"productName\":\"%s\",\"amount\":%s,\"currency\":\"%s\",\"orderId\":\"%s\",\"confirmUrl\":\"%s\",\"cancelUrl\":\"%s\",\"checkConfirmUrlBrowser\":true}",
                escapeJson("發呆亭露營區訂單 " + booking.getBookingNo()),
                toLinePayAmount(record.getAmount()),
                record.getCurrency(),
                record.getMerchantOrderNo(),
                escapeJson(confirmUrl),
                escapeJson(cancelUrl)
        );
    }

    private void applyMockPaymentRequest(PaymentRecord record, Booking booking) {
        record.setProviderTransactionId("mock-" + UUID.randomUUID().toString().replace("-", ""));
        record.setPaymentUrl(buildPlaceholderPaymentUrl(record.getProvider(), record.getPaymentMethod(), record.getMerchantOrderNo()));
        record.setCallbackPayload(String.format(
                "{\"mode\":\"mock-request\",\"bookingId\":%d,\"bookingNo\":\"%s\"}",
                booking.getId(),
                booking.getBookingNo()
        ));
    }

    private String buildPlaceholderPaymentUrl(String provider, String paymentMethod, String merchantOrderNo) {
        return String.format("/payment/checkout/%s/%s/%s", provider, paymentMethod, merchantOrderNo);
    }

    private String buildProviderMessage(String provider) {
        if ("linepay".equals(provider) && isLinePayConfigured()) {
            return "已建立 LINE Pay 正式付款請求，可進一步接前台導轉與 confirm 流程。";
        }
        if ("ecpay".equals(provider) && isEcpayConfigured()) {
            return "已建立綠界付款表單，前台將以 POST 導轉至綠界付款頁。";
        }
        if ("ecpay".equals(provider)) {
            return "目前為綠界 mock 付款模式，可先驗證前台付款流程。";
        }
        return "目前為 mock 付款模式，可先串前台流程與後台付款紀錄。";
    }

    private String resolveRedirectMethod(PaymentRecord record) {
        if (record == null) {
            return "get";
        }
        if ("ecpay".equals(record.getProvider()) && isEcpayConfigured()) {
            return "post";
        }
        return "get";
    }

    private String buildEcpayResultPageUrl(String path, Integer paymentRecordId, String bookingNo, Integer bookingId) {
        String baseUrl = trimTrailingSlash(ecpayProperties.getResultPageBaseUrl());
        if (!hasText(baseUrl)) {
            baseUrl = "http://127.0.0.1:5173";
        }
        Map<String, String> params = new LinkedHashMap<>();
        if (paymentRecordId != null) {
            params.put("paymentRecordId", String.valueOf(paymentRecordId));
        }
        if (hasText(bookingNo)) {
            params.put("bookingNo", bookingNo);
        }
        if (bookingId != null) {
            params.put("bookingId", String.valueOf(bookingId));
        }
        return appendQueryParams(baseUrl + path, params);
    }

    private String resolvePaymentActor(String provider) {
        if ("linepay".equals(provider)) {
            return "LINE Pay";
        }
        if ("ecpay".equals(provider)) {
            return "綠界";
        }
        return "付款系統";
    }

    private String translateProvider(String provider) {
        if ("linepay".equals(provider)) {
            return "LINE Pay";
        }
        if ("ecpay".equals(provider)) {
            return "綠界";
        }
        return provider;
    }

    private boolean isLinePayConfigured() {
        return linePayProperties.isEnabled()
                && linePayProperties.getChannelId() != null
                && !linePayProperties.getChannelId().isBlank()
                && linePayProperties.getChannelSecret() != null
                && !linePayProperties.getChannelSecret().isBlank()
                && linePayProperties.getApiBaseUrl() != null
                && !linePayProperties.getApiBaseUrl().isBlank();
    }

    private boolean isEcpayConfigured() {
        return ecpayProperties.isEnabled()
                && hasText(ecpayProperties.getMerchantId())
                && hasText(ecpayProperties.getHashKey())
                && hasText(ecpayProperties.getHashIv())
                && hasText(ecpayProperties.getCheckoutUrl())
                && hasText(ecpayProperties.getReturnUrl());
    }

    private boolean isValidEcpayCheckMacValue(Map<String, String> callbackParams) {
        String received = callbackParams.get("CheckMacValue");
        if (!hasText(received)) {
            return false;
        }
        Map<String, String> params = new LinkedHashMap<>(callbackParams);
        params.remove("CheckMacValue");
        return received.equalsIgnoreCase(generateEcpayCheckMacValue(params));
    }

    private String generateEcpayCheckMacValue(Map<String, String> params) {
        try {
            Map<String, String> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            sorted.putAll(params);
            StringBuilder source = new StringBuilder("HashKey=").append(ecpayProperties.getHashKey());
            sorted.forEach((key, value) -> source.append("&").append(key).append("=").append(value == null ? "" : value));
            source.append("&HashIV=").append(ecpayProperties.getHashIv());

            String encoded = URLEncoder.encode(source.toString(), StandardCharsets.UTF_8)
                    .replace("%2D", "-")
                    .replace("%5F", "_")
                    .replace("%2E", ".")
                    .replace("%21", "!")
                    .replace("%2A", "*")
                    .replace("%28", "(")
                    .replace("%29", ")")
                    .toLowerCase(Locale.ROOT);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(encoded.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("綠界檢查碼產生失敗: " + e.getMessage(), e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02X", value));
        }
        return result.toString();
    }

    private String resolveEcpayPaymentMethod(String paymentMethod) {
        if ("credit".equalsIgnoreCase(paymentMethod)) return "Credit";
        if ("atm".equalsIgnoreCase(paymentMethod)) return "ATM";
        if ("cvs".equalsIgnoreCase(paymentMethod)) return "CVS";
        if ("barcode".equalsIgnoreCase(paymentMethod)) return "BARCODE";
        return "ALL";
    }

    private String toEcpayAmount(BigDecimal amount) {
        try {
            return amount.setScale(0, RoundingMode.UNNECESSARY).toPlainString();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("綠界付款金額必須為整數新台幣");
        }
    }

    private boolean shouldReusePendingPayment(String provider) {
        return "linepay".equals(provider);
    }

    @Override
    @Transactional
    public void expireStalePendingPayments(Integer bookingId) {
        LocalDateTime createdBefore = LocalDateTime.now().minus(PENDING_PAYMENT_TTL);
        paymentMapper.findStalePendingPayments(bookingId, createdBefore).stream()
                .sorted(java.util.Comparator.comparing(PaymentRecord::getBookingId).thenComparing(PaymentRecord::getId)).forEach(candidate -> {
            bookingMapper.findBookingForUpdate(candidate.getBookingId());
            PaymentRecord record = paymentMapper.findById(candidate.getId());
            if (!"pending".equals(record.getStatus()) || !record.getCreatedTime().isBefore(createdBefore)) return;
            record.setStatus("expired");
            record.setCallbackPayload(mergePayload(record.getCallbackPayload(), "{\"mode\":\"payment-expired-by-timeout\"}"));
            paymentMapper.updatePaymentRecord(record);
            appendPaymentExpiredTimeline(record);
        });
    }

    private String toLinePayAmount(BigDecimal amount) {
        try {
            return amount.setScale(0, RoundingMode.UNNECESSARY).toPlainString();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("LINE Pay 付款金額必須為整數新台幣");
        }
    }

    private void validateEcpayCallbackAmount(PaymentRecord record, String tradeAmount) {
        if (!hasText(tradeAmount)) {
            throw new IllegalArgumentException("TradeAmt is required");
        }
        BigDecimal callbackAmount = new BigDecimal(tradeAmount);
        if (record.getAmount().compareTo(callbackAmount) != 0) {
            throw new IllegalArgumentException("TradeAmt mismatch");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String resolveLinePayPaymentUrl(JsonNode paymentUrlNode, String paymentMethod) {
        if ("app".equals(paymentMethod) && paymentUrlNode.hasNonNull("app")) {
            return paymentUrlNode.path("app").asText();
        }
        if ("universal".equals(paymentMethod) && paymentUrlNode.hasNonNull("universal")) {
            return paymentUrlNode.path("universal").asText();
        }
        if (paymentUrlNode.hasNonNull("web")) {
            return paymentUrlNode.path("web").asText();
        }
        return paymentUrlNode.toString();
    }

    private String safeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String safeJson(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String mergePayload(String left, String right) {
        if (left == null || left.isBlank()) {
            return right;
        }
        return left + "\n" + right;
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String appendQueryParams(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return url == null ? "" : url;
        }
        StringBuilder result = new StringBuilder(url == null ? "" : url);
        result.append(result.indexOf("?") >= 0 ? "&" : "?");
        params.forEach((key, value) -> result
                .append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append("=")
                .append(URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8))
                .append("&"));
        result.setLength(result.length() - 1);
        return result.toString();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizePhone(String value) {
        String normalized = normalize(value);
        return normalized == null ? "" : normalized.replaceAll("\\D", "");
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
