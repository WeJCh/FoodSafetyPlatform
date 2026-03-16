package com.mortal.warning.service;

import com.mortal.platform.common.PageResult;
import com.mortal.warning.dto.WarningAssignDTO;
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
     * 按新接口执行单一动作（PROCESS/RESOLVE）。
     */
    WarningRecordDetailVO processWarningAction(Long warningId,
                                               String actionType,
                                               String actionComment,
                                               Long operatorId,
                                               String operatorName,
                                               WarningScopeDTO scopeDTO);

    /**
     * 指派预警处理人。
     */
    WarningRecordDetailVO assignWarning(Long warningId,
                                        WarningAssignDTO assignDTO,
                                        Long operatorId,
                                        String operatorName,
                                        WarningScopeDTO scopeDTO);
}
