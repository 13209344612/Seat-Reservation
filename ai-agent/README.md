# AI Agent（Python 主线）

校园自习室预约助手的 AI 编排服务，采用 **Python Agent 主线 + Java 业务底座** 模式：

- Python 负责：LLM 对话编排、Tool Calling、RAG 知识库检索、多轮记忆与历史、SSE 流式输出。
- Java 负责：用户/自习室/预约等业务与鉴权，通过 `/internal/**` 接口对 Agent 暴露能力。

## 技术栈

| 类别 | 技术 |
| --- | --- |
| Web 框架 | FastAPI + Uvicorn |
| AI 编排 | LangChain `create_agent` + LangGraph |
| 大模型 | 通义千问 `qwen-plus`（DashScope OpenAI 兼容接口） |
| 向量/RAG | `text-embedding-v3` + `InMemoryVectorStore` |
| 会话记忆 | LangGraph `SqliteSaver`（按 `thread_id` 持久化） |
| HTTP 调用 | httpx（携带 `X-Internal-Token` 调 Java 内部接口） |
| 鉴权 | PyJWT（与 Java 共用同一 `JWT_SECRET`，HS256） |
| 依赖管理 | uv（pyproject.toml） |

## 目录结构

```
ai-agent/
├── app/
│   ├── main.py                      # 应用入口（CORS、路由、启动加载知识库）
│   ├── core/config.py               # 环境变量配置
│   ├── common/logger.py             # 日志
│   ├── models/schemas.py            # 请求模型 ChatRequest
│   ├── security/jwt_auth.py         # JWT 校验依赖（解析 userId/role）
│   ├── tools/
│   │   ├── java_client.py           # Java /internal/** httpx 客户端
│   │   └── reservation_tools.py     # 5 个预约 @tool（userId 由服务端注入）
│   ├── rag/knowledge.py             # 知识库加载 + search_knowledge 工具
│   ├── agents/reservation_agent.py  # create_agent + SqliteSaver + 流式 + 历史
│   └── api/ai.py                    # /api/ai 路由（保持前端契约）
├── knowledge/                       # RAG 文档（预约规则/签到与取消）
├── db/                              # SQLite 会话库（运行后生成）
├── .env.example
├── Dockerfile
└── pyproject.toml
```

## 本地开发

```powershell
# 1. 安装 uv（若未安装）：https://docs.astral.sh/uv/
# 2. 安装依赖
cd ai-agent
uv sync

# 3. 配置环境变量
copy .env.example .env
# 编辑 .env：填写 DASHSCOPE_API_KEY、INTERNAL_API_TOKEN，
# 确保 JWT_SECRET 与 Java application.yml 的 jwt.secret 一致，
# JAVA_INTERNAL_BASE_URL 指向本地 Java（默认 http://localhost:8080）

# 4. 确保 db/ 目录存在
mkdir db

# 5. 启动
uv run python -m app.main
```

启动后：
- 服务地址：http://127.0.0.1:8000
- API 文档：http://127.0.0.1:8000/docs
- 健康检查：http://127.0.0.1:8000/health

## 端到端联调（三服务，本地开发）

整个系统由三类服务组成，本地开发按下列顺序启动（建议开三个终端）：

> 前置约定（已内置，无需再配环境变量）：
> - `jwt.secret`（Java）与 `.env` 的 `JWT_SECRET` 使用同一 Base64 串；
> - Java `application.yml` 的 `internal.api.token` 默认值与 `.env` 的 `INTERNAL_API_TOKEN` 已对齐，
>   两端一致即可直接联调（生产请用随机强密钥并以环境变量覆盖）。

### 1. 基础中间件（MySQL / Redis / RabbitMQ）
```powershell
# 在项目根目录
docker compose up -d mysql redis rabbitmq
```
> Java 通过 `localhost:3307` 连接 Docker 内的 MySQL（对应 `application.yml` 的 datasource）。

### 2. Java 业务底座（端口 8080）
在 IntelliJ IDEA 中直接运行 `SeatReservationApplication` 即可；无需额外环境变量。

### 3. Python Agent（端口 8000）
```powershell
cd ai-agent
uv run python -m app.main
```
看到日志 `AI Agent 就绪，监听端口 8000`（首次启动会拉取 DashScope 向量做 RAG，日志含 `[RAG] 知识库加载完成`）即成功。

### 4. 前端（端口 3000）
```powershell
cd frontend
npm run dev
```
`vite.config.js` 已配置代理：`/api/ai → http://localhost:8000`，其余 `/api → http://localhost:8080`。

### 5. 浏览器验证
打开 http://localhost:3000，用测试账号 `student1 / 123456` 登录，进入「AI 助手」页，逐项自测：
- 打字机式流式输出
- 「现在有哪些自习室还有空位？」→ 触发 `list_rooms` / `query_availability`
- 「帮我预约明天下午的座位」→ 触发 `create_reservation`（userId 服务端注入）
- 「我有哪些预约？」/「取消预约 xx」→ 触发 `list_my_reservations` / `cancel_reservation`
- 「预约后多久不签到会被取消？」→ 触发 `search_knowledge`（RAG 知识库）
- 左侧历史会话列表 / 切换 / 删除；退出重登验证会话按用户隔离

### 排错速查
| 现象 | 排查 |
| --- | --- |
| 前端对话报 401 / 登录过期 | Java 与 `.env` 的 JWT 密钥不一致（比较 `jwt.secret` 与 `JWT_SECRET`） |
| 工具调用失败（查询/预约） | Java 是否已启动；`.env` 的 `JAVA_INTERNAL_BASE_URL` 是否为 `http://localhost:8080`；两端 `INTERNAL_API_TOKEN` 是否一致 |
| RAG 无知识库增强 | `.env` 是否配置 `DASHSCOPE_API_KEY`；`ai-agent/knowledge/*.md` 是否存在 |
| 启动报无法打开数据库 | 确认 `ai-agent/db/` 目录存在 |

## 容器化部署（生产验证）
```powershell
# 在项目根目录，先设置主机环境变量再构建
docker compose up --build
```
- 需在主机环境提供 `AI_DASHSCOPE_API_KEY`（compose 透传给 `ai-agent` 的 `DASHSCOPE_API_KEY`）与 `INTERNAL_API_TOKEN`（Java/Agent 共用）。
- nginx 将 `/api/ai/` 反向代理到 `ai-agent:8000`，其余 `/api/` 到 `app:8080`；`ai-agent` 仅内网暴露 8000。
- 会话记忆持久化在数据卷 `ai_agent_db`（挂载到 `/app/db`）。

## 主要接口（与前端契约一致，均需 Bearer JWT）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /api/ai/chat/stream | 流式对话（SSE，打字机效果） |
| POST | /api/ai/chat | 同步对话（一次性返回） |
| GET | /api/ai/conversations | 当前用户历史会话列表 |
| GET | /api/ai/conversations/{id}/messages | 某会话的全部消息 |
| DELETE | /api/ai/conversations/{id} | 删除某会话及其消息 |

## 安全设计

- `userId` 只来自服务端校验后的 JWT，绝不接受客户端显式传入；工具通过 LangGraph
  运行时配置 `configurable.userId` 读取身份，模型无法伪造他人身份。
- 会话 `thread_id = userId:conversationId`，历史列表仅扫描当前用户前缀，天然隔离。
- 调用 Java 内部接口需携带 `X-Internal-Token`，与 Java `internal.api.token` 一致。
