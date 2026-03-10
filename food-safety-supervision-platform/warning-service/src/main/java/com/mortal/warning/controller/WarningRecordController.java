package com.mortal.warning.controller;

import com.mortal.warning.common.ApiResponse;
import com.mortal.warning.common.PageResult;
import com.mortal.warning.dto.WarningActionCommentDTO;
import com.mortal.warning.dto.WarningAssignDTO;
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
     * 指派处理人。
     */
    @PostMapping("/{id}/assign")
    public ApiResponse<WarningRecordDetailVO> assign(@PathVariable("id") Long id,
                                                     @Valid @RequestBody WarningAssignDTO assignDTO,
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
        return ApiResponse.success(warningEventService.assignWarning(id, assignDTO, operatorId, operatorName, scopeDTO));
    }

    /**
     * 标记处理中。
     */
    @PostMapping("/{id}/process")
    public ApiResponse<WarningRecordDetailVO> processSingle(@PathVariable("id") Long id,
                                                            @RequestBody(required = false) WarningActionCommentDTO body,
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
        return ApiResponse.success(warningEventService.processWarningAction(
            id,
            "PROCESS",
            body == null ? null : body.getActionComment(),
            operatorId,
            operatorName,
            scopeDTO
        ));
    }

    /**
     * 标记已解决。
     */
    @PostMapping("/{id}/resolve")
    public ApiResponse<WarningRecordDetailVO> resolve(@PathVariable("id") Long id,
                                                      @RequestBody(required = false) WarningActionCommentDTO body,
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
        return ApiResponse.success(warningEventService.processWarningAction(
            id,
            "RESOLVE",
            body == null ? null : body.getActionComment(),
            operatorId,
            operatorName,
            scopeDTO
        ));
    }

}
