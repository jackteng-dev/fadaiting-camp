package com.fadaiting.camp.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fadaiting.camp.config.BackupMonitorProperties;
import com.fadaiting.camp.dto.BackupMonitorStatusDto;
import com.fadaiting.camp.dto.BackupMonitorStatusDto.Alert;
import com.fadaiting.camp.dto.BackupMonitorStatusDto.RunSummary;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class BackupMonitorService {
    private final BackupMonitorProperties properties;
    private final Clock clock;
    private final ObjectMapper json = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final Set<String> FIELDS = Set.of("format","project","sourceKey","runId","action","status",
            "startedAt","finishedAt","archiveSha256","errorCode");
    private record Receipt(String runId,String action,RunSummary summary) { }

    public BackupMonitorService(BackupMonitorProperties properties,Clock clock) {
        this.properties=properties; this.clock=clock;
    }

    public BackupMonitorStatusDto status() {
        var result=new BackupMonitorStatusDto(); var now=OffsetDateTime.now(clock);
        result.setObservedAt(now); result.setSourceKey(properties.getSourceKey());
        result.setMaxBackupAgeSeconds(properties.getMaxBackupAgeSeconds());
        result.setMaxRestoreAgeSeconds(properties.getMaxRestoreAgeSeconds());
        result.setRunTimeoutSeconds(properties.getRunTimeoutSeconds());
        result.setConfigured(!properties.getJournalPath().isBlank());
        if (!result.isConfigured()) {
            alert(result,"NOT_CONFIGURED","info","尚未設定備份紀錄來源","請由管理者設定私有紀錄目錄；不從檔案存在或舊交接推定成功。");
            return result;
        }
        List<Receipt> receipts;
        try { receipts=readReceipts(now); }
        catch (Exception error) {
            alert(result,"RECORDS_UNAVAILABLE","critical","執行紀錄無法完整核對","核對私有儲存、權限與紀錄格式；目前不能判定備份狀態。");
            return result;
        }
        result.setJournalAvailable(true); result.setRecordCount(receipts.size());
        result.setLastBackupAttempt(latest(receipts,"backup",false));
        result.setLastRestoreAttempt(latest(receipts,"restore-check",false));
        result.setLastBackupSuccess(latest(receipts,"backup",true));
        result.setLastRestoreSuccess(latest(receipts,"restore-check",true));
        var backup=result.getLastBackupSuccess(); var restore=result.getLastRestoreSuccess();
        if (backup==null) alert(result,"NO_BACKUP_SUCCESS","warning","尚無成功備份紀錄","先執行帶紀錄的備份；中斷或失敗不計成功。");
        else {
            receipts.stream().filter(r->"restore-check".equals(r.action()) && "success".equals(r.summary().status())
                    && backup.archiveSha256().equals(r.summary().archiveSha256()))
                .map(r->r.summary().finishedAt()).max(Comparator.comparing(OffsetDateTime::toInstant))
                .ifPresent(time->{result.setLatestBackupVerified(true);result.setLatestBackupVerifiedAt(time);});
            if (!result.isLatestBackupVerified()) alert(result,"LATEST_BACKUP_UNVERIFIED","warning","最新備份尚未驗證還原","對相同備份執行隔離還原核對；其他備份的成功驗證不能代替。");
            if (!backup.finishedAt().plusSeconds(properties.getMaxBackupAgeSeconds()).isAfter(now))
                alert(result,"BACKUP_OVERDUE","warning","成功備份已超過時間門檻","核對排程與儲存，再建立新的可信備份；頁面不會自動備份。");
        }
        if (restore==null) alert(result,"NO_RESTORE_SUCCESS","warning","尚無成功還原驗證紀錄","先完成隔離還原，不能以備份成功代替可還原驗證。");
        else if (!restore.finishedAt().plusSeconds(properties.getMaxRestoreAgeSeconds()).isAfter(now))
            alert(result,"RESTORE_OVERDUE","warning","成功還原驗證已超過時間門檻","重新演練可信備份的隔離還原；不覆寫既有資料庫。");
        checkAttempt(result,result.getLastBackupAttempt(),"BACKUP",now);
        checkAttempt(result,result.getLastRestoreAttempt(),"RESTORE",now);
        result.setHealth(result.getAlerts().stream().anyMatch(a->"critical".equals(a.severity())) ? "critical" :
                backup==null ? "unknown" : result.getAlerts().stream().anyMatch(a->"warning".equals(a.severity())) ? "warning" :
                result.getAlerts().isEmpty() ? "healthy" : "info");
        return result;
    }

    private void checkAttempt(BackupMonitorStatusDto result,RunSummary attempt,String kind,OffsetDateTime now) {
        if (attempt==null) return;
        String name="BACKUP".equals(kind) ? "備份" : "還原驗證";
        if ("failed".equals(attempt.status())) alert(result,kind+"_FAILED","critical","最近一次"+name+"失敗","成功歷史保留；核對 CLI 結果與私有日誌，不直接宣稱最新操作成功。");
        else if ("running".equals(attempt.status())) {
            boolean overdue=!attempt.startedAt().plusSeconds(properties.getRunTimeoutSeconds()).isAfter(now);
            alert(result,kind+(overdue ? "_INCOMPLETE" : "_RUNNING"),overdue ? "critical" : "info",
                    name+(overdue ? "尚未完成且已超時" : "尚未完成"),"核對實際進程與紀錄儲存；running 不代表進程仍存活，也不計成功。");
        }
    }

    private static void alert(BackupMonitorStatusDto result,String code,String severity,String message,String action) {
        result.getAlerts().add(new Alert(code,severity,message,action));
    }

    private RunSummary latest(List<Receipt> receipts,String action,boolean successOnly) {
        Comparator<Receipt> order=Comparator.comparing((Receipt r)->successOnly ? r.summary().finishedAt().toInstant() : r.summary().startedAt().toInstant());
        return receipts.stream().filter(r->action.equals(r.action()) && (!successOnly || "success".equals(r.summary().status())))
                .max(order.thenComparing(Receipt::runId)).map(Receipt::summary).orElse(null);
    }

    private List<Receipt> readReceipts(OffsetDateTime now) throws IOException {
        Path folder=Path.of(properties.getJournalPath()).toAbsolutePath().normalize();
        for (Path part=folder;part!=null;part=part.getParent()) if (Files.isSymbolicLink(part)) throw new IOException();
        if (!Files.isDirectory(folder,LinkOption.NOFOLLOW_LINKS)) throw new IOException();
        var receipts=new ArrayList<Receipt>(); int count=0;
        try (var files=Files.newDirectoryStream(folder,"*.json")) {
            for (Path file:files) {
                if (++count>properties.getRecordLimit() || !Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)) throw new IOException();
                byte[] bytes;
                try (var stream=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS)) { bytes=stream.readNBytes(16385); }
                if (bytes.length>16384) throw new IOException();
                JsonNode node=json.readTree(bytes);
                if (node==null || !node.isObject() || node.size()!=FIELDS.size()) throw new IOException();
                for (var fields=node.fieldNames();fields.hasNext();) if (!FIELDS.contains(fields.next())) throw new IOException();
                if (!node.path("format").isIntegralNumber() || !node.path("format").canConvertToInt()
                        || node.path("format").intValue()!=1 || !"fadaiting-camp".equals(text(node,"project"))) throw new IOException();
                String key=text(node,"sourceKey"),id=text(node,"runId"),action=text(node,"action"),status=text(node,"status");
                if (!key.matches("[a-z0-9][a-z0-9-]{0,39}") || !UUID.fromString(id).toString().equals(id)
                        || !file.getFileName().toString().equals(id+".json") || !Set.of("backup","restore-check").contains(action)
                        || !Set.of("running","success","failed").contains(status)) throw new IOException();
                var started=OffsetDateTime.parse(text(node,"startedAt"));
                var finished=node.path("finishedAt").isNull() ? null : OffsetDateTime.parse(text(node,"finishedAt"));
                String sha=node.path("archiveSha256").isNull() ? null : text(node,"archiveSha256");
                String error=node.path("errorCode").isNull() ? null : text(node,"errorCode");
                if (started.isAfter(now.plusSeconds(300)) || (finished!=null && (finished.isBefore(started) || finished.isAfter(now.plusSeconds(300))))) throw new IOException();
                if ("running".equals(status) && (finished!=null || sha!=null || error!=null)
                        || "success".equals(status) && (finished==null || sha==null || !sha.matches("[0-9a-f]{64}") || error!=null)
                        || "failed".equals(status) && (finished==null || sha!=null || !"OPERATION_FAILED".equals(error))) throw new IOException();
                if (properties.getSourceKey().equals(key)) receipts.add(new Receipt(id,action,new RunSummary(status,started,finished,sha,error)));
            }
        }
        return receipts;
    }

    private static String text(JsonNode node,String field) throws IOException {
        if (!node.path(field).isTextual()) throw new IOException();
        return node.path(field).textValue();
    }
}
