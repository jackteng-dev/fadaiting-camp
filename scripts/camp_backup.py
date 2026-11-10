"""PostgreSQL 自訂格式備份與一次性隔離還原；只使用標準函式庫。"""
import argparse
import contextlib
import hashlib
import json
import os
from pathlib import Path
import queue
import re
import shutil
import subprocess
import tempfile
import threading
import time
import uuid
from backup_journal import JournalError, recorded_operation

ROOT = Path(__file__).resolve().parents[1]
PROJECT = "fadaiting-camp"
LABEL = "com.fadaiting.restore-run"
REQUIRED = {"app_user", "camp_site", "booking", "booking_guest", "booking_item",
            "booking_timeline", "payment_record", "refund_record",
            "notification_record", "notification_attempt"}


class BackupError(Exception):
    pass


def identifier(value):
    if not isinstance(value, str) or not re.fullmatch(r"[a-z_][a-z0-9_]{0,62}", value):
        raise BackupError("資料庫／表／使用者名稱格式不支援。")
    return '"' + value + '"'


def digest(path):
    sha = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            sha.update(block)
    return sha.hexdigest()


def normalize_definition(value):
    # pg_dump round-trip 會把「常值 varchar[] 整體轉 text[]」展開為逐項轉型。
    # 僅處理無長度限制的純字串常值，不移除任意 cast／括號或變更條件。
    literal = r"'(?:[^']|'')*'::character varying"
    pattern = r"\(ARRAY\[(" + literal + r"(?:, " + literal + r")*)\]\)::text\[\]"
    return re.sub(pattern, lambda m: "ARRAY[" + ", ".join(
        "(" + token + ")::text" for token in re.findall(literal, m.group(1))) + "]", value)


def normalized_expected(value):
    result = json.loads(json.dumps(value))
    for group, field in [("constraints", "definition"), ("indexes", "indexdef")]:
        for item in result.get("schema", {}).get(group, []):
            item[field] = normalize_definition(item[field])
    return result


def private_output(path):
    path = Path(path).absolute()
    if path.is_symlink() or path.exists():
        raise BackupError("備份輸出必須是不存在的新目錄，不覆蓋既有資料。")
    resolved = path.resolve()
    if resolved.is_relative_to(ROOT) and not resolved.is_relative_to(ROOT / "work"):
        raise BackupError("倉庫內的敏感備份只能存放在 Git 忽略的 work 目錄。")
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    path.mkdir(mode=0o700)
    return path


def write_json(path, value):
    with path.open("x", encoding="utf-8") as stream:
        os.chmod(path, 0o600)
        json.dump(value, stream, ensure_ascii=False, indent=2)
        stream.write("\n")
        stream.flush()
        os.fsync(stream.fileno())


class Docker:
    def __init__(self, host=None, timeout=300):
        self.command = ["docker"] + (["--host=" + host] if host else [])
        self.timeout = timeout

    def run(self, args, *, stdin=None, stdout=subprocess.PIPE):
        try:
            result = subprocess.run(self.command + args, stdin=stdin, stdout=stdout,
                                    stderr=subprocess.PIPE, timeout=self.timeout, check=False)
        except (OSError, subprocess.TimeoutExpired) as exc:
            raise BackupError("Docker 操作失敗或逾時；不輸出可能含敏感資料的原始錯誤。") from exc
        if result.returncode:
            raise BackupError("Docker／PostgreSQL 操作失敗；不輸出可能含資料或憑證的原始錯誤。")
        return result.stdout

    def inspect(self, container, require_running=True):
        fmt = '{"id":{{json .Id}},"name":{{json .Name}},"image":{{json .Image}},"running":{{json .State.Running}},"labels":{{json .Config.Labels}}}'
        result = json.loads(self.run(["inspect", "--format", fmt, container]))
        if require_running and not result["running"]:
            raise BackupError("PostgreSQL 容器未運行。")
        return result

    def exec(self, container, command, **kwargs):
        return self.run(["exec", "-i", container] + command, **kwargs)

    def sql(self, container, database, user, sql):
        identifier(database)
        identifier(user)
        with tempfile.TemporaryFile() as source:
            source.write(sql.encode("utf-8"))
            source.seek(0)
            return self.exec(container, ["psql", "-XqAt", "--no-password", "-v", "ON_ERROR_STOP=1",
                                         "-U", user, "-d", database], stdin=source)


class Snapshot:
    """保持 exported snapshot，pg_dump 與資料指紋讀取同一個 MVCC 快照。"""
    def __init__(self, docker, container, database, user):
        identifier(database)
        identifier(user)
        self.docker = docker
        self.lines = queue.Queue(maxsize=64)
        self.errors = tempfile.TemporaryFile()
        self.process = subprocess.Popen(docker.command + ["exec", "-i", container, "psql", "-XqAt",
            "--no-password", "-v", "ON_ERROR_STOP=1", "-U", user, "-d", database],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=self.errors)
        self.reader = threading.Thread(target=self._read, daemon=True)
        self.reader.start()

    def _read(self):
        try:
            for line in self.process.stdout:
                self.lines.put(line)
        finally:
            self.lines.put(None)

    def consume(self, sql, consumer):
        marker = ("camp_end_" + uuid.uuid4().hex).encode()
        try:
            self.process.stdin.write(sql.encode() + b"\n\\echo " + marker + b"\n")
            self.process.stdin.flush()
            deadline = time.monotonic() + self.docker.timeout
            while True:
                line = self.lines.get(timeout=max(0.01, deadline - time.monotonic()))
                if line is None:
                    raise BackupError("PostgreSQL 快照查詢失敗。")
                if line.rstrip(b"\r\n") == marker:
                    return
                consumer(line)
        except (BrokenPipeError, queue.Empty) as exc:
            raise BackupError("PostgreSQL 快照查詢失敗或逾時。") from exc

    def query(self, sql):
        lines = []
        self.consume(sql, lines.append)
        return b"".join(lines).decode().strip()

    def __enter__(self):
        try:
            self.query("BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY; SET statement_timeout='120s'; SET lock_timeout='5s'; SET timezone='UTC'; SET datestyle='ISO, YMD';")
            self.snapshot = self.query("SELECT pg_export_snapshot();")
            self.version = int(self.query("SHOW server_version_num;")) // 10000
            return self
        except BaseException:
            self.close()
            raise

    def close(self):
        # Docker exec 的 stdin 關閉讓 psql 離開，未提交 read-only transaction 回滾。
        with contextlib.suppress(OSError):
            self.process.stdin.close()
        try:
            self.process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            self.process.kill()
            self.process.wait()
        self.reader.join(timeout=2)
        self.process.stdout.close()
        self.errors.close()

    def __exit__(self, *_args):
        self.close()


SCHEMA_SQL = """
SELECT json_build_object(
 'columns', (SELECT coalesce(json_agg(row_to_json(x) ORDER BY x.table_name,x.ordinal_position),'[]') FROM
   (SELECT table_name,column_name,ordinal_position,data_type,udt_name,is_nullable,column_default,
           is_identity,identity_generation,character_maximum_length,numeric_precision,numeric_scale
    FROM information_schema.columns WHERE table_schema='public') x),
 'constraints', (SELECT coalesce(json_agg(row_to_json(x) ORDER BY x.relname,x.conname),'[]') FROM
   (SELECT c.relname,k.conname,k.contype,k.convalidated,pg_get_constraintdef(k.oid) AS definition
    FROM pg_constraint k JOIN pg_class c ON c.oid=k.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE n.nspname='public') x),
 'indexes', (SELECT coalesce(json_agg(row_to_json(x) ORDER BY x.tablename,x.indexname),'[]') FROM
   (SELECT tablename,indexname,indexdef FROM pg_indexes WHERE schemaname='public') x),
 'views', (SELECT coalesce(json_agg(row_to_json(x) ORDER BY x.viewname),'[]') FROM
   (SELECT viewname,definition FROM pg_views WHERE schemaname='public') x),
 'sequences', (SELECT coalesce(json_agg(row_to_json(x) ORDER BY x.sequencename),'[]') FROM
   (SELECT sequencename,data_type,start_value,min_value,max_value,increment_by,cycle,cache_size
    FROM pg_sequences WHERE schemaname='public') x));
"""


def fingerprint(snapshot):
    names = json.loads(snapshot.query("SELECT coalesce(json_agg(tablename ORDER BY tablename),'[]') FROM pg_tables WHERE schemaname='public';"))
    if not REQUIRED.issubset(names):
        raise BackupError("缺少發呆亭必要資料表；請先依序升級至 migration 008。")
    schema = json.loads(snapshot.query(SCHEMA_SQL))
    if any(c["contype"] == "f" and not c["convalidated"] for c in schema["constraints"]):
        raise BackupError("來源存在未驗證外鍵，請先人工核對。")
    tables = {}
    for name in names:
        sha = hashlib.sha256()
        count = 0

        def row(line):
            nonlocal count
            sha.update(line)
            count += 1

        snapshot.consume("COPY (SELECT to_jsonb(t)::text FROM public." + identifier(name) +
                         " t ORDER BY to_jsonb(t)::text COLLATE \"C\") TO STDOUT;", row)
        tables[name] = {"rows": count, "sha256": sha.hexdigest()}
    return normalized_expected({"schema": schema, "tables": tables})


def backup(docker, container, database, user, output):
    identifier(database)
    identifier(user)
    info = docker.inspect(container)
    path = private_output(output)
    try:
        with Snapshot(docker, info["id"], database, user) as snapshot:
            expected = fingerprint(snapshot)
            dump = path / "database.dump"
            with dump.open("xb") as stream:
                os.chmod(dump, 0o600)
                docker.exec(info["id"], ["pg_dump", "--no-password", "-U", user, "-d", database,
                    "--format=custom", "--schema=public", "--no-owner", "--no-privileges",
                    "--snapshot=" + snapshot.snapshot], stdout=stream)
                stream.flush()
                os.fsync(stream.fileno())
            with dump.open("rb") as stream:
                docker.exec(info["id"], ["pg_restore", "--list"], stdin=stream, stdout=subprocess.DEVNULL)
            manifest = {"format": 1, "project": PROJECT, "database": database,
                        "sourceContainerId": info["id"], "imageId": info["image"],
                        "postgresMajor": snapshot.version, "scope": "public",
                        "archiveSha256": digest(dump), "expected": expected}
            write_json(path / "manifest.json", manifest)
        return {"backup": str(path), "tables": len(expected["tables"]), "snapshotConsistent": True,
                "archiveSha256": manifest["archiveSha256"]}
    except BaseException:
        # 只刪本次 exclusive mkdir 所建立的私有輸出，既有備份永不覆蓋。
        shutil.rmtree(path)
        raise


def load_backup(directory):
    directory = Path(directory)
    if directory.is_symlink() or not directory.is_dir():
        raise BackupError("備份目錄無效。")
    manifest_path, archive = directory / "manifest.json", directory / "database.dump"
    if any(p.is_symlink() or not p.is_file() for p in (manifest_path, archive)):
        raise BackupError("備份不完整或含符號連結。")
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        valid = (manifest["format"] == 1 and manifest["project"] == PROJECT and manifest["scope"] == "public"
                 and manifest["postgresMajor"] >= 14
                 and re.fullmatch(r"sha256:[0-9a-f]{64}", manifest["imageId"])
                 and re.fullmatch(r"[0-9a-f]{64}", manifest["sourceContainerId"])
                 and manifest["archiveSha256"] == digest(archive)
                 and REQUIRED.issubset(manifest["expected"]["tables"]))
        identifier(manifest["database"])
    except (KeyError, TypeError, ValueError, OSError) as exc:
        raise BackupError("備份 metadata／SHA-256 核對失敗。") from exc
    if not valid:
        raise BackupError("備份格式、專案或 SHA-256 核對失敗。")
    return manifest, archive


def remove_owned(docker, container, token):
    info = docker.inspect(container, require_running=False)
    if (not re.fullmatch(r"[0-9a-f]{32}", token)
            or (info.get("labels") or {}).get(LABEL) != token
            or info.get("name") != "/camp-restore-" + token):
        raise BackupError("還原容器所有權不符，拒絕清理。")
    docker.run(["rm", "-f", "-v", info["id"]])


def restore_check(docker, directory, keep=False):
    manifest, archive = load_backup(directory)  # 在建立任何容器前先驗證內容。
    token = uuid.uuid4().hex
    name = "camp-restore-" + token
    container = None
    success = False
    try:
        container = docker.run(["create", "--pull=never", "--name", name,
            "--label", LABEL + "=" + token, "--network=none", "--memory=512m", "--cpus=1",
            "--tmpfs", "/camp-restore-data:rw,nosuid,nodev,size=512m",
            "-e", "PGDATA=/camp-restore-data", "-e", "POSTGRES_DB=fadaiting_restore",
            "-e", "POSTGRES_USER=camp", "-e", "POSTGRES_HOST_AUTH_METHOD=trust",
            manifest["imageId"]]).decode().strip()
        if container == manifest["sourceContainerId"]:
            raise BackupError("還原容器不可等於來源。")
        docker.run(["start", container], stdout=subprocess.DEVNULL)
        deadline = time.monotonic() + min(docker.timeout, 45)
        while True:
            try:
                version = int(docker.sql(container, "fadaiting_restore", "camp", "SHOW server_version_num;")) // 10000
                break
            except BackupError:
                if time.monotonic() >= deadline:
                    raise BackupError("隔離 PostgreSQL 啟動逾時。")
                time.sleep(0.25)
        if version != manifest["postgresMajor"]:
            raise BackupError("PostgreSQL major 不符。")
        with archive.open("rb") as stream:
            # 開啟同一個 file handle 後重驗 checksum，避免驗證後檔案被替換。
            sha = hashlib.sha256()
            for block in iter(lambda: stream.read(1024 * 1024), b""):
                sha.update(block)
            if sha.hexdigest() != manifest["archiveSha256"]:
                raise BackupError("還原前 archive 已變動。")
            stream.seek(0)
            docker.exec(container, ["pg_restore", "--no-password", "-U", "camp", "-d", "fadaiting_restore",
                "--exit-on-error", "--single-transaction", "--clean", "--if-exists", "--no-owner", "--no-privileges",
                "--no-security-labels"], stdin=stream, stdout=subprocess.DEVNULL)
        with Snapshot(docker, container, "fadaiting_restore", "camp") as snapshot:
            actual = fingerprint(snapshot)
        if actual != normalized_expected(manifest["expected"]):
            raise BackupError("還原後資料／結構／外鍵指紋不符。")
        result = {"verified": True, "tables": len(actual["tables"]),
                  "rows": sum(t["rows"] for t in actual["tables"].values()),
                  "foreignKeys": sum(c["contype"] == "f" for c in actual["schema"]["constraints"]),
                  "views": len(actual["schema"]["views"]),
                  "sequences": len(actual["schema"]["sequences"]),
                  "archiveSha256": manifest["archiveSha256"], "kept": keep}
        if keep:
            result.update(container=container, name=name, cleanupToken=token)
        success = True
        return result
    finally:
        # 失敗亦清理本次容器；keep 只保留已完整核對成功的實例。
        if container and (not keep or not success):
            remove_owned(docker, container, token)


def main():
    parser = argparse.ArgumentParser(description="發呆亭 PostgreSQL 備份與一次性隔離還原核對（Linux／WSL）")
    parser.add_argument("--docker-host", help="明確選擇 Docker daemon；不改全域設定")
    parser.add_argument("--timeout", type=int, default=300)
    parser.add_argument("--journal", type=Path, help="私有執行紀錄目錄；只供後台唯讀觀測")
    parser.add_argument("--source-key", help="與後台一致的來源識別碼；使用 journal 時必填")
    sub = parser.add_subparsers(dest="action", required=True)
    make = sub.add_parser("backup")
    make.add_argument("--container", required=True)
    make.add_argument("--database", default="fadaiting_camp")
    make.add_argument("--user", default="camp")
    make.add_argument("--output", type=Path, required=True)
    restore = sub.add_parser("restore-check")
    restore.add_argument("--backup", type=Path, required=True)
    restore.add_argument("--keep", action="store_true")
    clean = sub.add_parser("cleanup")
    clean.add_argument("--container", required=True)
    clean.add_argument("--token", required=True)
    args = parser.parse_args()
    if not 1 <= args.timeout <= 3600:
        parser.error("timeout 必須為 1–3600 秒。")
    if bool(args.journal) != bool(args.source_key) or (args.journal and args.action == "cleanup"):
        parser.error("journal／source-key 須成對指定，且僅支援 backup／restore-check。")
    docker = Docker(args.docker_host, args.timeout)
    try:
        if args.action == "backup":
            operation = lambda: backup(docker, args.container, args.database, args.user, args.output)
        elif args.action == "restore-check":
            operation = lambda: restore_check(docker, args.backup, args.keep)
        else:
            remove_owned(docker, args.container, args.token)
            result = {"removed": True}
        if args.action != "cleanup":
            result = recorded_operation(args.journal, args.source_key, args.action, operation) if args.journal else operation()
        print(json.dumps(result, ensure_ascii=False))
    except (BackupError, JournalError, OSError) as exc:
        print(json.dumps({"error": str(exc)}, ensure_ascii=False))
        raise SystemExit(1)


if __name__ == "__main__":
    main()
