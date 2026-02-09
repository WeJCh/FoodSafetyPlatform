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

/**
 * 投诉服务实现
 */
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

    /**
     * 提交投诉
     * @param dto 投诉提交DTO
     * @return 投诉跟踪VO
     */
    @Override
    public ComplaintTrackVO submitPublic(ComplaintSubmitDTO dto) {
        FoodEnterprise enterprise = requireEnterprise(dto.getEnterpriseId());
        Complaint complaint = new Complaint();
        complaint.setComplaintNo(generateComplaintNo());
        complaint.setComplainantName(trim(dto.getComplainantName()));
        complaint.setContact(trim(dto.getContact()));
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
     * 跟踪投诉
     * @param complaintNo 投诉编号
     * @param contact 联系方式
     * @return 投诉跟踪VO
     */
    @Override
    public ComplaintTrackVO track(String complaintNo, String contact) {
        // 关键校验：投诉编号不能为空
        if (!StringUtils.hasText(complaintNo)) {
            throw new IllegalArgumentException("complaintNo required");
        }
        // 关键校验：查询投诉
        Complaint complaint = complaintMapper.selectOne(new LambdaQueryWrapper<Complaint>()
            .eq(Complaint::getComplaintNo, complaintNo.trim())
            .eq(Complaint::getDeleted, 0));
        // 关键校验：投诉不存在
        if (complaint == null) {
            throw new IllegalArgumentException("complaint not found");
        }
        // 关键校验：匿名查询仅允许通过联系方式进行核验
        if (StringUtils.hasText(complaint.getContact())
            && !Objects.equals(normalize(contact), normalize(complaint.getContact()))) {
            throw new IllegalArgumentException("complaint not found");
        }
        return toTrackVO(complaint);
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
        // 关键校验：查询条件
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<Complaint>()
            .eq(Complaint::getDeleted, 0);
        // 关键校验：状态
        if (StringUtils.hasText(status)) {
            wrapper.eq(Complaint::getStatus, normalize(status));
        }
        // 关键校验：执法人员仅能查看分配给自己的投诉
        if (ROLE_ENFORCER.equalsIgnoreCase(regulator.getRoleType())) {
            wrapper.eq(Complaint::getAssignedTo, regulator.getId());
        }
        // 关键校验：企业名称
        List<Long> enterpriseIds = resolveEnterpriseIdsByName(enterpriseName);
        // 关键校验：只能访问辖区内企业投诉
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
        // 关键校验：被指派去处理投诉的执行人姓名
        List<Long> assignedToIds = resolveRegulatorIdsByName(assignedToName);
        if (StringUtils.hasText(assignedToName) && assignedToIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        if (assignedToIds != null) {
            wrapper.in(Complaint::getAssignedTo, assignedToIds);
        }
        // 关键校验：指派监管员名称
        List<Long> assignedByIds = resolveRegulatorIdsByName(assignedByName);
        if (StringUtils.hasText(assignedByName) && assignedByIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        if (assignedByIds != null) {
            wrapper.in(Complaint::getAssignedBy, assignedByIds);
        }
        // 关键校验：排序
        wrapper.orderByDesc(Complaint::getUpdateTime);
        Page<Complaint> pageInfo = complaintMapper.selectPage(new Page<>(page, size), wrapper);
        // 关键校验：转换为VO
        List<Complaint> complaints = pageInfo.getRecords();
        Map<Long, String> enterpriseNames = loadEnterpriseNames(complaints);
        Map<Long, String> regulatorNames = loadRegulatorNames(complaints);
        List<ComplaintVO> records = complaints.stream()
            .map(complaint -> toVO(complaint, enterpriseNames, regulatorNames))
            .toList();
        // 关键校验：返回结果
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
        // 关键校验：只能访问辖区内企业投诉
        if (!isComplaintInRegion(regulator, complaint.getEnterpriseId())) {
            throw new IllegalArgumentException("complaint not in regulator region");
        }
        // 关键校验：监管员角色必须是执行员
        if (ROLE_ENFORCER.equalsIgnoreCase(regulator.getRoleType())) {
            if (!Objects.equals(complaint.getAssignedTo(), regulator.getId())) {
                throw new IllegalArgumentException("complaint not assigned to you");
            }
        }
        ComplaintDetailVO detail = new ComplaintDetailVO();
        detail.setComplaint(toVOWithNames(complaint));
        // 关键展示：投诉详情返回企业信息
        EnterpriseProfileVO enterprise = enterpriseProfileService.getById(complaint.getEnterpriseId());
        detail.setEnterprise(enterprise);
        // 关键展示：投诉处理记录明细
        detail.setHandles(loadHandleDetails(complaint.getId()));
        return detail;
    }

    public ComplaintVO accept(Long operatorUserId, Long complaintId) {
        FoodRegulator regulator = requireRegulator(operatorUserId);
        requireRole(regulator, ROLE_ADMIN);
        // 关键校验：查询投诉
        Complaint complaint = requireComplaint(complaintId);
        // 关键校验：状态流转
        transitionComplaint(complaint, ComplaintStatus.PENDING);
        complaint.setUpdateTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
        return toVOWithNames(complaint);
    }

    /**
     * 指派投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @param dto 投诉指派DTO
     * @return 投诉VO
     */
    @Override
    public ComplaintVO assign(Long operatorUserId, Long complaintId, ComplaintAssignDTO dto) {
        FoodRegulator regulator = requireRegulator(operatorUserId);
        // 关键校验：监管员角色必须是管理员
        requireRole(regulator, ROLE_ADMIN);
        Complaint complaint = requireComplaint(complaintId);
        // 关键校验：投诉状态必须是待指派或已指派
        if (!ComplaintStatus.PENDING.equals(complaint.getStatus())
            && !ComplaintStatus.ASSIGNED.equals(complaint.getStatus())) {
            throw new IllegalArgumentException("complaint not ready for assignment");
        }
        // 关键校验：查询指派监管员
        FoodRegulator assignee = foodRegulatorMapper.selectById(dto.getRegulatorId());
        if (assignee == null || isDeleted(assignee.getDeleted())) {
            throw new IllegalArgumentException("assignee not found");
        }
        // 关键校验：指派监管员必须是执行员
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
    /**
     * 开始处理投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @return 投诉VO
     */
    @Override
    public ComplaintVO startProcess(Long operatorUserId, Long complaintId) {
        FoodRegulator regulator = requireRegulator(operatorUserId);
        // 关键校验：监管员角色必须是执行员
        requireRole(regulator, ROLE_ENFORCER);
        Complaint complaint = requireComplaint(complaintId);
        // 关键校验：投诉必须指派给当前监管员
        if (!Objects.equals(complaint.getAssignedTo(), regulator.getId())) {
            throw new IllegalArgumentException("complaint not assigned to you");
        }
        // 关键校验：状态流转
        transitionComplaint(complaint, ComplaintStatus.PROCESSING);
        complaint.setUpdateTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
        return toVOWithNames(complaint);
    }
    /**
     * 处理投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @param dto 投诉处理DTO
     * @return 投诉VO
     */
    @Override
    public ComplaintVO handle(Long operatorUserId, Long complaintId, ComplaintHandleDTO dto) {
        FoodRegulator regulator = requireRegulator(operatorUserId);
        // 关键校验：监管员角色必须是执行员
        requireRole(regulator, ROLE_ENFORCER);
        Complaint complaint = requireComplaint(complaintId);
        // 关键校验：投诉必须指派给当前监管员
        if (!Objects.equals(complaint.getAssignedTo(), regulator.getId())) {
            throw new IllegalArgumentException("complaint not assigned to you");
        }
        // 关键校验：投诉状态必须是处理中
        if (!ComplaintStatus.PROCESSING.equals(complaint.getStatus())) {
            throw new IllegalArgumentException("complaint not in processing");
        }
        ComplaintHandle handle = new ComplaintHandle();
        handle.setComplaintId(complaint.getId());
        handle.setHandlerId(regulator.getId());
        handle.setHandleResult(dto.getHandleResult().trim());
        handle.setHandleTime(LocalDateTime.now());
        handle.setCreateTime(LocalDateTime.now());
        handle.setUpdateTime(LocalDateTime.now());
        handle.setDeleted(0);
        complaintHandleMapper.insert(handle);
        transitionComplaint(complaint, ComplaintStatus.FEEDBACKED);
        complaint.setUpdateTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
        return toVOWithNames(complaint);
    }
    /**
     * 状态流转
     * @param complaint 投诉
     * @param target 目标状态
     */
    private void transitionComplaint(Complaint complaint, ComplaintStatus target) {
        // 关键校验：统一通过状态机校验，防止非法跳转
        StatusTransitionValidator.validateComplaintTransition(complaint.getStatus(), target);
        complaint.setStatus(target);
    }
    /**
     * 查询投诉
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
     * 查询企业
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
     * 查询监管员
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
     * 查询角色
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
     * @return 投诉VO
     */
    private ComplaintVO toVOWithNames(Complaint complaint) {
        Map<Long, String> enterpriseNames = loadEnterpriseNames(List.of(complaint));
        Map<Long, String> regulatorNames = loadRegulatorNames(List.of(complaint));
        return toVO(complaint, enterpriseNames, regulatorNames);
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
        vo.setStatus(complaint.getStatus());
        vo.setAssignedTo(complaint.getAssignedTo());
        vo.setAssignedToName(regulatorNames.get(complaint.getAssignedTo()));
        vo.setAssignedBy(complaint.getAssignedBy());
        vo.setAssignedByName(regulatorNames.get(complaint.getAssignedBy()));
        vo.setAssignedTime(complaint.getAssignedTime());
        vo.setCreateTime(complaint.getCreateTime());
        vo.setUpdateTime(complaint.getUpdateTime());
        return vo;
    }
    /**
     * 转换为跟踪VO
     * @param complaint 投诉
     * @return 跟踪VO
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
     * 加载投诉处理记录
     * @param complaintId 投诉ID
     * @return 投诉处理记录
     */
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
     * 查询企业ID
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
     * 查询监管员ID
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
     * 查询辖区内企业ID
     * @param regulator 监管员
     * @return 辖区内企业ID列表
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
     * 查询投诉是否在辖区内
     * @param regulator 监管员
     * @param enterpriseId 企业ID
     * @return 是否在辖区内
     */
    private boolean isComplaintInRegion(FoodRegulator regulator, Long enterpriseId) {
        if (regulator == null || enterpriseId == null) {
            return false;
        }
        List<Long> enterpriseIds = resolveEnterpriseIdsByRegion(regulator);
        return enterpriseIds.contains(enterpriseId);
    }
    /**
     * 查询监管员辖区ID
     * @param regulatorId 监管员ID
     * @return 监管员辖区ID列表
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
     * 收集辖区ID
     * @param rootIds 根ID列表
     * @return 辖区ID列表
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
        }
        if (regulatorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return foodRegulatorMapper.selectBatchIds(regulatorIds)
            .stream()
            .filter(regulator -> !isDeleted(regulator.getDeleted()))
            .collect(Collectors.toMap(FoodRegulator::getId, FoodRegulator::getName, (a, b) -> a));
    }
    /**
     * 标准化
     * @param value 值
     * @return 标准化值
     */
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }
    /**
     * 修剪
     * @param value 值
     * @return 修剪值
     */
    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 中文注释：将图片地址列表序列化为 JSON，便于存库。
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
     * 中文注释：将存库的 JSON 字段还原为前端需要的图片地址列表。
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
     * @param deleted 删除状态
     * @return 是否删除
     */
    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}
