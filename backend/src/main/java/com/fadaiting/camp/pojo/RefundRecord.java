package com.fadaiting.camp.pojo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class RefundRecord {
    private Integer id;
    private Integer paymentRecordId;
    private BigDecimal amount;
    private String status;
    private UUID idempotencyKey;
    private String reason;
    private String decisionNote;
    private String externalReference;
    private Integer requestedBy;
    private Integer decidedBy;
    private Integer completedBy;
    private OffsetDateTime requestedAt;
    private OffsetDateTime decidedAt;
    private OffsetDateTime completedAt;
    private Long operationVersion;
}
