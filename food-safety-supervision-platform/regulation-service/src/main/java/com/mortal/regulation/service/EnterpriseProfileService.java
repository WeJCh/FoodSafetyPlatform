package com.mortal.regulation.service;

import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.dto.EnterpriseApprovalDTO;
import com.mortal.regulation.dto.EnterpriseProfileDTO;
import com.mortal.regulation.vo.EnterpriseProfileVO;
import java.util.List;

public interface EnterpriseProfileService {

    EnterpriseProfileVO submitProfile(Long userId, EnterpriseProfileDTO dto);

    EnterpriseProfileVO getProfile(Long userId);

    EnterpriseProfileVO getById(Long enterpriseId);

    PageResult<EnterpriseProfileVO> list(String enterpriseName,
                                         String status,
                                         String approvalStatus,
                                         int page,
                                         int size);

    List<EnterpriseProfileVO> listPending();

    EnterpriseProfileVO approve(Long enterpriseId, Long operatorId, EnterpriseApprovalDTO dto);

    EnterpriseProfileVO reject(Long enterpriseId, Long operatorId, EnterpriseApprovalDTO dto);

    void deleteEnterprise(Long enterpriseId);

    void deleteEnterpriseByUserId(Long userId);
}
