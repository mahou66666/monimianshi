# monimianshi — AI 模拟面试平台

重构前源码基线。候选人端位于 `vue-front (4)/`，仓库包含各服务源码。

本地密钥、数据库导出、模型资源、依赖及构建产物不纳入版本管理。首次克隆时请阅读 [本地配置说明](docs/LOCAL_CONFIGURATION.md)，复制配置示例并填写自己的环境参数。

多服务架构的全栈 AI 面试平台，为求职者提供简历解析、AI 评分、JD 分析、模拟面试、语音识别等功能。

## 项目结构

```
├── vue-front (4)/           # 候选人端前端 — Vue 3 + Vite + Pinia（端口 5173）
├── springboot-front(4)/     # 候选人端后端 API — Spring Boot 4 + MyBatis（端口 8080）
├── backend/
│   ├── admin-web/           # 管理后台前端 — Vue 2 + Element UI（端口 9527）
│   └── admin-server/        # 管理后台后端 — Spring Boot 3 + MyBatis-Plus（端口 18081）
├── resume_cut_docker/       # 简历解析服务 — Spring Boot + DJL/PyTorch（端口 8089）
├── WB_interview_agent/      # 多智能体面试系统 — FastAPI + LangGraph（端口 8010）
├── Interview_elf_agent/     # 简历分析 + RAG 知识库 — FastAPI + Qdrant（端口 8000）
├── FunASR-main/sv-service/  # 语音识别网关 — Spring Boot + ONNX（端口 8082）
├── resume-revision/         # 简历修改 CLI 工具 — Java + LangChain4j
├── Redis/                   # 本地 Redis（Windows）
└── docs/                    # 架构图、PPT 素材
```

## 快速开始

### 环境要求

- Java 17+ / Java 21（springboot-front）
- Node.js 20.19+ / 22.12+
- Python 3.11+
- Docker Desktop
- MySQL 5.7+
- Maven / Gradle

### 一键启动全部服务

```powershell
.\start-all-4-services.ps1
```

启动后访问：
- 候选人端：http://localhost:5173
- 管理后台：http://localhost:9527

### Docker Compose 启动（Admin 后台）

```bat
start-dev.bat
```

### 停止服务

```powershell
.\stop-all-4-services.ps1
```

## 数据库

MySQL 数据库 `interview_agent`，核心表包括用户、权限、简历、JD 四大模块。完整表结构见 `DATABASE_TABLES.md`。

数据库初始化脚本：`interview_agent.sql`

## LLM 配置

所有 AI 服务统一通过 SiliconFlow API 调用大模型，主要使用：
- DeepSeek-V3.2（面试、分析）
- Qwen 系列（简历解析、修改）

API Key 配置在各服务的 `.env` 文件中。

## 相关文档

- `CLAUDE.md` — Claude Code 开发指南
- `DATABASE_TABLES.md` — 数据库表结构说明
- `docs/` — 架构图与 PPT 素材
- `docs.zip` — 文档打包
