package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mortal.regulation.client.UserServiceClient;
import com.mortal.regulation.common.enums.FileBizType;
import com.mortal.regulation.config.MinioProperties;
import com.mortal.regulation.dto.EnterpriseProfileAttachmentDTO;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.entity.EnterpriseProfileAttachment;
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
import com.mortal.regulation.mapper.EnterpriseProfileAttachmentMapper;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.mapper.FoodRegulatorMapper;
import com.mortal.regulation.mapper.FoodRegulatorRegionMapper;
import com.mortal.regulation.service.EnterpriseKeyReasonService;
import com.mortal.regulation.service.EnterpriseProfileService;
import com.mortal.regulation.support.EnterpriseMasterCacheService;
import com.mortal.regulation.support.EnterprisePublicCacheService;
import com.mortal.regulation.support.RegulatorMasterCacheService;
import com.mortal.regulation.vo.BatchActionResult;
import com.mortal.regulation.vo.EnterpriseProfileVO;
import com.mortal.regulation.vo.EnterpriseProfileAttachmentVO;
import com.mortal.regulation.vo.PublicEnterpriseDetailVO;
import com.mortal.regulation.vo.PublicEnterpriseVO;
import com.mortal.regulation.vo.RegionVO;
import java.nio.charset.StandardCharsets;
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
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

@Service
public class EnterpriseProfileServiceImpl implements EnterpriseProfileService {

    private static final String STATUS_NORMAL = "NORMAL";
    private static final String APPROVAL_PENDING = "PENDING";
    private static final String APPROVAL_APPROVED = "APPROVED";
    private static final String APPROVAL_REJECTED = "REJECTED";
    private static final String ENTERPRISE_PROFILE_PREFIX = FileBizType.ENTERPRISE_PROFILE.prefix();

    private final FoodEnterpriseMapper foodEnterpriseMapper;
    private final EnterpriseProfileAttachmentMapper enterpriseProfileAttachmentMapper;
    private final AddrLocationMapper addrLocationMapper;
    private final AddrRegionMapper addrRegionMapper;
    private final FoodRegulatorMapper foodRegulatorMapper;
    private final FoodRegulatorRegionMapper foodRegulatorRegionMapper;
    private final UserServiceClient userServiceClient;
    private final EnterpriseKeyReasonService enterpriseKeyReasonService;
    private final MinioProperties minioProperties;
    private final EnterpriseMasterCacheService enterpriseMasterCacheService;
    private final EnterprisePublicCacheService enterprisePublicCacheService;
    private final RegulatorMasterCacheService regulatorMasterCacheService;

    public EnterpriseProfileServiceImpl(FoodEnterpriseMapper foodEnterpriseMapper,
                                        EnterpriseProfileAttachmentMapper enterpriseProfileAttachmentMapper,
                                        AddrLocationMapper addrLocationMapper,
                                        AddrRegionMapper addrRegionMapper,
                                        FoodRegulatorMapper foodRegulatorMapper,
                                        FoodRegulatorRegionMapper foodRegulatorRegionMapper,
                                        UserServiceClient userServiceClient,
                                        EnterpriseKeyReasonService enterpriseKeyReasonService,
                                        MinioProperties minioProperties,
                                        EnterpriseMasterCacheService enterpriseMasterCacheService,
                                        EnterprisePublicCacheService enterprisePublicCacheService,
                                        RegulatorMasterCacheService regulatorMasterCacheService) {
        this.foodEnterpriseMapper = foodEnterpriseMapper;
        this.enterpriseProfileAttachmentMapper = enterpriseProfileAttachmentMapper;
        this.addrLocationMapper = addrLocationMapper;
        this.addrRegionMapper = addrRegionMapper;
        this.foodRegulatorMapper = foodRegulatorMapper;
        this.foodRegulatorRegionMapper = foodRegulatorRegionMapper;
        this.userServiceClient = userServiceClient;
        this.enterpriseKeyReasonService = enterpriseKeyReasonService;
        this.minioProperties = minioProperties;
        this.enterpriseMasterCacheService = enterpriseMasterCacheService;
        this.enterprisePublicCacheService = enterprisePublicCacheService;
        this.regulatorMasterCacheService = regulatorMasterCacheService;
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
        enterprise.setCreditCode(normalizeCreditCode(dto.getCreditCode()));
        enterprise.setLegalRepresentative(normalizeOptionalText(dto.getLegalRepresentative()));
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

        saveProfileAttachments(enterprise.getId(), userId, dto.getAttachments());
        evictEnterpriseMasterCaches(enterprise);
        EnterpriseProfileVO vo = toVO(enterprise, location.getDetail(), resolveRegionPath(enterprise.getRegionId()));
        attachAttachments(vo, listAttachmentVOs(enterprise.getId()));
        return vo;
    }

    @Override
    public EnterpriseProfileVO getProfile(Long userId) {
        FoodEnterprise enterprise = findEnterpriseByUserId(userId);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            return null;
        }
        EnterpriseProfileVO vo = toVO(
            enterprise,
            resolveAddressDetail(enterprise.getAddressId()),
            resolveRegionPath(enterprise.getRegionId())
        );
        attachAttachments(vo, listAttachmentVOs(enterprise.getId()));
        attachKeyReasons(vo, enterprise.getId());
        return vo;
    }

    @Override
    public EnterpriseProfileVO getById(Long enterpriseId) {
        FoodEnterprise enterprise = foodEnterpriseMapper.selectById(enterpriseId);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            return null;
        }
        EnterpriseProfileVO vo = toVO(
            enterprise,
            resolveAddressDetail(enterprise.getAddressId()),
            resolveRegionPath(enterprise.getRegionId())
        );
        attachAttachments(vo, listAttachmentVOs(enterprise.getId()));
        attachKeyReasons(vo, enterprise.getId());
        return vo;
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
        String queryHash = buildPublicEnterpriseQueryHash(enterpriseName, page, size);
        return enterprisePublicCacheService.getList(queryHash, () -> loadPublicEnterpriseList(enterpriseName, page, size));
    }

    /**
     * 鑾峰彇鍏紬浼佷笟璇︽儏
     * @param enterpriseId 浼佷笟ID
     * @return 鍏紬浼佷笟璇︽儏VO
     */
    @Override
    public PublicEnterpriseDetailVO getPublicById(Long enterpriseId) {
        if (enterpriseId == null) {
            return null;
        }
        return enterprisePublicCacheService.getDetail(enterpriseId, () -> loadPublicEnterpriseDetail(enterpriseId));
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
        Map<Long, List<EnterpriseProfileAttachmentVO>> attachmentMap = loadAttachmentMap(enterprises);
        List<EnterpriseProfileVO> records = enterprises.stream()
            .map(enterprise -> toVO(
                enterprise,
                addressMap.get(enterprise.getAddressId()),
                regionPathMap.getOrDefault(enterprise.getRegionId(), List.of())))
            .toList();
        records.forEach(vo -> attachAttachments(vo, attachmentMap.get(vo.getId())));
        return PageResult.of(records, pageInfo.getTotal(), page, size);
    }

    @Override
    public List<EnterpriseProfileVO> listPending() {
        List<FoodEnterprise> enterprises = foodEnterpriseMapper.selectList(new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getApprovalStatus, APPROVAL_PENDING)
            .eq(FoodEnterprise::getDeleted, 0));
        Map<Long, String> addressMap = loadAddressDetails(enterprises);
        Map<Long, List<RegionVO>> regionPathMap = loadRegionPaths(enterprises);
        Map<Long, List<EnterpriseProfileAttachmentVO>> attachmentMap = loadAttachmentMap(enterprises);
        return enterprises.stream()
            .map(enterprise -> toVO(
                enterprise,
                addressMap.get(enterprise.getAddressId()),
                regionPathMap.getOrDefault(enterprise.getRegionId(), List.of())))
            .peek(vo -> attachAttachments(vo, attachmentMap.get(vo.getId())))
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
        Map<Long, List<EnterpriseProfileAttachmentVO>> attachmentMap = loadAttachmentMap(enterprises);
        return enterprises.stream()
            .map(enterprise -> toVO(
                enterprise,
                addressMap.get(enterprise.getAddressId()),
                regionPathMap.getOrDefault(enterprise.getRegionId(), List.of())))
            .peek(vo -> attachAttachments(vo, attachmentMap.get(vo.getId())))
            .toList();
    }

    @Override
    public EnterpriseProfileVO approve(Long enterpriseId, Long operatorId, EnterpriseApprovalDTO dto) {
        FoodEnterprise enterprise = requireEnterprise(enterpriseId);
        applyApproval(enterprise, APPROVAL_APPROVED, operatorId, dto.getComment(), dto.getRegulatorName());
        evictEnterpriseMasterCaches(enterprise);
        EnterpriseProfileVO vo = toVO(
            enterprise,
            resolveAddressDetail(enterprise.getAddressId()),
            resolveRegionPath(enterprise.getRegionId())
        );
        attachAttachments(vo, listAttachmentVOs(enterprise.getId()));
        return vo;
    }

    @Override
    public EnterpriseProfileVO reject(Long enterpriseId, Long operatorId, EnterpriseApprovalDTO dto) {
        FoodEnterprise enterprise = requireEnterprise(enterpriseId);
        applyApproval(enterprise, APPROVAL_REJECTED, operatorId, dto.getComment(), dto.getRegulatorName());
        evictEnterpriseMasterCaches(enterprise);
        EnterpriseProfileVO vo = toVO(
            enterprise,
            resolveAddressDetail(enterprise.getAddressId()),
            resolveRegionPath(enterprise.getRegionId())
        );
        attachAttachments(vo, listAttachmentVOs(enterprise.getId()));
        return vo;
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
        markAttachmentsDeleted(enterprise.getId());
        evictEnterpriseMasterCaches(enterprise);
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

    private void saveProfileAttachments(Long enterpriseId, Long userId, List<EnterpriseProfileAttachmentDTO> attachments) {
        if (enterpriseId == null || attachments == null) {
            return;
        }
        List<EnterpriseProfileAttachmentDTO> cleaned = attachments.stream()
            .filter(Objects::nonNull)
            .filter(item -> StringUtils.hasText(item.getType()) && StringUtils.hasText(item.getUrl()))
            .toList();
        cleaned.forEach(item -> validateAttachmentOwnership(userId, item.getUrl()));
        markAttachmentsDeleted(enterpriseId);
        for (EnterpriseProfileAttachmentDTO item : cleaned) {
            EnterpriseProfileAttachment attachment = new EnterpriseProfileAttachment();
            attachment.setEnterpriseId(enterpriseId);
            attachment.setAttachmentType(item.getType().trim());
            attachment.setAttachmentName(normalizeOptionalText(
                StringUtils.hasText(item.getName()) ? item.getName() : item.getLabel()
            ));
            attachment.setAttachmentUrl(item.getUrl().trim());
            attachment.setUploadedBy(userId);
            attachment.setUploadedAt(LocalDateTime.now());
            attachment.setDeleted(0);
            enterpriseProfileAttachmentMapper.insert(attachment);
        }
    }

    private void validateAttachmentOwnership(Long userId, String attachmentUrl) {
        if (userId == null) {
            throw new IllegalArgumentException("attachment uploader required");
        }
        String objectKey = extractObjectKey(attachmentUrl);
        if (!StringUtils.hasText(objectKey)) {
            throw new IllegalArgumentException("invalid enterprise profile attachment url");
        }
        String[] segments = objectKey.split("/");
        if (segments.length < 6) {
            throw new IllegalArgumentException("invalid enterprise profile attachment path");
        }
        if (!ENTERPRISE_PROFILE_PREFIX.equals(segments[0])) {
            throw new IllegalArgumentException("attachment must use enterprise profile prefix");
        }
        if (!segments[1].matches("\\d{4}") || !segments[2].matches("\\d{2}") || !segments[3].matches("\\d{2}")) {
            throw new IllegalArgumentException("invalid enterprise profile attachment date path");
        }
        if (!String.valueOf(userId).equals(segments[4])) {
            throw new IllegalArgumentException("attachment does not belong to current enterprise user");
        }
    }

    private String extractObjectKey(String attachmentUrl) {
        if (!StringUtils.hasText(attachmentUrl) || minioProperties == null || !StringUtils.hasText(minioProperties.getBucket())) {
            return null;
        }
        String marker = "/" + minioProperties.getBucket() + "/";
        int index = attachmentUrl.indexOf(marker);
        if (index < 0) {
            return null;
        }
        String objectKey = attachmentUrl.substring(index + marker.length()).trim();
        return StringUtils.hasText(objectKey) ? objectKey : null;
    }

    private void markAttachmentsDeleted(Long enterpriseId) {
        if (enterpriseId == null) {
            return;
        }
        List<EnterpriseProfileAttachment> attachments = enterpriseProfileAttachmentMapper.selectList(
            new LambdaQueryWrapper<EnterpriseProfileAttachment>()
                .eq(EnterpriseProfileAttachment::getEnterpriseId, enterpriseId)
                .eq(EnterpriseProfileAttachment::getDeleted, 0)
        );
        for (EnterpriseProfileAttachment attachment : attachments) {
            attachment.setDeleted(1);
            enterpriseProfileAttachmentMapper.updateById(attachment);
        }
    }

    private List<EnterpriseProfileAttachmentVO> listAttachmentVOs(Long enterpriseId) {
        if (enterpriseId == null) {
            return List.of();
        }
        return enterpriseProfileAttachmentMapper.selectList(
                new LambdaQueryWrapper<EnterpriseProfileAttachment>()
                    .eq(EnterpriseProfileAttachment::getEnterpriseId, enterpriseId)
                    .eq(EnterpriseProfileAttachment::getDeleted, 0)
                    .orderByAsc(EnterpriseProfileAttachment::getAttachmentType)
                    .orderByAsc(EnterpriseProfileAttachment::getId))
            .stream()
            .map(this::toAttachmentVO)
            .toList();
    }

    private Map<Long, List<EnterpriseProfileAttachmentVO>> loadAttachmentMap(List<FoodEnterprise> enterprises) {
        if (enterprises == null || enterprises.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> enterpriseIds = enterprises.stream()
            .map(FoodEnterprise::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (enterpriseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return enterpriseProfileAttachmentMapper.selectList(
                new LambdaQueryWrapper<EnterpriseProfileAttachment>()
                    .in(EnterpriseProfileAttachment::getEnterpriseId, enterpriseIds)
                    .eq(EnterpriseProfileAttachment::getDeleted, 0)
                    .orderByAsc(EnterpriseProfileAttachment::getAttachmentType)
                    .orderByAsc(EnterpriseProfileAttachment::getId))
            .stream()
            .collect(Collectors.groupingBy(
                EnterpriseProfileAttachment::getEnterpriseId,
                Collectors.mapping(this::toAttachmentVO, Collectors.toList())
            ));
    }

    private EnterpriseProfileAttachmentVO toAttachmentVO(EnterpriseProfileAttachment attachment) {
        EnterpriseProfileAttachmentVO vo = new EnterpriseProfileAttachmentVO();
        vo.setId(attachment.getId());
        vo.setType(attachment.getAttachmentType());
        vo.setLabel(resolveAttachmentLabel(attachment.getAttachmentType()));
        vo.setName(attachment.getAttachmentName());
        vo.setUrl(attachment.getAttachmentUrl());
        vo.setUploadedBy(attachment.getUploadedBy());
        vo.setUploadedAt(attachment.getUploadedAt());
        return vo;
    }

    private void attachAttachments(EnterpriseProfileVO vo, List<EnterpriseProfileAttachmentVO> attachments) {
        if (vo == null) {
            return;
        }
        vo.setAttachments(attachments == null ? List.of() : attachments);
    }

    private void attachAttachments(PublicEnterpriseDetailVO vo, List<EnterpriseProfileAttachmentVO> attachments) {
        if (vo == null) {
            return;
        }
        vo.setAttachments(attachments == null ? List.of() : attachments);
    }

    private String resolveAttachmentLabel(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        return switch (type.trim()) {
            case "businessLicense" -> "营业执照";
            case "foodPermit" -> "食品经营许可证";
            case "onsitePhoto" -> "经营场所照片";
            default -> type.trim();
        };
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
            evictEnterpriseMasterCaches(enterprise);
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
        // Batch-load region paths to avoid repeated queries in list views.
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

    private void evictEnterpriseMasterCaches(FoodEnterprise enterprise) {
        if (enterprise == null) {
            return;
        }
        enterpriseMasterCacheService.evict(enterprise.getId(), enterprise.getUserId());
        enterprisePublicCacheService.evict(enterprise.getId());
        regulatorMasterCacheService.bumpScopeEnterpriseVersion();
    }

    private PageResult<PublicEnterpriseVO> loadPublicEnterpriseList(String enterpriseName, int page, int size) {
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
        Map<Long, String> addressMap = loadAddressDetails(enterprises);
        List<PublicEnterpriseVO> records = enterprises.stream()
            .map(enterprise -> toPublicVO(
                enterprise,
                regionPathMap.get(enterprise.getRegionId()),
                addressMap.get(enterprise.getAddressId())))
            .toList();
        return PageResult.of(records, pageInfo.getTotal(), safePage, safeSize);
    }

    private PublicEnterpriseDetailVO loadPublicEnterpriseDetail(Long enterpriseId) {
        FoodEnterprise enterprise = foodEnterpriseMapper.selectById(enterpriseId);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            return null;
        }
        if (!APPROVAL_APPROVED.equalsIgnoreCase(enterprise.getApprovalStatus())) {
            return null;
        }
        PublicEnterpriseDetailVO vo = toPublicDetailVO(
            enterprise,
            resolveRegionPath(enterprise.getRegionId()),
            resolveAddressDetail(enterprise.getAddressId()));
        attachAttachments(vo, listAttachmentVOs(enterprise.getId()));
        attachKeyReasons(vo, enterprise.getId());
        return vo;
    }

    private String buildPublicEnterpriseQueryHash(String enterpriseName, int page, int size) {
        String raw = String.join("|",
            StringUtils.hasText(enterpriseName) ? enterpriseName.trim() : "",
            String.valueOf(Math.max(1, page)),
            String.valueOf(Math.max(1, Math.min(size, 50)))
        );
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private List<RegionVO> resolveRegionPath(Long regionId) {
        if (regionId == null) {
            return List.of();
        }
        // Walk upward from the current region to build the full region path.
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

    private String normalizeCreditCode(String creditCode) {
        if (!StringUtils.hasText(creditCode)) {
            return null;
        }
        return creditCode.trim().toUpperCase();
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private EnterpriseProfileVO toVO(FoodEnterprise enterprise, String addressDetail, List<RegionVO> regionPath) {
        EnterpriseProfileVO vo = new EnterpriseProfileVO();
        vo.setId(enterprise.getId());
        vo.setUserId(enterprise.getUserId());
        vo.setEnterpriseName(enterprise.getEnterpriseName());
        vo.setLicenseNo(enterprise.getLicenseNo());
        vo.setCreditCode(enterprise.getCreditCode());
        vo.setLegalRepresentative(enterprise.getLegalRepresentative());
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
        // Return both the structured path and the flattened text for frontend display.
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
     * 杞崲涓哄叕鍏变紒涓氫俊鎭疺O
     * @param enterprise 浼佷笟
     * @param regionPath 琛屾斂鍖鸿矾寰?     * @return 鍏叡浼佷笟淇℃伅VO
     */
    private PublicEnterpriseVO toPublicVO(FoodEnterprise enterprise,
                                          List<RegionVO> regionPath,
                                          String addressDetail) {
        PublicEnterpriseVO vo = new PublicEnterpriseVO();
        vo.setId(enterprise.getId());
        vo.setEnterpriseName(enterprise.getEnterpriseName());
        vo.setCreditCode(enterprise.getCreditCode());
        vo.setRegionId(enterprise.getRegionId());
        vo.setRegionPathText(buildRegionPathText(regionPath));
        vo.setAddressDetail(addressDetail);
        vo.setStatus(enterprise.getStatus());
        vo.setApprovedTime(enterprise.getApprovedTime());
        vo.setUpdateTime(enterprise.getUpdateTime());
        return vo;
    }

    private PublicEnterpriseDetailVO toPublicDetailVO(FoodEnterprise enterprise,
                                                      List<RegionVO> regionPath,
                                                      String addressDetail) {
        PublicEnterpriseDetailVO vo = new PublicEnterpriseDetailVO();
        vo.setId(enterprise.getId());
        vo.setEnterpriseName(enterprise.getEnterpriseName());
        vo.setLicenseNo(enterprise.getLicenseNo());
        vo.setCreditCode(enterprise.getCreditCode());
        vo.setLegalRepresentative(enterprise.getLegalRepresentative());
        vo.setRegionId(enterprise.getRegionId());
        vo.setRegionPathText(buildRegionPathText(regionPath));
        vo.setAddressDetail(addressDetail);
        vo.setPrincipal(enterprise.getPrincipal());
        vo.setPrincipalPhoneMasked(maskPhone(enterprise.getPrincipalPhone()));
        vo.setRegulatorName(enterprise.getRegulatorName());
        vo.setStatus(enterprise.getStatus());
        vo.setApprovedTime(enterprise.getApprovedTime());
        vo.setUpdateTime(enterprise.getUpdateTime());
        return vo;
    }

    private void attachKeyReasons(EnterpriseProfileVO vo, Long enterpriseId) {
        if (vo == null) {
            return;
        }
        vo.setKeyReasons(enterpriseKeyReasonService.listRecentByEnterpriseId(enterpriseId, 3));
    }

    private void attachKeyReasons(PublicEnterpriseDetailVO vo, Long enterpriseId) {
        if (vo == null) {
            return;
        }
        vo.setKeyReasons(enterpriseKeyReasonService.listRecentByEnterpriseId(enterpriseId, 3));
    }

    /**
     * 鎵嬫満鍙疯劚鏁?     * @param phone 鎵嬫満鍙?     * @return 鑴辨晱鍚庣殑鎵嬫満鍙?     */
    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        String text = phone.trim();
        if (text.length() < 7) {
            return text;
        }
        return text.substring(0, 3) + "****" + text.substring(text.length() - 4);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }
}
