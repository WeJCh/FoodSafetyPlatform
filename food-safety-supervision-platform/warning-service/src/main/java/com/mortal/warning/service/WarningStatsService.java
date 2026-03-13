package com.mortal.warning.service;

import com.mortal.warning.dto.WarningStatsQueryDTO;
import com.mortal.warning.vo.WarningEfficiencyStatsVO;
import com.mortal.warning.vo.WarningStatsOverviewVO;
import com.mortal.warning.vo.WarningTrendPointVO;
import com.mortal.warning.vo.WarningTypeStatsVO;
import java.util.List;

/**
 * 预警统计服务。
 */
public interface WarningStatsService {

    WarningStatsOverviewVO getOverview(WarningStatsQueryDTO queryDTO);

    List<WarningTrendPointVO> getTrend(WarningStatsQueryDTO queryDTO);

    List<WarningTypeStatsVO> getTypeTop(WarningStatsQueryDTO queryDTO);

    WarningEfficiencyStatsVO getEfficiency(WarningStatsQueryDTO queryDTO);
}

