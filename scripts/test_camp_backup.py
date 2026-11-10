"""備份破壞性保護與真實 PostgreSQL 演練；fixture 僅寫入一次性容器。"""
import copy
import json
import os
from pathlib import Path
import shutil
import tempfile
import unittest
from unittest.mock import Mock, patch

import camp_backup as b


class BackupGuards(unittest.TestCase):
    def setUp(self):
        (b.ROOT / "work").mkdir(exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(dir=b.ROOT / "work")
        self.root = Path(self.temp.name)
        self.archive = self.root / "database.dump"
        self.archive.write_bytes(b"opaque fixture archive")
        self.manifest = {"format": 1, "project": b.PROJECT, "scope": "public",
            "database": "fadaiting_camp", "postgresMajor": 17,
            "sourceContainerId": "a" * 64, "imageId": "sha256:" + "b" * 64,
            "archiveSha256": b.digest(self.archive),
            "expected": {"tables": {name: {"rows": 0, "sha256": "c" * 64} for name in b.REQUIRED}}}
        self.write_manifest()

    def tearDown(self):
        self.temp.cleanup()

    def write_manifest(self):
        (self.root / "manifest.json").write_text(json.dumps(self.manifest))

    def test_corruption_rejected_before_docker_operations(self):
        self.archive.write_bytes(b"truncated")
        docker = Mock()
        with self.assertRaises(b.BackupError):
            b.restore_check(docker, self.root)
        docker.run.assert_not_called()

    def test_wrong_project_format_image_and_missing_tables_rejected(self):
        original = copy.deepcopy(self.manifest)
        for key, value in [("project", "other-project"), ("format", 2), ("imageId", "postgres:latest"),
                           ("sourceContainerId", "untrusted"), ("expected", {"tables": {}})]:
            with self.subTest(key=key):
                self.manifest = copy.deepcopy(original)
                self.manifest[key] = value
                self.write_manifest()
                with self.assertRaises(b.BackupError):
                    b.load_backup(self.root)

    def test_missing_and_invalid_manifest_rejected(self):
        p = self.root / "manifest.json"
        p.write_text("not JSON")
        with self.assertRaises(b.BackupError):
            b.load_backup(self.root)
        p.unlink()
        with self.assertRaises(b.BackupError):
            b.load_backup(self.root)

    def test_symlink_archive_and_directory_rejected(self):
        other = self.root / "other.dump"
        self.archive.rename(other)
        self.archive.symlink_to(other)
        with self.assertRaises(b.BackupError):
            b.load_backup(self.root)
        alias = self.root / "alias"
        alias.symlink_to(self.root, target_is_directory=True)
        with self.assertRaises(b.BackupError):
            b.load_backup(alias)

    def test_existing_output_never_overwritten(self):
        before = self.archive.read_bytes()
        with self.assertRaises(b.BackupError):
            b.private_output(self.root)
        self.assertEqual(self.archive.read_bytes(), before)

    def test_sensitive_backup_cannot_be_written_to_repo_source(self):
        with self.assertRaises(b.BackupError):
            b.private_output(b.ROOT / "docs" / "forbidden-backup-fixture")

    def test_identifier_rejects_options_sql_and_path_injection(self):
        for value in ["--dbname=postgres", "a;DROP TABLE booking", "../data", "a\n", "", None]:
            with self.subTest(value=value), self.assertRaises(b.BackupError):
                b.identifier(value)

    def test_only_literal_varchar_array_roundtrip_is_normalized(self):
        source = "CHECK (status = ANY ((ARRAY['中文'::character varying, 'can''t, stop'::character varying])::text[]))"
        target = "CHECK (status = ANY (ARRAY[('中文'::character varying)::text, ('can''t, stop'::character varying)::text]))"
        self.assertEqual(b.normalize_definition(source), target)
        self.assertNotEqual(b.normalize_definition(source.replace('中文', '其他')), target)
        for other in ['(ARRAY[status]::text[])', 'value::character varying(3)', "'1'::integer"]:
            self.assertEqual(b.normalize_definition(other), other)

    def test_cleanup_rejects_source_and_forged_ownership(self):
        docker = Mock()
        token = "e" * 32
        for info in [{"id": "source", "name": "/camp-roles-cloud-postgres", "labels": {}},
                     {"id": "other", "name": "/other-service", "labels": {b.LABEL: token}}]:
            docker.inspect.return_value = info
            with self.assertRaises(b.BackupError):
                b.remove_owned(docker, "source", token)
            docker.run.assert_not_called()

    def test_stopped_owned_container_can_be_cleaned(self):
        docker = Mock()
        token = "e" * 32
        docker.inspect.return_value = {"id": "owned", "name": "/camp-restore-" + token,
                                     "running": False, "labels": {b.LABEL: token}}
        b.remove_owned(docker, "owned", token)
        docker.inspect.assert_called_once_with("owned", require_running=False)
        docker.run.assert_called_once_with(["rm", "-f", "-v", "owned"])

    def test_failed_dump_removes_only_new_partial_output(self):
        target = self.root / "partial"
        docker = Mock()
        docker.inspect.return_value = {"id": "source", "image": "sha256:" + "b" * 64}
        with patch.object(b, "Snapshot") as snap:
            snap.return_value.__enter__.side_effect = b.BackupError("failure fixture")
            with self.assertRaises(b.BackupError):
                b.backup(docker, "source", "fadaiting_camp", "camp", target)
        self.assertFalse(target.exists())
        self.assertTrue(self.archive.exists())


@unittest.skipUnless(os.getenv("CAMP_BACKUP_INTEGRATION") == "true", "需明確啟用隔離 PostgreSQL 演練")
class PostgreSQLDrill(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.docker = b.Docker(os.getenv("CAMP_BACKUP_DOCKER_HOST"))
        cls.source = os.environ["CAMP_BACKUP_SOURCE"]
        cls.temp = tempfile.TemporaryDirectory(dir=b.ROOT / "work")
        cls.root = Path(cls.temp.name)
        try:
            b.backup(cls.docker, cls.source, "fadaiting_camp", "camp", cls.root / "baseline")
            cls.baseline = b.load_backup(cls.root / "baseline")[0]
            cls.clone = b.restore_check(cls.docker, cls.root / "baseline", keep=True)
            cls.container = cls.clone["container"]
            cls.docker.sql(cls.container, "fadaiting_restore", "camp", """
WITH booked AS (
 INSERT INTO booking(booking_no,camp_site_id,status,check_in_date,check_out_date,nights,total_amount,paid_amount,note)
 SELECT 'CLOUD-BACKUP-FIXTURE',min(id),'confirmed','2035-01-01','2035-01-02',1,3000,1000,'中文備份演練 😀'
 FROM camp_site RETURNING id,camp_site_id
), guest AS (
 INSERT INTO booking_guest(booking_id,contact_name,contact_phone,contact_email)
 SELECT id,'備份演練訪客','0000000000','fixture@example.invalid' FROM booked
), item AS (
 INSERT INTO booking_item(booking_id,camp_unit_id,item_name,quantity,unit_price,line_total)
 SELECT b.id,(SELECT min(id) FROM camp_unit WHERE camp_site_id=b.camp_site_id),'合成營位',1,3000,3000 FROM booked b
), timeline AS (
 INSERT INTO booking_timeline(booking_id,action_type,action_label,detail)
 SELECT id,'backup_fixture','隔離還原演練','不寄信、不轉帳、來源資料不修改' FROM booked
), paid AS (
 INSERT INTO payment_record(booking_id,provider,payment_method,status,amount,merchant_order_no)
 SELECT id,'mock','mock','paid',1000,'CLOUD-BACKUP-PAYMENT' FROM booked RETURNING id
), refunded AS (
 INSERT INTO refund_record(payment_record_id,amount,status,idempotency_key,reason,requested_by,decided_by,completed_by,
   requested_at,decided_at,completed_at,external_reference)
 SELECT p.id,100,'completed',gen_random_uuid(),'合成退款',(SELECT min(id) FROM app_user),
   (SELECT min(id) FROM app_user),(SELECT min(id) FROM app_user),now(),now(),now(),'CLOUD-FIXTURE' FROM paid p
), notified AS (
 INSERT INTO notification_record(booking_id,channel,event_type,recipient,message,status,queue_managed,event_key,attempt_count,delivery_mode)
 SELECT id,'email','backup_fixture','fixture@example.invalid','中文通知 😀','simulated',true,'CLOUD-BACKUP-EVENT',1,'simulate'
 FROM booked RETURNING id
)
INSERT INTO notification_attempt(notification_id,attempt_number,lease_token,delivery_mode,status,started_at,finished_at)
SELECT id,1,gen_random_uuid(),'simulate','simulated',now(),now() FROM notified;
""")
            b.backup(cls.docker, cls.container, "fadaiting_restore", "camp", cls.root / "fixture")
            cls.expected = b.load_backup(cls.root / "fixture")[0]["expected"]
        except BaseException:
            if hasattr(cls, "clone"):
                b.remove_owned(cls.docker, cls.clone["container"], cls.clone["cleanupToken"])
            cls.temp.cleanup()
            raise

    @classmethod
    def tearDownClass(cls):
        try:
            with b.Snapshot(cls.docker, cls.source, "fadaiting_camp", "camp") as snapshot:
                actual = b.fingerprint(snapshot)
            if actual != cls.baseline["expected"]:
                raise AssertionError("來源資料或結構在演練期間變動。")
        finally:
            b.remove_owned(cls.docker, cls.clone["container"], cls.clone["cleanupToken"])
            cls.temp.cleanup()

    def containers(self):
        return set(self.docker.run(["ps", "-aq", "--filter", "label=" + b.LABEL]).splitlines())

    def clone_fingerprint(self):
        with b.Snapshot(self.docker, self.container, "fadaiting_restore", "camp") as snapshot:
            return b.fingerprint(snapshot)

    def copy_fixture(self, name):
        target = self.root / name
        shutil.copytree(self.root / "fixture", target)
        return target

    def test_chinese_relations_sequence_and_private_files(self):
        result = b.restore_check(self.docker, self.root / "fixture", keep=True)
        try:
            self.assertTrue(result["verified"])
            self.assertGreater(result["foreignKeys"], 0)
            self.assertGreater(result["sequences"], 0)
            sql = """SELECT g.contact_name || '|' || n.message FROM booking b
 JOIN booking_guest g ON g.booking_id=b.id JOIN booking_item i ON i.booking_id=b.id
 JOIN payment_record p ON p.booking_id=b.id JOIN refund_record r ON r.payment_record_id=p.id
 JOIN notification_record n ON n.booking_id=b.id JOIN notification_attempt a ON a.notification_id=n.id
 WHERE b.booking_no='CLOUD-BACKUP-FIXTURE';"""
            actual = self.docker.sql(result["container"], "fadaiting_restore", "camp", sql).decode().strip()
            self.assertEqual(actual, "備份演練訪客|中文通知 😀")
            source_sequence = self.docker.sql(self.container, "fadaiting_restore", "camp", "SELECT last_value,is_called FROM booking_id_seq;")
            target_sequence = self.docker.sql(result["container"], "fadaiting_restore", "camp", "SELECT last_value,is_called FROM booking_id_seq;")
            self.assertEqual(source_sequence, target_sequence)
            last, called = source_sequence.decode().strip().split("|")
            inserted = self.docker.sql(result["container"], "fadaiting_restore", "camp", """INSERT INTO booking
 (booking_no,camp_site_id,check_in_date,check_out_date) SELECT 'TARGET-ONLY-INSERT',min(id),'2035-02-01','2035-02-02'
 FROM camp_site RETURNING id;""")
            self.assertEqual(int(inserted), int(last) + (called == "t"))
            self.assertEqual((self.root / "fixture").stat().st_mode & 0o777, 0o700)
            for file in (self.root / "fixture").iterdir():
                self.assertEqual(file.stat().st_mode & 0o777, 0o600)
            info = self.docker.run(["inspect", "--format", '{{json .HostConfig.NetworkMode}} {{json .HostConfig.PortBindings}}', result["container"]]).decode()
            self.assertIn('"none"', info)
            self.assertIn('{}', info)
        finally:
            b.remove_owned(self.docker, result["container"], result["cleanupToken"])

    def test_corrupt_bytes_create_no_container(self):
        path = self.copy_fixture("corrupt")
        (path / "database.dump").write_bytes(b"corrupt fixture")
        before = self.containers()
        with self.assertRaises(b.BackupError):
            b.restore_check(self.docker, path)
        self.assertEqual(self.containers(), before)

    def test_restore_failure_cleans_target_and_preserves_source(self):
        path = self.copy_fixture("broken-pg")
        archive = path / "database.dump"
        archive.write_bytes(archive.read_bytes()[:128])
        manifest = json.loads((path / "manifest.json").read_text())
        manifest["archiveSha256"] = b.digest(archive)
        (path / "manifest.json").write_text(json.dumps(manifest))
        before = self.containers()
        source = self.clone_fingerprint()
        with self.assertRaises(b.BackupError):
            b.restore_check(self.docker, path, keep=True)
        self.assertEqual(self.containers(), before)
        self.assertEqual(self.clone_fingerprint(), source)

    def test_valid_archive_with_wrong_expected_data_is_rejected(self):
        path = self.copy_fixture("wrong-expected")
        manifest = json.loads((path / "manifest.json").read_text())
        manifest["expected"]["tables"]["booking"]["sha256"] = "0" * 64
        (path / "manifest.json").write_text(json.dumps(manifest))
        before = self.containers()
        with self.assertRaises(b.BackupError):
            b.restore_check(self.docker, path)
        self.assertEqual(self.containers(), before)

    def test_existing_backup_and_wrong_schema_preserve_data(self):
        before = b.digest(self.root / "fixture" / "database.dump")
        with self.assertRaises(b.BackupError):
            b.backup(self.docker, self.container, "fadaiting_restore", "camp", self.root / "fixture")
        self.assertEqual(b.digest(self.root / "fixture" / "database.dump"), before)
        with self.assertRaises(b.BackupError):
            b.backup(self.docker, self.container, "postgres", "camp", self.root / "incomplete")
        self.assertFalse((self.root / "incomplete").exists())
        self.assertEqual(self.clone_fingerprint(), self.expected)

    def test_exported_snapshot_excludes_concurrent_committed_write(self):
        original = b.fingerprint
        before = self.clone_fingerprint()

        def write_after_fingerprint(snapshot):
            result = original(snapshot)
            self.docker.sql(self.container, "fadaiting_restore", "camp", """INSERT INTO booking
 (booking_no,camp_site_id,check_in_date,check_out_date) SELECT 'CONCURRENT-SNAPSHOT-INSERT',min(id),'2035-03-01','2035-03-02' FROM camp_site;""")
            return result

        try:
            with patch.object(b, "fingerprint", side_effect=write_after_fingerprint):
                b.backup(self.docker, self.container, "fadaiting_restore", "camp", self.root / "concurrent")
            expected = b.load_backup(self.root / "concurrent")[0]["expected"]
            self.assertEqual(expected, before)
            self.assertEqual(self.clone_fingerprint()["tables"]["booking"]["rows"], before["tables"]["booking"]["rows"] + 1)
            self.assertTrue(b.restore_check(self.docker, self.root / "concurrent")["verified"])
        finally:
            self.docker.sql(self.container, "fadaiting_restore", "camp", "DELETE FROM booking WHERE booking_no='CONCURRENT-SNAPSHOT-INSERT';")


if __name__ == "__main__":
    unittest.main(verbosity=2)
