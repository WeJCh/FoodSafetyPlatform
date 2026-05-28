package com.mortal.regulation.client;

import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.dto.WarningActionCommentDTO;
import com.mortal.regulation.dto.WarningAssignDTO;
import com.mortal.regulation.dto.WarningRecordQueryDTO;
import com.mortal.regulation.vo.WarningProcessLogVO;
import com.mortal.regulation.vo.WarningRecordDetailVO;
import com.mortal.regulation.vo.WarningRecordVO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("warning-service")
public interface WarningServiceClient {

    @GetMapping("/api/warning/warnings")
    ApiResponse<PageResult<WarningRecordVO>> pageRecords(@SpringQueryMap WarningRecordQueryDTO queryDTO);

    @GetMapping("/api/warning/warnings/{id}")
    ApiResponse<WarningRecordDetailVO> detail(@PathVariable("id") Long id,
                                              @RequestHeader(value = "X-Scope-Assigned-Regulator-Id", required = false)
                                              String assignedRegulatorId,
                                              @RequestHeader(value = "X-Scope-Region-Ids", required = false)
                                              String regionIds);

    @GetMapping("/api/warning/warnings/logs/recent")
    ApiResponse<List<WarningProcessLogVO>> recentLogs(
        @RequestHeader(value = "X-Scope-Assigned-Regulator-Id", required = false) String assignedRegulatorId,
        @RequestHeader(value = "X-Scope-Region-Ids", required = false) String regionIds,
        @RequestParam(value = "limit", required = false) Integer limit
    );

    @PostMapping("/api/warning/warnings/{id}/process")
    ApiResponse<WarningRecordDetailVO> process(@PathVariable("id") Long id,
                                               @RequestBody WarningActionCommentDTO dto,
                                               @RequestHeader(value = "X-Scope-Assigned-Regulator-Id", required = false)
                                               String assignedRegulatorId,
                                               @RequestHeader(value = "X-Scope-Region-Ids", required = false)
                                               String regionIds,
                                               @RequestHeader(value = "X-User-Id", required = false)
                                               String operatorUserId,
                                               @RequestHeader(value = "X-Username", required = false)
                                               String operatorName);

    @PostMapping("/api/warning/warnings/{id}/resolve")
    ApiResponse<WarningRecordDetailVO> resolve(@PathVariable("id") Long id,
                                               @RequestBody WarningActionCommentDTO dto,
                                               @RequestHeader(value = "X-Scope-Assigned-Regulator-Id", required = false)
                                               String assignedRegulatorId,
                                               @RequestHeader(value = "X-Scope-Region-Ids", required = false)
                                               String regionIds,
                                               @RequestHeader(value = "X-User-Id", required = false)
                                               String operatorUserId,
                                               @RequestHeader(value = "X-Username", required = false)
                                               String operatorName);

    @PostMapping("/api/warning/warnings/{id}/assign")
    ApiResponse<WarningRecordDetailVO> assign(@PathVariable("id") Long id,
                                              @RequestBody WarningAssignDTO dto,
                                              @RequestHeader(value = "X-Scope-Region-Ids", required = false)
                                              String regionIds,
                                              @RequestHeader(value = "X-User-Id", required = false)
                                              String operatorUserId,
                                              @RequestHeader(value = "X-Username", required = false)
                                              String operatorName);
}
