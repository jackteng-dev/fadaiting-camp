package com.fadaiting.camp.controller;

import com.fadaiting.camp.service.LineBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/line")
public class PublicLineBotController {
    @Autowired
    private LineBotService lineBotService;

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestHeader(value = "X-Line-Signature", required = false) String signature,
            @RequestBody String requestBody
    ) {
        lineBotService.handleWebhook(signature, requestBody);
        return ResponseEntity.ok("OK");
    }
}
