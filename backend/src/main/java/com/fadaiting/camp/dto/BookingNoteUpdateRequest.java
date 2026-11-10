package com.fadaiting.camp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BookingNoteUpdateRequest {
    @NotNull(message = "缺少訂單編號")
    private Integer bookingId;
    @Size(max = 2000, message = "交接備註不得超過 2000 字")
    private String adminNote;
}
