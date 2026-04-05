package com.mortal.regulation.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.operation.client.regulation.vo.InternalEnterpriseDetailVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.regulation.operation.common.OperationErrorMessages;
import com.mortal.regulation.operation.dto.InspectionItemDTO;
import com.mortal.regulation.operation.dto.InspectionSubmitDTO;
import com.mortal.regulation.operation.dto.InspectionTaskAssignDTO;
import com.mortal.regulation.operation.dto.InspectionTaskCreateDTO;
import com.mortal.regulation.operation.dto.WarningEventUpsertDTO;
import com.mortal.regulation.operation.entity.InspectionItem;
import com.mortal.regulation.operation.entity.InspectionRecord;
import com.mortal.regulation.operation.entity.InspectionTask;
import com.mortal.regulation.operation.mapper.InspectionItemMapper;
import com.mortal.regulation.operation.mapper.InspectionRecordMapper;
import com.mortal.regulation.operation.mapper.InspectionTaskMapper;
import com.mortal.regulation.operation.service.InspectionTaskService;
import com.mortal.regulation.operation.service.RectificationService;
import com.mortal.regulation.operation.service.WarningEventOutboxService;
import com.mortal.regulation.operation.support.OperationMasterDataSupport;
import com.mortal.regulation.operation.vo.InspectionTaskVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InspectionTaskServiceImpl implements InspectionTaskService {

    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_ASSIGNED = "ASSIGNED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String PRIORITY_LOW = "LOW";
    private static final String PRIORITY_MEDIUM = "MEDIUM";
    private static final String PRIORITY_HIGH = "HIGH";
    private static final String RESULT_FAIL = "FAIL";
    private static final String KEY_REASON_CONSECUTIVE_FAIL = "CONSECUTIVE_FAIL";
    private static final String KEY_SOURCE_ROUTINE = "ROUTINE";
    private static final String WARNING_BIZ_TYPE_INSPECTION = "INSPECTION";
    private static final String WARNING_EVENT_CONSECUTIVE_FAIL = "INSPECTION_CONSECUTIVE_FAIL";
    private static final String WARNING_SOURCE_SERVICE = "regulation-operation-service";

    private final InspectionTaskMapper inspectionTaskMapper;
    private final InspectionRecordMapper inspectionRecordMapper;
    private final InspectionItemMapper inspectionItemMapper;
    private final OperationMasterDataSupport masterDataSupport;
    private final RectificationService rectificationService;
    private final WarningEventOutboxService warningEventOutboxService;

    @Value("${regulation.inspection.key-threshold:2}")
    private int consecutiveFailThreshold = 2;

    public InspectionTaskServiceImpl(InspectionTaskMapper inspectionTaskMapper,
                                     InspectionRecordMapper inspectionRecordMapper,
                                     InspectionItemMapper inspectionItemMapper,
                                     OperationMasterDataSupport masterDataSupport,
                                     RectificationService rectificationService,
                                     WarningEventOutboxService warningEventOutboxService) {
        this.inspectionTaskMapper = inspectionTaskMapper;
        this.inspectionRecordMapper = inspectionRecordMapper;
        this.inspectionItemMapper = inspectionItemMapper;
        this.masterDataSupport = masterDataSupport;
        this.rectificationService = rectificationService;
        this.warningEventOutboxService = warningEventOutboxService;
    }

    @Override
    public InspectionTaskVO createTask(Long userId, InspectionTaskCreateDTO dto) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireAdmin(userId);
        InternalEnterpriseDetailVO enterprise = masterDataSupport.requireApprovedEnterprise(dto.getEnterpriseId());
        validateCreateDeadline(dto.getDeadline());
        masterDataSupport.requireEnterpriseInScope(regulator.getId(), enterprise.getId());
        InspectionTask task = new InspectionTask();
        task.setTaskNo(generateTaskNo());
        task.setEnterpriseId(enterprise.getId());
        task.setRegionId(enterprise.getRegionId());
        task.setTaskTitle(dto.getTaskTitle().trim());
        task.setTaskDesc(StringUtils.hasText(dto.getTaskDesc()) ? dto.getTaskDesc().trim() : null);
        task.setPriority(normalizePriority(dto.getPriority()));
        task.setStatus(STATUS_CREATED);
        task.setCreatedBy(regulator.getId());
        task.setDeadline(dto.getDeadline());
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        task.setDeleted(0);
        inspectionTaskMapper.insert(task);
        return toVO(task, Map.of(enterprise.getId(), enterprise.getEnterpriseName()), loadRegulatorNames(List.of(task)));
    }

    @Override
    public InspectionTaskVO assignTask(Long userId, Long taskId, InspectionTaskAssignDTO dto) {
        InternalRegulatorIdentityVO operator = masterDataSupport.requireAdmin(userId);
        InspectionTask task = requireTask(taskId);
        ensureTaskNotExpired(task, "assign");
        if (STATUS_IN_PROGRESS.equals(task.getStatus())
            || STATUS_COMPLETED.equals(task.getStatus())
            || STATUS_CLOSED.equals(task.getStatus())) {
            throw new IllegalArgumentException("task already started");
        }
        InternalRegulatorIdentityVO assignee = masterDataSupport.requireRegulatorById(dto.getRegulatorId());
        masterDataSupport.requireRole(assignee, OperationMasterDataSupport.ROLE_ENFORCER);
        if (!masterDataSupport.isRegulatorAssignableToRegion(assignee.getId(), task.getRegionId())) {
            throw new IllegalArgumentException("assignee not in task region");
        }
        task.setAssignedTo(assignee.getId());
        task.setAssignedBy(operator.getId());
        task.setAssignedTime(LocalDateTime.now());
        task.setStatus(STATUS_ASSIGNED);
        task.setUpdateTime(LocalDateTime.now());
        inspectionTaskMapper.updateById(task);
        return toVO(task, loadEnterpriseNames(List.of(task)), loadRegulatorNames(List.of(task)));
    }

    @Override
    public PageResult<InspectionTaskVO> listTasksForAdmin(Long userId,
                                                          String enterpriseName,
                                                          String status,
                                                          int page,
                                                          int size) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireAdmin(userId);
        List<Long> scopeRegionIds = masterDataSupport.resolveScopeRegionIds(regulator.getId());
        if (scopeRegionIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        List<Long> enterpriseIds = resolveEnterpriseIds(enterpriseName);
        if (StringUtils.hasText(enterpriseName) && enterpriseIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        LambdaQueryWrapper<InspectionTask> wrapper = new LambdaQueryWrapper<InspectionTask>()
            .eq(InspectionTask::getDeleted, 0)
            .in(InspectionTask::getRegionId, scopeRegionIds);
        if (StringUtils.hasText(status)) {
            wrapper.eq(InspectionTask::getStatus, normalize(status));
        }
        if (enterpriseIds != null) {
            wrapper.in(InspectionTask::getEnterpriseId, enterpriseIds);
        }
        wrapper.orderByDesc(InspectionTask::getUpdateTime);
        Page<InspectionTask> pageInfo = inspectionTaskMapper.selectPage(new Page<>(page, size), wrapper);
        List<InspectionTaskVO> records = toVOs(pageInfo.getRecords());
        return PageResult.of(records, pageInfo.getTotal(), page, size);
    }

    @Override
    public PageResult<InspectionTaskVO> listTasksForEnforcer(Long userId,
                                                             String status,
                                                             int page,
                                                             int size) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireEnforcer(userId);
        LambdaQueryWrapper<InspectionTask> wrapper = new LambdaQueryWrapper<InspectionTask>()
            .eq(InspectionTask::getDeleted, 0)
            .eq(InspectionTask::getAssignedTo, regulator.getId());
        if (StringUtils.hasText(status)) {
            wrapper.eq(InspectionTask::getStatus, normalize(status));
        }
        wrapper.orderByDesc(InspectionTask::getUpdateTime);
        Page<InspectionTask> pageInfo = inspectionTaskMapper.selectPage(new Page<>(page, size), wrapper);
        List<InspectionTaskVO> records = toVOs(pageInfo.getRecords());
        return PageResult.of(records, pageInfo.getTotal(), page, size);
    }

    @Override
    public InspectionTaskVO startTask(Long userId, Long taskId) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireEnforcer(userId);
        InspectionTask task = requireTask(taskId);
        ensureTaskNotExpired(task, "start");
        if (!Objects.equals(task.getAssignedTo(), regulator.getId())) {
            throw new IllegalArgumentException("task not assigned to you");
        }
        if (!STATUS_ASSIGNED.equals(task.getStatus())) {
            throw new IllegalArgumentException("task not ready to start");
        }
        task.setStatus(STATUS_IN_PROGRESS);
        task.setStartedTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        inspectionTaskMapper.updateById(task);
        return toVO(task, loadEnterpriseNames(List.of(task)), loadRegulatorNames(List.of(task)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionTaskVO submitTask(Long userId, Long taskId, InspectionSubmitDTO dto) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireEnforcer(userId);
        InspectionTask task = requireTask(taskId);
        if (!Objects.equals(task.getAssignedTo(), regulator.getId())) {
            throw new IllegalArgumentException("task not assigned to you");
        }
        if (!STATUS_IN_PROGRESS.equals(task.getStatus())) {
            throw new IllegalArgumentException("task not in progress");
        }
        InspectionRecord record = new InspectionRecord();
        record.setTaskId(task.getId());
        record.setEnterpriseId(task.getEnterpriseId());
        record.setInspectorId(regulator.getId());
        record.setInspectionDate(dto.getInspectionDate() == null ? LocalDate.now() : dto.getInspectionDate());
        record.setResult(normalize(dto.getResult()));
        record.setProblemDesc(StringUtils.hasText(dto.getProblemDesc()) ? dto.getProblemDesc().trim() : null);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        record.setDeleted(0);
        inspectionRecordMapper.insert(record);

        List<InspectionItemDTO> items = dto.getItems();
        int insertedItemCount = 0;
        if (items != null && !items.isEmpty()) {
            for (InspectionItemDTO itemDTO : items) {
                if (itemDTO == null || !StringUtils.hasText(itemDTO.getItemName())) {
                    continue;
                }
                InspectionItem item = new InspectionItem();
                item.setInspectionId(record.getId());
                item.setItemName(itemDTO.getItemName().trim());
                item.setItemResult(normalize(itemDTO.getItemResult()));
                item.setProblemDesc(StringUtils.hasText(itemDTO.getProblemDesc()) ? itemDTO.getProblemDesc().trim() : null);
                item.setCreateTime(LocalDateTime.now());
                item.setUpdateTime(LocalDateTime.now());
                item.setDeleted(0);
                inspectionItemMapper.insert(item);
                insertedItemCount++;
            }
        }
        if (insertedItemCount <= 0) {
            throw new IllegalArgumentException("at least one inspection item required");
        }

        if (needRectification(record.getResult(), items)) {
            rectificationService.createFromInspection(
                record.getId(),
                task.getEnterpriseId(),
                buildRectificationDesc(dto, items)
            );
        }
        tryMarkConsecutiveFailAsKey(task, record, regulator.getId());

        task.setStatus(STATUS_COMPLETED);
        task.setCompletedTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        inspectionTaskMapper.updateById(task);
        return toVO(task, loadEnterpriseNames(List.of(task)), loadRegulatorNames(List.of(task)));
    }

    @Override
    public InspectionTaskVO closeTask(Long userId, Long taskId) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireAdmin(userId);
        InspectionTask task = requireTask(taskId);
        masterDataSupport.requireRegionInScope(regulator.getId(), task.getRegionId());
        if (!STATUS_COMPLETED.equals(task.getStatus())) {
            throw new IllegalArgumentException("task not completed");
        }
        task.setStatus(STATUS_CLOSED);
        task.setUpdateTime(LocalDateTime.now());
        inspectionTaskMapper.updateById(task);
        return toVO(task, loadEnterpriseNames(List.of(task)), loadRegulatorNames(List.of(task)));
    }

    private InspectionTask requireTask(Long taskId) {
        InspectionTask task = inspectionTaskMapper.selectById(taskId);
        if (task == null || isDeleted(task.getDeleted())) {
            throw new IllegalArgumentException(OperationErrorMessages.TASK_NOT_FOUND);
        }
        return task;
    }

    private List<InspectionTaskVO> toVOs(List<InspectionTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        Map<Long, String> enterpriseNames = loadEnterpriseNames(tasks);
        Map<Long, String> regulatorNames = loadRegulatorNames(tasks);
        return tasks.stream()
            .map(task -> toVO(task, enterpriseNames, regulatorNames))
            .toList();
    }

    private InspectionTaskVO toVO(InspectionTask task,
                                  Map<Long, String> enterpriseNames,
                                  Map<Long, String> regulatorNames) {
        InspectionTaskVO vo = new InspectionTaskVO();
        vo.setId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setEnterpriseId(task.getEnterpriseId());
        vo.setEnterpriseName(enterpriseNames.get(task.getEnterpriseId()));
        vo.setRegionId(task.getRegionId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setTaskDesc(task.getTaskDesc());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setCreatedBy(task.getCreatedBy());
        vo.setCreatedByName(regulatorNames.get(task.getCreatedBy()));
        vo.setAssignedTo(task.getAssignedTo());
        vo.setAssignedToName(regulatorNames.get(task.getAssignedTo()));
        vo.setAssignedBy(task.getAssignedBy());
        vo.setAssignedByName(regulatorNames.get(task.getAssignedBy()));
        vo.setAssignedTime(task.getAssignedTime());
        vo.setStartedTime(task.getStartedTime());
        vo.setCompletedTime(task.getCompletedTime());
        vo.setDeadline(task.getDeadline());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }

    private Map<Long, String> loadEnterpriseNames(List<InspectionTask> tasks) {
        List<Long> enterpriseIds = tasks.stream()
            .map(InspectionTask::getEnterpriseId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (enterpriseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return masterDataSupport.loadEnterpriseNames(enterpriseIds);
    }

    private Map<Long, String> loadRegulatorNames(List<InspectionTask> tasks) {
        Set<Long> regulatorIds = new LinkedHashSet<>();
        for (InspectionTask task : tasks) {
            if (task.getCreatedBy() != null) {
                regulatorIds.add(task.getCreatedBy());
            }
            if (task.getAssignedBy() != null) {
                regulatorIds.add(task.getAssignedBy());
            }
            if (task.getAssignedTo() != null) {
                regulatorIds.add(task.getAssignedTo());
            }
        }
        if (regulatorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return masterDataSupport.loadRegulatorNames(regulatorIds);
    }

    private List<Long> resolveEnterpriseIds(String enterpriseName) {
        return masterDataSupport.queryEnterpriseIdsByName(enterpriseName);
    }

    private String normalizePriority(String value) {
        String normalized = normalize(value);
        if (PRIORITY_LOW.equals(normalized) || PRIORITY_MEDIUM.equals(normalized) || PRIORITY_HIGH.equals(normalized)) {
            return normalized;
        }
        return PRIORITY_MEDIUM;
    }

    private void validateCreateDeadline(LocalDateTime deadline) {
        if (deadline == null) {
            throw new IllegalArgumentException("deadline required");
        }
        if (!deadline.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("deadline must be future");
        }
    }

    private void ensureTaskNotExpired(InspectionTask task, String action) {
        if (task == null) {
            throw new IllegalArgumentException(OperationErrorMessages.TASK_NOT_FOUND);
        }
        LocalDateTime deadline = task.getDeadline();
        if (deadline != null && !deadline.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("task deadline exceeded, cannot " + action);
        }
    }

    private String generateTaskNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 9000) + 1000;
        return "TSK" + time + random;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private boolean needRectification(String recordResult, List<InspectionItemDTO> items) {
        if ("FAIL".equals(recordResult)) {
            return true;
        }
        if (items == null || items.isEmpty()) {
            return false;
        }
        for (InspectionItemDTO item : items) {
            if (item == null) {
                continue;
            }
            if ("FAIL".equals(normalize(item.getItemResult()))) {
                return true;
            }
        }
        return false;
    }

    private String buildRectificationDesc(InspectionSubmitDTO dto, List<InspectionItemDTO> items) {
        StringBuilder builder = new StringBuilder("请针对本次检查不合格项完成整改并提交整改说明。");
        if (StringUtils.hasText(dto.getProblemDesc())) {
            builder.append("\n问题概述：").append(dto.getProblemDesc().trim());
        }
        if (items != null && !items.isEmpty()) {
            String failedItems = items.stream()
                .filter(Objects::nonNull)
                .filter(item -> "FAIL".equals(normalize(item.getItemResult())))
                .map(item -> {
                    String itemName = StringUtils.hasText(item.getItemName()) ? item.getItemName().trim() : "未命名检查项";
                    if (StringUtils.hasText(item.getProblemDesc())) {
                        return itemName + "（" + item.getProblemDesc().trim() + "）";
                    }
                    return itemName;
                })
                .collect(Collectors.joining("、"));
            if (StringUtils.hasText(failedItems)) {
                builder.append("\n不合格项：").append(failedItems);
            }
        }
        return builder.toString();
    }

    /**
     * 尝试标记连续不合格为关键企业
     * @param task 检查任务
     * @param record 检查记录
     * @param operatorId 操作员ID
     */
    private void tryMarkConsecutiveFailAsKey(InspectionTask task, InspectionRecord record, Long operatorId) {
        if (task == null || record == null || !RESULT_FAIL.equals(record.getResult())) {
            return;
        }
        int threshold = Math.max(2, consecutiveFailThreshold);
        List<InspectionRecord> recentRecords = inspectionRecordMapper.selectList(new LambdaQueryWrapper<InspectionRecord>()
            .eq(InspectionRecord::getDeleted, 0)
            .eq(InspectionRecord::getEnterpriseId, task.getEnterpriseId())
            .orderByDesc(InspectionRecord::getInspectionDate, InspectionRecord::getId)
            .last("limit " + threshold));
        if (recentRecords.size() < threshold) {
            return;
        }
        boolean allFailed = recentRecords.stream()
            .allMatch(item -> RESULT_FAIL.equalsIgnoreCase(item.getResult()));
        if (!allFailed) {
            return;
        }
        String reasonDetail = "企业最近" + threshold + "次检查均为不合格，已自动纳入重点监管";
        masterDataSupport.markEnterpriseAsKey(
            task.getEnterpriseId(),
            KEY_REASON_CONSECUTIVE_FAIL,
            reasonDetail,
            KEY_SOURCE_ROUTINE,
            record.getId(),
            operatorId
        );
        ensureInspectionWarning(task, record, threshold, reasonDetail);
    }

    /**
     * 确保检查警告事件
     * @param task 检查任务
     * @param record 检查记录
     * @param consecutiveFailCount 连续不合格次数
     * @param reasonDetail 原因详情
     */
    private void ensureInspectionWarning(InspectionTask task,
                                         InspectionRecord record,
                                         int consecutiveFailCount,
                                         String reasonDetail) {
        LocalDateTime now = LocalDateTime.now();
        String eventKey = buildInspectionWarningKey(record.getId());
        WarningEventUpsertDTO dto = new WarningEventUpsertDTO();
        dto.setEventType(WARNING_EVENT_CONSECUTIVE_FAIL);
        dto.setBizType(WARNING_BIZ_TYPE_INSPECTION);
        dto.setBizId(record.getId());
        dto.setRegionId(task.getRegionId());
        dto.setOwnerRegulatorId(record.getInspectorId());
        dto.setDedupKey(eventKey);
        dto.setLevel("L2");
        dto.setTitle("企业连续检查不合格");
        dto.setContent(reasonDetail);
        dto.setSourceService(WARNING_SOURCE_SERVICE);
        dto.setOccurTime(now);
        dto.setPayload(Map.of(
            "enterpriseId", task.getEnterpriseId(),
            "taskId", task.getId(),
            "inspectionId", record.getId(),
            "consecutiveFailCount", consecutiveFailCount,
            "inspectionDate", record.getInspectionDate()
        ));
        warningEventOutboxService.ensurePendingEvent(eventKey, dto, now);
        warningEventOutboxService.dispatchByEventKey(eventKey);
    }

    private String buildInspectionWarningKey(Long inspectionId) {
        return WARNING_BIZ_TYPE_INSPECTION + ":" + inspectionId + ":" + WARNING_EVENT_CONSECUTIVE_FAIL;
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}
