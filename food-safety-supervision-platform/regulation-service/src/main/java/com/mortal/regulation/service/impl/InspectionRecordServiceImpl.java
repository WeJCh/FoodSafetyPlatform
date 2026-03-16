package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.entity.AddrRegion;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.entity.FoodRegulator;
import com.mortal.regulation.entity.FoodRegulatorRegion;
import com.mortal.regulation.entity.InspectionItem;
import com.mortal.regulation.entity.InspectionRecord;
import com.mortal.regulation.mapper.AddrRegionMapper;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.mapper.FoodRegulatorMapper;
import com.mortal.regulation.mapper.FoodRegulatorRegionMapper;
import com.mortal.regulation.mapper.InspectionItemMapper;
import com.mortal.regulation.mapper.InspectionRecordMapper;
import com.mortal.regulation.service.InspectionRecordService;
import com.mortal.regulation.vo.InspectionItemVO;
import com.mortal.regulation.vo.InspectionRecordDetailVO;
import com.mortal.regulation.vo.InspectionRecordVO;
import java.time.LocalDate;
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
public class InspectionRecordServiceImpl implements InspectionRecordService {

    private static final String ROLE_ADMIN = "REGULATOR_ADMIN";
    private static final String ROLE_ENFORCER = "REGULATOR_ENFORCER";

    private final InspectionRecordMapper inspectionRecordMapper;
    private final InspectionItemMapper inspectionItemMapper;
    private final FoodEnterpriseMapper foodEnterpriseMapper;
    private final FoodRegulatorMapper foodRegulatorMapper;
    private final FoodRegulatorRegionMapper foodRegulatorRegionMapper;
    private final AddrRegionMapper addrRegionMapper;

    public InspectionRecordServiceImpl(InspectionRecordMapper inspectionRecordMapper,
                                       InspectionItemMapper inspectionItemMapper,
                                       FoodEnterpriseMapper foodEnterpriseMapper,
                                       FoodRegulatorMapper foodRegulatorMapper,
                                       FoodRegulatorRegionMapper foodRegulatorRegionMapper,
                                       AddrRegionMapper addrRegionMapper) {
        this.inspectionRecordMapper = inspectionRecordMapper;
        this.inspectionItemMapper = inspectionItemMapper;
        this.foodEnterpriseMapper = foodEnterpriseMapper;
        this.foodRegulatorMapper = foodRegulatorMapper;
        this.foodRegulatorRegionMapper = foodRegulatorRegionMapper;
        this.addrRegionMapper = addrRegionMapper;
    }

    @Override
    public PageResult<InspectionRecordVO> listMy(Long userId,
                                                 String enterpriseName,
                                                 String result,
                                                 LocalDate startDate,
                                                 LocalDate endDate,
                                                 int page,
                                                 int size) {
        FoodRegulator regulator = requireRegulator(userId);
        requireRole(regulator, ROLE_ENFORCER);
        List<Long> enterpriseIds = resolveEnterpriseIds(enterpriseName);
        if (StringUtils.hasText(enterpriseName) && enterpriseIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<InspectionRecord>()
            .eq(InspectionRecord::getDeleted, 0)
            .eq(InspectionRecord::getInspectorId, regulator.getId());
        if (enterpriseIds != null) {
            wrapper.in(InspectionRecord::getEnterpriseId, enterpriseIds);
        }
        if (StringUtils.hasText(result)) {
            wrapper.eq(InspectionRecord::getResult, normalize(result));
        }
        if (startDate != null) {
            wrapper.ge(InspectionRecord::getInspectionDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(InspectionRecord::getInspectionDate, endDate);
        }
        wrapper.orderByDesc(InspectionRecord::getUpdateTime);
        Page<InspectionRecord> pageInfo = inspectionRecordMapper.selectPage(new Page<>(page, size), wrapper);
        List<InspectionRecord> records = pageInfo.getRecords();
        List<InspectionRecordVO> vos = toVOs(records);
        return PageResult.of(vos, pageInfo.getTotal(), page, size);
    }

    @Override
    public PageResult<InspectionRecordVO> listForAdmin(Long userId,
                                                       String enterpriseName,
                                                       String result,
                                                       LocalDate startDate,
                                                       LocalDate endDate,
                                                       int page,
                                                       int size) {
        FoodRegulator regulator = requireRegulator(userId);
        requireRole(regulator, ROLE_ADMIN);
        List<Long> regionIds = resolveRegulatorRegionIds(regulator.getId());
        if (regionIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        List<Long> enterpriseIds = resolveEnterpriseIds(regionIds, enterpriseName);
        if (enterpriseIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<InspectionRecord>()
            .eq(InspectionRecord::getDeleted, 0)
            .in(InspectionRecord::getEnterpriseId, enterpriseIds);
        if (StringUtils.hasText(result)) {
            wrapper.eq(InspectionRecord::getResult, normalize(result));
        }
        if (startDate != null) {
            wrapper.ge(InspectionRecord::getInspectionDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(InspectionRecord::getInspectionDate, endDate);
        }
        wrapper.orderByDesc(InspectionRecord::getUpdateTime);
        Page<InspectionRecord> pageInfo = inspectionRecordMapper.selectPage(new Page<>(page, size), wrapper);
        List<InspectionRecord> records = pageInfo.getRecords();
        List<InspectionRecordVO> vos = toVOs(records);
        return PageResult.of(vos, pageInfo.getTotal(), page, size);
    }

    @Override
    public InspectionRecordDetailVO getDetail(Long userId, Long recordId) {
        FoodRegulator regulator = requireRegulator(userId);
        InspectionRecord record = requireRecord(recordId);
        if (ROLE_ENFORCER.equalsIgnoreCase(regulator.getRoleType())) {
            if (!Objects.equals(record.getInspectorId(), regulator.getId())) {
                throw new IllegalArgumentException("record not assigned to you");
            }
        } else if (ROLE_ADMIN.equalsIgnoreCase(regulator.getRoleType())) {
            FoodEnterprise enterprise = requireEnterprise(record.getEnterpriseId());
            if (!coversRegion(regulator.getId(), enterprise.getRegionId())) {
                throw new IllegalArgumentException("record not in regulator region");
            }
        } else {
            throw new IllegalArgumentException("invalid regulator role");
        }
        InspectionRecordDetailVO detail = new InspectionRecordDetailVO();
        detail.setRecord(toVO(record, loadEnterpriseNames(List.of(record))));
        detail.setItems(loadItems(record.getId()));
        return detail;
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

    private InspectionRecord requireRecord(Long id) {
        InspectionRecord record = inspectionRecordMapper.selectById(id);
        if (record == null || isDeleted(record.getDeleted())) {
            throw new IllegalArgumentException("record not found");
        }
        return record;
    }

    private FoodEnterprise requireEnterprise(Long enterpriseId) {
        FoodEnterprise enterprise = foodEnterpriseMapper.selectById(enterpriseId);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            throw new IllegalArgumentException("enterprise not found");
        }
        return enterprise;
    }

    private List<InspectionItemVO> loadItems(Long recordId) {
        if (recordId == null) {
            return List.of();
        }
        List<InspectionItem> items = inspectionItemMapper.selectList(new LambdaQueryWrapper<InspectionItem>()
            .eq(InspectionItem::getInspectionId, recordId)
            .eq(InspectionItem::getDeleted, 0)
            .orderByAsc(InspectionItem::getId));
        return items.stream().map(this::toItemVO).toList();
    }

    private List<InspectionRecordVO> toVOs(List<InspectionRecord> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        Map<Long, String> enterpriseNames = loadEnterpriseNames(records);
        return records.stream()
            .map(record -> toVO(record, enterpriseNames))
            .toList();
    }

    private InspectionRecordVO toVO(InspectionRecord record, Map<Long, String> enterpriseNames) {
        InspectionRecordVO vo = new InspectionRecordVO();
        vo.setId(record.getId());
        vo.setEnterpriseId(record.getEnterpriseId());
        vo.setEnterpriseName(enterpriseNames.get(record.getEnterpriseId()));
        vo.setInspectionDate(record.getInspectionDate());
        vo.setResult(record.getResult());
        vo.setProblemDesc(record.getProblemDesc());
        vo.setUpdateTime(record.getUpdateTime());
        return vo;
    }

    private InspectionItemVO toItemVO(InspectionItem item) {
        InspectionItemVO vo = new InspectionItemVO();
        vo.setItemName(item.getItemName());
        vo.setItemResult(item.getItemResult());
        vo.setProblemDesc(item.getProblemDesc());
        return vo;
    }

    private Map<Long, String> loadEnterpriseNames(List<InspectionRecord> records) {
        List<Long> enterpriseIds = records.stream()
            .map(InspectionRecord::getEnterpriseId)
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

    private List<Long> resolveEnterpriseIds(List<Long> regionIds, String enterpriseName) {
        if (regionIds == null || regionIds.isEmpty()) {
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

    private boolean coversRegion(Long regulatorId, Long regionId) {
        if (regulatorId == null || regionId == null) {
            return false;
        }
        List<Long> regionIds = resolveRegulatorRegionIds(regulatorId);
        return regionIds.contains(regionId);
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

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}

