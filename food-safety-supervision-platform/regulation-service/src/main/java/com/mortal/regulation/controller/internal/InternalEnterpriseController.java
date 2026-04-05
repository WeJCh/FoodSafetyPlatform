package com.mortal.regulation.controller.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.dto.EnterpriseKeyReasonUpsertDTO;
import com.mortal.regulation.entity.AddrLocation;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.mapper.AddrLocationMapper;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.service.EnterpriseKeyReasonService;
import com.mortal.regulation.vo.internal.InternalEnterpriseDetailVO;
import com.mortal.regulation.vo.internal.InternalEnterpriseSummaryVO;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;
/**
 * 内部企业控制器
 */
/**
    这个“内部只读能力”，本质上不是给前端用的功能，而是给 complaint-service 用的“内部查询接口”。

    你现在看到的这两个类：
    InternalEnterpriseController.java
    InternalRegulatorController.java

    它们的作用是：
    regulation-service 继续拥有“企业/监管员主数据”的所有权
    complaint-service 不再直接查 food_enterprise、food_regulator、food_regulator_region、addr_region 这些表
    complaint-service 需要这些数据时，只能通过这些内部接口来拿
    这就是“内部只读能力”。

    加 InternalEnterpriseController 和 InternalRegulatorController，不是为了多写两个 Controller，而是为了保证拆分后：
    投诉服务不再直接碰监管库表
    企业/监管员/辖区数据仍由监管服务统一拥有
    数据权限和辖区规则不被复制到投诉服务
    后续独立拆库仍然成立
 */
@RestController
@RequestMapping("/api/internal/regulation/enterprises")
public class InternalEnterpriseController {

    private final FoodEnterpriseMapper foodEnterpriseMapper;
    private final AddrLocationMapper addrLocationMapper;
    private final EnterpriseKeyReasonService enterpriseKeyReasonService;

    public InternalEnterpriseController(FoodEnterpriseMapper foodEnterpriseMapper,
                                        AddrLocationMapper addrLocationMapper,
                                        EnterpriseKeyReasonService enterpriseKeyReasonService) {
        this.foodEnterpriseMapper = foodEnterpriseMapper;
        this.addrLocationMapper = addrLocationMapper;
        this.enterpriseKeyReasonService = enterpriseKeyReasonService;
    }

    @GetMapping("/{id}")
    public ApiResponse<InternalEnterpriseDetailVO> getById(@PathVariable Long id) {
        FoodEnterprise enterprise = foodEnterpriseMapper.selectById(id);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            return ApiResponse.failure(404, "enterprise not found");
        }
        return ApiResponse.success(toDetailVO(enterprise, loadAddressDetail(enterprise.getAddressId())));
    }

    @GetMapping("/by-user/{userId}")
    public ApiResponse<InternalEnterpriseDetailVO> getByUserId(@PathVariable Long userId) {
        FoodEnterprise enterprise = foodEnterpriseMapper.selectOne(new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getUserId, userId)
            .eq(FoodEnterprise::getDeleted, 0));
        if (enterprise == null) {
            return ApiResponse.failure(404, "enterprise not found");
        }
        return ApiResponse.success(toDetailVO(enterprise, loadAddressDetail(enterprise.getAddressId())));
    }

    @PostMapping("/summaries")
    public ApiResponse<List<InternalEnterpriseSummaryVO>> summaries(@RequestBody(required = false) List<Long> ids) {
        List<Long> cleanedIds = sanitizeIds(ids);
        if (cleanedIds.isEmpty()) {
            return ApiResponse.success(List.of());
        }
        List<FoodEnterprise> enterprises = foodEnterpriseMapper.selectBatchIds(cleanedIds)
            .stream()
            .filter(Objects::nonNull)
            .filter(enterprise -> !isDeleted(enterprise.getDeleted()))
            .toList();
        if (enterprises.isEmpty()) {
            return ApiResponse.success(List.of());
        }
        Map<Long, FoodEnterprise> enterpriseMap = enterprises.stream()
            .collect(Collectors.toMap(FoodEnterprise::getId, Function.identity(), (a, b) -> a));
        Map<Long, String> addressMap = loadAddressMap(enterprises);
        List<InternalEnterpriseSummaryVO> result = cleanedIds.stream()
            .map(enterpriseMap::get)
            .filter(Objects::nonNull)
            .map(enterprise -> toSummaryVO(enterprise, addressMap.get(enterprise.getAddressId())))
            .toList();
        return ApiResponse.success(result);
    }

    @GetMapping("/query-ids-by-name")
    public ApiResponse<List<Long>> queryIdsByName(@RequestParam(required = false) String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return ApiResponse.success(List.of());
        }
        List<Long> ids = foodEnterpriseMapper.selectList(new LambdaQueryWrapper<FoodEnterprise>()
                .eq(FoodEnterprise::getDeleted, 0)
                .like(FoodEnterprise::getEnterpriseName, keyword.trim())
                .orderByAsc(FoodEnterprise::getEnterpriseName))
            .stream()
            .map(FoodEnterprise::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        return ApiResponse.success(ids);
    }

    /**
     * 标记企业为关键企业
     * @param id 企业ID
     * @param dto 企业关键原因插入DTO
     * @return 空响应
     */
    @PostMapping("/{id}/key-reasons")
    public ApiResponse<Void> markAsKey(@PathVariable Long id,
                                       @RequestBody EnterpriseKeyReasonUpsertDTO dto) {
        if (dto == null) {
            return ApiResponse.failure(400, "request body required");
        }
        enterpriseKeyReasonService.markEnterpriseAsKey(
            id,
            dto.getReasonType(),
            dto.getReasonDetail(),
            dto.getSourceType(),
            dto.getSourceId(),
            dto.getOperatorId()
        );
        return ApiResponse.success(null);
    }

    private InternalEnterpriseDetailVO toDetailVO(FoodEnterprise enterprise, String addressDetail) {
        InternalEnterpriseDetailVO vo = new InternalEnterpriseDetailVO();
        vo.setId(enterprise.getId());
        vo.setUserId(enterprise.getUserId());
        vo.setEnterpriseName(enterprise.getEnterpriseName());
        vo.setLicenseNo(enterprise.getLicenseNo());
        vo.setRegionId(enterprise.getRegionId());
        vo.setAddressId(enterprise.getAddressId());
        vo.setAddressDetail(addressDetail);
        vo.setPrincipal(enterprise.getPrincipal());
        vo.setPrincipalPhone(enterprise.getPrincipalPhone());
        vo.setRegulatorName(enterprise.getRegulatorName());
        vo.setStatus(enterprise.getStatus());
        vo.setApprovalStatus(enterprise.getApprovalStatus());
        return vo;
    }

    private InternalEnterpriseSummaryVO toSummaryVO(FoodEnterprise enterprise, String addressDetail) {
        InternalEnterpriseSummaryVO vo = new InternalEnterpriseSummaryVO();
        vo.setId(enterprise.getId());
        vo.setEnterpriseName(enterprise.getEnterpriseName());
        vo.setRegionId(enterprise.getRegionId());
        vo.setAddressDetail(addressDetail);
        return vo;
    }

    private Map<Long, String> loadAddressMap(List<FoodEnterprise> enterprises) {
        if (enterprises == null || enterprises.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> addressIds = enterprises.stream()
            .map(FoodEnterprise::getAddressId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (addressIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return addrLocationMapper.selectBatchIds(addressIds)
            .stream()
            .filter(Objects::nonNull)
            .filter(location -> !isDeleted(location.getDeleted()))
            .collect(Collectors.toMap(AddrLocation::getId, AddrLocation::getDetail, (a, b) -> a));
    }

    private String loadAddressDetail(Long addressId) {
        if (addressId == null) {
            return null;
        }
        AddrLocation location = addrLocationMapper.selectById(addressId);
        if (location == null || isDeleted(location.getDeleted())) {
            return null;
        }
        return location.getDetail();
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
