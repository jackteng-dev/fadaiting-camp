package com.fadaiting.camp.service;

import com.fadaiting.camp.dto.*;
import com.fadaiting.camp.pojo.RefundRecord;

public interface PaymentFinancialService {
    PaymentFinancialResponse getFinancial(Integer paymentId, Integer actorId);
    RefundRecord createRefund(RefundCreateRequest request, Integer actorId, String actorName);
    RefundRecord updateRefund(Integer refundId, RefundActionRequest request, Integer actorId, String actorName);
    PaymentFinancialResponse reconcile(Integer paymentId, PaymentReconciliationRequest request, Integer actorId, String actorName);
}
