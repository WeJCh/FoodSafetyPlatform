package com.mortal.regulation.controller.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.client.UserServiceClient;
import com.mortal.regulation.client.vo.UserVO;
import com.mortal.regulation.entity.AddrRegion;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.entity.FoodRegulator;
import com.mortal.regulation.entity.FoodRegulatorRegion;
import com.mortal.regulation.mapper.AddrRegionMapper;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.mapper.FoodRegulatorMapper;
import com.mortal.regulation.mapper.FoodRegulatorRegionMapper;
import com.mortal.regulation.support.RegulatorMasterCacheService;
import com.mortal.regulation.vo.internal.InternalRegulatorIdentityVO;
import com.mortal.regulation.vo.internal.InternalRegulatorSummaryVO;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/regulation/regulators")
public class InternalRegulatorController {

    private final FoodRegulatorMapper foodRegulatorMapper;
    private final FoodRegulatorRegionMapper foodRegulatorRegionMapper;
    private final AddrRegionMapper addrRegionMapper;
    private final FoodEnterpriseMapper foodEnterpriseMapper;
    private final RegulatorMasterCacheService regulatorMasterCacheService;
    private final UserServiceClient userServiceClient;

    public InternalRegulatorController(FoodRegulatorMapper foodRegulatorMapper,
                                       FoodRegulatorRegionMapper foodRegulatorRegionMapper,
                                       AddrRegionMapper addrRegionMapper,
                                       FoodEnterpriseMapper foodEnterpriseMapper,
                                       RegulatorMasterCacheService regulatorMasterCacheService,
                                       UserServiceClient userServiceClient) {
        this.foodRegulatorMapper = foodRegulatorMapper;
        this.foodRegulatorRegionMapper = foodRegulatorRegionMapper;
        this.addrRegionMapper = addrRegionMapper;
        this.foodEnterpriseMapper = foodEnterpriseMapper;
        this.regulatorMasterCacheService = regulatorMasterCacheService;
        this.userServiceClient = userServiceClient;
    }

    @GetMapping("/by-user/{userId}")
    public ApiResponse<InternalRegulatorIdentityVO> getByUserId(@PathVariable Long userId) {
        InternalRegulatorIdentityVO identity =
            regulatorMasterCacheService.getByUser(userId, () -> loadIdentityByUserId(userId));
        if (identity == null) {
            return ApiResponse.failure(404, "regulator not found");
        }
        return ApiResponse.success(identity);
    }

    @GetMapping("/{id}/identity")
    public ApiResponse<InternalRegulatorIdentityVO> getIdentityById(@PathVariable Long id) {
        InternalRegulatorIdentityVO identity =
            regulatorMasterCacheService.getIdentity(id, () -> loadIdentityById(id));
        if (identity == null) {
            return ApiResponse.failure(404, "regulator not found");
        }
        return ApiResponse.success(identity);
    }

    @GetMapping("/{id}")
    public ApiResponse<InternalRegulatorSummaryVO> getById(@PathVariable Long id) {
        InternalRegulatorSummaryVO summary =
            regulatorMasterCacheService.getSummary(id, () -> loadSummaryById(id));
        if (summary == null) {
            return ApiResponse.failure(404, "regulator not found");
        }
        return ApiResponse.success(summary);
    }

    @PostMapping("/summaries")
    public ApiResponse<List<InternalRegulatorSummaryVO>> summaries(@RequestBody(required = false) List<Long> ids) {
        List<Long> cleanedIds = sanitizeIds(ids);
        if (cleanedIds.isEmpty()) {
            return ApiResponse.success(List.of());
        }
        List<InternalRegulatorSummaryVO> result =
            regulatorMasterCacheService.getSummaries(cleanedIds, this::loadSummariesByIds);
        return ApiResponse.success(result);
    }

    @GetMapping("/query-ids-by-name")
    public ApiResponse<List<Long>> queryIdsByName(@RequestParam(required = false) String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return ApiResponse.success(List.of());
        }
        List<Long> ids = foodRegulatorMapper.selectList(new LambdaQueryWrapper<FoodRegulator>()
                .eq(FoodRegulator::getDeleted, 0)
                .like(FoodRegulator::getName, keyword.trim())
                .orderByAsc(FoodRegulator::getName))
            .stream()
            .map(FoodRegulator::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        return ApiResponse.success(ids);
    }

    @GetMapping("/{id}/scope-enterprise-ids")
    public ApiResponse<List<Long>> scopeEnterpriseIds(@PathVariable Long id) {
        FoodRegulator regulator = foodRegulatorMapper.selectById(id);
        if (regulator == null || isDeleted(regulator.getDeleted())) {
            return ApiResponse.failure(404, "regulator not found");
        }
        List<Long> enterpriseIds =
            regulatorMasterCacheService.getScopeEnterpriseIds(id, () -> loadScopeEnterpriseIds(id));
        return ApiResponse.success(enterpriseIds);
    }

    @GetMapping("/{id}/scope-region-ids")
    public ApiResponse<List<Long>> scopeRegionIds(@PathVariable Long id) {
        FoodRegulator regulator = foodRegulatorMapper.selectById(id);
        if (regulator == null || isDeleted(regulator.getDeleted())) {
            return ApiResponse.failure(404, "regulator not found");
        }
        return ApiResponse.success(
            regulatorMasterCacheService.getScopeRegionIds(id, () -> loadScopeRegionIds(id))
        );
    }

    private InternalRegulatorIdentityVO loadIdentityByUserId(Long userId) {
        FoodRegulator regulator = foodRegulatorMapper.selectOne(new LambdaQueryWrapper<FoodRegulator>()
            .eq(FoodRegulator::getUserId, userId)
            .eq(FoodRegulator::getDeleted, 0));
        if (regulator == null) {
            return null;
        }
        return toIdentityVO(regulator, findDirectRegionIds(regulator.getId()));
    }

    private InternalRegulatorIdentityVO loadIdentityById(Long id) {
        FoodRegulator regulator = foodRegulatorMapper.selectById(id);
        if (regulator == null || isDeleted(regulator.getDeleted())) {
            return null;
        }
        return toIdentityVO(regulator, findDirectRegionIds(regulator.getId()));
    }

    private InternalRegulatorSummaryVO loadSummaryById(Long id) {
        FoodRegulator regulator = foodRegulatorMapper.selectById(id);
        if (regulator == null || isDeleted(regulator.getDeleted())) {
            return null;
        }
        return toSummaryVO(regulator);
    }

    private List<InternalRegulatorSummaryVO> loadSummariesByIds(List<Long> ids) {
        List<FoodRegulator> regulators = foodRegulatorMapper.selectBatchIds(ids)
            .stream()
            .filter(Objects::nonNull)
            .filter(regulator -> !isDeleted(regulator.getDeleted()))
            .toList();
        if (regulators.isEmpty()) {
            return List.of();
        }
        Map<Long, FoodRegulator> regulatorMap = regulators.stream()
            .collect(Collectors.toMap(FoodRegulator::getId, Function.identity(), (a, b) -> a));
        return ids.stream()
            .map(regulatorMap::get)
            .filter(Objects::nonNull)
            .map(this::toSummaryVO)
            .toList();
    }

    private List<Long> loadScopeRegionIds(Long id) {
        List<Long> directRegionIds = findDirectRegionIds(id);
        if (directRegionIds.isEmpty()) {
            return List.of();
        }
        return collectRegionIds(directRegionIds);
    }

    private List<Long> loadScopeEnterpriseIds(Long id) {
        List<Long> scopeRegionIds = loadScopeRegionIds(id);
        if (scopeRegionIds.isEmpty()) {
            return List.of();
        }
        return foodEnterpriseMapper.selectList(new LambdaQueryWrapper<FoodEnterprise>()
                .eq(FoodEnterprise::getDeleted, 0)
                .in(FoodEnterprise::getRegionId, scopeRegionIds))
            .stream()
            .map(FoodEnterprise::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    @GetMapping("/{id}/assignable-to-region/{regionId}")
    public ApiResponse<Boolean> assignableToRegion(@PathVariable Long id, @PathVariable Long regionId) {
        FoodRegulator regulator = foodRegulatorMapper.selectById(id);
        if (regulator == null || isDeleted(regulator.getDeleted())) {
            return ApiResponse.failure(404, "regulator not found");
        }
        if (regionId == null) {
            return ApiResponse.success(false);
        }
        boolean matched = findDirectRegionIds(id).stream()
            .anyMatch(directRegionId -> isAncestorRegion(directRegionId, regionId));
        return ApiResponse.success(matched);
    }

    private InternalRegulatorIdentityVO toIdentityVO(FoodRegulator regulator, List<Long> regionIds) {
        InternalRegulatorIdentityVO vo = new InternalRegulatorIdentityVO();
        vo.setId(regulator.getId());
        vo.setUserId(regulator.getUserId());
        vo.setName(regulator.getName());
        vo.setUsername(resolveUsername(regulator.getUserId()));
        vo.setPhone(regulator.getPhone());
        vo.setRoleType(regulator.getRoleType());
        vo.setStatus(regulator.getStatus());
        vo.setRegionIds(regionIds == null ? List.of() : regionIds);
        return vo;
    }

    private InternalRegulatorSummaryVO toSummaryVO(FoodRegulator regulator) {
        InternalRegulatorSummaryVO vo = new InternalRegulatorSummaryVO();
        vo.setId(regulator.getId());
        vo.setUserId(regulator.getUserId());
        vo.setName(regulator.getName());
        vo.setUsername(resolveUsername(regulator.getUserId()));
        vo.setPhone(regulator.getPhone());
        vo.setRoleType(regulator.getRoleType());
        vo.setStatus(regulator.getStatus());
        return vo;
    }

    private String resolveUsername(Long userId) {
        if (userId == null) {
            return null;
        }
        ApiResponse<UserVO> response = userServiceClient.getUserById(userId);
        if (response == null || response.getCode() != 0 || response.getData() == null) {
            return null;
        }
        return response.getData().getUsername();
    }

    private List<Long> findDirectRegionIds(Long regulatorId) {
        if (regulatorId == null) {
            return List.of();
        }
        return foodRegulatorRegionMapper.selectList(new LambdaQueryWrapper<FoodRegulatorRegion>()
                .eq(FoodRegulatorRegion::getRegulatorId, regulatorId)
                .eq(FoodRegulatorRegion::getDeleted, 0))
            .stream()
            .map(FoodRegulatorRegion::getRegionId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private List<Long> collectRegionIds(List<Long> rootIds) {
        if (rootIds == null || rootIds.isEmpty()) {
            return List.of();
        }
        Set<Long> result = new LinkedHashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>(rootIds);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current == null || !result.add(current)) {
                continue;
            }
            List<AddrRegion> children = addrRegionMapper.selectList(new LambdaQueryWrapper<AddrRegion>()
                .eq(AddrRegion::getParentId, current)
                .eq(AddrRegion::getDeleted, 0));
            for (AddrRegion child : children) {
                if (child.getId() != null) {
                    queue.add(child.getId());
                }
            }
        }
        return result.stream().toList();
    }

    private boolean isAncestorRegion(Long ancestorId, Long regionId) {
        if (ancestorId == null || regionId == null) {
            return false;
        }
        Long cursor = regionId;
        while (cursor != null) {
            if (ancestorId.equals(cursor)) {
                return true;
            }
            AddrRegion current = addrRegionMapper.selectById(cursor);
            if (current == null || isDeleted(current.getDeleted())) {
                break;
            }
            cursor = current.getParentId();
        }
        return false;
    }

    private List<Long> sanitizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Long> cleaned = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) {
                cleaned.add(id);
            }
        }
        return cleaned.stream().toList();
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}

