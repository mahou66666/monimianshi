#!/usr/bin/env python3
"""Build a local FAISS index from a corpus directory.

Supported input file types:
- .md
- .txt
- .jsonl

This script writes `index.faiss` and `index.pkl` into --output-dir.
"""

from __future__ import annotations

import argparse
import json
import os
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from dotenv import load_dotenv
from langchain_community.vectorstores.faiss import FAISS
from langchain_core.documents import Document


TEXT_FIELD_CANDIDATES = (
    "text",
    "content",
    "body",
    "chunk",
    "passage",
    "answer",
)


@dataclass
class BuildStats:
    files_scanned: int = 0
    raw_docs: int = 0
    chunks: int = 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Build FAISS index from local files.")
    parser.add_argument("--input-dir", required=True, help="Corpus root directory.")
    parser.add_argument("--output-dir", required=True, help="Target index directory.")
    parser.add_argument(
        "--extensions",
        default="md,txt,jsonl",
        help="Comma-separated file extensions to ingest.",
    )
    parser.add_argument("--chunk-size", type=int, default=800, help="Chunk size in characters.")
    parser.add_argument("--chunk-overlap", type=int, default=120, help="Chunk overlap in characters.")
    parser.add_argument("--min-chunk-chars", type=int, default=80, help="Drop chunks shorter than this value.")
    parser.add_argument("--industry", default="", help="Default metadata.industry for all docs.")
    parser.add_argument("--role", default="", help="Default metadata.role for all docs.")
    parser.add_argument("--topic", default="", help="Default metadata.topic for all docs.")
    parser.add_argument(
        "--prepend-tag-fields",
        default="industry,role,topic",
        help="Comma-separated metadata fields to prepend as [key=value] tags.",
    )
    parser.add_argument("--append", action="store_true", help="Append to existing index if present.")
    parser.add_argument("--dry-run", action="store_true", help="Parse and chunk only, do not call embeddings.")
    parser.add_argument("--verbose", action="store_true", help="Print per-file ingest details.")
    return parser.parse_args()


def parse_extensions(raw: str) -> set[str]:
    values = {x.strip().lower().lstrip(".") for x in raw.split(",") if x.strip()}
    return values or {"md", "txt", "jsonl"}


def iter_files(input_dir: Path, allowed_ext: set[str]) -> Iterable[Path]:
    for path in sorted(input_dir.rglob("*")):
        if not path.is_file():
            continue
        suffix = path.suffix.lower().lstrip(".")
        if suffix in allowed_ext:
            yield path


def normalize_text(text: str) -> str:
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    return text.strip()


def extract_text_and_meta_from_json_obj(obj: object) -> tuple[str, dict]:
    if isinstance(obj, str):
        return normalize_text(obj), {}
    if not isinstance(obj, dict):
        return "", {}

    metadata = {}
    raw_meta = obj.get("metadata")
    if isinstance(raw_meta, dict):
        metadata.update(raw_meta)

    for key in ("industry", "role", "topic", "source", "id", "difficulty"):
        value = obj.get(key)
        if value not in (None, ""):
            metadata[key] = value

    text = ""
    for key in TEXT_FIELD_CANDIDATES:
        value = obj.get(key)
        if isinstance(value, str) and value.strip():
            text = value
            break

    if not text:
        question = str(obj.get("question", "")).strip()
        answer = str(obj.get("answer", "")).strip()
        if question or answer:
            text = f"Question: {question}\nAnswer: {answer}".strip()

    return normalize_text(text), metadata


def load_docs_from_file(path: Path, input_dir: Path, verbose: bool = False) -> list[Document]:
    docs: list[Document] = []
    rel_path = str(path.relative_to(input_dir))
    ext = path.suffix.lower().lstrip(".")

    if ext == "jsonl":
        with path.open("r", encoding="utf-8") as f:
            for line_no, line in enumerate(f, start=1):
                line = line.strip()
                if not line:
                    continue
                try:
                    obj = json.loads(line)
                except json.JSONDecodeError as exc:
                    raise ValueError(f"Invalid JSONL at {rel_path}:{line_no}: {exc}") from exc
                text, metadata = extract_text_and_meta_from_json_obj(obj)
                if not text:
                    continue
                merged = {
                    "source_file": rel_path,
                    "source_line": line_no,
                    "file_ext": ext,
                    **metadata,
                }
                docs.append(Document(page_content=text, metadata=merged))
    else:
        text = normalize_text(path.read_text(encoding="utf-8"))
        if text:
            docs.append(
                Document(
                    page_content=text,
                    metadata={
                        "source_file": rel_path,
                        "file_ext": ext,
                    },
                )
            )

    if verbose:
        print(f"[ingest] {rel_path} -> {len(docs)} doc(s)")
    return docs


def split_document(
    doc: Document,
    *,
    chunk_size: int,
    chunk_overlap: int,
    min_chunk_chars: int,
    prepend_fields: list[str],
) -> list[Document]:
    text = doc.page_content
    if not text:
        return []

    step = chunk_size - chunk_overlap
    chunks: list[Document] = []
    start = 0
    idx = 0

    tag_tokens = []
    for key in prepend_fields:
        value = doc.metadata.get(key)
        if value in (None, ""):
            continue
        normalized = str(value).strip().replace("\n", " ")
        if normalized:
            tag_tokens.append(f"[{key}={normalized}]")
    tag_prefix = (" ".join(tag_tokens) + "\n") if tag_tokens else ""

    while start < len(text):
        end = min(len(text), start + chunk_size)
        chunk_text = text[start:end].strip()
        include_short_single_chunk = idx == 0 and end >= len(text)
        if len(chunk_text) >= min_chunk_chars or (include_short_single_chunk and len(chunk_text) > 0):
            meta = dict(doc.metadata)
            meta.update(
                {
                    "chunk_index": idx,
                    "chunk_start": start,
                    "chunk_end": end,
                }
            )
            chunks.append(Document(page_content=f"{tag_prefix}{chunk_text}" if tag_prefix else chunk_text, metadata=meta))
            idx += 1
        if end >= len(text):
            break
        start += step

    return chunks


def create_embeddings_from_env():
    from langchain_openai import OpenAIEmbeddings

    siliconflow_api_key = os.getenv("SILICONFLOW_API_KEY", "").strip()
    openai_api_key = os.getenv("OPENAI_API_KEY", "").strip()
    siliconflow_base_url = os.getenv("SILICONFLOW_BASE_URL", "https://api.siliconflow.cn/v1").strip()
    openai_base_url = os.getenv("OPENAI_BASE_URL", "").strip()

    if siliconflow_api_key:
        provider = "siliconflow"
        api_key = siliconflow_api_key
        base_url = siliconflow_base_url
        default_embedding_model = "BAAI/bge-m3"
    elif openai_api_key:
        provider = "openai"
        api_key = openai_api_key
        base_url = openai_base_url or None
        default_embedding_model = "text-embedding-3-large"
    else:
        raise RuntimeError("Missing API key. Set SILICONFLOW_API_KEY or OPENAI_API_KEY first.")

    embedding_model = os.getenv("EMBEDDING_MODEL", default_embedding_model).strip() or default_embedding_model
    kwargs = {
        "model": embedding_model,
        "api_key": api_key,
    }
    if base_url:
        kwargs["base_url"] = base_url

    return OpenAIEmbeddings(**kwargs), provider, embedding_model


def ensure_valid_args(args: argparse.Namespace) -> None:
    if args.chunk_size <= 0:
        raise ValueError("--chunk-size must be > 0")
    if args.chunk_overlap < 0:
        raise ValueError("--chunk-overlap must be >= 0")
    if args.chunk_overlap >= args.chunk_size:
        raise ValueError("--chunk-overlap must be smaller than --chunk-size")
    if args.min_chunk_chars <= 0:
        raise ValueError("--min-chunk-chars must be > 0")


def apply_default_metadata(doc: Document, args: argparse.Namespace) -> None:
    if args.industry and not doc.metadata.get("industry"):
        doc.metadata["industry"] = args.industry
    if args.role and not doc.metadata.get("role"):
        doc.metadata["role"] = args.role
    if args.topic and not doc.metadata.get("topic"):
        doc.metadata["topic"] = args.topic


def main() -> int:
    load_dotenv()
    args = parse_args()
    ensure_valid_args(args)

    input_dir = Path(args.input_dir).expanduser().resolve()
    output_dir = Path(args.output_dir).expanduser().resolve()
    allowed_ext = parse_extensions(args.extensions)
    prepend_fields = [x.strip() for x in args.prepend_tag_fields.split(",") if x.strip()]

    if not input_dir.exists() or not input_dir.is_dir():
        raise FileNotFoundError(f"Input directory does not exist or is not a directory: {input_dir}")

    raw_docs: list[Document] = []
    stats = BuildStats()

    for path in iter_files(input_dir, allowed_ext):
        stats.files_scanned += 1
        file_docs = load_docs_from_file(path, input_dir, verbose=args.verbose)
        for doc in file_docs:
            apply_default_metadata(doc, args)
        raw_docs.extend(file_docs)

    stats.raw_docs = len(raw_docs)
    if not raw_docs:
        raise RuntimeError(f"No documents ingested from {input_dir} with extensions: {sorted(allowed_ext)}")

    chunked_docs: list[Document] = []
    for doc in raw_docs:
        chunked_docs.extend(
            split_document(
                doc,
                chunk_size=args.chunk_size,
                chunk_overlap=args.chunk_overlap,
                min_chunk_chars=args.min_chunk_chars,
                prepend_fields=prepend_fields,
            )
        )
    stats.chunks = len(chunked_docs)

    if not chunked_docs:
        raise RuntimeError("No chunks produced after split. Check chunk params or input text.")

    print(
        "[summary] files_scanned=%d raw_docs=%d chunks=%d output_dir=%s"
        % (stats.files_scanned, stats.raw_docs, stats.chunks, output_dir)
    )
    if args.dry_run:
        print("[dry-run] Done. No embeddings call made.")
        return 0

    embeddings, provider, embedding_model = create_embeddings_from_env()
    output_dir.mkdir(parents=True, exist_ok=True)
    index_file = output_dir / "index.faiss"
    pkl_file = output_dir / "index.pkl"

    if args.append and index_file.exists() and pkl_file.exists():
        print(f"[index] appending to existing index: {output_dir}")
        store = FAISS.load_local(
            str(output_dir),
            embeddings,
            allow_dangerous_deserialization=True,
        )
        store.add_documents(chunked_docs)
    else:
        print(f"[index] creating new index: {output_dir}")
        store = FAISS.from_documents(chunked_docs, embeddings)

    store.save_local(str(output_dir))
    print(
        "[done] provider=%s embedding_model=%s index=%s"
        % (provider, embedding_model, output_dir)
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
