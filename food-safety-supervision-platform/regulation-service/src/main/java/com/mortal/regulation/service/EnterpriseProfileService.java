package com.mortal.regulation.service;

import com.mortal.regulation.dto.EnterpriseApprovalDTO;
import com.mortal.regulation.dto.EnterpriseProfileDTO;
import com.mortal.regulation.vo.EnterpriseProfileVO;
import java.util.List;

public interface EnterpriseProfileService {

    EnterpriseProfileVO submitProfile(Long userId, EnterpriseProfileDTO dto);

    EnterpriseProfileVO getProfile(Long userId);

    List<EnterpriseProfileVO> listPending();

    EnterpriseProfileVO approve(Long enterpriseId, Long operatorId, EnterpriseApprovalDTO dto);

    EnterpriseProfileVO reject(Long enterpriseId, Long operatorId, EnterpriseApprovalDTO dto);
}
