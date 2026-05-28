package com.mortal.query.service;

import com.mortal.platform.common.ApiResponse;
import com.mortal.query.client.RegulationRegionClient;
import com.mortal.query.client.RegulatorProfileClient;
import com.mortal.query.common.ForbiddenException;
import com.mortal.query.dto.WarningStatsQueryDTO;
import com.mortal.query.support.QueryRedisCacheSupport;
import com.mortal.query.vo.RegulatorProfileVO;
import com.mortal.query.vo.RegionVO;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 预警统计权限范围处理。
 */
@Service
public class WarningStatsScopeService {

    private static final Logger log = LoggerFactory.getLogger(WarningStatsScopeService.class);

    private static final String ROLE_ADMIN = "REGULATOR_ADMIN";
    private static final String ROLE_ENFORCER = "REGULATOR_ENFORCER";

    private final RegulatorProfileClient regulatorProfileClient;
    private final RegulationRegionClient regulationRegionClient;
    private final QueryRedisCacheSupport queryRedisCacheSupport;

    public WarningStatsScopeService(RegulatorProfileClient regulatorProfileClient,
                                    RegulationRegionClient regulationRegionClient,
                                    QueryRedisCacheSupport queryRedisCacheSupport) {
        this.regulatorProfileClient = regulatorProfileClient;
        this.regulationRegionClient = regulationRegionClient;
        this.queryRedisCacheSupport = queryRedisCacheSupport;
    }

    /**
     * 根据当前用户身份补全并校验统计范围。
     */
    public WarningStatsQueryDTO applyScope(WarningStatsQueryDTO source,
                                           Long userId,
                                           String userType,
                                           String authorization) {
        if (userId == null || !StringUtils.hasText(userType)) {
            throw new IllegalArgumentException("unauthorized");
        }
        WarningStatsQueryDTO query = copy(source);
        String normalizedUserType = userType.trim().toUpperCase(Locale.ROOT);
        if (!normalizedUserType.startsWith("REGULATOR")) {
            return query;
        }
        RegulatorProfileVO profile = loadMyProfile(userId, authorization);
        String roleType = normalizeRoleType(profile.getRoleType());
        if (ROLE_ENFORCER.equals(roleType)) {
            applyEnforcerScope(query, profile.getId());
            return query;
        }
        if (ROLE_ADMIN.equals(roleType)) {
            Set<Long> directRegionIds = parsePositiveIds(profile.getRegionIds());
            Set<Long> fullRegionIds = expandRegionIds(profile.getId(), directRegionIds, authorization);
            applyAdminScope(query, fullRegionIds);
            return query;
        }
        throw new ForbiddenException("unsupported regulator role");
    }

    private void applyEnforcerScope(WarningStatsQueryDTO query, Long regulatorId) {
        if (regulatorId == null || regulatorId <= 0) {
            throw new ForbiddenException("enforcer profile invalid");
        }
        if (query.getRegionId() != null || StringUtils.hasText(query.getRegionIds())) {
            throw new ForbiddenException("enforcer cannot query by region");
        }
        if (query.getAssignedTo() == null) {
            query.setAssignedTo(regulatorId);
            return;
        }
        if (!regulatorId.equals(query.getAssignedTo())) {
            throw new ForbiddenException("enforcer cannot query other enforcer scope");
        }
    }

    private void applyAdminScope(WarningStatsQueryDTO query, Set<Long> allowedRegionIds) {
        if (allowedRegionIds.isEmpty()) {
            throw new ForbiddenException("admin region scope empty");
        }
        if (query.getRegionId() != null) {
            if (!allowedRegionIds.contains(query.getRegionId())) {
                throw new ForbiddenException("region out of admin scope");
            }
            return;
        }
        Set<Long> requestRegionIds = parseRegionIds(query.getRegionIds());
        if (!requestRegionIds.isEmpty()) {
            if (!allowedRegionIds.containsAll(requestRegionIds)) {
                throw new ForbiddenException("region out of admin scope");
            }
            return;
        }
        query.setRegionIds(joinRegionIds(allowedRegionIds));
    }

    private RegulatorProfileVO loadMyProfile(Long userId, String authorization) {
        if (!StringUtils.hasText(authorization)) {
            throw new IllegalArgumentException("authorization required");
        }
        if (userId == null || userId <= 0) {
            return requireProfile(regulatorProfileClient.getMyProfile(authorization));
        }
        String cacheKey = queryRedisCacheSupport.buildKey("query", "scope", "profile", String.valueOf(userId));
        return queryRedisCacheSupport.getScopeOrLoad(
            cacheKey,
            () -> requireProfile(regulatorProfileClient.getMyProfile(authorization))
        );
    }

    private WarningStatsQueryDTO copy(WarningStatsQueryDTO source) {
        WarningStatsQueryDTO copy = new WarningStatsQueryDTO();
        if (source == null) {
            return copy;
        }
        copy.setStartTime(source.getStartTime());
        copy.setEndTime(source.getEndTime());
        copy.setWarningType(source.getWarningType());
        copy.setBizType(source.getBizType());
        copy.setLevel(source.getLevel());
        copy.setStatus(source.getStatus());
        copy.setRegionId(source.getRegionId());
        copy.setRegionIds(source.getRegionIds());
        copy.setAssignedTo(source.getAssignedTo());
        copy.setTopN(source.getTopN());
        copy.setTrendDays(source.getTrendDays());
        copy.setOverdueHours(source.getOverdueHours());
        return copy;
    }

    private String normalizeRoleType(String roleType) {
        if (!StringUtils.hasText(roleType)) {
            return "";
        }
        return roleType.trim().toUpperCase(Locale.ROOT);
    }

    private Set<Long> parsePositiveIds(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
            .filter(value -> value != null && value > 0)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> parseRegionIds(String regionIds) {
        if (!StringUtils.hasText(regionIds)) {
            return Set.of();
        }
        return List.of(regionIds.split(",")).stream()
            .map(String::trim)
            .filter(StringUtils::hasText)
            .map(text -> {
                try {
                    return Long.valueOf(text);
                } catch (NumberFormatException ex) {
                    return null;
                }
            })
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String joinRegionIds(Set<Long> regionIds) {
        return regionIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    /**
     * 管理员统计范围按“辖区 + 下级辖区”展开，保证与监管预警列表口径一致。
     */
    private Set<Long> expandRegionIds(Long regulatorId, Set<Long> rootRegionIds, String authorization) {
        if (rootRegionIds == null || rootRegionIds.isEmpty()) {
            return Set.of();
        }
        if (!StringUtils.hasText(authorization)) {
            return rootRegionIds;
        }
        if (regulatorId != null && regulatorId > 0) {
            String cacheKey = queryRedisCacheSupport.buildKey(
                "query",
                "scope",
                "region-set",
                String.valueOf(regulatorId)
            );
            return queryRedisCacheSupport.getScopeOrLoad(
                cacheKey,
                () -> loadExpandedRegionIds(rootRegionIds, authorization)
            );
        }
        return loadExpandedRegionIds(rootRegionIds, authorization);
    }

    private Set<Long> loadExpandedRegionIds(Set<Long> rootRegionIds, String authorization) {
        Set<Long> result = new LinkedHashSet<>(rootRegionIds);
        ArrayDeque<Long> queue = new ArrayDeque<>(rootRegionIds);
        while (!queue.isEmpty()) {
            Long parentId = queue.poll();
            List<RegionVO> children = fetchRegions(authorization, parentId);
            for (RegionVO child : children) {
                Long childId = child == null ? null : child.getId();
                if (childId == null || childId <= 0 || result.contains(childId)) {
                    continue;
                }
                result.add(childId);
                queue.add(childId);
            }
        }
        return result;
    }

    private RegulatorProfileVO requireProfile(ApiResponse<RegulatorProfileVO> response) {
        if (response == null) {
            throw new IllegalStateException("load regulator profile failed");
        }
        if (response.getCode() != 0 || response.getData() == null) {
            String message = StringUtils.hasText(response.getMessage())
                ? response.getMessage()
                : "load regulator profile failed";
            throw new ForbiddenException(message);
        }
        return response.getData();
    }

    private List<RegionVO> fetchRegions(String authorization, Long parentId) {
        try {
            ApiResponse<List<RegionVO>> response = regulationRegionClient.listRegions(authorization, parentId);
            if (response == null || response.getCode() != 0 || response.getData() == null) {
                return List.of();
            }
            return response.getData();
        } catch (Exception ex) {
            log.warn("expand region scope failed. parentId={}, message={}", parentId, ex.getMessage());
            return List.of();
        }
    }
}
