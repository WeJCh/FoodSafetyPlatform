package com.mortal.regulation.service;

import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.dto.ComplaintAssignDTO;
import com.mortal.regulation.dto.ComplaintHandleDTO;
import com.mortal.regulation.dto.ComplaintSubmitDTO;
import com.mortal.regulation.vo.ComplaintDetailVO;
import com.mortal.regulation.vo.ComplaintTrackVO;
import com.mortal.regulation.vo.ComplaintVO;

public interface ComplaintService {
    /**
     * 提交投诉
     * @param dto 投诉提交DTO
     * @return 投诉跟踪VO
     */
    ComplaintTrackVO submitPublic(ComplaintSubmitDTO dto);

    /**
     * 跟踪投诉
     * @param complaintNo 投诉编号
     * @param contact 联系方式
     * @return 投诉跟踪VO
     */
    ComplaintTrackVO track(String complaintNo, String contact);

    /**
     * 查询投诉列表
     * @param operatorUserId 操作员用户ID
     * @param status 状态
     * @param enterpriseName 企业名称
     * @param assignedToName 被指派去处理投诉的执行人姓名
     * @param assignedByName 指派监管员名称
     * @param page 页码
     * @param size 每页条数
     * @return 投诉列表
     */
    PageResult<ComplaintVO> list(Long operatorUserId,
                                 String status,
                                 String enterpriseName,
                                 String assignedToName,
                                 String assignedByName,
                                 int page,
                                 int size);

    ComplaintDetailVO getDetail(Long operatorUserId, Long complaintId);

    /**
     * 接受投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @return 投诉VO
     */
    ComplaintVO accept(Long operatorUserId, Long complaintId);

    /**
     * 指派投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @param dto 投诉指派DTO
     * @return 投诉VO
     */
    ComplaintVO assign(Long operatorUserId, Long complaintId, ComplaintAssignDTO dto);

    /**
     * 开始处理投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @return 投诉VO
     */
    ComplaintVO startProcess(Long operatorUserId, Long complaintId);

    /**
     * 处理投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @param dto 投诉处理DTO
     * @return 投诉VO
     */
    ComplaintVO handle(Long operatorUserId, Long complaintId, ComplaintHandleDTO dto);
}
