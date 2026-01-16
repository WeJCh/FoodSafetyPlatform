package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.regulation.dto.RegionCreateDTO;
import com.mortal.regulation.entity.AddrRegion;
import com.mortal.regulation.mapper.AddrRegionMapper;
import com.mortal.regulation.service.RegionService;
import com.mortal.regulation.vo.RegionVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegionServiceImpl implements RegionService {

    private final AddrRegionMapper addrRegionMapper;

    public RegionServiceImpl(AddrRegionMapper addrRegionMapper) {
        this.addrRegionMapper = addrRegionMapper;
    }

    @Override
    public RegionVO create(RegionCreateDTO dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new IllegalArgumentException("region name required");
        }
        AddrRegion region = new AddrRegion();
        region.setParentId(dto.getParentId());
        region.setName(dto.getName().trim());
        region.setLevel(dto.getLevel());
        region.setDeleted(0);
        addrRegionMapper.insert(region);
        return toVO(region);
    }

    @Override
    public List<RegionVO> listByParentId(Long parentId) {
        LambdaQueryWrapper<AddrRegion> wrapper = new LambdaQueryWrapper<AddrRegion>()
            .eq(AddrRegion::getDeleted, 0);
        if (parentId == null) {
            wrapper.isNull(AddrRegion::getParentId);
        } else {
            wrapper.eq(AddrRegion::getParentId, parentId);
        }
        return addrRegionMapper.selectList(wrapper)
            .stream()
            .map(this::toVO)
            .toList();
    }

    private RegionVO toVO(AddrRegion region) {
        RegionVO vo = new RegionVO();
        vo.setId(region.getId());
        vo.setParentId(region.getParentId());
        vo.setName(region.getName());
        vo.setLevel(region.getLevel());
        return vo;
    }
}
