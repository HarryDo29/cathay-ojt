package com.cathay.apigateway.model;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class ManualSlidingWindow {
    private record SlidingWindowState(Deque<Instant> timestamps) {}

    private final long limit;
    private final Duration window;
    private final Deque<Instant> timestamps = new ConcurrentLinkedDeque<>();

    private final AtomicReference<SlidingWindowState> stateRef = new AtomicReference<>();

    public ManualSlidingWindow(int limit, Duration window) {
        if (limit <= 0 || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("limit must be positive and window must be positive");
        }
        this.limit = limit;
        this.window = window;
    }

    public boolean tryConsume() {
        while (true) {
            SlidingWindowState state = stateRef.get();
            Instant now = Instant.now();

            // loại bỏ các timestamp đã hết hạn
            Instant before = now.minus(window);
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(before)) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= limit) {
                return false; // đã đạt giới hạn
            }

            timestamps.offerLast(now); // thêm timestamp mới
            SlidingWindowState newState = new SlidingWindowState(new ConcurrentLinkedDeque<>(timestamps));

            if (stateRef.compareAndSet(state, newState)) {
                return true; // tiêu thụ thành công
            }
        }
    }

    public void logging(StringBuilder builder) {
        SlidingWindowState state = stateRef.get();
        builder.append(String.format("\nlimit     : %d", limit));
        builder.append(String.format("\nwindow    : %s", window));
        builder.append(String.format("\ntimestamps: %s", state.timestamps()));
    }
}
