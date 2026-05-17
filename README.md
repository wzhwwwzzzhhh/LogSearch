# LogSearch - 日志数据分析系统

## 项目简介

一个用于处理和分析大量日志数据的系统，支持自然语言查询与可视化分析。用户可以使用自然语言提出问题，系统会自动将其转换为 Elasticsearch DSL 查询，并返回分析结果和可视化图表。

## 技术栈

### 后端
- **Spring Boot** - 应用框架
- **Elasticsearch** - 日志存储与检索
- **DeepSeek LLM** - 自然语言到 DSL 转换
- **Spring Async** - 异步任务处理

### 前端
- **Vue 3** - 前端框架
- **Element Plus** - UI 组件库
- **ECharts** - 数据可视化
- **Axios** - HTTP 客户端

## 功能特性

- ✅ 自然语言查询日志
- ✅ 自动转换为 Elasticsearch DSL
- ✅ 查询结果可视化展示
- ✅ AI 智能分析（异步处理）
- ✅ 多种图表类型支持
- ✅ 批量测试脚本

## 快速开始

### 环境要求
- JDK 17+
- Node.js 16+
- Elasticsearch 7.x+
- Maven 3.6+

### 后端启动

```bash
cd backend

# 配置 Elasticsearch 和 LLM API Key
# 编辑 src/main/resources/application.yml

# 启动后端
mvn spring-boot:run
```

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

## 项目结构

```
LogSearch/
├── backend/              # Spring Boot 后端
│   └── src/main/java/com/loganalytics/
│       ├── controller/   # REST API 控制器
│       ├── service/      # 业务逻辑层
│       └── config/       # 配置类
├── frontend/             # Vue 3 前端
│   └── src/
│       ├── views/        # 页面组件
│       ├── components/   # 可复用组件
│       └── api/          # API 调用
├── scripts/              # 批量测试脚本
├── data/                 # 数据生成与导入脚本
├── sql/                  # SQL 脚本
└── md/                   # 项目文档
```

## 配置说明

### application.yml 关键配置

```yaml
elasticsearch:
  host: localhost
  port: 9200
  index: user_logs

llm:
  api-key: your-api-key-here  # 替换为你的 DeepSeek API Key
  api-url: https://api.deepseek.com/v1/chat/completions
  model: deepseek-chat
```

## 测试

### 批量测试

```bash
# 25 条查询测试
node scripts/batch-test-25.js

# 50 条查询测试
node scripts/batch-test-50.js

# 100 条查询测试
node scripts/batch-test.js
```

## 文档

- [设计方案](md/设计方案.md) - 系统架构与设计思路
- [面试准备](md/面试准备.md) - 技术面试准备指南
- [测试查询语句](md/测试查询语句.md) - 100 条测试查询

## 注意事项

1. 使用前需要配置自己的 DeepSeek API Key
2. 确保 Elasticsearch 服务已启动
3. 需要先导入测试数据才能进行查询
