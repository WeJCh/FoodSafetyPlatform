package com.mortal.regulation.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.operation.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.regulation.operation.common.OperationErrorMessages;
import com.mortal.regulation.operation.entity.InspectionItem;
import com.mortal.regulation.operation.entity.InspectionRecord;
import com.mortal.regulation.operation.mapper.InspectionItemMapper;
import com.mortal.regulation.operation.mapper.InspectionRecordMapper;
import com.mortal.regulation.operation.service.InspectionRecordService;
import com.mortal.regulation.operation.support.OperationMasterDataSupport;
import com.mortal.regulation.operation.vo.InspectionItemVO;
import com.mortal.regulation.operation.vo.InspectionRecordDetailVO;
import com.mortal.regulation.operation.vo.InspectionRecordVO;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InspectionRecordServiceImpl implements InspectionRecordService {

    private final InspectionRecordMapper inspectionRecordMapper;
    private final InspectionItemMapper inspectionItemMapper;
    private final OperationMasterDataSupport masterDataSupport;

    public InspectionRecordServiceImpl(InspectionRecordMapper inspectionRecordMapper,
                                       InspectionItemMapper inspectionItemMapper,
                                       OperationMasterDataSupport masterDataSupport) {
        this.inspectionRecordMapper = inspectionRecordMapper;
        this.inspectionItemMapper = inspectionItemMapper;
        this.masterDataSupport = masterDataSupport;
    }

    @Override
    public PageResult<InspectionRecordVO> listMy(Long userId,
                                                 String enterpriseName,
                                                 String result,
                                                 LocalDate startDate,
                                                 LocalDate endDate,
                                                 int page,
                                                 int size) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireEnforcer(userId);
        List<Long> enterpriseIds = masterDataSupport.queryEnterpriseIdsByName(enterpriseName);
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
        List<InspectionRecordVO> vos = toVOs(pageInfo.getRecords());
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
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireAdmin(userId);
        List<Long> enterpriseIds = masterDataSupport.resolveScopedEnterpriseIds(regulator.getId(), enterpriseName);
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
        List<InspectionRecordVO> vos = toVOs(pageInfo.getRecords());
        return PageResult.of(vos, pageInfo.getTotal(), page, size);
    }

    @Override
    public InspectionRecordDetailVO getDetail(Long userId, Long recordId) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireRegulatorByUserId(userId);
        InspectionRecord record = requireRecord(recordId);
        if (OperationMasterDataSupport.ROLE_ENFORCER.equalsIgnoreCase(regulator.getRoleType())) {
            if (!Objects.equals(record.getInspectorId(), regulator.getId())) {
                throw new IllegalArgumentException("record not assigned to you");
            }
        } else if (OperationMasterDataSupport.ROLE_ADMIN.equalsIgnoreCase(regulator.getRoleType())) {
            masterDataSupport.requireEnterpriseInScope(regulator.getId(), record.getEnterpriseId());
        } else {
            throw new IllegalArgumentException(OperationErrorMessages.INVALID_REGULATOR_ROLE);
        }
        InspectionRecordDetailVO detail = new InspectionRecordDetailVO();
        detail.setRecord(toVO(record, loadEnterpriseNames(List.of(record))));
        detail.setItems(loadItems(record.getId()));
        return detail;
    }

    private InspectionRecord requireRecord(Long id) {
        InspectionRecord record = inspectionRecordMapper.selectById(id);
        if (record == null || isDeleted(record.getDeleted())) {
            throw new IllegalArgumentException(OperationErrorMessages.RECORD_NOT_FOUND);
        }
        return record;
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
        return masterDataSupport.loadEnterpriseNames(enterpriseIds);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}
