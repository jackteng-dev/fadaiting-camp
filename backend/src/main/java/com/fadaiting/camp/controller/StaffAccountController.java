package com.fadaiting.camp.controller;

import com.fadaiting.camp.dto.*;
import com.fadaiting.camp.pojo.CustomUserDetails;
import com.fadaiting.camp.pojo.PageBean;
import com.fadaiting.camp.pojo.Result;
import com.fadaiting.camp.service.StaffAccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/staff")
@Validated
public class StaffAccountController {
    private final StaffAccountService service;

    public StaffAccountController(StaffAccountService service) { this.service = service; }

    @GetMapping
    public Result<PageBean<StaffAccountDto>> list(@AuthenticationPrincipal CustomUserDetails actor,
            @RequestParam(defaultValue = "") @Size(max = 20) String username,
            @RequestParam(defaultValue = "1") @Min(1) @Max(100000) int pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return Result.success(service.list(actor.getId(), username, pageNum, pageSize));
    }

    @PostMapping
    public Result<StaffAccountDto> create(@AuthenticationPrincipal CustomUserDetails actor,
                                         @RequestBody @Valid StaffAccountCreateRequest request) {
        return Result.success(service.create(actor.getId(), request));
    }

    @PatchMapping("/{id}")
    public Result<StaffAccountDto> update(@AuthenticationPrincipal CustomUserDetails actor,
                                         @PathVariable @Positive Integer id,
                                         @RequestBody @Valid StaffAccountUpdateRequest request) {
        return Result.success(service.update(actor.getId(), id, request));
    }
}
