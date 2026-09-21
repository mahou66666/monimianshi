# Docker 部署说明

## 1. 准备环境变量

```bash
cp .env.example .env
```

然后在 `.env` 中填写你的 `LLM_API_KEY`。

## 2. 构建并后台启动

```bash
docker compose up -d --build
```

## 3. 验证服务

```bash
docker compose ps
docker compose logs -f resume-cut
```

服务端口固定为 `8089`，外部访问地址示例：

```text
http://localhost:8089/api/resume/parse
```

## 4. 长期运行策略

- 已设置 `restart: always`，Docker 重启后容器会自动拉起。
- 容器内默认端口为 `8089`，并映射为主机 `8089:8089`。
