# Interview Elf Agent

面向简历分析与面试题生成的 FastAPI 微服务。当前流程使用 LangGraph 编排多个角色节点（分析、审核、面试）。支持前端/后端通过 HTTP 接口传入数据，Agent 处理后返回结构化结果。

## 项目说明

该项目聚焦“简历分析 + 面试押题 + 知识库辅助”，提供统一的 HTTP 服务接口，支持同步/异步两种调用方式。

核心能力：
- 简历评分、问题分析与改进建议
- 面试押题（简历押题 / JD 押题 / 综合押题）
- RAG 知识库检索（Qdrant）辅助生成
- 结构化输出（便于前端直接渲染）

系统流程（简化）：
1) 解析简历文本或结构化输入  
2) 按 task_type 选择链路（评分 / 问题分析 / 面试押题）  
3) 面试押题阶段可结合 JD + 知识库，生成不少于指定题量的问题  
4) 后处理补答：基于简历内容为每题补 1–3 句参考答案  

## 快速开始

1. 复制环境变量模板并填写：
   ```bash
   cp .env.example .env
   ```
2. 安装依赖：
   ```bash
   pip install -r requirements.txt
   ```
3. 启动服务：
   ```bash
   python app/api/server.py
   ```

## RAG 入库与运行（本地/容器双模式）

### 一、本地模式（推荐开发调试）

1. 启动 Qdrant（本地 Docker）：
   ```bash
   docker run -p 6333:6333 -v "$(pwd)/qdrant_data:/qdrant/storage" qdrant/qdrant:v1.13.0
   ```
2. 配置 `.env`（本地运行）：
   ```bash
   QDRANT_HOST=localhost
   QDRANT_PORT=6333
   COLLECTION_NAME=interview_kb
   ```
3. 放入知识文件（支持 `.docx/.xlsx`）：
   ```
   ./data_source
   ```
4. 入库：
   ```bash
   python scripts/ingest_files.py
   ```
5. 启动服务：
   ```bash
   python app/api/server.py
   ```

### 二、容器模式（推荐部署）

1. 配置 `.env`（容器运行）：
   ```bash
   QDRANT_HOST=qdrant
   QDRANT_PORT=6333
   COLLECTION_NAME=interview_kb
   QDRANT_WAIT_ON_STARTUP=true
   ```
2. 启动：
   ```bash
   docker-compose up --build
   ```
3. 入库（在容器中执行，使用挂载的 `./data_source`）：
   ```bash
   docker-compose run --rm interview-elf-agent python scripts/ingest_files.py
   ```

### 三、离线/缓存与镜像源

- 复用缓存：模型默认落在 `./models`，容器会挂载到 `/app/models`，重启不会重复下载。
- 离线模式：先准备好模型目录，然后配置：
  ```bash
  HF_HUB_OFFLINE=true
  EMBEDDING_MODEL_PATH=/absolute/path/to/model
  ```
- 镜像源：需要走镜像时设置：
  ```bash
  HF_ENDPOINT=https://hf-mirror.com
  ```
  若你已有内部镜像源，请替换为你的地址。

## 接口调用指南

### 基础信息

- Base URL（本地默认）：`http://localhost:8000`
- 请求体格式：`application/json`
- 鉴权方式（可选）：请求头 `X-API-Key: <your_api_key>`

如果 `.env` 中 `REQUIRE_API_KEY=true`，则所有业务接口都必须带 `X-API-Key`。

### 接口一览

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/health` | GET | 健康检查 |
| `/api/v1/agent/process` | POST | 同步统一入口（立即返回结果） |
| `/api/v1/agent/submit` | POST | 异步提交（仅返回 task_id） |
| `/api/v1/agent/status/{task_id}` | GET | 异步任务状态查询 |
| `/api/v1/agent/score` | POST | 简历评分（仅评分） |
| `/api/v1/agent/interview` | POST | 面试押题（简历+JD） |
| `/api/v1/agent/interview/resume` | POST | 面试押题（仅简历） |
| `/api/v1/agent/interview/jd` | POST | 面试押题（仅 JD） |
| `/admin/score/normalize` | GET | 查询简历总分正态化参数 |
| `/admin/score/normalize` | POST | 更新简历总分正态化参数 |

### 同步 vs 异步（如何选择）

- `/api/v1/agent/process`（同步）：
  - 适合：调用方希望“一次请求直接拿结果”；
  - 优点：接入简单，不需要任务轮询；
  - 缺点：请求会阻塞到任务完成，超时时间受网关/客户端限制。
- `/api/v1/agent/submit + /api/v1/agent/status/{task_id}`（异步）：
  - 适合：任务较长（如大模型推理、长文本分析）或前后端解耦场景；
  - 优点：提交快速返回 `task_id`，可轮询、可重试、可独立展示进度；
  - 缺点：调用链路更长，需要管理任务状态。

推荐实践：
- 管理后台、批处理、移动网络不稳定场景：优先异步。
- 内网服务编排、短任务直返：优先同步。

### 押题模式说明

- **综合押题**：`task_type=interview` 或 `/api/v1/agent/interview`  
  同时结合简历与 JD，覆盖技能匹配、项目深挖、软技能与开放题。
- **简历押题**：`task_type=interview_resume` 或 `/api/v1/agent/interview/resume`  
  仅依据简历中的项目/实习/工作经历，不使用 JD 与知识库。
- **JD 押题**：`task_type=interview_jd` 或 `/api/v1/agent/interview/jd`  
  仅依据 JD 与知识库，偏技术与八股文方向，不依赖简历信息。

题量下限由 `.env` 中 `MIN_INTERVIEW_QUESTIONS` 控制，默认 40。
如需调整题量，可在请求体或 URL 查询参数中传 `question_count`，实际题量不会低于最小题量。

### 鉴权与调用方式

若启用鉴权（`.env` 中 `REQUIRE_API_KEY=true`），请求头需包含：
```
X-API-Key: your_api_key
```

### 前后端如何调用

- 前端或后端通过 HTTP 请求调用上述接口。
- 支持同步（直接获取结果）与异步（提交任务后轮询状态）两种模式。

#### 前端示例（fetch）

```javascript
const payload = {
  resume_text: "简历文本...",
  jd_text: "岗位JD...",
  task_type: "interview"
};

const resp = await fetch("http://localhost:8000/api/v1/agent/process", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "X-API-Key": "your_api_key"
  },
  body: JSON.stringify(payload)
});
const data = await resp.json();
console.log(data);
```

#### 后端示例（Python requests）

```python
import requests

payload = {
    "resume_text": "简历文本...",
    "jd_text": "岗位JD...",
    "task_type": "score_only"
}
resp = requests.post(
    "http://localhost:8000/api/v1/agent/process",
    json=payload,
    headers={"X-API-Key": "your_api_key"},
    timeout=120,
)
print(resp.json())
```

#### 异步调用示例

```bash
# 提交任务
curl -X POST http://localhost:8000/api/v1/agent/submit \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your_api_key" \
  -d '{"resume_text":"简历文本...","jd_text":"岗位JD...","task_type":"interview"}'

# 查询状态
curl http://localhost:8000/api/v1/agent/status/<task_id> \
  -H "X-API-Key: your_api_key"
```

#### 终端快速测试（Docker + 异步）

```bash
cd /path/to/Interview_elf_agent
docker compose up -d --build
curl -sS http://localhost:8000/health | jq

API_KEY=$(awk -F= '/^API_KEY=/{print $2}' .env)

# 提交一个评分任务
TASK_ID=$(jq -n --arg t "3年后端开发，负责SpringBoot微服务、Redis缓存、MySQL性能优化。" \
'{resume_text:$t,task_type:"score_only"}' | \
curl -sS -X POST "http://localhost:8000/api/v1/agent/submit" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: ${API_KEY}" \
  -d @- | jq -r '.task_id')

echo "task_id=$TASK_ID"

# 轮询直到完成
for i in $(seq 1 90); do
  S=$(curl -sS "http://localhost:8000/api/v1/agent/status/${TASK_ID}" \
    -H "X-API-Key: ${API_KEY}")
  ST=$(echo "$S" | jq -r '.status // empty')
  echo "poll_$i status=$ST"
  if [ "$ST" = "SUCCESS" ] || [ "$ST" = "FAILED" ]; then
    echo "$S" | jq
    break
  fi
  sleep 2
done
```

#### 查看详细信息（status 结果）

完整返回：
```bash
curl -sS "http://localhost:8000/api/v1/agent/status/${TASK_ID}" \
  -H "X-API-Key: ${API_KEY}" | jq
```

只看业务数据：
```bash
curl -sS "http://localhost:8000/api/v1/agent/status/${TASK_ID}" \
  -H "X-API-Key: ${API_KEY}" | jq '.data'
```

只看执行轨迹：
```bash
curl -sS "http://localhost:8000/api/v1/agent/status/${TASK_ID}" \
  -H "X-API-Key: ${API_KEY}" | jq '.trace_log'
```

若偶发 `jq: parse error ... control characters`，可先清洗控制字符再解析：
```bash
curl -sS "http://localhost:8000/api/v1/agent/status/${TASK_ID}" \
  -H "X-API-Key: ${API_KEY}" | \
tr -d '\000-\010\013\014\016-\037' | jq
```

#### 押题接口示例（curl）

综合押题：
```bash
curl -X POST "http://localhost:8000/api/v1/agent/interview?question_count=60" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your_api_key" \
  -d '{"resume_text":"简历文本...","jd_text":"岗位JD..."}'
```

简历押题：
```bash
curl -X POST "http://localhost:8000/api/v1/agent/interview/resume?question_count=60" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your_api_key" \
  -d '{"resume_text":"简历文本..."}'
```

JD 押题：
```bash
curl -X POST "http://localhost:8000/api/v1/agent/interview/jd?question_count=60" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your_api_key" \
  -d '{"jd_text":"岗位JD..."}'
```

#### 后端调用：JD 生题（字段与返回结构）

推荐接口：`POST /api/v1/agent/interview/jd`

请求参数说明：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Header | `Content-Type` | string | 是 | 固定 `application/json` |
| Header | `X-API-Key` | string | 条件必填 | 当 `.env` 中 `REQUIRE_API_KEY=true` 时必填 |
| Query | `question_count` | int | 否 | 期望题量；实际题量 `>= MIN_INTERVIEW_QUESTIONS`（默认 40） |
| Body | `jd_text` | string | 是 | JD 原文（建议完整粘贴岗位职责 + 任职要求） |
| Body | `metadata` | object | 否 | 透传上下文，可用于 RAG 过滤、链路追踪 |

`metadata` 支持的 RAG 过滤字段（可选）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `major` | string \| string[] | 专业过滤 |
| `topic` | string \| string[] | 主题过滤 |
| `subtopic` | string \| string[] | 子主题过滤 |
| `source` | string \| string[] | 数据来源过滤 |
| `type` | string \| string[] | 题型过滤 |

说明：
- 上述过滤字段既可以直接放在 `metadata` 下，也可以放在 `metadata.rag_filter` 下。
- `interview/jd` 接口会强制按 JD 押题模式执行，不依赖 `resume_text`。

请求体示例（最小）：
```json
{
  "jd_text": "负责 NLP 算法研发，熟悉 PyTorch、检索增强生成、向量数据库..."
}
```

请求体示例（含 metadata 与过滤）：
```json
{
  "jd_text": "负责大模型应用研发，要求熟悉 RAG、Agent、Prompt Engineering。",
  "metadata": {
    "request_id": "jd_case_20260313_001",
    "rag_filter": {
      "topic": ["NLP", "LLM"],
      "subtopic": ["RAG", "Agent"],
      "source": ["internal_kb"]
    }
  }
}
```

成功响应结构（`200`）：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "gap_analysis": "候选人与 JD 的能力差距分析...",
    "questions": [
      {
        "question": "请解释 RAG 的召回与重排链路，并说明如何评估召回质量？",
        "answer": "回答可参考：可从召回率、MRR、nDCG 等指标描述评估方式。"
      },
      {
        "question": "如果检索结果噪声较大，你会如何设计 Query Rewrite 与过滤策略？",
        "answer": "回答可参考：可结合关键词扩展、意图分类和元数据过滤策略。"
      }
    ]
  },
  "trace_log": [
    "human: 开始执行任务...",
    "ai: 【模拟面试准备】..."
  ]
}
```

返回字段说明：
- `code`：业务状态码，成功为 `200`
- `msg`：业务状态信息，成功通常为 `success`
- `data.gap_analysis`：JD 维度的差距分析文本
- `data.questions`：题目数组；每项至少包含 `question`，通常包含 `answer`
- `trace_log`：Agent 过程摘要，便于排障（可选）

常见错误响应：

1) 缺少 `jd_text`（HTTP 400）
```json
{
  "detail": "JD 押题必须提供 jd_text"
}
```

2) 鉴权失败（HTTP 401）
```json
{
  "detail": "Unauthorized"
}
```

后端调用示例（Python requests）：
```python
import requests

url = "http://localhost:8000/api/v1/agent/interview/jd"
params = {"question_count": 60}
payload = {
    "jd_text": "岗位 JD 原文...",
    "metadata": {
        "request_id": "backend_jd_test_001",
        "rag_filter": {"topic": ["NLP"], "subtopic": ["RAG"]}
    }
}
headers = {
    "Content-Type": "application/json",
    "X-API-Key": "your_api_key",
}

resp = requests.post(url, params=params, json=payload, headers=headers, timeout=180)
resp.raise_for_status()
data = resp.json()
print("questions_count =", len((data.get("data") or {}).get("questions") or []))
print("first_question =", ((data.get("data") or {}).get("questions") or [{}])[0].get("question"))
```

#### 评分接口示例（curl）

```bash
curl -X POST "http://localhost:8000/api/v1/agent/score" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your_api_key" \
  -d '{"resume_text":"简历文本...","jd_text":"岗位JD（可选）"}'
```

#### 统一入口示例（curl）

```bash
curl -X POST "http://localhost:8000/api/v1/agent/process" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your_api_key" \
  -d '{"resume_text":"简历文本...","jd_text":"岗位JD...","task_type":"score_only"}'
```

#### 异步接口示例（curl）

```bash
curl -X POST "http://localhost:8000/api/v1/agent/submit" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your_api_key" \
  -d '{"resume_text":"简历文本...","jd_text":"岗位JD...","task_type":"interview"}'
```

#### 问题分析专用示例请求/响应

请求示例（同步）：
```bash
curl -X POST "http://localhost:8000/api/v1/agent/process" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your_api_key" \
  -d '{"resume_text":"简历文本...","task_type":"problem_only"}'
```

响应示例（节选）：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "resume_overview": "简历主体信息较完整，但项目成果量化不足，需优先补强关键证据。",
    "problems": [
      {
        "title": "项目描述缺少量化结果",
        "severity": "中",
        "tags": ["项目经历", "量化"],
        "problem": "项目经历只描述职责，缺少结果与指标，难以评估贡献。",
        "answer": "补充关键指标，例如接口延迟降低 30%、QPS 提升 2 倍等。"
      }
    ],
    "optimization_checklist": [
      "补充项目成果的量化指标（性能/效率/业务结果）",
      "统一经历时间格式，并明确项目/实习背景",
      "将技术描述改为“场景-动作-结果”的表述方式"
    ],
    "highlights": [
      {"rank": 1, "highlight": "后端项目经验"},
      {"rank": 2, "highlight": "技术栈覆盖"},
      {"rank": 3, "highlight": "业务实践经验"}
    ],
    "advice": "优先补充量化成果与技术细节，避免空泛描述。"
  }
}
```

### 请求数据格式

所有业务接口均使用 JSON 请求体，字段如下：

```json
{
  "resume_text": "完整简历文本（可选）",
  "input_data": { },
  "jd_text": "岗位JD（可选）",
  "task_type": "auto | problem_only | score_only | interview | interview_resume | interview_jd | refine",
  "question_count": 60,
  "metadata": { "任意扩展字段" }
}
```

新增字段总览（请求侧）：

| 字段 | 位置 | 类型 | 适用场景 | 说明 |
| --- | --- | --- | --- | --- |
| `question_count` | Query 或 Body | int | `interview*` | 题量参数，实际题量不低于 `MIN_INTERVIEW_QUESTIONS` |
| `metadata` | Body | object | 全部任务 | 扩展上下文（如 `request_id`、`callback_url` 等） |
| `metadata.rag_filter` | Body | object | `interview` / `interview_jd` | 知识库检索过滤条件 |
| `metadata.rag_filter.major` | Body | string\|string[] | RAG | 专业过滤 |
| `metadata.rag_filter.topic` | Body | string\|string[] | RAG | 主题过滤 |
| `metadata.rag_filter.subtopic` | Body | string\|string[] | RAG | 子主题过滤 |
| `metadata.rag_filter.source` | Body | string\|string[] | RAG | 来源过滤 |
| `metadata.rag_filter.type` | Body | string\|string[] | RAG | 类型过滤 |

两种输入方式：
1) 直接传 `resume_text`（完整文本）
2) 传 `input_data`（结构化 JSON），系统会自动拼装为 Markdown

`input_data` 校验规则（重要）：
- 只接受已定义板块（如 `personal_info/education/work_history/project_experience/skills/...`），未知字段会返回 `422`。
- `input_data` 必须包含至少一个“非空且可识别”的简历板块，否则返回 `422`（模型校验）或 `400`（运行时保护）。
- 推荐传输格式：`personal_info` 用对象；其余板块（`education/work_history/internship_history/project_experience/skills/campus_experience/awards`）直接传“完整文段字符串”。
- 仍兼容旧结构化格式（数组对象），例如 `skills` 也可传 `[{ "name": "...", "items": ["...", "..."] }]`。
- 兼容旧版切割字段：`BASIC_INFO/EDUCATION/WORK_EXPERIENCE/INTERNSHIP_EXPERIENCE/PROJECT_EXPERIENCE/SKILLS/AWARDS/SELF_EVALUATION`，服务端会自动映射到标准字段。
- `rawContent/errorMessage` 会被忽略，不再直接作为简历正文参与评分。
- AI 评估阶段对乱码容错：若简历中存在乱码/转码异常/OCR 噪声，系统会提示模型忽略该片段，不将乱码本身作为扣分或风险依据。

`question_count` 的使用方式：
1) 统一入口 `/api/v1/agent/process`：可放在请求体里  
2) 押题接口 `/api/v1/agent/interview*`：建议放在 URL 参数 `?question_count=60`

### 结构化简历 input_data 示例

```json
{
  "personal_info": {
    "name": "张三",
    "phone": "13800138000",
    "email": "zhangsan@example.com",
    "location": "北京",
    "summary": "专注后端与数据工程"
  },
  "education": "某大学 本科 计算机科学 2019.09-2023.06，主修数据结构、操作系统、数据库系统，GPA 3.8/4.0。",
  "work_history": "字节跳动 后端实习生 2023.06-2023.09，参与服务拆分，优化核心接口性能，支持业务高峰稳定性。",
  "internship_history": "某互联网公司 Java 实习生 2022.07-2022.09，负责报表模块开发与性能优化。",
  "project_experience": "智能简历助手项目（后端负责人），技术栈 Python/FastAPI/LangChain，实现多角色协作与结构化输出。",
  "skills": "熟练使用 Python、Java、SQL；掌握 FastAPI、Spring Boot、MyBatis；熟悉 Redis、MySQL、Docker。",
  "campus_experience": "担任学院技术社团负责人，组织算法训练与项目实战活动。",
  "awards": "ACM 银牌（2022），校级一等奖学金（2021）。",
  "self_evaluation": "学习能力强，能快速定位问题并推动落地。"
}
```

### 响应数据格式

统一响应：
```json
{
  "code": 200,
  "msg": "success",
  "data": { "业务结果" },
  "trace_log": ["Agent 过程摘要..."]
}
```

新增字段总览（响应侧）：

| 字段 | 所在对象 | 类型 | 说明 |
| --- | --- | --- | --- |
| `dimension_briefs` | `EvaluationResult` | object[] | 6个维度的扣分点/优势点简介 |
| `dimension_briefs[].dimension` | `EvaluationResult.dimension_briefs[]` | string | 维度名称 |
| `dimension_briefs[].score` | `EvaluationResult.dimension_briefs[]` | int | 该维度分数 |
| `dimension_briefs[].deduction_summary` | `EvaluationResult.dimension_briefs[]` | string | 扣分点一句话简介 |
| `dimension_briefs[].strength_summary` | `EvaluationResult.dimension_briefs[]` | string | 优势点一句话简介 |
| `problems` | `EvaluationResult` | object[] | 结构化问题列表（与 `problem_only` 同格式） |
| `problems[].title` | `EvaluationResult.problems[]` | string | 问题标题 |
| `problems[].severity` | `EvaluationResult.problems[]` | string | 严重程度（低/中/高） |
| `problems[].tags` | `EvaluationResult.problems[]` | string[] | 2-5 个标签 |
| `problems[].problem` | `EvaluationResult.problems[]` | string | 问题描述 |
| `problems[].answer` | `EvaluationResult.problems[]` | string | 简历优化建议 |
| `resume_overview` | `ProblemAnalysisResult` | string | 简历现状简介（一句话简短概括） |
| `problems[].answer` | `ProblemAnalysisResult` | string | 简历优化建议（不是面试回答） |
| `optimization_checklist` | `ProblemAnalysisResult` | string[] | 3-5 条简历优化清单（简洁直观） |
| `highlights` | `ProblemAnalysisResult` | object[] | 3 条亮点，按主次排序 |
| `highlights[].rank` | `highlights[]` | int | 亮点优先级，`1` 为最大亮点 |
| `highlights[].highlight` | `highlights[]` | string | 亮点关键词短语（如“后端项目经验”） |

简历评分结果 `data` 示例：
```json
{
  "scores": {
    "education_score": 80,
    "work_score": 75,
    "project_score": 78,
    "skill_score": 82,
    "award_score": 60,
    "job_fit_score": 70,
    "total_score": 77
  },
  "dimension_analysis": "各维度评分理由...",
  "dimension_briefs": [
    {
      "dimension": "教育经历",
      "score": 80,
      "deduction_summary": "院校背景信息不够完整，扣分主要来自证据不足。",
      "strength_summary": "专业方向与目标岗位相关，基础较好。"
    }
  ],
  "problems": [
    {
      "title": "项目描述缺少量化结果",
      "severity": "中",
      "tags": ["项目经历", "量化"],
      "problem": "项目描述偏概括，缺少结果指标与业务影响。",
      "answer": "建议：补充关键指标，例如延迟降低30%、QPS提升2倍。"
    }
  ],
  "advice": "修改建议..."
}
```

简历问题分析字段说明：
- `resume_overview`：简历现状简介（一句话简短概括，放在结果最开始）
- `problems`：问题列表，每项包含 `title`（简短标题）、`severity`（低/中/高）、`tags`（2-5 个标签）、`problem`（问题描述）、`answer`（简历优化建议：告诉候选人该如何改写）
- `optimization_checklist`：简历优化清单（3-5 条，简洁直观，每条一个动作）
- `highlights`：简历亮点（固定 3 条，按主次排序，`rank=1` 为最大亮点；`highlight` 使用关键词短语）
- `advice`：整体修改建议

简历问题分析结果 `data` 示例（含标题/严重程度/标签）：
```json
{
  "resume_overview": "简历主体信息较完整，但项目成果量化不足，需优先补强关键证据。",
  "problems": [
    {
      "title": "项目描述缺少量化结果",
      "severity": "中",
      "tags": ["项目经历", "量化"],
      "problem": "项目经历只描述职责，缺少结果与指标，难以评估贡献。",
      "answer": "补充关键指标，例如接口延迟降低 30%、QPS 提升 2 倍等。"
    }
  ],
  "optimization_checklist": [
    "补充项目成果的量化指标（性能/效率/业务结果）",
    "统一经历时间格式，并明确项目/实习背景",
    "将技术描述改为“场景-动作-结果”的表述方式"
  ],
  "highlights": [
    {"rank": 1, "highlight": "后端项目经验"},
    {"rank": 2, "highlight": "技术栈覆盖"},
    {"rank": 3, "highlight": "业务实践经验"}
  ],
  "advice": "优先补充量化成果与技术细节，避免空泛描述。"
}
```

面试预测结果 `data` 示例：
```json
{
  "gap_analysis": "能力差距分析...",
  "questions": [
    {"question": "问题1", "answer": "参考回答1"},
    {"question": "问题2", "answer": "参考回答2"}
  ]
}
```

异步任务提交响应：
```json
{
  "code": 200,
  "msg": "success",
  "task_id": "xxxx"
}
```

异步任务状态查询响应：
```json
{
  "code": 200,
  "msg": "success",
  "task_id": "xxxx",
  "status": "PENDING | RUNNING | SUCCESS | FAILED",
  "data": { "业务结果" },
  "error": null,
  "trace_log": ["Agent 过程摘要..."]
}
```

说明：若提供 `resume_text`，系统直接使用文本；若提供 `input_data`，系统会自动拼装为 Markdown。若 `input_data` 无有效内容，不会再生成空占位简历。

## 环境变量

- `OPENAI_API_KEY`：必填
- `MODEL_NAME`：模型名称
- `BASE_URL`：OpenAI 兼容 API 地址
- `TEMPERATURE`：默认 `0.0`
- `QUESTION_TEMPERATURE`：面试题生成用温度，默认 `0.5`
- `MIN_INTERVIEW_QUESTIONS`：面试题最少题量，默认 `40`
- `RESUME_WEIGHT_EDUCATION`：教育经历权重（默认 0.15）
- `RESUME_WEIGHT_WORK`：工作/实习权重（默认 0.25）
- `RESUME_WEIGHT_PROJECT`：项目权重（默认 0.20）
- `RESUME_WEIGHT_SKILL`：技能权重（默认 0.20）
- `RESUME_WEIGHT_AWARD`：获奖权重（默认 0.05）
- `RESUME_WEIGHT_JOB_FIT`：岗位匹配度权重（默认 0.15）
- `RESUME_SCORE_NORMALIZE`：是否启用总分正态化（true/false，默认 true）
- `RESUME_SCORE_CENTER`：总分分布中心（默认 75）
- `RESUME_SCORE_SPREAD`：总分分布跨度（默认 15）
- `RESUME_SCORE_MIN`：总分最低值（默认 55）
- `RESUME_SCORE_MAX`：总分最高值（默认 95）

## 简历总分正态化参数（可动态调控）

后端可通过以下接口动态调整正态化参数（需要 `X-API-Key`）：

- 查询当前参数：
  - `GET /admin/score/normalize`
- 更新参数：
  - `POST /admin/score/normalize`

示例：
```bash
curl -X POST "http://localhost:8000/admin/score/normalize" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your_api_key" \
  -d '{
    "enabled": true,
    "center": 75,
    "spread": 12,
    "min_score": 60,
    "max_score": 92
  }'
```
- `TOP_P`：默认 `1.0`
- `TOP_K`：可选，不填则不发送
- `FREQUENCY_PENALTY`：默认 `0.0`
- `ENABLE_THINKING`：是否启用思考模式（true/false）
- `API_KEY`：接口访问密钥
- `REQUIRE_API_KEY`：是否强制校验 API Key（true/false）
- `MAX_CONCURRENCY`：最大并发任务数（含异步队列）
- `TASK_TIMEOUT_SECONDS`：单次任务超时（秒）
- `RETRY_MAX`：失败重试次数
- `RETRY_BACKOFF_SECONDS`：重试退避基础秒数
- `CB_FAILURE_THRESHOLD`：熔断失败阈值
- `CB_RECOVERY_SECONDS`：熔断恢复时间（秒）
