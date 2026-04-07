package com.cathay.apigateway.core.routing;

import com.cathay.apigateway.entity.EndpointEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
public class TrieNode {
    Map<String, TrieNode> children = new ConcurrentHashMap<>(); // static path segments

    TrieNode paramNode = null;

    String paramName = null;

    Map<String, EndpointEntity> operations = new ConcurrentHashMap<>();
}
