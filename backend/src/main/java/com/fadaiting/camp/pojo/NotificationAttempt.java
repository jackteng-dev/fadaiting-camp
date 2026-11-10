package com.fadaiting.camp.pojo;
import lombok.Data;
import java.time.OffsetDateTime;
@Data public class NotificationAttempt {
    private Long id;
    private Integer notificationId, attemptNumber, actorUserId;
    private String deliveryMode, status, errorCode, actorName;
    private OffsetDateTime startedAt, finishedAt;
}
