#!/usr/bin/env python3

# Interview_elf_agent/scripts/convert_docs_to_qa_jsonl.py

import argparse
import hashlib
import json
import os
import re
import sys
from typing import Dict, Iterable, List, Optional, Tuple

try:
    from docx import Document as DocxDocument
except ImportError:
    DocxDocument = None

try:
    import pdfplumber
except ImportError:
    pdfplumber = None


def _stable_id(question: str, answer: str) -> str:
    raw = f"{question}\n{answer}".encode("utf-8")
    return hashlib.md5(raw).hexdigest()


def _is_heading(text: str, style_name: str = "", prev_blank: bool = False) -> bool:
    if not text:
        return False
    style = style_name.lower()
    if "heading" in style or "title" in style or "标题" in style_name:
        return True
    if _looks_like_numbered_question(text):
        return True
    if text.endswith(("?", "？")) and len(text) <= 60:
        return True
    if prev_blank and len(text) <= 40 and not text.endswith(("。", "；", "，", ".", ";")):
        return True
    return False


_QUESTION_PREFIX_PATTERNS = [
    re.compile(r"^\s*\d+\s*[\.\、\)]\s*"),
    re.compile(r"^\s*[（(]?\d+[)）]\s*"),
    re.compile(r"^\s*[一二三四五六七八九十]{1,3}\s*[、\.]\s*"),
    re.compile(r"^\s*Q[:：]\s*", re.IGNORECASE),
    re.compile(r"^\s*(题目|问题)[:：]\s*"),
]


_QUESTION_CUE_PATTERN = re.compile(
    r"(什么|为何|为什么|如何|怎么|是否|请问|请解释|解释|区别|作用|原理|优缺点|优点|缺点|意义|步骤|流程|方法|注意|场景|比较|实现|设计|适用|怎么做|如何做)"
)
_ANSWER_LIST_CUE_PATTERN = re.compile(
    r"(首先|其次|最后|一般|通常|包括|包含|例如|比如|可以|需要|应该|常见|主要|比如说)"
)


def _has_question_cue(text: str) -> bool:
    if "?" in text or "？" in text:
        return True
    return bool(_QUESTION_CUE_PATTERN.search(text))


def _is_numbered_line(text: str) -> bool:
    if len(text) > 140:
        return False
    for pattern in _QUESTION_PREFIX_PATTERNS:
        if pattern.search(text):
            return True
    return False


def _title_like(text: str) -> bool:
    has_end_punct = any(p in text for p in ("。", "；", "，", ".", ";", "、"))
    if len(text) <= 40 and not has_end_punct and not text.endswith(("：", ":")):
        return True
    return False


def _looks_like_numbered_question(text: str) -> bool:
    if not _is_numbered_line(text):
        return False
    if _has_question_cue(text):
        return True
    if _title_like(text):
        return True
    return False


def _clean_heading(text: str) -> str:
    cleaned = text
    for pattern in _QUESTION_PREFIX_PATTERNS:
        cleaned = pattern.sub("", cleaned, count=1)
    return cleaned.strip()


def _emit_qa(
    qas: List[Dict],
    question: Optional[str],
    answer_lines: List[str],
    source: str,
    major: str,
    topic: str,
    subtopic: str,
) -> None:
    if not question:
        return
    answer = "\n".join([line for line in answer_lines if line.strip()])
    if not answer.strip():
        return
    qas.append(
        {
            "id": _stable_id(question, answer),
            "question": question.strip(),
            "answer": answer.strip(),
            "major": major,
            "topic": topic,
            "subtopic": subtopic,
            "source": source,
        }
    )

def parse_docx(path: str, major: str, topic: str, subtopic: str) -> List[Dict]:
    if DocxDocument is None:
        raise RuntimeError("python-docx not installed. Run: pip install python-docx")
    doc = DocxDocument(path)
    qas: List[Dict] = []
    current_q: Optional[str] = None
    answer_lines: List[str] = []
    prev_blank = True
    for para in doc.paragraphs:
        text = (para.text or "").strip()
        if not text:
            prev_blank = True
            continue
        style_name = para.style.name if para.style else ""
        is_heading = _is_heading(text, style_name, prev_blank)
        heading_by_style = "heading" in style_name.lower() or "title" in style_name.lower() or "标题" in style_name
        if (
            is_heading
            and not heading_by_style
            and _is_numbered_line(text)
            and current_q
            and answer_lines
            and not _has_question_cue(text)
            and not _title_like(text)
            and _ANSWER_LIST_CUE_PATTERN.search(text)
        ):
            is_heading = False
        if is_heading:
            _emit_qa(
                qas, current_q, answer_lines, os.path.basename(path), major, topic, subtopic
            )
            current_q = _clean_heading(text)
            answer_lines = []
        else:
            answer_lines.append(text)
        prev_blank = False
    _emit_qa(qas, current_q, answer_lines, os.path.basename(path), major, topic, subtopic)
    return qas


def _group_pdf_words(words: List[Dict]) -> List[Dict]:
    lines: Dict[float, List[Dict]] = {}
    for w in words:
        top = round(w.get("top", 0.0), 1)
        lines.setdefault(top, []).append(w)
    grouped = []
    for top, line_words in sorted(lines.items()):
        line_words = sorted(line_words, key=lambda x: x.get("x0", 0.0))
        text = " ".join(w.get("text", "") for w in line_words).strip()
        sizes = [w.get("size") for w in line_words if isinstance(w.get("size"), (int, float))]
        size = sorted(sizes)[len(sizes) // 2] if sizes else None
        fontnames = [w.get("fontname", "") for w in line_words]
        is_bold = any("bold" in (fn or "").lower() for fn in fontnames)
        grouped.append({"text": text, "size": size, "bold": is_bold})
    return grouped


def _pdf_heading(
    text: str,
    size: Optional[float],
    is_bold: bool,
    body_size: Optional[float],
    prev_blank: bool,
) -> Tuple[bool, bool]:
    if not text:
        return False, False
    if text.isdigit():
        return False, False
    if len(text) > 140:
        return False, False
    has_end_punct = any(p in text for p in ("。", "；", "，", ".", ";", "、"))
    if _looks_like_numbered_question(text):
        return True, False
    if text.endswith(("?", "？")) and len(text) <= 90:
        return True, False
    heading_by_font = False
    if body_size and size:
        # stricter font-based heading detection
        if size >= body_size + 2.5 and len(text) <= 50 and not has_end_punct:
            heading_by_font = True
        if is_bold and size >= body_size + 1.5 and len(text) <= 40 and not has_end_punct:
            heading_by_font = True
    if heading_by_font:
        return True, True
    # avoid relying on blank lines in PDF (too noisy)
    return False, False


def parse_pdf(path: str, major: str, topic: str, subtopic: str) -> List[Dict]:
    if pdfplumber is None:
        raise RuntimeError("pdfplumber not installed. Run: pip install pdfplumber")
    qas: List[Dict] = []
    current_q: Optional[str] = None
    answer_lines: List[str] = []
    prev_blank = True

    all_words: List[Dict] = []
    with pdfplumber.open(path) as pdf:
        for page in pdf.pages:
            words = page.extract_words(extra_attrs=["size", "fontname"])
            all_words.extend(words)

        sizes = [w.get("size") for w in all_words if isinstance(w.get("size"), (int, float))]
        body_size = sorted(sizes)[len(sizes) // 2] if sizes else None

        for page in pdf.pages:
            words = page.extract_words(extra_attrs=["size", "fontname"])
            for line in _group_pdf_words(words):
                text = (line.get("text") or "").strip()
                if not text:
                    prev_blank = True
                    continue
                is_heading, heading_by_font = _pdf_heading(
                    text, line.get("size"), line.get("bold", False), body_size, prev_blank
                )
                if (
                    is_heading
                    and _is_numbered_line(text)
                    and current_q
                    and answer_lines
                    and not _has_question_cue(text)
                    and not _title_like(text)
                    and not heading_by_font
                    and _ANSWER_LIST_CUE_PATTERN.search(text)
                ):
                    is_heading = False
                if is_heading:
                    _emit_qa(
                        qas,
                        current_q,
                        answer_lines,
                        os.path.basename(path),
                        major,
                        topic,
                        subtopic,
                    )
                    current_q = _clean_heading(text)
                    answer_lines = []
                else:
                    answer_lines.append(text)
                prev_blank = False

    _emit_qa(qas, current_q, answer_lines, os.path.basename(path), major, topic, subtopic)
    return qas


def iter_files(input_path: str) -> Iterable[str]:
    if os.path.isfile(input_path):
        yield input_path
        return
    for root, _, files in os.walk(input_path):
        for name in files:
            if name.startswith("~$") or name.startswith("."):
                continue
            lower = name.lower()
            if lower.endswith(".docx") or lower.endswith(".pdf"):
                yield os.path.join(root, name)


def infer_major_topic(file_path: str, input_root: str) -> Tuple[str, str]:
    if not input_root or not os.path.isdir(input_root):
        return "", ""
    try:
        rel = os.path.relpath(file_path, input_root)
    except ValueError:
        return "", ""
    parts = rel.split(os.sep)
    if len(parts) >= 3:
        return parts[0], parts[1]
    if len(parts) == 2:
        return parts[0], ""
    return "", ""


def convert(
    input_path: str, output_path: str, major: str, topic: str, subtopic: str
) -> None:
    total = 0
    written = 0
    with open(output_path, "w", encoding="utf-8") as out:
        for file_path in iter_files(input_path):
            lower = file_path.lower()
            auto_major, auto_topic = infer_major_topic(file_path, input_path)
            file_major = major or auto_major
            file_topic = topic or auto_topic
            if lower.endswith(".docx"):
                qas = parse_docx(file_path, file_major, file_topic, subtopic)
            elif lower.endswith(".pdf"):
                qas = parse_pdf(file_path, file_major, file_topic, subtopic)
            else:
                continue
            total += len(qas)
            for qa in qas:
                out.write(json.dumps(qa, ensure_ascii=False) + "\n")
                written += 1
            print(
                f"✅ 解析完成: {file_path} -> {len(qas)} 条 "
                f"(major={file_major}, topic={file_topic})"
            )
    print(f"🎉 转换完成！total={total}, written={written}, output={output_path}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Convert docx/pdf to QA JSONL.")
    parser.add_argument(
        "--input",
        default=os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "data_source"
        ),
        help="Input file or directory containing .docx/.pdf",
    )
    parser.add_argument(
        "--output",
        default=os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
            "data_source",
            "qa.jsonl",
        ),
        help="Output JSONL file",
    )
    parser.add_argument("--major", default="", help="Major category for all entries")
    parser.add_argument("--topic", default="", help="Topic for all entries")
    parser.add_argument("--subtopic", default="", help="Subtopic for all entries")
    args = parser.parse_args()
    convert(args.input, args.output, args.major, args.topic, args.subtopic)


if __name__ == "__main__":
    main()
