# Python Agent 服务镜像（uv 管理依赖）
FROM python:3.12-slim

# 安装 uv（ astral 官方镜像拷贝二进制 ）
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app

ENV UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    PYTHONUNBUFFERED=1

# 先装依赖（利用构建缓存），再拷源码
COPY pyproject.toml ./
RUN uv sync --no-install-project --no-dev || uv sync --no-dev

COPY app ./app
COPY knowledge ./knowledge

# 会话记忆 SQLite 目录（可被 docker volume 挂载持久化）
RUN mkdir -p /app/db && uv sync --no-dev

EXPOSE 8000

CMD ["uv", "run", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
