import asyncio
import time
import uuid
from typing import Any, Dict, Optional


class TaskQueue:
    def __init__(self, max_concurrency: int = 4, timeout_seconds: int = 120) -> None:
        self._lock = asyncio.Lock()
        self._tasks: Dict[str, Dict[str, Any]] = {}
        self._sem = asyncio.Semaphore(max_concurrency)
        self._timeout_seconds = timeout_seconds

    async def submit(self, coro) -> str:
        task_id = uuid.uuid4().hex
        async with self._lock:
            self._tasks[task_id] = {
                "status": "PENDING",
                "result": None,
                "error": None,
                "trace_log": [],
                "submitted_at": time.time(),
            }
        asyncio.create_task(self._run(task_id, coro))
        return task_id

    async def _run(self, task_id: str, coro) -> None:
        try:
            async with self._sem:
                async with self._lock:
                    self._tasks[task_id]["status"] = "RUNNING"
                result = await asyncio.wait_for(coro, timeout=self._timeout_seconds)
                if hasattr(result, "model_dump"):
                    payload = result.model_dump()
                elif isinstance(result, dict):
                    payload = result
                else:
                    payload = {}
                async with self._lock:
                    self._tasks[task_id]["status"] = "SUCCESS"
                    self._tasks[task_id]["result"] = payload.get("data")
                    self._tasks[task_id]["trace_log"] = payload.get("trace_log") or []
        except Exception as exc:
            async with self._lock:
                self._tasks[task_id]["status"] = "FAILED"
                self._tasks[task_id]["error"] = str(exc)

    async def get(self, task_id: str) -> Optional[Dict[str, Any]]:
        async with self._lock:
            return self._tasks.get(task_id)
