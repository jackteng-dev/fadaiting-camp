"""每次備份／還原的私有原子紀錄；不保存路徑、帳密或資料內容。"""
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import tempfile
import uuid

ROOT = Path(__file__).resolve().parents[1]


class JournalError(Exception):
    pass


def now():
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


class BackupJournal:
    def __init__(self, directory, source_key, action):
        if not re.fullmatch(r"[a-z0-9][a-z0-9-]{0,39}", source_key or ""):
            raise JournalError("紀錄來源識別碼格式無效。")
        if action not in {"backup", "restore-check"}:
            raise JournalError("此操作不支援紀錄。")
        self.directory = Path(directory).absolute()
        if self.directory.resolve() != self.directory:
            raise JournalError("紀錄目錄不可透過符號連結存取。")
        if self.directory.is_relative_to(ROOT) and not self.directory.is_relative_to(ROOT / "work"):
            raise JournalError("倉庫內的私有紀錄僅可存於 work。")
        self.directory.mkdir(parents=True, exist_ok=True, mode=0o700)
        if not self.directory.is_dir() or self.directory.stat().st_mode & 0o077:
            raise JournalError("紀錄目錄須為私有目錄（0700），請先核對權限。")
        self.record = {"format": 1, "project": "fadaiting-camp", "sourceKey": source_key,
            "runId": str(uuid.uuid4()), "action": action, "status": "running",
            "startedAt": now(), "finishedAt": None, "archiveSha256": None, "errorCode": None}
        self.path = self.directory / (self.record["runId"] + ".json")
        self._write()

    def _write(self):
        temporary = None
        try:
            with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", prefix=".receipt-", suffix=".tmp",
                    dir=self.directory, delete=False) as stream:
                temporary = Path(stream.name)
                os.chmod(temporary, 0o600)
                json.dump(self.record, stream, ensure_ascii=False)
                stream.write("\n")
                stream.flush()
                os.fsync(stream.fileno())
            os.replace(temporary, self.path)
            directory = os.open(self.directory, os.O_RDONLY | os.O_DIRECTORY)
            try:
                os.fsync(directory)
            finally:
                os.close(directory)
        except OSError as exc:
            raise JournalError("執行紀錄無法保存；不宣稱已記錄成功，請核對私有儲存。") from exc
        finally:
            if temporary is not None:
                temporary.unlink(missing_ok=True)

    def success(self, archive_sha):
        if not re.fullmatch(r"[0-9a-f]{64}", archive_sha or ""):
            raise JournalError("成功紀錄缺少有效的備份雜湊。")
        self.record.update(status="success", finishedAt=now(), archiveSha256=archive_sha)
        self._write()

    def failed(self):
        self.record.update(status="failed", finishedAt=now(), archiveSha256=None, errorCode="OPERATION_FAILED")
        self._write()


def recorded_operation(directory, source_key, action, operation):
    # 起始紀錄無法寫入時，不開始 Docker 操作；終態寫入失敗仍保留實際產物。
    journal = BackupJournal(directory, source_key, action)
    try:
        result = operation()
    except Exception:
        journal.failed()
        raise
    if action == "restore-check" and result.get("verified") is not True:
        journal.failed()
        raise JournalError("還原未完整核對，不能記錄成功。")
    journal.success(result.get("archiveSha256"))
    return {**result, "recorded": True, "runId": journal.record["runId"]}
