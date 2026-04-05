package com.mortal.regulation.controller.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.dto.InternalStatsQueryDTO;
import com.mortal.regulation.entity.AddrRegion;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.entity.FoodRegulator;
import com.mortal.regulation.entity.FoodRegulatorRegion;
import com.mortal.regulation.mapper.AddrRegionMapper;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.mapper.FoodRegulatorMapper;
import com.mortal.regulation.mapper.FoodRegulatorRegionMapper;
import com.mortal.regulation.vo.internal.InternalEnterpriseStatsOverviewVO;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业内部统计接口。
 */
@RestController
@RequestMapping("/api/internal/regulation/enterprises")
public class InternalEnterpriseStatsController {

    private static final String STATUS_KEY = "KEY";
    private static final String APPROVAL_APPROVED = "APPROVED";

    private final FoodEnterpriseMapper foodEnterpriseMapper;
    private final FoodRegulatorMapper foodRegulatorMapper;
    private final FoodRegulatorRegionMapper foodRegulatorRegionMapper;
    private final AddrRegionMapper addrRegionMapper;

    public InternalEnterpriseStatsController(FoodEnterpriseMapper foodEnterpriseMapper,
                                             FoodRegulatorMapper foodRegulatorMapper,
                                             FoodRegulatorRegionMapper foodRegulatorRegionMapper,
                                             AddrRegionMapper addrRegionMapper) {
        this.foodEnterpriseMapper = foodEnterpriseMapper;
        this.foodRegulatorMapper = foodRegulatorMapper;
        this.foodRegulatorRegionMapper = foodRegulatorRegionMapper;
        this.addrRegionMapper = addrRegionMapper;
    }
    /**
     * 获取企业范围ID列表。
     * 
     * @param queryDTO 查询条件
     * @return 企业范围ID列表
     */
    @GetMapping("/scope-ids")
    public ApiResponse<List<Long>> scopeIds(InternalStatsQueryDTO queryDTO) {
        List<Long> regionIds = resolveScopeRegionIds(queryDTO);
        LambdaQueryWrapper<FoodEnterprise> wrapper = new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getDeleted, 0);
        if (!regionIds.isEmpty()) {
            wrapper.in(FoodEnterprise::getRegionId, regionIds);
        }
        List<Long> enterpriseIds = foodEnterpriseMapper.selectList(wrapper)
            .stream()
            .map(FoodEnterprise::getId)
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
        return ApiResponse.success(enterpriseIds);
    }
    /**
     * 获取企业统计概览。
     * 
     * @param queryDTO 查询条件
     * @return 企业统计概览
     */
    @GetMapping("/stats/overview")
    public ApiResponse<InternalEnterpriseStatsOverviewVO> overview(InternalStatsQueryDTO queryDTO) {
        List<Long> regionIds = resolveScopeRegionIds(queryDTO);

        InternalEnterpriseStatsOverviewVO overview = new InternalEnterpriseStatsOverviewVO();
        overview.setTotalCount(countEnterprises(regionIds, null, null));
        overview.setKeyEnterpriseCount(countEnterprises(regionIds, STATUS_KEY, null));
        overview.setApprovedEnterpriseCount(countEnterprises(regionIds, null, APPROVAL_APPROVED));
        return ApiResponse.success(overview);
    }

    /**
     * 统计企业数量。
     * 
     * @param regionIds 辖区ID列表
     * @param status 状态
     * @param approvalStatus 审批状态
     * @return 企业数量
     */
    private long countEnterprises(List<Long> regionIds, String status, String approvalStatus) {
        LambdaQueryWrapper<FoodEnterprise> wrapper = new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getDeleted, 0);
        if (!regionIds.isEmpty()) {
            wrapper.in(FoodEnterprise::getRegionId, regionIds);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(FoodEnterprise::getStatus, status);
        }
        if (StringUtils.hasText(approvalStatus)) {
            wrapper.eq(FoodEnterprise::getApprovalStatus, approvalStatus);
        }
        return foodEnterpriseMapper.selectCount(wrapper);
    }

    /**
     * 解析辖区ID列表。
     * 
     * @param queryDTO 查询条件
     * @return 辖区ID列表
     */
    private List<Long> resolveScopeRegionIds(InternalStatsQueryDTO queryDTO) {
        if (queryDTO == null) {
            return List.of();
        }
        if (queryDTO.getOwnerRegulatorId() != null) {
            return loadRegulatorScopeRegionIds(queryDTO.getOwnerRegulatorId());
        }
        if (queryDTO.getRegionId() != null && queryDTO.getRegionId() > 0) {
            return List.of(queryDTO.getRegionId());
        }
        return parseRegionIds(queryDTO.getRegionIds()).stream().toList();
    }

    /**
     * 加载监管机构辖区ID列表。
     * 
     * @param regulatorId 监管机构ID
     * @return 辖区ID列表
     */
    private List<Long> loadRegulatorScopeRegionIds(Long regulatorId) {
        if (regulatorId == null || regulatorId <= 0) {
            return List.of();
        }
        FoodRegulator regulator = foodRegulatorMapper.selectById(regulatorId);
        if (regulator == null || isDeleted(regulator.getDeleted())) {
            return List.of();
        }
        List<Long> directRegionIds = foodRegulatorRegionMapper.selectList(new LambdaQueryWrapper<FoodRegulatorRegion>()
                .eq(FoodRegulatorRegion::getRegulatorId, regulatorId)
                .eq(FoodRegulatorRegion::getDeleted, 0))
            .stream()
            .map(FoodRegulatorRegion::getRegionId)
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
        return collectRegionIds(directRegionIds);
    }

    /**
     * 收集辖区ID列表。
     * 
     * @param rootIds 根辖区ID列表
     * @return 辖区ID列表
     */
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
                if (child.getId() != null && child.getId() > 0) {
                    queue.add(child.getId());
                }
            }
        }
        return result.stream().toList();
    }

    /**
     * 解析辖区ID列表。
     * 
     * @param regionIds 辖区ID列表字符串
     * @return 辖区ID集合
     */
    private Set<Long> parseRegionIds(String regionIds) {
        if (!StringUtils.hasText(regionIds)) {
            return Set.of();
        }
        return List.of(regionIds.split(",")).stream()
            .map(String::trim)
            .filter(StringUtils::hasText)
            .map(text -> {
                try {
                    return Long.valueOf(text);
                } catch (NumberFormatException ex) {
                    return null;
                }
            })
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}
