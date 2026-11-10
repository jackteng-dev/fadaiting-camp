package com.fadaiting.camp.service;

import com.fadaiting.camp.dto.*;
import com.fadaiting.camp.pojo.PageBean;

public interface StaffAccountService {
    PageBean<StaffAccountDto> list(Integer actorId, String username, int pageNum, int pageSize);
    StaffAccountDto create(Integer actorId, StaffAccountCreateRequest request);
    StaffAccountDto update(Integer actorId, Integer id, StaffAccountUpdateRequest request);
}
