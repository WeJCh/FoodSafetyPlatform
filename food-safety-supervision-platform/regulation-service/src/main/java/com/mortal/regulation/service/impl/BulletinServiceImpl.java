package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.dto.BulletinSaveDTO;
import com.mortal.regulation.entity.FoodRegulator;
import com.mortal.regulation.entity.PublicBulletin;
import com.mortal.regulation.mapper.FoodRegulatorMapper;
import com.mortal.regulation.mapper.PublicBulletinMapper;
import com.mortal.regulation.service.AuditLogService;
import com.mortal.regulation.service.BulletinService;
import com.mortal.regulation.support.AuditOperatorNameResolver;
import com.mortal.regulation.support.BulletinPublicCacheService;
import com.mortal.regulation.vo.BulletinDetailVO;
import com.mortal.regulation.vo.BulletinVO;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

@Service
public class BulletinServiceImpl implements BulletinService {

    private static final String ROLE_ADMIN = "REGULATOR_ADMIN";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_OFFLINE = "OFFLINE";
    private static final int MAX_PAGE_SIZE = 20;

    private final PublicBulletinMapper publicBulletinMapper;
    private final FoodRegulatorMapper foodRegulatorMapper;
    private final BulletinPublicCacheService bulletinPublicCacheService;
    private final AuditLogService auditLogService;
    private final AuditOperatorNameResolver auditOperatorNameResolver;

    public BulletinServiceImpl(PublicBulletinMapper publicBulletinMapper,
                               FoodRegulatorMapper foodRegulatorMapper,
                               BulletinPublicCacheService bulletinPublicCacheService,
                               AuditLogService auditLogService,
                               AuditOperatorNameResolver auditOperatorNameResolver) {
        this.publicBulletinMapper = publicBulletinMapper;
        this.foodRegulatorMapper = foodRegulatorMapper;
        this.bulletinPublicCacheService = bulletinPublicCacheService;
        this.auditLogService = auditLogService;
        this.auditOperatorNameResolver = auditOperatorNameResolver;
    }

    @Override
    public PageResult<BulletinVO> listAdmin(Long userId, String keyword, String category, String status, int page, int size) {
        requireAdmin(userId);
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        LambdaQueryWrapper<PublicBulletin> wrapper = new LambdaQueryWrapper<PublicBulletin>()
            .eq(PublicBulletin::getDeleted, 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(condition -> condition
                .like(PublicBulletin::getTitle, keyword.trim())
                .or()
                .like(PublicBulletin::getCategory, keyword.trim()));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(PublicBulletin::getCategory, normalizeCategory(category));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(PublicBulletin::getStatus, normalize(status));
        }
        wrapper.orderByDesc(PublicBulletin::getUpdateTime).orderByDesc(PublicBulletin::getId);
        Page<PublicBulletin> pageInfo = publicBulletinMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        Map<Long, String> regulatorNameMap = loadRegulatorNameMap(pageInfo.getRecords());
        List<BulletinVO> records = pageInfo.getRecords().stream()
            .map(item -> toVO(item, regulatorNameMap))
            .toList();
        return PageResult.of(records, pageInfo.getTotal(), safePage, safeSize);
    }

    @Override
    public BulletinDetailVO getAdminDetail(Long userId, Long bulletinId) {
        requireAdmin(userId);
        PublicBulletin bulletin = requireBulletin(bulletinId);
        return toDetailVO(bulletin, loadRegulatorNameMap(List.of(bulletin)));
    }

    @Override
    public BulletinDetailVO create(Long userId, String username, BulletinSaveDTO dto) {
        FoodRegulator operator = requireAdmin(userId);
        PublicBulletin bulletin = new PublicBulletin();
        bulletin.setTitle(dto.getTitle().trim());
        bulletin.setCategory(normalizeCategory(dto.getCategory()));
        bulletin.setContent(normalizeContent(dto.getContent()));
        bulletin.setStatus(STATUS_DRAFT);
        bulletin.setCreatedBy(userId);
        bulletin.setUpdateTime(LocalDateTime.now());
        bulletin.setDeleted(0);
        publicBulletinMapper.insert(bulletin);
        bulletinPublicCacheService.evict(bulletin.getId());
        PublicBulletin saved = publicBulletinMapper.selectById(bulletin.getId());
        auditLogService.recordBulletinAudit(
            userId,
            ROLE_ADMIN,
            auditOperatorNameResolver.resolveRegulatorOperatorName(operator, username),
            "BULLETIN_CREATE",
            "创建公告草稿",
            null,
            saved,
            "创建公告草稿，当前状态=" + saved.getStatus()
        );
        return toDetailVO(saved, loadRegulatorNameMap(List.of(saved)));
    }

    @Override
    public BulletinDetailVO update(Long userId, String username, Long bulletinId, BulletinSaveDTO dto) {
        FoodRegulator operator = requireAdmin(userId);
        PublicBulletin bulletin = requireBulletin(bulletinId);
        PublicBulletin before = copyBulletin(bulletin);
        bulletin.setTitle(dto.getTitle().trim());
        bulletin.setCategory(normalizeCategory(dto.getCategory()));
        bulletin.setContent(normalizeContent(dto.getContent()));
        bulletin.setUpdateTime(LocalDateTime.now());
        publicBulletinMapper.updateById(bulletin);
        bulletinPublicCacheService.evict(bulletinId);
        PublicBulletin saved = publicBulletinMapper.selectById(bulletinId);
        if (hasBulletinCoreChanges(before, saved)) {
            auditLogService.recordBulletinAudit(
                userId,
                ROLE_ADMIN,
                auditOperatorNameResolver.resolveRegulatorOperatorName(operator, username),
                "BULLETIN_UPDATE",
                "更新公告",
                before,
                saved,
                "更新公告内容：标题/分类/正文已调整"
            );
        }
        return toDetailVO(saved, loadRegulatorNameMap(List.of(saved)));
    }

    @Override
    public BulletinDetailVO publish(Long userId, String username, Long bulletinId) {
        FoodRegulator operator = requireAdmin(userId);
        PublicBulletin bulletin = requireBulletin(bulletinId);
        if (STATUS_PUBLISHED.equalsIgnoreCase(bulletin.getStatus())) {
            return toDetailVO(bulletin, loadRegulatorNameMap(List.of(bulletin)));
        }
        PublicBulletin before = copyBulletin(bulletin);
        bulletin.setStatus(STATUS_PUBLISHED);
        bulletin.setPublishedBy(userId);
        bulletin.setPublishedTime(LocalDateTime.now());
        bulletin.setUpdateTime(LocalDateTime.now());
        publicBulletinMapper.updateById(bulletin);
        bulletinPublicCacheService.evict(bulletinId);
        PublicBulletin saved = publicBulletinMapper.selectById(bulletinId);
        auditLogService.recordBulletinAudit(
            userId,
            ROLE_ADMIN,
            auditOperatorNameResolver.resolveRegulatorOperatorName(operator, username),
            "BULLETIN_PUBLISH",
            "发布公告",
            before,
            saved,
            "公告状态由 " + before.getStatus() + " 调整为 " + saved.getStatus()
        );
        return toDetailVO(saved, loadRegulatorNameMap(List.of(saved)));
    }

    @Override
    public BulletinDetailVO offline(Long userId, String username, Long bulletinId) {
        FoodRegulator operator = requireAdmin(userId);
        PublicBulletin bulletin = requireBulletin(bulletinId);
        if (STATUS_OFFLINE.equalsIgnoreCase(bulletin.getStatus())) {
            return toDetailVO(bulletin, loadRegulatorNameMap(List.of(bulletin)));
        }
        PublicBulletin before = copyBulletin(bulletin);
        bulletin.setStatus(STATUS_OFFLINE);
        bulletin.setUpdateTime(LocalDateTime.now());
        publicBulletinMapper.updateById(bulletin);
        bulletinPublicCacheService.evict(bulletinId);
        PublicBulletin saved = publicBulletinMapper.selectById(bulletinId);
        auditLogService.recordBulletinAudit(
            userId,
            ROLE_ADMIN,
            auditOperatorNameResolver.resolveRegulatorOperatorName(operator, username),
            "BULLETIN_OFFLINE",
            "下线公告",
            before,
            saved,
            "公告状态由 " + before.getStatus() + " 调整为 " + saved.getStatus()
        );
        return toDetailVO(saved, loadRegulatorNameMap(List.of(saved)));
    }

    @Override
    public PageResult<BulletinVO> listPublic(String keyword, String category, int page, int size) {
        String queryHash = buildPublicListQueryHash(keyword, category, page, size);
        return bulletinPublicCacheService.getList(queryHash, () -> loadPublicBulletinList(keyword, category, page, size));
    }

    @Override
    public BulletinDetailVO getPublicDetail(Long bulletinId) {
        if (bulletinId == null) {
            return null;
        }
        return bulletinPublicCacheService.getDetail(bulletinId, () -> loadPublicBulletinDetail(bulletinId));
    }

    private PageResult<BulletinVO> loadPublicBulletinList(String keyword, String category, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        LambdaQueryWrapper<PublicBulletin> wrapper = new LambdaQueryWrapper<PublicBulletin>()
            .eq(PublicBulletin::getDeleted, 0)
            .eq(PublicBulletin::getStatus, STATUS_PUBLISHED);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(condition -> condition
                .like(PublicBulletin::getTitle, keyword.trim())
                .or()
                .like(PublicBulletin::getCategory, keyword.trim()));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(PublicBulletin::getCategory, normalizeCategory(category));
        }
        wrapper.orderByDesc(PublicBulletin::getPublishedTime).orderByDesc(PublicBulletin::getId);
        Page<PublicBulletin> pageInfo = publicBulletinMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        Map<Long, String> regulatorNameMap = loadRegulatorNameMap(pageInfo.getRecords());
        List<BulletinVO> records = pageInfo.getRecords().stream()
            .map(item -> toVO(item, regulatorNameMap))
            .toList();
        return PageResult.of(records, pageInfo.getTotal(), safePage, safeSize);
    }

    private BulletinDetailVO loadPublicBulletinDetail(Long bulletinId) {
        PublicBulletin bulletin = publicBulletinMapper.selectById(bulletinId);
        if (bulletin == null || isDeleted(bulletin.getDeleted())) {
            return null;
        }
        if (!STATUS_PUBLISHED.equalsIgnoreCase(bulletin.getStatus())) {
            return null;
        }
        return toDetailVO(bulletin, loadRegulatorNameMap(List.of(bulletin)));
    }

    private String buildPublicListQueryHash(String keyword, String category, int page, int size) {
        String raw = String.join("|",
            StringUtils.hasText(keyword) ? keyword.trim() : "",
            StringUtils.hasText(category) ? normalizeCategory(category) : "",
            String.valueOf(Math.max(1, page)),
            String.valueOf(Math.max(1, Math.min(size, MAX_PAGE_SIZE)))
        );
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private FoodRegulator requireAdmin(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        FoodRegulator regulator = foodRegulatorMapper.selectOne(new LambdaQueryWrapper<FoodRegulator>()
            .eq(FoodRegulator::getUserId, userId)
            .eq(FoodRegulator::getDeleted, 0)
            .last("limit 1"));
        if (regulator == null) {
            throw new IllegalArgumentException("admin only");
        }
        if (regulator.getStatus() != null && regulator.getStatus() != 1) {
            throw new IllegalArgumentException("regulator disabled");
        }
        if (!ROLE_ADMIN.equalsIgnoreCase(regulator.getRoleType())) {
            throw new IllegalArgumentException("admin only");
        }
        return regulator;
    }

    private PublicBulletin requireBulletin(Long bulletinId) {
        if (bulletinId == null) {
            throw new IllegalArgumentException("bulletinId required");
        }
        PublicBulletin bulletin = publicBulletinMapper.selectById(bulletinId);
        if (bulletin == null || isDeleted(bulletin.getDeleted())) {
            throw new IllegalArgumentException("bulletin not found");
        }
        return bulletin;
    }

    private BulletinVO toVO(PublicBulletin bulletin, Map<Long, String> regulatorNameMap) {
        BulletinVO vo = new BulletinVO();
        vo.setId(bulletin.getId());
        vo.setTitle(bulletin.getTitle());
        vo.setCategory(bulletin.getCategory());
        vo.setStatus(bulletin.getStatus());
        vo.setCreatedBy(bulletin.getCreatedBy());
        vo.setCreatedByName(resolveName(regulatorNameMap, bulletin.getCreatedBy()));
        vo.setPublishedBy(bulletin.getPublishedBy());
        vo.setPublishedByName(resolveName(regulatorNameMap, bulletin.getPublishedBy()));
        vo.setPublishedTime(bulletin.getPublishedTime());
        vo.setCreateTime(bulletin.getCreateTime());
        vo.setUpdateTime(bulletin.getUpdateTime());
        return vo;
    }

    private BulletinDetailVO toDetailVO(PublicBulletin bulletin, Map<Long, String> regulatorNameMap) {
        BulletinDetailVO vo = new BulletinDetailVO();
        vo.setId(bulletin.getId());
        vo.setTitle(bulletin.getTitle());
        vo.setCategory(bulletin.getCategory());
        vo.setContent(bulletin.getContent());
        vo.setStatus(bulletin.getStatus());
        vo.setCreatedBy(bulletin.getCreatedBy());
        vo.setCreatedByName(resolveName(regulatorNameMap, bulletin.getCreatedBy()));
        vo.setPublishedBy(bulletin.getPublishedBy());
        vo.setPublishedByName(resolveName(regulatorNameMap, bulletin.getPublishedBy()));
        vo.setPublishedTime(bulletin.getPublishedTime());
        vo.setCreateTime(bulletin.getCreateTime());
        vo.setUpdateTime(bulletin.getUpdateTime());
        return vo;
    }

    private Map<Long, String> loadRegulatorNameMap(List<PublicBulletin> bulletins) {
        Set<Long> userIds = bulletins.stream()
            .flatMap(item -> java.util.stream.Stream.of(item.getCreatedBy(), item.getPublishedBy()))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return foodRegulatorMapper.selectList(new LambdaQueryWrapper<FoodRegulator>()
                .in(FoodRegulator::getUserId, userIds)
                .eq(FoodRegulator::getDeleted, 0))
            .stream()
            .collect(Collectors.toMap(FoodRegulator::getUserId, FoodRegulator::getName, (left, right) -> left));
    }

    private String resolveName(Map<Long, String> regulatorNameMap, Long userId) {
        if (userId == null) {
            return null;
        }
        return regulatorNameMap.getOrDefault(userId, "监管人员");
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }

    private String normalizeContent(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String normalizeCategory(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private boolean hasBulletinCoreChanges(PublicBulletin before, PublicBulletin after) {
        if (before == null || after == null) {
            return true;
        }
        return !Objects.equals(before.getTitle(), after.getTitle())
            || !Objects.equals(before.getCategory(), after.getCategory())
            || !Objects.equals(before.getContent(), after.getContent())
            || !Objects.equals(before.getStatus(), after.getStatus());
    }

    private PublicBulletin copyBulletin(PublicBulletin source) {
        if (source == null) {
            return null;
        }
        PublicBulletin copy = new PublicBulletin();
        copy.setId(source.getId());
        copy.setTitle(source.getTitle());
        copy.setCategory(source.getCategory());
        copy.setContent(source.getContent());
        copy.setStatus(source.getStatus());
        copy.setCreatedBy(source.getCreatedBy());
        copy.setPublishedBy(source.getPublishedBy());
        copy.setPublishedTime(source.getPublishedTime());
        copy.setCreateTime(source.getCreateTime());
        copy.setUpdateTime(source.getUpdateTime());
        copy.setDeleted(source.getDeleted());
        return copy;
    }
}
