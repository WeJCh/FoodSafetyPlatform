package com.mortal.regulation.operation.service;

import com.mortal.platform.common.PageResult;
import com.mortal.regulation.operation.dto.SamplingResultSubmitDTO;
import com.mortal.regulation.operation.dto.SamplingTaskAssignDTO;
import com.mortal.regulation.operation.dto.SamplingTaskCreateDTO;
import com.mortal.regulation.operation.vo.SamplingResultVO;
import com.mortal.regulation.operation.vo.SamplingTaskVO;

public interface SamplingTaskService {
    /**
     * 创建抽检任务
     * 
     * @param userId 用户ID
     * @param dto 创建DTO
     * @return 抽检任务VO
     */
    SamplingTaskVO createTask(Long userId, SamplingTaskCreateDTO dto);

    /**
     * 指派抽检任务
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param dto 指派DTO
     * @return 抽检任务VO
     */
    SamplingTaskVO assignTask(Long userId, Long taskId, SamplingTaskAssignDTO dto);

    /**
     * 查询区域管理员抽检任务列表
     * 
     * @param userId 用户ID
     * @param enterpriseName 企业名称
     * @param status 任务状态
     * @param page 页码
     * @param size 每页大小
     * @return 抽检任务列表
     */
    PageResult<SamplingTaskVO> listTasksForAdmin(Long userId,
                                                 String enterpriseName,
                                                 String status,
                                                 int page,
                                                 int size);

    /**
     * 查询执法人员抽检任务列表
     * 
     * @param userId 用户ID
     * @param status 任务状态
     * @param page 页码
     * @param size 每页大小
     * @return 抽检任务列表
     */
    PageResult<SamplingTaskVO> listTasksForEnforcer(Long userId,
                                                    String status,
                                                    int page,
                                                    int size);

    /**
     * 提交抽检结果
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param dto 提交DTO
     * @return 抽检结果VO
     */
    SamplingResultVO submitResult(Long userId, Long taskId, SamplingResultSubmitDTO dto);
    /**
     * 发布抽检结果
     * 
     * @param userId 用户ID
     * @param resultId 结果ID
     * @return 抽检结果VO
     */
    SamplingResultVO publishResult(Long userId, Long resultId);
    /**
     * 下线抽检结果
     * 
     * @param userId 用户ID
     * @param resultId 结果ID
     * @return 抽检结果VO
     */
    SamplingResultVO offlineResult(Long userId, Long resultId);
    /**
     * 查询公众抽检结果列表
     * 
     * @param enterpriseName 企业名称
     * @param result 结果
     * @param page 页码
     * @param size 每页大小
     * @return 抽检结果列表
     */
    PageResult<SamplingResultVO> listPublicResults(String enterpriseName,
                                                   String result,
                                                   int page,
                                                   int size);

    /**
     * 获取公众抽检结果详情
     * 
     * @param resultId 结果ID
     * @return 抽检结果VO
     */
    SamplingResultVO getPublicResultDetail(Long resultId);

    /**
     * 关闭抽检任务
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return 抽检任务VO
     */
    SamplingTaskVO closeTask(Long userId, Long taskId);
}
