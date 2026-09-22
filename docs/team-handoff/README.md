# 队友本地部署与账号交接

更新：2026-09-22。适用 Windows / PowerShell，克隆项目后的独立本地环境。

本目录提供独立交接说明、脱敏表结构及新建测试数据。没有导出原用户、简历、邮箱、登录凭证和面试回答。不要把根目录旧 `interview_agent.sql` 或 `admin-auth-backup.sql` 作为交接数据发给队友。

## 1. 代码、环境与账号区别

先克隆仓库，进入项目根目录：

```powershell
git clone https://github.com/mahou66666/monimianshi.git
Set-Location monimianshi
```

本次文件只在本地新增；分享前需将本目录和依赖的最新代码提交推送，或者把完整交接目录单独发给队友。仅克隆旧远程版本不一定包含新前台。

准备 Docker Desktop、Java 21、Maven、Node.js 22.12+。候选人后端自带 Gradle Wrapper；各服务首次启动需要下载依赖。管理员后端源码目标为 Java 17，具体兼容性以其构建结果为准。

| 用途 | 地址 / 数据库 | 本交接包初始化的账号 | 密码 |
|---|---|---|---|
| 候选人前台 | http://127.0.0.1:5174 / interview_studio_local | 19900000001 | StudioDemo2026! |
| 管理后台 | http://localhost:9527 / interview_agent | 138-0000-0303（保留连字符） | 123456 |
| MySQL 连接 | 127.0.0.1:13306 | zzh | 队友自己设置 |
| MySQL 初始化管理 | 同上 | root | 队友自己设置，应用运行不用 root |

上面两个网站密码是新建的公开本地测试夹具，不是复制原用户密码。前台账号没有后台权限。后台测试账号被授予本包定义的全部 9 项权限。后续新增权限不会自动加入本包。

## 2. 创建独立数据库与用户（推荐）

如果本机已有服务占用 13306 或 16379，先确认其用途，不要删除旧容器或数据卷。本方案使用独立 Compose 项目和数据卷，但仍需要空闲端口。

在项目根目录的 PowerShell 输入自己的三个密码（输入不会回显）：

```powershell
function Read-LocalSecret([string]$Prompt) {
    $secret = Read-Host $Prompt -AsSecureString
    try { [System.Net.NetworkCredential]::new('', $secret).Password }
    finally { $secret.Dispose() }
}
$env:MYSQL_ROOT_PASSWORD = Read-LocalSecret '新本地 MySQL root 密码'
$env:MYSQL_PASSWORD = Read-LocalSecret '新本地 MySQL zzh 密码'
$env:TEAM_REDIS_PASSWORD = Read-LocalSecret '新本地 Redis 密码'
docker compose -f .\docs\team-handoff\compose.yml up -d
docker compose -f .\docs\team-handoff\compose.yml logs --tail 60 mysql
```

首次初始化自动完成：创建 MySQL 用户 `zzh` → 建立两个库及表 → 插入测试账号与权限 → 授予 `zzh` 两个业务库权限。等日志显示数据库可接收连接后继续。

文件顺序：

1. `01-admin-schema-seed.sql`：后台表结构、一个测试管理员、9 项权限及关联。
2. `02-studio-schema-seed.sql`：前台身份、资料、简历、会话、逐轮记录及账户安全表，一个候选人测试账号。
3. `03-grant.sql`：授予 SELECT / INSERT / UPDATE / DELETE / CREATE / ALTER / INDEX；不授予全局管理权限。

这些初始化 SQL 仅供全新环境。已有同名数据库时会报错，不使用强制忽略错误模式。不要对当前工作数据库重复执行。Docker 初始化文件只在数据目录为空时执行，修改环境变量不会重置已有用户密码；不要为了重跑而执行 `down -v`。

### 已有自己的 MySQL 时

不用本目录 Compose 的 mysql 服务。以自己的数据库管理员身份连接本机 13306，确认两个业务库不存在，然后在 MySQL 客户端执行（不是 PowerShell 命令）：

```sql
CREATE USER 'zzh'@'%' IDENTIFIED BY '替换成你自行设置的本地密码';
SOURCE C:/你的项目目录/docs/team-handoff/01-admin-schema-seed.sql;
SOURCE C:/你的项目目录/docs/team-handoff/02-studio-schema-seed.sql;
SOURCE C:/你的项目目录/docs/team-handoff/03-grant.sql;
SHOW GRANTS FOR 'zzh'@'%';
```

若 `zzh` 已存在，不重复 CREATE USER、不擅自覆盖密码；使用已知凭据并由管理员检查授权即可。这里 `%` 用于本地容器转发访问，数据库端口只应绑定回环地址。已有 Redis 则沿用自己的连接配置。

## 3. 准备配置（不覆盖现有文件）

在项目根目录执行：

```powershell
$configPaths = @(
  'springboot-front(4)/src/main/resources/application.properties',
  'backend/admin-server/src/main/resources/application.properties'
)
foreach ($configPath in $configPaths) {
    if (-not (Test-Path -LiteralPath $configPath)) {
        Copy-Item -LiteralPath "$configPath.example" -Destination $configPath
    }
}
```

已存在的配置需检查，不把原开发者远程数据库或密钥带入队友环境。下面显式设置数据源，避免意外使用默认数据库。

## 4. 启动候选人后端与前台

在项目根目录的新 PowerShell 窗口执行：

```powershell
$env:STUDIO_DB_USER = 'zzh'
# start-studio-real.ps1 会交互询问 STUDIO_DB_PASSWORD。
# 以下两个可选 AI 配置先置空，仅验收账户功能。
$env:RESUME_PROBLEM_AGENT_API_KEY = ''
$env:RESUME_SILICONFLOW_API_KEY = ''
.\start-studio-real.ps1 -Database mysql
```

服务应监听 8084，profile 为 `studio-mysql`。MySQL 模式不要使用 `-InitializeTestAccounts`。出现“8084 already in use”说明已有进程占用，不是密码错误；检查已有服务，避免重复启动。

另开窗口，在项目根目录启动候选人前台：

```powershell
Set-Location 'vue-front (4)'
npm ci
$env:VITE_STUDIO_API_TARGET = 'http://127.0.0.1:8084'
npm run dev -- --host 127.0.0.1 --port 5174 --strictPort
```

也可将 `VITE_STUDIO_API_TARGET=http://127.0.0.1:8084` 写入该前端目录的本地 `.env.local`（先检查已有内容）。默认代理为 8083，必须显式改到 MySQL 服务的 8084。更改后重启 Vite。

## 5. 启动管理后台

另开窗口，在项目根目录执行。此窗口中的 Read-LocalSecret 函数须重新定义，或使用上文相同函数：

```powershell
function Read-LocalSecret([string]$Prompt) {
    $secret = Read-Host $Prompt -AsSecureString
    try { [System.Net.NetworkCredential]::new('', $secret).Password }
    finally { $secret.Dispose() }
}
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:13306/interview_agent?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
$env:SPRING_DATASOURCE_USERNAME = 'zzh'
$env:SPRING_DATASOURCE_PASSWORD = Read-LocalSecret '前面设置的 zzh 密码'
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'com.mysql.cj.jdbc.Driver'
$env:SPRING_DATA_REDIS_HOST = '127.0.0.1'
$env:SPRING_DATA_REDIS_PORT = '16379'
$env:SPRING_DATA_REDIS_PASSWORD = Read-LocalSecret '前面设置的 Redis 密码'
$env:APP_JWT_SECRET = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
Set-Location backend/admin-server
mvn spring-boot:run
```

临时 JWT 密钥重启后变化会使旧登录失效；需要持久登录时，将自行生成的密钥保存在自己的安全本地配置，不提交 Git。

再开窗口启动后台网页：

```powershell
Set-Location backend/admin-web
npm ci
$env:VUE_APP_PROXY_TARGET = 'http://127.0.0.1:18081'
npm run dev -- --host 127.0.0.1 --port 9527
```

上述启动顺序用于登录及基础数据验收，不依赖 root 密码运行应用。环境变量只属于当前窗口及其子进程，换窗口必须重新配置。

## 6. 完整面试所需的额外服务

登录通过不代表所有 AI 服务可用。完整流程还需：

| 服务 | 本地地址 | 配置与启动参考 |
|---|---|---|
| 简历解析 | 8089 | `resume_cut_docker`，准备其 `.env` 和模型依赖，按服务说明启动 |
| WB 面试与 PostgreSQL | 8010 | `WB_interview_agent/README.md`；目录内配置自己的模型 Key，执行 `docker compose up -d --build postgres app` |
| ASR 网关 | 8082 | `FunASR-main/sv-service/README.md` |
| FunASR | 10095 | 配置网关可连接的 FunASR 服务与所需模型 |
| 简历改写模型 | 外部 API | 为候选人后端设置 `RESUME_SILICONFLOW_API_KEY`，使用可访问模型后重启后端 |

WB 使用自己的 PostgreSQL 状态库，不是上述两个 MySQL 库。模型文件、API Key、上传文件、向量索引不会随代码克隆获得。不要直接使用其他机器的 `.env`。旧一键脚本可能启动 8080/5173，本交接默认 8084/5174，不混用。

## 7. 队友登录与权限验收

### 前台

1. 打开 `http://127.0.0.1:5174/login`，使用本说明的候选人测试账号和密码。
2. 确认进入首页；个人资料显示“交接测试同学”。
3. 修改昵称并保存，刷新、退出重新登录，检查修改保留。
4. 历史、成长没有训练记录属于正常初始状态，没有复制或伪造原来的面试成绩。
5. 也可通过注册页创建自己的账号。当前本地 profile 的手机、邮箱验证码为演示机制 `123456`，按页面先获取验证码，不发送短信或邮件，不代表所有权验证。新注册密码至少 8 位。

### 后台

1. 打开 `http://localhost:9527`，使用 `138-0000-0303` / `123456`。
2. 确认能够登录，用户与权限等菜单可见；打开权限配置核对 9 项授权。
3. 权限代码为：`job:add`、`company:view`、`resume:manage`、`job:view`、`job:edit`、`company:manage`、`resume:view`、`resume:evaluate`、`system:log`。
4. 不修改测试管理员的权限来验证，避免把自己锁出系统。

需要数据库核对时，以管理员连接相应数据库后执行只读查询：

```sql
SELECT phone,user_name,status,is_deleted FROM interview_agent.user_info;
SELECT p.perm_code,p.status FROM interview_agent.user_permission_rel r
JOIN interview_agent.permission p ON p.id=r.perm_id WHERE r.user_id=1001;
SELECT phone,user_name,status FROM interview_studio_local.user_info;
```

### 完整流程追加验收

上传脱敏 PDF → 解析与确认 → 改写保存新版本 → 开始面试 → 文字回答与追问 → 语音转写 → 完成报告 → 专项再练 → 刷新历史与成长。服务未就绪的项目单独记录，不把登录成功当作全流程通过。

## 8. 常见错误

| 错误 | 排查 |
|---|---|
| MySQL Access denied | 检查 `zzh` 的数据库密码、用户 Host 和授权，不使用网站密码 |
| 后台提示未注册 | 检查是否导入本包、是否连接本机 `interview_agent`、账号是否保留连字符 |
| 前台提示未注册 | 检查 8084 是否连接 `interview_studio_local`，前后台账号独立 |
| 前台请求失败 | 检查 Vite 代理是否仍是 8083，以及后端启动是否成功 |
| 后台登录时 Redis 异常 | 检查 16379、密码及后台窗口环境变量 |
| 有数据库但没有表 | MySQL profile 不自动建表，初始化是否执行成功；旧数据卷不重放 SQL |
| 修改 Compose 密码无效 | 已有数据卷的密码不会由环境变量覆盖，需要原管理员执行正式修改 |

## 9. 本次交付的验证范围

本包从已有 SQL 中仅提取建表语句，测试身份与权限重新构造，密码为新生成的 BCrypt 哈希。未连接或修改现有数据库。本次完成文件结构、敏感数据隔离与哈希校验；队友新机器的 Docker 初始化、构建和双端登录仍需按第 7 节实际验收。
