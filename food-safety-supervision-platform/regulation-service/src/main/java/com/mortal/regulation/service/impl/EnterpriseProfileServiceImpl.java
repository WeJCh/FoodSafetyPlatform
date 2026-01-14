package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.regulation.dto.EnterpriseApprovalDTO;
import com.mortal.regulation.dto.EnterpriseProfileDTO;
import com.mortal.regulation.entity.EnterpriseAccount;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.mapper.EnterpriseAccountMapper;
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
    private final EnterpriseAccountMapper enterpriseAccountMapper;

    public EnterpriseProfileServiceImpl(FoodEnterpriseMapper foodEnterpriseMapper,
                                        EnterpriseAccountMapper enterpriseAccountMapper) {
        this.foodEnterpriseMapper = foodEnterpriseMapper;
        this.enterpriseAccountMapper = enterpriseAccountMapper;
    }

    @Override
    public EnterpriseProfileVO submitProfile(Long userId, EnterpriseProfileDTO dto) {
        EnterpriseAccount account = findAccount(userId);
        FoodEnterprise enterprise = account == null ? null : foodEnterpriseMapper.selectById(account.getEnterpriseId());
        if (enterprise == null) {
            enterprise = new FoodEnterprise();
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

        if (account == null) {
            EnterpriseAccount newAccount = new EnterpriseAccount();
            newAccount.setUserId(userId);
            newAccount.setEnterpriseId(enterprise.getId());
            newAccount.setCreateTime(LocalDateTime.now());
            newAccount.setUpdateTime(LocalDateTime.now());
            newAccount.setDeleted(0);
            enterpriseAccountMapper.insert(newAccount);
        } else if (!enterprise.getId().equals(account.getEnterpriseId())) {
            account.setEnterpriseId(enterprise.getId());
            account.setUpdateTime(LocalDateTime.now());
            enterpriseAccountMapper.updateById(account);
        }

        return toVO(enterprise);
    }

    @Override
    public EnterpriseProfileVO getProfile(Long userId) {
        EnterpriseAccount account = findAccount(userId);
        if (account == null) {
            return null;
        }
        FoodEnterprise enterprise = foodEnterpriseMapper.selectById(account.getEnterpriseId());
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            return null;
        }
        return toVO(enterprise);
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

    private EnterpriseAccount findAccount(Long userId) {
        return enterpriseAccountMapper.selectOne(new LambdaQueryWrapper<EnterpriseAccount>()
            .eq(EnterpriseAccount::getUserId, userId)
            .eq(EnterpriseAccount::getDeleted, 0));
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
}
