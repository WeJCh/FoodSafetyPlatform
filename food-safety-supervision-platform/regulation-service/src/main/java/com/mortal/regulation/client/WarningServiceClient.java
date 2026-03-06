package com.mortal.regulation.client;

import com.mortal.regulation.common.ApiResponse;
import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.dto.WarningProcessActionDTO;
import com.mortal.regulation.dto.WarningRecordQueryDTO;
import com.mortal.regulation.dto.WarningEventUpsertDTO;
import com.mortal.regulation.vo.WarningRecordDetailVO;
import com.mortal.regulation.vo.WarningRecordVO;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("warning-service")
public interface WarningServiceClient {

    @PostMapping("/api/warning/internal/events/upsert")
    ApiResponse<Map<String, Object>> upsertInternalEvent(@RequestBody WarningEventUpsertDTO dto,
                                                         @RequestHeader("X-Internal-Token") String internalToken);

    @GetMapping("/api/warning/warnings")
    ApiResponse<PageResult<WarningRecordVO>> pageRecords(@SpringQueryMap WarningRecordQueryDTO queryDTO);

    @GetMapping("/api/warning/warnings/{id}")
    ApiResponse<WarningRecordDetailVO> detail(@PathVariable("id") Long id,
                                              @RequestHeader(value = "X-Scope-Owner-Regulator-Id", required = false)
                                              String ownerRegulatorId,
                                              @RequestHeader(value = "X-Scope-Region-Ids", required = false)
                                              String regionIds);

    @PostMapping("/api/warning/warnings/{id}/actions")
    ApiResponse<WarningRecordDetailVO> process(@PathVariable("id") Long id,
                                               @RequestBody WarningProcessActionDTO dto,
                                               @RequestHeader(value = "X-Scope-Owner-Regulator-Id", required = false)
                                               String ownerRegulatorId,
                                               @RequestHeader(value = "X-Scope-Region-Ids", required = false)
                                               String regionIds,
                                               @RequestHeader(value = "X-User-Id", required = false)
                                               String operatorUserId,
                                               @RequestHeader(value = "X-Username", required = false)
                                               String operatorName);
}
