package com.mortal.query.service.impl;

import com.mortal.platform.common.ApiResponse;
import com.mortal.query.client.ComplaintStatsClient;
import com.mortal.query.client.RegulationEnterpriseStatsClient;
import com.mortal.query.client.RegulationOperationStatsClient;
import com.mortal.query.client.WarningStatsClient;
import com.mortal.query.dto.WarningStatsQueryDTO;
import com.mortal.query.service.SupervisionOverviewQueryService;
import com.mortal.query.support.QueryRedisCacheSupport;
import com.mortal.query.vo.ComplaintStatsOverviewVO;
import com.mortal.query.vo.EnterpriseStatsOverviewVO;
import com.mortal.query.vo.OperationStatsOverviewVO;
import com.mortal.query.vo.SupervisionOverviewVO;
import com.mortal.query.vo.WarningStatsOverviewVO;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

@Service
public class SupervisionOverviewQueryServiceImpl implements SupervisionOverviewQueryService {

    private static final DateTimeFormatter KEY_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final RegulationEnterpriseStatsClient regulationEnterpriseStatsClient;
    private final RegulationOperationStatsClient regulationOperationStatsClient;
    private final ComplaintStatsClient complaintStatsClient;
    private final WarningStatsClient warningStatsClient;
    private final QueryRedisCacheSupport queryRedisCacheSupport;
    private final String regulationInternalToken;
    private final String operationInternalToken;
    private final String complaintInternalToken;
    private final String warningInternalToken;

    public SupervisionOverviewQueryServiceImpl(RegulationEnterpriseStatsClient regulationEnterpriseStatsClient,
                                               RegulationOperationStatsClient regulationOperationStatsClient,
                                               ComplaintStatsClient complaintStatsClient,
                                               WarningStatsClient warningStatsClient,
                                               QueryRedisCacheSupport queryRedisCacheSupport,
                                               @Value("${regulation.internal.token:regulation-internal-token}")
                                               String regulationInternalToken,
                                               @Value("${regulation-operation.internal.token:regulation-operation-internal-token}")
                                               String operationInternalToken,
                                               @Value("${complaint.internal.token:complaint-internal-token}")
                                               String complaintInternalToken,
                                               @Value("${warning.internal.token:warning-internal-token}")
                                               String warningInternalToken) {
        this.regulationEnterpriseStatsClient = regulationEnterpriseStatsClient;
        this.regulationOperationStatsClient = regulationOperationStatsClient;
        this.complaintStatsClient = complaintStatsClient;
        this.warningStatsClient = warningStatsClient;
        this.queryRedisCacheSupport = queryRedisCacheSupport;
        this.regulationInternalToken = regulationInternalToken;
        this.operationInternalToken = operationInternalToken;
        this.complaintInternalToken = complaintInternalToken;
        this.warningInternalToken = warningInternalToken;
    }

    @Override
    public SupervisionOverviewVO getOverview(WarningStatsQueryDTO queryDTO) {
        WarningStatsQueryDTO scopeQuery = toScopeOnlyQuery(queryDTO);
        String cacheKey = queryRedisCacheSupport.buildKey(
            "query",
            "supervision",
            "overview",
            buildScopeFingerprint(scopeQuery)
        );
        return queryRedisCacheSupport.getOverviewOrLoad(cacheKey, () -> loadOverview(scopeQuery));
    }

    private SupervisionOverviewVO loadOverview(WarningStatsQueryDTO scopeQuery) {
        EnterpriseStatsOverviewVO enterpriseOverview = requireSuccess(
            regulationEnterpriseStatsClient.fetchOverview(scopeQuery, regulationInternalToken),
            "load enterprise overview failed"
        );
        OperationStatsOverviewVO operationOverview = requireSuccess(
            regulationOperationStatsClient.fetchOverview(scopeQuery, operationInternalToken),
            "load operation overview failed"
        );
        ComplaintStatsOverviewVO complaintOverview = requireSuccess(
            complaintStatsClient.fetchOverview(scopeQuery, complaintInternalToken),
            "load complaint overview failed"
        );
        WarningStatsOverviewVO warningOverview = requireSuccess(
            warningStatsClient.fetchOverview(scopeQuery, warningInternalToken),
            "load warning overview failed"
        );

        SupervisionOverviewVO overview = new SupervisionOverviewVO();
        overview.setEnterpriseTotalCount(zeroIfNull(enterpriseOverview.getTotalCount()));
        overview.setKeyEnterpriseCount(zeroIfNull(enterpriseOverview.getKeyEnterpriseCount()));
        overview.setApprovedEnterpriseCount(zeroIfNull(enterpriseOverview.getApprovedEnterpriseCount()));
        overview.setInspectionTotalCount(zeroIfNull(operationOverview.getInspectionTotalCount()));
        overview.setInspectionFailCount(zeroIfNull(operationOverview.getInspectionFailCount()));
        overview.setSamplingTotalCount(zeroIfNull(operationOverview.getSamplingTotalCount()));
        overview.setSamplingFailCount(zeroIfNull(operationOverview.getSamplingFailCount()));
        overview.setComplaintTotalCount(zeroIfNull(complaintOverview.getTotalCount()));
        overview.setComplaintFeedbackedCount(zeroIfNull(complaintOverview.getFeedbackedCount()));
        overview.setComplaintOverdueCount(zeroIfNull(complaintOverview.getOverdueCount()));
        overview.setOpenWarningCount(zeroIfNull(warningOverview.getOpenCount()));
        return overview;
    }

    private String buildScopeFingerprint(WarningStatsQueryDTO queryDTO) {
        WarningStatsQueryDTO query = queryDTO == null ? new WarningStatsQueryDTO() : queryDTO;
        Set<Long> regionIds = parseRegionIds(query.getRegionIds());
        String raw = String.join("|",
            formatTime(query.getStartTime()),
            formatTime(query.getEndTime()),
            normalizeText(query.getWarningType()),
            normalizeText(query.getBizType()),
            normalizeText(query.getLevel()),
            normalizeText(query.getStatus()),
            query.getRegionId() == null ? "" : String.valueOf(query.getRegionId()),
            regionIds.stream().map(String::valueOf).collect(Collectors.joining(",")),
            query.getOwnerRegulatorId() == null ? "" : String.valueOf(query.getOwnerRegulatorId()),
            query.getTopN() == null ? "" : String.valueOf(query.getTopN()),
            query.getTrendDays() == null ? "" : String.valueOf(query.getTrendDays()),
            query.getOverdueHours() == null ? "" : String.valueOf(query.getOverdueHours())
        );
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private WarningStatsQueryDTO toScopeOnlyQuery(WarningStatsQueryDTO source) {
        WarningStatsQueryDTO query = new WarningStatsQueryDTO();
        if (source == null) {
            return query;
        }
        query.setRegionId(source.getRegionId());
        query.setRegionIds(source.getRegionIds());
        query.setOwnerRegulatorId(source.getOwnerRegulatorId());
        return query;
    }

    private <T> T requireSuccess(ApiResponse<T> response, String defaultMessage) {
        if (response == null) {
            throw new IllegalStateException(defaultMessage);
        }
        if (response.getCode() != 0) {
            String message = StringUtils.hasText(response.getMessage()) ? response.getMessage() : defaultMessage;
            if (response.getCode() >= 500) {
                throw new IllegalStateException(message);
            }
            throw new IllegalArgumentException(message);
        }
        return response.getData();
    }

    private long zeroIfNull(Long value) {
        return value == null ? 0L : value;
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

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : KEY_TIME_FORMATTER.format(time);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
