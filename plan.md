# Java + Spring AI 智能体平台详细实施计划

## Summary

基于当前 `spring-ai-learn` 项目，将现有基础 chat API 演进为“个人多用户 AI Agent 平台”。后端使用 Java 17 + Spring Boot + Spring AI，核心能力包括用户注册登录、Redis token 管理、AI 调用额度、会话记忆、RAG 知识库、多 Agent 编排、Docker 沙箱、上下文压缩、召回优化、审计与可观测性。

前端采用 Next.js + React + TypeScript + shadcn/ui + Tailwind CSS。Next.js 只作为 UI 应用，不承载核心业务逻辑；鉴权、AI 编排、RAG、Agent、沙箱等可信能力仍由 Java 后端提供。

## Architecture

- 后端模块保持现状：`common` 放通用响应、异常、枚举；`spring-ai` 放业务能力。
- `spring-ai` 内部按包拆分：`auth`、`user`、`chat`、`memory`、`rag`、`agent`、`tool`、`sandbox`、`quota`、`observe`、`config`。
- PostgreSQL 存业务数据、会话、知识库、Agent trace；pgvector 存文档 chunk embedding；Redis 存 refresh token、登录状态、token 黑名单、限流计数、热点缓存；Docker 承载沙箱任务。
- 后端接入 `springdoc-openapi` 和 `swagger-ui`，用于暴露 REST API 文档、鉴权方式说明和接口调试入口。
- AI 层先沿用当前智谱 AI，增加 `ModelProvider` 抽象，后续支持 OpenAI、Ollama、本地模型。
- 前端新增 `web/` 应用，使用 Next.js App Router、TypeScript、Tailwind CSS、shadcn/ui、TanStack Query、Zustand，通过 REST + SSE 访问 Java 后端。

## Authentication And User System

- 用户范围按“个人多用户”设计，不做团队/组织/租户角色体系，但所有业务数据必须绑定 `userId`。
- access token 使用短有效期 JWT，前端请求时放入 `Authorization: Bearer <token>`。
- refresh token 生成随机不可预测 token，只存 Redis，不长期落库；Redis key 设置过期时间。
- Redis key 约定：`auth:refresh:{userId}:{tokenId}`，value 存 token hash、设备信息、创建时间。
- logout 删除当前 refresh token；logout-all 删除该用户所有 refresh token。
- 可选增加 `auth:blacklist:access:{jti}`，用于 access token 主动失效，TTL 等于 JWT 剩余有效期。
- 密码使用 BCrypt；接口使用 Spring Security 保护；登录、注册、refresh、静态资源放行。

## AI Core Implementation

- `ChatRequest` 增加 `conversationId`、`message`、`mode`、`knowledgeBaseId`、`agentId`、`metadata`，`mode` 支持 `CHAT`、`RAG`、`AGENT`。
- 会话记忆使用 `conversations`、`messages`、`conversation_summaries`，最近 N 轮消息直接进入上下文，超出窗口后由 `SummaryAgent` 生成摘要。
- RAG 流程：上传文档 -> 解析 -> 清洗 -> chunk 切分 -> embedding -> pgvector 入库；查询时执行 query rewrite -> vector search -> metadata filter -> rerank -> prompt stuffing -> LLM answer。
- RAG 回答必须返回引用来源：文档名、chunk id、相似度、片段、页码或段落号。
- 多 Agent 使用统一 `AgentRuntime`、`AgentContext`、`AgentStep`，内置 `RouterAgent`、`RetrieverAgent`、`PlannerAgent`、`ToolAgent`、`CriticAgent`、`SummaryAgent`。
- 默认 Agent 链路：Router -> Retriever/Planner -> Tool -> Critic -> Final，每一步落库到 `agent_runs`、`agent_steps`、`tool_calls`。
- 沙箱通过 Docker 创建受限执行环境，限制 CPU、内存、运行时间、网络、挂载目录，只允许白名单工具和参数 schema。

## Public APIs

- 用户认证：`POST /api/auth/register`、`POST /api/auth/login`、`POST /api/auth/refresh`、`POST /api/auth/logout`、`POST /api/auth/logout-all`、`GET /api/users/me`、`PUT /api/users/me`。
- AI 对话：`POST /api/ai/chat`、`POST /api/ai/chat/stream`、`POST /api/ai/rag/chat`、`POST /api/ai/agent/run`、`GET /api/ai/conversations`、`GET /api/ai/conversations/{conversationId}`、`DELETE /api/ai/conversations/{conversationId}`。
- 知识库：`POST /api/knowledge-bases`、`GET /api/knowledge-bases`、`PUT /api/knowledge-bases/{id}`、`DELETE /api/knowledge-bases/{id}`、`POST /api/knowledge-bases/{id}/documents`、`GET /api/knowledge-bases/{id}/documents`、`DELETE /api/documents/{id}`、`POST /api/knowledge-bases/{id}/retrieval-test`。
- 额度：`GET /api/usage/me`、`GET /api/usage/me/daily`。
- 沙箱：`POST /api/sandbox/jobs`、`GET /api/sandbox/jobs/{jobId}`。
- 文档：`/swagger-ui.html` 或 `/swagger-ui/index.html`，以及 `/v3/api-docs`。

## Frontend Plan

- 技术选型：Next.js App Router + React + TypeScript + shadcn/ui + Tailwind CSS + lucide-react + TanStack Query + Zustand。
- 页面规划：`/login`、`/register`、`/chat`、`/knowledge`、`/retrieval-test`、`/agents`、`/sandbox`、`/usage`、`/settings`。
- token 策略：access token 优先放内存状态，页面刷新后通过 refresh 接口换新；refresh token 推荐后端通过 HttpOnly Cookie 返回。
- Next.js 中间件只做页面访问保护，不做核心鉴权判断；后端仍以 Spring Security 为准。
- UI 以工作台为主，避免 landing page；Chat 页面左侧会话列表，中间消息流，右侧展示引用、Agent trace 或参数。

## Phased Implementation

- 第 0 批：写入 `plan.md`，整理 profile 配置，确认 Spring AI 是否升级到当前稳定版本。
- 第 1 批：创建 `web/` 前端应用骨架，完成 Next.js + TypeScript + Tailwind CSS + shadcn/ui 初始化；后端完成用户注册登录、Spring Security、JWT access token、Redis refresh token、logout/logout-all，并接入 `springdoc-openapi + swagger-ui`。
- 第 2 批：额度和审计，记录 chat token、embedding token、请求次数、文档容量、沙箱次数，并实现超额拒绝。
- 第 3 批：会话模型、消息落库、Chat Memory、上下文摘要、SSE streaming。
- 第 4 批：知识库上传、文档解析、chunk、embedding、pgvector 检索、RAG 回答和引用。
- 第 5 批：RAG 召回优化，加入 query rewrite、threshold、MMR、混合检索、retrieval-test。
- 第 6 批：多 Agent runtime，完成 Router、Retriever、Planner、Tool、Critic、Summary。
- 第 7 批：Docker 沙箱，完成工具白名单、资源限制、任务审计。
- 第 8 批：完善 Next.js 前端业务页面，完成登录、chat、知识库、召回测试、Agent trace、usage 页面。
- 第 9 批：生产化，完善 OpenAPI 分组与鉴权文档、Micrometer、traceId、Testcontainers、备份和索引重建。

## Test Plan

- 单元测试：JWT 生成校验、Redis refresh token 过期/撤销、密码校验、额度扣减、上下文压缩触发、RAG 参数校验。
- 集成测试：Spring Security 鉴权、Redis token 刷新、PostgreSQL + pgvector 写入检索、Chat Memory 持久化、知识库上传到召回。
- API 测试：未登录拒绝、跨用户数据隔离、额度超限、普通 chat、RAG chat、stream chat、Agent run。
- 沙箱测试：超时、内存限制、非法命令、无网络、输出截断、审计落库。
- 前端测试：登录刷新 token、会话 streaming、上传文档、召回测试、查看 Agent trace、usage dashboard。
- RAG 质量测试：标准问题集、期望 chunk 命中、引用覆盖率、回答事实一致性。

## Assumptions

- 用户体系采用个人多用户，不做组织/团队。
- refresh token 存 Redis 并设置 TTL，数据库不保存 refresh token。
- access token 使用短期 JWT。
- 前端采用 Next.js + React + TypeScript + shadcn/ui + Tailwind CSS。
- 后端是唯一业务和鉴权可信源。
- 向量库默认 PostgreSQL + pgvector。
- 沙箱默认 Docker 隔离。
- 智谱 AI 继续作为默认模型供应商。
