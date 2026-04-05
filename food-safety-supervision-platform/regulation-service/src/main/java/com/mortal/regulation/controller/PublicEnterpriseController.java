package com.mortal.regulation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.service.EnterpriseProfileService;
import com.mortal.regulation.vo.PublicEnterpriseDetailVO;
import com.mortal.regulation.vo.PublicEnterpriseVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共企业控制器
 */
@RestController
@RequestMapping("/api/regulation/public/enterprises")
public class PublicEnterpriseController {

    private final EnterpriseProfileService enterpriseProfileService;

    public PublicEnterpriseController(EnterpriseProfileService enterpriseProfileService) {
        this.enterpriseProfileService = enterpriseProfileService;
    }

    /**
     * 获取公共企业列表
     * @param enterpriseName 企业名称
     * @param page 页码
     * @param size 每页大小
     * @return 公共企业列表
     */
    @GetMapping
    public ApiResponse<PageResult<PublicEnterpriseVO>> list(@RequestParam(required = false) String enterpriseName,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(enterpriseProfileService.listPublic(enterpriseName, page, size));
    }

    /**
     * 获取公共企业详情
     * @param id 企业ID
     * @return 公共企业详情
     */
    @GetMapping("/{id}")
    public ApiResponse<PublicEnterpriseDetailVO> detail(@PathVariable Long id) {
        PublicEnterpriseDetailVO detail = enterpriseProfileService.getPublicById(id);
        if (detail == null) {
            return ApiResponse.failure(404, "public enterprise not found");
        }
        return ApiResponse.success(detail);
    }
}

