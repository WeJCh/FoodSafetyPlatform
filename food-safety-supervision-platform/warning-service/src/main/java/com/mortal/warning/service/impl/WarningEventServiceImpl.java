package com.mortal.warning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.platform.common.PageResult;
import com.mortal.platform.common.ApiResponse;
import com.mortal.warning.client.regulation.RegulationRegulatorInternalClient;
import com.mortal.warning.client.regulation.vo.InternalRegulatorSummaryVO;
import com.mortal.warning.common.enums.WarningActionType;
import com.mortal.warning.common.enums.WarningLevel;
import com.mortal.warning.common.enums.WarningStatus;
import com.mortal.warning.dto.WarningAssignDTO;
import com.mortal.warning.dto.WarningRecordQueryDTO;
import com.mortal.warning.dto.WarningScopeDTO;
import com.mortal.warning.dto.WarningEventUpsertDTO;
import com.mortal.warning.entity.WarningProcessLog;
import com.mortal.warning.entity.WarningRecord;
import com.mortal.warning.mapper.WarningProcessLogMapper;
import com.mortal.warning.mapper.WarningRecordMapper;
import com.mortal.warning.service.WarningEventService;
import com.mortal.warning.support.WarningLockSupport;
import com.mortal.warning.support.WarningStatsCacheSupport;
import com.mortal.warning.vo.WarningProcessLogVO;
import com.mortal.warning.vo.WarningRecordDetailVO;
import com.mortal.warning.vo.WarningRecordVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WarningEventServiceImpl implements WarningEventService {

    private final WarningRecordMapper warningRecordMapper;
    private final WarningProcessLogMapper warningProcessLogMapper;
    private final ObjectMapper objectMapper;
    private final WarningLockSupport warningLockSupport;
    private final WarningStatsCacheSupport warningStatsCacheSupport;
    private final RegulationRegulatorInternalClient regulationRegulatorInternalClient;
    private final String regulationInternalToken;
    private final Random random = new Random();

    public WarningEventServiceImpl(WarningRecordMapper warningRecordMapper,
                                   WarningProcessLogMapper warningProcessLogMapper,
                                   ObjectMapper objectMapper,
                                   WarningLockSupport warningLockSupport,
                                   WarningStatsCacheSupport warningStatsCacheSupport,
                                   RegulationRegulatorInternalClient regulationRegulatorInternalClient,
                                   @Value("${regulation.internal.token:regulation-internal-token}")
                                   String regulationInternalToken) {
        this.warningRecordMapper = warningRecordMapper;
        this.warningProcessLogMapper = warningProcessLogMapper;
        this.objectMapper = objectMapper;
        this.warningLockSupport = warningLockSupport;
        this.warningStatsCacheSupport = warningStatsCacheSupport;
        this.regulationRegulatorInternalClient = regulationRegulatorInternalClient;
        this.regulationInternalToken = regulationInternalToken;
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
            syncOwnerAssignment(created, dto.getOwnerRegulatorId());
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
            warningStatsCacheSupport.bumpVersion();
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
            syncOwnerAssignment(existing, dto.getOwnerRegulatorId());
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
        warningStatsCacheSupport.bumpVersion();
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WarningRecordDetailVO processWarningAction(Long warningId,
                                                      String actionType,
                                                      String actionComment,
                                                      Long operatorId,
                                                      String operatorName,
                                                      WarningScopeDTO scopeDTO) {
        WarningActionType normalized = normalizeActionType(actionType);
        return warningLockSupport.executeWithLock(
            "warning-action",
            warningId,
            () -> doProcessWarning(
                warningId,
                normalized,
                actionComment,
                operatorId,
                operatorName,
                scopeDTO,
                null
            )
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WarningRecordDetailVO assignWarning(Long warningId,
                                               WarningAssignDTO assignDTO,
                                               Long operatorId,
                                               String operatorName,
                                               WarningScopeDTO scopeDTO) {
        if (assignDTO == null || assignDTO.getAssignedTo() == null) {
            throw new IllegalArgumentException("assignedTo required");
        }
        return warningLockSupport.executeWithLock(
            "warning-action",
            warningId,
            () -> doProcessWarning(
                warningId,
                WarningActionType.ASSIGN,
                assignDTO.getActionComment(),
                operatorId,
                operatorName,
                scopeDTO,
                assignDTO.getAssignedTo()
            )
        );
    }

    @Override
    public List<WarningProcessLogVO> listRecentWarningLogs(WarningScopeDTO scopeDTO, Integer limit) {
        ensureScope(scopeDTO);
        int safeLimit = normalizeRecentLimit(limit);
        int candidateLimit = Math.max(safeLimit * 5, 30);
        List<WarningProcessLog> logs = warningProcessLogMapper.selectList(
            new LambdaQueryWrapper<WarningProcessLog>()
                .eq(WarningProcessLog::getDeleted, 0)
                .orderByDesc(WarningProcessLog::getCreateTime)
                .orderByDesc(WarningProcessLog::getId)
                .last("limit " + candidateLimit)
        );
        if (logs.isEmpty()) {
            return List.of();
        }
        Map<Long, WarningRecord> warningMap = warningRecordMapper.selectBatchIds(
                logs.stream()
                    .map(WarningProcessLog::getWarningId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList()
            ).stream()
            .filter(Objects::nonNull)
            .filter(item -> item.getDeleted() == null || item.getDeleted() == 0)
            .collect(Collectors.toMap(WarningRecord::getId, item -> item, (a, b) -> a, HashMap::new));
        return logs.stream()
            .map(log -> toScopedProcessLogVO(log, warningMap.get(log.getWarningId()), scopeDTO))
            .filter(Objects::nonNull)
            .limit(safeLimit)
            .toList();
    }

    private WarningRecordDetailVO doProcessWarning(Long warningId,
                                                   WarningActionType actionType,
                                                   String actionComment,
                                                   Long operatorId,
                                                   String operatorName,
                                                   WarningScopeDTO scopeDTO,
                                                   Long assignedTo) {
        WarningRecord record = loadWarningRecord(warningId);
        ensureInScope(record, scopeDTO);
        WarningStatus targetStatus = resolveTargetStatus(record.getStatus(), actionType);
        LocalDateTime now = LocalDateTime.now();
        applyActionMutation(record, actionType, actionComment, operatorId, assignedTo, now);
        record.setStatus(targetStatus.name());
        record.setUpdateTime(now);
        warningRecordMapper.updateById(record);

        WarningProcessLog processLog = new WarningProcessLog();
        processLog.setWarningId(record.getId());
        processLog.setActionType(actionType.name());
        processLog.setOperatorId(operatorId);
        processLog.setOperatorName(resolveOperatorName(operatorId, operatorName));
        processLog.setActionComment(normalizeOptional(actionComment));
        processLog.setCreateTime(now);
        processLog.setUpdateTime(now);
        processLog.setDeleted(0);
        warningProcessLogMapper.insert(processLog);
        warningStatsCacheSupport.bumpVersion();
        return toDetailVO(record);
    }

    /**
     * 应用动作变更
     * @param record 预警记录
     * @param actionType 动作类型
     * @param actionComment 动作备注
     * @param operatorId 操作员ID
     * @param assignedTo 指派处理人ID
     * @param now 当前时间
     */
    private void applyActionMutation(WarningRecord record,
                                     WarningActionType actionType,
                                     String actionComment,
                                     Long operatorId,
                                     Long assignedTo,
                                     LocalDateTime now) {
        if (actionType == WarningActionType.ASSIGN) {
            if (assignedTo == null || assignedTo <= 0) {
                throw new IllegalArgumentException("assignedTo required");
            }
            record.setAssignedTo(assignedTo);
            record.setAssignedTime(now);
            return;
        }
        if (actionType == WarningActionType.RESOLVE) {
            if (operatorId != null && operatorId > 0) {
                record.setResolvedBy(operatorId);
            }
            record.setResolvedTime(now);
        }
    }

    /**
     * 同步责任执法员（包保责任人），不写入 assigned_to。
     */
    private void syncOwnerAssignment(WarningRecord record, Long regulatorId) {
        if (record == null) {
            return;
        }
        if (regulatorId == null || regulatorId <= 0) {
            record.setOwnerRegulatorId(null);
            return;
        }
        record.setOwnerRegulatorId(regulatorId);
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
        processLog.setOperatorName("系统");
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
        ).stream().map(logItem -> toProcessLogVO(logItem, null, null)).toList();
    }

    private WarningProcessLogVO toProcessLogVO(WarningProcessLog logItem,
                                               WarningRecord record,
                                               WarningScopeDTO scopeDTO) {
        if (logItem == null) {
            return null;
        }
        if (record == null && scopeDTO != null) {
            return null;
        }
        if (record != null) {
            ensureInScope(record, scopeDTO);
        }
        WarningProcessLogVO vo = new WarningProcessLogVO();
        vo.setId(logItem.getId());
        vo.setWarningId(logItem.getWarningId());
        vo.setActionType(logItem.getActionType());
        vo.setOperatorId(logItem.getOperatorId());
        vo.setOperatorName(logItem.getOperatorName());
        vo.setActionComment(logItem.getActionComment());
        vo.setCreateTime(logItem.getCreateTime());
        if (record != null) {
            vo.setWarningNo(record.getWarningNo());
            vo.setWarningTitle(record.getTitle());
            vo.setWarningStatus(record.getStatus());
            vo.setWarningType(record.getWarningType());
            vo.setBizType(record.getBizType());
            vo.setBizId(record.getBizId());
            vo.setRegionId(record.getRegionId());
            vo.setOwnerRegulatorId(record.getOwnerRegulatorId());
            vo.setAssignedTo(record.getAssignedTo());
            vo.setResolvedBy(record.getResolvedBy());
        }
        return vo;
    }

    private WarningProcessLogVO toScopedProcessLogVO(WarningProcessLog logItem,
                                                     WarningRecord record,
                                                     WarningScopeDTO scopeDTO) {
        try {
            return toProcessLogVO(logItem, record, scopeDTO);
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage();
            if ("warning not found".equalsIgnoreCase(message)) {
                return null;
            }
            throw ex;
        }
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

    private String resolveOperatorName(Long operatorId, String fallbackName) {
        if (operatorId != null && operatorId > 0) {
            try {
                ApiResponse<InternalRegulatorSummaryVO> response =
                    regulationRegulatorInternalClient.getRegulatorById(operatorId, regulationInternalToken);
                if (response != null && response.getCode() == 0 && response.getData() != null) {
                    String realName = normalizeOptional(response.getData().getName());
                    String username = normalizeOptional(response.getData().getUsername());
                    if (StringUtils.hasText(realName) && StringUtils.hasText(username)) {
                        return realName + "（" + username + "）";
                    }
                    if (StringUtils.hasText(realName)) {
                        return realName;
                    }
                    if (StringUtils.hasText(username)) {
                        return username;
                    }
                }
            } catch (Exception ignored) {
                // fallback below
            }
        }
        return StringUtils.hasText(fallbackName) ? fallbackName.trim() : "系统";
    }

    private WarningActionType normalizeActionType(String actionType) {
        WarningActionType normalized = WarningActionType.fromValue(actionType);
        Set<WarningActionType> supported = Set.of(
            WarningActionType.PROCESS,
            WarningActionType.RESOLVE
        );
        if (!supported.contains(normalized)) {
            throw new IllegalArgumentException("unsupported actionType");
        }
        return normalized;
    }

    private WarningStatus resolveTargetStatus(String currentStatus, WarningActionType actionType) {
        WarningStatus normalizedStatus = WarningStatus.fromValue(currentStatus);
        switch (actionType) {
            case PROCESS -> {
                ensureAllowed(normalizedStatus, Set.of(WarningStatus.OPEN), actionType);
                return WarningStatus.PROCESSING;
            }
            case ASSIGN -> {
                ensureAllowed(
                    normalizedStatus,
                    Set.of(WarningStatus.OPEN, WarningStatus.PROCESSING),
                    actionType
                );
                return normalizedStatus;
            }
            case RESOLVE -> {
                ensureAllowed(normalizedStatus, Set.of(WarningStatus.PROCESSING), actionType);
                return WarningStatus.RESOLVED;
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
        ensureScope(queryDTO.getAssignedTo(), regionIdSet);
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
        if (queryDTO.getAssignedTo() != null) {
            wrapper.eq(WarningRecord::getAssignedTo, queryDTO.getAssignedTo());
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
        Long assignedScope = scopeDTO.getAssignedRegulatorId();
        if (assignedScope != null && assignedScope > 0) {
            if (record.getAssignedTo() == null || !assignedScope.equals(record.getAssignedTo())) {
                throw new IllegalArgumentException("warning not found");
            }
            return;
        }
        Set<Long> regionIds = parseRegionIds(scopeDTO.getRegionIds());
        ensureScope(null, regionIds);
        if (!regionIds.isEmpty() && (record.getRegionId() == null || !regionIds.contains(record.getRegionId()))) {
            throw new IllegalArgumentException("warning not found");
        }
    }

    private void ensureScope(WarningScopeDTO scopeDTO) {
        if (scopeDTO == null) {
            throw new IllegalArgumentException("scope required");
        }
        ensureScope(scopeDTO.getAssignedRegulatorId(), parseRegionIds(scopeDTO.getRegionIds()));
    }

    private void ensureScope(Long assignedRegulatorId, Set<Long> regionIds) {
        if (assignedRegulatorId != null && assignedRegulatorId > 0) {
            return;
        }
        if (regionIds != null && !regionIds.isEmpty()) {
            return;
        }
        throw new IllegalArgumentException("scope required");
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

    private int normalizeRecentLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 10;
        }
        return Math.min(limit, 20);
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
