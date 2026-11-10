package com.fadaiting.camp.service;

import com.fadaiting.camp.config.NotificationQueueProperties;
import com.fadaiting.camp.dto.*;
import com.fadaiting.camp.expection.BookingConflictException;
import com.fadaiting.camp.expection.NotificationDeliveryException;
import com.fadaiting.camp.mapper.BookingMapper;
import com.fadaiting.camp.mapper.NotificationMapper;
import com.fadaiting.camp.pojo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;

@Service
public class NotificationQueueService {
    private final NotificationMapper notifications;
    private final BookingMapper bookings;
    private final NotificationDeliveryGateway delivery;
    private final NotificationQueueProperties properties;
    private final Clock clock;
    private final TransactionTemplate transaction;
    public NotificationQueueService(NotificationMapper notifications, BookingMapper bookings, NotificationDeliveryGateway delivery,
            NotificationQueueProperties properties, Clock clock, PlatformTransactionManager manager) {
        this.notifications=notifications; this.bookings=bookings; this.delivery=delivery; this.properties=properties; this.clock=clock;
        this.transaction=new TransactionTemplate(manager); this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    public NotificationQueueStatusDto status() {
        var now=OffsetDateTime.now(clock);
        var result=notifications.queueStatus(now,now.minusSeconds(properties.getOverdueThresholdSeconds()));
        result.setObservedAt(now);
        result.setOverdueThresholdSeconds(properties.getOverdueThresholdSeconds());
        result.setOldestDueAgeSeconds(result.getOldestDueAt()==null ? 0 :
                Math.max(0,Duration.between(result.getOldestDueAt(),now).getSeconds()));
        result.setDeliveryMode(properties.getDeliveryMode());
        result.setWorkerEnabled(properties.isWorkerEnabled());
        var alerts=new ArrayList<NotificationQueueAlertDto>();
        if (result.getUncertainCount()>0) alerts.add(new NotificationQueueAlertDto("DELIVERY_UNCERTAIN","critical",result.getUncertainCount(),
                "寄送結果不明", "先核對寄件紀錄及嘗試歷程，確認是否已寄達；勿直接重送。"));
        if (result.getExpiredLeaseCount()>0) alerts.add(new NotificationQueueAlertDto("LEASE_EXPIRED","critical",result.getExpiredLeaseCount(),
                "處理租約已到期", "核對 worker 與嘗試歷程；SMTP 可能已寄出，讀取此頁不會回收或重送。"));
        if (result.getBlockedCount()>0) alerts.add(new NotificationQueueAlertDto("QUEUE_BLOCKED","warning",result.getBlockedCount(),
                "任務無法自動領取", "核對重試預算、排程時間與租約資料；不要直接批次修改資料庫。"));
        long ordinaryFailures=result.getFailedCount()-result.getUncertainCount();
        if (ordinaryFailures>0) alerts.add(new NotificationQueueAlertDto("DELIVERY_FAILED","warning",ordinaryFailures,
                "通知發送失敗", "篩選發送失敗，查看錯誤代碼；修正原因並核對寄送紀錄後再決定重新排入或略過。"));
        if (result.getOverdueCount()>0) alerts.add(new NotificationQueueAlertDto("QUEUE_OVERDUE","warning",result.getOverdueCount(),
                "到期任務等待過久", "核對處理模式、worker 狀態與錯誤；各模式不會自動切換，模擬不代表寄信。"));
        if (result.getDueCount()>0 && (!properties.isWorkerEnabled() || "disabled".equals(properties.getDeliveryMode())))
            alerts.add(new NotificationQueueAlertDto("WORKER_PAUSED","info",result.getDueCount(),
                    "自動處理關閉", "目前可由 ADMIN 免費模擬；正式寄信須另驗收設定與送達，不會由告警自行啟用。"));
        result.setAlerts(List.copyOf(alerts));
        result.setHealth(alerts.stream().anyMatch(a->"critical".equals(a.getSeverity())) ? "critical" :
                alerts.stream().anyMatch(a->"warning".equals(a.getSeverity())) ? "warning" :
                alerts.isEmpty() ? "healthy" : "info");
        return result;
    }
    public List<NotificationAttempt> attempts(int id) {
        if (notifications.findById(id)==null) throw new IllegalArgumentException("找不到通知紀錄");
        return notifications.findAttempts(id);
    }
    public NotificationSendResultDto dispatch(NotificationSendQueuedRequest request,String mode,Integer actorId,String actorName) {
        delivery.checkReady(mode);
        int limit=request.getLimit()==null ? properties.getBatchSize() : request.getLimit();
        if (limit<1 || limit>100) throw new IllegalArgumentException("批次筆數須介於 1 到 100");
        String scope=request.getBookingNo()==null || request.getBookingNo().isBlank() ? null : request.getBookingNo().trim();
        if (scope!=null && scope.length()>50) throw new IllegalArgumentException("訂單編號過長");
        recoverExpiredLeases();
        NotificationSendResultDto result=new NotificationSendResultDto(0,0,0,0);
        for (int i=0;i<limit;i++) {
            // 每次只領一筆，寄送在交易外，避免批次尾端任務提早租約到期。
            NotificationRecord record=transaction.execute(tx -> {
                var now=OffsetDateTime.now(clock);
                var claimed=notifications.claim(now,now.plusSeconds(properties.getLeaseSeconds()),UUID.randomUUID(),mode,scope);
                if (claimed!=null) notifications.insertAttempt(claimed,now,actorId,actorName==null ? "通知排程" : actorName);
                return claimed;
            });
            if (record==null) break;
            result.setProcessedCount(result.getProcessedCount()+1);
            String status="simulate".equals(mode) ? "simulated" : "sent", code=null;
            boolean retryable=false;
            if (isObsolete(record)) { status="skipped"; code="OBSOLETE_BOOKING"; }
            else {
                try { delivery.deliver(record,mode); }
                catch (NotificationDeliveryException error) { status="failed"; code=error.getErrorCode(); retryable=error.isRetryable(); }
                catch (Exception error) { status="failed"; code="DELIVERY_UNCERTAIN"; }
            }
            if (retryable && record.getAttemptCount()<record.getAttemptLimit()) status="retry_wait";
            if (!complete(record,status,code,actorId,actorName)) { result.setUnresolvedCount(result.getUnresolvedCount()+1); continue; }
            switch (status) {
                case "sent" -> result.setSentCount(result.getSentCount()+1);
                case "simulated" -> result.setSimulatedCount(result.getSimulatedCount()+1);
                case "retry_wait" -> result.setRetryCount(result.getRetryCount()+1);
                case "skipped" -> result.setSkippedCount(result.getSkippedCount()+1);
                default -> result.setFailedCount(result.getFailedCount()+1);
            }
        }
        return result;
    }
    private boolean isObsolete(NotificationRecord record) {
        Booking booking=bookings.findBookingById(record.getBookingId());
        if (booking==null) return true;
        if ("departure_reminder".equals(record.getEventType())) return !"confirmed".equals(booking.getStatus())
                || !"not_checked_in".equals(booking.getStayStatus()) || booking.getCheckInDate().isBefore(LocalDate.now(clock))
                || !Objects.equals(record.getEventKey(),"booking:"+booking.getId()+":departure:"+booking.getCheckInDate());
        return "booking_submitted".equals(record.getEventType()) && "cancelled".equals(booking.getStatus());
    }
    private boolean complete(NotificationRecord record,String status,String code,Integer actorId,String actorName) {
        return Boolean.TRUE.equals(transaction.execute(tx -> {
            bookings.findBookingForUpdate(record.getBookingId());
            var current=notifications.findForUpdate(record.getId());
            var now=OffsetDateTime.now(clock);
            if (current==null || !"processing".equals(current.getStatus()) || !record.getLeaseToken().equals(current.getLeaseToken())
                    || current.getLeaseUntil()==null || !current.getLeaseUntil().isAfter(now)) return false;
            var next="retry_wait".equals(status) ? now.plusSeconds(properties.retryDelay(record.getAttemptCount())) : null;
            if (notifications.finish(record.getId(),record.getLeaseToken(),status,code,code==null ? null : "通知未完成，請核對錯誤代碼",next,
                    "sent".equals(status) ? LocalDateTime.now(clock) : null)!=1) throw new BookingConflictException("通知領取狀態已變更");
            if (notifications.finishAttempt(record.getLeaseToken(),status,code,now)!=1) throw new BookingConflictException("通知嘗試紀錄已變更");
            appendTimeline(record,actorId,actorName,"通知 #"+record.getId()+" 處理結果："+status+(code==null ? "" : "（"+code+"）"));
            return true;
        }));
    }
    public int recoverExpiredLeases() {
        return Objects.requireNonNull(transaction.execute(tx -> {
            var now=OffsetDateTime.now(clock); int count=0;
            for (var record:notifications.findExpiredLeases(now,100)) {
                boolean safeSimulation="simulate".equals(record.getDeliveryMode());
                String status=safeSimulation && record.getAttemptCount()<record.getAttemptLimit() ? "retry_wait" : "failed";
                String code=safeSimulation ? "LEASE_EXPIRED" : "DELIVERY_UNCERTAIN";
                notifications.finish(record.getId(),record.getLeaseToken(),status,code,"處理租約已到期，請核對嘗試紀錄",
                        "retry_wait".equals(status) ? now.plusSeconds(properties.retryDelay(record.getAttemptCount())) : null,null);
                if (notifications.finishAttempt(record.getLeaseToken(),"lease_expired",code,now)!=1) throw new BookingConflictException("通知嘗試紀錄已變更");
                count++;
            }
            return count;
        }));
    }
    public void updateStatus(NotificationStatusUpdateRequest request,Integer actorId,String actorName) {
        transaction.executeWithoutResult(tx -> {
            var candidate=notifications.findById(request.getNotificationId());
            if (candidate==null) throw new IllegalArgumentException("找不到通知紀錄");
            bookings.findBookingForUpdate(candidate.getBookingId());
            var record=notifications.findForUpdate(candidate.getId());
            if (!Objects.equals(record.getOperationVersion(),request.getExpectedVersion())) throw new BookingConflictException("通知版本已變更，請重新載入");
            String desired=request.getStatus();
            boolean retry="queued".equals(desired) && record.isQueueManaged() && "email".equals(record.getChannel()) && "failed".equals(record.getStatus());
            boolean skip="skipped".equals(desired) && Set.of("queued","retry_wait","failed").contains(record.getStatus());
            boolean ack="acknowledged".equals(desired) && "internal".equals(record.getChannel()) && "queued".equals(record.getStatus());
            if (!retry && !skip && !ack) throw new BookingConflictException("此通知不支援該操作，已完成或處理中的通知不可重新發送");
            int attempts=record.getAttemptCount()==null ? 0 : record.getAttemptCount();
            int budget=retry ? attempts+properties.getMaxAttempts() : record.getAttemptLimit();
            if (notifications.manualUpdate(record.getId(),request.getExpectedVersion(),desired,retry ? OffsetDateTime.now(clock) : null,budget)!=1)
                throw new BookingConflictException("通知版本已變更，請重新載入");
            appendTimeline(record,actorId,actorName,"通知 #"+record.getId()+" 操作："+desired+"；原因："+request.getReason());
        });
    }
    private void appendTimeline(NotificationRecord record,Integer actorId,String actorName,String detail) {
        var timeline=new BookingTimeline(); timeline.setBookingId(record.getBookingId()); timeline.setActionType("notification_updated");
        timeline.setActionLabel("更新通知紀錄"); timeline.setActorUserId(actorId); timeline.setActorName(actorName==null ? "通知排程" : actorName);
        timeline.setDetail(detail); timeline.setCreatedTime(LocalDateTime.now(clock)); bookings.insertBookingTimeline(timeline);
    }
}
