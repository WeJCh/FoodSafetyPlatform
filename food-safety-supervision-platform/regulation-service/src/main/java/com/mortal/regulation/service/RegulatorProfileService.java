package com.mortal.regulation.service;

import com.mortal.regulation.dto.RegulatorProfileDTO;
import com.mortal.regulation.vo.RegulatorProfileVO;
import java.util.List;

public interface RegulatorProfileService {

    RegulatorProfileVO createOrUpdate(RegulatorProfileDTO dto);

    RegulatorProfileVO getByUserId(Long userId);

    RegulatorProfileVO getById(Long id);

    List<RegulatorProfileVO> list(String roleType, String jurisdictionArea);

    RegulatorProfileVO updateStatus(Long id, Integer status);

    void deleteRegulator(Long id);
}
