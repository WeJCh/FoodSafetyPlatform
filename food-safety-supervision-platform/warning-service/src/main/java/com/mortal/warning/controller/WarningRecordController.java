package com.mortal.warning.controller;

import com.mortal.warning.common.ApiResponse;
import com.mortal.warning.common.PageResult;
import com.mortal.warning.dto.WarningProcessActionDTO;
import com.mortal.warning.dto.WarningRecordQueryDTO;
import com.mortal.warning.dto.WarningScopeDTO;
import com.mortal.warning.service.WarningEventService;
import com.mortal.warning.vo.WarningRecordDetailVO;
import com.mortal.warning.vo.WarningRecordVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预警中心对外接口。
 */
@RestController
@RequestMapping("/api/warning/warnings")
public class WarningRecordController {

    private final WarningEventService warningEventService;

    public WarningRecordController(WarningEventService warningEventService) {
        this.warningEventService = warningEventService;
    }

    /**
     * 分页查询预警列表。
     */
    @GetMapping
    public ApiResponse<PageResult<WarningRecordVO>> page(WarningRecordQueryDTO queryDTO) {
        return ApiResponse.success(warningEventService.pageWarningRecords(queryDTO));
    }

    /**
     * 查询预警详情（含处理日志）。
     */
    @GetMapping("/{id}")
    public ApiResponse<WarningRecordDetailVO> detail(@PathVariable("id") Long id,
                                                     @RequestHeader(value = "X-Scope-Owner-Regulator-Id",
                                                         required = false) Long ownerRegulatorId,
                                                     @RequestHeader(value = "X-Scope-Region-Ids",
                                                         required = false) String regionIds) {
        WarningScopeDTO scopeDTO = new WarningScopeDTO();
        scopeDTO.setOwnerRegulatorId(ownerRegulatorId);
        scopeDTO.setRegionIds(regionIds);
        return ApiResponse.success(warningEventService.getWarningRecordDetail(id, scopeDTO));
    }

    /**
     * 处理预警动作（签收/进入处理中/解决/关闭）。
     */
    @PostMapping("/{id}/actions")
    public ApiResponse<WarningRecordDetailVO> process(@PathVariable("id") Long id,
                                                      @Valid @RequestBody WarningProcessActionDTO actionDTO,
                                                      @RequestHeader(value = "X-Scope-Owner-Regulator-Id",
                                                          required = false) Long ownerRegulatorId,
                                                      @RequestHeader(value = "X-Scope-Region-Ids",
                                                          required = false) String regionIds,
                                                      @RequestHeader(value = "X-User-Id", required = false)
                                                      Long operatorId,
                                                      @RequestHeader(value = "X-Username", required = false)
                                                      String operatorName) {
        WarningScopeDTO scopeDTO = new WarningScopeDTO();
        scopeDTO.setOwnerRegulatorId(ownerRegulatorId);
        scopeDTO.setRegionIds(regionIds);
        return ApiResponse.success(warningEventService.processWarning(id, actionDTO, operatorId, operatorName, scopeDTO));
    }
}
