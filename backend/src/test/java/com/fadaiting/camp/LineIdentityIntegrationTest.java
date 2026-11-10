package com.fadaiting.camp;

import com.fasterxml.jackson.databind.*;
import com.fadaiting.camp.expection.LineIdentityException;
import com.fadaiting.camp.service.LineIdTokenClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "CAMP_SECURITY_INTEGRATION", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "auth.cookie.secure=false", "line.identity.enabled=true", "line.identity.channel-id=synthetic-channel", "booking.hold.enabled=false",
        "JWT_SECRET=僅供隔離整合測試使用的固定金鑰123456789012345678901234567890"})
@Import(LineIdentityIntegrationTest.SyntheticProvider.class)
class LineIdentityIntegrationTest {
    @TestConfiguration static class SyntheticProvider {
        @Bean @Primary LineIdTokenClient syntheticProvider() {
            return (token,channel) -> {
                if ("offline".equals(token)) throw new LineIdentityException(true);
                if (!Set.of("valid-A","valid-B","expired","wrong-channel").contains(token)) throw new LineIdentityException(false);
                long now=Instant.now().getEpochSecond();
                return "{\"iss\":\"https://access.line.me\",\"aud\":\""+("wrong-channel".equals(token) ? "different-channel" : channel)+"\",\"sub\":\"U"+("valid-B".equals(token) ? "b" : "a").repeat(32)+"\",\"iat\":"+(now-60)+",\"exp\":"+("expired".equals(token) ? now-1 : now+600)+",\"name\":\"核驗名稱\",\"picture\":\"https://profile.line-scdn.net/synthetic\"}";
            };
        }
    }
    @Autowired JdbcTemplate db;
    @Autowired ObjectMapper json;
    @LocalServerPort int port;
    private final HttpClient http=HttpClient.newBuilder().proxy(new java.net.ProxySelector() {
        public List<java.net.Proxy> select(URI uri) { return List.of(java.net.Proxy.NO_PROXY); }
        public void connectFailed(URI uri,java.net.SocketAddress address,java.io.IOException error) {}
    }).build();
    private final List<Integer> ids=new ArrayList<>();
    private String firstNo,secondNo;
    private int first,second;
    record Response(int status,JsonNode body) {}
    @BeforeEach void seed() {
        firstNo="SEC"+UUID.randomUUID().toString().replace("-","").substring(0,20); secondNo="SEC"+UUID.randomUUID().toString().replace("-","").substring(0,20);
        first=booking(firstNo); second=booking(secondNo);
    }
    private int booking(String number) {
        int id=db.queryForObject("insert into booking(booking_no,camp_site_id,check_in_date,check_out_date) select ?,id,current_date+30,current_date+31 from camp_site order by id limit 1 returning id",Integer.class,number);
        ids.add(id); db.update("insert into booking_guest(booking_id,contact_name,contact_phone) values(?,'Cloud 身分合成','0999888777')",id); return id;
    }
    @AfterEach void cleanup() {
        for(int id:ids) { db.update("delete from line_user_binding where booking_id=?",id); db.update("delete from booking where id=?",id); } ids.clear();
    }
    private Response post(String path,Map<String,Object> body) throws Exception {
        HttpResponse<String> response=http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/fadaiting-camp/api/public/line"+path)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(),json.readTree(response.body()));
    }
    private JsonNode ok(Response response) { assertEquals(200,response.status()); assertEquals(0,response.body().path("code").asInt()); return response.body().path("data"); }
    private JsonNode request(String number) throws Exception { return ok(post("/bind/request",Map.of("bookingNo",number,"contactPhone","0999888777","idToken","valid-A"))); }
    private JsonNode bind(String number) throws Exception { var pending=request(number); return ok(post("/bind/confirm",Map.of("bindToken",pending.path("bindToken").asText(),"idToken","valid-A","lineUserId","U"+"b".repeat(32),"displayName","偽造名稱"))); }

    @Test void selfReportedUserIdCannotListOrBindWithoutToken() throws Exception {
        assertNotEquals(0,post("/booking/list",Map.of("lineUserId","U"+"a".repeat(32))).body().path("code").asInt());
        assertNotEquals(0,post("/bind/request",Map.of("bookingNo",firstNo,"contactPhone","0999888777")).body().path("code").asInt());
        assertEquals(0,db.queryForObject("select count(*) from line_user_binding where booking_id=?",Integer.class,first));
    }
    @Test void invalidExpiredWrongChannelAndProviderFailureNeverReadBookings() throws Exception {
        for(String token:new String[]{"invalid","expired","wrong-channel","offline"}) {
            assertEquals("offline".equals(token) ? 503 : 401,post("/booking/list",Map.of("idToken",token)).status());
        }
    }
    @Test void stolenBindTokenCannotBeConfirmedByAnotherVerifiedIdentity() throws Exception {
        var pending=request(firstNo);
        assertEquals(403,post("/bind/confirm",Map.of("idToken","valid-B","bindToken",pending.path("bindToken").asText())).status());
        assertEquals("pending",db.queryForObject("select status from line_user_binding where id=?",String.class,pending.path("bindingId").asInt()));
    }
    @Test void boundScopeDoesNotExpandToOtherOrdersSharingPhone() throws Exception {
        var bound=bind(firstNo); assertEquals("U"+"a".repeat(32),bound.path("lineUserId").asText()); assertEquals("核驗名稱",bound.path("displayName").asText());
        JsonNode list=ok(post("/booking/list",Map.of("idToken","valid-A","lineUserId","U"+"b".repeat(32)))).path("bookings"); assertEquals(1,list.size()); assertEquals(first,list.get(0).path("bookingId").asInt());
        assertEquals(403,post("/booking/detail",Map.of("idToken","valid-A","bookingId",second)).status());
        assertEquals(403,post("/booking/detail",Map.of("idToken","valid-B","bookingId",first)).status());
        JsonNode detail=ok(post("/booking/detail",Map.of("idToken","valid-A","bookingId",first))); assertEquals(first,detail.path("bookingId").asInt()); assertFalse(detail.toString().contains("identityVerifiedAt"));
    }
    @Test void sameIdentityCanExplicitlyBindMultipleBookings() throws Exception {
        bind(firstNo); bind(secondNo); assertEquals(2,ok(post("/booking/list",Map.of("idToken","valid-A"))).path("bookings").size());
    }
    @Test void legacyUnverifiedBindingGivesNoOrderAccessAndCanBeReplaced() throws Exception {
        db.update("insert into line_user_binding(line_user_id,booking_id,contact_phone,status) values(?,?,?,'active')","U"+"a".repeat(32),first,"0999888777");
        assertNotEquals(0,post("/booking/list",Map.of("idToken","valid-A")).body().path("code").asInt());
        bind(firstNo); assertEquals(1,db.queryForObject("select count(*) from line_user_binding where booking_id=? and status='inactive'",Integer.class,first));
        assertEquals(1,db.queryForObject("select count(*) from line_user_binding where booking_id=? and status='active' and identity_verified_at is not null",Integer.class,first));
    }
    @Test void expiredBindingCannotBecomeActive() throws Exception {
        var pending=request(firstNo); db.update("update line_user_binding set bind_token_expire_time=timestamp '2000-01-01' where id=?",pending.path("bindingId").asInt());
        assertEquals(409,post("/bind/confirm",Map.of("idToken","valid-A","bindToken",pending.path("bindToken").asText())).status());
    }
    @Test void concurrentConfirmationConsumesTokenOnce() throws Exception {
        var pending=request(firstNo);Map<String,Object> payload=Map.of("idToken","valid-A","bindToken",pending.path("bindToken").asText()); var barrier=new CyclicBarrier(2);
        try(var executor=Executors.newFixedThreadPool(2)) {
            Callable<Response> job=()->{barrier.await();return post("/bind/confirm",payload);};var a=executor.submit(job);var b=executor.submit(job);
            assertEquals(List.of(200,409),java.util.stream.Stream.of(a.get().status(),b.get().status()).sorted().toList());
        }
        assertEquals(1,db.queryForObject("select count(*) from line_user_binding where booking_id=? and status='active' and identity_verified_at is not null",Integer.class,first));
    }
    @Test void secondAccountCannotReplaceVerifiedBookingOwner() throws Exception {
        bind(firstNo); assertEquals(409,post("/bind/request",Map.of("idToken","valid-B","bookingNo",firstNo,"contactPhone","0999888777")).status());
    }
    @Test void bindingFailureRollsBackLegacyDeactivation() throws Exception {
        db.update("insert into line_user_binding(line_user_id,booking_id,contact_phone,status) values('legacy',?,?,'active')",first,"0999888777");var pending=request(firstNo);
        String function="cloud_line_failure_"+first;
        db.execute("create function "+function+"() returns trigger language plpgsql as $$ begin if NEW.id="+pending.path("bindingId").asInt()+" and NEW.status='active' then raise exception '合成綁定故障'; end if; return NEW; end $$");
        db.execute("create trigger "+function+" before update on line_user_binding for each row execute function "+function+"()");
        try {
            assertNotEquals(0,post("/bind/confirm",Map.of("idToken","valid-A","bindToken",pending.path("bindToken").asText())).body().path("code").asInt());
            assertEquals(1,db.queryForObject("select count(*) from line_user_binding where booking_id=? and line_user_id='legacy' and status='active'",Integer.class,first));
            assertEquals("pending",db.queryForObject("select status from line_user_binding where id=?",String.class,pending.path("bindingId").asInt()));
        } finally { db.execute("drop trigger "+function+" on line_user_binding");db.execute("drop function "+function+"()"); }
    }
}
