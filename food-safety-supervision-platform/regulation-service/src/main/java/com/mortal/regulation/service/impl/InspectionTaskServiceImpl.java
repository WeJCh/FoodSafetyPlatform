package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.dto.InspectionItemDTO;
import com.mortal.regulation.dto.InspectionSubmitDTO;
import com.mortal.regulation.dto.InspectionTaskAssignDTO;
import com.mortal.regulation.dto.InspectionTaskCreateDTO;
import com.mortal.regulation.entity.AddrRegion;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.entity.FoodRegulator;
import com.mortal.regulation.entity.FoodRegulatorRegion;
import com.mortal.regulation.entity.InspectionItem;
import com.mortal.regulation.entity.InspectionRecord;
import com.mortal.regulation.entity.InspectionTask;
import com.mortal.regulation.mapper.AddrRegionMapper;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.mapper.FoodRegulatorMapper;
import com.mortal.regulation.mapper.FoodRegulatorRegionMapper;
import com.mortal.regulation.mapper.InspectionItemMapper;
import com.mortal.regulation.mapper.InspectionRecordMapper;
import com.mortal.regulation.mapper.InspectionTaskMapper;
import com.mortal.regulation.service.InspectionTaskService;
import com.mortal.regulation.service.RectificationService;
import com.mortal.regulation.vo.InspectionTaskVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InspectionTaskServiceImpl implements InspectionTaskService {

    private static final String ROLE_ADMIN = "REGULATOR_ADMIN";
    private static final String ROLE_ENFORCER = "REGULATOR_ENFORCER";
    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_ASSIGNED = "ASSIGNED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String PRIORITY_LOW = "LOW";
    private static final String PRIORITY_MEDIUM = "MEDIUM";
    private static final String PRIORITY_HIGH = "HIGH";

    private final InspectionTaskMapper inspectionTaskMapper;
    private final InspectionRecordMapper inspectionRecordMapper;
    private final InspectionItemMapper inspectionItemMapper;
    private final FoodEnterpriseMapper foodEnterpriseMapper;
    private final FoodRegulatorMapper foodRegulatorMapper;
    private final FoodRegulatorRegionMapper foodRegulatorRegionMapper;
    private final AddrRegionMapper addrRegionMapper;
    private final RectificationService rectificationService;

    public InspectionTaskServiceImpl(InspectionTaskMapper inspectionTaskMapper,
                                     InspectionRecordMapper inspectionRecordMapper,
                                     InspectionItemMapper inspectionItemMapper,
                                     FoodEnterpriseMapper foodEnterpriseMapper,
                                     FoodRegulatorMapper foodRegulatorMapper,
                                     FoodRegulatorRegionMapper foodRegulatorRegionMapper,
                                     AddrRegionMapper addrRegionMapper,
                                     RectificationService rectificationService) {
        this.inspectionTaskMapper = inspectionTaskMapper;
        this.inspectionRecordMapper = inspectionRecordMapper;
        this.inspectionItemMapper = inspectionItemMapper;
        this.foodEnterpriseMapper = foodEnterpriseMapper;
        this.foodRegulatorMapper = foodRegulatorMapper;
        this.foodRegulatorRegionMapper = foodRegulatorRegionMapper;
        this.addrRegionMapper = addrRegionMapper;
        this.rectificationService = rectificationService;
    }

    @Override
    public InspectionTaskVO createTask(Long userId, InspectionTaskCreateDTO dto) {
        FoodRegulator regulator = requireRegulator(userId);
        requireRole(regulator, ROLE_ADMIN);
        FoodEnterprise enterprise = requireEnterprise(dto.getEnterpriseId());
        validateCreateDeadline(dto.getDeadline());
        if (!"APPROVED".equalsIgnoreCase(enterprise.getApprovalStatus())) {
            throw new IllegalArgumentException("enterprise not approved");
        }
        if (!coversRegion(regulator.getId(), enterprise.getRegionId())) {
            throw new IllegalArgumentException("enterprise not in regulator region");
        }
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
        FoodRegulator operator = requireRegulator(userId);
        requireRole(operator, ROLE_ADMIN);
        InspectionTask task = requireTask(taskId);
        ensureTaskNotExpired(task, "assign");
        if (STATUS_IN_PROGRESS.equals(task.getStatus())
            || STATUS_COMPLETED.equals(task.getStatus())
            || STATUS_CLOSED.equals(task.getStatus())) {
            throw new IllegalArgumentException("task already started");
        }
        FoodRegulator assignee = foodRegulatorMapper.selectById(dto.getRegulatorId());
        if (assignee == null || isDeleted(assignee.getDeleted())) {
            throw new IllegalArgumentException("assignee not found");
        }
        if (!ROLE_ENFORCER.equalsIgnoreCase(assignee.getRoleType())) {
            throw new IllegalArgumentException("assignee must be enforcer");
        }
        if (!isRegulatorWithinRegion(assignee.getId(), task.getRegionId())) {
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
        FoodRegulator regulator = requireRegulator(userId);
        requireRole(regulator, ROLE_ADMIN);
        List<Long> regionIds = resolveRegulatorRegionIds(regulator.getId());
        if (regionIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        List<Long> enterpriseIds = resolveEnterpriseIds(enterpriseName);
        if (StringUtils.hasText(enterpriseName) && enterpriseIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        LambdaQueryWrapper<InspectionTask> wrapper = new LambdaQueryWrapper<InspectionTask>()
            .eq(InspectionTask::getDeleted, 0)
            .in(InspectionTask::getRegionId, regionIds);
        if (StringUtils.hasText(status)) {
            wrapper.eq(InspectionTask::getStatus, normalize(status));
        }
        if (enterpriseIds != null) {
            wrapper.in(InspectionTask::getEnterpriseId, enterpriseIds);
        }
        wrapper.orderByDesc(InspectionTask::getUpdateTime);
        Page<InspectionTask> pageInfo = inspectionTaskMapper.selectPage(new Page<>(page, size), wrapper);
        List<InspectionTask> tasks = pageInfo.getRecords();
        List<InspectionTaskVO> records = toVOs(tasks);
        return PageResult.of(records, pageInfo.getTotal(), page, size);
    }

    @Override
    public PageResult<InspectionTaskVO> listTasksForEnforcer(Long userId,
                                                             String status,
                                                             int page,
                                                             int size) {
        FoodRegulator regulator = requireRegulator(userId);
        requireRole(regulator, ROLE_ENFORCER);
        LambdaQueryWrapper<InspectionTask> wrapper = new LambdaQueryWrapper<InspectionTask>()
            .eq(InspectionTask::getDeleted, 0)
            .eq(InspectionTask::getAssignedTo, regulator.getId());
        if (StringUtils.hasText(status)) {
            wrapper.eq(InspectionTask::getStatus, normalize(status));
        }
        wrapper.orderByDesc(InspectionTask::getUpdateTime);
        Page<InspectionTask> pageInfo = inspectionTaskMapper.selectPage(new Page<>(page, size), wrapper);
        List<InspectionTask> tasks = pageInfo.getRecords();
        List<InspectionTaskVO> records = toVOs(tasks);
        return PageResult.of(records, pageInfo.getTotal(), page, size);
    }

    @Override
    public InspectionTaskVO startTask(Long userId, Long taskId) {
        FoodRegulator regulator = requireRegulator(userId);
        requireRole(regulator, ROLE_ENFORCER);
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
        FoodRegulator regulator = requireRegulator(userId);
        requireRole(regulator, ROLE_ENFORCER);
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
            // 检查结论或任一检查项不合格时，自动创建整改任务（同一检查记录幂等）。
            rectificationService.createFromInspection(
                record.getId(),
                task.getEnterpriseId(),
                buildRectificationDesc(dto, items)
            );
        }

        task.setStatus(STATUS_COMPLETED);
        task.setCompletedTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        inspectionTaskMapper.updateById(task);
        return toVO(task, loadEnterpriseNames(List.of(task)), loadRegulatorNames(List.of(task)));
    }

    @Override
    public InspectionTaskVO closeTask(Long userId, Long taskId) {
        FoodRegulator regulator = requireRegulator(userId);
        requireRole(regulator, ROLE_ADMIN);
        InspectionTask task = requireTask(taskId);
        if (!coversRegion(regulator.getId(), task.getRegionId())) {
            throw new IllegalArgumentException("task not in regulator region");
        }
        if (!STATUS_COMPLETED.equals(task.getStatus())) {
            throw new IllegalArgumentException("task not completed");
        }
        task.setStatus(STATUS_CLOSED);
        task.setUpdateTime(LocalDateTime.now());
        inspectionTaskMapper.updateById(task);
        return toVO(task, loadEnterpriseNames(List.of(task)), loadRegulatorNames(List.of(task)));
    }

    private FoodRegulator requireRegulator(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        FoodRegulator regulator = foodRegulatorMapper.selectOne(new LambdaQueryWrapper<FoodRegulator>()
            .eq(FoodRegulator::getUserId, userId)
            .eq(FoodRegulator::getDeleted, 0));
        if (regulator == null) {
            throw new IllegalArgumentException("regulator not found");
        }
        if (regulator.getStatus() != null && regulator.getStatus() != 1) {
            throw new IllegalArgumentException("regulator disabled");
        }
        return regulator;
    }

    private void requireRole(FoodRegulator regulator, String roleType) {
        if (regulator == null || !roleType.equalsIgnoreCase(regulator.getRoleType())) {
            throw new IllegalArgumentException("invalid regulator role");
        }
    }

    private FoodEnterprise requireEnterprise(Long enterpriseId) {
        FoodEnterprise enterprise = foodEnterpriseMapper.selectById(enterpriseId);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            throw new IllegalArgumentException("enterprise not found");
        }
        return enterprise;
    }

    private InspectionTask requireTask(Long taskId) {
        InspectionTask task = inspectionTaskMapper.selectById(taskId);
        if (task == null || isDeleted(task.getDeleted())) {
            throw new IllegalArgumentException("task not found");
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
        return foodEnterpriseMapper.selectBatchIds(enterpriseIds)
            .stream()
            .filter(enterprise -> !isDeleted(enterprise.getDeleted()))
            .collect(Collectors.toMap(FoodEnterprise::getId, FoodEnterprise::getEnterpriseName, (a, b) -> a));
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
        return foodRegulatorMapper.selectBatchIds(regulatorIds)
            .stream()
            .filter(regulator -> !isDeleted(regulator.getDeleted()))
            .collect(Collectors.toMap(FoodRegulator::getId, FoodRegulator::getName, (a, b) -> a));
    }

    private List<Long> resolveRegulatorRegionIds(Long regulatorId) {
        if (regulatorId == null) {
            return List.of();
        }
        List<Long> directRegionIds = foodRegulatorRegionMapper.selectList(new LambdaQueryWrapper<FoodRegulatorRegion>()
                .eq(FoodRegulatorRegion::getRegulatorId, regulatorId)
                .eq(FoodRegulatorRegion::getDeleted, 0))
            .stream()
            .map(FoodRegulatorRegion::getRegionId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (directRegionIds.isEmpty()) {
            return List.of();
        }
        return collectRegionIds(directRegionIds);
    }

    private List<Long> resolveRegulatorDirectRegionIds(Long regulatorId) {
        if (regulatorId == null) {
            return List.of();
        }
        return foodRegulatorRegionMapper.selectList(new LambdaQueryWrapper<FoodRegulatorRegion>()
                .eq(FoodRegulatorRegion::getRegulatorId, regulatorId)
                .eq(FoodRegulatorRegion::getDeleted, 0))
            .stream()
            .map(FoodRegulatorRegion::getRegionId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private boolean coversRegion(Long regulatorId, Long regionId) {
        if (regionId == null) {
            return false;
        }
        List<Long> regionIds = resolveRegulatorRegionIds(regulatorId);
        return regionIds.contains(regionId);
    }

    private boolean isRegulatorWithinRegion(Long regulatorId, Long regionId) {
        if (regionId == null) {
            return false;
        }
        List<Long> directRegionIds = resolveRegulatorDirectRegionIds(regulatorId);
        if (directRegionIds.isEmpty()) {
            return false;
        }
        for (Long directId : directRegionIds) {
            if (isAncestorRegion(regionId, directId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAncestorRegion(Long ancestorId, Long regionId) {
        if (ancestorId == null || regionId == null) {
            return false;
        }
        Long cursor = regionId;
        while (cursor != null) {
            if (ancestorId.equals(cursor)) {
                return true;
            }
            AddrRegion current = addrRegionMapper.selectById(cursor);
            if (current == null || isDeleted(current.getDeleted())) {
                break;
            }
            cursor = current.getParentId();
        }
        return false;
    }

    private List<Long> collectRegionIds(List<Long> rootIds) {
        Set<Long> result = new LinkedHashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>(rootIds);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current == null || result.contains(current)) {
                continue;
            }
            result.add(current);
            List<AddrRegion> children = addrRegionMapper.selectList(new LambdaQueryWrapper<AddrRegion>()
                .eq(AddrRegion::getParentId, current)
                .eq(AddrRegion::getDeleted, 0));
            for (AddrRegion child : children) {
                queue.add(child.getId());
            }
        }
        return result.stream().toList();
    }

    private List<Long> resolveEnterpriseIds(String enterpriseName) {
        if (!StringUtils.hasText(enterpriseName)) {
            return null;
        }
        List<FoodEnterprise> enterprises = foodEnterpriseMapper.selectList(new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getDeleted, 0)
            .like(FoodEnterprise::getEnterpriseName, enterpriseName.trim()));
        if (enterprises.isEmpty()) {
            return List.of();
        }
        return enterprises.stream()
            .map(FoodEnterprise::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
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
            throw new IllegalArgumentException("task not found");
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

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}
