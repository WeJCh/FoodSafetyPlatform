package com.mortal.warning.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.warning.dto.WarningEventUpsertDTO;
import com.mortal.warning.service.WarningEventService;
import com.mortal.warning.vo.WarningRecordVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/warning/internal/events")
public class WarningInternalEventController {

    private final WarningEventService warningEventService;
    private final String internalToken;

    public WarningInternalEventController(WarningEventService warningEventService,
                                          @Value("${warning.internal.token:warning-internal-token}") String internalToken) {
        this.warningEventService = warningEventService;
        this.internalToken = internalToken;
    }

    /**
     * 内部预警事件幂等上报：相同 dedupKey 会合并触发次数，不重复建单。
     * @param dto 预警事件插入DTO
     * @return 预警记录VO
     */
    @PostMapping("/upsert")
    public ApiResponse<WarningRecordVO> upsert(@Valid @RequestBody WarningEventUpsertDTO dto,
                                               @RequestHeader(value = "X-Internal-Token", required = false)
                                               String token) {
        // 中文注释：internal 接口仅允许服务间调用，必须携带约定 token。
        if (!StringUtils.hasText(token) || !internalToken.equals(token.trim())) {
            return ApiResponse.failure(403, "forbidden");
        }
        return ApiResponse.success(warningEventService.upsertInternalEvent(dto));
    }
}
