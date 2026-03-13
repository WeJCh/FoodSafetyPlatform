package com.mortal.query.service;

import com.mortal.query.client.RegulatorProfileClient;
import com.mortal.query.common.ApiResponse;
import com.mortal.query.common.ForbiddenException;
import com.mortal.query.dto.WarningStatsQueryDTO;
import com.mortal.query.vo.RegulatorProfileVO;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 预警统计权限范围处理。
 */
@Service
public class WarningStatsScopeService {

    private static final String ROLE_ADMIN = "REGULATOR_ADMIN";
    private static final String ROLE_ENFORCER = "REGULATOR_ENFORCER";

    private final RegulatorProfileClient regulatorProfileClient;

    public WarningStatsScopeService(RegulatorProfileClient regulatorProfileClient) {
        this.regulatorProfileClient = regulatorProfileClient;
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
        RegulatorProfileVO profile = loadMyProfile(authorization);
        String roleType = normalizeRoleType(profile.getRoleType());
        if (ROLE_ENFORCER.equals(roleType)) {
            applyEnforcerScope(query, profile.getId());
            return query;
        }
        if (ROLE_ADMIN.equals(roleType)) {
            applyAdminScope(query, parsePositiveIds(profile.getRegionIds()));
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
        if (query.getOwnerRegulatorId() == null) {
            query.setOwnerRegulatorId(regulatorId);
            return;
        }
        if (!regulatorId.equals(query.getOwnerRegulatorId())) {
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

    private RegulatorProfileVO loadMyProfile(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            throw new IllegalArgumentException("authorization required");
        }
        ApiResponse<RegulatorProfileVO> response = regulatorProfileClient.getMyProfile(authorization);
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
        copy.setOwnerRegulatorId(source.getOwnerRegulatorId());
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
}

