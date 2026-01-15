package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.regulation.client.UserServiceClient;
import com.mortal.regulation.dto.RegulatorProfileDTO;
import com.mortal.regulation.entity.FoodRegulator;
import com.mortal.regulation.mapper.FoodRegulatorMapper;
import com.mortal.regulation.service.RegulatorProfileService;
import com.mortal.regulation.vo.RegulatorProfileVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegulatorProfileServiceImpl implements RegulatorProfileService {

    private static final Set<String> ROLE_TYPES = Set.of("REGULATOR_ADMIN", "REGULATOR_ENFORCER");

    private final FoodRegulatorMapper foodRegulatorMapper;
    private final UserServiceClient userServiceClient;

    public RegulatorProfileServiceImpl(FoodRegulatorMapper foodRegulatorMapper,
                                       UserServiceClient userServiceClient) {
        this.foodRegulatorMapper = foodRegulatorMapper;
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
        regulator.setJurisdictionArea(dto.getJurisdictionArea());
        regulator.setWorkIdUrl(dto.getWorkIdUrl());
        regulator.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        regulator.setUpdateTime(LocalDateTime.now());

        if (regulator.getId() == null) {
            foodRegulatorMapper.insert(regulator);
        } else {
            foodRegulatorMapper.updateById(regulator);
        }
        return toVO(regulator);
    }

    @Override
    public RegulatorProfileVO getByUserId(Long userId) {
        FoodRegulator regulator = foodRegulatorMapper.selectOne(new LambdaQueryWrapper<FoodRegulator>()
            .eq(FoodRegulator::getUserId, userId)
            .eq(FoodRegulator::getDeleted, 0));
        return regulator == null ? null : toVO(regulator);
    }

    @Override
    public RegulatorProfileVO getById(Long id) {
        FoodRegulator regulator = foodRegulatorMapper.selectById(id);
        if (regulator == null || isDeleted(regulator.getDeleted())) {
            return null;
        }
        return toVO(regulator);
    }

    @Override
    public List<RegulatorProfileVO> list(String roleType, String jurisdictionArea) {
        LambdaQueryWrapper<FoodRegulator> wrapper = new LambdaQueryWrapper<FoodRegulator>()
            .eq(FoodRegulator::getDeleted, 0);
        if (StringUtils.hasText(roleType)) {
            wrapper.eq(FoodRegulator::getRoleType, normalize(roleType));
        }
        if (StringUtils.hasText(jurisdictionArea)) {
            wrapper.like(FoodRegulator::getJurisdictionArea, jurisdictionArea.trim());
        }
        return foodRegulatorMapper.selectList(wrapper)
            .stream()
            .map(this::toVO)
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
        return toVO(regulator);
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
        if (regulator.getUserId() != null) {
            userServiceClient.deleteUser(regulator.getUserId());
        }
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private RegulatorProfileVO toVO(FoodRegulator regulator) {
        RegulatorProfileVO vo = new RegulatorProfileVO();
        vo.setId(regulator.getId());
        vo.setUserId(regulator.getUserId());
        vo.setName(regulator.getName());
        vo.setPhone(regulator.getPhone());
        vo.setRoleType(regulator.getRoleType());
        vo.setJurisdictionArea(regulator.getJurisdictionArea());
        vo.setStatus(regulator.getStatus());
        vo.setWorkIdUrl(regulator.getWorkIdUrl());
        vo.setCreateTime(regulator.getCreateTime());
        vo.setUpdateTime(regulator.getUpdateTime());
        return vo;
    }
}
