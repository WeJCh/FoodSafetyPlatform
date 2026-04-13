package com.mortal.regulation.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.operation.client.regulation.vo.InternalEnterpriseDetailVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.regulation.operation.common.OperationErrorMessages;
import com.mortal.regulation.operation.entity.InspectionItem;
import com.mortal.regulation.operation.entity.InspectionRecord;
import com.mortal.regulation.operation.entity.InspectionTask;
import com.mortal.regulation.operation.entity.RectificationTask;
import com.mortal.regulation.operation.mapper.InspectionItemMapper;
import com.mortal.regulation.operation.mapper.InspectionRecordMapper;
import com.mortal.regulation.operation.mapper.InspectionTaskMapper;
import com.mortal.regulation.operation.mapper.RectificationTaskMapper;
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
    private final InspectionTaskMapper inspectionTaskMapper;
    private final RectificationTaskMapper rectificationTaskMapper;
    private final OperationMasterDataSupport masterDataSupport;

    public InspectionRecordServiceImpl(InspectionRecordMapper inspectionRecordMapper,
                                       InspectionItemMapper inspectionItemMapper,
                                       InspectionTaskMapper inspectionTaskMapper,
                                       RectificationTaskMapper rectificationTaskMapper,
                                       OperationMasterDataSupport masterDataSupport) {
        this.inspectionRecordMapper = inspectionRecordMapper;
        this.inspectionItemMapper = inspectionItemMapper;
        this.inspectionTaskMapper = inspectionTaskMapper;
        this.rectificationTaskMapper = rectificationTaskMapper;
        this.masterDataSupport = masterDataSupport;
    }

    @Override
    public PageResult<InspectionRecordVO> listForEnterprise(Long userId,
                                                            String result,
                                                            LocalDate startDate,
                                                            LocalDate endDate,
                                                            int page,
                                                            int size) {
        InternalEnterpriseDetailVO enterprise = masterDataSupport.requireEnterpriseByUserId(userId);
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<InspectionRecord>()
            .eq(InspectionRecord::getDeleted, 0)
            .eq(InspectionRecord::getEnterpriseId, enterprise.getId());
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
    public InspectionRecordDetailVO getDetailForEnterprise(Long userId, Long recordId) {
        InternalEnterpriseDetailVO enterprise = masterDataSupport.requireEnterpriseByUserId(userId);
        InspectionRecord record = requireRecord(recordId);
        if (!Objects.equals(record.getEnterpriseId(), enterprise.getId())) {
            throw new IllegalArgumentException(OperationErrorMessages.RECORD_NOT_FOUND);
        }
        InspectionRecordDetailVO detail = new InspectionRecordDetailVO();
        detail.setRecord(toVOs(List.of(record)).get(0));
        detail.setItems(loadItems(record.getId()));
        return detail;
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
        detail.setRecord(toVOs(List.of(record)).get(0));
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
        Map<Long, InternalEnterpriseDetailVO> enterpriseDetails = loadEnterpriseDetails(records);
        Map<Long, InspectionTask> taskMap = loadTasks(records);
        Map<Long, RectificationTask> rectificationMap = loadRectificationTasks(records);
        return records.stream()
            .map(record -> toVO(record, enterpriseNames, enterpriseDetails, taskMap, rectificationMap))
            .toList();
    }

    private InspectionRecordVO toVO(InspectionRecord record,
                                    Map<Long, String> enterpriseNames,
                                    Map<Long, InternalEnterpriseDetailVO> enterpriseDetails,
                                    Map<Long, InspectionTask> taskMap,
                                    Map<Long, RectificationTask> rectificationMap) {
        InternalEnterpriseDetailVO enterprise = enterpriseDetails.get(record.getEnterpriseId());
        InspectionTask task = record.getTaskId() == null ? null : taskMap.get(record.getTaskId());
        RectificationTask rectification = rectificationMap.get(record.getId());
        InspectionRecordVO vo = new InspectionRecordVO();
        vo.setId(record.getId());
        vo.setTaskId(record.getTaskId());
        if (task != null) {
            vo.setTaskNo(task.getTaskNo());
            vo.setTaskTitle(task.getTaskTitle());
        }
        vo.setEnterpriseId(record.getEnterpriseId());
        vo.setEnterpriseName(enterpriseNames.get(record.getEnterpriseId()));
        if (enterprise != null) {
            vo.setCreditCode(enterprise.getCreditCode());
            vo.setEnterpriseAddress(enterprise.getAddressDetail());
        }
        vo.setInspectionDate(record.getInspectionDate());
        vo.setResult(record.getResult());
        vo.setProblemDesc(record.getProblemDesc());
        if (rectification != null) {
            vo.setRectificationId(rectification.getId());
            vo.setRectificationStatus(rectification.getStatus());
        }
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

    private Map<Long, InternalEnterpriseDetailVO> loadEnterpriseDetails(List<InspectionRecord> records) {
        List<Long> enterpriseIds = records.stream()
            .map(InspectionRecord::getEnterpriseId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (enterpriseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return enterpriseIds.stream().collect(java.util.stream.Collectors.toMap(
            id -> id,
            masterDataSupport::requireEnterprise,
            (a, b) -> a
        ));
    }

    private Map<Long, InspectionTask> loadTasks(List<InspectionRecord> records) {
        List<Long> taskIds = records.stream()
            .map(InspectionRecord::getTaskId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return inspectionTaskMapper.selectList(new LambdaQueryWrapper<InspectionTask>()
                .in(InspectionTask::getId, taskIds)
                .eq(InspectionTask::getDeleted, 0))
            .stream()
            .collect(java.util.stream.Collectors.toMap(InspectionTask::getId, item -> item, (a, b) -> a));
    }

    private Map<Long, RectificationTask> loadRectificationTasks(List<InspectionRecord> records) {
        List<Long> inspectionIds = records.stream()
            .map(InspectionRecord::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (inspectionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return rectificationTaskMapper.selectList(new LambdaQueryWrapper<RectificationTask>()
                .in(RectificationTask::getInspectionId, inspectionIds)
                .eq(RectificationTask::getDeleted, 0))
            .stream()
            .collect(java.util.stream.Collectors.toMap(RectificationTask::getInspectionId, item -> item, (a, b) -> a));
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}
