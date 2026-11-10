package com.fadaiting.camp;

import com.fadaiting.camp.config.NotificationQueueProperties;
import com.fadaiting.camp.dto.*;
import com.fadaiting.camp.expection.*;
import com.fadaiting.camp.mapper.*;
import com.fadaiting.camp.pojo.*;
import com.fadaiting.camp.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="CAMP_NOTIFICATION_INTEGRATION",matches="true")
@SpringBootTest(properties={"booking.hold.enabled=false","notification.queue.worker-enabled=false","JWT_SECRET=通知隔離測試專用金鑰123456789012345678901234567890"})
@Import(NotificationQueueIntegrationTest.Fixtures.class)
class NotificationQueueIntegrationTest {
    static class TestClock extends Clock {
        final AtomicReference<Instant> now=new AtomicReference<>();
        public ZoneId getZone() { return ZoneId.of("Asia/Taipei"); }
        public Clock withZone(ZoneId zone) { return Clock.fixed(instant(),zone); }
        public Instant instant() { return now.get(); }
        void advance(long seconds) { now.updateAndGet(i -> i.plusSeconds(seconds)); }
    }
    static class SyntheticDelivery implements NotificationDeliveryGateway {
        final AtomicInteger calls=new AtomicInteger();
        volatile int transientFailures=0; volatile boolean permanent=false;
        volatile CountDownLatch entered, release;
        public void checkReady(String mode) { }
        public void deliver(NotificationRecord record,String mode) {
            int attempt=calls.incrementAndGet();
            if (entered!=null) { entered.countDown(); try { assertTrue(release.await(5,TimeUnit.SECONDS)); } catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new RuntimeException(); } }
            if (permanent) throw new NotificationDeliveryException("INVALID_RECIPIENT",false);
            if (attempt<=transientFailures) throw new NotificationDeliveryException("SMTP_UNAVAILABLE",true);
        }
    }
    @TestConfiguration static class Fixtures {
        @Bean @Primary TestClock testClock() { return new TestClock(); }
        @Bean @Primary SyntheticDelivery syntheticDelivery() { return new SyntheticDelivery(); }
    }
    @Autowired NotificationQueueService queue;
    @Autowired NotificationService events;
    @Autowired NotificationMapper notifications;
    @Autowired BookingMapper bookings;
    @Autowired JdbcTemplate db;
    @Autowired TestClock clock;
    @Autowired SyntheticDelivery delivery;
    @Autowired PlatformTransactionManager manager;
    @Autowired NotificationQueueProperties properties;
    private int bookingId;
    private String bookingNo;
    @BeforeEach void seed() {
        clock.now.set(Instant.parse("2030-01-01T16:30:00Z")); delivery.calls.set(0); delivery.transientFailures=0; delivery.permanent=false; delivery.entered=null; delivery.release=null;
        bookingNo="NQ"+UUID.randomUUID().toString().replace("-","").substring(0,20);
        bookingId=db.queryForObject("insert into booking(booking_no,camp_site_id,check_in_date,check_out_date,status) select ?,id,date '2030-01-05',date '2030-01-06','confirmed' from camp_site order by id limit 1 returning id",Integer.class,bookingNo);
        db.update("insert into booking_guest(booking_id,contact_name,contact_phone,contact_email) values(?,'通知合成','0999000222','synthetic@example.invalid')",bookingId);
    }
    @AfterEach void cleanup() {
        if (delivery.release!=null) delivery.release.countDown();
        db.update("delete from booking where id=?",bookingId);
        properties.setWorkerEnabled(false); properties.setDeliveryMode("disabled"); properties.setOverdueThresholdSeconds(900);
    }
    private void enqueue() { events.queueBookingSubmittedNotification(bookings.findBookingById(bookingId),bookings.findBookingGuestByBookingId(bookingId)); }
    private NotificationRecord record() { return notifications.findById(db.queryForObject("select id from notification_record where booking_id=? and channel='email' order by id limit 1",Integer.class,bookingId)); }
    private NotificationSendResultDto dispatch(String mode) { var request=new NotificationSendQueuedRequest(); request.setLimit(1); request.setBookingNo(bookingNo); return queue.dispatch(request,mode,null,"合成測試"); }
    private NotificationStatusUpdateRequest action(NotificationRecord row,String status) { var r=new NotificationStatusUpdateRequest();r.setNotificationId(row.getId());r.setExpectedVersion(row.getOperationVersion());r.setStatus(status);r.setReason("合成資料人工核對");return r; }

    @Test void outboxDeduplicatesBookingAndEachPayment() {
        enqueue(); enqueue(); assertEquals(2,db.queryForObject("select count(*) from notification_record where booking_id=?",Integer.class,bookingId));
        PaymentRecord payment=new PaymentRecord();payment.setId(987001);payment.setProvider("mock");payment.setAmount(java.math.BigDecimal.TEN);
        events.queuePaymentConfirmedNotification(bookings.findBookingById(bookingId),bookings.findBookingGuestByBookingId(bookingId),payment);
        events.queuePaymentConfirmedNotification(bookings.findBookingById(bookingId),bookings.findBookingGuestByBookingId(bookingId),payment);
        assertEquals(4,db.queryForObject("select count(*) from notification_record where booking_id=?",Integer.class,bookingId));
    }
    @Test void businessRollbackLeavesNoNotificationOrHistory() {
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).executeWithoutResult(tx->{enqueue();throw new IllegalStateException("合成交易故障");}));
        assertEquals(0,db.queryForObject("select count(*) from notification_record where booking_id=?",Integer.class,bookingId));
        assertEquals(0,db.queryForObject("select count(*) from booking_timeline where booking_id=?",Integer.class,bookingId));
    }
    @Test void simulationNeverCountsAsSentAndHasOneAttempt() {
        enqueue();var result=dispatch("simulate");assertEquals(1,result.getSimulatedCount());assertEquals(0,result.getSentCount());
        var row=record();assertEquals("simulated",row.getStatus());assertNull(row.getSentTime());assertNull(row.getLeaseToken());assertEquals(1,queue.attempts(row.getId()).size());
        assertEquals(0,dispatch("simulate").getProcessedCount());
    }
    @Test void transientFailureWaitsBeforeRetryAndThenSucceeds() {
        enqueue();delivery.transientFailures=1;assertEquals(1,dispatch("simulate").getRetryCount());
        var row=record();assertEquals(OffsetDateTime.now(clock).plusSeconds(60).toInstant(),row.getNextAttemptAt().toInstant());assertEquals(0,dispatch("simulate").getProcessedCount());
        clock.advance(60);assertEquals(1,dispatch("simulate").getSimulatedCount());assertEquals(2,queue.attempts(row.getId()).size());
    }
    @Test void retryBudgetStopsAndManualRetryPreservesAttemptHistory() {
        enqueue();delivery.transientFailures=99;
        for(int n=1;n<=5;n++) {dispatch("simulate");if(n<5)clock.advance(properties.retryDelay(n));}
        var row=record();assertEquals("failed",row.getStatus());assertEquals(5,row.getAttemptCount());assertEquals(0,dispatch("simulate").getProcessedCount());
        var request=action(row,"queued");queue.updateStatus(request,null,"測試");assertThrows(BookingConflictException.class,()->queue.updateStatus(request,null,"測試"));
        assertEquals(10,record().getAttemptLimit());delivery.transientFailures=0;assertEquals(1,dispatch("simulate").getSimulatedCount());assertEquals(6,queue.attempts(row.getId()).size());
    }
    @Test void permanentFailureIsNotAutomaticallyRetried() { enqueue();delivery.permanent=true;assertEquals(1,dispatch("simulate").getFailedCount());clock.advance(9999);assertEquals(0,dispatch("simulate").getProcessedCount()); }
    @Test void twoWorkersClaimOnlyOneCopy() throws Exception {
        enqueue();delivery.entered=new CountDownLatch(1);delivery.release=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var first=executor.submit(()->dispatch("simulate"));assertTrue(delivery.entered.await(3,TimeUnit.SECONDS));
            assertEquals(0,executor.submit(()->dispatch("simulate")).get(3,TimeUnit.SECONDS).getProcessedCount());
            delivery.release.countDown();assertEquals(1,first.get(3,TimeUnit.SECONDS).getSimulatedCount());
        }
        assertEquals(1,delivery.calls.get());assertEquals(1,queue.attempts(record().getId()).size());
    }
    @Test void expiredSimulationRecoversWithFencingAgainstOldWorker() throws Exception {
        enqueue();delivery.entered=new CountDownLatch(1);delivery.release=new CountDownLatch(1);
        try(var executor=Executors.newSingleThreadExecutor()) {
            var old=executor.submit(()->dispatch("simulate"));assertTrue(delivery.entered.await(3,TimeUnit.SECONDS));clock.advance(120);
            assertEquals(1,queue.recoverExpiredLeases());delivery.release.countDown();assertEquals(0,old.get(3,TimeUnit.SECONDS).getSimulatedCount());
        }
        assertEquals("retry_wait",record().getStatus());clock.advance(60);delivery.entered=null;assertEquals(1,dispatch("simulate").getSimulatedCount());
        assertEquals("lease_expired",queue.attempts(record().getId()).get(1).getStatus());
    }
    @Test void expiredSmtpQuarantinesUncertainDelivery() throws Exception {
        enqueue();delivery.entered=new CountDownLatch(1);delivery.release=new CountDownLatch(1);
        try(var executor=Executors.newSingleThreadExecutor()) {
            var old=executor.submit(()->dispatch("smtp"));assertTrue(delivery.entered.await(3,TimeUnit.SECONDS));clock.advance(120);queue.recoverExpiredLeases();delivery.release.countDown();old.get(3,TimeUnit.SECONDS);
        }
        assertEquals("failed",record().getStatus());assertEquals("DELIVERY_UNCERTAIN",record().getErrorCode());clock.advance(9999);assertEquals(0,dispatch("smtp").getProcessedCount());
    }
    @Test void historyFailureKeepsClaimAndLaterQuarantinesSmtp() {
        enqueue();String function="cloud_notification_failure_"+bookingId;
        db.execute("create function "+function+"() returns trigger language plpgsql as $$ begin if NEW.booking_id="+bookingId+" and NEW.action_type='notification_updated' then raise exception '合成歷程故障'; end if; return NEW; end $$");
        db.execute("create trigger "+function+" before insert on booking_timeline for each row execute function "+function+"()");
        try { assertThrows(Exception.class,()->dispatch("smtp"));assertEquals("processing",record().getStatus());assertEquals("processing",queue.attempts(record().getId()).getFirst().getStatus()); }
        finally { db.execute("drop trigger "+function+" on booking_timeline");db.execute("drop function "+function+"()"); }
        clock.advance(120);queue.recoverExpiredLeases();assertEquals("DELIVERY_UNCERTAIN",record().getErrorCode());assertEquals(1,delivery.calls.get());
    }
    @Test void finishedEmailCannotBeFalsifiedOrRequeued() {
        enqueue();var row=record();assertThrows(BookingConflictException.class,()->queue.updateStatus(action(row,"sent"),null,"測試"));
        dispatch("simulate");assertThrows(BookingConflictException.class,()->queue.updateStatus(action(record(),"queued"),null,"測試"));
    }
    @Test void legacyQueueAndInternalReminderAreNeverDelivered() {
        enqueue();db.update("update notification_record set queue_managed=false where booking_id=? and channel='email'",bookingId);
        assertEquals(0,dispatch("simulate").getProcessedCount());assertEquals(0,delivery.calls.get());
        int internal=db.queryForObject("select id from notification_record where booking_id=? and channel='internal'",Integer.class,bookingId);
        queue.updateStatus(action(notifications.findById(internal),"acknowledged"),null,"測試");assertEquals("acknowledged",notifications.findById(internal).getStatus());
    }
    @Test void cancelledBookingNotificationIsSkippedWithoutDelivery() { enqueue();db.update("update booking set status='cancelled' where id=?",bookingId);assertEquals(1,dispatch("simulate").getSkippedCount());assertEquals(0,delivery.calls.get()); }
    @Test void remindersRequireConfirmedStayAndDeduplicateConcurrently() throws Exception {
        var request=new NotificationReminderCreateRequest();request.setTargetDate("2030-01-05");var barrier=new CyclicBarrier(2);
        try(var executor=Executors.newFixedThreadPool(2)) {
            Callable<NotificationSendResultDto> task=()->{barrier.await();return events.queueDepartureReminders(request,null,"測試");};var a=executor.submit(task);var b=executor.submit(task);var first=a.get();var second=b.get();assertEquals(1,first.getCreatedCount()+second.getCreatedCount());assertEquals(0,first.getSentCount()+second.getSentCount());
        }
        assertEquals(1,db.queryForObject("select count(*) from notification_record where booking_id=? and event_type='departure_reminder'",Integer.class,bookingId));
        db.update("update booking set stay_status='checked_in', checked_in_at=now() where id=?",bookingId);assertEquals(1,dispatch("simulate").getSkippedCount());assertEquals(0,delivery.calls.get());
    }
    @Test void rescheduledReminderIsSkippedRatherThanSendingOldDates() {
        var request=new NotificationReminderCreateRequest();request.setTargetDate("2030-01-05");events.queueDepartureReminders(request,null,"測試");
        db.update("update booking set check_in_date=date '2030-01-06',check_out_date=date '2030-01-07' where id=?",bookingId);
        assertEquals(1,dispatch("simulate").getSkippedCount());assertEquals(0,delivery.calls.get());
    }
    @Test void paginationCountsWithoutLoadingAllRows() {
        enqueue();var request=new NotificationAdminQueryDto();request.setBookingNo(bookingNo);request.setPageSize(1);assertEquals(2,events.getAdminNotifications(request).getTotal());
        request.setPageNum(2);assertEquals(1,events.getAdminNotifications(request).getItems().size());request.setPageNum(3);assertEquals(0,events.getAdminNotifications(request).getItems().size());
    }
    @Test void modesAreNotSilentlySwitchedAfterFirstAttempt() {enqueue();delivery.transientFailures=1;dispatch("smtp");clock.advance(60);assertEquals(0,dispatch("simulate").getProcessedCount());assertEquals(1,dispatch("smtp").getSentCount());}

    private void observationFixture(String channel,boolean managed,String status,OffsetDateTime next,
            OffsetDateTime lease,int attempts,String code) {
        db.update("""
            insert into notification_record(booking_id,channel,event_type,recipient,subject,message,status,
                queue_managed,next_attempt_at,lease_until,lease_token,attempt_count,attempt_limit,error_code)
            values(?,?,'observation_fixture','observation-private@example.invalid','私人測試標題','私人測試內文',
                ?,?,?,?,?,?,5,?)
            """,bookingId,channel,status,managed,next,lease,lease==null ? null : UUID.randomUUID(),attempts,code);
    }
    private boolean hasAlert(NotificationQueueStatusDto status,String code,String severity) {
        return status.getAlerts().stream().anyMatch(a->code.equals(a.getCode()) && severity.equals(a.getSeverity()));
    }

    @Test void observationCountsScopeOverlapsAndNeverMutatesOrDelivers() throws Exception {
        var before=queue.status(); var now=OffsetDateTime.now(clock);
        observationFixture("email",true,"queued",now,null,0,null);
        observationFixture("email",true,"retry_wait",now.minusSeconds(900),null,1,null);
        observationFixture("email",true,"retry_wait",now.plusSeconds(60),null,1,null);
        observationFixture("email",true,"failed",null,null,2,"SMTP_UNAVAILABLE");
        observationFixture("email",true,"failed",null,null,1,"DELIVERY_UNCERTAIN");
        observationFixture("email",true,"processing",null,now.plusSeconds(60),1,null);
        observationFixture("email",true,"processing",null,now,1,null);
        observationFixture("email",true,"queued",now,null,5,null);
        observationFixture("email",true,"retry_wait",null,null,1,null);
        observationFixture("email",true,"processing",null,null,1,null);
        for(String channel:List.of("internal","manual")) observationFixture(channel,true,"queued",now.minusDays(2),null,0,null);
        observationFixture("email",false,"queued",now.minusDays(2),null,0,null);
        for(String status:List.of("sent","simulated","skipped","acknowledged")) observationFixture("email",true,status,now.minusDays(2),null,0,null);
        var records=db.queryForList("select * from notification_record where booking_id=? order by id",bookingId);
        var result=queue.status();
        assertEquals(before.getDueCount()+2,result.getDueCount());
        assertEquals(before.getWaitingCount()+1,result.getWaitingCount());
        assertEquals(before.getProcessingCount()+3,result.getProcessingCount());
        assertEquals(before.getFailedCount()+2,result.getFailedCount());
        assertEquals(before.getUncertainCount()+1,result.getUncertainCount());
        assertEquals(before.getExpiredLeaseCount()+1,result.getExpiredLeaseCount());
        assertEquals(before.getOverdueCount()+1,result.getOverdueCount());
        assertEquals(before.getBlockedCount()+3,result.getBlockedCount());
        assertEquals(now,result.getObservedAt()); assertEquals("critical",result.getHealth());
        assertTrue(hasAlert(result,"DELIVERY_UNCERTAIN","critical")); assertTrue(hasAlert(result,"LEASE_EXPIRED","critical"));
        assertTrue(hasAlert(result,"QUEUE_BLOCKED","warning")); assertTrue(hasAlert(result,"QUEUE_OVERDUE","warning"));
        assertTrue(hasAlert(result,"DELIVERY_FAILED","warning")); assertTrue(hasAlert(result,"WORKER_PAUSED","info"));
        assertEquals(records,db.queryForList("select * from notification_record where booking_id=? order by id",bookingId));
        assertEquals(0,queue.attempts(record().getId()).size()); assertEquals(0,delivery.calls.get());
        var json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValueAsString(result);
        for(String privateValue:List.of("observation-private@example.invalid","私人測試標題","私人測試內文",bookingNo,"leaseToken"))
            assertFalse(json.contains(privateValue));
    }

    @Test void observationThresholdAndLeaseExpiryUseInclusiveBoundaries() {
        var before=queue.status(); var now=OffsetDateTime.now(clock);
        observationFixture("email",true,"queued",now.minusSeconds(899),null,0,null);
        observationFixture("email",true,"processing",null,now.plusSeconds(1),1,null);
        assertEquals(before.getOverdueCount(),queue.status().getOverdueCount());
        assertEquals(before.getExpiredLeaseCount(),queue.status().getExpiredLeaseCount());
        clock.advance(1); var result=queue.status();
        assertEquals(before.getOverdueCount()+1,result.getOverdueCount());
        assertEquals(before.getExpiredLeaseCount()+1,result.getExpiredLeaseCount());
        var oldest=before.getOldestDueAt()==null ? now.minusSeconds(899) : before.getOldestDueAt();
        assertEquals(Duration.between(oldest,OffsetDateTime.now(clock)).getSeconds(),result.getOldestDueAgeSeconds());
        properties.setOverdueThresholdSeconds(1800);
        assertEquals(before.getOverdueCount(),queue.status().getOverdueCount());
        assertEquals(1800,queue.status().getOverdueThresholdSeconds());
    }

    @Test void observationWorkerFlagsDoNotChangeModeOrClaimTasks() {
        var now=OffsetDateTime.now(clock); observationFixture("email",true,"queued",now,null,0,null);
        assertTrue(hasAlert(queue.status(),"WORKER_PAUSED","info"));
        properties.setWorkerEnabled(true);
        assertTrue(hasAlert(queue.status(),"WORKER_PAUSED","info"));
        properties.setDeliveryMode("simulate"); var result=queue.status();
        assertFalse(hasAlert(result,"WORKER_PAUSED","info")); assertEquals("simulate",result.getDeliveryMode());
        assertEquals(0,delivery.calls.get()); assertEquals("queued",record().getStatus());
    }

    @Test void observationEmptyScopeHasNoInventedTimesOrAlerts() {
        // 交易內暫時隱藏既有任務，rollback 保留所有來源資料。
        var transaction=new TransactionTemplate(manager);
        transaction.executeWithoutResult(tx->{
            db.update("update notification_record set queue_managed=false where queue_managed");
            var result=queue.status();
            assertEquals(0,result.getDueCount()); assertEquals(0,result.getFailedCount());
            assertNull(result.getOldestDueAt()); assertNull(result.getOldestExpiredLeaseAt());
            assertEquals(0,result.getOldestDueAgeSeconds()); assertEquals("healthy",result.getHealth()); assertTrue(result.getAlerts().isEmpty());
            tx.setRollbackOnly();
        });
    }
}
