package com.fadaiting.camp.service;

import com.fadaiting.camp.dto.BookingNoteUpdateRequest;

import com.fadaiting.camp.dto.BookingCreateRequest;
import com.fadaiting.camp.dto.BookingCreateResponse;
import com.fadaiting.camp.dto.BookingAdminQueryDto;
import com.fadaiting.camp.dto.BookingAdminUpdateRequest;
import com.fadaiting.camp.dto.BookingDetailDto;
import com.fadaiting.camp.dto.BookingQuoteRequest;
import com.fadaiting.camp.dto.BookingQuoteResponse;
import com.fadaiting.camp.dto.BookingStatusUpdateRequest;
import com.fadaiting.camp.dto.PublicBookingLookupRequest;
import com.fadaiting.camp.dto.PublicBookingLookupResponse;
import com.fadaiting.camp.dto.PublicBookingSummaryRequest;
import com.fadaiting.camp.pojo.PageBean;
import com.fadaiting.camp.dto.BookingAdminRowDto;

public interface BookingService {
    BookingCreateResponse createPublicBooking(BookingCreateRequest request);

    BookingQuoteResponse quotePublicBooking(BookingQuoteRequest request);

    PublicBookingLookupResponse lookupPublicBooking(PublicBookingLookupRequest request);

    PublicBookingLookupResponse getPublicBookingSummary(PublicBookingSummaryRequest request);

    PageBean<BookingAdminRowDto> getAdminBookings(BookingAdminQueryDto queryDto);

    BookingDetailDto getAdminBookingDetail(Integer bookingId);

    void updateBookingStatus(BookingStatusUpdateRequest request, Integer actorUserId, String actorName);

    void updateAdminBooking(BookingAdminUpdateRequest request, Integer actorUserId, String actorName);
    void updateBookingNote(BookingNoteUpdateRequest request, Integer actorUserId, String actorName);
}
