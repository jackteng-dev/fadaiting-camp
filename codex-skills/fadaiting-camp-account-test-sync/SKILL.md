---
name: fadaiting-camp-account-test-sync
description: "Keep D:\\Project\\fadaiting-camp local plaintext test accounts synchronized for the 發呆亭露營區 project. Use whenever Codex creates, seeds, changes, or mentions a new backend/admin/test/login account, especially when a password is known or generated. The skill updates the local plaintext account Excel and Tampermonkey userscript so the user can test admin login by typing only the username."
---

# 發呆亭測試帳號同步

## Purpose

Keep local-only test account helpers in sync for the local 發呆亭露營區 repo, usually `D:\Project\fadaiting-camp`:

- `db/test-accounts-plaintext.xlsx`
- `db/test-accounts-plaintext.json`
- `frontend/public/local-userscripts/admin-login-autofill.user.js`

These files contain plaintext passwords for local testing. Ensure they are ignored by Git.

## Workflow

When adding or changing any test/admin/login account with a known password:

1. Make or confirm the actual app/database account change first.
2. Run `scripts/sync_fadaiting_camp_test_accounts.mjs` with the account username and plaintext password.
3. Confirm `.gitignore` ignores:
   - `db/test-accounts-plaintext.xlsx`
   - `db/test-accounts-plaintext.json`
   - `frontend/public/local-userscripts/`
4. Tell the user the Excel and Tampermonkey helper were updated.

## Script

Use the bundled workspace Node runtime and `@oai/artifact-tool`. On a fresh Codex environment, first locate the bundled runtime with `load_workspace_dependencies`, then ensure the project has `.codex-runtime/node_modules` pointing at the bundled `node_modules`.

Example:

```powershell
# Replace these two paths with the values returned by Codex workspace dependencies if they differ.
$node = "$env:USERPROFILE\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe"
$deps = "$env:USERPROFILE\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules"
if (-not (Test-Path "D:\Project\fadaiting-camp\.codex-runtime\node_modules")) {
  New-Item -ItemType Junction -Path "D:\Project\fadaiting-camp\.codex-runtime\node_modules" -Target $deps | Out-Null
}
& $node "$env:CODEX_HOME\skills\fadaiting-camp-account-test-sync\scripts\sync_fadaiting_camp_test_accounts.mjs" `
  --project "D:\Project\fadaiting-camp" `
  --username "admin001" `
  --password "<seed 預設密碼：見 db/init，首次登入後立即更改>" `
  --role "admin" `
  --purpose "預設後台管理員"
```

Optional flags:

- `--login-url`: defaults to `http://localhost:5173/admin/login`
- `--userscript-base-url`: defaults to the origin from `--login-url`, used for `@downloadURL` / `@updateURL`
- `--status`: defaults to `啟用`
- `--note`: defaults to `本機開發測試帳號`

The script upserts by `username`, so re-running it is safe.
