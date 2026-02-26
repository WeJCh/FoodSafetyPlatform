package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.common.enums.RectificationReviewAction;
import com.mortal.regulation.common.enums.RectificationStatus;
import com.mortal.regulation.dto.RectificationReviewDTO;
import com.mortal.regulation.dto.RectificationSubmitDTO;
import com.mortal.regulation.entity.AddrRegion;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.entity.FoodRegulator;
import com.mortal.regulation.entity.FoodRegulatorRegion;
import com.mortal.regulation.entity.RectificationActionLog;
import com.mortal.regulation.entity.RectificationTask;
import com.mortal.regulation.mapper.AddrRegionMapper;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.mapper.FoodRegulatorMapper;
import com.mortal.regulation.mapper.FoodRegulatorRegionMapper;
import com.mortal.regulation.mapper.RectificationActionLogMapper;
import com.mortal.regulation.mapper.RectificationTaskMapper;
import com.mortal.regulation.service.RectificationService;
import com.mortal.regulation.service.StatusTransitionValidator;
import com.mortal.regulation.vo.RectificationTaskVO;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RectificationServiceImpl implements RectificationService {

    private static final String ROLE_ADMIN = "REGULATOR_ADMIN";
    private static final String DEFAULT_RECTIFICATION_DESC = "请根据检查问题完成整改并提交说明";
    private static final String ACTION_SYSTEM_CREATE = "SYSTEM_CREATE";
    private static final String ACTION_ENTERPRISE_SUBMIT = "ENTERPRISE_SUBMIT";
    private static final String ACTION_REVIEW_CONFIRM = "REVIEW_CONFIRM";
    private static final String ACTION_REVIEW_REWORK = "REVIEW_REWORK";

    private final RectificationTaskMapper rectificationTaskMapper;
    private final RectificationActionLogMapper rectificationActionLogMapper;
    private final FoodEnterpriseMapper foodEnterpriseMapper;
    private final FoodRegulatorMapper foodRegulatorMapper;
    private final FoodRegulatorRegionMapper foodRegulatorRegionMapper;
    private final AddrRegionMapper addrRegionMapper;
    private final ObjectMapper objectMapper;

    public RectificationServiceImpl(RectificationTaskMapper rectificationTaskMapper,
                                    RectificationActionLogMapper rectificationActionLogMapper,
                                    FoodEnterpriseMapper foodEnterpriseMapper,
                                    FoodRegulatorMapper foodRegulatorMapper,
                                    FoodRegulatorRegionMapper foodRegulatorRegionMapper,
                                    AddrRegionMapper addrRegionMapper,
                                    ObjectMapper objectMapper) {
        this.rectificationTaskMapper = rectificationTaskMapper;
        this.rectificationActionLogMapper = rectificationActionLogMapper;
        this.foodEnterpriseMapper = foodEnterpriseMapper;
        this.foodRegulatorMapper = foodRegulatorMapper;
        this.foodRegulatorRegionMapper = foodRegulatorRegionMapper;
        this.addrRegionMapper = addrRegionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFromInspection(Long inspectionId, Long enterpriseId, String rectificationDesc) {
        if (inspectionId == null || enterpriseId == null) {
            return;
        }
        RectificationTask existing = rectificationTaskMapper.selectOne(new LambdaQueryWrapper<RectificationTask>()
            .eq(RectificationTask::getInspectionId, inspectionId)
            .eq(RectificationTask::getDeleted, 0)
            .last("limit 1"));
        if (existing != null) {
            return;
        }
        RectificationTask task = new RectificationTask();
        task.setInspectionId(inspectionId);
        task.setEnterpriseId(enterpriseId);
        task.setRectificationDesc(normalizeRectificationDesc(rectificationDesc));
        task.setStatus(RectificationStatus.ONGOING);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        task.setDeleted(0);
        try {
            rectificationTaskMapper.insert(task);
            // 记录系统自动创建动作，便于后续审计追溯。
            saveActionLog(task.getId(), ACTION_SYSTEM_CREATE, null, task.getRectificationDesc(), null);
        } catch (DuplicateKeyException ignored) {
            // 通过 inspection_id 唯一索引兜底并发场景，避免重复生成整改任务。
        }
    }

    @Override
    public PageResult<RectificationTaskVO> listMy(Long enterpriseUserId, String status, int page, int size) {
        FoodEnterprise enterprise = requireEnterpriseByUserId(enterpriseUserId);
        LambdaQueryWrapper<RectificationTask> wrapper = new LambdaQueryWrapper<RectificationTask>()
            .eq(RectificationTask::getDeleted, 0)
            .eq(RectificationTask::getEnterpriseId, enterprise.getId());
        RectificationStatus statusValue = normalizeStatus(status);
        if (statusValue != null) {
            wrapper.eq(RectificationTask::getStatus, statusValue);
        }
        wrapper.orderByDesc(RectificationTask::getUpdateTime);
        Page<RectificationTask> pageInfo = rectificationTaskMapper.selectPage(new Page<>(page, size), wrapper);
        List<RectificationTask> records = pageInfo.getRecords();
        List<RectificationTaskVO> vos = toVOs(records);
        return PageResult.of(vos, pageInfo.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RectificationTaskVO submitMy(Long enterpriseUserId, Long rectificationId, RectificationSubmitDTO dto) {
        FoodEnterprise enterprise = requireEnterpriseByUserId(enterpriseUserId);
        RectificationTask task = requireTask(rectificationId);
        if (!Objects.equals(task.getEnterpriseId(), enterprise.getId())) {
            throw new IllegalArgumentException("rectification not found");
        }
        StatusTransitionValidator.validateRectificationTransition(task.getStatus(), RectificationStatus.SUBMITTED);
        String progress = dto.getProgress().trim();
        task.setProgress(progress);
        task.setStatus(RectificationStatus.SUBMITTED);
        task.setFinishTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        rectificationTaskMapper.updateById(task);
        saveActionLog(task.getId(), ACTION_ENTERPRISE_SUBMIT, enterpriseUserId, progress, null);
        return toVOWithNames(task);
    }

    @Override
    public PageResult<RectificationTaskVO> listForAdmin(Long regulatorUserId,
                                                        String status,
                                                        String enterpriseName,
                                                        int page,
                                                        int size) {
        FoodRegulator regulator = requireRegulator(regulatorUserId);
        requireRole(regulator, ROLE_ADMIN);
        List<Long> enterpriseIds = resolveEnterpriseIdsForAdmin(regulator.getId(), enterpriseName);
        if (enterpriseIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        LambdaQueryWrapper<RectificationTask> wrapper = new LambdaQueryWrapper<RectificationTask>()
            .eq(RectificationTask::getDeleted, 0)
            .in(RectificationTask::getEnterpriseId, enterpriseIds);
        RectificationStatus statusValue = normalizeStatus(status);
        if (statusValue != null) {
            wrapper.eq(RectificationTask::getStatus, statusValue);
        }
        wrapper.orderByDesc(RectificationTask::getUpdateTime);
        Page<RectificationTask> pageInfo = rectificationTaskMapper.selectPage(new Page<>(page, size), wrapper);
        List<RectificationTask> records = pageInfo.getRecords();
        List<RectificationTaskVO> vos = toVOs(records);
        return PageResult.of(vos, pageInfo.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RectificationTaskVO review(Long regulatorUserId, Long rectificationId, RectificationReviewDTO dto) {
        FoodRegulator regulator = requireRegulator(regulatorUserId);
        requireRole(regulator, ROLE_ADMIN);
        RectificationTask task = requireTask(rectificationId);
        FoodEnterprise enterprise = requireEnterprise(task.getEnterpriseId());
        if (!coversRegion(regulator.getId(), enterprise.getRegionId())) {
            throw new IllegalArgumentException("rectification not in regulator region");
        }
        RectificationReviewAction action = normalizeReviewAction(dto.getAction());
        String comment = normalizeOptionalText(dto.getComment());
        if (action == RectificationReviewAction.REWORK && !StringUtils.hasText(comment)) {
            throw new IllegalArgumentException("review comment required for rework");
        }
        if (action == RectificationReviewAction.CONFIRM) {
            StatusTransitionValidator.validateRectificationTransition(task.getStatus(), RectificationStatus.CONFIRMED);
            task.setStatus(RectificationStatus.CONFIRMED);
            task.setConfirmedBy(regulator.getId());
            task.setConfirmedTime(LocalDateTime.now());
        } else {
            StatusTransitionValidator.validateRectificationTransition(task.getStatus(), RectificationStatus.REWORK);
            task.setStatus(RectificationStatus.REWORK);
            task.setConfirmedBy(null);
            task.setConfirmedTime(null);
        }
        task.setUpdateTime(LocalDateTime.now());
        rectificationTaskMapper.updateById(task);
        saveActionLog(
            task.getId(),
            action == RectificationReviewAction.CONFIRM ? ACTION_REVIEW_CONFIRM : ACTION_REVIEW_REWORK,
            regulatorUserId,
            comment,
            dto.getAttachmentUrls()
        );
        return toVOWithNames(task);
    }

    private String normalizeRectificationDesc(String value) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_RECTIFICATION_DESC;
        }
        return value.trim();
    }

    private RectificationTask requireTask(Long rectificationId) {
        RectificationTask task = rectificationTaskMapper.selectById(rectificationId);
        if (task == null || isDeleted(task.getDeleted())) {
            throw new IllegalArgumentException("rectification not found");
        }
        return task;
    }

    private FoodEnterprise requireEnterprise(Long enterpriseId) {
        FoodEnterprise enterprise = foodEnterpriseMapper.selectById(enterpriseId);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            throw new IllegalArgumentException("enterprise not found");
        }
        return enterprise;
    }

    private FoodEnterprise requireEnterpriseByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        FoodEnterprise enterprise = foodEnterpriseMapper.selectOne(new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getUserId, userId)
            .eq(FoodEnterprise::getDeleted, 0));
        if (enterprise == null) {
            throw new IllegalArgumentException("enterprise not found");
        }
        return enterprise;
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

    private List<RectificationTaskVO> toVOs(List<RectificationTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        Map<Long, String> enterpriseNames = loadEnterpriseNames(tasks);
        Map<Long, String> regulatorNames = loadRegulatorNames(tasks);
        return tasks.stream().map(task -> toVO(task, enterpriseNames, regulatorNames)).toList();
    }

    private RectificationTaskVO toVOWithNames(RectificationTask task) {
        Map<Long, String> enterpriseNames = loadEnterpriseNames(List.of(task));
        Map<Long, String> regulatorNames = loadRegulatorNames(List.of(task));
        return toVO(task, enterpriseNames, regulatorNames);
    }

    private RectificationTaskVO toVO(RectificationTask task,
                                     Map<Long, String> enterpriseNames,
                                     Map<Long, String> regulatorNames) {
        RectificationTaskVO vo = new RectificationTaskVO();
        vo.setId(task.getId());
        vo.setInspectionId(task.getInspectionId());
        vo.setEnterpriseId(task.getEnterpriseId());
        vo.setEnterpriseName(enterpriseNames.get(task.getEnterpriseId()));
        vo.setRectificationDesc(task.getRectificationDesc());
        vo.setProgress(task.getProgress());
        vo.setStatus(task.getStatus());
        vo.setFinishTime(task.getFinishTime());
        vo.setConfirmedBy(task.getConfirmedBy());
        vo.setConfirmedByName(regulatorNames.get(task.getConfirmedBy()));
        vo.setConfirmedTime(task.getConfirmedTime());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }

    private Map<Long, String> loadEnterpriseNames(List<RectificationTask> tasks) {
        List<Long> enterpriseIds = tasks.stream()
            .map(RectificationTask::getEnterpriseId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (enterpriseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return foodEnterpriseMapper.selectBatchIds(enterpriseIds).stream()
            .filter(enterprise -> !isDeleted(enterprise.getDeleted()))
            .collect(Collectors.toMap(FoodEnterprise::getId, FoodEnterprise::getEnterpriseName, (a, b) -> a));
    }

    private Map<Long, String> loadRegulatorNames(List<RectificationTask> tasks) {
        Set<Long> regulatorIds = tasks.stream()
            .map(RectificationTask::getConfirmedBy)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (regulatorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return foodRegulatorMapper.selectBatchIds(regulatorIds).stream()
            .filter(regulator -> !isDeleted(regulator.getDeleted()))
            .collect(Collectors.toMap(FoodRegulator::getId, FoodRegulator::getName, (a, b) -> a));
    }

    private RectificationStatus normalizeStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return RectificationStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid rectification status");
        }
    }

    private RectificationReviewAction normalizeReviewAction(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("review action required");
        }
        try {
            return RectificationReviewAction.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid review action");
        }
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String serializeAttachmentUrls(List<String> attachmentUrls) {
        if (attachmentUrls == null || attachmentUrls.isEmpty()) {
            return null;
        }
        List<String> normalized = new ArrayList<>();
        for (String attachmentUrl : attachmentUrls) {
            if (StringUtils.hasText(attachmentUrl)) {
                normalized.add(attachmentUrl.trim());
            }
        }
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid attachment urls");
        }
    }

    private void saveActionLog(Long rectificationId,
                               String actionType,
                               Long operatorId,
                               String actionComment,
                               List<String> attachmentUrls) {
        RectificationActionLog log = new RectificationActionLog();
        log.setRectificationId(rectificationId);
        log.setActionType(actionType);
        log.setOperatorId(operatorId);
        log.setActionComment(normalizeOptionalText(actionComment));
        log.setAttachmentUrls(serializeAttachmentUrls(attachmentUrls));
        log.setCreateTime(LocalDateTime.now());
        log.setDeleted(0);
        rectificationActionLogMapper.insert(log);
    }

    private List<Long> resolveEnterpriseIdsForAdmin(Long regulatorId, String enterpriseName) {
        List<Long> regionIds = resolveRegulatorRegionIds(regulatorId);
        if (regionIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<FoodEnterprise> wrapper = new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getDeleted, 0)
            .in(FoodEnterprise::getRegionId, regionIds);
        if (StringUtils.hasText(enterpriseName)) {
            wrapper.like(FoodEnterprise::getEnterpriseName, enterpriseName.trim());
        }
        List<FoodEnterprise> enterprises = foodEnterpriseMapper.selectList(wrapper);
        if (enterprises.isEmpty()) {
            return List.of();
        }
        return enterprises.stream()
            .map(FoodEnterprise::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
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

    private boolean coversRegion(Long regulatorId, Long regionId) {
        if (regulatorId == null || regionId == null) {
            return false;
        }
        return resolveRegulatorRegionIds(regulatorId).contains(regionId);
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}