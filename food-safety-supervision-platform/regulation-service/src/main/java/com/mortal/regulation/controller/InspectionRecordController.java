package com.mortal.regulation.controller;

import com.mortal.regulation.common.ApiResponse;
import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.service.InspectionRecordService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.InspectionRecordDetailVO;
import com.mortal.regulation.vo.InspectionRecordVO;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regulation/inspections")
public class InspectionRecordController {

    private final InspectionRecordService inspectionRecordService;
    private final JwtUserResolver jwtUserResolver;

    public InspectionRecordController(InspectionRecordService inspectionRecordService,
                                      JwtUserResolver jwtUserResolver) {
        this.inspectionRecordService = inspectionRecordService;
        this.jwtUserResolver = jwtUserResolver;
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
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
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
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(
            inspectionRecordService.listForAdmin(identity.userId(), enterpriseName, result, startDate, endDate, page, size)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<InspectionRecordDetailVO> detail(@RequestHeader("Authorization") String token,
                                                        @PathVariable Long id) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(inspectionRecordService.getDetail(identity.userId(), id));
    }

    private UserIdentity resolveIdentity(String token) {
        Long userId = jwtUserResolver.resolveUserId(token);
        String userType = jwtUserResolver.resolveUserType(token);
        if (userId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        return new UserIdentity(userId, userType);
    }

    private record UserIdentity(Long userId, String userType) {

        boolean isRegulator() {
            return "REGULATOR".equals(userType) || "ADMIN".equals(userType);
        }
    }
}
