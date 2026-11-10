package com.fadaiting.camp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fadaiting.camp.config.BackupMonitorProperties;
import com.fadaiting.camp.service.BackupMonitorService;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BackupMonitorServiceTest {
    @TempDir Path folder;
    final Instant now=Instant.parse("2026-10-06T12:00:00Z");
    BackupMonitorProperties properties;
    BackupMonitorService monitor;

    @BeforeEach void setup() {
        properties=new BackupMonitorProperties(); properties.setJournalPath(folder.toString());
        monitor=new BackupMonitorService(properties,Clock.fixed(now,ZoneOffset.UTC));
    }
    Path receipt(String source,String action,String status,String sha,long ageSeconds) throws Exception {
        String id=UUID.randomUUID().toString();var record=new LinkedHashMap<String,Object>();
        record.put("format",1);record.put("project","fadaiting-camp");record.put("sourceKey",source);record.put("runId",id);
        record.put("action",action);record.put("status",status);
        record.put("startedAt",now.minusSeconds(ageSeconds+1).toString());
        record.put("finishedAt","running".equals(status) ? null : now.minusSeconds(ageSeconds).toString());
        record.put("archiveSha256",sha);record.put("errorCode","failed".equals(status) ? "OPERATION_FAILED" : null);
        Path target=folder.resolve(id+".json");Files.writeString(target,new ObjectMapper().writeValueAsString(record));return target;
    }
    boolean alert(String code) {return monitor.status().getAlerts().stream().anyMatch(a->code.equals(a.code()));}

    @Test void unconfiguredAndEmptyDoNotInventSuccess() {
        assertNull(monitor.status().getLastBackupSuccess()); assertEquals("unknown",monitor.status().getHealth());
        assertTrue(alert("NO_BACKUP_SUCCESS"));
        properties.setJournalPath(""); assertFalse(monitor.status().isConfigured()); assertTrue(alert("NOT_CONFIGURED"));
    }
    @Test void backupAloneDoesNotBecomeVerified() throws Exception {
        receipt("primary","backup","success","a".repeat(64),60);
        assertFalse(monitor.status().isLatestBackupVerified()); assertNull(monitor.status().getLastRestoreSuccess());
        assertTrue(alert("LATEST_BACKUP_UNVERIFIED"));
    }
    @Test void unrelatedRestoreDoesNotVerifyLatestBackup() throws Exception {
        receipt("primary","backup","success","a".repeat(64),60);
        receipt("primary","restore-check","success","b".repeat(64),30);
        assertFalse(monitor.status().isLatestBackupVerified());assertNotNull(monitor.status().getLastRestoreSuccess());
    }
    @Test void exactHashAndSourceMatchingVerifyOnlyTheirBackup() throws Exception {
        receipt("primary","backup","success","a".repeat(64),60);
        receipt("staging","restore-check","success","a".repeat(64),30);
        assertFalse(monitor.status().isLatestBackupVerified());
        receipt("primary","restore-check","success","a".repeat(64),10);
        assertTrue(monitor.status().isLatestBackupVerified());assertEquals("healthy",monitor.status().getHealth());
        assertEquals(2,monitor.status().getRecordCount());assertEquals(now.minusSeconds(10),monitor.status().getLatestBackupVerifiedAt().toInstant());
    }
    @Test void newerBackupRequiresNewMatchingVerification() throws Exception {
        receipt("primary","backup","success","a".repeat(64),120);
        receipt("primary","restore-check","success","a".repeat(64),90);
        receipt("primary","backup","success","b".repeat(64),30);
        assertFalse(monitor.status().isLatestBackupVerified());assertTrue(alert("LATEST_BACKUP_UNVERIFIED"));
    }
    @Test void failurePreservesLastSuccessButRaisesCritical() throws Exception {
        receipt("primary","backup","success","a".repeat(64),120);
        receipt("primary","restore-check","success","a".repeat(64),90);
        receipt("primary","backup","failed",null,10);
        assertNotNull(monitor.status().getLastBackupSuccess());assertEquals("critical",monitor.status().getHealth());assertTrue(alert("BACKUP_FAILED"));
    }
    @Test void runningDoesNotProveProcessAliveOrSuccessAndTimeoutIsInclusive() throws Exception {
        receipt("primary","restore-check","running",null,properties.getRunTimeoutSeconds()-1);
        assertNull(monitor.status().getLastRestoreSuccess());assertTrue(alert("RESTORE_INCOMPLETE"));
    }
    @Test void ageThresholdsAreInclusive() throws Exception {
        receipt("primary","backup","success","a".repeat(64),properties.getMaxBackupAgeSeconds());
        receipt("primary","restore-check","success","a".repeat(64),properties.getMaxRestoreAgeSeconds());
        assertTrue(alert("BACKUP_OVERDUE"));assertTrue(alert("RESTORE_OVERDUE"));
    }
    @Test void corruptAndSymlinkRecordsAreUnavailableAndNeverLeakPath() throws Exception {
        receipt("primary","backup","success","a".repeat(64),60);
        Path invalid=folder.resolve("private-secret-path.json");Files.writeString(invalid,"not JSON");
        assertFalse(monitor.status().isJournalAvailable()); assertNull(monitor.status().getLastBackupSuccess());
        String json=new ObjectMapper().findAndRegisterModules().writeValueAsString(monitor.status());
        assertFalse(json.contains(folder.toString()));assertFalse(json.contains("private-secret"));
        Files.delete(invalid);Files.createSymbolicLink(invalid,folder.resolve("missing"));
        assertTrue(alert("RECORDS_UNAVAILABLE"));
    }
    @Test void fileCountAndSizeBoundsCannotProducePartialHealthyResult() throws Exception {
        receipt("primary","backup","success","a".repeat(64),60);
        receipt("primary","restore-check","success","a".repeat(64),30);
        properties.setRecordLimit(1);assertFalse(monitor.status().isJournalAvailable());
        properties.setRecordLimit(2000);Files.writeString(folder.resolve("oversize.json")," ".repeat(16385));
        assertNull(monitor.status().getLastRestoreSuccess());assertTrue(alert("RECORDS_UNAVAILABLE"));
    }
    @Test void readingDoesNotWriteReceipts() throws Exception {
        Path path=receipt("primary","backup","success","a".repeat(64),60);byte[] before=Files.readAllBytes(path);
        var modified=Files.getLastModifiedTime(path);monitor.status();monitor.status();
        assertArrayEquals(before,Files.readAllBytes(path));assertEquals(modified,Files.getLastModifiedTime(path));
        try (var files=Files.list(folder)) { assertEquals(1,files.count()); }
    }
    @Test void duplicateFieldsTrailingJsonAndFutureTimeCannotClaimSuccess() throws Exception {
        Path path=receipt("primary","backup","success","a".repeat(64),60);String valid=Files.readString(path);
        for (String invalid:List.of(valid.replace("\"format\":1", "\"format\":1,\"format\":1"),valid+" {}",
                valid.replace(now.minusSeconds(61).toString(),now.plusSeconds(301).toString()))) {
            Files.writeString(path,invalid); assertFalse(monitor.status().isJournalAvailable()); assertNull(monitor.status().getLastBackupSuccess());
        }
    }
}
