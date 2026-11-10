package com.fadaiting.camp.service;
import com.fadaiting.camp.pojo.NotificationRecord;
public interface NotificationDeliveryGateway {
    void checkReady(String mode);
    void deliver(NotificationRecord record, String mode);
}
