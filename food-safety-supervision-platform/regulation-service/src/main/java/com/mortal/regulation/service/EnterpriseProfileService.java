package com.mortal.regulation.service;

import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.dto.EnterpriseApprovalBatchDTO;
import com.mortal.regulation.dto.EnterpriseApprovalDTO;
import com.mortal.regulation.dto.EnterpriseProfileDTO;
import com.mortal.regulation.vo.BatchActionResult;
import com.mortal.regulation.vo.EnterpriseProfileVO;
import com.mortal.regulation.vo.PublicEnterpriseVO;
import java.util.List;

public interface EnterpriseProfileService {

    EnterpriseProfileVO submitProfile(Long userId, EnterpriseProfileDTO dto);

    EnterpriseProfileVO getProfile(Long userId);

    EnterpriseProfileVO getById(Long enterpriseId);

    /**
     * 获取企业列表
     * @param enterpriseName 企业名称
     * @param status 企业状态
     * @param approvalStatus 审核状态
     * @param page 页码
     * @param size 每页大小
     * @return 企业列表
     */
    PageResult<EnterpriseProfileVO> list(String enterpriseName,
                                         String status,
                                         String approvalStatus,
                                         int page,
                                         int size);

    /**
     * 获取监管者视角的企业列表
     * @param userId 监管者ID
     * @param enterpriseName 企业名称
     * @param status 企业状态
     * @param approvalStatus 审核状态
     * @param page 页码
     * @param size 每页大小
     * @return 企业列表
     */
    PageResult<EnterpriseProfileVO> listForRegulator(Long userId,
                                                     String enterpriseName,
                                                     String status,
                                                     String approvalStatus,
                                                     int page,
                                                     int size);

    /**
     * 获取公共企业列表
     * @param enterpriseName 企业名称
     * @param page 页码
     * @param size 每页大小
     * @return 公共企业列表
     */
    PageResult<PublicEnterpriseVO> listPublic(String enterpriseName, int page, int size);

    /**
     * 获取待审核企业列表
     * @return 企业列表
     */
    List<EnterpriseProfileVO> listPending();

    /**
     * 获取监管者视角的待审核企业列表
     * @param userId 监管者ID
     * @return 企业列表
     */
    List<EnterpriseProfileVO> listPendingForRegulator(Long userId);

    /**
     * 审核企业
     * @param enterpriseId 企业ID
     * @param operatorId 操作员ID
     * @param dto 审核DTO
     * @return 企业信息VO
     */
    EnterpriseProfileVO approve(Long enterpriseId, Long operatorId, EnterpriseApprovalDTO dto);

    /**
     * 拒绝企业
     * @param enterpriseId 企业ID
     * @param operatorId 操作员ID
     * @param dto 拒绝DTO
     * @return 企业信息VO
     */
    EnterpriseProfileVO reject(Long enterpriseId, Long operatorId, EnterpriseApprovalDTO dto);

    /**
     * 批量审核企业
     * @param operatorId 操作员ID
     * @param dto 审核DTO
     * @return 批量审核结果
     */
    BatchActionResult approveBatch(Long operatorId, EnterpriseApprovalBatchDTO dto);

    /**
     * 批量拒绝企业
     * @param operatorId 操作员ID
     * @param dto 拒绝DTO
     * @return 批量拒绝结果
     */
    BatchActionResult rejectBatch(Long operatorId, EnterpriseApprovalBatchDTO dto);

    /**
     * 删除企业
     * @param enterpriseId 企业ID
     */
    void deleteEnterprise(Long enterpriseId);

    /**
     * 删除企业
     * @param userId 用户ID
     */
    void deleteEnterpriseByUserId(Long userId);
}
