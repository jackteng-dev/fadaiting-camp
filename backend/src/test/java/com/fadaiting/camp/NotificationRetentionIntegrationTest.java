package com.fadaiting.camp;

import com.fadaiting.camp.config.NotificationRetentionProperties;
import com.fadaiting.camp.dto.NotificationRetentionPreviewDto;
import com.fadaiting.camp.service.NotificationRetentionService;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="CAMP_NOTIFICATION_INTEGRATION",matches="true")
@SpringBootTest(properties={"booking.hold.enabled=false","notification.queue.worker-enabled=false",
    "JWT_SECRET=通知保留測試專用金鑰123456789012345678901234567890"})
@Import(NotificationRetentionIntegrationTest.Fixtures.class)
class NotificationRetentionIntegrationTest {
    @TestConfiguration static class Fixtures {
        @Bean @Primary Clock retentionClock() { return Clock.fixed(Instant.parse("2030-01-01T16:30:00Z"),ZoneId.of("Asia/Taipei")); }
    }
    @Autowired NotificationRetentionService service;
    @Autowired NotificationRetentionProperties properties;
    @Autowired JdbcTemplate db;
    @Autowired Clock clock;
    private int bookingId;
    private NotificationRetentionPreviewDto baseline;
    private LocalDateTime cutoff;
    @BeforeEach void seed() {
        properties.setPreviewDays(180);baseline=service.preview();cutoff=baseline.getCutoffAt();
        bookingId=db.queryForObject("""
            insert into booking(booking_no,camp_site_id,check_in_date,check_out_date,status,created_time,modified_time)
            select ?,id,date '2020-01-01',date '2020-01-02','cancelled',timestamp '2020-01-01',timestamp '2020-01-02'
            from camp_site order by id limit 1 returning id
            """,Integer.class,"NR"+UUID.randomUUID().toString().replace("-","").substring(0,20));
    }
    @AfterEach void cleanup() { db.update("delete from booking where id=?",bookingId);properties.setPreviewDays(180); }
    private int notification(String status) {
        return db.queryForObject("""
            insert into notification_record(booking_id,channel,event_type,status,recipient,message,payload,created_time,modified_time)
            values(?,'internal','admin_new_booking',?,'retention-private@example.invalid','保留私人內文','私人 payload',timestamp '2020-01-01',timestamp '2020-01-02') returning id
            """,Integer.class,bookingId,status);
    }
    private long count(NotificationRetentionPreviewDto result,String reason) {
        return result.getBuckets().stream().filter(b->reason.equals(b.getReason())).mapToLong(b->b.getCount()).sum();
    }
    private long delta(String reason) { return count(service.preview(),reason)-count(baseline,reason); }
    private void attempt(int id,String code) {
        db.update("""
            insert into notification_attempt(notification_id,attempt_number,lease_token,delivery_mode,status,error_code,started_at,finished_at)
            values(?,1,?,'simulate','simulated',?,timestamp with time zone '2020-01-01 00:00:00+08',timestamp with time zone '2020-01-01 00:00:01+08')
            """,id,UUID.randomUUID(),code);
    }
    @Test void onlyLowRiskTerminalHistoryBecomesCandidate() {
        notification("acknowledged");notification("skipped");int id=notification("simulated");
        db.update("update notification_record set channel='manual',provider='manual' where id=?",id);
        assertEquals(3,delta("CANDIDATE"));var result=service.preview();
        assertEquals(3,result.getCandidateCount()-baseline.getCandidateCount());
        assertEquals(result.getTotalCount(),result.getCandidateCount()+result.getProtectedCount());
        assertTrue(result.isDryRun());assertFalse(result.isDeletionEnabled());
    }
    @Test void pendingProcessingFailedRetryAndUnknownStatusesRemainProtected() {
        for (String status:List.of("pending","queued","processing","retry_wait","failed","lease_expired","future_status")) notification(status);
        assertEquals(7,delta("UNRESOLVED_STATUS"));assertEquals(0,delta("CANDIDATE"));
    }
    @Test void uncertainCurrentOrHistoricalDeliveryAlwaysRemainsProtected() {
        int current=notification("skipped");db.update("update notification_record set error_code='DELIVERY_UNCERTAIN' where id=?",current);
        int historical=notification("acknowledged");attempt(historical,"DELIVERY_UNCERTAIN");
        assertEquals(2,delta("DELIVERY_UNCERTAIN"));assertEquals(0,delta("CANDIDATE"));
    }
    @Test void queueIdentityAndAttemptAuditCannotBecomeCandidates() {
        int keyed=notification("skipped"),managed=notification("simulated"),audited=notification("acknowledged");
        db.update("update notification_record set event_key=? where id=?","retention:"+UUID.randomUUID(),keyed);
        db.update("update notification_record set queue_managed=true where id=?",managed);attempt(audited,null);
        int missingHistory=notification("skipped");db.update("update notification_record set attempt_count=1 where id=?",missingHistory);
        assertEquals(2,delta("QUEUE_IDENTITY"));assertEquals(2,delta("ATTEMPT_HISTORY"));assertEquals(0,delta("CANDIDATE"));
    }
    @Test void emailSentSmtpAndUnknownProvidersPreserveDeliveryAudit() {
        int email=notification("skipped"),smtp=notification("acknowledged"),provider=notification("simulated");notification("sent");
        db.update("update notification_record set channel='email' where id=?",email);
        db.update("update notification_record set delivery_mode='smtp' where id=?",smtp);
        db.update("update notification_record set provider='unknown-provider' where id=?",provider);
        int error=notification("skipped");db.update("update notification_record set error_code='UNKNOWN_ERROR' where id=?",error);
        int message=notification("acknowledged");db.update("update notification_record set error_message='待核對' where id=?",message);
        assertEquals(6,delta("DELIVERY_AUDIT"));assertEquals(0,delta("CANDIDATE"));
    }
    @Test void financialOrUnknownEventsAndAnyPaymentRecordPreserveAudit() {
        int event=notification("skipped"),unknown=notification("acknowledged");
        db.update("update notification_record set event_type='payment_confirmed' where id=?",event);
        db.update("update notification_record set event_type='unknown-event' where id=?",unknown);
        assertEquals(2,delta("FINANCIAL_AUDIT"));
        notification("skipped");db.update("insert into payment_record(booking_id,provider,status,merchant_order_no) values(?,'mock','pending',?)",bookingId,"NRPAY"+UUID.randomUUID().toString().replace("-",""));
        assertEquals(3,delta("FINANCIAL_AUDIT"));assertEquals(0,delta("CANDIDATE"));
    }
    @Test void openBookingOrOutstandingLeasePreservesHistory() {
        int id=notification("skipped");db.update("update notification_record set lease_token=? where id=?",UUID.randomUUID(),id);
        assertEquals(1,delta("SCHEDULE_OR_LEASE"));
        db.update("update booking set status='confirmed' where id=?",bookingId);
        assertEquals(1,delta("BOOKING_OPEN"));assertEquals(0,delta("CANDIDATE"));
    }
    @Test void exactCutoffIsProtectedAndOnlyStrictlyOlderActivityQualifies() {
        int boundary=notification("acknowledged"),older=notification("skipped"),sent=notification("simulated");
        db.update("update notification_record set modified_time=? where id=?",cutoff,boundary);
        db.update("update notification_record set modified_time=? where id=?",cutoff.minusSeconds(1),older);
        db.update("update notification_record set sent_time=? where id=?",cutoff,sent);
        assertEquals(2,delta("RECENT_ACTIVITY"));assertEquals(1,delta("CANDIDATE"));
        db.update("update booking set modified_time=? where id=?",cutoff,bookingId);
        assertEquals(3,delta("RECENT_ACTIVITY"));assertEquals(0,delta("CANDIDATE"));
    }
    @Test void bookingCheckoutAndPaidBalanceAlsoPreventCandidates() {
        notification("skipped");db.update("update booking set check_out_date=? where id=?",cutoff.plusDays(1).toLocalDate(),bookingId);
        assertEquals(1,delta("RECENT_ACTIVITY"));db.update("update booking set paid_amount=1 where id=?",bookingId);
        assertEquals(1,delta("FINANCIAL_AUDIT"));
    }
    @Test void previewIsReadOnlyPrivateAndUsesSingleClockAndConfigurableDays() throws Exception {
        int id=notification("skipped");attempt(id,null);
        var records=db.queryForList("select * from notification_record where booking_id=?",bookingId);
        var attempts=db.queryForList("select * from notification_attempt where notification_id=?",id);
        var timeline=db.queryForList("select * from booking_timeline where booking_id=?",bookingId);
        var result=service.preview();service.preview();
        assertEquals(records,db.queryForList("select * from notification_record where booking_id=?",bookingId));
        assertEquals(attempts,db.queryForList("select * from notification_attempt where notification_id=?",id));
        assertEquals(timeline,db.queryForList("select * from booking_timeline where booking_id=?",bookingId));
        assertEquals(clock.instant(),result.getObservedAt().toInstant());assertEquals("Asia/Taipei",result.getTimeZone());
        properties.setPreviewDays(365);assertEquals(365,service.preview().getPreviewDays());
        assertEquals(LocalDateTime.now(clock).minusDays(365),service.preview().getCutoffAt());
        String json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValueAsString(result);
        for (String value:List.of("retention-private","私人","payload","recipient","leaseToken")) assertFalse(json.contains(value));
    }
}
