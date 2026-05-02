package com.mortal.regulation.operation.service;

import com.mortal.regulation.operation.vo.AuditLogVO;
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

    List<AuditLogVO> listTargetLogs(String targetType, Long targetId, int limit);

    List<AuditLogVO> listRecentLogs(String bizType, int limit);
}
