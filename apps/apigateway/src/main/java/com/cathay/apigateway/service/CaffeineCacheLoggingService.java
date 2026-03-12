package com.cathay.apigateway.service;

import com.cathay.apigateway.model.SlidingWindowState;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class CaffeineCacheLoggingService {

    public <T> void logRateLimitDetail(String type, String key, Cache<String, T> cache) {
        T value = cache.getIfPresent(key);

        if (value == null) {
            log.warn("[{}] Rate Limit Hit for Key: {} | Nhưng không tìm thấy dữ liệu trong cache (có thể vừa bị expire)", type, key);
            return;
        }

        StringBuilder detail = new StringBuilder();
        detail.append(String.format("\n--- RATE LIMIT HIT: %s ---", type));
        detail.append(String.format("\nKey: %s", key));
        detail.append(String.format("\nTime: %s", Instant.now()));

        if (value instanceof Bucket bucket) {
            // Nếu là Bucket4j
            detail.append(String.format("\nBucket4j - tokens còn lại: %d", bucket.getAvailableTokens()));
        } else if (value instanceof SlidingWindowState state) {
            // Nếu là SlidingWindowState (sliding window in-memory)
            detail.append(String.format("\nSlidingWindowState: %s", state.snapshot()));
        }

        // In thêm toàn bộ object trong cache (dựa trên toString())
        detail.append(String.format("\nRaw cache value: %s", String.valueOf(value)));
        detail.append("\n------------------------------");
        log.warn(detail.toString());
    }

}
