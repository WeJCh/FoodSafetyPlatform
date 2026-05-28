package com.mortal.user.service;

import com.mortal.user.vo.AuditLogVO;
import java.util.List;

public interface AuditLogService {

    void recordAudit(Long operatorUserId,
                     String operatorUserType,
                     String operatorName,
                     String targetType,
                     Long targetId,
                     Long targetUserId,
                     String targetName,
                     String bizType,
                     String actionType,
                     String actionName,
                     String beforeData,
                     String afterData,
                     String remark);

    void recordAudit(Long operatorUserId,
                     String operatorUserType,
                     String operatorName,
                     String targetType,
                     Long targetId,
                     Long targetUserId,
                     String targetName,
                     String bizType,
                     String actionType,
                     String actionName,
                     String beforeData,
                     String afterData,
                     Integer successFlag,
                     String errorMessage,
                     String remark);

    List<AuditLogVO> listTargetLogs(String targetType, Long targetId, int limit);

    List<AuditLogVO> listTargetLogs(String targetType, Long targetId, List<String> actionTypes, int limit);

    List<AuditLogVO> listRecentLogs(String bizType, int limit);
}
