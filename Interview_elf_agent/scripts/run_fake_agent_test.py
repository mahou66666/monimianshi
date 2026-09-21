import argparse
import json
import os
import sys
import urllib.request


def load_text(path: str) -> str:
    with open(path, "r", encoding="utf-8") as f:
        return f.read().strip()


def load_json(path: str) -> dict:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def main() -> int:
    parser = argparse.ArgumentParser(description="Run agent test with local fake data.")
    parser.add_argument(
        "--task-type",
        default=os.getenv("TASK_TYPE", "interview"),
        help="Task type: problem_only | score_only | interview | auto | refine",
    )
    parser.add_argument(
        "--raw",
        action="store_true",
        help="Print raw JSON response",
    )
    parser.add_argument(
        "--timeout",
        type=int,
        default=int(os.getenv("REQUEST_TIMEOUT", "120")),
        help="Request timeout in seconds",
    )
    parser.add_argument(
        "--out",
        default="",
        help="Write output to file path",
    )
    args = parser.parse_args()

    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    data_dir = os.path.join(base_dir, "test_data")

    resume_path = os.path.join(data_dir, "resume.txt")
    jd_path = os.path.join(data_dir, "jd.txt")
    input_data_path = os.path.join(data_dir, "input_data.json")

    payload = load_json(os.path.join(data_dir, "request.json"))
    payload["resume_text"] = load_text(resume_path)
    payload["jd_text"] = load_text(jd_path)
    payload["input_data"] = load_json(input_data_path)
    payload["task_type"] = args.task_type

    endpoint = os.getenv("AGENT_ENDPOINT", "http://localhost:8000/api/v1/agent/process")
    api_key = os.getenv("API_KEY", "")

    body = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(endpoint, data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    if api_key:
        req.add_header("X-API-Key", api_key)

    try:
        with urllib.request.urlopen(req, timeout=args.timeout) as resp:
            raw = resp.read().decode("utf-8")
            if args.raw:
                if args.out:
                    with open(args.out, "w", encoding="utf-8") as f:
                        f.write(raw)
                else:
                    print(raw)
                return 0
            data = json.loads(raw)
            lines = [f"code={data.get('code')} msg={data.get('msg')}"]
            result = data.get("data") or {}
            if args.task_type == "score_only":
                scores = (result.get("scores") or {})
                total = scores.get("total_score")
                lines.append(f"total_score={total}")
            elif args.task_type == "problem_only":
                problems = result.get("problems") or []
                lines.append(f"problems_count={len(problems)}")
                for idx, item in enumerate(problems, 1):
                    if isinstance(item, dict):
                        title_text = item.get("title", "")
                        severity_text = item.get("severity", "")
                        tags_list = item.get("tags") or []
                        problem_text = item.get("problem", "")
                        answer_text = item.get("answer", "")
                    else:
                        title_text = ""
                        severity_text = ""
                        tags_list = []
                        problem_text = str(item)
                        answer_text = ""
                    tags_text = ",".join([str(t) for t in tags_list if t])
                    if title_text:
                        lines.append(f"{idx}. title={title_text}")
                    if severity_text:
                        lines.append(f"   severity={severity_text}")
                    if tags_text:
                        lines.append(f"   tags={tags_text}")
                    lines.append(f"   problem={problem_text}")
                    if answer_text:
                        lines.append(f"   answer={answer_text}")
            else:
                gap = result.get("gap_analysis", "")
                questions = result.get("questions") or []
                lines.append(f"gap_analysis={gap[:120]}{'...' if len(gap) > 120 else ''}")
                lines.append(f"questions_count={len(questions)}")
                if questions:
                    first = questions[0] or {}
                    if isinstance(first, dict):
                        first_q = first.get("question") or first.get("text") or ""
                    else:
                        first_q = str(first)
                    lines.append(f"first_question={first_q}")
            output = "\n".join(lines)
            if args.out:
                with open(args.out, "w", encoding="utf-8") as f:
                    f.write(output)
            else:
                print(output)
        return 0
    except Exception as exc:
        print(f"Request failed: {exc}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
