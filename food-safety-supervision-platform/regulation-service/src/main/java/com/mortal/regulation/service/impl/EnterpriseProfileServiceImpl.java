package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mortal.regulation.client.UserServiceClient;
import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.dto.EnterpriseApprovalBatchDTO;
import com.mortal.regulation.dto.EnterpriseApprovalDTO;
import com.mortal.regulation.dto.EnterpriseProfileDTO;
import com.mortal.regulation.entity.AddrLocation;
import com.mortal.regulation.entity.AddrRegion;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.entity.FoodRegulator;
import com.mortal.regulation.entity.FoodRegulatorRegion;
import com.mortal.regulation.mapper.AddrLocationMapper;
import com.mortal.regulation.mapper.AddrRegionMapper;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.mapper.FoodRegulatorMapper;
import com.mortal.regulation.mapper.FoodRegulatorRegionMapper;
import com.mortal.regulation.service.EnterpriseProfileService;
import com.mortal.regulation.vo.BatchActionResult;
import com.mortal.regulation.vo.EnterpriseProfileVO;
import com.mortal.regulation.vo.PublicEnterpriseVO;
import com.mortal.regulation.vo.RegionVO;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EnterpriseProfileServiceImpl implements EnterpriseProfileService {

    private static final String STATUS_NORMAL = "NORMAL";
    private static final String APPROVAL_PENDING = "PENDING";
    private static final String APPROVAL_APPROVED = "APPROVED";
    private static final String APPROVAL_REJECTED = "REJECTED";

    private final FoodEnterpriseMapper foodEnterpriseMapper;
    private final AddrLocationMapper addrLocationMapper;
    private final AddrRegionMapper addrRegionMapper;
    private final FoodRegulatorMapper foodRegulatorMapper;
    private final FoodRegulatorRegionMapper foodRegulatorRegionMapper;
    private final UserServiceClient userServiceClient;

    public EnterpriseProfileServiceImpl(FoodEnterpriseMapper foodEnterpriseMapper,
                                        AddrLocationMapper addrLocationMapper,
                                        AddrRegionMapper addrRegionMapper,
                                        FoodRegulatorMapper foodRegulatorMapper,
                                        FoodRegulatorRegionMapper foodRegulatorRegionMapper,
                                        UserServiceClient userServiceClient) {
        this.foodEnterpriseMapper = foodEnterpriseMapper;
        this.addrLocationMapper = addrLocationMapper;
        this.addrRegionMapper = addrRegionMapper;
        this.foodRegulatorMapper = foodRegulatorMapper;
        this.foodRegulatorRegionMapper = foodRegulatorRegionMapper;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public EnterpriseProfileVO submitProfile(Long userId, EnterpriseProfileDTO dto) {
        FoodEnterprise enterprise = findEnterpriseByUserId(userId);
        if (enterprise == null) {
            enterprise = new FoodEnterprise();
            enterprise.setUserId(userId);
            enterprise.setStatus(STATUS_NORMAL);
            enterprise.setCreateTime(LocalDateTime.now());
        }
        requireRegion(dto.getRegionId());
        AddrLocation location = upsertLocation(enterprise.getAddressId(), dto.getRegionId(), dto.getAddressDetail());
        enterprise.setEnterpriseName(dto.getEnterpriseName());
        enterprise.setLicenseNo(dto.getLicenseNo());
        enterprise.setRegionId(dto.getRegionId());
        enterprise.setAddressId(location.getId());
        enterprise.setPrincipal(dto.getPrincipal());
        enterprise.setPrincipalPhone(dto.getPrincipalPhone());
        enterprise.setApprovalStatus(APPROVAL_PENDING);
        enterprise.setApprovalComment(null);
        enterprise.setApprovedBy(null);
        enterprise.setApprovedTime(null);
        enterprise.setUpdateTime(LocalDateTime.now());
        if (enterprise.getDeleted() == null) {
            enterprise.setDeleted(0);
        }

        if (enterprise.getId() == null) {
            foodEnterpriseMapper.insert(enterprise);
        } else {
            foodEnterpriseMapper.updateById(enterprise);
        }

        return toVO(enterprise, location.getDetail(), resolveRegionPath(enterprise.getRegionId()));
    }

    @Override
    public EnterpriseProfileVO getProfile(Long userId) {
        FoodEnterprise enterprise = findEnterpriseByUserId(userId);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            return null;
        }
        return toVO(enterprise, resolveAddressDetail(enterprise.getAddressId()), resolveRegionPath(enterprise.getRegionId()));
    }

    @Override
    public EnterpriseProfileVO getById(Long enterpriseId) {
        FoodEnterprise enterprise = foodEnterpriseMapper.selectById(enterpriseId);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            return null;
        }
        return toVO(enterprise, resolveAddressDetail(enterprise.getAddressId()), resolveRegionPath(enterprise.getRegionId()));
    }

    @Override
    public PageResult<EnterpriseProfileVO> list(String enterpriseName,
                                                String status,
                                                String approvalStatus,
                                                int page,
                                                int size) {
        return listByRegionIds(enterpriseName, status, approvalStatus, page, size, null);
    }

    @Override
    public PageResult<EnterpriseProfileVO> listForRegulator(Long userId,
                                                            String enterpriseName,
                                                            String status,
                                                            String approvalStatus,
                                                            int page,
                                                            int size) {
        List<Long> regionIds = resolveRegulatorRegionIds(userId);
        if (regionIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        return listByRegionIds(enterpriseName, status, approvalStatus, page, size, regionIds);
    }

    @Override
    public PageResult<PublicEnterpriseVO> listPublic(String enterpriseName, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 50));
        int safePage = Math.max(1, page);
        var wrapper = new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getDeleted, 0)
            .eq(FoodEnterprise::getApprovalStatus, APPROVAL_APPROVED);
        if (StringUtils.hasText(enterpriseName)) {
            wrapper.like(FoodEnterprise::getEnterpriseName, enterpriseName.trim());
        }
        wrapper.orderByAsc(FoodEnterprise::getEnterpriseName);
        Page<FoodEnterprise> pageInfo = foodEnterpriseMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        List<FoodEnterprise> enterprises = pageInfo.getRecords();
        Map<Long, List<RegionVO>> regionPathMap = loadRegionPaths(enterprises);
        List<PublicEnterpriseVO> records = enterprises.stream()
            .map(enterprise -> toPublicVO(enterprise, regionPathMap.get(enterprise.getRegionId())))
            .toList();
        return PageResult.of(records, pageInfo.getTotal(), safePage, safeSize);
    }

    private PageResult<EnterpriseProfileVO> listByRegionIds(String enterpriseName,
                                                            String status,
                                                            String approvalStatus,
                                                            int page,
                                                            int size,
                                                            List<Long> regionIds) {
        var wrapper = new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getDeleted, 0);
        if (StringUtils.hasText(enterpriseName)) {
            wrapper.like(FoodEnterprise::getEnterpriseName, enterpriseName.trim());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(FoodEnterprise::getStatus, normalize(status));
        }
        if (StringUtils.hasText(approvalStatus)) {
            wrapper.eq(FoodEnterprise::getApprovalStatus, normalize(approvalStatus));
        }
        if (regionIds != null && !regionIds.isEmpty()) {
            wrapper.in(FoodEnterprise::getRegionId, regionIds);
        }
        wrapper.orderByDesc(FoodEnterprise::getUpdateTime);
        Page<FoodEnterprise> pageInfo = foodEnterpriseMapper.selectPage(new Page<>(page, size), wrapper);
        List<FoodEnterprise> enterprises = pageInfo.getRecords();
        Map<Long, String> addressMap = loadAddressDetails(enterprises);
        Map<Long, List<RegionVO>> regionPathMap = loadRegionPaths(enterprises);
        List<EnterpriseProfileVO> records = enterprises.stream()
            .map(enterprise -> toVO(
                enterprise,
                addressMap.get(enterprise.getAddressId()),
                regionPathMap.getOrDefault(enterprise.getRegionId(), List.of())))
            .toList();
        return PageResult.of(records, pageInfo.getTotal(), page, size);
    }

    @Override
    public List<EnterpriseProfileVO> listPending() {
        List<FoodEnterprise> enterprises = foodEnterpriseMapper.selectList(new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getApprovalStatus, APPROVAL_PENDING)
            .eq(FoodEnterprise::getDeleted, 0));
        Map<Long, String> addressMap = loadAddressDetails(enterprises);
        Map<Long, List<RegionVO>> regionPathMap = loadRegionPaths(enterprises);
        return enterprises.stream()
            .map(enterprise -> toVO(
                enterprise,
                addressMap.get(enterprise.getAddressId()),
                regionPathMap.getOrDefault(enterprise.getRegionId(), List.of())))
            .toList();
    }

    @Override
    public List<EnterpriseProfileVO> listPendingForRegulator(Long userId) {
        List<Long> regionIds = resolveRegulatorRegionIds(userId);
        if (regionIds.isEmpty()) {
            return List.of();
        }
        List<FoodEnterprise> enterprises = foodEnterpriseMapper.selectList(new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getApprovalStatus, APPROVAL_PENDING)
            .eq(FoodEnterprise::getDeleted, 0)
            .in(FoodEnterprise::getRegionId, regionIds));
        Map<Long, String> addressMap = loadAddressDetails(enterprises);
        Map<Long, List<RegionVO>> regionPathMap = loadRegionPaths(enterprises);
        return enterprises.stream()
            .map(enterprise -> toVO(
                enterprise,
                addressMap.get(enterprise.getAddressId()),
                regionPathMap.getOrDefault(enterprise.getRegionId(), List.of())))
            .toList();
    }

    @Override
    public EnterpriseProfileVO approve(Long enterpriseId, Long operatorId, EnterpriseApprovalDTO dto) {
        FoodEnterprise enterprise = requireEnterprise(enterpriseId);
        applyApproval(enterprise, APPROVAL_APPROVED, operatorId, dto.getComment(), dto.getRegulatorName());
        return toVO(enterprise, resolveAddressDetail(enterprise.getAddressId()), resolveRegionPath(enterprise.getRegionId()));
    }

    @Override
    public EnterpriseProfileVO reject(Long enterpriseId, Long operatorId, EnterpriseApprovalDTO dto) {
        FoodEnterprise enterprise = requireEnterprise(enterpriseId);
        applyApproval(enterprise, APPROVAL_REJECTED, operatorId, dto.getComment(), dto.getRegulatorName());
        return toVO(enterprise, resolveAddressDetail(enterprise.getAddressId()), resolveRegionPath(enterprise.getRegionId()));
    }

    @Override
    public BatchActionResult approveBatch(Long operatorId, EnterpriseApprovalBatchDTO dto) {
        return batchApply(dto.getIds(), operatorId, APPROVAL_APPROVED, dto.getComment(), dto.getRegulatorName());
    }

    @Override
    public BatchActionResult rejectBatch(Long operatorId, EnterpriseApprovalBatchDTO dto) {
        return batchApply(dto.getIds(), operatorId, APPROVAL_REJECTED, dto.getComment(), dto.getRegulatorName());
    }

    @Override
    public void deleteEnterprise(Long enterpriseId) {
        FoodEnterprise enterprise = requireEnterprise(enterpriseId);
        enterprise.setDeleted(1);
        enterprise.setUpdateTime(LocalDateTime.now());
        foodEnterpriseMapper.updateById(enterprise);
        markAddressDeleted(enterprise.getAddressId());
        if (enterprise.getUserId() != null) {
            userServiceClient.deleteUser(enterprise.getUserId());
        }
    }

    @Override
    public void deleteEnterpriseByUserId(Long userId) {
        FoodEnterprise enterprise = findEnterpriseByUserId(userId);
        if (enterprise == null) {
            throw new IllegalArgumentException("enterprise not found");
        }
        deleteEnterprise(enterprise.getId());
    }

    private FoodEnterprise findEnterpriseByUserId(Long userId) {
        return foodEnterpriseMapper.selectOne(new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getUserId, userId)
            .eq(FoodEnterprise::getDeleted, 0));
    }

    private FoodEnterprise requireEnterprise(Long enterpriseId) {
        FoodEnterprise enterprise = foodEnterpriseMapper.selectById(enterpriseId);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            throw new IllegalArgumentException("enterprise not found");
        }
        return enterprise;
    }

    private void requireRegion(Long regionId) {
        if (regionId == null) {
            throw new IllegalArgumentException("regionId required");
        }
        AddrRegion region = addrRegionMapper.selectById(regionId);
        if (region == null || isDeleted(region.getDeleted())) {
            throw new IllegalArgumentException("region not found");
        }
    }

    private List<Long> resolveRegulatorRegionIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        FoodRegulator regulator = foodRegulatorMapper.selectOne(new LambdaQueryWrapper<FoodRegulator>()
            .eq(FoodRegulator::getUserId, userId)
            .eq(FoodRegulator::getDeleted, 0));
        if (regulator == null) {
            return List.of();
        }
        List<Long> directRegionIds = foodRegulatorRegionMapper.selectList(new LambdaQueryWrapper<FoodRegulatorRegion>()
                .eq(FoodRegulatorRegion::getRegulatorId, regulator.getId())
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

    private AddrLocation upsertLocation(Long addressId, Long regionId, String detail) {
        String cleanedDetail = StringUtils.hasText(detail) ? detail.trim() : detail;
        if (addressId != null) {
            AddrLocation location = addrLocationMapper.selectById(addressId);
            if (location != null && !isDeleted(location.getDeleted())) {
                location.setRegionId(regionId);
                location.setDetail(cleanedDetail);
                addrLocationMapper.updateById(location);
                return location;
            }
        }
        AddrLocation location = new AddrLocation();
        location.setRegionId(regionId);
        location.setDetail(cleanedDetail);
        location.setDeleted(0);
        addrLocationMapper.insert(location);
        return location;
    }

    private String resolveAddressDetail(Long addressId) {
        if (addressId == null) {
            return null;
        }
        AddrLocation location = addrLocationMapper.selectById(addressId);
        if (location == null || isDeleted(location.getDeleted())) {
            return null;
        }
        return location.getDetail();
    }

    private void markAddressDeleted(Long addressId) {
        if (addressId == null) {
            return;
        }
        AddrLocation location = addrLocationMapper.selectById(addressId);
        if (location == null || isDeleted(location.getDeleted())) {
            return;
        }
        location.setDeleted(1);
        addrLocationMapper.updateById(location);
    }

    private void applyApproval(FoodEnterprise enterprise,
                               String status,
                               Long operatorId,
                               String comment,
                               String regulatorName) {
        enterprise.setApprovalStatus(status);
        enterprise.setApprovalComment(comment);
        enterprise.setApprovedBy(operatorId);
        enterprise.setApprovedTime(LocalDateTime.now());
        if (StringUtils.hasText(regulatorName)) {
            enterprise.setRegulatorName(regulatorName.trim());
        }
        enterprise.setUpdateTime(LocalDateTime.now());
        foodEnterpriseMapper.updateById(enterprise);
    }

    private BatchActionResult batchApply(List<Long> ids,
                                         Long operatorId,
                                         String status,
                                         String comment,
                                         String regulatorName) {
        BatchActionResult result = new BatchActionResult();
        if (ids == null || ids.isEmpty()) {
            result.setSuccessCount(0);
            result.setFailedIds(List.of());
            return result;
        }
        List<Long> failed = new java.util.ArrayList<>();
        int successCount = 0;
        for (Long id : ids) {
            if (id == null) {
                failed.add(null);
                continue;
            }
            FoodEnterprise enterprise = foodEnterpriseMapper.selectById(id);
            if (enterprise == null
                || isDeleted(enterprise.getDeleted())
                || !APPROVAL_PENDING.equals(enterprise.getApprovalStatus())) {
                failed.add(id);
                continue;
            }
            applyApproval(enterprise, status, operatorId, comment, regulatorName);
            successCount += 1;
        }
        result.setSuccessCount(successCount);
        result.setFailedIds(failed);
        return result;
    }

    private Map<Long, String> loadAddressDetails(List<FoodEnterprise> enterprises) {
        if (enterprises == null || enterprises.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> addressIds = enterprises.stream()
            .map(FoodEnterprise::getAddressId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (addressIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return addrLocationMapper.selectBatchIds(addressIds)
            .stream()
            .filter(location -> !isDeleted(location.getDeleted()))
            .collect(Collectors.toMap(AddrLocation::getId, AddrLocation::getDetail, (a, b) -> a));
    }

    private Map<Long, List<RegionVO>> loadRegionPaths(List<FoodEnterprise> enterprises) {
        if (enterprises == null || enterprises.isEmpty()) {
            return Collections.emptyMap();
        }
        // 关键注释：批量预加载行政区路径，避免列表页重复查询
        List<Long> regionIds = enterprises.stream()
            .map(FoodEnterprise::getRegionId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (regionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<RegionVO>> result = new HashMap<>();
        for (Long regionId : regionIds) {
            result.put(regionId, resolveRegionPath(regionId));
        }
        return result;
    }

    private List<RegionVO> resolveRegionPath(Long regionId) {
        if (regionId == null) {
            return List.of();
        }
        // 关键注释：从当前行政区向上追溯，拼出完整省市区街道链路
        List<RegionVO> path = new ArrayList<>();
        Set<Long> visited = new LinkedHashSet<>();
        Long current = regionId;
        while (current != null && visited.add(current)) {
            AddrRegion region = addrRegionMapper.selectById(current);
            if (region == null || isDeleted(region.getDeleted())) {
                break;
            }
            path.add(toRegionVO(region));
            current = region.getParentId();
        }
        Collections.reverse(path);
        return path;
    }

    private RegionVO toRegionVO(AddrRegion region) {
        RegionVO vo = new RegionVO();
        vo.setId(region.getId());
        vo.setParentId(region.getParentId());
        vo.setName(region.getName());
        vo.setLevel(region.getLevel());
        return vo;
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }

    private EnterpriseProfileVO toVO(FoodEnterprise enterprise, String addressDetail, List<RegionVO> regionPath) {
        EnterpriseProfileVO vo = new EnterpriseProfileVO();
        vo.setId(enterprise.getId());
        vo.setUserId(enterprise.getUserId());
        vo.setEnterpriseName(enterprise.getEnterpriseName());
        vo.setLicenseNo(enterprise.getLicenseNo());
        vo.setRegionId(enterprise.getRegionId());
        vo.setAddressId(enterprise.getAddressId());
        vo.setAddressDetail(addressDetail);
        vo.setPrincipal(enterprise.getPrincipal());
        vo.setPrincipalPhone(enterprise.getPrincipalPhone());
        vo.setRegulatorName(enterprise.getRegulatorName());
        vo.setStatus(enterprise.getStatus());
        vo.setApprovalStatus(enterprise.getApprovalStatus());
        vo.setApprovalComment(enterprise.getApprovalComment());
        vo.setApprovedBy(enterprise.getApprovedBy());
        vo.setApprovedTime(enterprise.getApprovedTime());
        // 关键注释：前端展示需要“路径名称 + 级联回显”，同时返回文本与路径明细
        vo.setRegionPath(regionPath == null ? List.of() : regionPath);
        vo.setRegionPathText(buildRegionPathText(regionPath));
        vo.setCreateTime(enterprise.getCreateTime());
        vo.setUpdateTime(enterprise.getUpdateTime());
        return vo;
    }

    private String buildRegionPathText(List<RegionVO> regionPath) {
        if (regionPath == null || regionPath.isEmpty()) {
            return "";
        }
        return regionPath.stream()
            .map(RegionVO::getName)
            .filter(StringUtils::hasText)
            .collect(Collectors.joining("/"));
    }
    /**
     * 转换为公共企业信息VO
     * @param enterprise 企业
     * @param regionPath 行政区路径
     * @return 公共企业信息VO
     */
    private PublicEnterpriseVO toPublicVO(FoodEnterprise enterprise, List<RegionVO> regionPath) {
        PublicEnterpriseVO vo = new PublicEnterpriseVO();
        vo.setId(enterprise.getId());
        vo.setEnterpriseName(enterprise.getEnterpriseName());
        vo.setRegionId(enterprise.getRegionId());
        vo.setRegionPathText(buildRegionPathText(regionPath));
        return vo;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }
}
