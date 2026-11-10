package com.fadaiting.camp.controller;

import com.fadaiting.camp.dto.BookingCreateRequest;
import com.fadaiting.camp.dto.BookingCreateResponse;
import com.fadaiting.camp.dto.BookingQuoteRequest;
import com.fadaiting.camp.dto.BookingQuoteResponse;
import com.fadaiting.camp.dto.PublicBookingLookupRequest;
import com.fadaiting.camp.dto.PublicBookingLookupResponse;
import com.fadaiting.camp.dto.PublicBookingSummaryRequest;
import com.fadaiting.camp.pojo.Result;
import com.fadaiting.camp.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/booking")
public class PublicBookingController {
    @Autowired
    private BookingService bookingService;

    @PostMapping("/quote")
    public Result<BookingQuoteResponse> quote(@RequestBody @Valid BookingQuoteRequest request) {
        return Result.success(bookingService.quotePublicBooking(request));
    }

    @PostMapping
    public Result<BookingCreateResponse> create(@RequestBody @Valid BookingCreateRequest request) {
        return Result.success(bookingService.createPublicBooking(request));
    }

    @PostMapping("/lookup")
    public Result<PublicBookingLookupResponse> lookup(@RequestBody @Valid PublicBookingLookupRequest request) {
        return Result.success(bookingService.lookupPublicBooking(request));
    }

    @PostMapping("/summary")
    public Result<PublicBookingLookupResponse> summary(@RequestBody @Valid PublicBookingSummaryRequest request) {
        return Result.success(bookingService.getPublicBookingSummary(request));
    }
}
