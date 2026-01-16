package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mortal.regulation.client.UserServiceClient;
import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.dto.EnterpriseApprovalDTO;
import com.mortal.regulation.dto.EnterpriseProfileDTO;
import com.mortal.regulation.entity.AddrLocation;
import com.mortal.regulation.entity.AddrRegion;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.mapper.AddrLocationMapper;
import com.mortal.regulation.mapper.AddrRegionMapper;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.service.EnterpriseProfileService;
import com.mortal.regulation.vo.EnterpriseProfileVO;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EnterpriseProfileServiceImpl implements EnterpriseProfileService {

    private static final String STATUS_NORMAL = "NORMAL";
    private static final String APPROVAL_PENDING = "PENDING";
    private static final String APPROVAL_APPROVED = "APPROVED";
    private static final String APPROVAL_REJECTED = "REJECTED";

    private final FoodEnterpriseMapper foodEnterpriseMapper;
    private final AddrLocationMapper addrLocationMapper;
    private final AddrRegionMapper addrRegionMapper;
    private final UserServiceClient userServiceClient;

    public EnterpriseProfileServiceImpl(FoodEnterpriseMapper foodEnterpriseMapper,
                                        AddrLocationMapper addrLocationMapper,
                                        AddrRegionMapper addrRegionMapper,
                                        UserServiceClient userServiceClient) {
        this.foodEnterpriseMapper = foodEnterpriseMapper;
        this.addrLocationMapper = addrLocationMapper;
        this.addrRegionMapper = addrRegionMapper;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public EnterpriseProfileVO submitProfile(Long userId, EnterpriseProfileDTO dto) {
        FoodEnterprise enterprise = findEnterpriseByUserId(userId);
        if (enterprise == null) {
            enterprise = new FoodEnterprise();
            enterprise.setUserId(userId);
            enterprise.setStatus(STATUS_NORMAL);
            enterprise.setCreateTime(LocalDateTime.now());
        }
        requireRegion(dto.getRegionId());
        AddrLocation location = upsertLocation(enterprise.getAddressId(), dto.getRegionId(), dto.getAddressDetail());
        enterprise.setEnterpriseName(dto.getEnterpriseName());
        enterprise.setLicenseNo(dto.getLicenseNo());
        enterprise.setRegionId(dto.getRegionId());
        enterprise.setAddressId(location.getId());
        enterprise.setPrincipal(dto.getPrincipal());
        enterprise.setPrincipalPhone(dto.getPrincipalPhone());
        enterprise.setApprovalStatus(APPROVAL_PENDING);
        enterprise.setApprovalComment(null);
        enterprise.setApprovedBy(null);
        enterprise.setApprovedTime(null);
        enterprise.setUpdateTime(LocalDateTime.now());
        if (enterprise.getDeleted() == null) {
            enterprise.setDeleted(0);
        }

        if (enterprise.getId() == null) {
            foodEnterpriseMapper.insert(enterprise);
        } else {
            foodEnterpriseMapper.updateById(enterprise);
        }

        return toVO(enterprise, location.getDetail());
    }

    @Override
    public EnterpriseProfileVO getProfile(Long userId) {
        FoodEnterprise enterprise = findEnterpriseByUserId(userId);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            return null;
        }
        return toVO(enterprise, resolveAddressDetail(enterprise.getAddressId()));
    }

    @Override
    public EnterpriseProfileVO getById(Long enterpriseId) {
        FoodEnterprise enterprise = foodEnterpriseMapper.selectById(enterpriseId);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            return null;
        }
        return toVO(enterprise, resolveAddressDetail(enterprise.getAddressId()));
    }

    @Override
    public PageResult<EnterpriseProfileVO> list(String enterpriseName,
                                                String status,
                                                String approvalStatus,
                                                int page,
                                                int size) {
        var wrapper = new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getDeleted, 0);
        if (StringUtils.hasText(enterpriseName)) {
            wrapper.like(FoodEnterprise::getEnterpriseName, enterpriseName.trim());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(FoodEnterprise::getStatus, normalize(status));
        }
        if (StringUtils.hasText(approvalStatus)) {
            wrapper.eq(FoodEnterprise::getApprovalStatus, normalize(approvalStatus));
        }
        wrapper.orderByDesc(FoodEnterprise::getUpdateTime);
        Page<FoodEnterprise> pageInfo = foodEnterpriseMapper.selectPage(new Page<>(page, size), wrapper);
        List<FoodEnterprise> enterprises = pageInfo.getRecords();
        Map<Long, String> addressMap = loadAddressDetails(enterprises);
        List<EnterpriseProfileVO> records = enterprises.stream()
            .map(enterprise -> toVO(enterprise, addressMap.get(enterprise.getAddressId())))
            .toList();
        return PageResult.of(records, pageInfo.getTotal(), page, size);
    }

    @Override
    public List<EnterpriseProfileVO> listPending() {
        List<FoodEnterprise> enterprises = foodEnterpriseMapper.selectList(new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getApprovalStatus, APPROVAL_PENDING)
            .eq(FoodEnterprise::getDeleted, 0));
        Map<Long, String> addressMap = loadAddressDetails(enterprises);
        return enterprises.stream()
            .map(enterprise -> toVO(enterprise, addressMap.get(enterprise.getAddressId())))
            .toList();
    }

    @Override
    public EnterpriseProfileVO approve(Long enterpriseId, Long operatorId, EnterpriseApprovalDTO dto) {
        FoodEnterprise enterprise = requireEnterprise(enterpriseId);
        applyApproval(enterprise, APPROVAL_APPROVED, operatorId, dto);
        return toVO(enterprise, resolveAddressDetail(enterprise.getAddressId()));
    }

    @Override
    public EnterpriseProfileVO reject(Long enterpriseId, Long operatorId, EnterpriseApprovalDTO dto) {
        FoodEnterprise enterprise = requireEnterprise(enterpriseId);
        applyApproval(enterprise, APPROVAL_REJECTED, operatorId, dto);
        return toVO(enterprise, resolveAddressDetail(enterprise.getAddressId()));
    }

    @Override
    public void deleteEnterprise(Long enterpriseId) {
        FoodEnterprise enterprise = requireEnterprise(enterpriseId);
        enterprise.setDeleted(1);
        enterprise.setUpdateTime(LocalDateTime.now());
        foodEnterpriseMapper.updateById(enterprise);
        markAddressDeleted(enterprise.getAddressId());
        if (enterprise.getUserId() != null) {
            userServiceClient.deleteUser(enterprise.getUserId());
        }
    }

    @Override
    public void deleteEnterpriseByUserId(Long userId) {
        FoodEnterprise enterprise = findEnterpriseByUserId(userId);
        if (enterprise == null) {
            throw new IllegalArgumentException("enterprise not found");
        }
        deleteEnterprise(enterprise.getId());
    }

    private FoodEnterprise findEnterpriseByUserId(Long userId) {
        return foodEnterpriseMapper.selectOne(new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getUserId, userId)
            .eq(FoodEnterprise::getDeleted, 0));
    }

    private FoodEnterprise requireEnterprise(Long enterpriseId) {
        FoodEnterprise enterprise = foodEnterpriseMapper.selectById(enterpriseId);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            throw new IllegalArgumentException("enterprise not found");
        }
        return enterprise;
    }

    private void requireRegion(Long regionId) {
        if (regionId == null) {
            throw new IllegalArgumentException("regionId required");
        }
        AddrRegion region = addrRegionMapper.selectById(regionId);
        if (region == null || isDeleted(region.getDeleted())) {
            throw new IllegalArgumentException("region not found");
        }
    }

    private AddrLocation upsertLocation(Long addressId, Long regionId, String detail) {
        String cleanedDetail = StringUtils.hasText(detail) ? detail.trim() : detail;
        if (addressId != null) {
            AddrLocation location = addrLocationMapper.selectById(addressId);
            if (location != null && !isDeleted(location.getDeleted())) {
                location.setRegionId(regionId);
                location.setDetail(cleanedDetail);
                addrLocationMapper.updateById(location);
                return location;
            }
        }
        AddrLocation location = new AddrLocation();
        location.setRegionId(regionId);
        location.setDetail(cleanedDetail);
        location.setDeleted(0);
        addrLocationMapper.insert(location);
        return location;
    }

    private String resolveAddressDetail(Long addressId) {
        if (addressId == null) {
            return null;
        }
        AddrLocation location = addrLocationMapper.selectById(addressId);
        if (location == null || isDeleted(location.getDeleted())) {
            return null;
        }
        return location.getDetail();
    }

    private void markAddressDeleted(Long addressId) {
        if (addressId == null) {
            return;
        }
        AddrLocation location = addrLocationMapper.selectById(addressId);
        if (location == null || isDeleted(location.getDeleted())) {
            return;
        }
        location.setDeleted(1);
        addrLocationMapper.updateById(location);
    }

    private void applyApproval(FoodEnterprise enterprise, String status, Long operatorId, EnterpriseApprovalDTO dto) {
        enterprise.setApprovalStatus(status);
        enterprise.setApprovalComment(dto == null ? null : dto.getComment());
        enterprise.setApprovedBy(operatorId);
        enterprise.setApprovedTime(LocalDateTime.now());
        if (dto != null && StringUtils.hasText(dto.getRegulatorName())) {
            enterprise.setRegulatorName(dto.getRegulatorName());
        }
        enterprise.setUpdateTime(LocalDateTime.now());
        foodEnterpriseMapper.updateById(enterprise);
    }

    private Map<Long, String> loadAddressDetails(List<FoodEnterprise> enterprises) {
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
            .filter(location -> !isDeleted(location.getDeleted()))
            .collect(Collectors.toMap(AddrLocation::getId, AddrLocation::getDetail, (a, b) -> a));
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }

    private EnterpriseProfileVO toVO(FoodEnterprise enterprise, String addressDetail) {
        EnterpriseProfileVO vo = new EnterpriseProfileVO();
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
        vo.setApprovalComment(enterprise.getApprovalComment());
        vo.setApprovedBy(enterprise.getApprovedBy());
        vo.setApprovedTime(enterprise.getApprovedTime());
        vo.setCreateTime(enterprise.getCreateTime());
        vo.setUpdateTime(enterprise.getUpdateTime());
        return vo;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }
}
