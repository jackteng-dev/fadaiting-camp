package com.fadaiting.camp.service;

import com.fadaiting.camp.dto.BookingDetailItemDto;
import com.fadaiting.camp.pojo.Booking;
import com.fadaiting.camp.pojo.CampUnit;
import java.time.LocalDate;
import java.util.List;

public interface BookingInventoryService {
    void reserve(CampUnit unit, LocalDate start, LocalDate end, int quantity);
    void release(Booking booking, List<BookingDetailItemDto> items);
    void reopen(Booking booking, List<BookingDetailItemDto> items);
    void lockRentals(List<Integer> ids);
}
