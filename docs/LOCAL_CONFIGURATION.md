# 本地配置说明

此仓库保存前端重构前的源码基线，不包含开发者的本地凭据、数据库数据、模型下载和运行环境。

1. 将 `backend/admin-server/src/main/resources/application.properties.example` 复制为同目录的 `application.properties`。
2. 将 `springboot-front(4)/src/main/resources/application.properties.example` 复制为同目录的 `application.properties`。
3. 将各 Python、简历服务、前端目录的 `.env.example` 复制为对应的本地 `.env` 或 `.env.local`，填写自己的配置。
4. 使用本地 MySQL Docker 方案时，将根目录 `docker-compose.local-db.yml.example` 复制为 `docker-compose.local-db.yml` 并配置凭据。
5. 根目录的数据库导出未上传；需要自行提供已检查的数据初始化脚本或按 `DATABASE_TABLES.md` 建立开发库。`start-dev-local-db.bat` 引用的 `interview_agent.sql` 必须由使用者在本地准备。
6. FunASR 示例 TLS 私钥未上传；需要使用自己的证书与私钥。模型资源和依赖按各服务文档安装。

示例中的环境变量必须在启动前配置。请勿将本地真实配置、数据库备份或 `.baselines` 压缩包提交到远程仓库。

原后端独立仓库的 Git 元数据已在本地 `.baselines` 下保留。当前远程只记录统一项目的源码基线，不包含原后端仓库历史。
