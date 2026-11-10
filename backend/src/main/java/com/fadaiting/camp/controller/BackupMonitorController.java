package com.fadaiting.camp.controller;

import com.fadaiting.camp.dto.BackupMonitorStatusDto;
import com.fadaiting.camp.pojo.Result;
import com.fadaiting.camp.service.BackupMonitorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/backup")
public class BackupMonitorController {
    private final BackupMonitorService monitor;
    public BackupMonitorController(BackupMonitorService monitor) { this.monitor=monitor; }
    @GetMapping("/status")
    public Result<BackupMonitorStatusDto> status() { return Result.success(monitor.status()); }
}
