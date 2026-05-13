package com.mortal.regulation.service;

import com.mortal.regulation.dto.RegulatorProfileDTO;
import com.mortal.regulation.dto.RegulatorSelfUpdateDTO;
import com.mortal.regulation.vo.RegulatorProfileVO;
import com.mortal.regulation.vo.AuditLogVO;
import java.util.List;

public interface RegulatorProfileService {

    RegulatorProfileVO createOrUpdate(Long operatorUserId, String operatorUsername, RegulatorProfileDTO dto);

    RegulatorProfileVO getByUserId(Long userId);

    RegulatorProfileVO updateMyProfile(Long operatorUserId, String operatorUsername, RegulatorSelfUpdateDTO dto);

    RegulatorProfileVO getById(Long id);

    List<RegulatorProfileVO> list(String roleType, Long regionId);

    List<RegulatorProfileVO> listEligibleEnforcers(Long currentUserId, Long regionId);

    RegulatorProfileVO updateStatus(Long operatorUserId, String operatorUsername, Long id, Integer status);

    void deleteRegulator(Long operatorUserId, String operatorUsername, Long id);

    RegulatorProfileVO adjustRegions(Long operatorUserId, String operatorUsername, Long id, List<Long> regionIds, String remark);

    List<AuditLogVO> listAuditLogs(Long id, Integer limit);

    List<AuditLogVO> listRecentAuditLogs(Integer limit);
}
