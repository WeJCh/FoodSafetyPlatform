package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mortal.regulation.client.UserServiceClient;
import com.mortal.regulation.dto.RegulatorProfileDTO;
import com.mortal.regulation.entity.AddrRegion;
import com.mortal.regulation.entity.FoodRegulator;
import com.mortal.regulation.entity.FoodRegulatorRegion;
import com.mortal.regulation.mapper.AddrRegionMapper;
import com.mortal.regulation.mapper.FoodRegulatorMapper;
import com.mortal.regulation.mapper.FoodRegulatorRegionMapper;
import com.mortal.regulation.service.AuditLogService;
import com.mortal.regulation.service.RegulatorProfileService;
import com.mortal.regulation.support.RegulatorMasterCacheService;
import com.mortal.regulation.vo.AuditLogVO;
import com.mortal.regulation.vo.RegulatorProfileVO;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
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
public class RegulatorProfileServiceImpl implements RegulatorProfileService {

    private static final Set<String> ROLE_TYPES = Set.of("REGULATOR_ADMIN", "REGULATOR_ENFORCER");
    private static final int LEVEL_COUNTY = 3;
    private static final int LEVEL_STREET = 4;

    private final FoodRegulatorMapper foodRegulatorMapper;
    private final FoodRegulatorRegionMapper foodRegulatorRegionMapper;
    private final AddrRegionMapper addrRegionMapper;
    private final UserServiceClient userServiceClient;
    private final RegulatorMasterCacheService regulatorMasterCacheService;
    private final AuditLogService auditLogService;

    public RegulatorProfileServiceImpl(FoodRegulatorMapper foodRegulatorMapper,
                                       FoodRegulatorRegionMapper foodRegulatorRegionMapper,
                                       AddrRegionMapper addrRegionMapper,
                                       UserServiceClient userServiceClient,
                                       RegulatorMasterCacheService regulatorMasterCacheService,
                                       AuditLogService auditLogService) {
        this.foodRegulatorMapper = foodRegulatorMapper;
        this.foodRegulatorRegionMapper = foodRegulatorRegionMapper;
        this.addrRegionMapper = addrRegionMapper;
        this.userServiceClient = userServiceClient;
        this.regulatorMasterCacheService = regulatorMasterCacheService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegulatorProfileVO createOrUpdate(Long operatorUserId, String operatorName, RegulatorProfileDTO dto) {
        String roleType = normalize(dto.getRoleType());
        if (!ROLE_TYPES.contains(roleType)) {
            throw new IllegalArgumentException("invalid regulator role");
        }
        List<Long> regionIds = validateRegionIds(roleType, dto.getRegionIds());
        FoodRegulator regulator = foodRegulatorMapper.selectOne(new LambdaQueryWrapper<FoodRegulator>()
            .eq(FoodRegulator::getUserId, dto.getUserId())
            .eq(FoodRegulator::getDeleted, 0));
        boolean created = regulator == null;
        FoodRegulator beforeRegulator = regulator == null ? null : copyRegulator(regulator);
        List<Long> beforeRegionIds = regulator == null ? List.of() : findRegionIds(regulator.getId());
        if (regulator == null) {
            regulator = new FoodRegulator();
            regulator.setUserId(dto.getUserId());
            regulator.setCreateTime(LocalDateTime.now());
            regulator.setDeleted(0);
        }
        regulator.setName(dto.getName());
        regulator.setPhone(dto.getPhone());
        regulator.setRoleType(roleType);
        regulator.setWorkIdUrl(dto.getWorkIdUrl());
        regulator.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        regulator.setUpdateTime(LocalDateTime.now());

        if (regulator.getId() == null) {
            foodRegulatorMapper.insert(regulator);
        } else {
            foodRegulatorMapper.updateById(regulator);
        }
        updateRegions(regulator.getId(), regionIds);
        evictRegulatorCaches(regulator);
        auditLogService.recordRegulatorAudit(
            operatorUserId,
            operatorName,
            created ? "REGULATOR_CREATE" : "REGULATOR_UPDATE",
            created ? "创建监管人员" : "更新监管人员",
            beforeRegulator,
            copyRegulator(regulator),
            beforeRegionIds,
            regionIds,
            null
        );
        return toVO(regulator, regionIds);
    }

    @Override
    public RegulatorProfileVO getByUserId(Long userId) {
        FoodRegulator regulator = foodRegulatorMapper.selectOne(new LambdaQueryWrapper<FoodRegulator>()
            .eq(FoodRegulator::getUserId, userId)
            .eq(FoodRegulator::getDeleted, 0));
        if (regulator == null) {
            return null;
        }
        List<Long> regionIds = findRegionIds(regulator.getId());
        return toVO(regulator, regionIds);
    }

    @Override
    public RegulatorProfileVO getById(Long id) {
        FoodRegulator regulator = foodRegulatorMapper.selectById(id);
        if (regulator == null || isDeleted(regulator.getDeleted())) {
            return null;
        }
        List<Long> regionIds = findRegionIds(regulator.getId());
        return toVO(regulator, regionIds);
    }

    @Override
    public List<RegulatorProfileVO> list(String roleType, Long regionId) {
        LambdaQueryWrapper<FoodRegulator> wrapper = new LambdaQueryWrapper<FoodRegulator>()
            .eq(FoodRegulator::getDeleted, 0);
        if (StringUtils.hasText(roleType)) {
            wrapper.eq(FoodRegulator::getRoleType, normalize(roleType));
        }
        if (regionId != null) {
            List<Long> regionIds = resolveRegionIds(regionId);
            if (regionIds.isEmpty()) {
                return List.of();
            }
            List<Long> regulatorIds = foodRegulatorRegionMapper.selectList(new LambdaQueryWrapper<FoodRegulatorRegion>()
                    .in(FoodRegulatorRegion::getRegionId, regionIds)
                    .eq(FoodRegulatorRegion::getDeleted, 0))
                .stream()
                .map(FoodRegulatorRegion::getRegulatorId)
                .distinct()
                .toList();
            if (regulatorIds.isEmpty()) {
                return List.of();
            }
            wrapper.in(FoodRegulator::getId, regulatorIds);
        }
        List<FoodRegulator> regulators = foodRegulatorMapper.selectList(wrapper);
        Map<Long, List<Long>> regionMap = loadRegionMap(regulators);
        return regulators.stream()
            .map(regulator -> toVO(regulator, regionMap.getOrDefault(regulator.getId(), List.of())))
            .toList();
    }

    @Override
    public List<RegulatorProfileVO> listEligibleEnforcers(Long currentUserId, Long regionId) {
        FoodRegulator currentRegulator = foodRegulatorMapper.selectOne(new LambdaQueryWrapper<FoodRegulator>()
            .eq(FoodRegulator::getUserId, currentUserId)
            .eq(FoodRegulator::getDeleted, 0));
        if (currentRegulator == null
            || isDeleted(currentRegulator.getDeleted())
            || !"REGULATOR_ADMIN".equals(currentRegulator.getRoleType())) {
            return List.of();
        }
        List<Long> scopeRegionIds = resolveScopeRegionIds(currentRegulator.getId(), regionId);
        if (scopeRegionIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<FoodRegulator> wrapper = new LambdaQueryWrapper<FoodRegulator>()
            .eq(FoodRegulator::getDeleted, 0)
            .eq(FoodRegulator::getRoleType, "REGULATOR_ENFORCER")
            .eq(FoodRegulator::getStatus, 1);
        List<Long> regulatorIds = foodRegulatorRegionMapper.selectList(new LambdaQueryWrapper<FoodRegulatorRegion>()
                .in(FoodRegulatorRegion::getRegionId, scopeRegionIds)
                .eq(FoodRegulatorRegion::getDeleted, 0))
            .stream()
            .map(FoodRegulatorRegion::getRegulatorId)
            .distinct()
            .toList();
        if (regulatorIds.isEmpty()) {
            return List.of();
        }
        wrapper.in(FoodRegulator::getId, regulatorIds);
        List<FoodRegulator> regulators = foodRegulatorMapper.selectList(wrapper);
        Map<Long, List<Long>> regionMap = loadRegionMap(regulators);
        return regulators.stream()
            .map(regulator -> toVO(regulator, regionMap.getOrDefault(regulator.getId(), List.of())))
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegulatorProfileVO updateStatus(Long operatorUserId, String operatorName, Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("status must be 0 or 1");
        }
        FoodRegulator regulator = foodRegulatorMapper.selectById(id);
        if (regulator == null || isDeleted(regulator.getDeleted())) {
            throw new IllegalArgumentException("regulator not found");
        }
        List<Long> regionIds = findRegionIds(regulator.getId());
        FoodRegulator before = copyRegulator(regulator);
        regulator.setStatus(status);
        regulator.setUpdateTime(LocalDateTime.now());
        foodRegulatorMapper.updateById(regulator);
        evictRegulatorCaches(regulator);
        auditLogService.recordRegulatorAudit(
            operatorUserId,
            operatorName,
            "REGULATOR_STATUS_CHANGE",
            "调整账号状态",
            before,
            copyRegulator(regulator),
            regionIds,
            regionIds,
            "调整前状态=" + before.getStatus() + "，调整后状态=" + regulator.getStatus()
        );
        return toVO(regulator, regionIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRegulator(Long operatorUserId, String operatorName, Long id) {
        FoodRegulator regulator = foodRegulatorMapper.selectById(id);
        if (regulator == null || isDeleted(regulator.getDeleted())) {
            return;
        }
        List<Long> beforeRegionIds = findRegionIds(id);
        FoodRegulator beforeRegulator = copyRegulator(regulator);
        regulator.setDeleted(1);
        regulator.setStatus(0);
        regulator.setUpdateTime(LocalDateTime.now());
        foodRegulatorMapper.updateById(regulator);
        foodRegulatorRegionMapper.update(
            null,
            new LambdaUpdateWrapper<FoodRegulatorRegion>()
                .eq(FoodRegulatorRegion::getRegulatorId, id)
                .set(FoodRegulatorRegion::getDeleted, 1)
        );
        evictRegulatorCaches(regulator);
        auditLogService.recordRegulatorAudit(
            operatorUserId,
            operatorName,
            "REGULATOR_DELETE",
            "删除监管人员",
            beforeRegulator,
            copyRegulator(regulator),
            beforeRegionIds,
            List.of(),
            null
        );
        if (regulator.getUserId() != null) {
            userServiceClient.deleteUser(regulator.getUserId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegulatorProfileVO adjustRegions(Long operatorUserId,
                                            String operatorName,
                                            Long id,
                                            List<Long> regionIds,
                                            String remark) {
        FoodRegulator regulator = foodRegulatorMapper.selectById(id);
        if (regulator == null || isDeleted(regulator.getDeleted())) {
            throw new IllegalArgumentException("regulator not found");
        }
        List<Long> beforeRegionIds = findRegionIds(id);
        FoodRegulator beforeRegulator = copyRegulator(regulator);
        List<Long> validatedRegionIds = validateRegionIds(regulator.getRoleType(), regionIds);
        updateRegions(id, validatedRegionIds);
        regulator.setUpdateTime(LocalDateTime.now());
        foodRegulatorMapper.updateById(regulator);
        evictRegulatorCaches(regulator);
        auditLogService.recordRegulatorAudit(
            operatorUserId,
            operatorName,
            "REGULATOR_REGION_ADJUST",
            "调整监管辖区",
            beforeRegulator,
            copyRegulator(regulator),
            beforeRegionIds,
            validatedRegionIds,
            remark
        );
        return toVO(regulator, validatedRegionIds);
    }

    @Override
    public List<AuditLogVO> listAuditLogs(Long id, Integer limit) {
        return auditLogService.listRegulatorLogs(id, limit == null ? 10 : limit);
    }

    @Override
    public List<AuditLogVO> listRecentAuditLogs(Integer limit) {
        return auditLogService.listRecentRegulatorLogs(limit == null ? 10 : limit);
    }

    private void updateRegions(Long regulatorId, List<Long> regionIds) {
        if (regulatorId == null) {
            return;
        }
        List<Long> cleaned = sanitizeRegionIds(regionIds);
        foodRegulatorRegionMapper.update(
            null,
            new LambdaUpdateWrapper<FoodRegulatorRegion>()
                .eq(FoodRegulatorRegion::getRegulatorId, regulatorId)
                .set(FoodRegulatorRegion::getDeleted, 1)
        );
        for (Long regionId : cleaned) {
            int updated = foodRegulatorRegionMapper.update(
                null,
                new LambdaUpdateWrapper<FoodRegulatorRegion>()
                    .eq(FoodRegulatorRegion::getRegulatorId, regulatorId)
                    .eq(FoodRegulatorRegion::getRegionId, regionId)
                    .set(FoodRegulatorRegion::getDeleted, 0)
            );
            if (updated == 0) {
                FoodRegulatorRegion region = new FoodRegulatorRegion();
                region.setRegulatorId(regulatorId);
                region.setRegionId(regionId);
                region.setDeleted(0);
                foodRegulatorRegionMapper.insert(region);
            }
        }
    }

    private List<Long> findRegionIds(Long regulatorId) {
        if (regulatorId == null) {
            return List.of();
        }
        return foodRegulatorRegionMapper.selectList(new LambdaQueryWrapper<FoodRegulatorRegion>()
                .eq(FoodRegulatorRegion::getRegulatorId, regulatorId)
                .eq(FoodRegulatorRegion::getDeleted, 0))
            .stream()
            .map(FoodRegulatorRegion::getRegionId)
            .distinct()
            .toList();
    }

    private Map<Long, List<Long>> loadRegionMap(List<FoodRegulator> regulators) {
        if (regulators == null || regulators.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> regulatorIds = regulators.stream()
            .map(FoodRegulator::getId)
            .filter(Objects::nonNull)
            .toList();
        if (regulatorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<Long>> result = new HashMap<>();
        List<FoodRegulatorRegion> rows = foodRegulatorRegionMapper.selectList(
            new LambdaQueryWrapper<FoodRegulatorRegion>()
                .in(FoodRegulatorRegion::getRegulatorId, regulatorIds)
                .eq(FoodRegulatorRegion::getDeleted, 0)
        );
        for (FoodRegulatorRegion row : rows) {
            result.computeIfAbsent(row.getRegulatorId(), key -> new java.util.ArrayList<>())
                .add(row.getRegionId());
        }
        return result;
    }

    private List<Long> sanitizeRegionIds(List<Long> regionIds) {
        if (regionIds == null || regionIds.isEmpty()) {
            return List.of();
        }
        return regionIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    private List<Long> resolveRegionIds(Long regionId) {
        if (regionId == null) {
            return List.of();
        }
        AddrRegion root = addrRegionMapper.selectById(regionId);
        if (root == null || isDeleted(root.getDeleted())) {
            return List.of();
        }
        List<Long> result = new java.util.ArrayList<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(regionId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            result.add(current);
            List<AddrRegion> children = addrRegionMapper.selectList(new LambdaQueryWrapper<AddrRegion>()
                .eq(AddrRegion::getParentId, current)
                .eq(AddrRegion::getDeleted, 0));
            for (AddrRegion child : children) {
                if (child.getId() != null) {
                    queue.add(child.getId());
                }
            }
        }
        return result;
    }

    private void evictRegulatorCaches(FoodRegulator regulator) {
        if (regulator == null) {
            return;
        }
        regulatorMasterCacheService.evict(regulator.getId(), regulator.getUserId());
        regulatorMasterCacheService.bumpScopeEnterpriseVersion();
    }

    private List<Long> resolveAssignableRegionIds(Long regionId) {
        if (regionId == null) {
            return List.of();
        }
        AddrRegion region = addrRegionMapper.selectById(regionId);
        if (region == null || isDeleted(region.getDeleted())) {
            return List.of();
        }
        Integer level = region.getLevel();
        if (level != null && level >= LEVEL_STREET) {
            return List.of(regionId);
        }
        return resolveRegionIds(regionId);
    }

    private List<Long> resolveScopeRegionIds(Long regulatorId, Long requestedRegionId) {
        List<Long> directRegionIds = findRegionIds(regulatorId);
        if (directRegionIds.isEmpty()) {
            return List.of();
        }
        if (requestedRegionId != null) {
            boolean inScope = directRegionIds.stream()
                .anyMatch(directRegionId -> isAncestorRegion(directRegionId, requestedRegionId));
            if (!inScope) {
                return List.of();
            }
            return resolveAssignableRegionIds(requestedRegionId);
        }
        Set<Long> result = new LinkedHashSet<>();
        for (Long directRegionId : directRegionIds) {
            result.addAll(resolveRegionIds(directRegionId));
        }
        return result.stream().toList();
    }

    private List<Long> validateRegionIds(String roleType, List<Long> regionIds) {
        List<Long> cleaned = sanitizeRegionIds(regionIds);
        if (cleaned.size() != 1) {
            throw new IllegalArgumentException("exactly one region required");
        }
        Long regionId = cleaned.get(0);
        AddrRegion region = addrRegionMapper.selectById(regionId);
        if (region == null || isDeleted(region.getDeleted())) {
            throw new IllegalArgumentException("region not found");
        }
        Integer level = region.getLevel();
        if ("REGULATOR_ADMIN".equals(roleType) && !Objects.equals(level, LEVEL_COUNTY)) {
            throw new IllegalArgumentException("admin region must be county level");
        }
        if ("REGULATOR_ENFORCER".equals(roleType) && !Objects.equals(level, LEVEL_STREET)) {
            throw new IllegalArgumentException("enforcer region must be street level");
        }
        return cleaned;
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
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

    private FoodRegulator copyRegulator(FoodRegulator regulator) {
        FoodRegulator copy = new FoodRegulator();
        copy.setId(regulator.getId());
        copy.setUserId(regulator.getUserId());
        copy.setName(regulator.getName());
        copy.setPhone(regulator.getPhone());
        copy.setRoleType(regulator.getRoleType());
        copy.setStatus(regulator.getStatus());
        copy.setWorkIdUrl(regulator.getWorkIdUrl());
        copy.setCreateTime(regulator.getCreateTime());
        copy.setUpdateTime(regulator.getUpdateTime());
        copy.setDeleted(regulator.getDeleted());
        return copy;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private RegulatorProfileVO toVO(FoodRegulator regulator, List<Long> regionIds) {
        RegulatorProfileVO vo = new RegulatorProfileVO();
        vo.setId(regulator.getId());
        vo.setUserId(regulator.getUserId());
        vo.setName(regulator.getName());
        vo.setPhone(regulator.getPhone());
        vo.setRoleType(regulator.getRoleType());
        vo.setRegionIds(regionIds == null ? List.of() : regionIds);
        vo.setStatus(regulator.getStatus());
        vo.setWorkIdUrl(regulator.getWorkIdUrl());
        vo.setCreateTime(regulator.getCreateTime());
        vo.setUpdateTime(regulator.getUpdateTime());
        return vo;
    }
}
