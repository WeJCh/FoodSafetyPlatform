package com.mortal.query.service.impl;

import com.mortal.query.client.WarningStatsClient;
import com.mortal.query.common.ApiResponse;
import com.mortal.query.dto.WarningStatsQueryDTO;
import com.mortal.query.service.WarningStatsQueryService;
import com.mortal.query.vo.WarningEfficiencyStatsVO;
import com.mortal.query.vo.WarningStatsOverviewVO;
import com.mortal.query.vo.WarningTrendPointVO;
import com.mortal.query.vo.WarningTypeStatsVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WarningStatsQueryServiceImpl implements WarningStatsQueryService {

    private final WarningStatsClient warningStatsClient;
    private final String internalToken;

    public WarningStatsQueryServiceImpl(WarningStatsClient warningStatsClient,
                                        @Value("${warning.internal.token:warning-internal-token}")
                                        String internalToken) {
        this.warningStatsClient = warningStatsClient;
        this.internalToken = internalToken;
    }

    @Override
    public WarningStatsOverviewVO getOverview(WarningStatsQueryDTO queryDTO) {
        validateTimeRange(queryDTO);
        ApiResponse<WarningStatsOverviewVO> response = warningStatsClient.fetchOverview(queryDTO, internalToken);
        return requireSuccess(response, "load warning overview failed");
    }

    @Override
    public List<WarningTrendPointVO> getTrend(WarningStatsQueryDTO queryDTO) {
        validateTimeRange(queryDTO);
        ApiResponse<List<WarningTrendPointVO>> response = warningStatsClient.fetchTrend(queryDTO, internalToken);
        return requireSuccess(response, "load warning trend failed");
    }

    @Override
    public List<WarningTypeStatsVO> getTypeTop(WarningStatsQueryDTO queryDTO) {
        validateTimeRange(queryDTO);
        ApiResponse<List<WarningTypeStatsVO>> response = warningStatsClient.fetchTypes(queryDTO, internalToken);
        return requireSuccess(response, "load warning type stats failed");
    }

    @Override
    public WarningEfficiencyStatsVO getEfficiency(WarningStatsQueryDTO queryDTO) {
        validateTimeRange(queryDTO);
        ApiResponse<WarningEfficiencyStatsVO> response = warningStatsClient.fetchEfficiency(queryDTO, internalToken);
        return requireSuccess(response, "load warning efficiency failed");
    }

    private void validateTimeRange(WarningStatsQueryDTO queryDTO) {
        if (queryDTO == null) {
            return;
        }
        LocalDateTime startTime = queryDTO.getStartTime();
        LocalDateTime endTime = queryDTO.getEndTime();
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
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
}
