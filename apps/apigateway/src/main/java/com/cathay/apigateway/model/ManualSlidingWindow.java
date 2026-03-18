package com.cathay.apigateway.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

public class ManualSlidingWindow {
    private record SlidingWindowState(Deque<Instant> timestamps) {}

    private final long limit;
    private final Duration window;

    private final AtomicReference<SlidingWindowState> stateRef;

    public ManualSlidingWindow(int limit, Duration window) {
        if (limit <= 0 || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("limit must be positive and window must be positive");
        }
        this.limit = limit;
        this.window = window;
        this.stateRef = new AtomicReference<>(new SlidingWindowState(new ConcurrentLinkedDeque<>()));
    }

    public boolean tryConsume() {
        while (true) {
            SlidingWindowState state = stateRef.get();
            Instant now = Instant.now();

            // Work on a copy so state transitions are atomic
            Deque<Instant> nextTimestamps = new ConcurrentLinkedDeque<>(state.timestamps());

            // loại bỏ các timestamp đã hết hạn
            Instant before = now.minus(window);
            while (!nextTimestamps.isEmpty() && nextTimestamps.peekFirst().isBefore(before)) {
                nextTimestamps.pollFirst();
            }

            if (nextTimestamps.size() >= limit) {
                return false; // đã đạt giới hạn
            }

            nextTimestamps.offerLast(now); // thêm timestamp mới
            SlidingWindowState newState = new SlidingWindowState(nextTimestamps);

            if (stateRef.compareAndSet(state, newState)) {
                return true; // tiêu thụ thành công
            }
        }
    }

    public void logging(StringBuilder builder) {
        SlidingWindowState state = stateRef.get();
        builder.append(String.format("\nlimit          : %d", limit));
        builder.append(String.format("\nwindow         : %s", window));
        builder.append(String.format("\ntimestamps size: %s", state.timestamps.size()));
    }
}
