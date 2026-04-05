package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.regulation.common.enums.EnterpriseKeyReasonType;
import com.mortal.regulation.common.enums.TaskSourceType;
import com.mortal.regulation.entity.EnterpriseKeyReason;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.mapper.EnterpriseKeyReasonMapper;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.service.EnterpriseKeyReasonService;
import com.mortal.regulation.vo.EnterpriseKeyReasonVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 企业关键原因服务实现类
 */
@Service
public class EnterpriseKeyReasonServiceImpl implements EnterpriseKeyReasonService {

    private static final String STATUS_KEY = "KEY";
    private static final int DEFAULT_REASON_LIMIT = 5;

    private final EnterpriseKeyReasonMapper enterpriseKeyReasonMapper;
    private final FoodEnterpriseMapper foodEnterpriseMapper;

    public EnterpriseKeyReasonServiceImpl(EnterpriseKeyReasonMapper enterpriseKeyReasonMapper,
                                          FoodEnterpriseMapper foodEnterpriseMapper) {
        this.enterpriseKeyReasonMapper = enterpriseKeyReasonMapper;
        this.foodEnterpriseMapper = foodEnterpriseMapper;
    }

    /**
     * 标记企业为关键企业
     * @param enterpriseId 企业ID
     * @param reasonType 原因类型
     * @param reasonDetail 原因详情
     * @param sourceType 来源类型
     * @param sourceId 来源ID
     * @param operatorId 操作人ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markEnterpriseAsKey(Long enterpriseId,
                                    String reasonType,
                                    String reasonDetail,
                                    String sourceType,
                                    Long sourceId,
                                    Long operatorId) {
        if (enterpriseId == null) {
            throw new IllegalArgumentException("enterprise not found");
        }
        EnterpriseKeyReasonType normalizedReasonType = parseReasonType(reasonType);
        TaskSourceType normalizedSourceType = parseSourceType(sourceType);

        FoodEnterprise enterprise = foodEnterpriseMapper.selectById(enterpriseId);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            throw new IllegalArgumentException("enterprise not found");
        }

        EnterpriseKeyReason existing = enterpriseKeyReasonMapper.selectOne(buildDuplicateWrapper(
            enterpriseId,
            normalizedReasonType,
            normalizedSourceType,
            sourceId
        ));
        if (existing == null) {
            EnterpriseKeyReason record = new EnterpriseKeyReason();
            record.setEnterpriseId(enterpriseId);
            record.setReasonType(normalizedReasonType);
            record.setReasonDetail(normalizeText(reasonDetail));
            record.setSourceType(normalizedSourceType);
            record.setSourceId(sourceId);
            record.setOperatorId(operatorId);
            record.setCreateTime(LocalDateTime.now());
            enterpriseKeyReasonMapper.insert(record);
        }

        if (!STATUS_KEY.equalsIgnoreCase(enterprise.getStatus())) {
            enterprise.setStatus(STATUS_KEY);
            enterprise.setUpdateTime(LocalDateTime.now());
            foodEnterpriseMapper.updateById(enterprise);
        }
    }

    /**
     * 获取企业最近的关键原因列表
     * @param enterpriseId 企业ID
     * @param limit 限制数量
     * @return 关键原因列表
     */
    @Override
    public List<EnterpriseKeyReasonVO> listRecentByEnterpriseId(Long enterpriseId, int limit) {
        if (enterpriseId == null) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, DEFAULT_REASON_LIMIT));
        return enterpriseKeyReasonMapper.selectList(new LambdaQueryWrapper<EnterpriseKeyReason>()
                .eq(EnterpriseKeyReason::getEnterpriseId, enterpriseId)
                .orderByDesc(EnterpriseKeyReason::getCreateTime, EnterpriseKeyReason::getId)
                .last("limit " + safeLimit))
            .stream()
            .map(this::toVO)
            .toList();
    }

    /**
     * 构建重复关键原因查询条件
     * @param enterpriseId 企业ID
     * @param reasonType 原因类型
     * @param sourceType 来源类型
     * @param sourceId 来源ID
     * @return 重复关键原因查询条件
     */
    private LambdaQueryWrapper<EnterpriseKeyReason> buildDuplicateWrapper(Long enterpriseId,
                                                                          EnterpriseKeyReasonType reasonType,
                                                                          TaskSourceType sourceType,
                                                                          Long sourceId) {
        LambdaQueryWrapper<EnterpriseKeyReason> wrapper = new LambdaQueryWrapper<EnterpriseKeyReason>()
            .eq(EnterpriseKeyReason::getEnterpriseId, enterpriseId)
            .eq(EnterpriseKeyReason::getReasonType, reasonType)
            .eq(EnterpriseKeyReason::getSourceType, sourceType);
        if (sourceId == null) {
            wrapper.isNull(EnterpriseKeyReason::getSourceId);
        } else {
            wrapper.eq(EnterpriseKeyReason::getSourceId, sourceId);
        }
        wrapper.last("limit 1");
        return wrapper;
    }

    private EnterpriseKeyReasonVO toVO(EnterpriseKeyReason reason) {
        EnterpriseKeyReasonVO vo = new EnterpriseKeyReasonVO();
        vo.setReasonType(reason.getReasonType() == null ? null : reason.getReasonType().name());
        vo.setReasonLabel(resolveReasonLabel(reason.getReasonType()));
        vo.setReasonDetail(reason.getReasonDetail());
        vo.setSourceType(reason.getSourceType() == null ? null : reason.getSourceType().name());
        vo.setSourceId(reason.getSourceId());
        vo.setCreateTime(reason.getCreateTime());
        return vo;
    }

    private String resolveReasonLabel(EnterpriseKeyReasonType reasonType) {
        if (reasonType == null) {
            return null;
        }
        return switch (reasonType) {
            case WARNING_TRIGGERED -> "预警触发";
            case COMPLAINT_OVERFLOW -> "投诉过多";
            case CONSECUTIVE_FAIL -> "连续不合格";
            case SAMPLING_FAIL -> "抽检不合格";
            case MANUAL_SET -> "人工设定";
        };
    }

    private EnterpriseKeyReasonType parseReasonType(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("reasonType required");
        }
        try {
            return EnterpriseKeyReasonType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid reasonType");
        }
    }

    private TaskSourceType parseSourceType(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("sourceType required");
        }
        try {
            return TaskSourceType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid sourceType");
        }
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}
