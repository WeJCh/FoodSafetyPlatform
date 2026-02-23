package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.common.enums.ComplaintStatus;
import com.mortal.regulation.common.enums.TaskSourceType;
import com.mortal.regulation.dto.ComplaintAssignDTO;
import com.mortal.regulation.dto.ComplaintHandleDTO;
import com.mortal.regulation.dto.ComplaintSubmitDTO;
import com.mortal.regulation.dto.ComplaintRejectDTO;
import com.mortal.regulation.entity.AddrRegion;
import com.mortal.regulation.entity.Complaint;
import com.mortal.regulation.entity.ComplaintHandle;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.entity.FoodRegulator;
import com.mortal.regulation.entity.FoodRegulatorRegion;
import com.mortal.regulation.mapper.AddrRegionMapper;
import com.mortal.regulation.mapper.ComplaintHandleMapper;
import com.mortal.regulation.mapper.ComplaintMapper;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.mapper.FoodRegulatorMapper;
import com.mortal.regulation.mapper.FoodRegulatorRegionMapper;
import com.mortal.regulation.service.ComplaintService;
import com.mortal.regulation.service.EnterpriseProfileService;
import com.mortal.regulation.service.StatusTransitionValidator;
import com.mortal.regulation.vo.ComplaintDetailVO;
import com.mortal.regulation.vo.ComplaintHandleVO;
import com.mortal.regulation.vo.ComplaintListVO;
import com.mortal.regulation.vo.ComplaintTrackVO;
import com.mortal.regulation.vo.ComplaintVO;
import com.mortal.regulation.vo.EnterpriseProfileVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
public class ComplaintServiceImpl implements ComplaintService {

    private static final String ROLE_ADMIN = "REGULATOR_ADMIN";
    private static final String ROLE_ENFORCER = "REGULATOR_ENFORCER";
    private static final int MAX_IMAGE_COUNT = 5;

    private final ComplaintMapper complaintMapper;
    private final ComplaintHandleMapper complaintHandleMapper;
    private final FoodEnterpriseMapper foodEnterpriseMapper;
    private final FoodRegulatorMapper foodRegulatorMapper;
    private final FoodRegulatorRegionMapper foodRegulatorRegionMapper;
    private final AddrRegionMapper addrRegionMapper;
    private final EnterpriseProfileService enterpriseProfileService;
    private final ObjectMapper objectMapper;
    
    public ComplaintServiceImpl(ComplaintMapper complaintMapper,
                                ComplaintHandleMapper complaintHandleMapper,
                                FoodEnterpriseMapper foodEnterpriseMapper,
                                FoodRegulatorMapper foodRegulatorMapper,
                                FoodRegulatorRegionMapper foodRegulatorRegionMapper,
                                AddrRegionMapper addrRegionMapper,
                                EnterpriseProfileService enterpriseProfileService,
                                ObjectMapper objectMapper) {
        this.complaintMapper = complaintMapper;
        this.complaintHandleMapper = complaintHandleMapper;
        this.foodEnterpriseMapper = foodEnterpriseMapper;
        this.foodRegulatorMapper = foodRegulatorMapper;
        this.foodRegulatorRegionMapper = foodRegulatorRegionMapper;
        this.addrRegionMapper = addrRegionMapper;
        this.enterpriseProfileService = enterpriseProfileService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ComplaintTrackVO submitPublic(Long submitterUserId, ComplaintSubmitDTO dto) {
        FoodEnterprise enterprise = requireEnterprise(dto.getEnterpriseId());
        Complaint complaint = new Complaint();
        complaint.setComplaintNo(generateComplaintNo());
        complaint.setComplainantName(trim(dto.getComplainantName()));
        complaint.setContact(trim(dto.getContact()));
        complaint.setSubmitterUserId(submitterUserId);
        complaint.setEnterpriseId(enterprise.getId());
        complaint.setComplaintType(trim(dto.getComplaintType()));
        complaint.setContent(dto.getContent().trim());
        complaint.setImageUrls(serializeImageUrls(dto.getImageUrls()));
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setSourceType(TaskSourceType.MANUAL);
        complaint.setCreateTime(LocalDateTime.now());
        complaint.setUpdateTime(LocalDateTime.now());
        complaint.setDeleted(0);
        complaintMapper.insert(complaint);
        return toTrackVO(complaint);
    }

    /**
     * 闂佽崵濮惧▍锝夊窗閺囥垺鐓傛繝濠傜墕缁狀噣鏌￠崶椋庣？婵?
     * @param complaintNo 闂備胶顢婇崺鏍哄┑瀣剨闁哄啫鍊荤壕浠嬫煛瀹擃喖鍟埢?
     * @param contact 闂備浇澹堟ご绋款潖婵犳碍鐒鹃柟缁㈠枛濡﹢鏌ｉ悢绋款棆缁绢厸鍋?
     * @return 闂備胶顢婇崺鏍哄┑瀣剨闁哄啫鍊瑰畷澶愭煟閺傛鐓奸柛銈呯崏O
     */
    @Override
    public PageResult<ComplaintListVO> listMyPublic(Long submitterUserId, String status, int page, int size) {
        if (submitterUserId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<Complaint>()
            .eq(Complaint::getDeleted, 0)
            .eq(Complaint::getSubmitterUserId, submitterUserId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Complaint::getStatus, normalize(status));
        }
        wrapper.orderByDesc(Complaint::getUpdateTime);
        Page<Complaint> pageInfo = complaintMapper.selectPage(new Page<>(page, size), wrapper);
        List<Complaint> complaints = pageInfo.getRecords();
        Map<Long, String> enterpriseNames = loadEnterpriseNames(complaints);
        List<ComplaintListVO> records = complaints.stream()
            .map(complaint -> toListVO(complaint, enterpriseNames))
            .toList();
        return PageResult.of(records, pageInfo.getTotal(), page, size);
    }

    @Override
    public ComplaintVO getMyPublicDetail(Long submitterUserId, Long complaintId) {
        if (submitterUserId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        Complaint complaint = requireComplaint(complaintId);
        if (!Objects.equals(complaint.getSubmitterUserId(), submitterUserId)) {
            throw new IllegalArgumentException("complaint not found");
        }
        return toVOWithNames(complaint);
    }

    
    /**
     * 查询投诉列表
     * @param operatorUserId 操作员用户ID
     * @param status 状态
     * @param enterpriseName 企业名称
     * @param assignedToName 被指派去处理投诉的执行人姓名
     * @param assignedByName 指派监管员名称
     * @param page 页码
     * @param size 每页条数
     * @return 投诉列表
     */ 
    @Override
    public PageResult<ComplaintVO> list(Long operatorUserId,
                                        String status,
                                        String enterpriseName,
                                        String assignedToName,
                                        String assignedByName,
                                        int page,
                                        int size) {
        FoodRegulator regulator = requireRegulator(operatorUserId);
        // 过滤已删除的投诉
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<Complaint>()
            .eq(Complaint::getDeleted, 0);
        // 过滤状态
        if (StringUtils.hasText(status)) {
            wrapper.eq(Complaint::getStatus, normalize(status));
        }
        // 过滤监管员角色
        if (ROLE_ENFORCER.equalsIgnoreCase(regulator.getRoleType())) {
            wrapper.eq(Complaint::getAssignedTo, regulator.getId());
        }
            // 过滤企业名称
        List<Long> enterpriseIds = resolveEnterpriseIdsByName(enterpriseName);
        // 过滤监管员管辖的企业ID
        List<Long> scopeEnterpriseIds = resolveEnterpriseIdsByRegion(regulator);
        if (scopeEnterpriseIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        if (enterpriseIds == null) {
            enterpriseIds = scopeEnterpriseIds;
        } else {
            enterpriseIds = enterpriseIds.stream().filter(scopeEnterpriseIds::contains).toList();
        }
        if (StringUtils.hasText(enterpriseName) && enterpriseIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        if (enterpriseIds != null) {
            wrapper.in(Complaint::getEnterpriseId, enterpriseIds);
        }
        // 过滤被指派去处理投诉的执行人ID
        List<Long> assignedToIds = resolveRegulatorIdsByName(assignedToName);
        if (StringUtils.hasText(assignedToName) && assignedToIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        if (assignedToIds != null) {
            wrapper.in(Complaint::getAssignedTo, assignedToIds);
        }
        // 过滤指派监管员ID
        List<Long> assignedByIds = resolveRegulatorIdsByName(assignedByName);
        if (StringUtils.hasText(assignedByName) && assignedByIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        if (assignedByIds != null) {
            wrapper.in(Complaint::getAssignedBy, assignedByIds);
        }
        // 排序
        wrapper.orderByDesc(Complaint::getUpdateTime);
        Page<Complaint> pageInfo = complaintMapper.selectPage(new Page<>(page, size), wrapper);
        // 获取投诉列表
        List<Complaint> complaints = pageInfo.getRecords();
        Map<Long, String> enterpriseNames = loadEnterpriseNames(complaints);
        Map<Long, String> regulatorNames = loadRegulatorNames(complaints);
        Map<Long, String> handleResults = loadHandleResults(complaints);
        List<ComplaintVO> records = complaints.stream()
            .map(complaint -> {
                ComplaintVO vo = toVO(complaint, enterpriseNames, regulatorNames);
                vo.setHandleResult(handleResults.get(complaint.getId()));
                return vo;
            })
            .toList();
            // 返回分页结果
        return PageResult.of(records, pageInfo.getTotal(), page, size);
    }
    /**
     * 查询投诉详情
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @return 投诉详情VO
     */
    @Override
    public ComplaintDetailVO getDetail(Long operatorUserId, Long complaintId) {
        FoodRegulator regulator = requireRegulator(operatorUserId);
        Complaint complaint = requireComplaint(complaintId);
        // 验证投诉是否在监管员管辖区域内
        if (!isComplaintInRegion(regulator, complaint.getEnterpriseId())) {
            throw new IllegalArgumentException("complaint not in regulator region");
        }
        // 验证监管员角色
        if (ROLE_ENFORCER.equalsIgnoreCase(regulator.getRoleType())) {
            if (!Objects.equals(complaint.getAssignedTo(), regulator.getId())) {
                throw new IllegalArgumentException("complaint not assigned to you");
            }
        }
        ComplaintDetailVO detail = new ComplaintDetailVO();
        detail.setComplaint(toVOWithNames(complaint));
        // 获取企业信息
        EnterpriseProfileVO enterprise = enterpriseProfileService.getById(complaint.getEnterpriseId());
        detail.setEnterprise(enterprise);
        // 获取处理记录
        detail.setHandles(loadHandleDetails(complaint.getId()));
        return detail;
    }

    @Override
    public ComplaintVO accept(Long operatorUserId, Long complaintId) {
        FoodRegulator regulator = requireRegulator(operatorUserId);
        requireRole(regulator, ROLE_ADMIN);
        Complaint complaint = requireComplaint(complaintId);
        transitionComplaint(complaint, ComplaintStatus.PENDING);
        complaint.setAcceptedBy(regulator.getId());
        complaint.setAcceptedTime(LocalDateTime.now());
        complaint.setUpdateTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
        return toVOWithNames(complaint);
    }

    @Override
    public ComplaintVO assign(Long operatorUserId, Long complaintId, ComplaintAssignDTO dto) {
        FoodRegulator regulator = requireRegulator(operatorUserId);
        requireRole(regulator, ROLE_ADMIN);
        Complaint complaint = requireComplaint(complaintId);
        if (!ComplaintStatus.PENDING.equals(complaint.getStatus())
            && !ComplaintStatus.ASSIGNED.equals(complaint.getStatus())
            && !ComplaintStatus.PROCESSING.equals(complaint.getStatus())) {
            throw new IllegalArgumentException("complaint not ready for assignment");
        }
        FoodRegulator assignee = foodRegulatorMapper.selectById(dto.getRegulatorId());
        if (assignee == null || isDeleted(assignee.getDeleted())) {
            throw new IllegalArgumentException("assignee not found");
        }
        if (!ROLE_ENFORCER.equalsIgnoreCase(assignee.getRoleType())) {
            throw new IllegalArgumentException("assignee must be enforcer");
        }
        complaint.setAssignedTo(assignee.getId());
        complaint.setAssignedBy(regulator.getId());
        complaint.setAssignedTime(LocalDateTime.now());
        transitionComplaint(complaint, ComplaintStatus.ASSIGNED);
        complaint.setUpdateTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
        return toVOWithNames(complaint);
    }

    @Override
    public ComplaintVO startProcess(Long operatorUserId, Long complaintId) {
        FoodRegulator regulator = requireRegulator(operatorUserId);
        requireRole(regulator, ROLE_ENFORCER);
        Complaint complaint = requireComplaint(complaintId);
        if (!Objects.equals(complaint.getAssignedTo(), regulator.getId())) {
            throw new IllegalArgumentException("complaint not assigned to you");
        }
        transitionComplaint(complaint, ComplaintStatus.PROCESSING);
        complaint.setUpdateTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
        return toVOWithNames(complaint);
    }

    @Override
    public ComplaintVO handle(Long operatorUserId, Long complaintId, ComplaintHandleDTO dto) {
        FoodRegulator regulator = requireRegulator(operatorUserId);
        requireRole(regulator, ROLE_ENFORCER);
        Complaint complaint = requireComplaint(complaintId);
        if (!Objects.equals(complaint.getAssignedTo(), regulator.getId())) {
            throw new IllegalArgumentException("complaint not assigned to you");
        }
        if (!ComplaintStatus.PROCESSING.equals(complaint.getStatus())) {
            throw new IllegalArgumentException("complaint not in processing");
        }
        saveSingleHandle(complaint.getId(), regulator.getId(), dto.getHandleResult().trim());
        transitionComplaint(complaint, ComplaintStatus.FEEDBACKED);
        complaint.setProcessedBy(regulator.getId());
        complaint.setProcessedTime(LocalDateTime.now());
        complaint.setUpdateTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
        return toVOWithNames(complaint);
    }

    @Override
    public ComplaintVO reject(Long operatorUserId, Long complaintId, ComplaintRejectDTO dto) {
        FoodRegulator regulator = requireRegulator(operatorUserId);
        requireRole(regulator, ROLE_ADMIN);
        Complaint complaint = requireComplaint(complaintId);
        if (!isComplaintInRegion(regulator, complaint.getEnterpriseId())) {
            throw new IllegalArgumentException("complaint not in regulator region");
        }
        transitionComplaint(complaint, ComplaintStatus.REJECTED);
        saveSingleHandle(complaint.getId(), regulator.getId(), dto.getReason().trim());
        complaint.setRejectedBy(regulator.getId());
        complaint.setRejectedTime(LocalDateTime.now());
        complaint.setUpdateTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
        return toVOWithNames(complaint);
    }

    private void saveSingleHandle(Long complaintId, Long handlerId, String result) {
        LocalDateTime now = LocalDateTime.now();
        ComplaintHandle existing = complaintHandleMapper.selectOne(new LambdaQueryWrapper<ComplaintHandle>()
            .eq(ComplaintHandle::getComplaintId, complaintId)
            .eq(ComplaintHandle::getDeleted, 0)
            .last("limit 1"));
        if (existing == null) {
            ComplaintHandle handle = new ComplaintHandle();
            handle.setComplaintId(complaintId);
            handle.setHandlerId(handlerId);
            handle.setHandleResult(result);
            handle.setHandleTime(now);
            handle.setCreateTime(now);
            handle.setUpdateTime(now);
            handle.setDeleted(0);
            complaintHandleMapper.insert(handle);
            return;
        }
        existing.setHandlerId(handlerId);
        existing.setHandleResult(result);
        existing.setHandleTime(now);
        existing.setUpdateTime(now);
        complaintHandleMapper.updateById(existing);
    }

    private void transitionComplaint(Complaint complaint, ComplaintStatus target) {
        // 验证投诉状态流转
        StatusTransitionValidator.validateComplaintTransition(complaint.getStatus(), target);
        complaint.setStatus(target);
    }
    /**
     * 验证投诉
     * @param id 投诉ID
     * @return 投诉
     */
    private Complaint requireComplaint(Long id) {
        Complaint complaint = complaintMapper.selectById(id);
        if (complaint == null || isDeleted(complaint.getDeleted())) {
            throw new IllegalArgumentException("complaint not found");
        }
        return complaint;
    }
    /**
     * 验证企业
     * @param id 企业ID
     * @return 企业
     */
    private FoodEnterprise requireEnterprise(Long id) {
        FoodEnterprise enterprise = foodEnterpriseMapper.selectById(id);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            throw new IllegalArgumentException("enterprise not found");
        }
        return enterprise;
    }
    /**
     * 验证监管员
     * @param userId 用户ID
     * @return 监管员
     */
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
    /**
     * 验证监管员角色
     * @param regulator 监管员
     * @param roleType 角色类型
     */
    private void requireRole(FoodRegulator regulator, String roleType) {
        if (regulator == null || !roleType.equalsIgnoreCase(regulator.getRoleType())) {
            throw new IllegalArgumentException("invalid regulator role");
        }
    }
    /**
     * 转换为VO
     * @param complaint 投诉
     * @return VO
     */
    private ComplaintVO toVOWithNames(Complaint complaint) {
        Map<Long, String> enterpriseNames = loadEnterpriseNames(List.of(complaint));
        Map<Long, String> regulatorNames = loadRegulatorNames(List.of(complaint));
        Map<Long, String> handleResults = loadHandleResults(List.of(complaint));
        ComplaintVO vo = toVO(complaint, enterpriseNames, regulatorNames);
        vo.setHandleResult(handleResults.get(complaint.getId()));
        return vo;
    }

    private ComplaintListVO toListVO(Complaint complaint, Map<Long, String> enterpriseNames) {
        ComplaintListVO vo = new ComplaintListVO();
        vo.setId(complaint.getId());
        vo.setComplaintNo(complaint.getComplaintNo());
        vo.setEnterpriseId(complaint.getEnterpriseId());
        vo.setEnterpriseName(enterpriseNames.get(complaint.getEnterpriseId()));
        vo.setStatus(complaint.getStatus());
        vo.setCreateTime(complaint.getCreateTime());
        vo.setUpdateTime(complaint.getUpdateTime());
        return vo;
    }

    private ComplaintVO toVO(Complaint complaint,
                             Map<Long, String> enterpriseNames,
                             Map<Long, String> regulatorNames) {
        ComplaintVO vo = new ComplaintVO();
        vo.setId(complaint.getId());
        vo.setComplaintNo(complaint.getComplaintNo());
        vo.setEnterpriseId(complaint.getEnterpriseId());
        vo.setEnterpriseName(enterpriseNames.get(complaint.getEnterpriseId()));
        vo.setComplaintType(complaint.getComplaintType());
        vo.setContent(complaint.getContent());
        vo.setImageUrls(parseImageUrls(complaint.getImageUrls()));
        vo.setAcceptedBy(complaint.getAcceptedBy());
        vo.setAcceptedByName(regulatorNames.get(complaint.getAcceptedBy()));
        vo.setAcceptedTime(complaint.getAcceptedTime());
        vo.setStatus(complaint.getStatus());
        vo.setAssignedTo(complaint.getAssignedTo());
        vo.setAssignedToName(regulatorNames.get(complaint.getAssignedTo()));
        vo.setAssignedBy(complaint.getAssignedBy());
        vo.setAssignedByName(regulatorNames.get(complaint.getAssignedBy()));
        vo.setAssignedTime(complaint.getAssignedTime());
        vo.setProcessedBy(complaint.getProcessedBy());
        vo.setProcessedByName(regulatorNames.get(complaint.getProcessedBy()));
        vo.setProcessedTime(complaint.getProcessedTime());
        vo.setRejectedBy(complaint.getRejectedBy());
        vo.setRejectedByName(regulatorNames.get(complaint.getRejectedBy()));
        vo.setRejectedTime(complaint.getRejectedTime());
        vo.setCreateTime(complaint.getCreateTime());
        vo.setUpdateTime(complaint.getUpdateTime());
        return vo;
    }
    /**
     * 转换为VO
     * @param complaint 投诉
     * @return VO
     */
    private ComplaintTrackVO toTrackVO(Complaint complaint) {
        ComplaintTrackVO vo = new ComplaintTrackVO();
        vo.setComplaintNo(complaint.getComplaintNo());
        vo.setStatus(complaint.getStatus());
        vo.setUpdateTime(complaint.getUpdateTime());
        return vo;
    }

    /**
     * 生成投诉编号
     * @return 投诉编号
     */
    private String generateComplaintNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 9000) + 1000;
        return "CPT" + time + random;
    }

    /**
     * 加载处理记录
     * @param complaintId 投诉ID
     * @return 处理记录列表
     */
    private Map<Long, String> loadHandleResults(List<Complaint> complaints) {
        if (complaints == null || complaints.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> complaintIds = complaints.stream()
            .map(Complaint::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (complaintIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ComplaintHandle> handles = complaintHandleMapper.selectList(new LambdaQueryWrapper<ComplaintHandle>()
            .in(ComplaintHandle::getComplaintId, complaintIds)
            .eq(ComplaintHandle::getDeleted, 0)
            .orderByDesc(ComplaintHandle::getHandleTime));
        if (handles.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> result = new java.util.HashMap<>();
        for (ComplaintHandle handle : handles) {
            result.putIfAbsent(handle.getComplaintId(), handle.getHandleResult());
        }
        return result;
    }

    private List<ComplaintHandleVO> loadHandleDetails(Long complaintId) {
        if (complaintId == null) {
            return List.of();
        }
        List<ComplaintHandle> handles = complaintHandleMapper.selectList(new LambdaQueryWrapper<ComplaintHandle>()
            .eq(ComplaintHandle::getComplaintId, complaintId)
            .eq(ComplaintHandle::getDeleted, 0)
            .orderByDesc(ComplaintHandle::getHandleTime));
        if (handles.isEmpty()) {
            return List.of();
        }
        Set<Long> handlerIds = handles.stream()
            .map(ComplaintHandle::getHandlerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> handlerNames = handlerIds.isEmpty()
            ? Collections.emptyMap()
            : foodRegulatorMapper.selectBatchIds(handlerIds)
                .stream()
                .filter(regulator -> !isDeleted(regulator.getDeleted()))
                .collect(Collectors.toMap(FoodRegulator::getId, FoodRegulator::getName, (a, b) -> a));
        return handles.stream().map(handle -> {
            ComplaintHandleVO vo = new ComplaintHandleVO();
            vo.setHandlerId(handle.getHandlerId());
            vo.setHandlerName(handlerNames.get(handle.getHandlerId()));
            vo.setHandleResult(handle.getHandleResult());
            vo.setHandleTime(handle.getHandleTime());
            return vo;
        }).toList();
    }

    /**
     * 解析企业ID列表
     * @param enterpriseName 企业名称
     * @return 企业ID列表
     */
    private List<Long> resolveEnterpriseIdsByName(String enterpriseName) {
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

    /**
     * 解析监管员ID列表
     * @param regulatorName 监管员名称
     * @return 监管员ID列表
     */
    private List<Long> resolveRegulatorIdsByName(String regulatorName) {
        if (!StringUtils.hasText(regulatorName)) {
            return null;
        }
        List<FoodRegulator> regulators = foodRegulatorMapper.selectList(new LambdaQueryWrapper<FoodRegulator>()
            .eq(FoodRegulator::getDeleted, 0)
            .like(FoodRegulator::getName, regulatorName.trim()));
        if (regulators.isEmpty()) {
            return List.of();
        }
        return regulators.stream()
            .map(FoodRegulator::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    /**
     * 解析企业ID列表
     * @param regulator 监管员
     * @return 企业ID列表
     */
    private List<Long> resolveEnterpriseIdsByRegion(FoodRegulator regulator) {
        if (regulator == null || regulator.getId() == null) {
            return List.of();
        }
        List<Long> regionIds = resolveRegulatorRegionIds(regulator.getId());
        if (regionIds.isEmpty()) {
            return List.of();
        }
        List<FoodEnterprise> enterprises = foodEnterpriseMapper.selectList(new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getDeleted, 0)
            .in(FoodEnterprise::getRegionId, regionIds));
        if (enterprises.isEmpty()) {
            return List.of();
        }
        return enterprises.stream()
            .map(FoodEnterprise::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }
    /**
     * 判断投诉是否在监管员管辖区域内
     * @param regulator 监管员
     * @param enterpriseId 企业ID
     * @return 是否在管辖区域内
     */
    private boolean isComplaintInRegion(FoodRegulator regulator, Long enterpriseId) {
        if (regulator == null || enterpriseId == null) {
            return false;
        }
        List<Long> enterpriseIds = resolveEnterpriseIdsByRegion(regulator);
        return enterpriseIds.contains(enterpriseId);
    }
    /**
     * 解析监管员区域ID
     * @param regulatorId 监管员ID
     * @return 区域ID列表
     */
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

    /**
     * 收集区域ID
     * @param rootIds 根区域ID列表
     * @return 区域ID列表
     */
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

    /**
     * 加载企业名称
     * @param complaints 投诉列表
     * @return 企业名称Map
     */
    private Map<Long, String> loadEnterpriseNames(List<Complaint> complaints) {
        if (complaints == null || complaints.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> enterpriseIds = complaints.stream()
            .map(Complaint::getEnterpriseId)
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

    /**
     * 加载监管员名称
     * @param complaints 投诉列表
     * @return 监管员名称Map
     */
    private Map<Long, String> loadRegulatorNames(List<Complaint> complaints) {
        if (complaints == null || complaints.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> regulatorIds = new LinkedHashSet<>();
        for (Complaint complaint : complaints) {
            if (complaint.getAssignedTo() != null) {
                regulatorIds.add(complaint.getAssignedTo());
            }
            if (complaint.getAssignedBy() != null) {
                regulatorIds.add(complaint.getAssignedBy());
            }
            if (complaint.getAcceptedBy() != null) {
                regulatorIds.add(complaint.getAcceptedBy());
            }
            if (complaint.getProcessedBy() != null) {
                regulatorIds.add(complaint.getProcessedBy());
            }
            if (complaint.getRejectedBy() != null) {
                regulatorIds.add(complaint.getRejectedBy());
            }
        }
        if (regulatorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return foodRegulatorMapper.selectBatchIds(regulatorIds)
            .stream()
            .filter(regulator -> !isDeleted(regulator.getDeleted()))
            .collect(Collectors.toMap(FoodRegulator::getId, FoodRegulator::getName, (a, b) -> a));
    }
    
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }
    
    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 序列化图片URL
     * @param imageUrls 图片URL列表
     * @return 序列化后的图片URL
     */
    private String serializeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return null;
        }
        List<String> normalized = imageUrls.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .limit(MAX_IMAGE_COUNT)
            .toList();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 解析图片URL
     * @param value 图片URL
     * @return 图片URL列表
     */
    private List<String> parseImageUrls(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }
    /**
     * 是否删除
     * @param deleted 删除
     * @return 是否删除
     */
    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}
