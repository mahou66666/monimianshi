# finance_risk 语料模板

该目录用于沉淀“金融风控岗位”知识库语料，可直接被 `scripts/build_faiss_index.py` 读取。

建议结构：

- `core_domain_knowledge.md`：风控核心知识体系（长文本）
- `interview_points.sample.jsonl`：常见面试考点（结构化）
- `excellent_answers.sample.jsonl`：优秀回答范例（结构化）

示例构建命令：

```bash
python3 scripts/build_faiss_index.py \
  --input-dir knowledge_corpus/finance_risk \
  --output-dir memory/knowledge_index/finance_risk \
  --industry finance_risk \
  --role risk_analyst
```
