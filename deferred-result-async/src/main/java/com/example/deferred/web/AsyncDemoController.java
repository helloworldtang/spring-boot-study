package com.example.deferred.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;

@RestController
public class AsyncDemoController {
    private static final Logger log = LoggerFactory.getLogger(AsyncDemoController.class);
    private final Executor workerExecutor;

    public AsyncDemoController(@Qualifier("workerExecutor") Executor workerExecutor) {
        this.workerExecutor = workerExecutor;
    }

    @GetMapping(path = "/sync/process", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result sync(@RequestParam(name = "ms", defaultValue = "1500") long ms) throws InterruptedException {
        Instant start = Instant.now();
        log.info("[sync] request received, processingMs={}ms", ms);
        Thread.sleep(ms);
        Duration d = Duration.between(start, Instant.now());
        log.info("[sync] request done in {}ms", d.toMillis());
        return Result.ok(d.toMillis());
    }

    @GetMapping(path = "/async/process", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<Result> async(@RequestParam(name = "ms", defaultValue = "1500") long ms) {
        Instant start = Instant.now();
        log.info("[async] request received, processingMs={}ms", ms);

        long timeoutMs = Duration.ofMinutes(5).toMillis();
        DeferredResult<Result> dr = new DeferredResult<>(timeoutMs);

        dr.onTimeout(() -> {
            log.warn("[async] timed out after {}ms", timeoutMs);
            dr.setErrorResult(Result.timeout(timeoutMs));
        });

        dr.onError(throwable -> {
            log.error("[async] error: {}", throwable.getMessage());
            dr.setErrorResult(Result.error(throwable.getMessage()));
        });

        workerExecutor.execute(() -> {
            log.info("[async] worker started on {}", Thread.currentThread().getName());
            try {
                Thread.sleep(ms);
                Duration d = Duration.between(start, Instant.now());
                log.info("[async] worker completed in {}ms", d.toMillis());
                dr.setResult(Result.ok(d.toMillis()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                dr.setErrorResult(Result.error("interrupted"));
            } catch (Exception e) {
                dr.setErrorResult(Result.error(e.getMessage()));
            }
        });

        log.info("[async] controller returned DeferredResult immediately");
        return dr;
    }

    public record Result(boolean success, String message, long elapsedMs) {
        public static Result ok(long elapsed) { return new Result(true, "ok", elapsed); }
        public static Result timeout(long timeoutMs) { return new Result(false, "timeout", timeoutMs); }
        public static Result error(String msg) { return new Result(false, msg, -1); }
    }
}

