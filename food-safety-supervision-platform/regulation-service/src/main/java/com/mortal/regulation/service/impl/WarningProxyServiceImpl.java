package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.regulation.client.WarningServiceClient;
import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.dto.WarningActionCommentDTO;
import com.mortal.regulation.dto.WarningAssignDTO;
import com.mortal.regulation.dto.WarningProcessActionDTO;
import com.mortal.regulation.dto.WarningRecordQueryDTO;
import com.mortal.regulation.entity.AddrRegion;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.entity.FoodRegulator;
import com.mortal.regulation.entity.FoodRegulatorRegion;
import com.mortal.regulation.mapper.AddrRegionMapper;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.mapper.FoodRegulatorMapper;
import com.mortal.regulation.mapper.FoodRegulatorRegionMapper;
import com.mortal.regulation.service.WarningProxyService;
import com.mortal.regulation.vo.WarningProcessLogVO;
import com.mortal.regulation.vo.WarningRecordDetailVO;
import com.mortal.regulation.vo.WarningRecordVO;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 监管服务侧预警代理实现。
 *
 * <p>说明：权限过滤下沉到 warning-service，这里仅负责构造作用域参数。</p>
 */
@Service
public class WarningProxyServiceImpl implements WarningProxyService {

    private static final String ROLE_ADMIN = "REGULATOR_ADMIN";
    private static final String ROLE_ENFORCER = "REGULATOR_ENFORCER";
    private static final Set<String> ADMIN_ALLOWED_ACTIONS = Set.of("PROCESS", "RESOLVE");
    private static final Set<String> ENFORCER_ALLOWED_ACTIONS = Set.of("PROCESS", "RESOLVE");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final WarningServiceClient warningServiceClient;
    private final FoodEnterpriseMapper foodEnterpriseMapper;
    private final FoodRegulatorMapper foodRegulatorMapper;
    private final FoodRegulatorRegionMapper foodRegulatorRegionMapper;
    private final AddrRegionMapper addrRegionMapper;
    private final ObjectMapper objectMapper;

    public WarningProxyServiceImpl(WarningServiceClient warningServiceClient,
                                   FoodEnterpriseMapper foodEnterpriseMapper,
                                   FoodRegulatorMapper foodRegulatorMapper,
                                   FoodRegulatorRegionMapper foodRegulatorRegionMapper,
                                   AddrRegionMapper addrRegionMapper,
                                   ObjectMapper objectMapper) {
        this.warningServiceClient = warningServiceClient;
        this.foodEnterpriseMapper = foodEnterpriseMapper;
        this.foodRegulatorMapper = foodRegulatorMapper;
        this.foodRegulatorRegionMapper = foodRegulatorRegionMapper;
        this.addrRegionMapper = addrRegionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResult<WarningRecordVO> listAdminWarnings(Long userId, WarningRecordQueryDTO queryDTO) {
        FoodRegulator admin = requireAdmin(userId);
        Set<Long> regionIds = resolveRegulatorRegionIds(admin.getId());
        int page = normalizePage(queryDTO == null ? null : queryDTO.getPage());
        int size = normalizeSize(queryDTO == null ? null : queryDTO.getSize());
        if (regionIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }

        WarningRecordQueryDTO remoteQuery = buildRemoteQuery(queryDTO);
        remoteQuery.setRegionIds(joinRegionIds(regionIds));
        ApiResponse<PageResult<WarningRecordVO>> response = warningServiceClient.pageRecords(remoteQuery);
        return enrichPage(requireSuccess(response, "load warning records failed"));
    }

    @Override
    public WarningRecordDetailVO getAdminWarningDetail(Long userId, Long warningId) {
        FoodRegulator admin = requireAdmin(userId);
        String regionIds = joinRegionIds(resolveRegulatorRegionIds(admin.getId()));
        if (!StringUtils.hasText(regionIds)) {
            throw new IllegalArgumentException("warning not found");
        }
        ApiResponse<WarningRecordDetailVO> response = warningServiceClient.detail(warningId, null, regionIds);
        return enrichDetail(requireSuccess(response, "load warning detail failed"));
    }

    @Override
    public WarningRecordDetailVO processAdminWarning(Long userId,
                                                     String username,
                                                     Long warningId,
                                                     WarningProcessActionDTO actionDTO) {
        FoodRegulator admin = requireAdmin(userId);
        String actionType = normalizeActionType(actionDTO);
        if (!ADMIN_ALLOWED_ACTIONS.contains(actionType)) {
            throw new IllegalArgumentException("admin action not allowed");
        }
        String regionIds = joinRegionIds(resolveRegulatorRegionIds(admin.getId()));
        if (!StringUtils.hasText(regionIds)) {
            throw new IllegalArgumentException("warning not found");
        }

        ApiResponse<WarningRecordDetailVO> response = executeAction(
            warningId,
            actionType,
            actionDTO.getActionComment(),
            null,
            regionIds,
            String.valueOf(admin.getId()),
            StringUtils.hasText(username) ? username.trim() : "unknown"
        );
        return enrichDetail(requireSuccess(response, "process warning failed"));
    }

    @Override
    public WarningRecordDetailVO assignAdminWarning(Long userId,
                                                    String username,
                                                    Long warningId,
                                                    WarningAssignDTO assignDTO) {
        FoodRegulator admin = requireAdmin(userId);
        if (assignDTO == null || assignDTO.getAssignedTo() == null || assignDTO.getAssignedTo() <= 0) {
            throw new IllegalArgumentException("assignedTo required");
        }
        String regionIds = joinRegionIds(resolveRegulatorRegionIds(admin.getId()));
        if (!StringUtils.hasText(regionIds)) {
            throw new IllegalArgumentException("warning not found");
        }

        WarningAssignDTO remoteAssign = new WarningAssignDTO();
        remoteAssign.setAssignedTo(assignDTO.getAssignedTo());
        remoteAssign.setActionComment(assignDTO.getActionComment());
        ApiResponse<WarningRecordDetailVO> response = warningServiceClient.assign(
            warningId,
            remoteAssign,
            null,
            regionIds,
            String.valueOf(admin.getId()),
            StringUtils.hasText(username) ? username.trim() : "unknown"
        );
        return enrichDetail(requireSuccess(response, "assign warning failed"));
    }

    @Override
    public List<WarningProcessLogVO> listRecentAdminWarningLogs(Long userId, Integer limit) {
        FoodRegulator admin = requireAdmin(userId);
        String regionIds = joinRegionIds(resolveRegulatorRegionIds(admin.getId()));
        if (!StringUtils.hasText(regionIds)) {
            return List.of();
        }
        ApiResponse<List<WarningProcessLogVO>> response = warningServiceClient.recentLogs(
            null,
            regionIds,
            normalizeRecentLimit(limit)
        );
        return enrichProcessLogs(requireSuccess(response, "load recent warning logs failed"));
    }

    @Override
    public PageResult<WarningRecordVO> listMyWarnings(Long userId, WarningRecordQueryDTO queryDTO) {
        FoodRegulator enforcer = requireEnforcer(userId);
        WarningRecordQueryDTO remoteQuery = buildRemoteQuery(queryDTO);
        remoteQuery.setOwnerRegulatorId(enforcer.getId());
        ApiResponse<PageResult<WarningRecordVO>> response = warningServiceClient.pageRecords(remoteQuery);
        return enrichPage(requireSuccess(response, "load warning records failed"));
    }

    @Override
    public WarningRecordDetailVO getMyWarningDetail(Long userId, Long warningId) {
        FoodRegulator enforcer = requireEnforcer(userId);
        ApiResponse<WarningRecordDetailVO> response = warningServiceClient.detail(
            warningId,
            String.valueOf(enforcer.getId()),
            null
        );
        return enrichDetail(requireSuccess(response, "load warning detail failed"));
    }

    @Override
    public WarningRecordDetailVO processMyWarning(Long userId,
                                                  String username,
                                                  Long warningId,
                                                  WarningProcessActionDTO actionDTO) {
        FoodRegulator enforcer = requireEnforcer(userId);
        String actionType = normalizeActionType(actionDTO);
        if (!ENFORCER_ALLOWED_ACTIONS.contains(actionType)) {
            throw new IllegalArgumentException("enforcer action not allowed");
        }

        ApiResponse<WarningRecordDetailVO> response = executeAction(
            warningId,
            actionType,
            actionDTO.getActionComment(),
            String.valueOf(enforcer.getId()),
            null,
            String.valueOf(enforcer.getId()),
            StringUtils.hasText(username) ? username.trim() : "unknown"
        );
        return enrichDetail(requireSuccess(response, "process warning failed"));
    }

    @Override
    public List<WarningProcessLogVO> listRecentMyWarningLogs(Long userId, Integer limit) {
        FoodRegulator enforcer = requireEnforcer(userId);
        ApiResponse<List<WarningProcessLogVO>> response = warningServiceClient.recentLogs(
            String.valueOf(enforcer.getId()),
            null,
            normalizeRecentLimit(limit)
        );
        return enrichProcessLogs(requireSuccess(response, "load recent warning logs failed"));
    }

    private PageResult<WarningRecordVO> enrichPage(PageResult<WarningRecordVO> pageResult) {
        if (pageResult == null || pageResult.getRecords() == null || pageResult.getRecords().isEmpty()) {
            return pageResult;
        }
        enrichWarnings(pageResult.getRecords());
        return pageResult;
    }

    private WarningRecordDetailVO enrichDetail(WarningRecordDetailVO detail) {
        if (detail == null) {
            return null;
        }
        enrichWarnings(List.of(detail));
        if (detail.getProcessLogs() != null && !detail.getProcessLogs().isEmpty()) {
            enrichProcessLogs(detail.getProcessLogs());
        }
        return detail;
    }

    private List<WarningProcessLogVO> enrichProcessLogs(List<WarningProcessLogVO> logs) {
        if (logs == null || logs.isEmpty()) {
            return logs;
        }
        Map<Long, String> regulatorNames = loadRegulatorNameMapFromLogs(logs);
        Map<Long, String> regionNameMap = loadRegionNameMapFromLogs(logs);
        Map<Long, String> regionPathMap = loadRegionPathMapFromLogs(logs);
        Map<Long, String> enterpriseNameMap = loadEnterpriseNameMapFromLogs(logs);
        for (WarningProcessLogVO log : logs) {
            if (log == null) {
                continue;
            }
            log.setOperatorName(resolveOperatorName(log, regulatorNames));
            log.setOwnerName(safeGet(regulatorNames, log.getOwnerRegulatorId()));
            log.setAssignedToName(safeGet(regulatorNames, log.getAssignedTo()));
            log.setResolvedByName(safeGet(regulatorNames, log.getResolvedBy()));
            log.setRegionName(safeGet(regionNameMap, log.getRegionId()));
            log.setRegionPathText(safeGet(regionPathMap, log.getRegionId()));
            log.setBizName(resolveLogBizName(log, enterpriseNameMap));
        }
        return logs;
    }

    private void enrichWarnings(List<? extends WarningRecordVO> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return;
        }
        Map<Long, String> regulatorNames = loadRegulatorNameMap(warnings);
        Map<Long, String> regionNameMap = loadRegionNameMap(warnings);
        Map<Long, String> regionPathMap = loadRegionPathMap(warnings);
        Map<Long, String> enterpriseNameMap = loadEnterpriseNameMap(warnings);
        for (WarningRecordVO warning : warnings) {
            if (warning == null) {
                continue;
            }
            String ownerName = safeGet(regulatorNames, warning.getOwnerRegulatorId());
            String assignedToName = safeGet(regulatorNames, warning.getAssignedTo());
            warning.setOwnerName(ownerName);
            warning.setAssignedToName(StringUtils.hasText(assignedToName) ? assignedToName : ownerName);
            warning.setResolvedByName(safeGet(regulatorNames, warning.getResolvedBy()));
            warning.setRegionName(safeGet(regionNameMap, warning.getRegionId()));
            warning.setRegionPathText(safeGet(regionPathMap, warning.getRegionId()));
            warning.setBizName(resolveBizName(warning, enterpriseNameMap));
        }
    }

    private Map<Long, String> loadRegulatorNameMap(List<? extends WarningRecordVO> warnings) {
        Set<Long> regulatorIds = new LinkedHashSet<>();
        for (WarningRecordVO warning : warnings) {
            if (warning == null) {
                continue;
            }
            addPositiveId(regulatorIds, warning.getOwnerRegulatorId());
            addPositiveId(regulatorIds, warning.getAssignedTo());
            addPositiveId(regulatorIds, warning.getResolvedBy());
        }
        if (regulatorIds.isEmpty()) {
            return Map.of();
        }
        return foodRegulatorMapper.selectBatchIds(regulatorIds).stream()
            .filter(Objects::nonNull)
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(FoodRegulator::getId, item -> item.getName() == null ? "" : item.getName(), (a, b) -> a));
    }

    private Map<Long, String> loadRegulatorNameMapFromLogs(List<WarningProcessLogVO> logs) {
        Set<Long> regulatorIds = new LinkedHashSet<>();
        for (WarningProcessLogVO log : logs) {
            if (log == null) {
                continue;
            }
            addPositiveId(regulatorIds, log.getOperatorId());
            addPositiveId(regulatorIds, log.getOwnerRegulatorId());
            addPositiveId(regulatorIds, log.getAssignedTo());
            addPositiveId(regulatorIds, log.getResolvedBy());
        }
        if (regulatorIds.isEmpty()) {
            return Map.of();
        }
        return foodRegulatorMapper.selectBatchIds(regulatorIds).stream()
            .filter(Objects::nonNull)
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(FoodRegulator::getId, item -> item.getName() == null ? "" : item.getName(), (a, b) -> a));
    }

    private Map<Long, String> loadRegionNameMap(List<? extends WarningRecordVO> warnings) {
        Set<Long> regionIds = collectWarningRegionIds(warnings);
        if (regionIds.isEmpty()) {
            return Map.of();
        }
        return addrRegionMapper.selectBatchIds(regionIds).stream()
            .filter(Objects::nonNull)
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(AddrRegion::getId, item -> item.getName() == null ? "" : item.getName(), (a, b) -> a));
    }

    private Map<Long, String> loadRegionNameMapFromLogs(List<WarningProcessLogVO> logs) {
        Set<Long> regionIds = collectLogRegionIds(logs);
        if (regionIds.isEmpty()) {
            return Map.of();
        }
        return addrRegionMapper.selectBatchIds(regionIds).stream()
            .filter(Objects::nonNull)
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(AddrRegion::getId, item -> item.getName() == null ? "" : item.getName(), (a, b) -> a));
    }

    private Map<Long, String> loadRegionPathMap(List<? extends WarningRecordVO> warnings) {
        Set<Long> regionIds = collectWarningRegionIds(warnings);
        if (regionIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        for (Long regionId : regionIds) {
            result.put(regionId, resolveRegionPathText(regionId));
        }
        return result;
    }

    private Map<Long, String> loadRegionPathMapFromLogs(List<WarningProcessLogVO> logs) {
        Set<Long> regionIds = collectLogRegionIds(logs);
        if (regionIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        for (Long regionId : regionIds) {
            result.put(regionId, resolveRegionPathText(regionId));
        }
        return result;
    }

    private Set<Long> collectWarningRegionIds(List<? extends WarningRecordVO> warnings) {
        Set<Long> regionIds = new LinkedHashSet<>();
        for (WarningRecordVO warning : warnings) {
            if (warning != null) {
                addPositiveId(regionIds, warning.getRegionId());
            }
        }
        return regionIds;
    }

    private Set<Long> collectLogRegionIds(List<WarningProcessLogVO> logs) {
        Set<Long> regionIds = new LinkedHashSet<>();
        for (WarningProcessLogVO log : logs) {
            if (log != null) {
                addPositiveId(regionIds, log.getRegionId());
            }
        }
        return regionIds;
    }

    private Map<Long, String> loadEnterpriseNameMap(List<? extends WarningRecordVO> warnings) {
        Set<Long> enterpriseIds = new LinkedHashSet<>();
        for (WarningRecordVO warning : warnings) {
            if (warning == null) {
                continue;
            }
            if ("ENTERPRISE".equalsIgnoreCase(String.valueOf(warning.getBizType()))) {
                addPositiveId(enterpriseIds, warning.getBizId());
            }
            addPositiveId(enterpriseIds, extractEnterpriseId(warning.getPayloadJson()));
        }
        if (enterpriseIds.isEmpty()) {
            return Map.of();
        }
        return foodEnterpriseMapper.selectBatchIds(enterpriseIds).stream()
            .filter(Objects::nonNull)
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(FoodEnterprise::getId, item -> item.getEnterpriseName() == null ? "" : item.getEnterpriseName(), (a, b) -> a));
    }

    private Map<Long, String> loadEnterpriseNameMapFromLogs(List<WarningProcessLogVO> logs) {
        Set<Long> enterpriseIds = new LinkedHashSet<>();
        for (WarningProcessLogVO log : logs) {
            if (log == null) {
                continue;
            }
            if ("ENTERPRISE".equalsIgnoreCase(String.valueOf(log.getBizType()))) {
                addPositiveId(enterpriseIds, log.getBizId());
            }
        }
        if (enterpriseIds.isEmpty()) {
            return Map.of();
        }
        return foodEnterpriseMapper.selectBatchIds(enterpriseIds).stream()
            .filter(Objects::nonNull)
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(FoodEnterprise::getId, item -> item.getEnterpriseName() == null ? "" : item.getEnterpriseName(), (a, b) -> a));
    }

    private String resolveBizName(WarningRecordVO warning, Map<Long, String> enterpriseNameMap) {
        if (warning == null) {
            return null;
        }
        String bizType = warning.getBizType();
        if ("ENTERPRISE".equalsIgnoreCase(String.valueOf(bizType))) {
            String enterpriseName = safeGet(enterpriseNameMap, warning.getBizId());
            if (StringUtils.hasText(enterpriseName)) {
                return enterpriseName;
            }
        }
        String payloadEnterpriseName = safeGet(enterpriseNameMap, extractEnterpriseId(warning.getPayloadJson()));
        if (StringUtils.hasText(payloadEnterpriseName)) {
            return payloadEnterpriseName;
        }
        if (StringUtils.hasText(warning.getBizName())) {
            return warning.getBizName();
        }
        if (StringUtils.hasText(warning.getTitle())) {
            return warning.getTitle();
        }
        if (warning.getBizId() != null && StringUtils.hasText(bizType)) {
            return bizType + " #" + warning.getBizId();
        }
        return null;
    }

    private String resolveLogBizName(WarningProcessLogVO log, Map<Long, String> enterpriseNameMap) {
        if (log == null) {
            return null;
        }
        if ("ENTERPRISE".equalsIgnoreCase(String.valueOf(log.getBizType()))) {
            String enterpriseName = safeGet(enterpriseNameMap, log.getBizId());
            if (StringUtils.hasText(enterpriseName)) {
                return enterpriseName;
            }
        }
        if (StringUtils.hasText(log.getBizName())) {
            return log.getBizName();
        }
        if (StringUtils.hasText(log.getWarningTitle())) {
            return log.getWarningTitle();
        }
        if (log.getBizId() != null && StringUtils.hasText(log.getBizType())) {
            return log.getBizType() + " #" + log.getBizId();
        }
        return null;
    }

    private String resolveOperatorName(WarningProcessLogVO log, Map<Long, String> regulatorNames) {
        if (log == null) {
            return null;
        }
        if (log.getOperatorId() != null && log.getOperatorId() > 0) {
            String regulatorName = regulatorNames.get(log.getOperatorId());
            if (StringUtils.hasText(regulatorName)) {
                return regulatorName;
            }
        }
        String rawName = String.valueOf(log.getOperatorName() == null ? "" : log.getOperatorName()).trim();
        if (!StringUtils.hasText(rawName) || "system".equalsIgnoreCase(rawName)) {
            return "系统";
        }
        return rawName;
    }

    private Long extractEnterpriseId(String payloadJson) {
        Map<String, Object> payload = parsePayload(payloadJson);
        if (payload.isEmpty()) {
            return null;
        }
        return toLong(payload.get("enterpriseId"));
    }

    private Map<String, Object> parsePayload(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payloadJson, MAP_TYPE);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            long longValue = number.longValue();
            return longValue > 0 ? longValue : null;
        }
        try {
            long parsed = Long.parseLong(String.valueOf(value).trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private <K, V> V safeGet(Map<K, V> map, K key) {
        if (map == null || map.isEmpty() || key == null) {
            return null;
        }
        return map.get(key);
    }

    private void addPositiveId(Collection<Long> ids, Long value) {
        if (value != null && value > 0) {
            ids.add(value);
        }
    }

    private String resolveRegionPathText(Long regionId) {
        if (regionId == null || regionId <= 0) {
            return null;
        }
        List<String> names = new java.util.ArrayList<>();
        Long current = regionId;
        while (current != null && current > 0) {
            AddrRegion region = addrRegionMapper.selectById(current);
            if (region == null || region.getDeleted() != null && region.getDeleted() != 0) {
                break;
            }
            if (StringUtils.hasText(region.getName())) {
                names.add(0, region.getName().trim());
            }
            current = region.getParentId();
        }
        return names.isEmpty() ? null : String.join("/", names);
    }

    private String normalizeActionType(WarningProcessActionDTO actionDTO) {
        if (actionDTO == null || !StringUtils.hasText(actionDTO.getActionType())) {
            throw new IllegalArgumentException("actionType required");
        }
        return actionDTO.getActionType().trim().toUpperCase(Locale.ROOT);
    }

    private WarningActionCommentDTO buildActionCommentDTO(String actionComment) {
        WarningActionCommentDTO dto = new WarningActionCommentDTO();
        dto.setActionComment(actionComment);
        return dto;
    }

    private ApiResponse<WarningRecordDetailVO> executeAction(Long warningId,
                                                             String actionType,
                                                             String actionComment,
                                                             String ownerRegulatorId,
                                                             String regionIds,
                                                             String operatorUserId,
                                                             String operatorName) {
        WarningActionCommentDTO body = buildActionCommentDTO(actionComment);
        return switch (actionType) {
            case "PROCESS" -> warningServiceClient.process(
                warningId, body, ownerRegulatorId, regionIds, operatorUserId, operatorName
            );
            case "RESOLVE" -> warningServiceClient.resolve(
                warningId, body, ownerRegulatorId, regionIds, operatorUserId, operatorName
            );
            default -> throw new IllegalArgumentException("unsupported actionType");
        };
    }

    private FoodRegulator requireAdmin(Long userId) {
        FoodRegulator regulator = requireRegulator(userId);
        if (!ROLE_ADMIN.equalsIgnoreCase(regulator.getRoleType())) {
            throw new IllegalArgumentException("admin only");
        }
        return regulator;
    }

    private FoodRegulator requireEnforcer(Long userId) {
        FoodRegulator regulator = requireRegulator(userId);
        if (!ROLE_ENFORCER.equalsIgnoreCase(regulator.getRoleType())) {
            throw new IllegalArgumentException("enforcer only");
        }
        return regulator;
    }

    private FoodRegulator requireRegulator(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        FoodRegulator regulator = foodRegulatorMapper.selectOne(new LambdaQueryWrapper<FoodRegulator>()
            .eq(FoodRegulator::getUserId, userId)
            .eq(FoodRegulator::getDeleted, 0)
            .last("limit 1"));
        if (regulator == null) {
            throw new IllegalArgumentException("regulator not found");
        }
        if (regulator.getStatus() != null && regulator.getStatus() != 1) {
            throw new IllegalArgumentException("regulator disabled");
        }
        return regulator;
    }

    private WarningRecordQueryDTO buildRemoteQuery(WarningRecordQueryDTO queryDTO) {
        WarningRecordQueryDTO remote = new WarningRecordQueryDTO();
        if (queryDTO == null) {
            return remote;
        }
        remote.setPage(normalizePage(queryDTO.getPage()));
        remote.setSize(normalizeSize(queryDTO.getSize()));
        remote.setStatus(queryDTO.getStatus());
        remote.setLevel(queryDTO.getLevel());
        remote.setWarningType(queryDTO.getWarningType());
        remote.setBizType(queryDTO.getBizType());
        remote.setBizId(queryDTO.getBizId());
        remote.setKeyword(queryDTO.getKeyword());
        return remote;
    }

    private Set<Long> resolveRegulatorRegionIds(Long regulatorId) {
        if (regulatorId == null) {
            return Set.of();
        }
        List<Long> directRegionIds = foodRegulatorRegionMapper.selectList(new LambdaQueryWrapper<FoodRegulatorRegion>()
                .eq(FoodRegulatorRegion::getRegulatorId, regulatorId)
                .eq(FoodRegulatorRegion::getDeleted, 0))
            .stream()
            .map(FoodRegulatorRegion::getRegionId)
            .filter(regionId -> regionId != null && regionId > 0)
            .distinct()
            .toList();
        if (directRegionIds.isEmpty()) {
            return Set.of();
        }
        return collectRegionIds(directRegionIds);
    }

    private Set<Long> collectRegionIds(List<Long> rootIds) {
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
        return result;
    }

    private String joinRegionIds(Set<Long> regionIds) {
        if (regionIds == null || regionIds.isEmpty()) {
            return null;
        }
        return regionIds.stream()
            .filter(regionId -> regionId != null && regionId > 0)
            .map(String::valueOf)
            .collect(Collectors.joining(","));
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

    private <T> T requireSuccess(ApiResponse<T> response, String defaultMessage) {
        if (response == null) {
            throw new IllegalStateException(defaultMessage);
        }
        if (response.getCode() != 0) {
            String message = StringUtils.hasText(response.getMessage()) ? response.getMessage() : defaultMessage;
            if (response.getCode() >= 500) {
                throw new IllegalStateException(message);
            }
            throw new IllegalArgumentException(message);
        }
        return response.getData();
    }
}

