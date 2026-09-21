# Resume Revision Framework (Java + LangChain4j + SiliconFlow)

## 1. Environment variables

You can use either direct environment variables or a local `.env` file.

### Option A: direct environment variables (PowerShell)

```powershell
$env:SILICONFLOW_API_KEY="your_api_key"
$env:SILICONFLOW_BASE_URL="https://api.siliconflow.cn/v1"
$env:SILICONFLOW_MODEL="Qwen/Qwen2.5-72B-Instruct"
$env:RESUME_LLM_TEMPERATURE="0.3"
$env:RESUME_LLM_MAX_TOKENS="2048"
```

`SILICONFLOW_API_KEY` is required.

### Option B: `.env` file (recommended)

1. Edit `.env` in project root:

```dotenv
SILICONFLOW_API_KEY=replace_with_your_real_api_key
SILICONFLOW_BASE_URL=https://api.siliconflow.cn/v1
SILICONFLOW_MODEL=Qwen/Qwen2.5-72B-Instruct
RESUME_LLM_TEMPERATURE=0.3
RESUME_LLM_MAX_TOKENS=2048
```

2. Load `.env` into current PowerShell session:

```powershell
.\scripts\load-dotenv.ps1
```

3. Then run Maven commands in the same shell window.

## 2. Prepare resume source file

Create a plain text file, for example: `sample-resume.txt`

## 3. Run

```powershell
mvn -q -DskipTests compile
mvn -q exec:java -Dexec.args="Java后端开发工程师 sample-resume.txt 控制在一页，突出项目量化成果"
```

## 4. Current architecture

- `SiliconFlowConfig`: reads model configs from environment variables.
- `ModelFactory`: creates LangChain4j `OpenAiChatModel` with SiliconFlow base URL.
- `ResumeLlmGateway`: unified model access interface for downstream backend integration.
- `SiliconFlowResumeLlmGateway`: SiliconFlow implementation of `ResumeLlmGateway`.
- `ResumeEditorAgent`: prompt and output contract definition.
- `ResumeRevisionService`: orchestrates revision and parses structured output.
- `ResumeSectionSplitter`: enhanced division algorithm (title pattern + keyword fallback + normalized section names).
- `Application`: runnable CLI entry point.

## 5. Next extension suggestion

- Add a REST layer (Spring Boot Controller).
- Add retrieval (RAG) for JD keyword enhancement.
- Add test cases for output parser and prompt quality checks.
