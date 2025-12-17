# deferred-result-async

- 技术文章与链路图：见 `docs/deferred-result.md`
- 运行测试：`mvn -pl deferred-result-async test`
- 启动服务：`mvn -pl deferred-result-async spring-boot:run`

## 快速验证：并发对比（同步 vs 异步）

- 同步端点：`GET /sync/process?ms=1500`
- 异步端点：`GET /async/process?ms=1500`
- 在默认配置（`server.tomcat.threads.max=2`）下，4 并发时：
  - 同步总时长约 `~3.0s`
  - 异步总时长约 `~1.7–1.8s`
- 运行命令：`mvn -pl deferred-result-async -Dtest=AsyncDemoIntegrationTest test`

## 复现：请求超过最大连接数的连接超时/拒绝

- 方式一：直接运行内置严苛用例（推荐）

  - 命令：`mvn -pl deferred-result-async -Dtest=StrictConnectionLimitsTest test`
  - 该用例将服务端设置为：`server.tomcat.max-connections=2`、`server.tomcat.accept-count=1`、`server.tomcat.threads.max=2`
  - 并发请求数：`requests=30`，客户端连接超时：`200ms`
  - 预期：出现大量 `HTTP connect timed out`，统计日志在 `StrictConnectionLimitsTest.java`:101，失败明细在 `StrictConnectionLimitsTest.java`:94

- 方式二：本地运行服务并压测触发超限
  - 启动服务（覆盖参数）：
    - `mvn -pl deferred-result-async spring-boot:run -Dspring-boot.run.arguments="--server.tomcat.max-connections=2 --server.tomcat.accept-count=1 --server.tomcat.threads.max=2"`
  - 并发触发（示例使用 `curl` 并发）：
    - 同步：
      - `seq 1 30 | xargs -n1 -P30 -I{} curl -sS "http://localhost:8080/sync/process?ms=5000" >/dev/null`
    - 异步：
      - `seq 1 30 | xargs -n1 -P30 -I{} curl -sS "http://localhost:8080/async/process?ms=5000" >/dev/null`
  - 观察控制台：可见连接阶段大量失败；异步同样受连接/队列上限约束，无法“接纳更多连接”。

## 关键代码定位

- 同步阻塞控制器：`src/main/java/com/example/deferred/web/AsyncDemoController.java`:25、`29`
- 异步控制器与回调：`src/main/java/com/example/deferred/web/AsyncDemoController.java`:35、`41`、`43`、`48`、`53`、`58`、`68`
- 后台线程池：`src/main/java/com/example/deferred/config/TaskConfig.java`:11`
- 严苛连接上限测试：`src/test/java/com/example/deferred/StrictConnectionLimitsTest.java`:1`
