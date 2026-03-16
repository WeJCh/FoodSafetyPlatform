package com.mortal.regulation.service;

import com.mortal.platform.common.PageResult;
import com.mortal.regulation.dto.WarningAssignDTO;
import com.mortal.regulation.dto.WarningProcessActionDTO;
import com.mortal.regulation.dto.WarningRecordQueryDTO;
import com.mortal.regulation.vo.WarningRecordDetailVO;
import com.mortal.regulation.vo.WarningRecordVO;

/**
 * 预警代理服务（监管业务侧权限过滤）。
 */
public interface WarningProxyService {

    /**
     * 查询当前区域管理员可见预警列表。
     */
    PageResult<WarningRecordVO> listAdminWarnings(Long userId, WarningRecordQueryDTO queryDTO);

    /**
     * 查询当前区域管理员可见预警详情。
     */
    WarningRecordDetailVO getAdminWarningDetail(Long userId, Long warningId);

    /**
     * 区域管理员处理预警。
     */
    WarningRecordDetailVO processAdminWarning(Long userId,
                                              String username,
                                              Long warningId,
                                              WarningProcessActionDTO actionDTO);

    /**
     * 区域管理员指派预警处理人。
     */
    WarningRecordDetailVO assignAdminWarning(Long userId,
                                             String username,
                                             Long warningId,
                                             WarningAssignDTO assignDTO);

    /**
     * 查询当前执法员可见预警列表。
     */
    PageResult<WarningRecordVO> listMyWarnings(Long userId, WarningRecordQueryDTO queryDTO);

    /**
     * 查询当前执法员可见预警详情。
     */
    WarningRecordDetailVO getMyWarningDetail(Long userId, Long warningId);

    /**
     * 处理当前执法员可操作的预警。
     */
    WarningRecordDetailVO processMyWarning(Long userId,
                                           String username,
                                           Long warningId,
                                           WarningProcessActionDTO actionDTO);
}

