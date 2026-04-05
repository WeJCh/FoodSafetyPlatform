package com.mortal.regulation.operation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.operation.common.OperationErrorMessages;
import com.mortal.regulation.operation.common.RequestIdentity;
import com.mortal.regulation.operation.service.InspectionRecordService;
import com.mortal.regulation.operation.support.RequestIdentityResolver;
import com.mortal.regulation.operation.vo.InspectionRecordDetailVO;
import com.mortal.regulation.operation.vo.InspectionRecordVO;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regulation-operation/inspections")
public class InspectionRecordController {

    private final InspectionRecordService inspectionRecordService;
    private final RequestIdentityResolver requestIdentityResolver;

    public InspectionRecordController(InspectionRecordService inspectionRecordService,
                                      RequestIdentityResolver requestIdentityResolver) {
        this.inspectionRecordService = inspectionRecordService;
        this.requestIdentityResolver = requestIdentityResolver;
    }

    @GetMapping("/enterprise")
    public ApiResponse<PageResult<InspectionRecordVO>> listEnterprise(@RequestHeader("Authorization") String token,
                                                                      @RequestParam(required = false) String result,
                                                                      @RequestParam(required = false)
                                                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                      LocalDate startDate,
                                                                      @RequestParam(required = false)
                                                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                      LocalDate endDate,
                                                                      @RequestParam(defaultValue = "1") int page,
                                                                      @RequestParam(defaultValue = "10") int size) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, OperationErrorMessages.ENTERPRISE_ONLY);
        }
        return ApiResponse.success(
            inspectionRecordService.listForEnterprise(identity.userId(), result, startDate, endDate, page, size)
        );
    }

    @GetMapping("/enterprise/{id}")
    public ApiResponse<InspectionRecordDetailVO> detailEnterprise(@RequestHeader("Authorization") String token,
                                                                  @PathVariable Long id) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, OperationErrorMessages.ENTERPRISE_ONLY);
        }
        try {
            return ApiResponse.success(inspectionRecordService.getDetailForEnterprise(identity.userId(), id));
        } catch (IllegalArgumentException ex) {
            if (OperationErrorMessages.RECORD_NOT_FOUND.equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(404, OperationErrorMessages.RECORD_NOT_FOUND);
            }
            throw ex;
        }
    }

    @GetMapping("/my")
    public ApiResponse<PageResult<InspectionRecordVO>> listMy(@RequestHeader("Authorization") String token,
                                                              @RequestParam(required = false) String enterpriseName,
                                                              @RequestParam(required = false) String result,
                                                              @RequestParam(required = false)
                                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                              LocalDate startDate,
                                                              @RequestParam(required = false)
                                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                              LocalDate endDate,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(
            inspectionRecordService.listMy(identity.userId(), enterpriseName, result, startDate, endDate, page, size)
        );
    }

    @GetMapping
    public ApiResponse<PageResult<InspectionRecordVO>> list(@RequestHeader("Authorization") String token,
                                                            @RequestParam(required = false) String enterpriseName,
                                                            @RequestParam(required = false) String result,
                                                            @RequestParam(required = false)
                                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                            LocalDate startDate,
                                                            @RequestParam(required = false)
                                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                            LocalDate endDate,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(
            inspectionRecordService.listForAdmin(identity.userId(), enterpriseName, result, startDate, endDate, page, size)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<InspectionRecordDetailVO> detail(@RequestHeader("Authorization") String token,
                                                        @PathVariable Long id) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(inspectionRecordService.getDetail(identity.userId(), id));
    }
}
