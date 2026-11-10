#!/usr/bin/env python3
"""產生新的本機私有 S3 設定；不連主機、不部署、不覆寫既有檔案。"""
import argparse
import json
import os
from pathlib import Path

BUCKETS = ['user-avatar', 'camp-images', 'rental-item-images']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    access = os.environ.get('CAMP_STORAGE_ACCESS_KEY', '')
    secret = os.environ.get('CAMP_STORAGE_SECRET_KEY', '')
    if len(access) < 16 or len(secret) < 32:
        raise SystemExit('請使用至少 16／32 字元的 Camp 專用隨機 S3 金鑰')
    target = args.output.absolute()
    # 憑證只能落在明確的 private scope，拒絕 symlink 與群組／其他人可讀父目錄。
    if any(p.is_symlink() for p in [target, *target.parents]) or not target.parent.is_dir() or target.parent.stat().st_mode & 0o077:
        raise SystemExit('輸出父目錄必須存在、無 symlink 且權限為 0700')
    data = {'identities': [{'name': 'camp-images', 'credentials': [{'accessKey': access, 'secretKey': secret}],
            'actions': [action + ':' + bucket for bucket in BUCKETS for action in ['Admin', 'Read', 'Write', 'List', 'Tagging']]}]}
    descriptor = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(descriptor, 'w') as stream:
        json.dump(data, stream)
    print('CAMP_STORAGE_CONFIG=CREATED; 憑證值未輸出；未部署')


if __name__ == '__main__':
    main()
