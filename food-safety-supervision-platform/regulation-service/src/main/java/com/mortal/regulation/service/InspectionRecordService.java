package com.mortal.regulation.service;

import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.vo.InspectionRecordDetailVO;
import com.mortal.regulation.vo.InspectionRecordVO;
import java.time.LocalDate;

public interface InspectionRecordService {

    PageResult<InspectionRecordVO> listMy(Long userId,
                                          String enterpriseName,
                                          String result,
                                          LocalDate startDate,
                                          LocalDate endDate,
                                          int page,
                                          int size);

    PageResult<InspectionRecordVO> listForAdmin(Long userId,
                                                String enterpriseName,
                                                String result,
                                                LocalDate startDate,
                                                LocalDate endDate,
                                                int page,
                                                int size);

    InspectionRecordDetailVO getDetail(Long userId, Long recordId);
}
