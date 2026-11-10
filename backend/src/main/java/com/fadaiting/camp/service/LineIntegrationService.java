package com.fadaiting.camp.service;

import com.fadaiting.camp.dto.LineBindConfirmRequest;
import com.fadaiting.camp.dto.LineBindConfirmResponse;
import com.fadaiting.camp.dto.LineBindRequestRequest;
import com.fadaiting.camp.dto.LineBindRequestResponse;
import com.fadaiting.camp.dto.LineBookingDetailRequest;
import com.fadaiting.camp.dto.LineBookingListRequest;
import com.fadaiting.camp.dto.LineBookingListResponse;
import com.fadaiting.camp.dto.PublicBookingLookupResponse;

public interface LineIntegrationService {
    LineBindRequestResponse createBindRequest(LineBindRequestRequest request);

    LineBindConfirmResponse confirmBind(LineBindConfirmRequest request);

    LineBookingListResponse getLineBookings(LineBookingListRequest request);

    PublicBookingLookupResponse getLineBookingDetail(LineBookingDetailRequest request);
}
