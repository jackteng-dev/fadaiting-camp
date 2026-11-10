package com.fadaiting.camp.utils;

import java.util.Map;
import java.util.Set;

/** 已登記入住之後，完成訂單必須經由退房流程。 */
public final class BookingStatePolicy {
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
        "pending", Set.of("confirmed", "cancelled"),
        "confirmed", Set.of("cancelled"),
        "cancelled", Set.of("pending", "confirmed"),
        "completed", Set.of());
    private BookingStatePolicy() { }
    public static boolean allows(String status, String stayStatus, String nextStatus) {
        return "not_checked_in".equals(stayStatus) && TRANSITIONS.getOrDefault(status, Set.of()).contains(nextStatus);
    }
}
