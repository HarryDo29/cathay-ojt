package com.cathay.apigateway.service;

import com.cathay.apigateway.entity.RateLimitRuleEntity;
import com.cathay.apigateway.enums.KeyType;
import com.cathay.apigateway.enums.RateLimitType;
import com.cathay.apigateway.interfaces.IRateLimitRuleRepository;
import com.cathay.apigateway.model.SlideWindowRule;
import com.cathay.apigateway.model.TokenBucketRule;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.*;

@Slf4j
@Service
public class RateLimitRuleService {

    @Getter
    public volatile List<RateLimitRuleEntity> rateLimitList = List.of();
    @Getter
    public volatile List<SlideWindowRule> slideWindowRuleList = List.of();
    @Getter
    public volatile List<TokenBucketRule> tokenBucketRuleList = List.of();

    private final IRateLimitRuleRepository rateLimitRepository;

    public RateLimitRuleService(IRateLimitRuleRepository rateLimitRepository) {
        this.rateLimitRepository = rateLimitRepository;
    }

    @PostConstruct
    public void init() {
        log.info("[RateLimit] ▶️ Loading rate limit configurations...");
        loadRateLimits().block();
        log.info("[RateLimit] ✅ Rate limiter ready — {} rate limit rules configured", rateLimitList.size());
    }

    public Mono<Void> loadRateLimits() {
        return rateLimitRepository.getAllRateLimitRules()
                .collectList()
                .doOnNext(rateLimit -> {
                        rateLimitList = List.copyOf(rateLimit); // cache rate limit list (all types)
                        List<SlideWindowRule> slideWindowRule = new ArrayList<>();
                        List<TokenBucketRule> tokenBucketRule = new ArrayList<>();
                        rateLimitList.forEach(rate -> {
                            if (rate.getType() == RateLimitType.SLIDING_WINDOW && rate.getKeyType() == KeyType.ACCOUNT_ID) {
                                slideWindowRule.add(SlideWindowRule.fromJson(rate.getRule()));
                            }else if (rate.getType() == RateLimitType.TOKEN_BUCKET && rate.getKeyType() == KeyType.IP) {
                                tokenBucketRule.add(TokenBucketRule.fromJson(rate.getRule()));
                            }
                        });
                        slideWindowRule.sort(Comparator.comparingInt(SlideWindowRule::getPriority)); // sort by priority desc
                        this.slideWindowRuleList = List.copyOf(slideWindowRule); // cache sliding window rules for quick access
                        this.tokenBucketRuleList = List.copyOf(tokenBucketRule); // cache token bucket rules for quick access
                })
                .then();
    }

}
