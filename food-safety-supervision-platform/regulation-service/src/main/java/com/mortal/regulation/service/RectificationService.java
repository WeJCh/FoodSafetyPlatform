package com.mortal.regulation.service;

import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.dto.RectificationReviewDTO;
import com.mortal.regulation.dto.RectificationSubmitDTO;
import com.mortal.regulation.vo.RectificationTaskVO;

public interface RectificationService {

    void createFromInspection(Long inspectionId, Long enterpriseId, String rectificationDesc);

    PageResult<RectificationTaskVO> listMy(Long enterpriseUserId, String status, int page, int size);

    RectificationTaskVO submitMy(Long enterpriseUserId, Long rectificationId, RectificationSubmitDTO dto);

    PageResult<RectificationTaskVO> listForAdmin(Long regulatorUserId,
                                                 String status,
                                                 String enterpriseName,
                                                 int page,
                                                 int size);

    RectificationTaskVO review(Long regulatorUserId, Long rectificationId, RectificationReviewDTO dto);
}
