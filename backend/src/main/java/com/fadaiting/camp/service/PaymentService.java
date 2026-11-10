package com.fadaiting.camp.service;

import com.fadaiting.camp.dto.PublicPaymentCreateRequest;
import com.fadaiting.camp.dto.PublicPaymentCreateResponse;
import com.fadaiting.camp.dto.PublicPaymentCancelRequest;
import com.fadaiting.camp.dto.PublicPaymentFailRequest;
import com.fadaiting.camp.dto.PublicPaymentConfirmRequest;
import com.fadaiting.camp.dto.PublicPaymentConfirmResponse;
import com.fadaiting.camp.dto.PublicPaymentRetryRequest;
import com.fadaiting.camp.dto.ManualPaymentCreateRequest;
import com.fadaiting.camp.dto.PaymentAdminQueryDto;
import com.fadaiting.camp.dto.PaymentAdminRowDto;
import com.fadaiting.camp.dto.PaymentAdminSummaryDto;
import com.fadaiting.camp.pojo.PaymentRecord;
import com.fadaiting.camp.pojo.PaymentMethodConfig;
import com.fadaiting.camp.pojo.PageBean;

import java.util.List;
import java.util.Map;

public interface PaymentService {
    PublicPaymentCreateResponse createPublicPayment(PublicPaymentCreateRequest request);

    PublicPaymentCreateResponse retryPublicPayment(PublicPaymentRetryRequest request);

    PageBean<PaymentAdminRowDto> getAdminPayments(PaymentAdminQueryDto queryDto);

    List<PaymentAdminSummaryDto> getAdminPaymentSummary(PaymentAdminQueryDto queryDto);

    PaymentRecord createManualPayment(ManualPaymentCreateRequest request, Integer actorUserId, String actorName);

    List<PaymentMethodConfig> getAdminPaymentMethods();

    List<PaymentMethodConfig> updateAdminPaymentMethods(List<PaymentMethodConfig> configs);

    List<PaymentMethodConfig> getPublicPaymentMethods();

    PublicPaymentConfirmResponse confirmPublicPayment(PublicPaymentConfirmRequest request);

    PublicPaymentConfirmResponse cancelPublicPayment(PublicPaymentCancelRequest request);

    PublicPaymentConfirmResponse failPublicPayment(PublicPaymentFailRequest request);

    String handleEcpayCallback(Map<String, String> callbackParams);

    String resolveEcpayReturnRedirectUrl(Map<String, String> callbackParams);

    void expireStalePendingPayments(Integer bookingId);
}
