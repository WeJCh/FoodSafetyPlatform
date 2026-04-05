package com.mortal.regulation.operation.service;

import com.mortal.regulation.operation.dto.InternalStatsQueryDTO;
import com.mortal.regulation.operation.vo.InternalOperationStatsOverviewVO;

public interface InternalStatsService {

    InternalOperationStatsOverviewVO getOverview(InternalStatsQueryDTO queryDTO);
}
