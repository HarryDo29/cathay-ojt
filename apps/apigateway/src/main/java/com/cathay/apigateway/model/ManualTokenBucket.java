package com.cathay.apigateway.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ManualTokenBucket {
    private final long capacity; //token
    private final long replenishRate; // token/s

    private final AtomicReference<BucketState> stateRef;

    private record BucketState(long availableTokens, long lastRefillNanos) {
    }

    public ManualTokenBucket(long capacity, long replenishRate) {
        this.capacity = capacity;
        this.replenishRate = replenishRate;
        // khởi tạo xô vs đầy thẻ
        this.stateRef = new AtomicReference<>(new BucketState(capacity, System.nanoTime()));
    }

    public boolean tryConsume(Long consumeToken) {
        while (true) {
            BucketState bucketState = stateRef.get();
            long now = System.nanoTime();

            long period = TimeUnit.NANOSECONDS.toSeconds(now - bucketState.lastRefillNanos);
            long refillRange = period * this.replenishRate;
            long nCapacity = Math.min(this.capacity, bucketState.availableTokens + refillRange);

            if (nCapacity < consumeToken) {
                return false; // không đủ token để tiêu thụ
            }

            BucketState nBucketState = new BucketState(nCapacity - 1, now);

            if (stateRef.compareAndSet(bucketState, nBucketState)) {
                return true; // tiêu thụ thành công
            }
        }
    }

    public void logging(StringBuilder builder) {
        BucketState bucketState = stateRef.get();
        builder.append(String.format("\nCapacity       : %d", capacity));
        builder.append(String.format("\nReplenishRate  : %d", replenishRate));
        builder.append(String.format("\nAvailableTokens: %d", bucketState.availableTokens));
        builder.append(String.format("\nLastRefillNanos: %d", bucketState.lastRefillNanos));
    }
}