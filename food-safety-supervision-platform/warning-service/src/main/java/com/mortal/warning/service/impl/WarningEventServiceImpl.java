package com.mortal.warning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.warning.common.PageResult;
import com.mortal.warning.common.enums.WarningActionType;
import com.mortal.warning.common.enums.WarningLevel;
import com.mortal.warning.common.enums.WarningStatus;
import com.mortal.warning.dto.WarningProcessActionDTO;
import com.mortal.warning.dto.WarningRecordQueryDTO;
import com.mortal.warning.dto.WarningScopeDTO;
import com.mortal.warning.dto.WarningEventUpsertDTO;
import com.mortal.warning.entity.WarningProcessLog;
import com.mortal.warning.entity.WarningRecord;
import com.mortal.warning.mapper.WarningProcessLogMapper;
import com.mortal.warning.mapper.WarningRecordMapper;
import com.mortal.warning.service.WarningEventService;
import com.mortal.warning.vo.WarningProcessLogVO;
import com.mortal.warning.vo.WarningRecordDetailVO;
import com.mortal.warning.vo.WarningRecordVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WarningEventServiceImpl implements WarningEventService {

    private final WarningRecordMapper warningRecordMapper;
    private final WarningProcessLogMapper warningProcessLogMapper;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public WarningEventServiceImpl(WarningRecordMapper warningRecordMapper,
                                   WarningProcessLogMapper warningProcessLogMapper,
                                   ObjectMapper objectMapper) {
        this.warningRecordMapper = warningRecordMapper;
        this.warningProcessLogMapper = warningProcessLogMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 插入预警事件
     * @param dto 预警事件插入DTO
     * @return 预警记录VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WarningRecordVO upsertInternalEvent(WarningEventUpsertDTO dto) {
        String dedupKey = normalizeRequired(dto.getDedupKey(), "dedupKey required");
        String warningType = normalizeRequired(dto.getEventType(), "eventType required");
        String bizType = normalizeRequired(dto.getBizType(), "bizType required");
        WarningLevel level = WarningLevel.fromValue(dto.getLevel());
        Long bizId = dto.getBizId();
        if (bizId == null) {
            throw new IllegalArgumentException("bizId required");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime occurTime = dto.getOccurTime() == null ? now : dto.getOccurTime();
        String payloadJson = toPayloadJson(dto.getPayload());

        WarningRecord existing = warningRecordMapper.selectOne(new LambdaQueryWrapper<WarningRecord>()
            .eq(WarningRecord::getDedupKey, dedupKey)
            .eq(WarningRecord::getDeleted, 0)
            .last("limit 1"));

        if (existing == null) {
            // 中文注释：首次触发事件，创建新的预警主记录。
            WarningRecord created = new WarningRecord();
            created.setWarningNo(generateWarningNo(now));
            created.setWarningType(warningType);
            created.setBizType(bizType);
            created.setBizId(bizId);
            created.setRegionId(dto.getRegionId());
            created.setOwnerRegulatorId(dto.getOwnerRegulatorId());
            created.setDedupKey(dedupKey);
            created.setLevel(level.name());
            created.setStatus(WarningStatus.OPEN.name());
            created.setTitle(normalizeOptional(dto.getTitle()));
            created.setContent(normalizeOptional(dto.getContent()));
            created.setSourceService(normalizeSourceService(dto.getSourceService()));
            created.setFirstOccurTime(occurTime);
            created.setLastOccurTime(occurTime);
            created.setTriggerCount(1);
            created.setPayloadJson(payloadJson);
            created.setCreateTime(now);
            created.setUpdateTime(now);
            created.setDeleted(0);
            warningRecordMapper.insert(created);
            saveProcessLog(created.getId(), "新事件创建预警记录", now);
            return toVO(created);
        }

        // 中文注释：重复触发同一事件，仅合并次数与最近时间，不重复建单。
        existing.setWarningType(warningType);
        existing.setBizType(bizType);
        existing.setBizId(bizId);
        if (dto.getRegionId() != null) {
            existing.setRegionId(dto.getRegionId());
        }
        if (dto.getOwnerRegulatorId() != null) {
            existing.setOwnerRegulatorId(dto.getOwnerRegulatorId());
        }
        WarningLevel oldLevel = existing.getLevel() == null
            ? level
            : WarningLevel.fromValue(existing.getLevel());
        existing.setLevel(resolveHigherLevel(oldLevel, level).name());
        existing.setTitle(normalizeOptional(dto.getTitle()));
        existing.setContent(normalizeOptional(dto.getContent()));
        existing.setSourceService(normalizeSourceService(dto.getSourceService()));
        existing.setLastOccurTime(maxTime(existing.getLastOccurTime(), occurTime));
        existing.setTriggerCount((existing.getTriggerCount() == null ? 0 : existing.getTriggerCount()) + 1);
        existing.setPayloadJson(payloadJson);
        existing.setUpdateTime(now);
        warningRecordMapper.updateById(existing);
        saveProcessLog(existing.getId(), "重复事件合并，触发次数+1", now);
        return toVO(existing);
    }

    /**
     * 分页查询预警列表。
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<WarningRecordVO> pageWarningRecords(WarningRecordQueryDTO queryDTO) {
        WarningRecordQueryDTO safeQuery = queryDTO == null ? new WarningRecordQueryDTO() : queryDTO;
        int page = normalizePage(safeQuery.getPage());
        int size = normalizeSize(safeQuery.getSize());
        int offset = (page - 1) * size;

        LambdaQueryWrapper<WarningRecord> countWrapper = buildQueryWrapper(safeQuery);
        Long total = warningRecordMapper.selectCount(countWrapper);

        List<WarningRecordVO> records = List.of();
        if (total != null && total > 0) {
            LambdaQueryWrapper<WarningRecord> pageWrapper = buildQueryWrapper(safeQuery);
            pageWrapper
                .orderByDesc(WarningRecord::getLastOccurTime)
                .orderByDesc(WarningRecord::getCreateTime)
                .last("limit " + offset + "," + size);
            records = warningRecordMapper.selectList(pageWrapper).stream().map(this::toVO).toList();
        }
        return PageResult.of(records, total == null ? 0L : total, page, size);
    }

    /**
     * 查询预警详情（包含处理日志）。
     * @param warningId 预警ID
     * @return 预警详情
     */
    @Override
    public WarningRecordDetailVO getWarningRecordDetail(Long warningId, WarningScopeDTO scopeDTO) {
        WarningRecord record = loadWarningRecord(warningId);
        ensureInScope(record, scopeDTO);
        return toDetailVO(record);
    }

    /**
     * 处理预警动作，并记录处理日志。
     * @param warningId 预警ID
     * @param actionDTO 动作参数
     * @param operatorId 操作人ID
     * @param operatorName 操作人名称
     * @return 处理后的详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WarningRecordDetailVO processWarning(Long warningId,
                                                WarningProcessActionDTO actionDTO,
                                                Long operatorId,
                                                String operatorName,
                                                WarningScopeDTO scopeDTO) {
        WarningRecord record = loadWarningRecord(warningId);
        ensureInScope(record, scopeDTO);
        if (actionDTO == null) {
            throw new IllegalArgumentException("action payload required");
        }
        WarningActionType actionType = normalizeActionType(actionDTO.getActionType());
        WarningStatus targetStatus = resolveTargetStatus(record.getStatus(), actionType);

        record.setStatus(targetStatus.name());
        record.setUpdateTime(LocalDateTime.now());
        warningRecordMapper.updateById(record);

        WarningProcessLog processLog = new WarningProcessLog();
        processLog.setWarningId(record.getId());
        processLog.setActionType(actionType.name());
        processLog.setOperatorId(operatorId);
        processLog.setOperatorName(StringUtils.hasText(operatorName) ? operatorName.trim() : "unknown");
        processLog.setActionComment(normalizeOptional(actionDTO.getActionComment()));
        processLog.setCreateTime(LocalDateTime.now());
        processLog.setUpdateTime(LocalDateTime.now());
        processLog.setDeleted(0);
        warningProcessLogMapper.insert(processLog);
        return toDetailVO(record);
    }

    private WarningRecordVO toVO(WarningRecord record) {
        WarningRecordVO vo = new WarningRecordVO();
        vo.setId(record.getId());
        vo.setWarningNo(record.getWarningNo());
        vo.setWarningType(record.getWarningType());
        vo.setBizType(record.getBizType());
        vo.setBizId(record.getBizId());
        vo.setRegionId(record.getRegionId());
        vo.setOwnerRegulatorId(record.getOwnerRegulatorId());
        vo.setDedupKey(record.getDedupKey());
        vo.setLevel(record.getLevel());
        vo.setStatus(record.getStatus());
        vo.setTitle(record.getTitle());
        vo.setContent(record.getContent());
        vo.setSourceService(record.getSourceService());
        vo.setFirstOccurTime(record.getFirstOccurTime());
        vo.setLastOccurTime(record.getLastOccurTime());
        vo.setTriggerCount(record.getTriggerCount());
        vo.setAssignedTo(record.getAssignedTo());
        vo.setAssignedTime(record.getAssignedTime());
        vo.setResolvedBy(record.getResolvedBy());
        vo.setResolvedTime(record.getResolvedTime());
        vo.setCloseReason(record.getCloseReason());
        vo.setPayloadJson(record.getPayloadJson());
        return vo;
    }

    /**
     * 保存预警处理日志
     * @param warningId 预警ID
     * @param actionComment 处理备注
     * @param now 当前时间
     */
    private void saveProcessLog(Long warningId, String actionComment, LocalDateTime now) {
        WarningProcessLog processLog = new WarningProcessLog();
        processLog.setWarningId(warningId);
        processLog.setActionType(WarningActionType.EVENT_UPSERT.name());
        processLog.setOperatorId(null);
        processLog.setOperatorName("system");
        processLog.setActionComment(actionComment);
        processLog.setCreateTime(now);
        processLog.setUpdateTime(now);
        processLog.setDeleted(0);
        warningProcessLogMapper.insert(processLog);
    }

    private WarningRecordDetailVO toDetailVO(WarningRecord record) {
        WarningRecordDetailVO detailVO = new WarningRecordDetailVO();
        WarningRecordVO baseVO = toVO(record);
        detailVO.setId(baseVO.getId());
        detailVO.setWarningNo(baseVO.getWarningNo());
        detailVO.setWarningType(baseVO.getWarningType());
        detailVO.setBizType(baseVO.getBizType());
        detailVO.setBizId(baseVO.getBizId());
        detailVO.setRegionId(baseVO.getRegionId());
        detailVO.setOwnerRegulatorId(baseVO.getOwnerRegulatorId());
        detailVO.setDedupKey(baseVO.getDedupKey());
        detailVO.setLevel(baseVO.getLevel());
        detailVO.setStatus(baseVO.getStatus());
        detailVO.setTitle(baseVO.getTitle());
        detailVO.setContent(baseVO.getContent());
        detailVO.setSourceService(baseVO.getSourceService());
        detailVO.setFirstOccurTime(baseVO.getFirstOccurTime());
        detailVO.setLastOccurTime(baseVO.getLastOccurTime());
        detailVO.setTriggerCount(baseVO.getTriggerCount());
        detailVO.setAssignedTo(baseVO.getAssignedTo());
        detailVO.setAssignedTime(baseVO.getAssignedTime());
        detailVO.setResolvedBy(baseVO.getResolvedBy());
        detailVO.setResolvedTime(baseVO.getResolvedTime());
        detailVO.setCloseReason(baseVO.getCloseReason());
        detailVO.setPayloadJson(baseVO.getPayloadJson());
        detailVO.setProcessLogs(loadProcessLogs(record.getId()));
        return detailVO;
    }

    private List<WarningProcessLogVO> loadProcessLogs(Long warningId) {
        return warningProcessLogMapper.selectList(
            new LambdaQueryWrapper<WarningProcessLog>()
                .eq(WarningProcessLog::getWarningId, warningId)
                .eq(WarningProcessLog::getDeleted, 0)
                .orderByDesc(WarningProcessLog::getCreateTime)
                .orderByDesc(WarningProcessLog::getId)
        ).stream().map(logItem -> {
            WarningProcessLogVO vo = new WarningProcessLogVO();
            vo.setId(logItem.getId());
            vo.setWarningId(logItem.getWarningId());
            vo.setActionType(logItem.getActionType());
            vo.setOperatorId(logItem.getOperatorId());
            vo.setOperatorName(logItem.getOperatorName());
            vo.setActionComment(logItem.getActionComment());
            vo.setCreateTime(logItem.getCreateTime());
            return vo;
        }).toList();
    }

    private WarningRecord loadWarningRecord(Long warningId) {
        if (warningId == null) {
            throw new IllegalArgumentException("warningId required");
        }
        WarningRecord record = warningRecordMapper.selectOne(
            new LambdaQueryWrapper<WarningRecord>()
                .eq(WarningRecord::getId, warningId)
                .eq(WarningRecord::getDeleted, 0)
                .last("limit 1")
        );
        if (record == null) {
            throw new IllegalArgumentException("warning record not found");
        }
        return record;
    }

    private WarningActionType normalizeActionType(String actionType) {
        WarningActionType normalized = WarningActionType.fromValue(actionType);
        Set<WarningActionType> supported = Set.of(
            WarningActionType.ACK,
            WarningActionType.PROCESS,
            WarningActionType.RESOLVE,
            WarningActionType.CLOSE
        );
        if (!supported.contains(normalized)) {
            throw new IllegalArgumentException("unsupported actionType");
        }
        return normalized;
    }

    private WarningStatus resolveTargetStatus(String currentStatus, WarningActionType actionType) {
        WarningStatus normalizedStatus = WarningStatus.fromValue(currentStatus);
        switch (actionType) {
            case ACK -> {
                ensureAllowed(normalizedStatus, Set.of(WarningStatus.OPEN), actionType);
                return WarningStatus.ACKED;
            }
            case PROCESS -> {
                ensureAllowed(normalizedStatus, Set.of(WarningStatus.OPEN, WarningStatus.ACKED), actionType);
                return WarningStatus.PROCESSING;
            }
            case RESOLVE -> {
                ensureAllowed(
                    normalizedStatus,
                    Set.of(WarningStatus.OPEN, WarningStatus.ACKED, WarningStatus.PROCESSING),
                    actionType
                );
                return WarningStatus.RESOLVED;
            }
            case CLOSE -> {
                ensureAllowed(normalizedStatus, Set.of(WarningStatus.RESOLVED), actionType);
                return WarningStatus.CLOSED;
            }
            default -> throw new IllegalArgumentException("unsupported actionType");
        }
    }

    /**
     * 中文注释：确保状态机不跳级，避免出现无效闭环状态。
     */
    private void ensureAllowed(WarningStatus currentStatus,
                               Set<WarningStatus> allowed,
                               WarningActionType actionType) {
        if (!allowed.contains(currentStatus)) {
            throw new IllegalArgumentException(
                "invalid status transition: action=" + actionType + ", currentStatus=" + currentStatus
            );
        }
    }

    private LambdaQueryWrapper<WarningRecord> buildQueryWrapper(WarningRecordQueryDTO queryDTO) {
        LambdaQueryWrapper<WarningRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WarningRecord::getDeleted, 0);
        Set<Long> regionIdSet = parseRegionIds(queryDTO.getRegionIds());
        ensureScope(queryDTO.getOwnerRegulatorId(), regionIdSet);
        if (StringUtils.hasText(queryDTO.getStatus())) {
            wrapper.eq(WarningRecord::getStatus, WarningStatus.fromValue(queryDTO.getStatus()).name());
        }
        if (StringUtils.hasText(queryDTO.getLevel())) {
            wrapper.eq(WarningRecord::getLevel, WarningLevel.fromValue(queryDTO.getLevel()).name());
        }
        if (StringUtils.hasText(queryDTO.getWarningType())) {
            wrapper.eq(WarningRecord::getWarningType, queryDTO.getWarningType().trim());
        }
        if (StringUtils.hasText(queryDTO.getBizType())) {
            wrapper.eq(WarningRecord::getBizType, queryDTO.getBizType().trim());
        }
        if (queryDTO.getBizId() != null) {
            wrapper.eq(WarningRecord::getBizId, queryDTO.getBizId());
        }
        if (queryDTO.getOwnerRegulatorId() != null) {
            wrapper.eq(WarningRecord::getOwnerRegulatorId, queryDTO.getOwnerRegulatorId());
        }
        if (!regionIdSet.isEmpty()) {
            wrapper.in(WarningRecord::getRegionId, regionIdSet);
        }
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String keyword = queryDTO.getKeyword().trim();
            wrapper.and(w -> w.like(WarningRecord::getTitle, keyword).or().like(WarningRecord::getContent, keyword));
        }
        return wrapper;
    }

    /**
     * 中文注释：详情/动作接口的作用域校验，避免通过ID绕过列表过滤。
     */
    private void ensureInScope(WarningRecord record, WarningScopeDTO scopeDTO) {
        if (record == null || scopeDTO == null) {
            return;
        }
        Set<Long> regionIds = parseRegionIds(scopeDTO.getRegionIds());
        ensureScope(scopeDTO.getOwnerRegulatorId(), regionIds);
        if (scopeDTO.getOwnerRegulatorId() != null
            && !scopeDTO.getOwnerRegulatorId().equals(record.getOwnerRegulatorId())) {
            throw new IllegalArgumentException("warning not found");
        }
        if (!regionIds.isEmpty() && (record.getRegionId() == null || !regionIds.contains(record.getRegionId()))) {
            throw new IllegalArgumentException("warning not found");
        }
    }

    private void ensureScope(Long ownerRegulatorId, Set<Long> regionIds) {
        if (ownerRegulatorId == null && (regionIds == null || regionIds.isEmpty())) {
            throw new IllegalArgumentException("scope required");
        }
    }

    private Set<Long> parseRegionIds(String regionIds) {
        if (!StringUtils.hasText(regionIds)) {
            return Set.of();
        }
        return List.of(regionIds.split(","))
            .stream()
            .map(String::trim)
            .filter(StringUtils::hasText)
            .map(value -> {
                try {
                    return Long.valueOf(value);
                } catch (NumberFormatException ex) {
                    return null;
                }
            })
            .filter(value -> value != null && value > 0)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }

    private LocalDateTime maxTime(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    private WarningLevel resolveHigherLevel(WarningLevel oldLevel, WarningLevel newLevel) {
        if (oldLevel == WarningLevel.L2 || newLevel == WarningLevel.L2) {
            return WarningLevel.L2;
        }
        return WarningLevel.L1;
    }

    private String normalizeSourceService(String value) {
        if (!StringUtils.hasText(value)) {
            return "regulation-service";
        }
        return value.trim();
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String toPayloadJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid payload");
        }
    }

    private String generateWarningNo(LocalDateTime now) {
        String timePart = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomPart = 1000 + random.nextInt(9000);
        return "WRN" + timePart + randomPart;
    }
}
