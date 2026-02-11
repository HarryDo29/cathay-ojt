package com.cathay.apigateway.core.routing;

import com.cathay.apigateway.entity.EndpointsEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PathTrie {
    private final TrieNode root = new TrieNode();

    private String[] parsePath(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return new String[0];
        }
        // Xóa dấu / ở đầu và cuối để tránh tạo ra phần tử rỗng ""
        if (path.startsWith("/")) path = path.substring(1);
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);

        return path.split("/");
    }

    public void insertTrie(EndpointsEntity endpoint){
        TrieNode current = root; // Bắt đầu từ gốc của Trie
        String[] segments = this.parsePath(endpoint.getPath());

        for (String segment : segments) {
            if (segment.isEmpty()) continue;

            if (segment.startsWith("{") && segment.endsWith("}")){
                if (current.paramNode == null) {
                    current.paramNode = new TrieNode();
                    current.paramName = segment.substring(1, segment.length() - 1); // Take param name without '{','}'
                    // node cha sẽ lưu tên tham số vào paramName
                }
                current = current.paramNode;
            }else{
                current = current.children.computeIfAbsent(segment, k -> new TrieNode());
                // computeIfAbsent sẽ kiểm tra nếu segment đã tồn tại trong children
                // nếu chưa tồn tại thì tạo mới một TrieNode và thêm vào children
                // nếu đã tồn tại thì trả về TrieNode hiện có
            }
            // node cuối cùng là node rỗng không có children, paramNode, paramName
            // chỉ có operations để lưu các phương thức HTTP tương ứng với endpoint
            current.operations.computeIfAbsent(endpoint.getMethod().toUpperCase(), k -> endpoint);
        }
    }

    public MatchResult matchTrie(String path, String method){
        TrieNode current = root; // Duyệt từ gốc của Trie
        Map<String, String> extractedParams = new HashMap<>(); // Lưu trữ các tham số được trích xuất
        String[] segments = this.parsePath(path);

        for (String segment : segments) {
            TrieNode childNode = current.children.get(segment);
            if (childNode != null) {
                current = childNode;
            } else if (current.paramNode != null) {
                extractedParams.put(current.getParamName(), segment);
                current = current.paramNode; // Di chuyển đến node param
            } else {
                log.debug("Segment not found: {}", segment);
                return MatchResult.notFound(); // Không tìm thấy đường dẫn
            }

            EndpointsEntity matchedEndpoint = current.operations.get(method.toUpperCase());
            if (matchedEndpoint != null) {
                return MatchResult.found(matchedEndpoint, extractedParams);
            } else {
                return MatchResult.methodNotAllowed(method);
            }
        }
        return MatchResult.notFound();
    }
}
