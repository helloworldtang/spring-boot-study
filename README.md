# Spring Boot Study

- 多模块 Spring Boot 学习与实践仓库，当前包含：`deferred-result-async` 模块，用于演示 `DeferredResult` 在受限 Tomcat 资源下的行为与边界。
- 远程仓库：`https://github.com/helloworldtang/spring-boot-study.git`

## 快速开始

- 运行测试：`mvn -pl deferred-result-async test`
- 启动服务：`mvn -pl deferred-result-async spring-boot:run`

## 模块说明

- `deferred-result-async`：演示同步阻塞与 `DeferredResult` 异步的对比、并给出更严苛的连接上限复现用例。
  - 文章与链路说明：`deferred-result-async/docs/deferred-result.md`
  - 模块 README：`deferred-result-async/README.md`

## 端点

- 同步：`GET /sync/process?ms=1500`
- 异步：`GET /async/process?ms=1500`

## 并发对比（示例）

- 在 `server.tomcat.threads.max=2` 下，4 并发、每次约 1.5s：
  - 同步总时长约 `~3.0s`
  - 异步总时长约 `~1.7–1.8s`
- 运行：`mvn -pl deferred-result-async -Dtest=AsyncDemoIntegrationTest test`

## 复现：超过最大连接数的连接阶段超时/拒绝

- 方式一（推荐）：运行严苛用例
  - `mvn -pl deferred-result-async -Dtest=StrictConnectionLimitsTest test`
  - 服务端参数：`max-connections=2`、`accept-count=1`、`threads.max=2`
  - 并发：`requests=30`，客户端 `connectTimeout=200ms`
  - 预期：出现大量 `HTTP connect timed out`，统计日志在 `StrictConnectionLimitsTest.java`:101，失败明细在 `StrictConnectionLimitsTest.java`:94
- 方式二：本地覆盖参数并压测
  - 启动：`mvn -pl deferred-result-async spring-boot:run -Dspring-boot.run.arguments="--server.tomcat.max-connections=2 --server.tomcat.accept-count=1 --server.tomcat.threads.max=2"`
  - 压测（示例）：
    - 同步：`seq 1 30 | xargs -n1 -P30 -I{} curl -sS "http://localhost:8080/sync/process?ms=5000" >/dev/null`
    - 异步：`seq 1 30 | xargs -n1 -P30 -I{} curl -sS "http://localhost:8080/async/process?ms=5000" >/dev/null`

## 版权与维护

- 许可协议：MIT（见 `LICENSE`）
- Maintainer：Jackie `<793059909@qq.com>`
