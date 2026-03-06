package com.mortal.warning.service;

import com.mortal.warning.common.PageResult;
import com.mortal.warning.dto.WarningProcessActionDTO;
import com.mortal.warning.dto.WarningRecordQueryDTO;
import com.mortal.warning.dto.WarningScopeDTO;
import com.mortal.warning.dto.WarningEventUpsertDTO;
import com.mortal.warning.vo.WarningRecordDetailVO;
import com.mortal.warning.vo.WarningRecordVO;

public interface WarningEventService {

    /**
     * 内部预警事件幂等上报。
     */
    WarningRecordVO upsertInternalEvent(WarningEventUpsertDTO dto);

    /**
     * 分页查询预警列表。
     */
    PageResult<WarningRecordVO> pageWarningRecords(WarningRecordQueryDTO queryDTO);

    /**
     * 查询预警详情（含处理日志）。
     */
    WarningRecordDetailVO getWarningRecordDetail(Long warningId, WarningScopeDTO scopeDTO);

    /**
     * 处理预警动作（签收/处理中/已解决/已关闭）。
     */
    WarningRecordDetailVO processWarning(Long warningId,
                                         WarningProcessActionDTO actionDTO,
                                         Long operatorId,
                                         String operatorName,
                                         WarningScopeDTO scopeDTO);
}
