import os
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI, OpenAIEmbeddings

load_dotenv()

SILICONFLOW_BASE_URL = os.getenv("SILICONFLOW_BASE_URL", "https://api.siliconflow.cn/v1")
SILICONFLOW_API_KEY = os.getenv("SILICONFLOW_API_KEY")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")

if SILICONFLOW_API_KEY:
    PROVIDER = "siliconflow"
    API_KEY = SILICONFLOW_API_KEY
    BASE_URL = SILICONFLOW_BASE_URL
    DEFAULT_REASONING_MODEL = "deepseek-ai/DeepSeek-V4-Flash"
    DEFAULT_CHAT_MODEL = "deepseek-ai/DeepSeek-V4-Flash"
    DEFAULT_EMBEDDING_MODEL = "BAAI/bge-m3"
elif OPENAI_API_KEY:
    PROVIDER = "openai"
    API_KEY = OPENAI_API_KEY
    BASE_URL = os.getenv("OPENAI_BASE_URL", "").strip() or None
    DEFAULT_REASONING_MODEL = "gpt-4o-mini"
    DEFAULT_CHAT_MODEL = "gpt-4o-mini"
    DEFAULT_EMBEDDING_MODEL = "text-embedding-3-large"
else:
    raise RuntimeError("缺少 API Key，请在 .env 或环境变量中配置 SILICONFLOW_API_KEY 或 OPENAI_API_KEY 后再启动。")

REASONING_MODEL = os.getenv("REASONING_MODEL", DEFAULT_REASONING_MODEL)
CHAT_MODEL = os.getenv("CHAT_MODEL", DEFAULT_CHAT_MODEL)
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", DEFAULT_EMBEDDING_MODEL)

COMMON_CHAT_KWARGS = {
    "api_key": API_KEY,
    "max_retries": 2,
    "timeout": 30,
}
if BASE_URL:
    COMMON_CHAT_KWARGS["base_url"] = BASE_URL

# 复杂推理模型 (负责生成评估、深度专业问题)
reasoning_llm = ChatOpenAI(
    model=REASONING_MODEL,
    **COMMON_CHAT_KWARGS,
    temperature=0.7,
    max_tokens=2048,
)

# 快速交互模型 (负责控场、过渡)
chat_llm = ChatOpenAI(
    model=CHAT_MODEL,
    **COMMON_CHAT_KWARGS,
    temperature=0.6,
)

# 向量嵌入模型 (用于 FAISS 知识检索)
embedding_kwargs = {
    "model": EMBEDDING_MODEL,
    "api_key": API_KEY,
}
if BASE_URL:
    embedding_kwargs["base_url"] = BASE_URL

embeddings_model = OpenAIEmbeddings(**embedding_kwargs)
