package com.mortal.regulation.controller.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.entity.AddrRegion;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.entity.FoodRegulator;
import com.mortal.regulation.entity.FoodRegulatorRegion;
import com.mortal.regulation.mapper.AddrRegionMapper;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.mapper.FoodRegulatorMapper;
import com.mortal.regulation.mapper.FoodRegulatorRegionMapper;
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

    public InternalRegulatorController(FoodRegulatorMapper foodRegulatorMapper,
                                       FoodRegulatorRegionMapper foodRegulatorRegionMapper,
                                       AddrRegionMapper addrRegionMapper,
                                       FoodEnterpriseMapper foodEnterpriseMapper) {
        this.foodRegulatorMapper = foodRegulatorMapper;
        this.foodRegulatorRegionMapper = foodRegulatorRegionMapper;
        this.addrRegionMapper = addrRegionMapper;
        this.foodEnterpriseMapper = foodEnterpriseMapper;
    }

    @GetMapping("/by-user/{userId}")
    public ApiResponse<InternalRegulatorIdentityVO> getByUserId(@PathVariable Long userId) {
        FoodRegulator regulator = foodRegulatorMapper.selectOne(new LambdaQueryWrapper<FoodRegulator>()
            .eq(FoodRegulator::getUserId, userId)
            .eq(FoodRegulator::getDeleted, 0));
        if (regulator == null) {
            return ApiResponse.failure(404, "regulator not found");
        }
        return ApiResponse.success(toIdentityVO(regulator, findDirectRegionIds(regulator.getId())));
    }

    @GetMapping("/{id}/identity")
    public ApiResponse<InternalRegulatorIdentityVO> getIdentityById(@PathVariable Long id) {
        FoodRegulator regulator = foodRegulatorMapper.selectById(id);
        if (regulator == null || isDeleted(regulator.getDeleted())) {
            return ApiResponse.failure(404, "regulator not found");
        }
        return ApiResponse.success(toIdentityVO(regulator, findDirectRegionIds(regulator.getId())));
    }

    @GetMapping("/{id}")
    public ApiResponse<InternalRegulatorSummaryVO> getById(@PathVariable Long id) {
        FoodRegulator regulator = foodRegulatorMapper.selectById(id);
        if (regulator == null || isDeleted(regulator.getDeleted())) {
            return ApiResponse.failure(404, "regulator not found");
        }
        return ApiResponse.success(toSummaryVO(regulator));
    }

    @PostMapping("/summaries")
    public ApiResponse<List<InternalRegulatorSummaryVO>> summaries(@RequestBody(required = false) List<Long> ids) {
        List<Long> cleanedIds = sanitizeIds(ids);
        if (cleanedIds.isEmpty()) {
            return ApiResponse.success(List.of());
        }
        List<FoodRegulator> regulators = foodRegulatorMapper.selectBatchIds(cleanedIds)
            .stream()
            .filter(Objects::nonNull)
            .filter(regulator -> !isDeleted(regulator.getDeleted()))
            .toList();
        if (regulators.isEmpty()) {
            return ApiResponse.success(List.of());
        }
        Map<Long, FoodRegulator> regulatorMap = regulators.stream()
            .collect(Collectors.toMap(FoodRegulator::getId, Function.identity(), (a, b) -> a));
        List<InternalRegulatorSummaryVO> result = cleanedIds.stream()
            .map(regulatorMap::get)
            .filter(Objects::nonNull)
            .map(this::toSummaryVO)
            .toList();
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
        List<Long> directRegionIds = findDirectRegionIds(id);
        if (directRegionIds.isEmpty()) {
            return ApiResponse.success(List.of());
        }
        List<Long> scopeRegionIds = collectRegionIds(directRegionIds);
        if (scopeRegionIds.isEmpty()) {
            return ApiResponse.success(List.of());
        }
        List<Long> enterpriseIds = foodEnterpriseMapper.selectList(new LambdaQueryWrapper<FoodEnterprise>()
                .eq(FoodEnterprise::getDeleted, 0)
                .in(FoodEnterprise::getRegionId, scopeRegionIds))
            .stream()
            .map(FoodEnterprise::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        return ApiResponse.success(enterpriseIds);
    }

    @GetMapping("/{id}/scope-region-ids")
    public ApiResponse<List<Long>> scopeRegionIds(@PathVariable Long id) {
        FoodRegulator regulator = foodRegulatorMapper.selectById(id);
        if (regulator == null || isDeleted(regulator.getDeleted())) {
            return ApiResponse.failure(404, "regulator not found");
        }
        List<Long> directRegionIds = findDirectRegionIds(id);
        if (directRegionIds.isEmpty()) {
            return ApiResponse.success(List.of());
        }
        return ApiResponse.success(collectRegionIds(directRegionIds));
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
            .anyMatch(directRegionId -> isAncestorRegion(regionId, directRegionId));
        return ApiResponse.success(matched);
    }

    private InternalRegulatorIdentityVO toIdentityVO(FoodRegulator regulator, List<Long> regionIds) {
        InternalRegulatorIdentityVO vo = new InternalRegulatorIdentityVO();
        vo.setId(regulator.getId());
        vo.setUserId(regulator.getUserId());
        vo.setName(regulator.getName());
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
        vo.setPhone(regulator.getPhone());
        vo.setRoleType(regulator.getRoleType());
        vo.setStatus(regulator.getStatus());
        return vo;
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

