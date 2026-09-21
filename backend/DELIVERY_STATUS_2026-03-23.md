# Admin Delivery Status (2026-03-23)

## Summary
- Core scope for `admin-server` + `admin-web` is now functional.
- User management, resume management, and RBAC controls are connected end-to-end.
- Backend and frontend both enforce permissions.

## Completed
1. Login and session
- Login writes token to Redis (single-session semantics).
- Frontend user cache stores `userId`, `userName`, `permissionCodes`.
- Login redirect fallback fixed to avoid dead `/404` for low-permission accounts.

2. User management
- APIs integrated: list/create/update/delete/enable/disable.
- Permission assignment APIs and page dialog integrated.
- Button-level permission control enabled.
- Write actions restricted to `company:manage`.

3. Resume management
- APIs integrated: upload/list/search/download.
- Update/delete resume implemented and linked.
- Delete now removes local PDF file + DB records.
- Storage model matches requirement: local file storage, DB stores path.
- Button-level permission control enabled.
- Write actions restricted to `resume:manage`; read/download kept for `resume:view`.

4. Backend RBAC hardening
- `@RequirePermission` applied on key user/resume management APIs.
- Path-scope guard added in interceptor:
  - Non-`company:manage` users cannot access other users via `/{userId}`.
- MD5 precheck endpoint now requires `resume:manage`.

5. Acceptance assets
- Matrix doc: `backend/RBAC_ACCEPTANCE.md`
- Smoke script: `backend/admin-server/scripts/rbac_smoke_test.ps1`
- Smoke script executed locally: `ALL PASSED`.

## Quick validation
```powershell
cd backend/admin-server
powershell -ExecutionPolicy Bypass -File .\scripts\rbac_smoke_test.ps1
```

## Remaining (next iteration)
1. Batch resume parsing is still placeholder-level; connect real parsing service.
2. Add admin audit trail/report page for operation history.
3. If required by course review, add screenshot-based API evidence for happy/error paths.

## Risks and dependencies
1. RBAC behavior depends on DB seed data in `permission` and `user_permission_rel`.
2. Local storage depends on `app.storage.resume-dir` write permissions on target machine.
3. Large PDF uploads still constrained by backend upload limits.

## One-line update for team lead
- Admin backend/frontend core workflow is complete: user CRUD + permission assignment, resume local storage with DB path, resume update/delete with local file cleanup, and route/button/API 3-layer RBAC with reproducible smoke tests.
