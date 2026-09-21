# computer_backend 语料模板

该目录用于沉淀“计算机后端岗位”知识库语料，可直接被 `scripts/build_faiss_index.py` 读取。

建议结构：

- `core_tech_stack.md`：核心技术栈与原理要点（长文本）
- `interview_points.sample.jsonl`：常见面试考点（结构化）
- `excellent_answers.sample.jsonl`：优秀回答范例（结构化）

建议字段（jsonl 每行一个 JSON）：

- `text` 或 `content`：最终入库文本（推荐）
- `question` + `answer`：若无 `text/content`，脚本会自动拼接
- `industry` / `role` / `topic` / `difficulty`：可选元数据
- `metadata`：可选对象，可放 `tags/source/version` 等

示例构建命令：

```bash
python3 scripts/build_faiss_index.py \
  --input-dir knowledge_corpus/computer_backend \
  --output-dir memory/knowledge_index/computer_backend \
  --industry computer_backend \
  --role backend
```

