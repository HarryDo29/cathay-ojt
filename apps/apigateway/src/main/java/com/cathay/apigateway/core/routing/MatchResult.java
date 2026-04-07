package com.cathay.apigateway.core.routing;

import com.cathay.apigateway.entity.EndpointEntity;
import com.cathay.apigateway.enums.Status;
import lombok.Getter;
import java.util.Collections;
import java.util.Map;

@Getter
public class MatchResult {
    private Status status;
    private EndpointEntity entity;
    private Map<String, String> params;
    private String allowedMethod; // Để gợi ý cho client nếu lỗi 405

    // Private Constructor (Dùng Factory Method cho gọn)
    private MatchResult(Status status, EndpointEntity entity, Map<String, String> params, String allowedMethod) {
        this.status = status;
        this.entity = entity;
        this.params = params != null ? params : Collections.emptyMap();
        this.allowedMethod = allowedMethod != null ? allowedMethod : "";
    }

    // --- FACTORY METHODS ---
    public static MatchResult found(EndpointEntity entity, Map<String, String> params) {
        return new MatchResult(Status.FOUND, entity, params, null);
    }

    public static MatchResult notFound() {
        return new MatchResult(Status.PATH_NOT_FOUND, null, null, null);
    }

    public static MatchResult methodNotAllowed(String allowedMethod) {
        return new MatchResult(Status.METHOD_NOT_ALLOWED, null, null, allowedMethod);
    }
}
