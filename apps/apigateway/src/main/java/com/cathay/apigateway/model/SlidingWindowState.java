package com.cathay.apigateway.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;

public final class SlidingWindowState {

    private final int limit;
    private final Duration window;
    private final Deque<Instant> timestamps = new ConcurrentLinkedDeque<>();
    private final ReentrantLock lock = new ReentrantLock();

    public SlidingWindowState(int limit, Duration window) {
        if (limit <= 0 || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("limit must be positive and window must be positive");
        }
        this.limit = limit;
        this.window = window;
    }

    public boolean tryConsume() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);
        lock.lock();
        try {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() < limit) {
                timestamps.addLast(now);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
}
