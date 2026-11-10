package com.fadaiting.camp.dto;

import com.fadaiting.camp.pojo.Booking;
import com.fadaiting.camp.pojo.BookingGuest;
import com.fadaiting.camp.pojo.BookingTimeline;
import com.fadaiting.camp.pojo.PaymentRecord;
import lombok.Data;

import java.util.List;

@Data
public class BookingDetailDto {
    private Booking booking;
    private BookingGuest guest;
    private List<BookingDetailItemDto> items;
    private List<BookingTimeline> timeline;
    private List<PaymentRecord> payments;
}
