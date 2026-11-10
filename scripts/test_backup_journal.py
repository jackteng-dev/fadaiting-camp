"""紀錄與操作結果分離、原子保存及私有儲存保護測試。"""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock, patch
import backup_journal as journal


class BackupJournalTest(unittest.TestCase):
    def setUp(self):
        (journal.ROOT / "work").mkdir(exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(dir=journal.ROOT / "work")
        self.root = Path(self.temp.name)

    def tearDown(self):
        self.temp.cleanup()

    def receipt(self):
        files = list((self.root / "journal").glob("*.json"))
        self.assertEqual(len(files), 1)
        return json.loads(files[0].read_text())

    def test_success_backup_is_not_restore_success_and_contains_no_private_data(self):
        output = journal.recorded_operation(self.root / "journal", "primary", "backup",
            lambda: {"archiveSha256": "a" * 64, "backup": "/private/source-path"})
        receipt = self.receipt()
        self.assertTrue(output["recorded"])
        self.assertEqual(receipt["action"], "backup")
        self.assertEqual(receipt["status"], "success")
        self.assertEqual(receipt["archiveSha256"], "a" * 64)
        self.assertNotIn("private", json.dumps(receipt))
        self.assertEqual((self.root / "journal").stat().st_mode & 0o777, 0o700)
        self.assertEqual(next((self.root / "journal").glob("*.json")).stat().st_mode & 0o777, 0o600)

    def test_restore_success_requires_verified_true(self):
        with self.assertRaises(journal.JournalError):
            journal.recorded_operation(self.root / "journal", "primary", "restore-check",
                lambda: {"verified": False, "archiveSha256": "a" * 64})
        self.assertEqual(self.receipt()["status"], "failed")

    def test_restore_success_uses_same_archive_hash(self):
        journal.recorded_operation(self.root / "journal", "primary", "restore-check",
            lambda: {"verified": True, "archiveSha256": "b" * 64})
        self.assertEqual(self.receipt()["action"], "restore-check")
        self.assertEqual(self.receipt()["archiveSha256"], "b" * 64)

    def test_failure_is_recorded_without_raw_exception(self):
        def operation():
            raise RuntimeError("private-password/path/recipient")
        with self.assertRaises(RuntimeError):
            journal.recorded_operation(self.root / "journal", "primary", "backup", operation)
        receipt = self.receipt()
        self.assertEqual(receipt["status"], "failed")
        self.assertEqual(receipt["errorCode"], "OPERATION_FAILED")
        self.assertNotIn("private", json.dumps(receipt))
        self.assertIsNone(receipt["archiveSha256"])

    def test_abrupt_interruption_keeps_running_not_success(self):
        with self.assertRaises(KeyboardInterrupt):
            journal.recorded_operation(self.root / "journal", "primary", "backup",
                Mock(side_effect=KeyboardInterrupt))
        self.assertEqual(self.receipt()["status"], "running")
        self.assertIsNone(self.receipt()["finishedAt"])

    def test_invalid_path_or_source_stops_before_operation(self):
        operation = Mock()
        for directory, key in [(journal.ROOT / "docs" / "forbidden-journal", "primary"),
                               (self.root / "journal", "../other-project")]:
            with self.assertRaises(journal.JournalError):
                journal.recorded_operation(directory, key, "backup", operation)
        operation.assert_not_called()

    def test_symlink_and_shared_directory_rejected(self):
        alias = self.root / "alias"
        alias.symlink_to(self.root, target_is_directory=True)
        with self.assertRaises(journal.JournalError):
            journal.BackupJournal(alias, "primary", "backup")
        shared = self.root / "shared"
        shared.mkdir(mode=0o755)
        shared.chmod(0o755)
        with self.assertRaises(journal.JournalError):
            journal.BackupJournal(shared, "primary", "backup")

    def test_start_write_failure_does_not_start_operation(self):
        operation = Mock()
        with patch.object(journal.os, "replace", side_effect=OSError), self.assertRaises(journal.JournalError):
            journal.recorded_operation(self.root / "journal", "primary", "backup", operation)
        operation.assert_not_called()
        self.assertEqual(list((self.root / "journal").iterdir()), [])

    def test_finish_write_failure_preserves_artifact_and_running_receipt(self):
        artifact = self.root / "completed.dump"
        def operation():
            artifact.write_bytes(b"completed artifact")
            return {"archiveSha256": "c" * 64}
        original = journal.os.replace
        calls = 0
        def replace(source, target):
            nonlocal calls
            calls += 1
            if calls == 1:
                return original(source, target)
            raise OSError()
        with patch.object(journal.os, "replace", side_effect=replace), self.assertRaises(journal.JournalError):
            journal.recorded_operation(self.root / "journal", "primary", "backup", operation)
        self.assertTrue(artifact.exists())
        self.assertEqual(self.receipt()["status"], "running")

    def test_multiple_attempts_keep_history_and_source_isolation(self):
        for key, action in [("primary", "backup"), ("primary", "restore-check"), ("staging", "backup")]:
            journal.recorded_operation(self.root / "journal", key, action,
                lambda: {"verified": True, "archiveSha256": "d" * 64})
        records = [json.loads(p.read_text()) for p in (self.root / "journal").glob("*.json")]
        self.assertEqual(len({r["runId"] for r in records}), 3)
        self.assertEqual({r["sourceKey"] for r in records}, {"primary", "staging"})


if __name__ == "__main__":
    unittest.main(verbosity=2)
