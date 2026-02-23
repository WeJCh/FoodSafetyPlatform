package com.mortal.regulation.service;

import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.dto.InspectionSubmitDTO;
import com.mortal.regulation.dto.InspectionTaskAssignDTO;
import com.mortal.regulation.dto.InspectionTaskCreateDTO;
import com.mortal.regulation.vo.InspectionTaskVO;

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
