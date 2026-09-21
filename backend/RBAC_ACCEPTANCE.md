# Admin RBAC Acceptance Matrix (2026-03-23)

## Scope
- Backend: `backend/admin-server`
- Frontend: `backend/admin-web`
- Core routes: `/interviewelf/userManagement`, `/interviewelf/importResume`

## Test accounts
- Admin: `13000000001` (has `company:manage`, `resume:manage`, `resume:view`)
- Ops: `13000000002` (has `resume:view`, no `company:manage`, no `resume:manage`)
- Candidate: `13000000003` (no interview-admin permissions)
- Password: `123456`

## Expected behavior
| Area | Action | Admin | Ops | Candidate |
|---|---|---|---|---|
| Login | Access admin login successfully | Yes | Yes | No |
| Menu | User Management page visible | Yes | No | No |
| Menu | Import Resume page visible | Yes | Yes | No |
| User Mgmt | Create/Edit/Delete/Enable/Disable user | Yes | No | No |
| User Mgmt | Assign permissions | Yes | No | No |
| Resume Mgmt | View list/Search/Download | Yes | Yes | No |
| Resume Mgmt | Upload PDF / Batch extract | Yes | No | No |
| Resume Mgmt | Edit/Delete resume | Yes | No | No |
| API | Cross-user access (`/users/{otherId}/...`) | Allowed for `company:manage` | Forbidden | Forbidden |

## Backend smoke script
- Script: `backend/admin-server/scripts/rbac_smoke_test.ps1`
- Run:

```powershell
cd backend/admin-server
powershell -ExecutionPolicy Bypass -File .\scripts\rbac_smoke_test.ps1
```

- Expected final line: `ALL PASSED`

## Notes
- Login gate: user must be active and own at least one active backend permission.
- Route-level control: `meta.perms` in `admin-web`.
- Button-level control: `company:manage` for user management writes, `resume:manage` for resume writes.
- Backend hard-check:
  - `@RequirePermission` for endpoint permission.
  - `AuthInterceptor` path user-scope check: non-`company:manage` can only access own `{userId}`.
