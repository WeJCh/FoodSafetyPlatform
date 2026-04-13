package com.mortal.regulation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.service.BulletinService;
import com.mortal.regulation.vo.BulletinDetailVO;
import com.mortal.regulation.vo.BulletinVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regulation/public/bulletins")
public class PublicBulletinController {

    private final BulletinService bulletinService;

    public PublicBulletinController(BulletinService bulletinService) {
        this.bulletinService = bulletinService;
    }

    @GetMapping
    public ApiResponse<PageResult<BulletinVO>> list(@RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String category,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(bulletinService.listPublic(keyword, category, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<BulletinDetailVO> detail(@PathVariable Long id) {
        BulletinDetailVO detail = bulletinService.getPublicDetail(id);
        if (detail == null) {
            return ApiResponse.failure(404, "public bulletin not found");
        }
        return ApiResponse.success(detail);
    }
}
