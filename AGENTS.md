# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project overview

AI 模拟面试平台（AI Mock Interview Platform），是一个多服务架构的全栈项目，面向求职者提供简历解析、AI 评分、JD 分析、模拟面试、语音识别等功能，同时提供后台管理系统用于用户/简历/JD/权限管理。

## 启动命令

### 方式一：一键启动（推荐，PowerShell）

```powershell
# 启动全部 4 个核心服务（WB面试智能体、简历解析、后端 API、前端、FunASR/SV）
.\start-all-4-services.ps1

# 跳过某些服务
.\start-all-4-services.ps1 -SkipBackend8080 -SkipFrontend5173

# 停止所有服务
.\stop-all-4-services.ps1
```

### 方式二：Docker Compose 启动（Admin 后台 + Redis + 简历解析）

```bat
start-dev.bat              # docker compose up（不含本地 MySQL）
start-dev-local-db.bat     # docker compose up + 本地 MySQL 容器
```

### 方式三：单独启动

```bat
start-redis.bat            # 单独启动本地 Redis（端口 16379）
```

### 各子项目独立开发

```sh
# 前端（候选人端）—— vue-front (4)/
cd "vue-front (4)" && npm install && npm run dev      # 端口 5173

# 后端 API —— springboot-front(4)/
cd "springboot-front(4)" && .\gradlew.bat bootRun     # 端口 8080

# Admin 前端 —— backend/admin-web/
cd backend/admin-web && npm install && npm run dev     # 端口 9527

# Admin 后端 —— backend/admin-server/
cd backend/admin-server && mvn spring-boot:run        # 端口 18081

# 简历解析服务 —— resume_cut_docker/
cd resume_cut_docker && mvn spring-boot:run            # 端口 8089

# 面试智能体 —— WB_interview_agent/
cd WB_interview_agent && docker compose up -d postgres app  # 端口 8010

# 简历分析智能体 —— Interview_elf_agent/
cd Interview_elf_agent && uvicorn app.api.server:app --port 8000

# 语音识别网关 —— FunASR-main/sv-service/
cd FunASR-main/sv-service && java -jar target/sv-service-0.1.0.jar  # 端口 8082
```

## 架构概览

项目包含 **8 个服务**，所有 LLM 调用统一走 **SiliconFlow API**：

```
vue-front(4) :5173  ───proxy───>  springboot-front(4) :8080 ──> MySQL (interview_agent)
       │                                    ├──> resume_cut_docker :8089
       ├──> WB_interview_agent :8010        ├──> Interview_elf_agent :8000
       └──> sv-service :8082               └──> SiliconFlow API

admin-web :9527  ───proxy───>  admin-server :18081 ──> MySQL + Redis + resume_cut_docker
```

### 服务端口与技术栈一览

| 服务 | 端口 | 语言 | 框架 | 数据库 |
|------|------|------|------|--------|
| `vue-front (4)/` | 5173 | JS | Vue 3 + Vite 7 + Pinia + Vue Router 4 | — |
| `springboot-front(4)/` | 8080 | Java 21 | Spring Boot 4.0.4 + MyBatis | MySQL |
| `backend/admin-web/` | 9527 | JS | Vue 2 + Element UI + Vuex | — |
| `backend/admin-server/` | 18081 | Java 17 | Spring Boot 3.2.5 + MyBatis-Plus | MySQL + Redis |
| `resume_cut_docker/` | 8089 | Java 17 | Spring Boot 3.2.1 + DJL/PyTorch + PDFBox/POI | — |
| `WB_interview_agent/` | 8010 | Python | FastAPI + LangChain + LangGraph | PostgreSQL |
| `Interview_elf_agent/` | 8000 | Python | FastAPI + LangChain + LangGraph | Qdrant（向量库） |
| `FunASR-main/sv-service/` | 8082 | Java 17 | Spring Boot 3.2.2 + ONNX Runtime | — |
| FunASR WebSocket | 10095 | Docker | FunASR Runtime SDK | — |

### 服务职责

- **vue-front (4)/** — 候选人端移动端前端，模拟手机屏幕（390x844），负责简历上传、JD 查看、面试交互、语音录入
- **springboot-front(4)/** — 候选人端后端 API，负责用户/简历/JD/公司 CRUD，调用简历解析和 AI 分析服务
- **backend/admin-web/** — 管理后台前端（基于 vue-element-admin 模板），用户管理/权限分配/简历管理/JD管理/题库管理
- **backend/admin-server/** — 管理后台后端，JWT 认证 + RBAC 权限控制（`@RequirePermission` 注解 + `AuthInterceptor` 拦截器）
- **resume_cut_docker/** — AI 简历解析服务，将 PDF/DOCX 简历转为结构化数据
- **WB_interview_agent/** — 多智能体面试系统（Orchestrator + DomainExpert + HRAssessor + Reporter），LangGraph 状态图驱动，PostgreSQL 持久化会话
- **Interview_elf_agent/** — 简历智能分析（多维度评分、面试题生成、JD 分析）+ RAG 知识库管理（Qdrant 向量检索）
- **FunASR-main/sv-service/** — 语音识别网关，封装 FunASR WebSocket 连接，提供说话人注册/验证/ASR 转写 REST API

### 前端代理配置

`vue-front (4)/vite.config.js` 中的代理规则：

| 路径前缀 | 代理目标 | 说明 |
|----------|----------|------|
| `/api`, `/resume`, `/resumeFragment`, `/resumeScore` | `127.0.0.1:8080` | 后端 API |
| `/interview/start`, `/interview/answer`, `/interview/state` | `127.0.0.1:8010` | 面试智能体 |
| `/interview/asr/transcribe` | `127.0.0.1:8082` | 语音识别 |

## 数据库

数据库为 MySQL `interview_agent`，核心表 8 张：

- 用户：`user_info`
- 权限：`permission` + `user_permission_rel`
- 简历：`resume` + `resume_file` + `resume_content`
- JD：`jd_job` + `jd_guide`

完整表结构文档见项目根目录 `DATABASE_TABLES.md`。

生产数据库连接：`106.54.162.6:3306/interview_agent`（配置在 `springboot-front(4)/src/main/resources/application.properties`）。

## 关键技术决策

- **所有 LLM 调用统一走 SiliconFlow API**，主要使用 DeepSeek-V3.2 模型，简历修改等场景使用 Qwen 系列
- **前端路径别名**：`@` → `src/`（vite.config.js + jsconfig.json）
- **Admin RBAC**：基于 `@RequirePermission` 注解 + `AuthInterceptor` 拦截器实现方法级权限控制
- **面试会话持久化**：WB_interview_agent 使用 LangGraph Checkpoint + PostgreSQL 实现暂停/恢复
- **简历解析双路径**：先本地 DJL/PyTorch 模型提取，再调 LLM 增强
- **RAG 向量检索**：使用 Qdrant + BAAI/bge-large-zh-v1.5 嵌入模型，支持按专业/主题/子主题过滤
