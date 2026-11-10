package com.fadaiting.camp.pojo;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class LineUserBinding {
    private Integer id;
    private String lineUserId;
    private String displayName;
    private String pictureUrl;
    private Integer bookingId;
    private String bookingNo;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String status;
    private String bindToken;
    private LocalDateTime bindTokenExpireTime;
    private LocalDateTime boundAt;
    private LocalDateTime lastInteractionTime;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
    private OffsetDateTime identityVerifiedAt;
}
