package com.fadaiting.camp.controller;

import com.fadaiting.camp.dto.PublicPaymentCreateRequest;
import com.fadaiting.camp.dto.PublicPaymentCreateResponse;
import com.fadaiting.camp.dto.PublicPaymentCancelRequest;
import com.fadaiting.camp.dto.PublicPaymentFailRequest;
import com.fadaiting.camp.dto.PublicPaymentConfirmRequest;
import com.fadaiting.camp.dto.PublicPaymentConfirmResponse;
import com.fadaiting.camp.dto.PublicPaymentRetryRequest;
import com.fadaiting.camp.pojo.PaymentMethodConfig;
import com.fadaiting.camp.pojo.Result;
import com.fadaiting.camp.service.PaymentService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public/payment")
public class PublicPaymentController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping("/request")
    public Result<PublicPaymentCreateResponse> create(@RequestBody @Valid PublicPaymentCreateRequest request) {
        return Result.success(paymentService.createPublicPayment(request));
    }

    @GetMapping("/methods")
    public Result<List<PaymentMethodConfig>> methods() {
        return Result.success(paymentService.getPublicPaymentMethods());
    }

    @PostMapping("/retry")
    public Result<PublicPaymentCreateResponse> retry(@RequestBody @Valid PublicPaymentRetryRequest request) {
        return Result.success(paymentService.retryPublicPayment(request));
    }

    @PostMapping("/confirm")
    public Result<PublicPaymentConfirmResponse> confirm(@RequestBody @Valid PublicPaymentConfirmRequest request) {
        return Result.success(paymentService.confirmPublicPayment(request));
    }

    @PostMapping("/cancel")
    public Result<PublicPaymentConfirmResponse> cancel(@RequestBody @Valid PublicPaymentCancelRequest request) {
        return Result.success(paymentService.cancelPublicPayment(request));
    }

    @PostMapping("/fail")
    public Result<PublicPaymentConfirmResponse> fail(@RequestBody @Valid PublicPaymentFailRequest request) {
        return Result.success(paymentService.failPublicPayment(request));
    }

    @PostMapping(value = "/ecpay/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String ecpayCallback(@RequestParam Map<String, String> callbackParams) {
        try {
            return paymentService.handleEcpayCallback(callbackParams);
        } catch (IllegalArgumentException e) {
            return "0|" + e.getMessage();
        }
    }

    @PostMapping(value = "/ecpay/return", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void ecpayReturn(@RequestParam Map<String, String> callbackParams, HttpServletResponse response) throws java.io.IOException {
        response.sendRedirect(paymentService.resolveEcpayReturnRedirectUrl(callbackParams));
    }
}
