package com.mortal.regulation.operation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.operation.common.OperationErrorMessages;
import com.mortal.regulation.operation.common.RequestIdentity;
import com.mortal.regulation.operation.dto.RectificationReviewDTO;
import com.mortal.regulation.operation.dto.RectificationSubmitDTO;
import com.mortal.regulation.operation.service.RectificationService;
import com.mortal.regulation.operation.support.RequestIdentityResolver;
import com.mortal.regulation.operation.vo.RectificationActionLogVO;
import com.mortal.regulation.operation.vo.RectificationTaskVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regulation-operation/rectifications")
public class RectificationController {

    private final RectificationService rectificationService;
    private final RequestIdentityResolver requestIdentityResolver;

    public RectificationController(RectificationService rectificationService,
                                   RequestIdentityResolver requestIdentityResolver) {
        this.rectificationService = rectificationService;
        this.requestIdentityResolver = requestIdentityResolver;
    }

    @GetMapping("/my")
    public ApiResponse<PageResult<RectificationTaskVO>> listMy(@RequestHeader("Authorization") String token,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(required = false) String slaFilter,
                                                               @RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, OperationErrorMessages.ENTERPRISE_ONLY);
        }
        return ApiResponse.success(rectificationService.listMy(identity.userId(), status, slaFilter, page, size));
    }

    @PutMapping("/my/{id}/submit")
    public ApiResponse<RectificationTaskVO> submitMy(@RequestHeader("Authorization") String token,
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody RectificationSubmitDTO dto) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, OperationErrorMessages.ENTERPRISE_ONLY);
        }
        return ApiResponse.success(rectificationService.submitMy(identity.userId(), id, dto));
    }

    @GetMapping
    public ApiResponse<PageResult<RectificationTaskVO>> list(@RequestHeader("Authorization") String token,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) String enterpriseName,
                                                             @RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(
            rectificationService.listForAdmin(identity.userId(), status, enterpriseName, page, size)
        );
    }

    @GetMapping("/regulator/my")
    public ApiResponse<PageResult<RectificationTaskVO>> listForEnforcer(@RequestHeader("Authorization") String token,
                                                                        @RequestParam(required = false) String status,
                                                                        @RequestParam(required = false) String enterpriseName,
                                                                        @RequestParam(defaultValue = "1") int page,
                                                                        @RequestParam(defaultValue = "10") int size) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(
            rectificationService.listForEnforcer(identity.userId(), status, enterpriseName, page, size)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<RectificationTaskVO> detail(@RequestHeader("Authorization") String token,
                                                   @PathVariable Long id) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        return ApiResponse.success(rectificationService.getDetail(identity.userId(), identity.userType(), id));
    }

    @GetMapping("/{id}/actions")
    public ApiResponse<List<RectificationActionLogVO>> actions(@RequestHeader("Authorization") String token,
                                                               @PathVariable Long id) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        return ApiResponse.success(rectificationService.listActions(identity.userId(), identity.userType(), id));
    }

    @GetMapping("/actions/recent")
    public ApiResponse<List<RectificationActionLogVO>> recentActions(@RequestHeader("Authorization") String token,
                                                                     @RequestParam(required = false) Integer limit) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        return ApiResponse.success(rectificationService.listRecentActions(identity.userId(), identity.userType(), limit));
    }

    @PostMapping("/{id}/review")
    public ApiResponse<RectificationTaskVO> review(@RequestHeader("Authorization") String token,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody RectificationReviewDTO dto) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(rectificationService.review(identity.userId(), id, dto));
    }

    @PutMapping("/{id}/confirm")
    public ApiResponse<RectificationTaskVO> confirm(@RequestHeader("Authorization") String token,
                                                    @PathVariable Long id) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        RectificationReviewDTO dto = new RectificationReviewDTO();
        dto.setAction("CONFIRM");
        return ApiResponse.success(rectificationService.review(identity.userId(), id, dto));
    }
}
