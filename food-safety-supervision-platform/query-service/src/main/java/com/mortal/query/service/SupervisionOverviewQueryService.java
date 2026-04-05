package com.mortal.query.service;

import com.mortal.query.dto.WarningStatsQueryDTO;
import com.mortal.query.vo.SupervisionOverviewVO;

public interface SupervisionOverviewQueryService {

    SupervisionOverviewVO getOverview(WarningStatsQueryDTO queryDTO);
}
