package com.mortal.regulation.controller;

import com.mortal.regulation.common.ApiResponse;
import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.dto.ComplaintAssignDTO;
import com.mortal.regulation.dto.ComplaintHandleDTO;
import com.mortal.regulation.dto.ComplaintSubmitDTO;
import com.mortal.regulation.service.ComplaintService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.ComplaintDetailVO;
import com.mortal.regulation.vo.ComplaintTrackVO;
import com.mortal.regulation.vo.ComplaintVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 投诉控制器
 */
@RestController
@RequestMapping("/api/regulation/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final JwtUserResolver jwtUserResolver;

    public ComplaintController(ComplaintService complaintService, JwtUserResolver jwtUserResolver) {
        this.complaintService = complaintService;
        this.jwtUserResolver = jwtUserResolver;
    }
    /**
     * 提交投诉
     * @param dto 投诉提交DTO
     * @return 投诉跟踪VO
     */
    @PostMapping("/public")
    public ApiResponse<ComplaintTrackVO> submitPublic(@Valid @RequestBody ComplaintSubmitDTO dto) {
        return ApiResponse.success(complaintService.submitPublic(dto));
    }
    /**
     * 跟踪投诉
     * @param complaintNo 投诉编号
     * @param contact 联系方式
     * @return 投诉跟踪VO
     */
    @GetMapping("/track")
    public ApiResponse<ComplaintTrackVO> track(@RequestParam String complaintNo,
                                               @RequestParam(required = false) String contact) {
        return ApiResponse.success(complaintService.track(complaintNo, contact));
    }
    
    /**
     * 查询投诉列表
     * @param token 令牌
     * @param status 状态
     * @param enterpriseName 企业名称
     * @param assignedToName 被指派去处理投诉的执行人姓名
     * @param assignedByName 指派监管员名称
     * @param page 页码
     * @param size 每页条数
     * @return 投诉列表
     */
    @GetMapping
    public ApiResponse<PageResult<ComplaintVO>> list(@RequestHeader("Authorization") String token,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String enterpriseName,
                                                     @RequestParam(required = false) String assignedToName,
                                                     @RequestParam(required = false) String assignedByName,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(
            complaintService.list(identity.userId(), status, enterpriseName, assignedToName, assignedByName, page, size)
        );
    }

    /**
     * 查询投诉详情
     * @param token 令牌
     * @param id 投诉ID
     * @return 投诉详情VO
     */
    @GetMapping("/{id}")
    public ApiResponse<ComplaintDetailVO> detail(@RequestHeader("Authorization") String token,
                                                 @PathVariable Long id) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(complaintService.getDetail(identity.userId(), id));
    }
    /**
     * 接受投诉
     * @param token 令牌
     * @param id 投诉ID
     * @return 投诉VO
     */
    @PutMapping("/{id}/accept")
    public ApiResponse<ComplaintVO> accept(@RequestHeader("Authorization") String token,
                                           @PathVariable Long id) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(complaintService.accept(identity.userId(), id));
    }

    /**
     * 指派投诉
     * @param token 令牌
     * @param id 投诉ID
     * @param dto 投诉指派DTO
     * @return 投诉VO
     */
    @PutMapping("/{id}/assign")
    public ApiResponse<ComplaintVO> assign(@RequestHeader("Authorization") String token,
                                           @PathVariable Long id,
                                           @Valid @RequestBody ComplaintAssignDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(complaintService.assign(identity.userId(), id, dto));
    }

    /**
     * 开始处理投诉
     * @param token 令牌
     * @param id 投诉ID
     * @return 投诉VO
     */
    @PutMapping("/{id}/process")
    public ApiResponse<ComplaintVO> startProcess(@RequestHeader("Authorization") String token,
                                                 @PathVariable Long id) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(complaintService.startProcess(identity.userId(), id));
    }

    /**
     * 处理投诉
     * @param token 令牌
     * @param id 投诉ID
     * @param dto 投诉处理DTO
     * @return 投诉VO
     */
    @PostMapping("/{id}/handle")
    public ApiResponse<ComplaintVO> handle(@RequestHeader("Authorization") String token,
                                           @PathVariable Long id,
                                           @Valid @RequestBody ComplaintHandleDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(complaintService.handle(identity.userId(), id, dto));
    }
    /**
     * 解析用户身份
     * @param token 令牌
     * @return 用户身份
     */
    private UserIdentity resolveIdentity(String token) {
        Long userId = jwtUserResolver.resolveUserId(token);
        String userType = jwtUserResolver.resolveUserType(token);
        if (userId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        return new UserIdentity(userId, userType);
    }
    /**
     * 用户身份
     * @param userId 用户ID
     * @param userType 用户类型
     */
    private record UserIdentity(Long userId, String userType) {

        boolean isRegulator() {
            return "REGULATOR".equals(userType) || "ADMIN".equals(userType);
        }
    }
}
