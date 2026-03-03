package com.mortal.regulation.controller;

import com.mortal.regulation.common.ApiResponse;
import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.dto.RectificationReviewDTO;
import com.mortal.regulation.dto.RectificationSubmitDTO;
import com.mortal.regulation.service.RectificationService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.RectificationActionLogVO;
import com.mortal.regulation.vo.RectificationTaskVO;
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
@RequestMapping("/api/regulation/rectifications")
public class RectificationController {

    private final RectificationService rectificationService;
    private final JwtUserResolver jwtUserResolver;

    public RectificationController(RectificationService rectificationService, JwtUserResolver jwtUserResolver) {
        this.rectificationService = rectificationService;
        this.jwtUserResolver = jwtUserResolver;
    }

    @GetMapping("/my")
    public ApiResponse<PageResult<RectificationTaskVO>> listMy(@RequestHeader("Authorization") String token,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, "enterprise only");
        }
        return ApiResponse.success(rectificationService.listMy(identity.userId(), status, page, size));
    }

    @PutMapping("/my/{id}/submit")
    public ApiResponse<RectificationTaskVO> submitMy(@RequestHeader("Authorization") String token,
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody RectificationSubmitDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, "enterprise only");
        }
        return ApiResponse.success(rectificationService.submitMy(identity.userId(), id, dto));
    }

    @GetMapping
    public ApiResponse<PageResult<RectificationTaskVO>> list(@RequestHeader("Authorization") String token,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) String enterpriseName,
                                                             @RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(
            rectificationService.listForAdmin(identity.userId(), status, enterpriseName, page, size)
        );
    }

    /**
     * 获取整改任务详情
     * @param token 令牌
     * @param id 整改任务ID
     * @return 整改任务详情
     */
    @GetMapping("/{id}")
    public ApiResponse<RectificationTaskVO> detail(@RequestHeader("Authorization") String token,
                                                   @PathVariable Long id) {
        UserIdentity identity = resolveIdentity(token);
        return ApiResponse.success(rectificationService.getDetail(identity.userId(), identity.userType(), id));
    }

    /**
     * 获取整改任务操作日志
     * @param token 令牌
     * @param id 整改任务ID
     * @return 整改任务操作日志
     */
    @GetMapping("/{id}/actions")
    public ApiResponse<List<RectificationActionLogVO>> actions(@RequestHeader("Authorization") String token,
                                                               @PathVariable Long id) {
        UserIdentity identity = resolveIdentity(token);
        return ApiResponse.success(rectificationService.listActions(identity.userId(), identity.userType(), id));
    }

    /**
     * 审核整改任务
     * @param token 令牌
     * @param id 整改任务ID
     * @param dto 审核请求
     * @return 审核后的整改任务
     */
    @PostMapping("/{id}/review")
    public ApiResponse<RectificationTaskVO> review(@RequestHeader("Authorization") String token,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody RectificationReviewDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(rectificationService.review(identity.userId(), id, dto));
    }

    @PutMapping("/{id}/confirm")
    public ApiResponse<RectificationTaskVO> confirm(@RequestHeader("Authorization") String token,
                                                    @PathVariable Long id) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        RectificationReviewDTO dto = new RectificationReviewDTO();
        dto.setAction("CONFIRM");
        return ApiResponse.success(rectificationService.review(identity.userId(), id, dto));
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

        boolean isEnterprise() {
            return "ENTERPRISE".equals(userType);
        }

        boolean isRegulator() {
            return "REGULATOR".equals(userType) || "ADMIN".equals(userType);
        }
    }
}
