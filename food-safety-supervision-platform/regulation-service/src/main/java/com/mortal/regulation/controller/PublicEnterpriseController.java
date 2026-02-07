package com.mortal.regulation.controller;

import com.mortal.regulation.common.ApiResponse;
import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.service.EnterpriseProfileService;
import com.mortal.regulation.vo.PublicEnterpriseVO;
import org.springframework.web.bind.annotation.GetMapping;
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
}
