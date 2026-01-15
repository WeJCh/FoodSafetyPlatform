package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mortal.regulation.client.UserServiceClient;
import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.dto.EnterpriseApprovalDTO;
import com.mortal.regulation.dto.EnterpriseProfileDTO;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.service.EnterpriseProfileService;
import com.mortal.regulation.vo.EnterpriseProfileVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EnterpriseProfileServiceImpl implements EnterpriseProfileService {

    private static final String STATUS_NORMAL = "NORMAL";
    private static final String APPROVAL_PENDING = "PENDING";
    private static final String APPROVAL_APPROVED = "APPROVED";
    private static final String APPROVAL_REJECTED = "REJECTED";

    private final FoodEnterpriseMapper foodEnterpriseMapper;
    private final UserServiceClient userServiceClient;

    public EnterpriseProfileServiceImpl(FoodEnterpriseMapper foodEnterpriseMapper,
                                        UserServiceClient userServiceClient) {
        this.foodEnterpriseMapper = foodEnterpriseMapper;
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
        enterprise.setEnterpriseName(dto.getEnterpriseName());
        enterprise.setLicenseNo(dto.getLicenseNo());
        enterprise.setAddress(dto.getAddress());
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

        return toVO(enterprise);
    }

    @Override
    public EnterpriseProfileVO getProfile(Long userId) {
        FoodEnterprise enterprise = findEnterpriseByUserId(userId);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            return null;
        }
        return toVO(enterprise);
    }

    @Override
    public EnterpriseProfileVO getById(Long enterpriseId) {
        FoodEnterprise enterprise = foodEnterpriseMapper.selectById(enterpriseId);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            return null;
        }
        return toVO(enterprise);
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
        List<EnterpriseProfileVO> records = pageInfo.getRecords()
            .stream()
            .map(this::toVO)
            .toList();
        return PageResult.of(records, pageInfo.getTotal(), page, size);
    }

    @Override
    public List<EnterpriseProfileVO> listPending() {
        return foodEnterpriseMapper.selectList(new LambdaQueryWrapper<FoodEnterprise>()
                .eq(FoodEnterprise::getApprovalStatus, APPROVAL_PENDING)
                .eq(FoodEnterprise::getDeleted, 0))
            .stream()
            .map(this::toVO)
            .toList();
    }

    @Override
    public EnterpriseProfileVO approve(Long enterpriseId, Long operatorId, EnterpriseApprovalDTO dto) {
        FoodEnterprise enterprise = requireEnterprise(enterpriseId);
        applyApproval(enterprise, APPROVAL_APPROVED, operatorId, dto);
        return toVO(enterprise);
    }

    @Override
    public EnterpriseProfileVO reject(Long enterpriseId, Long operatorId, EnterpriseApprovalDTO dto) {
        FoodEnterprise enterprise = requireEnterprise(enterpriseId);
        applyApproval(enterprise, APPROVAL_REJECTED, operatorId, dto);
        return toVO(enterprise);
    }

    @Override
    public void deleteEnterprise(Long enterpriseId) {
        FoodEnterprise enterprise = requireEnterprise(enterpriseId);
        enterprise.setDeleted(1);
        enterprise.setUpdateTime(LocalDateTime.now());
        foodEnterpriseMapper.updateById(enterprise);
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

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }

    private EnterpriseProfileVO toVO(FoodEnterprise enterprise) {
        EnterpriseProfileVO vo = new EnterpriseProfileVO();
        vo.setId(enterprise.getId());
        vo.setUserId(enterprise.getUserId());
        vo.setEnterpriseName(enterprise.getEnterpriseName());
        vo.setLicenseNo(enterprise.getLicenseNo());
        vo.setAddress(enterprise.getAddress());
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
