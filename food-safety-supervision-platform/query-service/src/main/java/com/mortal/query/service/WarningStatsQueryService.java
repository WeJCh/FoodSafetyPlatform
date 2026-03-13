package com.mortal.query.service;

import com.mortal.query.dto.WarningStatsQueryDTO;
import com.mortal.query.vo.WarningEfficiencyStatsVO;
import com.mortal.query.vo.WarningStatsOverviewVO;
import com.mortal.query.vo.WarningTrendPointVO;
import com.mortal.query.vo.WarningTypeStatsVO;
import java.util.List;

public interface WarningStatsQueryService {

    WarningStatsOverviewVO getOverview(WarningStatsQueryDTO queryDTO);

    List<WarningTrendPointVO> getTrend(WarningStatsQueryDTO queryDTO);

    List<WarningTypeStatsVO> getTypeTop(WarningStatsQueryDTO queryDTO);

    WarningEfficiencyStatsVO getEfficiency(WarningStatsQueryDTO queryDTO);
}

