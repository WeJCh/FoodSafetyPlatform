package com.mortal.regulation.service;

import com.mortal.regulation.dto.RegionCreateDTO;
import com.mortal.regulation.vo.RegionVO;
import java.util.List;

public interface RegionService {

    RegionVO create(RegionCreateDTO dto);

    List<RegionVO> listByParentId(Long parentId);
}
