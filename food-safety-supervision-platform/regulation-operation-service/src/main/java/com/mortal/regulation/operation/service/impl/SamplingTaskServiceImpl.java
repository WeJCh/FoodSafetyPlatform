package com.mortal.regulation.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.operation.client.regulation.vo.InternalEnterpriseDetailVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalProductDetailVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalProductSummaryVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.regulation.operation.common.OperationErrorMessages;
import com.mortal.regulation.operation.dto.WarningEventUpsertDTO;
import com.mortal.regulation.operation.dto.SamplingResultSubmitDTO;
import com.mortal.regulation.operation.dto.SamplingTaskAssignDTO;
import com.mortal.regulation.operation.dto.SamplingTaskCreateDTO;
import com.mortal.regulation.operation.entity.SamplingResult;
import com.mortal.regulation.operation.entity.SamplingTask;
import com.mortal.regulation.operation.mapper.SamplingResultMapper;
import com.mortal.regulation.operation.mapper.SamplingTaskMapper;
import com.mortal.regulation.operation.service.SamplingTaskService;
import com.mortal.regulation.operation.service.WarningEventOutboxService;
import com.mortal.regulation.operation.support.OperationLockSupport;
import com.mortal.regulation.operation.support.OperationMasterDataSupport;
import com.mortal.regulation.operation.support.SamplingPublicCacheService;
import com.mortal.regulation.operation.vo.SamplingResultVO;
import com.mortal.regulation.operation.vo.SamplingTaskVO;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

@Service
public class SamplingTaskServiceImpl implements SamplingTaskService {

    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_ASSIGNED = "ASSIGNED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String PRIORITY_LOW = "LOW";
    private static final String PRIORITY_MEDIUM = "MEDIUM";
    private static final String PRIORITY_HIGH = "HIGH";
    private static final String RESULT_PASS = "PASS";
    private static final String RESULT_FAIL = "FAIL";
    private static final String PUBLIC_STATUS_DRAFT = "DRAFT";
    private static final String PUBLIC_STATUS_PUBLISHED = "PUBLISHED";
    private static final String PUBLIC_STATUS_OFFLINE = "OFFLINE";
    private static final String PRODUCT_STATUS_ACTIVE = "ACTIVE";
    private static final String KEY_REASON_SAMPLING_FAIL = "SAMPLING_FAIL";
    private static final String KEY_SOURCE_ROUTINE = "ROUTINE";
    private static final String WARNING_BIZ_TYPE_SAMPLING = "SAMPLING";
    private static final String WARNING_EVENT_SAMPLING_FAIL = "SAMPLING_FAIL";
    private static final String WARNING_SOURCE_SERVICE = "regulation-operation-service";

    private final SamplingTaskMapper samplingTaskMapper;
    private final SamplingResultMapper samplingResultMapper;
    private final OperationMasterDataSupport masterDataSupport;
    private final OperationLockSupport operationLockSupport;
    private final SamplingPublicCacheService samplingPublicCacheService;
    private final WarningEventOutboxService warningEventOutboxService;

    public SamplingTaskServiceImpl(SamplingTaskMapper samplingTaskMapper,
                                   SamplingResultMapper samplingResultMapper,
                                   OperationMasterDataSupport masterDataSupport,
                                   OperationLockSupport operationLockSupport,
                                   SamplingPublicCacheService samplingPublicCacheService,
                                   WarningEventOutboxService warningEventOutboxService) {
        this.samplingTaskMapper = samplingTaskMapper;
        this.samplingResultMapper = samplingResultMapper;
        this.masterDataSupport = masterDataSupport;
        this.operationLockSupport = operationLockSupport;
        this.samplingPublicCacheService = samplingPublicCacheService;
        this.warningEventOutboxService = warningEventOutboxService;
    }
    /**
     * 创建抽检任务
     * 
     * @param userId 用户ID
     * @param dto 创建DTO
     * @return 抽检任务VO
     */
    @Override
    public SamplingTaskVO createTask(Long userId, SamplingTaskCreateDTO dto) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireAdmin(userId);
        InternalEnterpriseDetailVO enterprise = masterDataSupport.requireApprovedEnterprise(dto.getEnterpriseId());
        InternalProductDetailVO product = requireActiveProduct(dto.getProductId());
        validateCreateDeadline(dto.getDeadline());
        masterDataSupport.requireEnterpriseInScope(regulator.getId(), enterprise.getId());
        ensureProductBelongsToEnterprise(product, enterprise.getId());

        SamplingTask task = new SamplingTask();
        task.setTaskNo(generateTaskNo());
        task.setEnterpriseId(enterprise.getId());
        task.setProductId(product.getId());
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
        samplingTaskMapper.insert(task);
        return toVO(task,
            Map.of(enterprise.getId(), enterprise.getEnterpriseName()),
            Map.of(product.getId(), toSummary(product)),
            loadRegulatorNames(List.of(task)),
            Map.of());
    }

    /**
     * 指派抽检任务
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param dto 指派DTO
     * @return 抽检任务VO
     */
    @Override
    public SamplingTaskVO assignTask(Long userId, Long taskId, SamplingTaskAssignDTO dto) {
        InternalRegulatorIdentityVO operator = masterDataSupport.requireAdmin(userId);
        SamplingTask task = requireTask(taskId);
        ensureTaskNotExpired(task, "assign");
        if (STATUS_COMPLETED.equals(task.getStatus()) || STATUS_CLOSED.equals(task.getStatus())) {
            throw new IllegalArgumentException("task already completed");
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
        samplingTaskMapper.updateById(task);
        return toVO(task,
            loadEnterpriseNames(List.of(task)),
            loadProductSummaries(List.of(task)),
            loadRegulatorNames(List.of(task)),
            loadSamplingResultsByTaskIds(List.of(task)));
    }

    /**
     * 查询区域管理员抽检任务列表
     * 
     * @param userId 用户ID
     * @param enterpriseName 企业名称
     * @param status 任务状态
     * @param page 页码
     * @param size 每页大小
     * @return 抽检任务列表
     */
    @Override
    public PageResult<SamplingTaskVO> listTasksForAdmin(Long userId,
                                                        String enterpriseName,
                                                        String status,
                                                        int page,
                                                        int size) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireAdmin(userId);
        List<Long> scopeRegionIds = masterDataSupport.resolveScopeRegionIds(regulator.getId());
        if (scopeRegionIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        List<Long> enterpriseIds = masterDataSupport.resolveScopedEnterpriseIds(regulator.getId(), enterpriseName);
        if (StringUtils.hasText(enterpriseName) && enterpriseIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        LambdaQueryWrapper<SamplingTask> wrapper = new LambdaQueryWrapper<SamplingTask>()
            .eq(SamplingTask::getDeleted, 0)
            .in(SamplingTask::getRegionId, scopeRegionIds);
        if (StringUtils.hasText(status)) {
            wrapper.eq(SamplingTask::getStatus, normalize(status));
        }
        if (enterpriseIds != null && !enterpriseIds.isEmpty()) {
            wrapper.in(SamplingTask::getEnterpriseId, enterpriseIds);
        }
        wrapper.orderByDesc(SamplingTask::getUpdateTime, SamplingTask::getId);
        Page<SamplingTask> pageInfo = samplingTaskMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(toVOs(pageInfo.getRecords()), pageInfo.getTotal(), page, size);
    }

    /**
     * 查询执法人员抽检任务列表
     * 
     * @param userId 用户ID
     * @param status 任务状态
     * @param page 页码
     * @param size 每页大小
     * @return 抽检任务列表
     */
    @Override
    public PageResult<SamplingTaskVO> listTasksForEnforcer(Long userId,
                                                           String status,
                                                           int page,
                                                           int size) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireEnforcer(userId);
        LambdaQueryWrapper<SamplingTask> wrapper = new LambdaQueryWrapper<SamplingTask>()
            .eq(SamplingTask::getDeleted, 0)
            .eq(SamplingTask::getAssignedTo, regulator.getId());
        if (StringUtils.hasText(status)) {
            wrapper.eq(SamplingTask::getStatus, normalize(status));
        }
        wrapper.orderByDesc(SamplingTask::getUpdateTime, SamplingTask::getId);
        Page<SamplingTask> pageInfo = samplingTaskMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(toVOs(pageInfo.getRecords()), pageInfo.getTotal(), page, size);
    }

    /**
     * 提交抽检结果
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param dto 提交DTO
     * @return 抽检结果VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SamplingResultVO submitResult(Long userId, Long taskId, SamplingResultSubmitDTO dto) {
        return operationLockSupport.executeWithLock("sampling-submit", taskId, () -> doSubmitResult(userId, taskId, dto));
    }

    private SamplingResultVO doSubmitResult(Long userId, Long taskId, SamplingResultSubmitDTO dto) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireEnforcer(userId);
        SamplingTask task = requireTask(taskId);
        if (!Objects.equals(task.getAssignedTo(), regulator.getId())) {
            throw new IllegalArgumentException("task not assigned to you");
        }
        if (!STATUS_ASSIGNED.equals(task.getStatus())) {
            throw new IllegalArgumentException("task not ready for sampling");
        }
        ensureNoSamplingResult(task.getId());
        SamplingResult result = new SamplingResult();
        result.setTaskId(task.getId());
        result.setEnterpriseId(task.getEnterpriseId());
        result.setProductId(task.getProductId());
        result.setSampledBy(regulator.getId());
        result.setSampledTime(dto.getSampledTime());
        result.setResult(normalizeResult(dto.getResult()));
        result.setConclusion(StringUtils.hasText(dto.getConclusion()) ? dto.getConclusion().trim() : null);
        result.setDisposalSuggestion(StringUtils.hasText(dto.getDisposalSuggestion())
            ? dto.getDisposalSuggestion().trim()
            : null);
        result.setPublicStatus(PUBLIC_STATUS_DRAFT);
        result.setCreateTime(LocalDateTime.now());
        result.setUpdateTime(LocalDateTime.now());
        result.setDeleted(0);
        samplingResultMapper.insert(result);
        samplingPublicCacheService.evict(result.getId());

        if (RESULT_FAIL.equals(result.getResult())) {
            markSamplingFailAsKey(task, result, regulator.getId());
        }

        task.setStatus(STATUS_COMPLETED);
        task.setCompletedTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        samplingTaskMapper.updateById(task);

        return toResultVO(result, task,
            loadEnterpriseNames(List.of(task)),
            loadProductSummaries(List.of(task)),
            masterDataSupport.loadRegulatorNames(List.of(regulator.getId())));
    }

    @Override
    public SamplingResultVO publishResult(Long userId, Long resultId) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireAdmin(userId);
        SamplingResult result = requireResult(resultId);
        SamplingTask task = requireTask(result.getTaskId());
        masterDataSupport.requireRegionInScope(regulator.getId(), task.getRegionId());
        if (!PUBLIC_STATUS_PUBLISHED.equalsIgnoreCase(result.getPublicStatus())) {
            result.setPublicStatus(PUBLIC_STATUS_PUBLISHED);
            result.setPublishedTime(LocalDateTime.now());
            result.setUpdateTime(LocalDateTime.now());
            samplingResultMapper.updateById(result);
            samplingPublicCacheService.evict(resultId);
        }
        return toResultVO(result, task,
            loadEnterpriseNames(List.of(task)),
            loadProductSummaries(List.of(task)),
            masterDataSupport.loadRegulatorNames(List.of(result.getSampledBy())));
    }

    @Override
    public SamplingResultVO offlineResult(Long userId, Long resultId) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireAdmin(userId);
        SamplingResult result = requireResult(resultId);
        SamplingTask task = requireTask(result.getTaskId());
        masterDataSupport.requireRegionInScope(regulator.getId(), task.getRegionId());
        if (!PUBLIC_STATUS_PUBLISHED.equalsIgnoreCase(result.getPublicStatus())) {
            throw new IllegalArgumentException("sampling result not published");
        }
        result.setPublicStatus(PUBLIC_STATUS_OFFLINE);
        result.setUpdateTime(LocalDateTime.now());
        samplingResultMapper.updateById(result);
        samplingPublicCacheService.evict(resultId);
        return toResultVO(result, task,
            loadEnterpriseNames(List.of(task)),
            loadProductSummaries(List.of(task)),
            masterDataSupport.loadRegulatorNames(List.of(result.getSampledBy())));
    }

    @Override
    public PageResult<SamplingResultVO> listPublicResults(String enterpriseName,
                                                          String productName,
                                                          String result,
                                                          int page,
                                                          int size) {
        String queryHash = buildPublicSamplingQueryHash(enterpriseName, productName, result, page, size);
        return samplingPublicCacheService.getList(
            queryHash,
            () -> loadPublicResults(enterpriseName, productName, result, page, size)
        );
    }

    private PageResult<SamplingResultVO> loadPublicResults(String enterpriseName,
                                                           String productName,
                                                           String result,
                                                           int page,
                                                           int size) {
        LambdaQueryWrapper<SamplingResult> wrapper = new LambdaQueryWrapper<SamplingResult>()
            .eq(SamplingResult::getDeleted, 0)
            .eq(SamplingResult::getPublicStatus, PUBLIC_STATUS_PUBLISHED);
        if (StringUtils.hasText(result)) {
            wrapper.eq(SamplingResult::getResult, normalizeResult(result));
        }
        if (StringUtils.hasText(enterpriseName)) {
            List<Long> enterpriseIds = masterDataSupport.queryEnterpriseIdsByName(enterpriseName);
            if (enterpriseIds == null || enterpriseIds.isEmpty()) {
                return PageResult.of(List.of(), 0, page, size);
            }
            wrapper.in(SamplingResult::getEnterpriseId, enterpriseIds);
        }
        if (StringUtils.hasText(productName)) {
            List<Long> productIds = masterDataSupport.queryProductIdsByName(productName);
            if (productIds.isEmpty()) {
                return PageResult.of(List.of(), 0, page, size);
            }
            wrapper.in(SamplingResult::getProductId, productIds);
        }
        wrapper.orderByDesc(SamplingResult::getPublishedTime, SamplingResult::getId);
        Page<SamplingResult> pageInfo = samplingResultMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(toResultVOs(pageInfo.getRecords()), pageInfo.getTotal(), page, size);
    }

    @Override
    public SamplingResultVO getPublicResultDetail(Long resultId) {
        return samplingPublicCacheService.getDetail(resultId, () -> loadPublicResultDetail(resultId));
    }

    private SamplingResultVO loadPublicResultDetail(Long resultId) {
        SamplingResult result = samplingResultMapper.selectOne(new LambdaQueryWrapper<SamplingResult>()
            .eq(SamplingResult::getId, resultId)
            .eq(SamplingResult::getDeleted, 0)
            .eq(SamplingResult::getPublicStatus, PUBLIC_STATUS_PUBLISHED)
            .last("limit 1"));
        if (result == null) {
            return null;
        }
        SamplingTask task = requireTask(result.getTaskId());
        return toResultVO(result, task,
            loadEnterpriseNames(List.of(task)),
            loadProductSummaries(List.of(task)),
            masterDataSupport.loadRegulatorNames(List.of(result.getSampledBy())));
    }

    private String buildPublicSamplingQueryHash(String enterpriseName,
                                                String productName,
                                                String result,
                                                int page,
                                                int size) {
        String raw = String.join("|",
            StringUtils.hasText(enterpriseName) ? enterpriseName.trim() : "",
            StringUtils.hasText(productName) ? productName.trim() : "",
            StringUtils.hasText(result) ? normalizeResult(result) : "",
            String.valueOf(Math.max(1, page)),
            String.valueOf(Math.max(1, size))
        );
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 关闭抽检任务
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return 抽检任务VO
     */
    @Override
    public SamplingTaskVO closeTask(Long userId, Long taskId) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireAdmin(userId);
        SamplingTask task = requireTask(taskId);
        masterDataSupport.requireRegionInScope(regulator.getId(), task.getRegionId());
        if (!STATUS_COMPLETED.equals(task.getStatus())) {
            throw new IllegalArgumentException("task not completed");
        }
        task.setStatus(STATUS_CLOSED);
        task.setUpdateTime(LocalDateTime.now());
        samplingTaskMapper.updateById(task);
        return toVO(task,
            loadEnterpriseNames(List.of(task)),
            loadProductSummaries(List.of(task)),
            loadRegulatorNames(List.of(task)),
            loadSamplingResultsByTaskIds(List.of(task)));
    }

    /**
     * 获取抽检任务
     * 
     * @param taskId 任务ID
     * @return 抽检任务
     */
    private SamplingTask requireTask(Long taskId) {
        SamplingTask task = samplingTaskMapper.selectById(taskId);
        if (task == null || isDeleted(task.getDeleted())) {
            throw new IllegalArgumentException("sampling task not found");
        }
        return task;
    }

    private SamplingResult requireResult(Long resultId) {
        SamplingResult result = samplingResultMapper.selectById(resultId);
        if (result == null || isDeleted(result.getDeleted())) {
            throw new IllegalArgumentException("sampling result not found");
        }
        return result;
    }

    private void ensureNoSamplingResult(Long taskId) {
        SamplingResult existing = samplingResultMapper.selectOne(new LambdaQueryWrapper<SamplingResult>()
            .eq(SamplingResult::getTaskId, taskId)
            .eq(SamplingResult::getDeleted, 0)
            .last("limit 1"));
        if (existing != null) {
            throw new IllegalArgumentException("sampling result already submitted");
        }
    }

    private InternalProductDetailVO requireActiveProduct(Long productId) {
        InternalProductDetailVO product = masterDataSupport.requireProduct(productId);
        if (!PRODUCT_STATUS_ACTIVE.equalsIgnoreCase(product.getStatus())) {
            throw new IllegalArgumentException("product not active");
        }
        return product;
    }

    private void ensureProductBelongsToEnterprise(InternalProductDetailVO product, Long enterpriseId) {
        if (product == null || !Objects.equals(product.getEnterpriseId(), enterpriseId)) {
            throw new IllegalArgumentException("product not in enterprise");
        }
    }

    private List<SamplingTaskVO> toVOs(List<SamplingTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        return tasks.stream()
            .filter(Objects::nonNull)
            .map(this::copyTask)
            .collect(Collectors.collectingAndThen(Collectors.toList(), copies -> {
                Map<Long, String> enterpriseNames = loadEnterpriseNames(copies);
                Map<Long, InternalProductSummaryVO> productMap = loadProductSummaries(copies);
                Map<Long, String> regulatorNames = loadRegulatorNames(copies);
                Map<Long, SamplingResult> resultMap = loadSamplingResultsByTaskIds(copies);
                return copies.stream()
                    .map(task -> toVO(task, enterpriseNames, productMap, regulatorNames, resultMap))
                    .toList();
            }));
    }

    private SamplingTaskVO toVO(SamplingTask task,
                                Map<Long, String> enterpriseNames,
                                Map<Long, InternalProductSummaryVO> productMap,
                                Map<Long, String> regulatorNames,
                                Map<Long, SamplingResult> resultMap) {
        SamplingTaskVO vo = new SamplingTaskVO();
        vo.setId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setEnterpriseId(task.getEnterpriseId());
        vo.setEnterpriseName(enterpriseNames.getOrDefault(task.getEnterpriseId(), "-"));
        vo.setProductId(task.getProductId());
        InternalProductSummaryVO product = productMap.get(task.getProductId());
        vo.setProductName(product == null ? "-" : product.getProductName());
        vo.setProductCategory(product == null ? null : product.getCategory());
        vo.setProductSpecification(product == null ? null : product.getSpecification());
        vo.setRegionId(task.getRegionId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setTaskDesc(task.getTaskDesc());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setCreatedBy(task.getCreatedBy());
        vo.setCreatedByName(lookupName(regulatorNames, task.getCreatedBy()));
        vo.setAssignedTo(task.getAssignedTo());
        vo.setAssignedToName(lookupName(regulatorNames, task.getAssignedTo()));
        vo.setAssignedBy(task.getAssignedBy());
        vo.setAssignedByName(lookupName(regulatorNames, task.getAssignedBy()));
        vo.setAssignedTime(task.getAssignedTime());
        vo.setCompletedTime(task.getCompletedTime());
        SamplingResult samplingResult = resultMap == null ? null : resultMap.get(task.getId());
        if (samplingResult != null) {
            vo.setSamplingResultId(samplingResult.getId());
            vo.setSamplingResult(samplingResult.getResult());
            vo.setSamplingConclusion(samplingResult.getConclusion());
            vo.setSamplingPublicStatus(samplingResult.getPublicStatus());
            vo.setSamplingPublishedTime(samplingResult.getPublishedTime());
            vo.setSampledTime(samplingResult.getSampledTime());
        }
        vo.setDeadline(task.getDeadline());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }

    private SamplingResultVO toResultVO(SamplingResult result,
                                        SamplingTask task,
                                        Map<Long, String> enterpriseNames,
                                        Map<Long, InternalProductSummaryVO> productMap,
                                        Map<Long, String> regulatorNames) {
        SamplingResultVO vo = new SamplingResultVO();
        vo.setId(result.getId());
        vo.setTaskId(result.getTaskId());
        vo.setTaskNo(task == null ? "-" : task.getTaskNo());
        vo.setEnterpriseId(result.getEnterpriseId());
        vo.setEnterpriseName(enterpriseNames.getOrDefault(result.getEnterpriseId(), "-"));
        vo.setProductId(result.getProductId());
        InternalProductSummaryVO product = productMap.get(result.getProductId());
        vo.setProductName(product == null ? "-" : product.getProductName());
        vo.setProductCategory(product == null ? null : product.getCategory());
        vo.setProductSpecification(product == null ? null : product.getSpecification());
        vo.setSampledBy(result.getSampledBy());
        vo.setSampledByName(lookupName(regulatorNames, result.getSampledBy()));
        vo.setSampledTime(result.getSampledTime());
        vo.setResult(result.getResult());
        vo.setConclusion(result.getConclusion());
        vo.setDisposalSuggestion(result.getDisposalSuggestion());
        vo.setPublicStatus(result.getPublicStatus());
        vo.setPublishedTime(result.getPublishedTime());
        vo.setCreateTime(result.getCreateTime());
        vo.setUpdateTime(result.getUpdateTime());
        return vo;
    }

    private List<SamplingResultVO> toResultVOs(List<SamplingResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        Set<Long> taskIds = results.stream()
            .map(SamplingResult::getTaskId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, SamplingTask> taskMap = samplingTaskMapper.selectBatchIds(taskIds).stream()
            .filter(Objects::nonNull)
            .filter(task -> !isDeleted(task.getDeleted()))
            .collect(Collectors.toMap(SamplingTask::getId, this::copyTask, (a, b) -> a));
        List<SamplingTask> tasks = taskMap.values().stream().toList();
        Map<Long, String> enterpriseNames = loadEnterpriseNames(tasks);
        Map<Long, InternalProductSummaryVO> productMap = loadProductSummaries(tasks);
        Set<Long> regulatorIds = results.stream()
            .map(SamplingResult::getSampledBy)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> regulatorNames = masterDataSupport.loadRegulatorNames(regulatorIds);
        return results.stream()
            .filter(Objects::nonNull)
            .map(result -> toResultVO(result, taskMap.get(result.getTaskId()), enterpriseNames, productMap, regulatorNames))
            .toList();
    }

    private Map<Long, String> loadEnterpriseNames(List<SamplingTask> tasks) {
        Set<Long> enterpriseIds = tasks.stream()
            .map(SamplingTask::getEnterpriseId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return masterDataSupport.loadEnterpriseNames(enterpriseIds);
    }

    private Map<Long, InternalProductSummaryVO> loadProductSummaries(List<SamplingTask> tasks) {
        Set<Long> productIds = tasks.stream()
            .map(SamplingTask::getProductId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return masterDataSupport.loadProductSummaries(productIds);
    }

    private Map<Long, SamplingResult> loadSamplingResultsByTaskIds(List<SamplingTask> tasks) {
        Set<Long> taskIds = tasks.stream()
            .map(SamplingTask::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        return samplingResultMapper.selectList(new LambdaQueryWrapper<SamplingResult>()
                .eq(SamplingResult::getDeleted, 0)
                .in(SamplingResult::getTaskId, taskIds))
            .stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(SamplingResult::getTaskId, item -> item, (a, b) -> a));
    }

    private Map<Long, String> loadRegulatorNames(List<SamplingTask> tasks) {
        Set<Long> regulatorIds = new LinkedHashSet<>();
        for (SamplingTask task : tasks) {
            if (task == null) {
                continue;
            }
            if (task.getCreatedBy() != null) {
                regulatorIds.add(task.getCreatedBy());
            }
            if (task.getAssignedTo() != null) {
                regulatorIds.add(task.getAssignedTo());
            }
            if (task.getAssignedBy() != null) {
                regulatorIds.add(task.getAssignedBy());
            }
        }
        return masterDataSupport.loadRegulatorNames(regulatorIds);
    }

    private SamplingTask copyTask(SamplingTask task) {
        SamplingTask copy = new SamplingTask();
        copy.setId(task.getId());
        copy.setTaskNo(task.getTaskNo());
        copy.setEnterpriseId(task.getEnterpriseId());
        copy.setProductId(task.getProductId());
        copy.setRegionId(task.getRegionId());
        copy.setTaskTitle(task.getTaskTitle());
        copy.setTaskDesc(task.getTaskDesc());
        copy.setPriority(task.getPriority());
        copy.setStatus(task.getStatus());
        copy.setCreatedBy(task.getCreatedBy());
        copy.setAssignedTo(task.getAssignedTo());
        copy.setAssignedBy(task.getAssignedBy());
        copy.setAssignedTime(task.getAssignedTime());
        copy.setCompletedTime(task.getCompletedTime());
        copy.setDeadline(task.getDeadline());
        copy.setCreateTime(task.getCreateTime());
        copy.setUpdateTime(task.getUpdateTime());
        copy.setDeleted(task.getDeleted());
        return copy;
    }

    private InternalProductSummaryVO toSummary(InternalProductDetailVO detail) {
        InternalProductSummaryVO summary = new InternalProductSummaryVO();
        summary.setId(detail.getId());
        summary.setEnterpriseId(detail.getEnterpriseId());
        summary.setProductName(detail.getProductName());
        summary.setCategory(detail.getCategory());
        summary.setSpecification(detail.getSpecification());
        summary.setStatus(detail.getStatus());
        return summary;
    }
    /**
     * 标记抽检失败为关键企业
     * 
     * @param task 抽检任务
     * @param result 抽检结果
     * @param operatorId 操作员ID
     */
    private void markSamplingFailAsKey(SamplingTask task, SamplingResult result, Long operatorId) {
        if (task == null || result == null || !RESULT_FAIL.equalsIgnoreCase(result.getResult())) {
            return;
        }
        String productName = resolveProductName(task.getProductId());
        String reasonDetail = "企业产品“" + productName + "”抽检结果不合格，已自动纳入重点监管";
        masterDataSupport.markEnterpriseAsKey(
            task.getEnterpriseId(),
            KEY_REASON_SAMPLING_FAIL,
            reasonDetail,
            KEY_SOURCE_ROUTINE,
            result.getId(),
            operatorId
        );
        ensureSamplingWarning(task, result, reasonDetail, productName);
    }

    /**
     * 确保抽检警告
     * 
     * @param task 抽检任务
     * @param result 抽检结果
     * @param reasonDetail 原因详情
     * @param productName 产品名称
     */
    private void ensureSamplingWarning(SamplingTask task,
                                       SamplingResult result,
                                       String reasonDetail,
                                       String productName) {
        LocalDateTime now = LocalDateTime.now();
        String eventKey = buildSamplingWarningKey(result.getId());
        WarningEventUpsertDTO dto = new WarningEventUpsertDTO();
        dto.setEventType(WARNING_EVENT_SAMPLING_FAIL);
        dto.setBizType(WARNING_BIZ_TYPE_SAMPLING);
        dto.setBizId(result.getId());
        dto.setRegionId(task.getRegionId());
        dto.setOwnerRegulatorId(result.getSampledBy());
        dto.setDedupKey(eventKey);
        dto.setLevel("L2");
        dto.setTitle("抽检发现不合格产品");
        dto.setContent(reasonDetail);
        dto.setSourceService(WARNING_SOURCE_SERVICE);
        dto.setOccurTime(now);
        dto.setPayload(Map.of(
            "enterpriseId", task.getEnterpriseId(),
            "taskId", task.getId(),
            "samplingResultId", result.getId(),
            "productId", task.getProductId(),
            "productName", productName,
            "sampledTime", result.getSampledTime()
        ));
        warningEventOutboxService.ensurePendingEvent(eventKey, dto, now);
        warningEventOutboxService.dispatchByEventKey(eventKey);
    }

    private String buildSamplingWarningKey(Long resultId) {
        return WARNING_BIZ_TYPE_SAMPLING + ":" + resultId + ":" + WARNING_EVENT_SAMPLING_FAIL;
    }

    private String resolveProductName(Long productId) {
        if (productId == null) {
            return "未知产品";
        }
        Map<Long, InternalProductSummaryVO> productMap = masterDataSupport.loadProductSummaries(List.of(productId));
        InternalProductSummaryVO product = productMap.get(productId);
        return product == null || !StringUtils.hasText(product.getProductName())
            ? "未知产品"
            : product.getProductName();
    }

    private void validateCreateDeadline(LocalDateTime deadline) {
        if (deadline == null) {
            throw new IllegalArgumentException("deadline required");
        }
        if (!deadline.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("deadline must be future");
        }
    }

    private void ensureTaskNotExpired(SamplingTask task, String action) {
        if (task == null) {
            throw new IllegalArgumentException(OperationErrorMessages.TASK_NOT_FOUND);
        }
        LocalDateTime deadline = task.getDeadline();
        if (deadline != null && !deadline.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("task deadline exceeded, cannot " + action);
        }
    }

    private String normalizePriority(String value) {
        String normalized = normalize(value);
        if (PRIORITY_LOW.equals(normalized) || PRIORITY_MEDIUM.equals(normalized) || PRIORITY_HIGH.equals(normalized)) {
            return normalized;
        }
        return PRIORITY_MEDIUM;
    }

    private String normalizeResult(String value) {
        String normalized = normalize(value);
        if (RESULT_PASS.equals(normalized) || RESULT_FAIL.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("invalid sampling result");
    }

    private String generateTaskNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 9000) + 1000;
        return "SMP" + time + random;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private String lookupName(Map<Long, String> source, Long id) {
        if (id == null || source == null || source.isEmpty()) {
            return "-";
        }
        return source.getOrDefault(id, "-");
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}
