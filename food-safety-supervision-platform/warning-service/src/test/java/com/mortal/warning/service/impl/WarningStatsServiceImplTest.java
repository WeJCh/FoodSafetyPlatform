package com.mortal.warning.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mortal.warning.dto.WarningStatsQueryDTO;
import com.mortal.warning.entity.WarningRecord;
import com.mortal.warning.mapper.WarningRecordMapper;
import com.mortal.warning.vo.WarningEfficiencyStatsVO;
import com.mortal.warning.vo.WarningStatsOverviewVO;
import com.mortal.warning.vo.WarningTrendPointVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WarningStatsServiceImplTest {

    @Mock
    private WarningRecordMapper warningRecordMapper;

    private WarningStatsServiceImpl warningStatsService;

    @BeforeEach
    void setUp() {
        warningStatsService = new WarningStatsServiceImpl(warningRecordMapper);
    }

    @Test
    void getOverview_shouldReturnZeroWhenNoData() {
        when(warningRecordMapper.selectList(any())).thenReturn(List.of());

        WarningStatsOverviewVO overview = warningStatsService.getOverview(new WarningStatsQueryDTO());

        assertNotNull(overview);
        assertEquals(0L, overview.getTotalCount());
        assertEquals(0L, overview.getOpenCount());
        assertEquals(0L, overview.getProcessingCount());
        assertEquals(0L, overview.getResolvedCount());
        assertEquals(0L, overview.getClosedCount());
        assertEquals(0L, overview.getCompletedCount());
        assertNotNull(overview.getStatusDistribution());
        assertNotNull(overview.getLevelDistribution());
        assertEquals(4, overview.getStatusDistribution().size());
        assertEquals(2, overview.getLevelDistribution().size());
    }

    @Test
    void getOverview_shouldThrowWhenStartTimeAfterEndTime() {
        WarningStatsQueryDTO query = new WarningStatsQueryDTO();
        query.setStartTime(LocalDateTime.of(2026, 3, 12, 0, 0));
        query.setEndTime(LocalDateTime.of(2026, 3, 10, 23, 59));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> warningStatsService.getOverview(query)
        );
        assertEquals("startTime must be before endTime", exception.getMessage());
        verifyNoInteractions(warningRecordMapper);
    }

    @Test
    void getTrend_shouldFillMissingDatesWithZero() {
        WarningStatsQueryDTO query = new WarningStatsQueryDTO();
        query.setStartTime(LocalDateTime.of(2026, 3, 10, 0, 0));
        query.setEndTime(LocalDateTime.of(2026, 3, 12, 23, 59, 59));

        WarningRecord dayOne = buildRecord("OPEN", "L1", LocalDateTime.of(2026, 3, 10, 10, 0), null);
        WarningRecord dayThree = buildRecord("OPEN", "L1", LocalDateTime.of(2026, 3, 12, 9, 0), null);
        when(warningRecordMapper.selectList(any())).thenReturn(List.of(dayOne, dayThree));

        List<WarningTrendPointVO> trend = warningStatsService.getTrend(query);

        assertEquals(3, trend.size());
        assertEquals("2026-03-10", trend.get(0).getDay());
        assertEquals(1L, trend.get(0).getCount());
        assertEquals("2026-03-11", trend.get(1).getDay());
        assertEquals(0L, trend.get(1).getCount());
        assertEquals("2026-03-12", trend.get(2).getDay());
        assertEquals(1L, trend.get(2).getCount());
    }

    @Test
    void getEfficiency_shouldCalculateExpectedMetrics() {
        WarningStatsQueryDTO query = new WarningStatsQueryDTO();
        query.setOverdueHours(24);

        LocalDateTime now = LocalDateTime.now();
        WarningRecord resolved = buildRecord("RESOLVED", "L1", now.minusHours(10), now.minusHours(8));
        WarningRecord closed = buildRecord("CLOSED", "L2", now.minusHours(30), now.minusHours(29));
        WarningRecord openOverdue = buildRecord("OPEN", "L1", now.minusHours(26), null);
        WarningRecord processingNormal = buildRecord("PROCESSING", "L1", now.minusHours(2), null);
        when(warningRecordMapper.selectList(any())).thenReturn(List.of(resolved, closed, openOverdue, processingNormal));

        WarningEfficiencyStatsVO efficiency = warningStatsService.getEfficiency(query);

        assertEquals(2L, efficiency.getResolvedCount());
        assertEquals(90L, efficiency.getAverageResolveMinutes());
        assertEquals(2L, efficiency.getPendingCount());
        assertEquals(1L, efficiency.getOverduePendingCount());
        assertEquals(24, efficiency.getOverdueHours());
    }

    private WarningRecord buildRecord(String status, String level, LocalDateTime firstOccurTime, LocalDateTime resolvedTime) {
        WarningRecord record = new WarningRecord();
        record.setStatus(status);
        record.setLevel(level);
        record.setFirstOccurTime(firstOccurTime);
        record.setResolvedTime(resolvedTime);
        record.setDeleted(0);
        return record;
    }
}
