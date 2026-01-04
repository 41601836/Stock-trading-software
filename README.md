# AI Stock Analysis System (AI 炒股分析系统)

![Java](https://img.shields.io/badge/Java-17%2B-blue) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-green) ![Redis](https://img.shields.io/badge/Redis-7.0-red) ![License](https://img.shields.io/badge/License-MIT-yellow)

## 📖 项目简介

本项目是一个基于 **Java Spring Boot** 构建的高性能、模块化 AI 炒股分析系统。它集成了**筹码分布分析**、**舆情情感计算**、**动态风控**、**游资行为追踪**、**大单异动监控**以及**K线形态识别**六大核心模块，旨在为量化交易和投资决策提供深度的技术支持。

系统采用 **RESTful** 架构风格，遵循严格的金融数据规范（金额精确到分），并引入了 **Redis 缓存**、**全链路追踪** 和 **事件驱动模型**，以确保在高并发场景下的实时性和稳定性。

## 🚀 核心功能 (六大模块)

### 1. 筹码分析 (Chip Analysis)
- **WAD 算法**：基于时间衰减加权平均算法，计算每日筹码分布。
- **核心指标**：精准测算主筹成本、90% 筹码集中度、获利比例。
- **高性能**：引入 Redis 缓存，支持毫秒级查询。

### 2. 舆情分析 (Public Opinion)
- **双层过滤引擎**：
  - Layer 1: 关键词/正则快速过滤广告与垃圾信息。
  - Layer 2: 模拟深度学习模型 (RoBERTa Hook) 进行情感打分 (0-100)。
- **热点自发现**：实时监控舆情流量，当 1 小时内激增 300% 时自动触发异动预警。

### 3. 风险控制 (Risk Control)
- **动态风控体系**：
  - **VaR 计算**：基于历史模拟法计算 95% 置信度下的在险价值。
  - **规则引擎**：支持固定比例止损 (10%) 和移动目标止损 (回撤 8%)。
- **跨模块联动**：实时监测主力大单流向，若大幅流出自动扣减账户健康分。

### 4. 大单异动 (Large Order)
- **Level-2 模拟流**：模拟接收实时交易流，识别单笔 > 50万 的大单。
- **事件驱动**：基于 Spring Event 实现大单监控与风控模块的解耦联动。
- **资金流向**：实时统计个股的主力资金净流入/流出情况。

### 5. 技术分析 (Tech Analysis)
- **形态识别**：基于 **DTW (动态时间规整)** 算法，识别“横盘拉升”等暴涨形态。
- **三剑合一信号**：综合 **筹码单峰密集** + **大单持续流入** + **K线突破平台**，生成高价值买入信号。
- **指标计算**：内置高性能 MA, MACD, RSI 计算引擎。

### 6. 游资行为 (Heat Flow)
- *(规划中)* 龙虎榜数据分析与游资席位追踪。

## 🛠 技术栈

- **后端框架**: Spring Boot 3.2.1
- **构建工具**: Maven
- **缓存**: Redis (Lettuce 客户端)
- **工具库**: Lombok, Jackson, Hutool
- **算法**: 
  - WAD (Weighted Average Distribution)
  - DTW (Dynamic Time Warping)
  - Historical Simulation (VaR)

## 🏁 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- Redis 6.0+ (默认端口 6379)

### 安装步骤

1.  **克隆仓库**
    ```bash
    git clone https://github.com/41601836/Stock-trading-software.git
    cd Stock-trading-software
    ```

2.  **配置 Redis**
    修改 `src/main/resources/application.yml` (如果 Redis 非本地默认配置)：
    ```yaml
    spring:
      data:
        redis:
          host: localhost
          port: 6379
    ```

3.  **编译打包**
    ```bash
    mvn clean package -DskipTests
    ```

4.  **运行应用**
    ```bash
    java -jar target/stock-analysis-0.0.1-SNAPSHOT.jar
    ```

## 📚 API 文档示例

所有接口均返回统一格式：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": { ... },
  "requestId": "req-uuid...",
  "timestamp": 1704355200000
}
```

### 1. 获取筹码分布
- **URL**: `GET /api/v1/chip/distribution?stockCode=600519`
- **功能**: 返回主筹成本、集中度等数据。

### 2. 舆情聚合摘要
- **URL**: `GET /api/v1/public/opinion/summary?stockCode=600519&days=3`
- **功能**: 返回正负面情绪比例及热点预警。

### 3. 账户风险评估
- **URL**: `POST /api/v1/risk/account/assessment`
- **Body**: 持仓列表 JSON
- **功能**: 计算 VaR、健康分及止损建议。

### 4. K线形态识别
- **URL**: `GET /api/v1/tech/kline/pattern/recognize?stockCode=600519`
- **功能**: 检测是否触发“三剑合一”买入信号。

### 5. 模拟大单流 (测试用)
- **URL**: `POST /api/v1/largeorder/simulate?stockCode=600519`
- **功能**: 触发模拟交易流，测试风控联动。

## ⚙️ 开发规范

- **金额单位**：统一使用 **分 (Integer/Long)**，严禁使用 Double。
- **时间格式**：`yyyy-MM-dd HH:mm:ss`。
- **异常处理**：全局统一拦截，返回标准错误码 (600xx 为业务异常)。

## 📄 License

[MIT](LICENSE)
