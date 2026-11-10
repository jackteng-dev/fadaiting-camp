package com.fadaiting.camp.dto;

import lombok.Data;
import com.fadaiting.camp.pojo.RefundRecord;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class PaymentFinancialResponse {
    private Integer paymentRecordId;
    private String bookingStatus;
    private String paymentStatus;
    private boolean canRequestRefund;
    private Long operationVersion;
    private Long bookingOperationVersion;
    private String reconciliationStatus;
    private String reconciliationNote;
    private String reconciliationReference;
    private Integer reconciledBy;
    private OffsetDateTime reconciledAt;
    private BigDecimal amount;
    private BigDecimal completedRefundAmount;
    private BigDecimal reservedRefundAmount;
    private BigDecimal availableRefundAmount;
    private BigDecimal ledgerPaidAmount;
    private BigDecimal bookingPaidAmount;
    private List<RefundRecord> refunds;
}
