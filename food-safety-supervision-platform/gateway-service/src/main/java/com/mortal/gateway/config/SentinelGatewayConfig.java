package com.mortal.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentinelGatewayConfig {

    private static final String API_LOGIN = "api_login";
    private static final String API_ALL = "api_all";
    private static final String API_QUERY = "api_query";
    private static final String API_WRITE = "api_write";

    @PostConstruct
    public void initGatewayRules() {
        // 关键注释：登录接口严格限流（精确匹配 /api/auth/login）
        ApiDefinition loginApi = new ApiDefinition(API_LOGIN)
            .setPredicateItems(Set.of(new ApiPathPredicateItem()
                .setPattern("/api/auth/login")
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_EXACT)));

        // 关键注释：全量 API 基础限流（/api/**）
        ApiDefinition allApi = new ApiDefinition(API_ALL)
            .setPredicateItems(Set.of(new ApiPathPredicateItem()
                .setPattern("/api/**")
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX)));

        // 关键注释：查询与写操作按请求方法区分限流（依赖网关透传 X-Method）
        ApiDefinition queryApi = new ApiDefinition(API_QUERY)
            .setPredicateItems(Set.of(new ApiPathPredicateItem()
                .setPattern("/api/**")
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX)));

        ApiDefinition writeApi = new ApiDefinition(API_WRITE)
            .setPredicateItems(Set.of(new ApiPathPredicateItem()
                .setPattern("/api/**")
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX)));

        Set<ApiDefinition> apis = new HashSet<>();
        apis.add(loginApi);
        apis.add(allApi);
        apis.add(queryApi);
        apis.add(writeApi);
        GatewayApiDefinitionManager.loadApiDefinitions(apis);

        // 关键注释：最小可用分级限流（若需按方法区分，可后续升级到自定义规则）
        Set<GatewayFlowRule> rules = new HashSet<>();
        rules.add(new GatewayFlowRule(API_LOGIN).setCount(5).setIntervalSec(1));
        rules.add(new GatewayFlowRule(API_ALL).setCount(30).setIntervalSec(1));

        GatewayParamFlowItem methodIsGet = new GatewayParamFlowItem()
            .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER)
            .setFieldName("X-Method")
            .setPattern("GET")
            .setMatchStrategy(SentinelGatewayConstants.PARAM_MATCH_STRATEGY_EXACT);

        GatewayParamFlowItem methodIsWrite = new GatewayParamFlowItem()
            .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER)
            .setFieldName("X-Method")
            .setPattern("POST|PUT|DELETE")
            .setMatchStrategy(SentinelGatewayConstants.PARAM_MATCH_STRATEGY_REGEX);

        // 关键注释：查询接口宽松限流（GET）
        rules.add(new GatewayFlowRule(API_QUERY)
            .setCount(50)
            .setIntervalSec(1)
            .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
            .setParamItem(methodIsGet));

        // 关键注释：写操作更严格（POST/PUT/DELETE）
        rules.add(new GatewayFlowRule(API_WRITE)
            .setCount(20)
            .setIntervalSec(1)
            .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
            .setParamItem(methodIsWrite));
        GatewayRuleManager.loadRules(rules);
    }
}
