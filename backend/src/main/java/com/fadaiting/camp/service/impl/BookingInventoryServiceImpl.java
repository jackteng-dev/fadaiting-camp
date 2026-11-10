package com.fadaiting.camp.service.impl;

import com.fadaiting.camp.dto.BookingDetailItemDto;
import com.fadaiting.camp.expection.BookingConflictException;
import com.fadaiting.camp.mapper.AvailabilityMapper;
import com.fadaiting.camp.mapper.BookingMapper;
import com.fadaiting.camp.mapper.RentalItemMapper;
import com.fadaiting.camp.pojo.Booking;
import com.fadaiting.camp.pojo.CampUnit;
import com.fadaiting.camp.pojo.RentalItem;
import com.fadaiting.camp.service.BookingInventoryService;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** 呼叫端持有交易；鎖定順序為訂單、租借品 ID、營位 ID／日期。 */
@Service
@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
public class BookingInventoryServiceImpl implements BookingInventoryService {
    private final AvailabilityMapper availability;
    private final BookingMapper bookings;
    private final RentalItemMapper rentals;
    public BookingInventoryServiceImpl(AvailabilityMapper availability, BookingMapper bookings, RentalItemMapper rentals) {
        this.availability = availability; this.bookings = bookings; this.rentals = rentals;
    }
    @Override public void reserve(CampUnit unit, LocalDate start, LocalDate end, int quantity) {
        for (LocalDate date = start; date.isBefore(end); date = date.plusDays(1)) {
            if (availability.reserveIfAvailable(unit.getCampSiteId(), unit.getId(), date, unit.getQuantity(), quantity) != 1)
                throw new BookingConflictException("目前庫存不足或已停售，請重新查詢");
        }
    }
    @Override public void release(Booking booking, List<BookingDetailItemDto> items) {
        for (var entry : quantities(items, false).entrySet()) {
            for (LocalDate date = booking.getCheckInDate(); date.isBefore(booking.getCheckOutDate()); date = date.plusDays(1)) {
                if (availability.releaseReserved(entry.getKey(), date, entry.getValue()) != 1)
                    throw new BookingConflictException("保留庫存不一致，請由管理員核對後重試");
            }
        }
        // 租借品可售數依有效訂單計算，取消狀態會在同一交易釋放。
    }
    @Override public void lockRentals(List<Integer> ids) {
        ids.stream().distinct().sorted().forEach(id -> {
            RentalItem item = rentals.findForUpdate(id);
            if (item == null || !Boolean.TRUE.equals(item.getIsActive())) throw new BookingConflictException("租借品已停售");
        });
    }
    @Override public void reopen(Booking booking, List<BookingDetailItemDto> items) {
        Map<Integer, Integer> rentalQuantities = quantities(items, true);
        lockRentals(List.copyOf(rentalQuantities.keySet()));
        for (var entry : rentalQuantities.entrySet()) {
            RentalItem item = rentals.findById(entry.getKey());
            int reserved = rentals.findMaxReservedQuantityByRentalItemAndDateRange(entry.getKey(), booking.getCheckInDate(), booking.getCheckOutDate());
            if (!item.getCampSiteId().equals(booking.getCampSiteId()) || reserved + entry.getValue() > item.getTotalQuantity())
                throw new BookingConflictException("租借品庫存不足，無法重新開啟預約");
        }
        for (var entry : quantities(items, false).entrySet()) {
            CampUnit unit = bookings.findActiveUnitById(entry.getKey());
            if (unit == null) throw new BookingConflictException("營位已停售，無法重新開啟預約");
            reserve(unit, booking.getCheckInDate(), booking.getCheckOutDate(), entry.getValue());
        }
    }
    private Map<Integer, Integer> quantities(List<BookingDetailItemDto> items, boolean rental) {
        Map<Integer, Integer> result = new TreeMap<>();
        for (BookingDetailItemDto item : items) {
            if (rental != (item.getRentalItemId() != null)) continue;
            Integer id = rental ? item.getRentalItemId() : item.getCampUnitId();
            if (id == null) continue;
            if (item.getQuantity() == null || item.getQuantity() < 1) throw new BookingConflictException("預約項目數量不一致，請先核對");
            result.merge(id, item.getQuantity(), Math::addExact);
        }
        return result;
    }
}
