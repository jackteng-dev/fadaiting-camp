package com.fadaiting.camp.controller;

import com.fadaiting.camp.dto.LineBindConfirmRequest;
import com.fadaiting.camp.dto.LineBindConfirmResponse;
import com.fadaiting.camp.dto.LineBindRequestRequest;
import com.fadaiting.camp.dto.LineBindRequestResponse;
import com.fadaiting.camp.dto.LineBookingDetailRequest;
import com.fadaiting.camp.dto.LineBookingListRequest;
import com.fadaiting.camp.dto.LineBookingListResponse;
import com.fadaiting.camp.dto.PublicBookingLookupResponse;
import com.fadaiting.camp.pojo.Result;
import com.fadaiting.camp.service.LineIntegrationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/line")
public class PublicLineIntegrationController {
    @Autowired
    private LineIntegrationService lineIntegrationService;

    @PostMapping("/bind/request")
    public Result<LineBindRequestResponse> bindRequest(@RequestBody @Valid LineBindRequestRequest request) {
        return Result.success(lineIntegrationService.createBindRequest(request));
    }

    @PostMapping("/bind/confirm")
    public Result<LineBindConfirmResponse> bindConfirm(@RequestBody @Valid LineBindConfirmRequest request) {
        return Result.success(lineIntegrationService.confirmBind(request));
    }

    @PostMapping("/booking/list")
    public Result<LineBookingListResponse> bookingList(@RequestBody @Valid LineBookingListRequest request) {
        return Result.success(lineIntegrationService.getLineBookings(request));
    }

    @PostMapping("/booking/detail")
    public Result<PublicBookingLookupResponse> bookingDetail(@RequestBody @Valid LineBookingDetailRequest request) {
        return Result.success(lineIntegrationService.getLineBookingDetail(request));
    }
}
