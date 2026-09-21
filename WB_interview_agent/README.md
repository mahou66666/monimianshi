# WB Interview Agent

一个基于 `LangGraph + 多智能体` 的面试系统。

系统支持技术面与 HR 面协同提问、长期会话记忆、行业化问题定制、语义去重、每轮结构化评分，以及最终基于累计证据的评估报告归档。

当前推荐接入方式是 `HTTP API`：

- 业务后端把候选人信息、JD、简历、当前回答通过 JSON 请求发送给 Agent
- Agent 返回下一问、本轮评分、最终报告等结构化结果给业务后端
- `.env` 仅保留模型与基础设施配置，不再承载业务侧候选人数据

项目边界说明：

- 本仓库仅包含 `Agent 算法引擎 + 对外 HTTP 接口`
- 不包含业务后端系统代码（如用户体系、权限、订单、支付、管理后台等）

## 1. 核心能力

- 多智能体面试流程
  - `Orchestrator`：控场与路由
  - `DomainExpert`：技术追问
  - `HRAssessor`：行为面追问
  - `Reporter`：生成最终评估与归档
- 后端接口驱动
  - `POST /api/interviews/start`：创建/恢复会话并返回开场白与首问
  - `POST /api/interviews/answer`：提交候选人回答并返回评分与下一问/终评
  - `POST /api/interviews/state`：获取当前会话状态
- 长期记忆与断点续面
  - 使用 PostgreSQL Checkpointer 保存会话状态
- 行业化提问策略
  - 金融、医疗、制造、零售电商、通用互联网兜底
- JD/简历定向追问
  - 自动提取 JD 技能、简历已体现技能与潜在差距
  - 按“本轮聚焦主题”驱动技术面与 HR 面追问
- 语义去重
  - 使用 embedding 计算问题相似度，减少重复提问
- 结构化评分器（逐轮）
  - 每轮输出 `competency -> score -> evidence`
  - 最终报告基于 `round_scores` 累计证据聚合，而不是一次性总结
- 安全与稳定性
  - 默认自动生成独立会话 ID，避免固定 `thread_id` 串号
  - 恢复历史会话时校验候选人/JD/简历元数据，拒绝跨候选人误恢复
  - 默认禁用危险 FAISS 反序列化
  - LLM 异常兜底问题
  - 报告归档默认关闭，显式开启后默认脱敏且不保存原始对话

## 2. 系统架构

```text
User Input
   |
   v
Orchestrator --(eval_last_answer)--> Evaluator(Tech/HR) --+
   |                                                       |
   +--(ask_next_question)--> Questioner(Tech/HR) ----------+--> END(本轮)
   |
   +--(end)---------------> Reporter -----------------------> END(面试结束)
```

状态由 `core/state.py` 定义，核心字段包括：

- 基础信息：`candidate_name`、`target_industry`、`job_description`、`resume_highlights`
- 面试控制：`current_stage`、`question_count`、`max_questions`
- 分路计数：`tech_question_count`、`hr_question_count`
- 结构化评分：`round_scores`（累计）
- 对话历史：`messages`

## 3. 目录结构

```text
.
├── agents/
│   ├── orchestrator.py        # 控场路由
│   ├── domain_expert.py       # 技术面智能体 + RAG + 逐轮评分
│   ├── hr_assessor.py         # HR 智能体 + 逐轮评分
│   └── reporter.py            # 累计证据报告与归档
├── api.py                     # FastAPI HTTP 接口入口
├── core/
│   ├── config.py              # 模型/Provider配置
│   ├── graph.py               # LangGraph构建与PostgreSQL checkpoint
│   ├── interview_service.py   # 后端接口服务层
│   ├── rag_config.py          # 会话级RAG路由配置解析
│   ├── scoring.py             # 结构化评分与最终聚合
│   ├── semantic_dedup.py      # embedding语义去重
│   └── state.py               # 共享状态定义
├── knowledge_corpus/          # 行业语料模板（可扩展）
│   ├── computer_backend/
│   └── finance_risk/
├── prompts/
│   ├── system_prompts.py
│   └── industries/            # 行业提示词路由
├── scripts/
│   └── build_faiss_index.py   # 本地语料构建FAISS索引脚本
├── memory/
│   └── knowledge_index/       # FAISS索引（可选）
├── archives/                  # 报告归档
├── tests/                     # 最小回归测试集
├── main.py                    # CLI入口
└── requirements.txt
```

## 4. 运行流程说明

1. 启动后读取 `thread_id` 对应历史状态（PostgreSQL）
2. 若未提供 `CANDIDATE_ID`，系统会自动生成独立会话 ID
3. 新会话先由 `Orchestrator` 打招呼并进入技术面
4. 候选人每次作答后：
   - 由上一位出题的面试官先对该轮回答做结构化评分
   - 再由 `Orchestrator` 决定下一位提问人，并按 JD/简历聚焦主题生成下一问
5. 最后一轮回答会先完成评分，再进入 `Reporter`
6. `Reporter` 聚合 `round_scores`，输出结构化总评；归档默认关闭，开启后仅写脱敏文件

## 5. 环境要求

- Python 3.9+（建议 3.9~3.12）
- 可访问的 LLM API（支持 SiliconFlow 或 OpenAI 兼容接口）

## 6. 安装

```bash
python3 -m pip install -r requirements.txt
```

## 7. 配置（.env）

至少配置一组 API Key：

- `SILICONFLOW_API_KEY`（优先）
- 或 `OPENAI_API_KEY`
- `POSTGRES_CHECKPOINT_URL`（本地直连运行时必填；Docker Compose 启动会自动注入）

示例：

```dotenv
# Provider
SILICONFLOW_API_KEY=your_key
# OPENAI_API_KEY=your_key

# 可选：模型/网关配置
SILICONFLOW_BASE_URL=https://api.siliconflow.cn/v1
OPENAI_BASE_URL=
REASONING_MODEL=deepseek-chat
CHAT_MODEL=Qwen/Qwen2.5-7B-Instruct
EMBEDDING_MODEL=BAAI/bge-m3

# 必填（仅本地直连运行时）：PostgreSQL 会话记忆
# 示例：
# postgresql://username:password@127.0.0.1:5432/interview_agent?sslmode=disable
POSTGRES_CHECKPOINT_URL=postgresql://username:password@127.0.0.1:5432/interview_agent?sslmode=disable

# 可选：会话初始化参数
# 建议显式提供唯一 CANDIDATE_ID 用于断点续面；留空时会自动生成 session_xxx
CANDIDATE_ID=
CANDIDATE_NAME=张三
TARGET_INDUSTRY=Java后端开发
JOB_DESCRIPTION=负责高并发微服务架构设计，精通 Spring Boot、MySQL、Redis。
RESUME_HIGHLIGHTS=拥有5年开发经验，主导过日活百万级电商系统重构。
MAX_QUESTIONS=3

# 可选：语义去重
QUESTION_DEDUP_THRESHOLD=0.90
QUESTION_DEDUP_WINDOW=6

# 可选：知识库检索（RAG）
# 总开关（默认 true）
RAG_ENABLED=true
# 本地向量索引目录（默认 memory/knowledge_index）
# 支持相对路径（相对项目根目录）和绝对路径
RAG_INDEX_DIR=memory/knowledge_index
# 召回条数（默认 2）
RAG_TOP_K=2
# 注入提示词的知识文本最大长度（默认 1200）
RAG_MAX_CONTEXT_CHARS=1200
# 检索 query 模板，可用变量：{industry}、{answer}
RAG_QUERY_TEMPLATE=针对行业：{industry}。候选人提及：{answer}

# 可选：本地FAISS危险反序列化（默认 false，谨慎开启）
ALLOW_DANGEROUS_FAISS_DESERIALIZATION=false

# 可选：报告归档（默认关闭，开启后默认脱敏且不含 raw_chat）
ARCHIVE_REPORTS_ENABLED=false
ARCHIVE_REDACT_SENSITIVE=true
ARCHIVE_INCLUDE_RAW_CHAT=false
```

## 8. 启动方式

推荐：Docker 一键启动（API + PostgreSQL）：

```bash
docker compose up -d --build
```

查看服务状态：

```bash
docker compose ps
```

查看 API 日志：

```bash
docker compose logs -f app
```

停止并清理容器：

```bash
docker compose down
```

说明：

- Docker Compose 会自动将应用连接到同编排中的 `postgres` 服务。
- 容器内使用的 `POSTGRES_CHECKPOINT_URL` 会被自动注入为 `postgres:5432`，不会使用你本机 `.env` 里的 `127.0.0.1` 地址。
- 仍需在环境变量或 `.env` 中提供 `SILICONFLOW_API_KEY` 或 `OPENAI_API_KEY`。

本地直连（不使用 Docker）启动 API 服务：

```bash
uvicorn api:app --host 0.0.0.0 --port 8010
```

本地 CLI 调试模式：

```bash
python3 main.py
```

退出命令：`q` / `quit` / `exit`

## 9. 后端如何调用算法（HTTP API）

本项目的推荐接入链路是：`前端 -> 业务后端 -> 面试算法服务`。  
前端不要直接调用算法服务，由业务后端统一管理 `session_id`、重试、鉴权和落库。

### 9.1 调用时序（按这个顺序接入）

1. 业务后端创建一条面试记录（你自己的 DB），生成或确定一个业务侧 `session_id`。
2. 调用 `POST /api/interviews/start`，拿到首问（`reply`）和真实 `session_id`。
3. 每次候选人作答后，业务后端调用 `POST /api/interviews/answer`。
4. 如果返回 `interview_completed=false`，继续把 `reply` 作为下一题展示给前端。
5. 如果返回 `interview_completed=true`，读取 `final_report` 和 `score_summary`，面试结束。
6. 页面刷新或服务重启后，可调用 `POST /api/interviews/state` 恢复当前进度。

### 9.2 基础约定

- Base URL（本地默认）：`http://127.0.0.1:8010`
- Content-Type：`application/json`
- 当前版本 API 无内置鉴权，生产环境请放在内网或加网关鉴权（例如 API Gateway / Nginx 鉴权）。

### 9.3 接口 1：创建/恢复会话 `POST /api/interviews/start`

请求体字段：

- `session_id`：可选。为空时算法服务自动生成 `session_xxx`。
- `candidate_name`：必填。
- `target_industry`：必填。
- `job_description`：必填。
- `resume_highlights`：必填。
- `max_questions`：可选，默认 `3`，最小 `1`。
- `rag_enabled`：可选，会话级知识库开关（`true/false`）。
- `rag_index_dir`：可选，会话级知识库索引目录（相对项目根目录或绝对路径）。
- `rag_top_k`：可选，会话级召回条数（>=1）。
- `rag_max_context_chars`：可选，会话级注入上下文长度（>=200）。
- `rag_query_template`：可选，会话级检索模板（支持 `{industry}`、`{answer}`）。
- `allow_dangerous_faiss_deserialization`：可选，会话级危险反序列化开关。

示例：

```bash
curl -X POST http://127.0.0.1:8010/api/interviews/start \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "candidate_123",
    "candidate_name": "张三",
    "target_industry": "Java后端开发",
    "job_description": "负责高并发微服务架构设计，精通 Spring Boot、MySQL、Redis。",
    "resume_highlights": "拥有5年开发经验，主导过日活百万级电商系统重构。",
    "max_questions": 3
  }'
```

响应关键字段（`start/answer/state` 三个接口通用）：

- `session_id`：会话唯一标识，后续必须原样回传。
- `session_created`：`true` 表示新建会话，`false` 表示恢复已有会话。
- `status`：`in_progress` 或 `completed`。
- `reply_type`：`question` 或 `report`。
- `reply`：给前端展示的当前文本（下一问或最终报告文本）。
- `awaiting_answer`：当前是否等待候选人回答。
- `interview_completed`：面试是否结束。
- `round_feedback`：本轮结构化评分文本（首轮通常为空）。
- `latest_round_score`：本轮评分明细对象。
- `score_summary`：累计评分汇总。
- `final_report`：面试结束后的结构化总评。
- `event_log`：本轮事件流（节点输出），可用于前端分段展示。
- `rag_config`：当前会话生效中的知识库路由配置（后端可据此确认路由是否生效）。

典型响应示例：

```json
{
  "session_id": "candidate_123",
  "session_created": true,
  "status": "in_progress",
  "candidate_name": "张三",
  "target_industry": "Java后端开发",
  "question_count": 1,
  "max_questions": 3,
  "last_interviewer": "tech",
  "awaiting_answer": true,
  "interview_completed": false,
  "reply_type": "question",
  "reply": "请介绍一次你主导的性能优化实践。",
  "round_feedback": "",
  "latest_round_score": null,
  "score_summary": null,
  "final_report": null,
  "rag_config": {
    "rag_enabled": true,
    "rag_index_dir": "memory/knowledge_index/computer_backend",
    "rag_top_k": 2,
    "rag_max_context_chars": 1200,
    "rag_query_template": "针对行业：{industry}。候选人提及：{answer}",
    "allow_dangerous_faiss_deserialization": true,
    "rag_index_dir_resolved": "/abs/path/to/memory/knowledge_index/computer_backend"
  },
  "event_log": [
    {
      "node": "Orchestrator",
      "kind": "message",
      "role": "ai",
      "content": "你好，候选人，我们开始面试。"
    }
  ]
}
```

说明：

- 若 `session_id` 已存在，`/start` 不会重开新面试，而是返回当前会话状态。
- 若传入的候选人/JD/简历与历史会话不一致，会返回 `409`（防串号保护）。

### 9.4 接口 2：提交回答 `POST /api/interviews/answer`

请求体字段：

- `session_id`：必填。
- `answer`：必填，候选人本轮回答。
- `candidate_name/target_industry/job_description/resume_highlights`：可选。  
  建议后端在关键场景传入，用于服务端做会话归属校验（不一致会返回 `409`）。
- `rag_enabled/rag_index_dir/rag_top_k/rag_max_context_chars/rag_query_template/allow_dangerous_faiss_deserialization`：可选。  
  若本轮传入，将按会话级配置更新并立即生效，用于后端动态切库/关库。

示例：

```bash
curl -X POST http://127.0.0.1:8010/api/interviews/answer \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "candidate_123",
    "answer": "我通过压测和监控把 p95 从 500ms 降到了 180ms。"
  }'
```

处理规则：

- 只有在 `awaiting_answer=true` 时才允许提交回答，否则返回 `409`。
- 会话已结束时提交回答会返回 `409`。
- 成功后返回“本轮评分 + 下一问”或“本轮评分 + 终评报告”。

### 9.5 接口 3：获取状态 `POST /api/interviews/state`

用于断点续面、页面刷新恢复、异常补偿查询。

该接口也支持传入 `rag_*` 参数做“无回答切库”，便于后端路由服务先切配置再继续面试。

```bash
curl -X POST http://127.0.0.1:8010/api/interviews/state \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "candidate_123"
  }'
```

无回答切库示例（会话级生效）：

```bash
curl -X POST http://127.0.0.1:8010/api/interviews/state \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "candidate_123",
    "rag_enabled": true,
    "rag_index_dir": "memory/knowledge_index/finance_risk",
    "allow_dangerous_faiss_deserialization": true
  }'
```

### 9.6 后端集成示例（Python）

```python
import requests

BASE_URL = "http://127.0.0.1:8010"
TIMEOUT = 60


def start_interview(session_id: str, candidate_name: str, target_industry: str, job_description: str, resume_highlights: str):
    payload = {
        "session_id": session_id,
        "candidate_name": candidate_name,
        "target_industry": target_industry,
        "job_description": job_description,
        "resume_highlights": resume_highlights,
        "max_questions": 3,
        # 会话级知识库路由
        "rag_enabled": True,
        "rag_index_dir": "memory/knowledge_index/computer_backend",
        "allow_dangerous_faiss_deserialization": True,
    }
    resp = requests.post(f"{BASE_URL}/api/interviews/start", json=payload, timeout=TIMEOUT)
    resp.raise_for_status()
    return resp.json()


def submit_answer(session_id: str, answer: str):
    payload = {
        "session_id": session_id,
        "answer": answer,
        # 也可在某一轮动态切库
        "rag_index_dir": "memory/knowledge_index/finance_risk",
    }
    resp = requests.post(f"{BASE_URL}/api/interviews/answer", json=payload, timeout=TIMEOUT)
    resp.raise_for_status()
    return resp.json()


def get_state(session_id: str):
    resp = requests.post(f"{BASE_URL}/api/interviews/state", json={"session_id": session_id}, timeout=TIMEOUT)
    resp.raise_for_status()
    return resp.json()
```

后端主流程伪代码：

```text
start = /start(...)
save(start.session_id)
show(start.reply)

loop:
  ans = wait_candidate_answer()
  turn = /answer(session_id, ans)
  persist(turn)
  show(turn.reply)
  if turn.interview_completed:
    break
```

### 9.7 错误码与后端处理建议

- `400`：请求参数错误（如缺少 `session_id`、`answer` 为空）。
- `404`：会话不存在（通常是未先调 `/start`，或 `session_id` 写错）。
- `409`：业务冲突（会话资料冲突、会话已结束、当前不在等待回答状态）。
- `500`：算法流程执行异常（模型或依赖异常）。

重试建议：

- `/start`：可安全重试（建议固定 `session_id` 重试）。
- `/state`：可安全重试。
- `/answer`：不要盲重试。若超时，先调 `/state` 确认本轮是否已推进，再决定是否补发。

### 9.8 后端落库建议（避免会话丢失）

建议至少保存以下字段到业务库：

- `biz_interview_id`：你业务系统自己的面试主键。
- `algo_session_id`：算法服务返回的 `session_id`。
- `algo_status`：`in_progress/completed`。
- `question_count/max_questions`：用于前端进度展示。
- `last_reply`：最近一轮给前端展示的文本。
- `latest_round_score/score_summary/final_report`：评分与终评结果。
- `last_raw_response`：算法返回原始 JSON（便于审计与排障）。

## 10. 测试

运行最小回归测试集：

```bash
python3 -m unittest discover -q
```

当前测试覆盖：

- API 路由与服务层会话流转（`tests/test_api.py`, `tests/test_interview_service.py`）
- 路由策略（`tests/test_routing.py`）
- 异常兜底与语义去重（`tests/test_fallback_and_semantic_dedup.py`）
- 报告归档（`tests/test_reporter_archive.py`）
- 行业路由（`tests/test_industry_routing.py`）
- JD/简历定向画像（`tests/test_targeting.py`）

## 11. 构建行业知识库索引（FAISS）

项目提供脚本：`scripts/build_faiss_index.py`，用于把本地语料目录构建为 `index.faiss + index.pkl`。

示例 1：构建“计算机后端岗位”知识库索引

```bash
python3 scripts/build_faiss_index.py \
  --input-dir knowledge_corpus/computer_backend \
  --output-dir memory/knowledge_index/computer_backend \
  --industry computer_backend \
  --role backend \
  --chunk-size 800 \
  --chunk-overlap 120
```

示例 2：构建“金融风控岗位”知识库索引

```bash
python3 scripts/build_faiss_index.py \
  --input-dir knowledge_corpus/finance_risk \
  --output-dir memory/knowledge_index/finance_risk \
  --industry finance_risk \
  --role risk_analyst
```

示例 3：先预览解析结果（不调用 embedding）

```bash
python3 scripts/build_faiss_index.py \
  --input-dir knowledge_corpus/computer_backend \
  --output-dir memory/knowledge_index/computer_backend \
  --dry-run
```

脚本参数说明（核心）：

- `--input-dir`: 原始语料目录，支持递归读取 `md/txt/jsonl`
- `--output-dir`: 索引输出目录
- `--append`: 追加写入到已有索引
- `--chunk-size/--chunk-overlap`: 切分参数
- `--industry/--role/--topic`: 写入默认元数据标签
- `--prepend-tag-fields`: 把元数据前缀到 chunk 文本（默认 `industry,role,topic`）

构建完成后，在 `.env` 指向对应行业目录：

```dotenv
RAG_ENABLED=true
RAG_INDEX_DIR=memory/knowledge_index/computer_backend
ALLOW_DANGEROUS_FAISS_DESERIALIZATION=true
```

说明：切换 `RAG_INDEX_DIR` 后需要重启服务，检索器才会重新加载索引。

### 11.1 多行业切换配置

方案 A：单实例手动切换（最简单）

1. 修改 `.env`：

```dotenv
RAG_ENABLED=true
ALLOW_DANGEROUS_FAISS_DESERIALIZATION=true
RAG_INDEX_DIR=memory/knowledge_index/computer_backend
```

2. 重启 API 服务。  
3. 需要切到金融风控时，把 `RAG_INDEX_DIR` 改为 `memory/knowledge_index/finance_risk` 后再次重启。

方案 B：多实例并行（推荐生产）

1. 启动计算机岗位实例（8010）：

```bash
RAG_ENABLED=true \
ALLOW_DANGEROUS_FAISS_DESERIALIZATION=true \
RAG_INDEX_DIR=memory/knowledge_index/computer_backend \
uvicorn api:app --host 0.0.0.0 --port 8010
```

2. 启动金融风控实例（8011）：

```bash
RAG_ENABLED=true \
ALLOW_DANGEROUS_FAISS_DESERIALIZATION=true \
RAG_INDEX_DIR=memory/knowledge_index/finance_risk \
uvicorn api:app --host 0.0.0.0 --port 8011
```

3. 业务后端按行业路由：

- `computer_backend -> http://127.0.0.1:8010`
- `finance_risk -> http://127.0.0.1:8011`

方案 C：按版本切换（灰度/回滚）

1. 索引目录版本化，例如：

- `memory/knowledge_index/computer_backend_v1`
- `memory/knowledge_index/computer_backend_v2`
- `memory/knowledge_index/finance_risk_v1`

2. 只切 `RAG_INDEX_DIR` 到新目录即可灰度。  
3. 若效果异常，回切旧目录并重启即可快速回滚。

## 12. 关键设计说明

### 12.1 结构化评分器

- 每轮从最近一轮 `AI问题 + 候选人回答` 生成评分记录：
  - `competency`
  - `score`（0-10）
  - `evidence`
- 每轮评分追加到 `round_scores`
- 最终由 `Reporter` 聚合能力项均分、风险项、优势项与 Offer 建议

### 12.2 语义去重

- 从最近若干条 AI 提问提取历史问题
- 计算新问题与历史问题 embedding 余弦相似度
- 相似度超过阈值时判定重复，切换到兜底问题
- embedding 服务异常时自动降级为精确匹配模式

### 12.3 RAG 安全策略

- 通过 `RAG_ENABLED` 一键控制是否启用知识库检索
- 通过 `RAG_INDEX_DIR` 指定索引目录（需同时包含 `index.faiss` 和 `index.pkl`）
- 通过 `RAG_TOP_K` 控制召回条数，`RAG_MAX_CONTEXT_CHARS` 控制注入上下文长度
- 默认不加载本地 FAISS `index.pkl`（避免危险反序列化）
- 显式开启 `ALLOW_DANGEROUS_FAISS_DESERIALIZATION=true` 才启用本地 FAISS 反序列化

## 13. 输出与归档

若显式开启 `ARCHIVE_REPORTS_ENABLED=true`，归档文件位于 `archives/`，默认包含：

- 脱敏后的候选人别名、行业、轮次、时间
- `round_scores` 原始逐轮证据
- `score_summary` 聚合结果
- `report` 最终结构化报告（脱敏后）
- `raw_chat` 默认不写入，需显式开启 `ARCHIVE_INCLUDE_RAW_CHAT=true`

## 14. 常见问题

### Q1: 启动时报“缺少 API Key”

请在 `.env` 配置 `SILICONFLOW_API_KEY` 或 `OPENAI_API_KEY`。

### Q2: 启动时报“缺少 POSTGRES_CHECKPOINT_URL”

请在 `.env` 配置 PostgreSQL 连接串，例如：
`postgresql://username:password@127.0.0.1:5432/interview_agent?sslmode=disable`。

### Q3: 为什么出现“语义去重降级为精确匹配模式”

说明 embedding 调用失败。系统会自动降级，不影响主流程，只是去重效果会下降。

### Q4: 为什么没有使用本地知识库

默认关闭危险反序列化。仅在你确认本地 FAISS 文件可信时再手动开启。

### Q5: 候选人/JD/简历还需要写进 `.env` 吗

在 API 模式下不需要。业务后端应通过 `POST /api/interviews/start` 和 `POST /api/interviews/answer` 传入。

### Q6: 如何精细控制知识库检索

- 临时关闭知识库：`RAG_ENABLED=false`
- 调整召回条数：`RAG_TOP_K=1~5`（建议先从 2 开始）
- 限制注入长度：`RAG_MAX_CONTEXT_CHARS=800~2000`
- 自定义索引目录：`RAG_INDEX_DIR=/abs/path/to/knowledge_index`

### Q7: 为什么本机 `python3` 运行时出现段错误（Segmentation fault）

部分环境下（例如特定 Anaconda + Python 3.13 组合）会触发三方依赖兼容问题。  
建议：

- 优先使用系统 Python 或稳定虚拟环境（3.9~3.12）
- 在虚拟环境中重新安装依赖：`python3 -m pip install -r requirements.txt`

## 15. 后续可扩展方向

- 增加基于能力项权重的岗位模板
- 将评分器从启发式+LLM升级为可校准评分模型
- 增加更细粒度单元测试与集成测试（含 mock graph）
