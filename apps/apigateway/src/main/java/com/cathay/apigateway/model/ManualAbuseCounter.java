package com.cathay.apigateway.model;

import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public class ManualAbuseCounter {
    private record AbuseCounterState(int counter, Queue<Instant> timestamps) {
    }

    private static final int MAX_COUNTER = 5; // giới hạn số lần vi phạm trong khoảng thời gian

    private final AtomicReference<AbuseCounterState> stateRef;

    public ManualAbuseCounter() {
        this.stateRef = new AtomicReference<>(new AbuseCounterState(
                0,
                new java.util.concurrent.ConcurrentLinkedQueue<>())
        );
    }

    public boolean tryConsume(String ip){
        while (true) {
            AbuseCounterState state = stateRef.get();
            Instant now = Instant.now();
            int nCounter = state.counter;
            Queue<Instant> nTimestamps = new ConcurrentLinkedQueue<>(state.timestamps);

            if (state.counter() >= MAX_COUNTER) {
                return false;// đã đạt giới hạn vi phạm
            }

            nTimestamps.offer(now);// thêm timestamp mới
            AbuseCounterState newState = new AbuseCounterState(nCounter + 1, nTimestamps);

            if (stateRef.compareAndSet(state, newState)) {
                return true;// tiêu thụ thành công
            }
        }
    }
}
