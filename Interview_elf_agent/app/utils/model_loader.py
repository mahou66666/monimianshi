# Interview_elf_agent/app/utils/model_loader.py

import os
import torch
from huggingface_hub import snapshot_download
from langchain_huggingface import HuggingFaceEmbeddings
from app.core.config import settings

def get_embedding_model():
    """
    自动获取 Embedding 模型实例。
    1. 检查本地 models 目录是否存在模型文件。
    2. 若不存在，自动从 HuggingFace 镜像站下载。
    3. 根据硬件 (Mac MPS / CUDA / CPU) 自动选择设备。
    """
    # 1. 确定本地模型路径
    if settings.EMBEDDING_MODEL_PATH:
        local_model_path = settings.EMBEDDING_MODEL_PATH
    else:
        # 将模型名中的斜杠替换为下划线，防止路径错误
        model_folder_name = settings.EMBEDDING_MODEL_NAME.replace("/", "_")
        local_model_path = os.path.join(settings.MODELS_DIR, model_folder_name)

    # 设置 Hugging Face 的镜像与缓存路径
    if settings.HF_ENDPOINT and not os.getenv("HUGGINGFACE_HUB_ENDPOINT"):
        os.environ["HUGGINGFACE_HUB_ENDPOINT"] = settings.HF_ENDPOINT
    if settings.HF_HOME and not os.getenv("HF_HOME"):
        os.environ["HF_HOME"] = settings.HF_HOME

    # 2. 检查并自动下载
    if not os.path.exists(local_model_path):
        print(f"⚠️  本地模型未找到: {local_model_path}")
        if settings.HF_HUB_OFFLINE:
            raise RuntimeError("❌ 离线模式已启用，但本地未找到 Embedding 模型。")
        print(f"⬇️  开始自动下载模型: {settings.EMBEDDING_MODEL_NAME} ...")
        
        # 确保目录存在
        os.makedirs(os.path.dirname(local_model_path), exist_ok=True)
        
        try:
            snapshot_download(
                repo_id=settings.EMBEDDING_MODEL_NAME,
                local_dir=local_model_path,
                local_dir_use_symlinks=False,  # 下载实体文件
                ignore_patterns=["*.msgpack", "*.h5", "*.ot", "flax*", "tf*", "rust*"],
                endpoint=settings.HF_ENDPOINT or None,
                cache_dir=settings.HF_CACHE_DIR or None,
                local_files_only=settings.HF_HUB_OFFLINE,
            )
            print("✅ 模型下载完成！")
        except Exception as e:
            raise RuntimeError(f"❌ 模型下载失败: {str(e)}")
    else:
        print(f"✅ 检测到本地模型: {local_model_path}")

    # 3. 智能选择设备
    device = "cpu"
    if torch.cuda.is_available():
        device = "cuda"
    elif torch.backends.mps.is_available():
        device = "mps"
    
    print(f"🚀 加载模型中 (Device: {device})...")

    # 4. 加载模型
    embeddings = HuggingFaceEmbeddings(
        model_name=local_model_path,
        model_kwargs={'device': device}
    )
    
    return embeddings
