package com.fadaiting.camp.service;

import com.fadaiting.camp.dto.BookingStayUpdateRequest;
import com.fadaiting.camp.pojo.Booking;

public interface BookingStayService {
    Booking updateStay(BookingStayUpdateRequest request, Integer actorId, String actorName);
}
