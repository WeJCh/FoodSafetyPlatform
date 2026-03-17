package com.mortal.regulation.operation.service;

import com.mortal.platform.common.PageResult;
import com.mortal.regulation.operation.dto.RectificationReviewDTO;
import com.mortal.regulation.operation.dto.RectificationSubmitDTO;
import com.mortal.regulation.operation.vo.RectificationActionLogVO;
import com.mortal.regulation.operation.vo.RectificationTaskVO;
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

    RectificationTaskVO getDetail(Long operatorUserId, String userType, Long rectificationId);

    List<RectificationActionLogVO> listActions(Long operatorUserId, String userType, Long rectificationId);
}
