package com.mortal.regulation.service;

import com.mortal.regulation.dto.RegulatorProfileDTO;
import com.mortal.regulation.vo.RegulatorProfileVO;
import com.mortal.regulation.vo.AuditLogVO;
import java.util.List;

public interface RegulatorProfileService {

    RegulatorProfileVO createOrUpdate(Long operatorUserId, String operatorName, RegulatorProfileDTO dto);

    RegulatorProfileVO getByUserId(Long userId);

    RegulatorProfileVO getById(Long id);

    List<RegulatorProfileVO> list(String roleType, Long regionId);

    List<RegulatorProfileVO> listEligibleEnforcers(Long currentUserId, Long regionId);

    RegulatorProfileVO updateStatus(Long operatorUserId, String operatorName, Long id, Integer status);

    void deleteRegulator(Long operatorUserId, String operatorName, Long id);

    RegulatorProfileVO adjustRegions(Long operatorUserId, String operatorName, Long id, List<Long> regionIds, String remark);

    List<AuditLogVO> listAuditLogs(Long id, Integer limit);

    List<AuditLogVO> listRecentAuditLogs(Integer limit);
}
