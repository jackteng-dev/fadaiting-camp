package com.fadaiting.camp.config;
import com.fadaiting.camp.dto.NotificationSendQueuedRequest;
import com.fadaiting.camp.service.NotificationQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
@Configuration @EnableConfigurationProperties(NotificationQueueProperties.class)
public class NotificationQueueScheduler {
    private static final Logger LOG=LoggerFactory.getLogger(NotificationQueueScheduler.class);
    private final NotificationQueueService queue;
    private final NotificationQueueProperties properties;
    public NotificationQueueScheduler(NotificationQueueService queue,NotificationQueueProperties properties) { this.queue=queue; this.properties=properties; }
    @Scheduled(fixedDelayString="${notification.queue.sweep-delay-ms:30000}",initialDelayString="${notification.queue.sweep-delay-ms:30000}")
    public void sweep() {
        if (!properties.isWorkerEnabled() || "disabled".equals(properties.getDeliveryMode())) return;
        try { var request=new NotificationSendQueuedRequest(); request.setLimit(properties.getBatchSize()); queue.dispatch(request,properties.getDeliveryMode(),null,"通知排程"); }
        catch (Exception error) { LOG.warn("通知批次未完成，請核對佇列及資料庫；錯誤類型 {}",error.getClass().getSimpleName()); }
    }
}
