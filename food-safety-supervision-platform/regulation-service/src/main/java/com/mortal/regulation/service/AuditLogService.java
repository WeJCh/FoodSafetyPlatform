package com.mortal.regulation.service;

import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.entity.FoodProduct;
import com.mortal.regulation.entity.FoodRegulator;
import com.mortal.regulation.entity.PublicBulletin;
import com.mortal.regulation.vo.AuditLogVO;
import java.util.List;

public interface AuditLogService {

    void recordRegulatorAudit(Long operatorUserId,
                              String operatorName,
                              String actionType,
                              String actionName,
                              FoodRegulator beforeRegulator,
                              FoodRegulator afterRegulator,
                              List<Long> beforeRegionIds,
                              List<Long> afterRegionIds,
                              String remark);

    void recordEnterpriseAudit(Long operatorUserId,
                               String operatorUserType,
                               String operatorName,
                               String actionType,
                               String actionName,
                               FoodEnterprise beforeEnterprise,
                               FoodEnterprise afterEnterprise,
                               String remark);

    void recordBulletinAudit(Long operatorUserId,
                             String operatorUserType,
                             String operatorName,
                             String actionType,
                             String actionName,
                             PublicBulletin beforeBulletin,
                             PublicBulletin afterBulletin,
                             String remark);

    void recordProductAudit(Long operatorUserId,
                            String operatorUserType,
                            String operatorName,
                            String actionType,
                            String actionName,
                            FoodProduct beforeProduct,
                            FoodProduct afterProduct,
                            String remark);

    List<AuditLogVO> listEnterpriseLogs(Long enterpriseId, int limit);

    List<AuditLogVO> listRecentEnterpriseLogs(int limit);

    List<AuditLogVO> listBulletinLogs(Long bulletinId, int limit);

    List<AuditLogVO> listRecentBulletinLogs(int limit);

    List<AuditLogVO> listProductLogs(Long productId, int limit);

    List<AuditLogVO> listRegulatorLogs(Long regulatorId, int limit);

    List<AuditLogVO> listRecentRegulatorLogs(int limit);
}
