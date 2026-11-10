"""私有 SeaweedFS 設定的防覆寫與憑證邊界；不啟動服務。"""
import json
import os
from pathlib import Path
import secrets
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class StorageConfigGuards(unittest.TestCase):
    def setUp(self):
        (ROOT/'work').mkdir(exist_ok=True)
        self.scope = tempfile.TemporaryDirectory(dir=ROOT/'work')
        self.root = Path(self.scope.name)
        self.target = self.root/'s3.json'
        self.env = {**os.environ, 'CAMP_STORAGE_ACCESS_KEY': secrets.token_hex(12),
                    'CAMP_STORAGE_SECRET_KEY': secrets.token_hex(24)}

    def tearDown(self):
        self.scope.cleanup()

    def run_tool(self):
        return subprocess.run(['python3', '-B', str(ROOT/'scripts/configure-camp-storage.py'),
                               '--output', str(self.target)], env=self.env, capture_output=True, text=True)

    def test_exact_bucket_scope_private_mode_and_no_secret_output(self):
        result = self.run_tool(); self.assertEqual(0, result.returncode)
        self.assertEqual(0o600, self.target.stat().st_mode & 0o777)
        identities = json.loads(self.target.read_text())['identities']
        self.assertEqual(1, len(identities))
        self.assertNotEqual('anonymous', identities[0]['name'])
        self.assertEqual({a + ':' + b for b in ['user-avatar', 'camp-images', 'rental-item-images']
                          for a in ['Admin', 'Read', 'Write', 'List', 'Tagging']}, set(identities[0]['actions']))
        for key in ['CAMP_STORAGE_ACCESS_KEY', 'CAMP_STORAGE_SECRET_KEY']:
            self.assertNotIn(self.env[key], result.stdout + result.stderr)

    def test_existing_credentials_are_not_overwritten(self):
        self.target.write_bytes(b'original-private-config')
        self.assertNotEqual(0, self.run_tool().returncode)
        self.assertEqual(b'original-private-config', self.target.read_bytes())

    def test_symlink_cannot_redirect_write(self):
        original = self.root/'original'; original.write_bytes(b'original')
        self.target.symlink_to(original)
        self.assertNotEqual(0, self.run_tool().returncode)
        self.assertEqual(b'original', original.read_bytes())

    def test_public_parent_is_refused(self):
        self.root.chmod(0o755)
        self.assertNotEqual(0, self.run_tool().returncode)
        self.assertFalse(self.target.exists())

    def test_missing_or_short_credentials_leave_no_file(self):
        for key in ['CAMP_STORAGE_ACCESS_KEY', 'CAMP_STORAGE_SECRET_KEY']:
            original = self.env[key]; self.env[key] = ''
            self.assertNotEqual(0, self.run_tool().returncode)
            self.assertFalse(self.target.exists()); self.env[key] = original


if __name__ == '__main__':
    unittest.main()
