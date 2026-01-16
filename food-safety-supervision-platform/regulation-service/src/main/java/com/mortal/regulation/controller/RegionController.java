package com.mortal.regulation.controller;

import com.mortal.regulation.common.ApiResponse;
import com.mortal.regulation.dto.RegionCreateDTO;
import com.mortal.regulation.service.RegionService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.RegionVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regulation/regions")
public class RegionController {

    private final RegionService regionService;
    private final JwtUserResolver jwtUserResolver;

    public RegionController(RegionService regionService, JwtUserResolver jwtUserResolver) {
        this.regionService = regionService;
        this.jwtUserResolver = jwtUserResolver;
    }

    @PostMapping
    public ApiResponse<RegionVO> create(@RequestHeader("Authorization") String token,
                                        @Valid @RequestBody RegionCreateDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isAdmin()) {
            return ApiResponse.failure(403, "admin only");
        }
        return ApiResponse.success(regionService.create(dto));
    }

    @GetMapping
    public ApiResponse<List<RegionVO>> list(@RequestHeader("Authorization") String token,
                                            @RequestParam(required = false) Long parentId) {
        resolveIdentity(token);
        return ApiResponse.success(regionService.listByParentId(parentId));
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

        boolean isAdmin() {
            return "ADMIN".equals(userType);
        }
    }
}
