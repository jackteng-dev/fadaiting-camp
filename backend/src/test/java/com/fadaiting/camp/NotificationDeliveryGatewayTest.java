package com.fadaiting.camp;
import com.fadaiting.camp.config.NotificationQueueProperties;
import com.fadaiting.camp.expection.NotificationDeliveryException;
import com.fadaiting.camp.pojo.NotificationRecord;
import com.fadaiting.camp.service.impl.NotificationDeliveryGatewayImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class NotificationDeliveryGatewayTest {
    private final ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);
    private final NotificationQueueProperties properties=new NotificationQueueProperties();
    private final NotificationDeliveryGatewayImpl gateway=new NotificationDeliveryGatewayImpl(provider,properties,"from@example.invalid","營區");
    private NotificationRecord row() {var row=new NotificationRecord();row.setRecipient("synthetic@example.invalid");row.setSubject("合成主旨");row.setMessage("合成內文");return row;}
    @Test void disabledSmtpStopsBeforeGettingSender() {assertThrows(IllegalStateException.class,()->gateway.checkReady("smtp"));verifyNoInteractions(provider);}
    @Test void simulationNeverUsesSmtpEvenWhenConfigured() {properties.setDeliveryMode("smtp");gateway.deliver(row(),"simulate");verifyNoInteractions(provider);}
    @Test void malformedRecipientAndHeaderInjectionFailPermanently() {var row=row();row.setRecipient("not-an-email");assertFalse(assertThrows(NotificationDeliveryException.class,()->gateway.deliver(row,"simulate")).isRetryable());row.setRecipient("synthetic@example.invalid");row.setSubject("Hello\r\nInjected: yes");assertEquals("INVALID_CONTENT",assertThrows(NotificationDeliveryException.class,()->gateway.deliver(row,"simulate")).getErrorCode());verifyNoInteractions(provider);}
    @Test void backoffGrowsAndIsCapped() {assertEquals(60,properties.retryDelay(1));assertEquals(960,properties.retryDelay(5));assertEquals(3600,properties.retryDelay(20));}
    @Test void smtpAuthenticationFailureIsSanitizedAndPermanent() {
        properties.setDeliveryMode("smtp");var sender=mock(JavaMailSender.class);when(provider.getIfAvailable()).thenReturn(sender);when(provider.getObject()).thenReturn(sender);
        doThrow(new org.springframework.mail.MailAuthenticationException("私有 SMTP 原始錯誤")).when(sender).send(any(org.springframework.mail.SimpleMailMessage.class));
        var error=assertThrows(NotificationDeliveryException.class,()->gateway.deliver(row(),"smtp"));
        assertEquals("SMTP_AUTH",error.getErrorCode());assertFalse(error.isRetryable());assertFalse(error.getMessage().contains("私有"));assertNull(error.getCause());
    }
    @Test void smtpTemporaryFailureIsSanitizedAndRetryable() {
        properties.setDeliveryMode("smtp");var sender=mock(JavaMailSender.class);when(provider.getIfAvailable()).thenReturn(sender);when(provider.getObject()).thenReturn(sender);
        doThrow(new org.springframework.mail.MailSendException("私有 SMTP 原始錯誤")).when(sender).send(any(org.springframework.mail.SimpleMailMessage.class));
        var error=assertThrows(NotificationDeliveryException.class,()->gateway.deliver(row(),"smtp"));
        assertEquals("SMTP_UNAVAILABLE",error.getErrorCode());assertTrue(error.isRetryable());assertFalse(error.getMessage().contains("私有"));assertNull(error.getCause());
    }
}
