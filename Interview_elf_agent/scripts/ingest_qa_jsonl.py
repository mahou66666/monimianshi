#!/usr/bin/env python3

# Interview_elf_agent/scripts/ingest_qa_jsonl.py

import argparse
import hashlib
import uuid
import json
import os
import sys
import time
from typing import Dict, List, Optional, Tuple

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from qdrant_client import QdrantClient
from qdrant_client.http import models

from app.core.config import settings
from app.utils.model_loader import get_embedding_model


def _stable_id(question: str, answer: str) -> str:
    raw = f"{question}\n{answer}".encode("utf-8")
    return hashlib.md5(raw).hexdigest()


def _point_id(doc_id: str) -> str:
    return str(uuid.uuid5(uuid.NAMESPACE_URL, doc_id))


def _normalize_record(rec: Dict) -> Optional[Tuple[str, str, Dict]]:
    question = str(rec.get("question", "")).strip()
    answer = str(rec.get("answer", "")).strip()
    if not question or not answer:
        return None
    doc_id = str(rec.get("id") or _stable_id(question, answer))
    text = f"Q: {question}\nA: {answer}"
    payload = {
        "page_content": text,
        "question": question,
        "answer": answer,
        "major": rec.get("major", ""),
        "topic": rec.get("topic", ""),
        "subtopic": rec.get("subtopic", ""),
        "keywords": rec.get("keywords", []),
        "source": rec.get("source", ""),
        "updated_at": rec.get("updated_at", ""),
        "type": "qa",
    }
    return doc_id, text, payload


def _ensure_collection(
    client: QdrantClient, collection_name: str, vector_size: int, recreate: bool
) -> None:
    if client.collection_exists(collection_name):
        info = client.get_collection(collection_name)
        vectors = info.config.params.vectors
        if isinstance(vectors, dict):
            existing_dim = next(iter(vectors.values())).size
        else:
            existing_dim = vectors.size
        if existing_dim != vector_size:
            if not recreate:
                raise RuntimeError(
                    f"Collection dim mismatch: existing={existing_dim}, new={vector_size}. "
                    "Use --recreate to rebuild."
                )
            print(f"♻️  Recreating collection: {collection_name}")
            client.delete_collection(collection_name)
        else:
            return

    client.create_collection(
        collection_name=collection_name,
        vectors_config=models.VectorParams(size=vector_size, distance=models.Distance.COSINE),
    )

    # Create payload indexes for faster filtering
    for field_name in ("major", "topic", "subtopic", "source", "type"):
        try:
            client.create_payload_index(
                collection_name=collection_name,
                field_name=field_name,
                field_schema=models.PayloadSchemaType.KEYWORD,
            )
        except Exception:
            # Index may already exist; safe to ignore
            pass


def ingest_jsonl(path: str, batch_size: int, recreate: bool, max_rows: int) -> None:
    embeddings = get_embedding_model()
    print(f"🔌 连接 Qdrant ({settings.QDRANT_HOST}:{settings.QDRANT_PORT})...")
    client = QdrantClient(host=settings.QDRANT_HOST, port=settings.QDRANT_PORT)

    if not os.path.exists(path):
        raise FileNotFoundError(f"JSONL file not found: {path}")

    print("🔍 探测 embedding 维度...")
    sample_vec = embeddings.embed_query("dimension check")
    vector_size = len(sample_vec)
    _ensure_collection(client, settings.COLLECTION_NAME, vector_size, recreate)

    total = 0
    inserted = 0
    skipped = 0
    batch_ids: List[str] = []
    batch_texts: List[str] = []
    batch_payloads: List[Dict] = []

    start = time.time()
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            if max_rows > 0 and total >= max_rows:
                break
            total += 1
            line = line.strip()
            if not line:
                continue
            try:
                rec = json.loads(line)
            except json.JSONDecodeError:
                skipped += 1
                continue

            normalized = _normalize_record(rec)
            if not normalized:
                skipped += 1
                continue

            doc_id, text, payload = normalized
            batch_ids.append(_point_id(doc_id))
            batch_texts.append(text)
            batch_payloads.append(payload)

            if len(batch_ids) >= batch_size:
                vectors = embeddings.embed_documents(batch_texts)
                points = [
                    models.PointStruct(id=batch_ids[i], vector=vectors[i], payload=batch_payloads[i])
                    for i in range(len(batch_ids))
                ]
                client.upsert(collection_name=settings.COLLECTION_NAME, points=points)
                inserted += len(batch_ids)
                print(f"  -> 进度: {inserted}/{total} (skipped {skipped})")
                batch_ids.clear()
                batch_texts.clear()
                batch_payloads.clear()

    if batch_ids:
        vectors = embeddings.embed_documents(batch_texts)
        points = [
            models.PointStruct(id=batch_ids[i], vector=vectors[i], payload=batch_payloads[i])
            for i in range(len(batch_ids))
        ]
        client.upsert(collection_name=settings.COLLECTION_NAME, points=points)
        inserted += len(batch_ids)

    cost = time.time() - start
    print(
        f"✅ 入库完成！total={total}, inserted={inserted}, skipped={skipped}, "
        f"time={cost:.1f}s"
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Ingest QA JSONL into Qdrant.")
    parser.add_argument(
        "--path",
        default=os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
            "data_source",
            "qa.jsonl",
        ),
        help="Path to QA JSONL file.",
    )
    parser.add_argument("--batch-size", type=int, default=64, help="Embedding batch size.")
    parser.add_argument(
        "--recreate", action="store_true", help="Recreate collection if exists."
    )
    parser.add_argument(
        "--max-rows", type=int, default=0, help="Limit number of rows for testing."
    )
    args = parser.parse_args()
    ingest_jsonl(args.path, args.batch_size, args.recreate, args.max_rows)


if __name__ == "__main__":
    main()
