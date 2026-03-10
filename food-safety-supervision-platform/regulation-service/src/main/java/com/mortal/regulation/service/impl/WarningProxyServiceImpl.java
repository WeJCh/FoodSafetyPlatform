package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.regulation.client.WarningServiceClient;
import com.mortal.regulation.common.ApiResponse;
import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.dto.WarningActionCommentDTO;
import com.mortal.regulation.dto.WarningAssignDTO;
import com.mortal.regulation.dto.WarningProcessActionDTO;
import com.mortal.regulation.dto.WarningRecordQueryDTO;
import com.mortal.regulation.entity.AddrRegion;
import com.mortal.regulation.entity.FoodRegulator;
import com.mortal.regulation.entity.FoodRegulatorRegion;
import com.mortal.regulation.mapper.AddrRegionMapper;
import com.mortal.regulation.mapper.FoodRegulatorMapper;
import com.mortal.regulation.mapper.FoodRegulatorRegionMapper;
import com.mortal.regulation.service.WarningProxyService;
import com.mortal.regulation.vo.WarningRecordDetailVO;
import com.mortal.regulation.vo.WarningRecordVO;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
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

    private final WarningServiceClient warningServiceClient;
    private final FoodRegulatorMapper foodRegulatorMapper;
    private final FoodRegulatorRegionMapper foodRegulatorRegionMapper;
    private final AddrRegionMapper addrRegionMapper;

    public WarningProxyServiceImpl(WarningServiceClient warningServiceClient,
                                   FoodRegulatorMapper foodRegulatorMapper,
                                   FoodRegulatorRegionMapper foodRegulatorRegionMapper,
                                   AddrRegionMapper addrRegionMapper) {
        this.warningServiceClient = warningServiceClient;
        this.foodRegulatorMapper = foodRegulatorMapper;
        this.foodRegulatorRegionMapper = foodRegulatorRegionMapper;
        this.addrRegionMapper = addrRegionMapper;
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
        return requireSuccess(response, "load warning records failed");
    }

    @Override
    public WarningRecordDetailVO getAdminWarningDetail(Long userId, Long warningId) {
        FoodRegulator admin = requireAdmin(userId);
        String regionIds = joinRegionIds(resolveRegulatorRegionIds(admin.getId()));
        if (!StringUtils.hasText(regionIds)) {
            throw new IllegalArgumentException("warning not found");
        }
        ApiResponse<WarningRecordDetailVO> response = warningServiceClient.detail(warningId, null, regionIds);
        return requireSuccess(response, "load warning detail failed");
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
            String.valueOf(userId),
            StringUtils.hasText(username) ? username.trim() : "unknown"
        );
        return requireSuccess(response, "process warning failed");
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
            String.valueOf(userId),
            StringUtils.hasText(username) ? username.trim() : "unknown"
        );
        return requireSuccess(response, "assign warning failed");
    }

    @Override
    public PageResult<WarningRecordVO> listMyWarnings(Long userId, WarningRecordQueryDTO queryDTO) {
        FoodRegulator enforcer = requireEnforcer(userId);
        WarningRecordQueryDTO remoteQuery = buildRemoteQuery(queryDTO);
        remoteQuery.setOwnerRegulatorId(enforcer.getId());
        ApiResponse<PageResult<WarningRecordVO>> response = warningServiceClient.pageRecords(remoteQuery);
        return requireSuccess(response, "load warning records failed");
    }

    @Override
    public WarningRecordDetailVO getMyWarningDetail(Long userId, Long warningId) {
        FoodRegulator enforcer = requireEnforcer(userId);
        ApiResponse<WarningRecordDetailVO> response = warningServiceClient.detail(
            warningId,
            String.valueOf(enforcer.getId()),
            null
        );
        return requireSuccess(response, "load warning detail failed");
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
            String.valueOf(userId),
            StringUtils.hasText(username) ? username.trim() : "unknown"
        );
        return requireSuccess(response, "process warning failed");
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
