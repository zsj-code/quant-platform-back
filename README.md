# quant-platform

AI 量化平台后端：整合 A 股市场多源数据（东方财富、同花顺、淘股吧等），提供基本面 / 技术面 / 情绪面因子计算、K 线存储与聚合、AI Agent 研究与交易决策工作流，并通过 REST API 对外服务。

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 / 构建 | Java 17、Maven 多模块 |
| 框架 | Spring Boot 3.4.3 |
| 持久化 | MySQL、MyBatis-Plus 3.5 |
| 缓存 | Redis |
| 定时任务 | XXL-JOB 2.4 |
| AI | LangChain4j 1.13（OpenAI 兼容接口） |
| 技术指标 | TA4J 0.15 |
| 工具 | Lombok、MapStruct、Guava、Jsoup |

## 模块结构

```
quant-platform-back/
├── quant-platform-common      # 公共模型、枚举、工具、统一 API 响应
├── quant-platform-ai-core     # AI Agent、因子引擎、外部数据源 Client
├── quant-platform-business    # 业务 API、实体、Mapper、数据同步 Job 服务
└── quant-platform-app         # Spring Boot 启动入口、全局配置、XXL-JOB Handler
```

依赖关系：`app` → `business` → `ai-core` → `common`。

## 功能概览

- **数据采集与同步**：股票基础信息、估值快照、K 线（日 / 分钟）、财报、公告、研报、股吧帖子与淘股吧评论等，由 XXL-JOB 定时触发。
- **K 线聚合**：由日 K 聚合周 / 月 / 年 K；由 1 分钟 K 聚合更高周期分钟 K（本地计算，不重复请求行情接口）。
- **因子体系**（`quant-platform-ai-core`）：
  - **基本面**：硬过滤（审计意见、壳公司、现金流背离等）、观察名单因子
  - **技术面**：基于 TA4J 与自研逻辑（MACD 背离、筹码、ADX、RSI 等）
  - **情绪面**：全市场、个股、风格、衍生品等多维度情绪因子
- **AI Agent**：基于 LangChain4j 的对话、研究计划与 SSE 流式输出（需配置 LLM API Key）。
- **交易决策**：工作流运行记录查询与 LLM 摘要 PDF 预览 / 导出。

## 环境要求

- JDK 17（项目通过 Maven Toolchains 约束版本）
- Maven 3.8+
- MySQL 5.7+ / 8.x（默认库名 `quantdb`）
- Redis 6+
- [XXL-JOB Admin](https://www.xuxueli.com/xxl-job/)（调度中心，默认 `http://127.0.0.1:8081/xxl-job-admin`）
- OpenAI 兼容 LLM 服务（可选，用于 Agent 功能）

## 快速开始

### 1. 克隆与编译

```bash
git clone <repository-url>
cd quant-platform-back
mvn clean install -DskipTests
```

### 2. 准备数据库

创建 MySQL 数据库（示例）：

```sql
CREATE DATABASE quantdb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

按业务需要导入表结构（实体与 `mapper` 位于 `quant-platform-business`）。仓库内仅包含少量增量 SQL，例如 `quant-platform-business/src/main/resources/sql/`。

修改 `quant-platform-app/src/main/resources/application.yml` 中的数据源与 Redis 连接信息。

### 3. 配置 AI（可选）

Agent 相关接口在未配置 API Key 时不会注册。通过环境变量注入密钥（**勿将 Key 提交到仓库**）：

**Windows PowerShell：**

```powershell
$env:QUANT_AI_LANGCHAIN4J_OPENAI_API_KEY="sk-..."
```

**Linux / macOS：**

```bash
export QUANT_AI_LANGCHAIN4J_OPENAI_API_KEY="sk-..."
```

可在 `application.yml` 的 `quant.ai.langchain4j.openai` 下调整 `base-url`、`model`、`temperature` 等（默认对接 DeepSeek 兼容接口）。

### 4. 启动应用

```bash
cd quant-platform-app
mvn spring-boot:run
```

或运行主类 `com.quant.platform.QuantPlatformApplication`。

默认访问地址：

| 项 | 值 |
|----|-----|
| 端口 | `8082` |
| Context Path | `/quant-ai` |
| 健康检查 | `GET http://localhost:8082/quant-ai/api/health` |
| Ping | `GET http://localhost:8082/quant-ai/api/ping` |

带数据库探测：`GET .../api/health?checkDb=true`

### 5. 配置 XXL-JOB

1. 部署并启动 XXL-JOB Admin。
2. 在调度中心注册执行器，应用名与 `application.yml` 中 `xxl.job.executor.appname` 一致（默认 `quant-ai-executor`）。
3. 在 Admin 中配置以下 Job Handler（定义于 `DataSyncJobHandler`）：

| Job 名称 | 说明 |
|----------|------|
| `syncStockBasics` | 同步股票基础信息 |
| `syncStockValuationSnapshot` | 同步个股估值 / 行情快照 |
| `syncFinanceReports` | 同步财报 |
| `syncEastmoneyNotice` | 同步公告 |
| `syncResearchReports` | 同步研报（个股 / 行业） |
| `syncStockPost` | 同步股吧帖子 |
| `syncTaoGubaPostComment` | 同步淘股吧帖与评论 |
| `syncKlineDay` | 同步日 K |
| `syncKlineSecond` | 同步 1 分钟 K |
| `aggregateKlineFromDaily` | 日 K → 周 / 月 / 年 K |
| `aggregateKlineFromM1` | 1 分钟 K → 更高周期 |

各任务支持通过 Job 参数传递 `sleepMs`、`beg`、`end`、`code` 等，详见 `DataSyncJobHandler` 类注释。

## 主要 API

所有路径均带前缀 `/quant-ai`。下表为业务路径（不含 context path 重复说明）。

| 路径 | 说明 |
|------|------|
| `/api/health`、`/api/ping` | 系统健康与连通性 |
| `/api/stocks` | 股票查询 |
| `/api/kline-bars` | K 线查询 |
| `/api/fundamental/**` | 基本面数据与因子评估 |
| `/api/technical/**` | 技术面因子 |
| `/api/sentiment/**` | 情绪面因子 |
| `/api/stock-valuation-snapshots` | 估值快照 |
| `/api/stock-industry-valuations` | 行业估值 |
| `/api/trader-decisions` | 交易决策运行记录与 PDF |
| `/api/agent/chat` | AI 对话（需 API Key） |
| `/api/agent/research/**` | 研究 Agent（含 SSE） |
| `/api/agent/runs` | Agent 运行记录 |
| `/taoguba/**` | 淘股吧相关 OpenAPI |

另有若干 `openapi` 包下的控制器，用于对接东方财富、同花顺等外部数据源的调试与集成。

统一响应封装为 `com.quant.platform.common.api.Result`。

## 配置说明

核心配置见 `quant-platform-app/src/main/resources/application.yml`：

| 配置项 | 说明 |
|--------|------|
| `server.port` | HTTP 端口，默认 `8082` |
| `server.servlet.context-path` | 上下文路径 `/quant-ai` |
| `spring.datasource.*` | MySQL 连接 |
| `spring.data.redis.*` | Redis 连接 |
| `xxl.job.*` | XXL-JOB 调度中心与执行器 |
| `quant.ai.langchain4j.openai.*` | LLM 接口与模型 |
| `quant.integration.community-post.*` | 淘股吧同步限速与时间窗 |

生产环境建议通过环境变量或外部配置中心覆盖敏感项（数据库密码、XXL `accessToken`、LLM API Key）。

## 开发与测试

```bash
# 全量编译
mvn clean package

# 运行测试
mvn test

# 仅启动模块测试
cd quant-platform-app && mvn test
```

## 项目结构（业务包）

| 包路径 | 职责 |
|--------|------|
| `com.quant.platform.business.stock` | 股票、公告、估值 |
| `com.quant.platform.business.kline` | K 线读写 |
| `com.quant.platform.business.financial` | 财报 |
| `com.quant.platform.business.job` | 数据同步与聚合服务 |
| `com.quant.platform.business.agent` | AI 研究 / 对话 API |
| `com.quant.platform.business.trader` | 交易决策工作流 |
| `com.quant.platform.ai.core.factor` | 因子实现与编排 |
| `com.quant.platform.ai.core.client` | 东财、同花顺等 HTTP Client |
| `com.quant.platform.ai.core.langchain4j` | LangChain4j 集成 |

## 注意事项

- 外部行情与社区数据接口存在访问频率限制，同步任务请合理设置 `sleepMs` 与分页参数。
- SSE 接口已强制 UTF-8 编码，避免中文乱码。
- AI 功能依赖第三方 LLM 服务，请自行评估费用与合规要求。
- 本项目仅供研究与学习，不构成任何投资建议。

## 许可证

未在仓库中声明许可证时，使用前请与项目维护者确认。
