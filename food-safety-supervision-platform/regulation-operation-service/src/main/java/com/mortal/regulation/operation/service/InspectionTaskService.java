package com.mortal.regulation.operation.service;

import com.mortal.platform.common.PageResult;
import com.mortal.regulation.operation.dto.InspectionSubmitDTO;
import com.mortal.regulation.operation.dto.InspectionTaskAssignDTO;
import com.mortal.regulation.operation.dto.InspectionTaskCreateDTO;
import com.mortal.regulation.operation.vo.InspectionTaskVO;

public interface InspectionTaskService {

    InspectionTaskVO createTask(Long userId, InspectionTaskCreateDTO dto);

    InspectionTaskVO assignTask(Long userId, Long taskId, InspectionTaskAssignDTO dto);

    PageResult<InspectionTaskVO> listTasksForAdmin(Long userId,
                                                   String enterpriseName,
                                                   String status,
                                                   int page,
                                                   int size);

    PageResult<InspectionTaskVO> listTasksForEnforcer(Long userId,
                                                      String status,
                                                      int page,
                                                      int size);

    InspectionTaskVO startTask(Long userId, Long taskId);

    InspectionTaskVO submitTask(Long userId, Long taskId, InspectionSubmitDTO dto);

    InspectionTaskVO closeTask(Long userId, Long taskId);
}
