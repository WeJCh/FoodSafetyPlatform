package com.mortal.regulation.operation.service;

import com.mortal.platform.common.PageResult;
import com.mortal.regulation.operation.vo.InspectionRecordDetailVO;
import com.mortal.regulation.operation.vo.InspectionRecordVO;
import java.time.LocalDate;

public interface InspectionRecordService {

    PageResult<InspectionRecordVO> listForEnterprise(Long userId,
                                                     String result,
                                                     LocalDate startDate,
                                                     LocalDate endDate,
                                                     int page,
                                                     int size);

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

    InspectionRecordDetailVO getDetailForEnterprise(Long userId, Long recordId);
}
