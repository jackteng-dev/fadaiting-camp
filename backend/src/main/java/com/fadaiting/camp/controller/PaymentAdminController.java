package com.fadaiting.camp.controller;

import com.fadaiting.camp.dto.ManualPaymentCreateRequest;
import com.fadaiting.camp.dto.PaymentAdminQueryDto;
import com.fadaiting.camp.dto.PaymentAdminRowDto;
import com.fadaiting.camp.dto.PaymentAdminSummaryDto;
import com.fadaiting.camp.pojo.CustomUserDetails;
import com.fadaiting.camp.pojo.PageBean;
import com.fadaiting.camp.pojo.PaymentMethodConfig;
import com.fadaiting.camp.pojo.PaymentRecord;
import com.fadaiting.camp.pojo.Result;
import com.fadaiting.camp.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/payment")
public class PaymentAdminController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping("/list")
    public Result<PageBean<PaymentAdminRowDto>> list(@RequestBody PaymentAdminQueryDto queryDto) {
        return Result.success(paymentService.getAdminPayments(queryDto));
    }

    @PostMapping("/summary")
    public Result<List<PaymentAdminSummaryDto>> summary(@RequestBody PaymentAdminQueryDto queryDto) {
        return Result.success(paymentService.getAdminPaymentSummary(queryDto));
    }

    @GetMapping("/methods")
    public Result<List<PaymentMethodConfig>> methods() {
        return Result.success(paymentService.getAdminPaymentMethods());
    }

    @PutMapping("/methods")
    public Result<List<PaymentMethodConfig>> updateMethods(@RequestBody List<PaymentMethodConfig> configs) {
        return Result.success(paymentService.updateAdminPaymentMethods(configs));
    }

    @PostMapping("/manual")
    public Result<PaymentRecord> createManualPayment(
            @RequestBody @Valid ManualPaymentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        PaymentRecord record = paymentService.createManualPayment(request, currentUser.getId(), currentUser.getUsername());
        return Result.success(record);
    }
}
