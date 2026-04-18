package com.mortal.warning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.warning.common.enums.WarningLevel;
import com.mortal.warning.common.enums.WarningStatus;
import com.mortal.warning.dto.WarningStatsQueryDTO;
import com.mortal.warning.entity.WarningRecord;
import com.mortal.warning.mapper.WarningRecordMapper;
import com.mortal.warning.service.WarningStatsService;
import com.mortal.warning.support.WarningStatsCacheSupport;
import com.mortal.warning.vo.WarningEfficiencyStatsVO;
import com.mortal.warning.vo.WarningStatsItemVO;
import com.mortal.warning.vo.WarningStatsOverviewVO;
import com.mortal.warning.vo.WarningTrendPointVO;
import com.mortal.warning.vo.WarningTypeStatsVO;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

@Service
public class WarningStatsServiceImpl implements WarningStatsService {

    private static final int DEFAULT_TOP_N = 5;
    private static final int MAX_TOP_N = 20;
    private static final int DEFAULT_TREND_DAYS = 7;
    private static final int MAX_TREND_DAYS = 60;
    private static final int DEFAULT_OVERDUE_HOURS = 24;
    private static final int MAX_OVERDUE_HOURS = 30 * 24;
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter KEY_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final WarningRecordMapper warningRecordMapper;
    private final WarningStatsCacheSupport warningStatsCacheSupport;

    public WarningStatsServiceImpl(WarningRecordMapper warningRecordMapper,
                                   WarningStatsCacheSupport warningStatsCacheSupport) {
        this.warningRecordMapper = warningRecordMapper;
        this.warningStatsCacheSupport = warningStatsCacheSupport;
    }

    @Override
    public WarningStatsOverviewVO getOverview(WarningStatsQueryDTO queryDTO) {
        String cacheKey = warningStatsCacheSupport.buildCacheKey("overview", buildFingerprint("overview", queryDTO));
        return warningStatsCacheSupport.getOrLoad(cacheKey, () -> loadOverview(queryDTO));
    }

    @Override
    public List<WarningTrendPointVO> getTrend(WarningStatsQueryDTO queryDTO) {
        String cacheKey = warningStatsCacheSupport.buildCacheKey("trend", buildFingerprint("trend", queryDTO));
        return warningStatsCacheSupport.getOrLoad(cacheKey, () -> loadTrend(queryDTO));
    }

    @Override
    public List<WarningTypeStatsVO> getTypeTop(WarningStatsQueryDTO queryDTO) {
        String cacheKey = warningStatsCacheSupport.buildCacheKey("types", buildFingerprint("types", queryDTO));
        return warningStatsCacheSupport.getOrLoad(cacheKey, () -> loadTypeTop(queryDTO));
    }

    @Override
    public WarningEfficiencyStatsVO getEfficiency(WarningStatsQueryDTO queryDTO) {
        String cacheKey = warningStatsCacheSupport.buildCacheKey(
            "efficiency",
            buildFingerprint("efficiency", queryDTO)
        );
        return warningStatsCacheSupport.getOrLoad(cacheKey, () -> loadEfficiency(queryDTO));
    }

    private WarningStatsOverviewVO loadOverview(WarningStatsQueryDTO queryDTO) {
        List<WarningRecord> records = loadRecords(queryDTO, null, null);
        Map<String, Long> statusCount = records.stream()
            .collect(Collectors.groupingBy(
                item -> safeUpper(item.getStatus()),
                Collectors.counting()
            ));
        Map<String, Long> levelCount = records.stream()
            .collect(Collectors.groupingBy(
                item -> safeUpper(item.getLevel()),
                Collectors.counting()
            ));

        WarningStatsOverviewVO vo = new WarningStatsOverviewVO();
        vo.setTotalCount((long) records.size());
        vo.setOpenCount(statusCount.getOrDefault(WarningStatus.OPEN.name(), 0L));
        vo.setProcessingCount(statusCount.getOrDefault(WarningStatus.PROCESSING.name(), 0L));
        vo.setResolvedCount(statusCount.getOrDefault(WarningStatus.RESOLVED.name(), 0L));
        vo.setClosedCount(statusCount.getOrDefault(WarningStatus.CLOSED.name(), 0L));
        vo.setCompletedCount(vo.getResolvedCount() + vo.getClosedCount());
        vo.setStatusDistribution(buildStatusDistribution(statusCount));
        vo.setLevelDistribution(buildLevelDistribution(levelCount));
        return vo;
    }

    private List<WarningTrendPointVO> loadTrend(WarningStatsQueryDTO queryDTO) {
        LocalDateTime now = LocalDateTime.now();
        int trendDays = normalizeTrendDays(queryDTO == null ? null : queryDTO.getTrendDays());
        LocalDateTime defaultStart = now.minusDays(trendDays - 1L).toLocalDate().atStartOfDay();
        LocalDateTime defaultEnd = now;
        List<WarningRecord> records = loadRecords(queryDTO, defaultStart, defaultEnd);

        LocalDate startDate = resolveStartTime(queryDTO, defaultStart).toLocalDate();
        LocalDate endDate = resolveEndTime(queryDTO, defaultEnd).toLocalDate();
        Map<LocalDate, Long> dateCount = new LinkedHashMap<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            dateCount.put(cursor, 0L);
            cursor = cursor.plusDays(1);
        }
        for (WarningRecord record : records) {
            LocalDateTime baseTime = resolveOccurTime(record);
            if (baseTime == null) {
                continue;
            }
            LocalDate day = baseTime.toLocalDate();
            if (day.isBefore(startDate) || day.isAfter(endDate)) {
                continue;
            }
            dateCount.put(day, dateCount.getOrDefault(day, 0L) + 1L);
        }

        List<WarningTrendPointVO> result = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entry : dateCount.entrySet()) {
            WarningTrendPointVO item = new WarningTrendPointVO();
            item.setDay(entry.getKey().format(DAY_FORMATTER));
            item.setCount(entry.getValue());
            result.add(item);
        }
        return result;
    }

    private List<WarningTypeStatsVO> loadTypeTop(WarningStatsQueryDTO queryDTO) {
        List<WarningRecord> records = loadRecords(queryDTO, null, null);
        int topN = normalizeTopN(queryDTO == null ? null : queryDTO.getTopN());
        Map<String, Long> typeCount = records.stream()
            .collect(Collectors.groupingBy(
                item -> normalizeType(item.getWarningType()),
                Collectors.counting()
            ));
        return typeCount.entrySet().stream()
            .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey))
            .limit(topN)
            .map(entry -> {
                WarningTypeStatsVO item = new WarningTypeStatsVO();
                item.setWarningType(entry.getKey());
                item.setCount(entry.getValue());
                return item;
            })
            .toList();
    }

    private WarningEfficiencyStatsVO loadEfficiency(WarningStatsQueryDTO queryDTO) {
        List<WarningRecord> records = loadRecords(queryDTO, null, null);
        int overdueHours = normalizeOverdueHours(queryDTO == null ? null : queryDTO.getOverdueHours());
        LocalDateTime threshold = LocalDateTime.now().minusHours(overdueHours);

        long resolvedCount = 0L;
        long totalResolveMinutes = 0L;
        long pendingCount = 0L;
        long overduePendingCount = 0L;
        for (WarningRecord record : records) {
            WarningStatus status = parseStatusSafe(record.getStatus());
            LocalDateTime occurTime = resolveOccurTime(record);
            if (status == WarningStatus.RESOLVED || status == WarningStatus.CLOSED) {
                LocalDateTime resolvedTime = record.getResolvedTime();
                if (occurTime != null && resolvedTime != null && !resolvedTime.isBefore(occurTime)) {
                    totalResolveMinutes += Duration.between(occurTime, resolvedTime).toMinutes();
                    resolvedCount++;
                }
                continue;
            }
            if (status == WarningStatus.OPEN || status == WarningStatus.PROCESSING) {
                pendingCount++;
                if (occurTime != null && !occurTime.isAfter(threshold)) {
                    overduePendingCount++;
                }
            }
        }

        WarningEfficiencyStatsVO vo = new WarningEfficiencyStatsVO();
        vo.setResolvedCount(resolvedCount);
        vo.setAverageResolveMinutes(resolvedCount == 0 ? 0L : totalResolveMinutes / resolvedCount);
        vo.setPendingCount(pendingCount);
        vo.setOverduePendingCount(overduePendingCount);
        vo.setOverdueHours(overdueHours);
        return vo;
    }

    private String buildFingerprint(String category, WarningStatsQueryDTO queryDTO) {
        WarningStatsQueryDTO query = queryDTO == null ? new WarningStatsQueryDTO() : queryDTO;
        String regionIds = parseRegionIds(query.getRegionIds()).stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
        String raw = String.join("|",
            category,
            formatTime(query.getStartTime()),
            formatTime(query.getEndTime()),
            normalizeKeyText(query.getWarningType()),
            normalizeKeyText(query.getBizType()),
            normalizeKeyText(query.getLevel()),
            normalizeKeyText(query.getStatus()),
            query.getRegionId() == null ? "" : String.valueOf(query.getRegionId()),
            regionIds,
            query.getOwnerRegulatorId() == null ? "" : String.valueOf(query.getOwnerRegulatorId()),
            String.valueOf(normalizeTopN(query.getTopN())),
            String.valueOf(normalizeTrendDays(query.getTrendDays())),
            String.valueOf(normalizeOverdueHours(query.getOverdueHours()))
        );
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : KEY_TIME_FORMATTER.format(time);
    }

    private String normalizeKeyText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private List<WarningRecord> loadRecords(WarningStatsQueryDTO queryDTO,
                                            LocalDateTime defaultStart,
                                            LocalDateTime defaultEnd) {
        WarningStatsQueryDTO query = queryDTO == null ? new WarningStatsQueryDTO() : queryDTO;
        LocalDateTime start = resolveStartTime(query, defaultStart);
        LocalDateTime end = resolveEndTime(query, defaultEnd);
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }

        LambdaQueryWrapper<WarningRecord> wrapper = new LambdaQueryWrapper<WarningRecord>()
            .eq(WarningRecord::getDeleted, 0);
        if (start != null) {
            wrapper.ge(WarningRecord::getFirstOccurTime, start);
        }
        if (end != null) {
            wrapper.le(WarningRecord::getFirstOccurTime, end);
        }
        if (StringUtils.hasText(query.getWarningType())) {
            wrapper.eq(WarningRecord::getWarningType, query.getWarningType().trim());
        }
        if (StringUtils.hasText(query.getBizType())) {
            wrapper.eq(WarningRecord::getBizType, query.getBizType().trim());
        }
        if (StringUtils.hasText(query.getLevel())) {
            wrapper.eq(WarningRecord::getLevel, WarningLevel.fromValue(query.getLevel()).name());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(WarningRecord::getStatus, WarningStatus.fromValue(query.getStatus()).name());
        }
        if (query.getRegionId() != null) {
            wrapper.eq(WarningRecord::getRegionId, query.getRegionId());
        }
        Set<Long> regionIds = parseRegionIds(query.getRegionIds());
        if (!regionIds.isEmpty()) {
            wrapper.in(WarningRecord::getRegionId, regionIds);
        }
        if (query.getOwnerRegulatorId() != null) {
            wrapper.eq(WarningRecord::getOwnerRegulatorId, query.getOwnerRegulatorId());
        }
        wrapper.orderByDesc(WarningRecord::getFirstOccurTime).orderByDesc(WarningRecord::getId);
        return warningRecordMapper.selectList(wrapper);
    }

    private List<WarningStatsItemVO> buildStatusDistribution(Map<String, Long> statusCount) {
        List<WarningStatsItemVO> result = new ArrayList<>();
        result.add(createStatsItem(WarningStatus.OPEN.name(), "待处理", statusCount));
        result.add(createStatsItem(WarningStatus.PROCESSING.name(), "处理中", statusCount));
        result.add(createStatsItem(WarningStatus.RESOLVED.name(), "已解决", statusCount));
        result.add(createStatsItem(WarningStatus.CLOSED.name(), "已归档", statusCount));
        return result;
    }

    private List<WarningStatsItemVO> buildLevelDistribution(Map<String, Long> levelCount) {
        List<WarningStatsItemVO> result = new ArrayList<>();
        result.add(createStatsItem(WarningLevel.L1.name(), "一级", levelCount));
        result.add(createStatsItem(WarningLevel.L2.name(), "二级", levelCount));
        return result;
    }

    private WarningStatsItemVO createStatsItem(String key, String label, Map<String, Long> countMap) {
        WarningStatsItemVO item = new WarningStatsItemVO();
        item.setKey(key);
        item.setLabel(label);
        item.setCount(countMap.getOrDefault(key, 0L));
        return item;
    }

    private String normalizeType(String warningType) {
        if (!StringUtils.hasText(warningType)) {
            return "UNKNOWN";
        }
        return warningType.trim();
    }

    private String safeUpper(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private Set<Long> parseRegionIds(String regionIds) {
        if (!StringUtils.hasText(regionIds)) {
            return Set.of();
        }
        return List.of(regionIds.split(",")).stream()
            .map(String::trim)
            .filter(StringUtils::hasText)
            .map(text -> {
                try {
                    return Long.valueOf(text);
                } catch (NumberFormatException ex) {
                    return null;
                }
            })
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private WarningStatus parseStatusSafe(String value) {
        try {
            return WarningStatus.fromValue(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private LocalDateTime resolveOccurTime(WarningRecord record) {
        if (record == null) {
            return null;
        }
        return record.getFirstOccurTime();
    }

    private LocalDateTime resolveStartTime(WarningStatsQueryDTO query, LocalDateTime defaultStart) {
        if (query == null) {
            return defaultStart;
        }
        return query.getStartTime() != null ? query.getStartTime() : defaultStart;
    }

    private LocalDateTime resolveEndTime(WarningStatsQueryDTO query, LocalDateTime defaultEnd) {
        if (query == null) {
            return defaultEnd;
        }
        return query.getEndTime() != null ? query.getEndTime() : defaultEnd;
    }

    private int normalizeTopN(Integer topN) {
        if (topN == null || topN < 1) {
            return DEFAULT_TOP_N;
        }
        return Math.min(topN, MAX_TOP_N);
    }

    private int normalizeTrendDays(Integer trendDays) {
        if (trendDays == null || trendDays < 1) {
            return DEFAULT_TREND_DAYS;
        }
        return Math.min(trendDays, MAX_TREND_DAYS);
    }

    private int normalizeOverdueHours(Integer overdueHours) {
        if (overdueHours == null || overdueHours < 1) {
            return DEFAULT_OVERDUE_HOURS;
        }
        return Math.min(overdueHours, MAX_OVERDUE_HOURS);
    }
}
