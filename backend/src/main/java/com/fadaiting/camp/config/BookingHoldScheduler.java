package com.fadaiting.camp.config;

import com.fadaiting.camp.service.BookingHoldService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class BookingHoldScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(BookingHoldScheduler.class);
    private final BookingHoldService holds;
    private final BookingHoldProperties properties;
    public BookingHoldScheduler(BookingHoldService holds, BookingHoldProperties properties) { this.holds = holds; this.properties = properties; }
    @Scheduled(fixedDelayString = "${booking.hold.sweep-delay-ms:60000}", initialDelayString = "${booking.hold.sweep-delay-ms:60000}")
    public void sweep() {
        if (!properties.isEnabled()) return;
        try {
            var result = holds.expireBatch();
            if (result.getExpiredCount() > 0) LOG.info("預約付款逾時取消 {} 筆", result.getExpiredCount());
            if (result.getFailedCount() > 0) LOG.warn("逾時處理失敗 {} 筆，請核對錯誤紀錄", result.getFailedCount());
        } catch (Exception error) {
            LOG.error("預約付款逾時批次失敗，未完成的訂單將於下次重試；請核對庫存與資料庫狀態", error);
        }
    }
}
