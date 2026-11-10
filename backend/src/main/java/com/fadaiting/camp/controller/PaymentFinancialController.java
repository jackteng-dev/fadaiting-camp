package com.fadaiting.camp.controller;

import com.fadaiting.camp.dto.*;
import com.fadaiting.camp.pojo.*;
import com.fadaiting.camp.service.PaymentFinancialService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentFinancialController {
    private final PaymentFinancialService service;
    public PaymentFinancialController(PaymentFinancialService service) { this.service = service; }
    @GetMapping("/finance/{paymentId}")
    public Result<PaymentFinancialResponse> financial(@PathVariable Integer paymentId, @AuthenticationPrincipal CustomUserDetails actor) {
        return Result.success(service.getFinancial(paymentId, actor.getId()));
    }
    @PostMapping("/refunds")
    public Result<RefundRecord> create(@RequestBody @Valid RefundCreateRequest request, @AuthenticationPrincipal CustomUserDetails actor) {
        return Result.success(service.createRefund(request, actor.getId(), actor.getUsername()));
    }
    @PatchMapping("/refunds/{refundId}")
    public Result<RefundRecord> update(@PathVariable Integer refundId, @RequestBody @Valid RefundActionRequest request, @AuthenticationPrincipal CustomUserDetails actor) {
        return Result.success(service.updateRefund(refundId, request, actor.getId(), actor.getUsername()));
    }
    @PatchMapping("/reconciliation/{paymentId}")
    public Result<PaymentFinancialResponse> reconcile(@PathVariable Integer paymentId, @RequestBody @Valid PaymentReconciliationRequest request, @AuthenticationPrincipal CustomUserDetails actor) {
        return Result.success(service.reconcile(paymentId, request, actor.getId(), actor.getUsername()));
    }
}
