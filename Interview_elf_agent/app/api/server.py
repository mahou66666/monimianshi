from fastapi import FastAPI, HTTPException, Depends, Header
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from langchain_core.messages import HumanMessage
import uvicorn
import logging
import asyncio
import time
import urllib.request
import urllib.error
import os
import sys
import subprocess
import tempfile
import json

from qdrant_client import QdrantClient
from qdrant_client.http import models as qdrant_models

from app.api.schemas import (
    AnalyzeRequest,
    AnalyzeResponse,
    KBCountResponse,
    KBDeleteRequest,
    KBExportRequest,
    KBFilter,
    KBIngestRequest,
    KBIngestResponse,
    ResumeScoreNormalizeConfig,
    TaskSubmitResponse,
    TaskStatusResponse,
)
from app.graph.workflow import app_graph
from app.utils.resume import assemble_resume_markdown, has_meaningful_resume_data
from app.utils.interview_mode import resolve_interview_mode
from app.utils.task_queue import TaskQueue
from app.core.config import settings

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


app = FastAPI(title="Interview Agent Microservice")

task_queue = TaskQueue(
    max_concurrency=settings.MAX_CONCURRENCY,
    timeout_seconds=settings.TASK_TIMEOUT_SECONDS,
)

STATIC_DIR = os.path.join(settings.BASE_DIR, "app", "api", "static")
if os.path.isdir(STATIC_DIR):
    app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")


class CircuitBreaker:
    def __init__(self, threshold: int, recovery_seconds: int) -> None:
        self._threshold = threshold
        self._recovery_seconds = recovery_seconds
        self._failures = 0
        self._opened_at = 0.0

    def allow(self) -> bool:
        if self._failures < self._threshold:
            return True
        if time.time() - self._opened_at > self._recovery_seconds:
            self._failures = 0
            self._opened_at = 0.0
            return True
        return False

    def success(self) -> None:
        self._failures = 0
        self._opened_at = 0.0

    def failure(self) -> None:
        self._failures += 1
        if self._failures >= self._threshold:
            self._opened_at = time.time()


breaker = CircuitBreaker(
    threshold=settings.CB_FAILURE_THRESHOLD,
    recovery_seconds=settings.CB_RECOVERY_SECONDS,
)


def is_qdrant_ready() -> bool:
    url = f"http://{settings.QDRANT_HOST}:{settings.QDRANT_PORT}/healthz"
    try:
        with urllib.request.urlopen(url, timeout=2) as resp:
            return resp.status == 200
    except Exception:
        return False


@app.on_event("startup")
async def wait_for_qdrant():
    if not settings.QDRANT_WAIT_ON_STARTUP:
        return
    deadline = time.monotonic() + settings.QDRANT_WAIT_TIMEOUT_SECONDS
    while time.monotonic() < deadline:
        if is_qdrant_ready():
            logger.info("Qdrant is ready.")
            return
        await asyncio.sleep(settings.QDRANT_WAIT_INTERVAL_SECONDS)
    logger.warning("Qdrant not ready after startup wait window.")


def verify_api_key(x_api_key: str = Header(default="")):
    if not settings.REQUIRE_API_KEY:
        return
    if not settings.API_KEY or x_api_key != settings.API_KEY:
        raise HTTPException(status_code=401, detail="Unauthorized")


def get_qdrant_admin_client() -> QdrantClient:
    return QdrantClient(host=settings.QDRANT_HOST, port=settings.QDRANT_PORT)


def build_kb_filter(filter_obj: KBFilter | dict | None):
    if not filter_obj:
        return None
    raw = filter_obj if isinstance(filter_obj, dict) else filter_obj.model_dump(exclude_none=True)
    conditions = []
    for key in ("major", "topic", "subtopic", "source", "type"):
        value = raw.get(key)
        if not value:
            continue
        if isinstance(value, list):
            cond = qdrant_models.FieldCondition(
                key=key, match=qdrant_models.MatchAny(any=value)
            )
        else:
            cond = qdrant_models.FieldCondition(
                key=key, match=qdrant_models.MatchValue(value=value)
            )
        conditions.append(cond)
    if not conditions:
        return None
    return qdrant_models.Filter(must=conditions)


def _safe_path(path: str) -> str:
    base_dir = os.path.realpath(settings.BASE_DIR)
    real = os.path.realpath(path)
    if real.startswith(base_dir) or real.startswith(tempfile.gettempdir()):
        return real
    raise HTTPException(status_code=400, detail="Path must be under project directory or /tmp")


def _run_ingest_command(args: list[str]) -> str:
    cmd = [sys.executable] + args
    proc = subprocess.run(cmd, capture_output=True, text=True)
    output = (proc.stdout or "") + (proc.stderr or "")
    if proc.returncode != 0:
        raise HTTPException(status_code=500, detail=output[-2000:])
    return output[-2000:] if output else "ok"


def _current_resume_score_config() -> ResumeScoreNormalizeConfig:
    return ResumeScoreNormalizeConfig(
        enabled=settings.RESUME_SCORE_NORMALIZE,
        center=settings.RESUME_SCORE_CENTER,
        spread=settings.RESUME_SCORE_SPREAD,
        min_score=settings.RESUME_SCORE_MIN,
        max_score=settings.RESUME_SCORE_MAX,
    )


async def run_agent_task(request: AnalyzeRequest) -> AnalyzeResponse:
    if not breaker.allow():
        raise HTTPException(status_code=503, detail="Service temporarily unavailable")
    resume_content = (request.resume_text or "").strip()
    if not resume_content and request.input_data:
        input_payload = request.input_data.model_dump(exclude_none=True)
        embedded_resume_text = (request.input_data.resume_text or "").strip()
        if embedded_resume_text:
            resume_content = embedded_resume_text
        else:
            if not has_meaningful_resume_data(input_payload):
                raise HTTPException(
                    status_code=400,
                    detail="input_data 缺少可识别的非空简历内容，请检查字段名与数据结构",
                )
            resume_content = assemble_resume_markdown(input_payload).strip()

    if not resume_content:
        mode = resolve_interview_mode(request.task_type, request.jd_text)
        if mode == "jd_only":
            if not request.jd_text:
                raise HTTPException(status_code=400, detail="JD 押题必须提供 jd_text")
            resume_content = ""
        else:
            raise HTTPException(status_code=400, detail="必须提供 resume_text 或 input_data")

    initial_state = {
        "messages": [HumanMessage(content=f"开始执行任务: {request.task_type}")],
        "resume_text": resume_content,
        "jd_text": request.jd_text or "",
        "task_type": request.task_type,
        "question_count": request.question_count,
        "retry_count": 0,
        "evaluation_data": None,
        "metadata": request.metadata or {},
        "input_data": request.input_data.model_dump(exclude_none=True)
        if request.input_data
        else None,
    }

    last_error = None
    for attempt in range(settings.RETRY_MAX + 1):
        try:
            result = await asyncio.wait_for(
                asyncio.to_thread(app_graph.invoke, initial_state),
                timeout=settings.TASK_TIMEOUT_SECONDS,
            )
            breaker.success()
            break
        except Exception as exc:
            last_error = exc
            breaker.failure()
            if attempt >= settings.RETRY_MAX:
                raise last_error
            await asyncio.sleep(settings.RETRY_BACKOFF_SECONDS * (attempt + 1))
    final_output = ""
    if result["messages"]:
        final_output = result["messages"][-1].content
    structured_data = result.get("evaluation_data")
    trace_log = [f"{m.type}: {m.content[:30]}..." for m in result["messages"]]

    return AnalyzeResponse(
        code=200,
        msg="success",
        data=structured_data,
        trace_log=trace_log,
    )

# --- 1. 健康检查 (Spring Boot Actuator 需要) ---
@app.get("/health")
async def health_check():
    qdrant_ok = is_qdrant_ready()
    return {"status": "UP", "service": "python-agent", "qdrant": "UP" if qdrant_ok else "DOWN"}


@app.get("/admin/kb")
async def kb_dashboard():
    dashboard_path = os.path.join(STATIC_DIR, "kb_dashboard.html")
    if not os.path.exists(dashboard_path):
        raise HTTPException(status_code=404, detail="Dashboard not found")
    return FileResponse(dashboard_path)


@app.get("/admin/kb/overview", dependencies=[Depends(verify_api_key)])
async def kb_overview():
    client = get_qdrant_admin_client()
    overview = {
        "qdrant_host": settings.QDRANT_HOST,
        "qdrant_port": settings.QDRANT_PORT,
        "collection_name": settings.COLLECTION_NAME,
        "collection_exists": False,
    }
    try:
        collections = client.get_collections().collections
        overview["collections"] = [c.name for c in collections]
    except Exception as exc:
        overview["collections_error"] = str(exc)

    try:
        exists = client.collection_exists(settings.COLLECTION_NAME)
        overview["collection_exists"] = exists
        if not exists:
            return overview
        info = client.get_collection(settings.COLLECTION_NAME)
        vectors = info.config.params.vectors
        if isinstance(vectors, dict):
            vector_size = next(iter(vectors.values())).size
        else:
            vector_size = vectors.size
        overview["vector_size"] = vector_size
        overview["points_count"] = getattr(info, "points_count", None)
        try:
            count_result = client.count(settings.COLLECTION_NAME, exact=True)
            overview["points_count_exact"] = count_result.count
        except Exception as exc:
            overview["points_count_exact_error"] = str(exc)
        payload_schema = getattr(info, "payload_schema", None)
        if isinstance(payload_schema, dict):
            overview["payload_schema"] = list(payload_schema.keys())
    except Exception as exc:
        overview["collection_error"] = str(exc)
    return overview


@app.get("/admin/kb/points", dependencies=[Depends(verify_api_key)])
async def kb_points(limit: int = 20, offset: str | None = None):
    client = get_qdrant_admin_client()
    try:
        points, next_offset = client.scroll(
            collection_name=settings.COLLECTION_NAME,
            limit=limit,
            offset=offset,
            with_payload=True,
            with_vectors=False,
        )
        items = []
        for point in points:
            payload = point.payload or {}
            items.append(
                {
                    "id": str(point.id),
                    "payload": payload,
                }
            )
        return {"items": items, "next_offset": next_offset}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))


@app.post("/admin/kb/count", response_model=KBCountResponse, dependencies=[Depends(verify_api_key)])
async def kb_count(filter: KBFilter | None = None):
    client = get_qdrant_admin_client()
    query_filter = build_kb_filter(filter)
    try:
        result = client.count(
            collection_name=settings.COLLECTION_NAME,
            exact=True,
            count_filter=query_filter,
        )
        return KBCountResponse(count=result.count)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))


@app.post("/admin/kb/delete", dependencies=[Depends(verify_api_key)])
async def kb_delete(request: KBDeleteRequest):
    client = get_qdrant_admin_client()
    if request.ids:
        try:
            client.delete(
                collection_name=settings.COLLECTION_NAME,
                points_selector=request.ids,
            )
            return {"status": "ok", "deleted": len(request.ids)}
        except Exception as exc:
            raise HTTPException(status_code=500, detail=str(exc))
    query_filter = build_kb_filter(request.filter)
    if query_filter is None:
        raise HTTPException(status_code=400, detail="ids or filter required")
    try:
        client.delete(
            collection_name=settings.COLLECTION_NAME,
            points_selector=query_filter,
        )
        return {"status": "ok", "deleted": "filter"}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))


@app.post("/admin/kb/ingest", response_model=KBIngestResponse, dependencies=[Depends(verify_api_key)])
async def kb_ingest(request: KBIngestRequest):
    input_path = request.input_path or os.path.join(settings.BASE_DIR, "data_source")
    input_path = _safe_path(input_path)
    args = [
        os.path.join(settings.BASE_DIR, "scripts", "ingest_files.py"),
        "--input",
        input_path,
        "--source",
        request.source or "auto",
        "--mode",
        request.mode or "incremental",
        "--batch-size",
        str(request.batch_size or 64),
        "--chunk-size",
        str(request.chunk_size or 700),
        "--chunk-overlap",
        str(request.chunk_overlap or 100),
    ]
    if request.recreate:
        args.append("--recreate")
    output = await asyncio.to_thread(_run_ingest_command, args)
    return KBIngestResponse(status="ok", output=output)


@app.post("/admin/kb/recreate", response_model=KBIngestResponse, dependencies=[Depends(verify_api_key)])
async def kb_recreate(request: KBIngestRequest):
    request.mode = "full"
    request.recreate = True
    return await kb_ingest(request)


@app.post("/admin/kb/export", dependencies=[Depends(verify_api_key)])
async def kb_export(request: KBExportRequest):
    client = get_qdrant_admin_client()
    query_filter = build_kb_filter(request.filter)
    output_path = request.output_path or os.path.join(tempfile.gettempdir(), "kb_export.jsonl")
    output_path = _safe_path(output_path)
    limit = request.limit or 0
    offset = None
    written = 0
    with open(output_path, "w", encoding="utf-8") as f:
        while True:
            points, next_offset = client.scroll(
                collection_name=settings.COLLECTION_NAME,
                limit=200,
                offset=offset,
                with_payload=True,
                with_vectors=False,
                scroll_filter=query_filter,
            )
            if not points:
                break
            for point in points:
                payload = point.payload or {}
                record = {"id": str(point.id), **payload}
                f.write(json.dumps(record, ensure_ascii=False) + "\n")
                written += 1
                if limit > 0 and written >= limit:
                    break
            if limit > 0 and written >= limit:
                break
            offset = next_offset
            if offset is None:
                break
    return FileResponse(output_path, filename=os.path.basename(output_path))


@app.get(
    "/admin/score/normalize",
    response_model=ResumeScoreNormalizeConfig,
    dependencies=[Depends(verify_api_key)],
)
async def get_resume_score_normalize_config():
    return _current_resume_score_config()


@app.post(
    "/admin/score/normalize",
    response_model=ResumeScoreNormalizeConfig,
    dependencies=[Depends(verify_api_key)],
)
async def update_resume_score_normalize_config(request: ResumeScoreNormalizeConfig):
    next_enabled = (
        settings.RESUME_SCORE_NORMALIZE if request.enabled is None else bool(request.enabled)
    )
    next_center = (
        settings.RESUME_SCORE_CENTER if request.center is None else float(request.center)
    )
    next_spread = (
        settings.RESUME_SCORE_SPREAD if request.spread is None else float(request.spread)
    )
    next_min = (
        settings.RESUME_SCORE_MIN if request.min_score is None else float(request.min_score)
    )
    next_max = (
        settings.RESUME_SCORE_MAX if request.max_score is None else float(request.max_score)
    )
    if next_min > next_max:
        raise HTTPException(
            status_code=400,
            detail="RESUME_SCORE_MIN cannot be greater than RESUME_SCORE_MAX",
        )
    if next_enabled and next_spread <= 0:
        raise HTTPException(
            status_code=400,
            detail="RESUME_SCORE_SPREAD must be > 0 when normalization is enabled",
        )

    settings.RESUME_SCORE_NORMALIZE = next_enabled
    settings.RESUME_SCORE_CENTER = next_center
    settings.RESUME_SCORE_SPREAD = next_spread
    settings.RESUME_SCORE_MIN = next_min
    settings.RESUME_SCORE_MAX = next_max

    return _current_resume_score_config()

# --- 2. 核心业务接口 (更新为 V1 路径) ---
@app.post(
    "/api/v1/agent/process",
    response_model=AnalyzeResponse,
    dependencies=[Depends(verify_api_key)],
)
async def process_task(request: AnalyzeRequest, question_count: int | None = None):
    """
    统一入口：接收 Spring Boot 传来的数据，执行指定任务
    """
    if question_count:
        request.question_count = question_count
    return await run_agent_task(request)


@app.post(
    "/api/v1/agent/submit",
    response_model=TaskSubmitResponse,
    dependencies=[Depends(verify_api_key)],
)
async def submit_task(request: AnalyzeRequest, question_count: int | None = None):
    if question_count:
        request.question_count = question_count
    task_id = await task_queue.submit(run_agent_task(request))
    return TaskSubmitResponse(code=200, msg="success", task_id=task_id)


@app.get(
    "/api/v1/agent/status/{task_id}",
    response_model=TaskStatusResponse,
    dependencies=[Depends(verify_api_key)],
)
async def get_task_status(task_id: str):
    task = await task_queue.get(task_id)
    if not task:
        raise HTTPException(status_code=404, detail="task not found")
    return TaskStatusResponse(
        code=200,
        msg="success",
        task_id=task_id,
        status=task["status"],
        data=task.get("result"),
        error=task.get("error"),
        trace_log=task.get("trace_log"),
    )


@app.post(
    "/api/v1/agent/score",
    response_model=AnalyzeResponse,
    dependencies=[Depends(verify_api_key)],
)
async def process_score(request: AnalyzeRequest):
    request.task_type = "score_only"
    return await run_agent_task(request)


@app.post(
    "/api/v1/agent/interview",
    response_model=AnalyzeResponse,
    dependencies=[Depends(verify_api_key)],
)
async def process_interview(request: AnalyzeRequest, question_count: int | None = None):
    if question_count:
        request.question_count = question_count
    request.task_type = "interview"
    return await run_agent_task(request)


@app.post(
    "/api/v1/agent/interview/resume",
    response_model=AnalyzeResponse,
    dependencies=[Depends(verify_api_key)],
)
async def process_interview_resume(request: AnalyzeRequest, question_count: int | None = None):
    if question_count:
        request.question_count = question_count
    request.task_type = "interview_resume"
    return await run_agent_task(request)


@app.post(
    "/api/v1/agent/interview/jd",
    response_model=AnalyzeResponse,
    dependencies=[Depends(verify_api_key)],
)
async def process_interview_jd(request: AnalyzeRequest, question_count: int | None = None):
    if question_count:
        request.question_count = question_count
    request.task_type = "interview_jd"
    return await run_agent_task(request)

if __name__ == "__main__":
    logger.info("🚀 Agent Service starting on port 8000...")
    uvicorn.run(app, host="0.0.0.0", port=8000)
