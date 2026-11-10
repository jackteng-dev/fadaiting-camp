package com.fadaiting.camp.service;

import com.fadaiting.camp.config.NotificationRetentionProperties;
import com.fadaiting.camp.dto.NotificationRetentionPreviewDto;
import com.fadaiting.camp.mapper.NotificationMapper;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationRetentionService {
    private final NotificationMapper notifications;
    private final NotificationRetentionProperties properties;
    private final Clock clock;

    public NotificationRetentionService(NotificationMapper notifications,NotificationRetentionProperties properties,Clock clock) {
        this.notifications=notifications;this.properties=properties;this.clock=clock;
    }

    @Transactional(readOnly=true)
    public NotificationRetentionPreviewDto preview() {
        var result=new NotificationRetentionPreviewDto();var now=OffsetDateTime.now(clock);
        var cutoff=now.toLocalDateTime().minusDays(properties.getPreviewDays());
        var buckets=notifications.retentionPreview(cutoff);
        result.setObservedAt(now);result.setCutoffAt(cutoff);result.setTimeZone(clock.getZone().getId());
        result.setPreviewDays(properties.getPreviewDays());result.setBuckets(buckets);
        result.setTotalCount(buckets.stream().mapToLong(b->b.getCount()).sum());
        result.setCandidateCount(buckets.stream().filter(b->"CANDIDATE".equals(b.getReason())).mapToLong(b->b.getCount()).sum());
        result.setProtectedCount(result.getTotalCount()-result.getCandidateCount());
        return result;
    }
}
