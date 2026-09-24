# Admin RBAC Acceptance Matrix (2026-03-23)

## Scope
- Backend: `backend/admin-server`
- Frontend: `backend/admin-web`
- Core routes: `/interviewelf/userManagement`, `/interviewelf/importResume`

## 当前本地管理员记录（2026-09-22 已核实）

- 后台入口：`http://localhost:9527`；后台 API：`http://127.0.0.1:18081/admin`。
- 数据库：本地 `127.0.0.1:13306/interview_agent`，不代表其他环境的账号配置。
- 用户 ID：`1001`。
- 登录账号：`138-0000-0303`（保留连字符）。
- 本地开发登录密码：`123456`。
- 验证结果：登录接口返回成功；当前数据库的 9 项权限全部已授权且启用。
- 权限：`job:add`、`company:view`、`resume:manage`、`job:view`、`job:edit`、`company:manage`、`resume:view`、`resume:evaluate`、`system:log`。
- 登录页面已支持该手机号格式；本次仅更新记录，没有更改数据库账号、密码或权限。
- 旧文档管理员 `13000000001` 在当前本地数据库中不存在，不能用于当前环境登录。

## 历史测试账号（原验收夹具，不代表当前本地数据）
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
- 该脚本仍使用历史夹具中的账号，不能直接作为当前本地管理员账号的验证依据。
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
