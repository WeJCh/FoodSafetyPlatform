package com.mortal.regulation.service;

import com.mortal.platform.common.PageResult;
import com.mortal.regulation.dto.RectificationReviewDTO;
import com.mortal.regulation.dto.RectificationSubmitDTO;
import com.mortal.regulation.vo.RectificationActionLogVO;
import com.mortal.regulation.vo.RectificationTaskVO;
import java.util.List;

public interface RectificationService {

    void createFromInspection(Long inspectionId, Long enterpriseId, String rectificationDesc);

    PageResult<RectificationTaskVO> listMy(Long enterpriseUserId, String status, int page, int size);

    RectificationTaskVO submitMy(Long enterpriseUserId, Long rectificationId, RectificationSubmitDTO dto);

    PageResult<RectificationTaskVO> listForAdmin(Long regulatorUserId,
                                                 String status,
                                                 String enterpriseName,
                                                 int page,
                                                 int size);

    PageResult<RectificationTaskVO> listForEnforcer(Long regulatorUserId,
                                                    String status,
                                                    String enterpriseName,
                                                    int page,
                                                    int size);

    RectificationTaskVO review(Long regulatorUserId, Long rectificationId, RectificationReviewDTO dto);

    /**
     * 获取整改任务详情
     * @param operatorUserId 操作员用户ID
     * @param userType 操作员用户类型
     * @param rectificationId 整改任务ID
     * @return 整改任务详情
     */
    RectificationTaskVO getDetail(Long operatorUserId, String userType, Long rectificationId);

    /**
     * 获取整改任务操作日志
     * @param operatorUserId 操作员用户ID
     * @param userType 操作员用户类型
     * @param rectificationId 整改任务ID
     * @return 整改任务操作日志
     */
    List<RectificationActionLogVO> listActions(Long operatorUserId, String userType, Long rectificationId);
}

