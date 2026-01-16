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
import com.mortal.regulation.service.RegulatorProfileService;
import com.mortal.regulation.vo.RegulatorProfileVO;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegulatorProfileServiceImpl implements RegulatorProfileService {

    private static final Set<String> ROLE_TYPES = Set.of("REGULATOR_ADMIN", "REGULATOR_ENFORCER");

    private final FoodRegulatorMapper foodRegulatorMapper;
    private final FoodRegulatorRegionMapper foodRegulatorRegionMapper;
    private final AddrRegionMapper addrRegionMapper;
    private final UserServiceClient userServiceClient;

    public RegulatorProfileServiceImpl(FoodRegulatorMapper foodRegulatorMapper,
                                       FoodRegulatorRegionMapper foodRegulatorRegionMapper,
                                       AddrRegionMapper addrRegionMapper,
                                       UserServiceClient userServiceClient) {
        this.foodRegulatorMapper = foodRegulatorMapper;
        this.foodRegulatorRegionMapper = foodRegulatorRegionMapper;
        this.addrRegionMapper = addrRegionMapper;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public RegulatorProfileVO createOrUpdate(RegulatorProfileDTO dto) {
        String roleType = normalize(dto.getRoleType());
        if (!ROLE_TYPES.contains(roleType)) {
            throw new IllegalArgumentException("invalid regulator role");
        }
        FoodRegulator regulator = foodRegulatorMapper.selectOne(new LambdaQueryWrapper<FoodRegulator>()
            .eq(FoodRegulator::getUserId, dto.getUserId())
            .eq(FoodRegulator::getDeleted, 0));
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
        updateRegions(regulator.getId(), dto.getRegionIds());
        return toVO(regulator, dto.getRegionIds());
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
    public RegulatorProfileVO updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("status must be 0 or 1");
        }
        FoodRegulator regulator = foodRegulatorMapper.selectById(id);
        if (regulator == null || isDeleted(regulator.getDeleted())) {
            throw new IllegalArgumentException("regulator not found");
        }
        regulator.setStatus(status);
        regulator.setUpdateTime(LocalDateTime.now());
        foodRegulatorMapper.updateById(regulator);
        List<Long> regionIds = findRegionIds(regulator.getId());
        return toVO(regulator, regionIds);
    }

    @Override
    public void deleteRegulator(Long id) {
        FoodRegulator regulator = foodRegulatorMapper.selectById(id);
        if (regulator == null || isDeleted(regulator.getDeleted())) {
            return;
        }
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
        if (regulator.getUserId() != null) {
            userServiceClient.deleteUser(regulator.getUserId());
        }
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

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
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
