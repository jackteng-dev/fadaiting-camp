package com.fadaiting.camp.controller;

import com.fadaiting.camp.dto.AdminOperationLogQueryDto;
import com.fadaiting.camp.pojo.AdminOperationLog;
import com.fadaiting.camp.pojo.PageBean;
import com.fadaiting.camp.pojo.Result;
import com.fadaiting.camp.service.AdminOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operation-log")
public class AdminOperationLogController {
    @Autowired
    private AdminOperationLogService adminOperationLogService;

    @PostMapping("/list")
    public Result<PageBean<AdminOperationLog>> list(@RequestBody AdminOperationLogQueryDto queryDto) {
        return Result.success(adminOperationLogService.getAdminLogs(queryDto));
    }
}
