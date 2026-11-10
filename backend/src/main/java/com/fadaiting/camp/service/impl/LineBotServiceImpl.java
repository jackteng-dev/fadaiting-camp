package com.fadaiting.camp.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fadaiting.camp.config.LineBotProperties;
import com.fadaiting.camp.dto.CampSiteDetailDto;
import com.fadaiting.camp.dto.CampZoneDetailDto;
import com.fadaiting.camp.pojo.Announcement;
import com.fadaiting.camp.pojo.Faq;
import com.fadaiting.camp.service.CampSiteService;
import com.fadaiting.camp.service.LineBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class LineBotServiceImpl implements LineBotService {
    @Autowired
    private LineBotProperties lineBotProperties;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CampSiteService campSiteService;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void handleWebhook(String signature, String requestBody) {
        if (!isConfigured()) {
            return;
        }
        if (!isValidSignature(signature, requestBody)) {
            throw new IllegalArgumentException("LINE webhook signature 驗證失敗");
        }
        try {
            JsonNode root = objectMapper.readTree(requestBody);
            JsonNode events = root.path("events");
            if (!events.isArray()) {
                return;
            }
            for (JsonNode event : events) {
                handleEvent(event);
            }
        } catch (Exception e) {
            throw new IllegalStateException("LINE webhook 解析失敗: " + e.getMessage(), e);
        }
    }

    private void handleEvent(JsonNode event) {
        String eventType = event.path("type").asText("");
        String replyToken = event.path("replyToken").asText("");
        if (replyToken.isBlank()) {
            return;
        }

        if ("follow".equals(eventType)) {
            replyText(replyToken, buildWelcomeMessage());
            return;
        }

        if (!"message".equals(eventType)) {
            return;
        }
        JsonNode message = event.path("message");
        if (!"text".equals(message.path("type").asText(""))) {
            replyText(replyToken, buildDefaultHelpMessage());
            return;
        }
        String text = message.path("text").asText("").trim();
        replyText(replyToken, resolveReplyMessage(text));
    }

    private String resolveReplyMessage(String input) {
        String text = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return buildDefaultHelpMessage();
        }
        if (matches(text, "資訊", "營區資訊", "介紹", "露營區")) {
            return buildCampInfoMessage();
        }
        if (matches(text, "預約", "訂位", "我要訂位", "我要預約")) {
            return "可以直接從這裡進入線上預約頁：" + buildFrontendUrl("/booking");
        }
        if (matches(text, "查詢", "查訂單", "我的訂單", "訂單查詢")) {
            return "可以從這裡查詢預約與付款進度：" + buildFrontendUrl("/booking/lookup");
        }
        if (matches(text, "公告", "最新公告", "最新消息")) {
            return buildAnnouncementsMessage();
        }
        if (matches(text, "faq", "問題", "常見問題")) {
            return buildFaqMessage();
        }
        if (matches(text, "營位", "區域", "帳篷")) {
            return buildZoneMessage();
        }
        return buildDefaultHelpMessage();
    }

    private boolean matches(String input, String... keywords) {
        for (String keyword : keywords) {
            if (input.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String buildWelcomeMessage() {
        return "歡迎加入發呆亭露營區 LINE 官方帳號。\n"
                + "你可以直接輸入：營區資訊、營位、公告、FAQ、預約、查訂單\n"
                + "我會把你帶到對應資訊或頁面。";
    }

    private String buildCampInfoMessage() {
        CampSiteDetailDto detail = campSiteService.getActiveSiteDetail();
        if (detail == null || detail.getSite() == null) {
            return "目前尚未設定營區資訊。";
        }
        return String.format(
                "營區名稱：%s\n地址：%s\n電話：%s\n入住 / 退住：%s / %s\n%s\n官網：%s",
                safe(detail.getSite().getName()),
                safe(detail.getSite().getAddressLine()),
                safe(detail.getSite().getContactPhone()),
                safe(detail.getSite().getCheckInTime()),
                safe(detail.getSite().getCheckOutTime()),
                safe(detail.getSite().getShortDescription()),
                buildFrontendUrl("/")
        );
    }

    private String buildAnnouncementsMessage() {
        List<Announcement> announcements = campSiteService.getActiveAnnouncements();
        if (announcements == null || announcements.isEmpty()) {
            return "目前沒有最新公告，可以直接看官網最新消息頁：" + buildFrontendUrl("/news");
        }
        List<String> lines = new ArrayList<>();
        lines.add("最新公告：");
        announcements.stream().limit(3).forEach(item -> lines.add("- " + safe(item.getTitle())));
        lines.add("查看更多：" + buildFrontendUrl("/news"));
        return String.join("\n", lines);
    }

    private String buildFaqMessage() {
        CampSiteDetailDto detail = campSiteService.getActiveSiteDetail();
        List<Faq> faqs = detail == null ? null : detail.getFaqs();
        if (faqs == null || faqs.isEmpty()) {
            return "目前沒有 FAQ，可直接查看官網頁面：" + buildFrontendUrl("/faq");
        }
        List<String> lines = new ArrayList<>();
        lines.add("常見問題：");
        faqs.stream().limit(3).forEach(item -> lines.add("- " + safe(item.getQuestion())));
        lines.add("查看更多：" + buildFrontendUrl("/faq"));
        return String.join("\n", lines);
    }

    private String buildZoneMessage() {
        List<CampZoneDetailDto> zones = campSiteService.getActiveZoneDetails();
        if (zones == null || zones.isEmpty()) {
            return "目前尚未設定營位資料。";
        }
        List<String> lines = new ArrayList<>();
        lines.add("營位 / 區域：");
        zones.stream().limit(4).forEach(item -> lines.add("- " + safe(item.getZone().getName())));
        lines.add("查看更多：" + buildFrontendUrl("/zones"));
        return String.join("\n", lines);
    }

    private String buildDefaultHelpMessage() {
        return "你可以輸入以下關鍵字：\n"
                + "- 營區資訊\n"
                + "- 營位\n"
                + "- 公告\n"
                + "- FAQ\n"
                + "- 預約\n"
                + "- 查訂單";
    }

    private void replyText(String replyToken, String text) {
        try {
            String payload = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "replyToken", replyToken,
                            "messages", java.util.List.of(java.util.Map.of("type", "text", "text", text))
                    )
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimTrailingSlash(lineBotProperties.getApiBaseUrl()) + "/v2/bot/message/reply"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + lineBotProperties.getChannelAccessToken())
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new IllegalStateException("LINE reply 發送失敗: " + e.getMessage(), e);
        }
    }

    private boolean isConfigured() {
        return lineBotProperties.isEnabled()
                && hasText(lineBotProperties.getChannelSecret())
                && hasText(lineBotProperties.getChannelAccessToken())
                && hasText(lineBotProperties.getApiBaseUrl());
    }

    private boolean isValidSignature(String signature, String requestBody) {
        if (!hasText(signature)) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(lineBotProperties.getChannelSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8));
            String expected = Base64.getEncoder().encodeToString(digest);
            return expected.equals(signature);
        } catch (Exception e) {
            throw new IllegalStateException("LINE signature 驗證失敗: " + e.getMessage(), e);
        }
    }

    private String buildFrontendUrl(String path) {
        return trimTrailingSlash(lineBotProperties.getFrontendBaseUrl()) + path;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
