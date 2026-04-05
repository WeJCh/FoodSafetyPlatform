package com.mortal.complaint.controller;

import com.mortal.complaint.application.ComplaintStatsService;
import com.mortal.complaint.dto.InternalStatsQueryDTO;
import com.mortal.complaint.vo.InternalComplaintStatsOverviewVO;
import com.mortal.platform.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 投诉内部统计接口。
 */
@RestController
@RequestMapping("/api/complaint/internal/stats")
public class InternalComplaintStatsController {

    private final ComplaintStatsService complaintStatsService;
    private final String internalToken;

    public InternalComplaintStatsController(ComplaintStatsService complaintStatsService,
                                            @Value("${complaint.internal.token:complaint-internal-token}")
                                            String internalToken) {
        this.complaintStatsService = complaintStatsService;
        this.internalToken = internalToken;
    }

    /**
     * 获取投诉统计概览。
     * 
     * @param queryDTO 查询条件
     * @param token 内部令牌
     * @return 投诉统计概览
     */
    @GetMapping("/overview")
    public ApiResponse<InternalComplaintStatsOverviewVO> overview(InternalStatsQueryDTO queryDTO,
                                                                  @RequestHeader(value = "X-Internal-Token",
                                                                      required = false) String token) {
        if (!isAllowed(token)) {
            return ApiResponse.failure(403, "forbidden");
        }
        return ApiResponse.success(complaintStatsService.getOverview(queryDTO));
    }

    private boolean isAllowed(String token) {
        return StringUtils.hasText(token) && internalToken.equals(token.trim());
    }
}
