package com.cathay.apigateway.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
public class SlideWindowRule {
        private String[] methods;
        private String path_regex;
        private Integer window; // thời gian tính bằng giây để đếm số lượng request
        private Integer limit;  // số lượng request tối đa trong window
        private Integer priority;

        public SlideWindowRule() {}

        // try to parse JSON to SlideWindowRule
        public static SlideWindowRule fromJson(String json) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(json, SlideWindowRule.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
}
