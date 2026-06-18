# API Notification Service

这是一个面向 AI Coding 面试题的最小可行实现：内部业务系统把“需要通知外部供应商 API”的请求提交给本服务，本服务负责异步投递、失败重试和状态查询。

## 问题理解

业务系统的核心诉求不是同步拿到外部 API 的业务结果，而是把通知稳定送到不同供应商的 HTTP(S) 地址。供应商之间 URL、Header、Body 格式不同，所以服务需要支持通用 HTTP 投递能力，并把重试、失败记录、最终失败状态收敛在一个内部服务里。

本实现选择先做一个 MVP，重点展示可靠投递的工程骨架，而不是一次性做成完整生产平台。

## 系统边界

本系统解决：

- 接收内部系统提交的 HTTP 通知任务。
- 保存任务状态，并暴露查询接口。
- 按至少一次语义投递到目标 HTTP(S) 地址。
- 对网络异常和非200 响应进行指数退避重试。
- 超过最大投递次数后进入 `DEAD` 状态，保留最后错误信息，并输出结构化 WARN 日志（含 id、URL、method、attempts、lastStatus、error），便于运维通过日志排查，无需依赖持久化存储。

本系统暂不解决：

- 不保证恰好一次。HTTP 通知在超时、连接中断、服务重启等场景下天然可能重复投递，恰好一次需要供应商侧幂等键或业务去重配合。
- 不做供应商模板管理。MVP 由调用方直接传入目标 URL、Header、Body，避免先设计复杂配置中心。
- 不做持久化。当前使用内存存储，进程重启会丢任务。生产版应替换为数据库或消息队列。
- 不做租户鉴权、限流、审计和加密脱敏。这些是生产必需能力，但会扩大第一版复杂度。

## 核心设计

状态流转：

```text
PENDING -> SENDING -> SUCCEEDED
PENDING -> SENDING -> RETRYING -> SENDING -> SUCCEEDED
PENDING -> SENDING -> RETRYING -> SENDING -> DEAD
```

主要组件：

- `NotificationController`：提供提交和查询接口。
- `NotificationService`：校验请求，创建通知任务。
- `NotificationStore`：内存任务仓库，便于 MVP 演示和测试。
- `NotificationDispatcher`：定时扫描到期任务，使用 Spring `RestClient` 投递。

投递策略：

- 投递语义：至少一次。
- 成功条件：外部 API 返回 2xx。
- 失败条件：网络异常、超时、4xx、5xx。
- 重试策略：指数退避，1s、2s、4s、8s...最大 60s。
- 默认最大尝试次数：5，可在请求中通过 `maxAttempts` 设置，硬上限为 10。

## 关键取舍

第一版没有引入 MQ、数据库、规则引擎、供应商模板 DSL 或工作流引擎。优先把可靠投递的核心闭环跑通。

关于 DEAD 任务的排查：更完整的方案是引入数据库日志表持久化失败记录，支持按时间、供应商、错误类型检索。但 MVP 阶段选择了折中——在任务进入 DEAD 时输出结构化 WARN 日志，运维可通过 `grep` 定位到完整现场（URL、method、attempts、statusCode、error），不引入新依赖，也能满足第一版的排查需求。

如果流量或复杂度增长，我会按以下顺序演进：

1. 用数据库持久化任务，并将 DEAD 记录写入独立日志表，支持按供应商、时间范围检索和人工重放。
2. 引入消息队列，将接收请求和投递执行解耦，提高削峰和水平扩展能力。
3. 增加供应商配置表，管理 URL、认证、Header 模板、Body 模板和签名算法。
4. 增加幂等键、请求签名、租户鉴权、限流、审计日志和敏感字段脱敏。
5. 增加死信队列、运维后台和告警指标，例如成功率、重试次数、积压量、供应商失败率。


## AI 使用说明

### AI 提供的帮助

- 帮助拆解题目：识别出核心不是"同步调用供应商 API"，而是内部通知任务的接收、异步投递、重试和状态管理。
- 帮助形成 MVP 范围：先实现内存任务仓库、定时投递器、至少一次语义和状态查询，避免把第一版做成过重的平台。
- 帮助检查实现细节：第一次编译发现 Spring Boot 4 工程没有暴露传统 `com.fasterxml.jackson` 包后，调整为对请求体使用 `Object` 透传，降低对具体 JSON 包名的耦合。
- 帮助补充文档结构：围绕系统边界、失败处理、工程取舍和未来演进组织 README。

### 没有采纳的 AI 建议

- 没有直接引入 Kafka/RabbitMQ。消息队列适合生产演进，但对于面试 MVP 会让本地运行和评审成本变高。
- 没有设计供应商模板 DSL。不同供应商的格式差异是真问题，但第一版由调用方直接传 URL、Header、Body，更符合最小闭环。
- 没有追求"恰好一次"。HTTP 通知无法仅靠通知服务单方面保证恰好一次，应该通过至少一次投递 + 下游幂等键协同解决。
- 没有加入复杂权限、审计、告警后台。它们属于生产化增强，不是当前最小实现的主线。

### 自己做出的关键决策

- 选择至少一次投递语义。原因是业务系统不关心同步返回值，但希望通知尽可能送达；至少一次比最多一次更符合可靠通知场景。
- 选择非 2xx 也重试。原因是供应商临时故障、限流、网关错误都可能表现为 HTTP 错误码，统一进入重试更稳妥。
- 选择内存实现能清楚展示状态机和重试策略。
- 选择给最大重试次数设置硬上限。原因是避免调用方误配置造成无限重试和资源挤占。

## API

提交通知：

```http
POST /notifications
Content-Type: application/json

{
  "targetUrl": "https://vendor.example/events",
  "method": "POST",
  "headers": {
    "Authorization": "Bearer token",
    "Content-Type": "application/json"
  },
  "body": {
    "event": "subscription_paid",
    "userId": "u_123"
  },
  "maxAttempts": 5
}
```

响应：

```json
{
  "id": "8dbb0da0-b20f-4a68-8b9a-0f67974f093c",
  "status": "PENDING",
  "attempts": 0,
  "maxAttempts": 5,
  "nextAttemptAt": "2026-06-18T00:00:00Z"
}
```

查询状态：

```http
GET /notifications/{id}
```



## 运行

```bash
./mvnw.cmd test
./mvnw.cmd spring-boot:run
```

服务启动后默认监听 Spring Boot 默认端口 `8080`。
