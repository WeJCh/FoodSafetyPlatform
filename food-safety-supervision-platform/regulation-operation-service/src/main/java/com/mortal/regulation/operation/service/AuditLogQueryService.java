package com.mortal.regulation.operation.service;

import com.mortal.regulation.operation.vo.AuditLogVO;
import java.util.List;

public interface AuditLogQueryService {

    List<AuditLogVO> listLogs(Long operatorUserId, String operatorUserType, String targetType, Long targetId, int limit);

    List<AuditLogVO> listRecentLogs(Long operatorUserId, String operatorUserType, String bizType, int limit);
}
