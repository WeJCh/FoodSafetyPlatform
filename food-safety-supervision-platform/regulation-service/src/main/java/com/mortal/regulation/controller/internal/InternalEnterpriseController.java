package com.mortal.regulation.controller.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.dto.EnterpriseKeyReasonUpsertDTO;
import com.mortal.regulation.entity.AddrLocation;
import com.mortal.regulation.entity.EnterpriseProfileAttachment;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.mapper.AddrLocationMapper;
import com.mortal.regulation.mapper.EnterpriseProfileAttachmentMapper;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.service.EnterpriseKeyReasonService;
import com.mortal.regulation.support.EnterpriseMasterCacheService;
import com.mortal.regulation.vo.EnterpriseProfileAttachmentVO;
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
 * 企业内部接口。
 */
@RestController
@RequestMapping("/api/internal/regulation/enterprises")
public class InternalEnterpriseController {

    private final FoodEnterpriseMapper foodEnterpriseMapper;
    private final EnterpriseProfileAttachmentMapper enterpriseProfileAttachmentMapper;
    private final AddrLocationMapper addrLocationMapper;
    private final EnterpriseKeyReasonService enterpriseKeyReasonService;
    private final EnterpriseMasterCacheService enterpriseMasterCacheService;

    public InternalEnterpriseController(FoodEnterpriseMapper foodEnterpriseMapper,
                                        EnterpriseProfileAttachmentMapper enterpriseProfileAttachmentMapper,
                                        AddrLocationMapper addrLocationMapper,
                                        EnterpriseKeyReasonService enterpriseKeyReasonService,
                                        EnterpriseMasterCacheService enterpriseMasterCacheService) {
        this.foodEnterpriseMapper = foodEnterpriseMapper;
        this.enterpriseProfileAttachmentMapper = enterpriseProfileAttachmentMapper;
        this.addrLocationMapper = addrLocationMapper;
        this.enterpriseKeyReasonService = enterpriseKeyReasonService;
        this.enterpriseMasterCacheService = enterpriseMasterCacheService;
    }

    @GetMapping("/{id}")
    public ApiResponse<InternalEnterpriseDetailVO> getById(@PathVariable Long id) {
        InternalEnterpriseDetailVO detail = enterpriseMasterCacheService.getDetail(id, () -> loadDetailById(id));
        if (detail == null) {
            return ApiResponse.failure(404, "enterprise not found");
        }
        return ApiResponse.success(detail);
    }

    @GetMapping("/by-user/{userId}")
    public ApiResponse<InternalEnterpriseDetailVO> getByUserId(@PathVariable Long userId) {
        InternalEnterpriseDetailVO detail = enterpriseMasterCacheService.getByUser(userId, () -> loadDetailByUserId(userId));
        if (detail == null) {
            return ApiResponse.failure(404, "enterprise not found");
        }
        return ApiResponse.success(detail);
    }

    @PostMapping("/summaries")
    public ApiResponse<List<InternalEnterpriseSummaryVO>> summaries(@RequestBody(required = false) List<Long> ids) {
        List<Long> cleanedIds = sanitizeIds(ids);
        if (cleanedIds.isEmpty()) {
            return ApiResponse.success(List.of());
        }
        List<InternalEnterpriseSummaryVO> result =
            enterpriseMasterCacheService.getSummaries(cleanedIds, this::loadSummariesByIds);
        return ApiResponse.success(result);
    }

    private InternalEnterpriseDetailVO loadDetailById(Long id) {
        FoodEnterprise enterprise = foodEnterpriseMapper.selectById(id);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            return null;
        }
        return toDetailVO(enterprise, loadAddressDetail(enterprise.getAddressId()));
    }

    private InternalEnterpriseDetailVO loadDetailByUserId(Long userId) {
        FoodEnterprise enterprise = foodEnterpriseMapper.selectOne(new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getUserId, userId)
            .eq(FoodEnterprise::getDeleted, 0));
        if (enterprise == null) {
            return null;
        }
        return toDetailVO(enterprise, loadAddressDetail(enterprise.getAddressId()));
    }

    private List<InternalEnterpriseSummaryVO> loadSummariesByIds(List<Long> ids) {
        List<FoodEnterprise> enterprises = foodEnterpriseMapper.selectBatchIds(ids)
            .stream()
            .filter(Objects::nonNull)
            .filter(enterprise -> !isDeleted(enterprise.getDeleted()))
            .toList();
        if (enterprises.isEmpty()) {
            return List.of();
        }
        Map<Long, FoodEnterprise> enterpriseMap = enterprises.stream()
            .collect(Collectors.toMap(FoodEnterprise::getId, Function.identity(), (a, b) -> a));
        Map<Long, String> addressMap = loadAddressMap(enterprises);
        return ids.stream()
            .map(enterpriseMap::get)
            .filter(Objects::nonNull)
            .map(enterprise -> toSummaryVO(enterprise, addressMap.get(enterprise.getAddressId())))
            .toList();
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
     * 标记企业为关键企业。
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
        vo.setCreditCode(enterprise.getCreditCode());
        vo.setLegalRepresentative(enterprise.getLegalRepresentative());
        vo.setRegionId(enterprise.getRegionId());
        vo.setAddressId(enterprise.getAddressId());
        vo.setAddressDetail(addressDetail);
        vo.setPrincipal(enterprise.getPrincipal());
        vo.setPrincipalPhone(enterprise.getPrincipalPhone());
        vo.setRegulatorId(enterprise.getRegulatorId());
        vo.setRegulatorName(enterprise.getRegulatorName());
        vo.setStatus(enterprise.getStatus());
        vo.setApprovalStatus(enterprise.getApprovalStatus());
        vo.setAttachments(loadAttachments(enterprise.getId()));
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

    private List<EnterpriseProfileAttachmentVO> loadAttachments(Long enterpriseId) {
        if (enterpriseId == null) {
            return List.of();
        }
        return enterpriseProfileAttachmentMapper.selectList(new LambdaQueryWrapper<EnterpriseProfileAttachment>()
                .eq(EnterpriseProfileAttachment::getEnterpriseId, enterpriseId)
                .eq(EnterpriseProfileAttachment::getDeleted, 0)
                .orderByAsc(EnterpriseProfileAttachment::getAttachmentType)
                .orderByAsc(EnterpriseProfileAttachment::getId))
            .stream()
            .map(this::toAttachmentVO)
            .toList();
    }

    private EnterpriseProfileAttachmentVO toAttachmentVO(EnterpriseProfileAttachment attachment) {
        EnterpriseProfileAttachmentVO vo = new EnterpriseProfileAttachmentVO();
        vo.setId(attachment.getId());
        vo.setType(attachment.getAttachmentType());
        vo.setLabel(resolveAttachmentLabel(attachment.getAttachmentType()));
        vo.setName(attachment.getAttachmentName());
        vo.setUrl(attachment.getAttachmentUrl());
        vo.setUploadedBy(attachment.getUploadedBy());
        vo.setUploadedAt(attachment.getUploadedAt());
        return vo;
    }

    private String resolveAttachmentLabel(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        return switch (type.trim()) {
            case "businessLicense" -> "营业执照";
            case "foodPermit" -> "食品经营许可证";
            case "onsitePhoto" -> "经营场所照片";
            default -> type.trim();
        };
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

