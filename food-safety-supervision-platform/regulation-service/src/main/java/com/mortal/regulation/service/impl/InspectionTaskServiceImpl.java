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

    public InspectionTaskServiceImpl(InspectionTaskMapper inspectionTaskMapper,
                                     InspectionRecordMapper inspectionRecordMapper,
                                     InspectionItemMapper inspectionItemMapper,
                                     FoodEnterpriseMapper foodEnterpriseMapper,
                                     FoodRegulatorMapper foodRegulatorMapper,
                                     FoodRegulatorRegionMapper foodRegulatorRegionMapper,
                                     AddrRegionMapper addrRegionMapper) {
        this.inspectionTaskMapper = inspectionTaskMapper;
        this.inspectionRecordMapper = inspectionRecordMapper;
        this.inspectionItemMapper = inspectionItemMapper;
        this.foodEnterpriseMapper = foodEnterpriseMapper;
        this.foodRegulatorMapper = foodRegulatorMapper;
        this.foodRegulatorRegionMapper = foodRegulatorRegionMapper;
        this.addrRegionMapper = addrRegionMapper;
    }

    @Override
    public InspectionTaskVO createTask(Long userId, InspectionTaskCreateDTO dto) {
        FoodRegulator regulator = requireRegulator(userId);
        requireRole(regulator, ROLE_ADMIN);
        FoodEnterprise enterprise = requireEnterprise(dto.getEnterpriseId());
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
        if (!coversRegion(assignee.getId(), task.getRegionId())) {
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
        if (items != null && !items.isEmpty()) {
            for (InspectionItemDTO itemDTO : items) {
                if (!StringUtils.hasText(itemDTO.getItemName())) {
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
            }
        }

        task.setStatus(STATUS_COMPLETED);
        task.setCompletedTime(LocalDateTime.now());
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

    private boolean coversRegion(Long regulatorId, Long regionId) {
        if (regionId == null) {
            return false;
        }
        List<Long> regionIds = resolveRegulatorRegionIds(regulatorId);
        return regionIds.contains(regionId);
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

    private String generateTaskNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 9000) + 1000;
        return "TSK" + time + random;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}
