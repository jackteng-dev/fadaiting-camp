package com.fadaiting.camp;

import com.fadaiting.camp.mapper.AvailabilityMapper;
import com.fadaiting.camp.mapper.CampZoneMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression: on PostgreSQL a bare "#{excludeId} is null" with a null parameter fails with
 * "could not determine data type of parameter", which broke creating availability rows and zones.
 */
@EnabledIfEnvironmentVariable(named = "CAMP_NOTIFICATION_INTEGRATION", matches = "true")
@SpringBootTest(properties = {"booking.hold.enabled=false", "notification.queue.worker-enabled=false",
        "JWT_SECRET=排除編號回歸測試專用金鑰123456789012345678901234567890"})
class NullableExcludeIdIntegrationTest {
    @Autowired AvailabilityMapper availability;
    @Autowired CampZoneMapper zones;

    @Test
    void availabilityDuplicateCheckAcceptsNullExcludeId() {
        LocalDate farFuture = LocalDate.of(2099, 1, 1);
        assertEquals(0, availability.countByUnitAndDate(-1, farFuture, null));
        assertEquals(0, availability.countByUnitAndDate(-1, farFuture, 123));
    }

    @Test
    void zoneSlugCheckAcceptsNullExcludeId() {
        assertEquals(0, zones.countBySlug(-1, "no-such-zone-slug", null));
        assertEquals(0, zones.countBySlug(-1, "no-such-zone-slug", 123));
    }
}
