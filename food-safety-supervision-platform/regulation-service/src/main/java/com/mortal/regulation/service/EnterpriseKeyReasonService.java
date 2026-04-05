package com.mortal.regulation.service;

import com.mortal.regulation.vo.EnterpriseKeyReasonVO;
import java.util.List;

/**
 * 企业关键原因服务接口
 */
public interface EnterpriseKeyReasonService {

    /**
     * 标记企业为关键企业
     * @param enterpriseId 企业ID
     * @param reasonType 原因类型
     * @param reasonDetail 原因详情
     * @param sourceType 来源类型
     * @param sourceId 来源ID
     * @param operatorId 操作人ID
     */
    void markEnterpriseAsKey(Long enterpriseId,
                             String reasonType,
                             String reasonDetail,
                             String sourceType,
                             Long sourceId,
                             Long operatorId);

    /**
     * 获取企业最近的关键原因列表
     * @param enterpriseId 企业ID
     * @param limit 限制数量
     * @return 关键原因列表
     */
    List<EnterpriseKeyReasonVO> listRecentByEnterpriseId(Long enterpriseId, int limit);
}
