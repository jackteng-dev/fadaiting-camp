package com.fadaiting.camp.service.impl;
import com.fadaiting.camp.config.NotificationQueueProperties;
import com.fadaiting.camp.expection.NotificationDeliveryException;
import com.fadaiting.camp.pojo.NotificationRecord;
import com.fadaiting.camp.service.NotificationDeliveryGateway;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.InternetAddress;
@Component public class NotificationDeliveryGatewayImpl implements NotificationDeliveryGateway {
    private final ObjectProvider<JavaMailSender> sender;
    private final NotificationQueueProperties properties;
    private final String fromAddress, fromName;
    public NotificationDeliveryGatewayImpl(ObjectProvider<JavaMailSender> sender, NotificationQueueProperties properties,
            @Value("${notification.mail.from-address:}") String fromAddress, @Value("${notification.mail.from-name:發呆亭露營區}") String fromName) {
        this.sender=sender; this.properties=properties; this.fromAddress=fromAddress; this.fromName=fromName;
    }
    @Override public void checkReady(String mode) {
        if ("simulate".equals(mode)) return;
        if (!"smtp".equals(mode) || !"smtp".equals(properties.getDeliveryMode()) || sender.getIfAvailable()==null || !validAddress(fromAddress))
            throw new IllegalStateException("真實寄信尚未啟用或設定未完成，請使用免費模擬處理");
    }
    private boolean validAddress(String value) {
        if (value==null || value.isBlank() || value.contains("\r") || value.contains("\n")) return false;
        try { InternetAddress address=new InternetAddress(value,true); address.validate(); return address.getAddress().contains("@"); }
        catch (Exception error) { return false; }
    }
    @Override public void deliver(NotificationRecord record, String mode) {
        checkReady(mode);
        if (!validAddress(record.getRecipient())) throw new NotificationDeliveryException("INVALID_RECIPIENT",false);
        if (record.getSubject()==null || record.getSubject().contains("\r") || record.getSubject().contains("\n")) throw new NotificationDeliveryException("INVALID_CONTENT",false);
        if ("simulate".equals(mode)) return;
        SimpleMailMessage message=new SimpleMailMessage();
        message.setFrom(fromName+" <"+fromAddress+">"); message.setTo(record.getRecipient()); message.setSubject(record.getSubject()); message.setText(record.getMessage());
        try { sender.getObject().send(message); }
        catch (MailAuthenticationException error) { throw new NotificationDeliveryException("SMTP_AUTH",false); }
        catch (MailParseException error) { throw new NotificationDeliveryException("INVALID_CONTENT",false); }
        catch (MailException error) { throw new NotificationDeliveryException("SMTP_UNAVAILABLE",true); }
    }
}
