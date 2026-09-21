# scripts/init_qdrant.py

import sys
import os

# 将当前目录加入 path 以便导入 app 配置（如果在项目根目录运行）
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from qdrant_client import QdrantClient
from qdrant_client.http.models import Distance, VectorParams
from app.core.config import settings
from app.utils.model_loader import get_embedding_model

def init_qdrant():
    qdrant_host = settings.QDRANT_HOST
    qdrant_port = settings.QDRANT_PORT
    collection_name = settings.COLLECTION_NAME
    vector_size = int(os.getenv("VECTOR_SIZE", "0"))
    if vector_size <= 0:
        try:
            embeddings = get_embedding_model()
            vector_size = len(embeddings.embed_query("dimension check"))
        except Exception:
            vector_size = 1024  # 回退默认值

    print(f"Connecting to Qdrant at {qdrant_host}:{qdrant_port}...")
    client = QdrantClient(host=qdrant_host, port=qdrant_port)

    # 检查集合是否存在
    try:
        client.get_collection(collection_name)
        print(f"✅ Collection '{collection_name}' already exists.")
    except Exception:
        print(f"⚠️ Collection '{collection_name}' not found. Creating...")
        
        client.create_collection(
            collection_name=collection_name,
            vectors_config=VectorParams(size=vector_size, distance=Distance.COSINE),
        )
        print(f"✅ Collection '{collection_name}' created successfully!")

if __name__ == "__main__":
    init_qdrant()
