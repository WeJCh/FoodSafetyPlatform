package com.mortal.regulation.operation.vo;

import java.util.List;
import lombok.Data;

@Data
public class InspectionRecordDetailVO {

    private InspectionRecordVO record;
    private List<InspectionItemVO> items;
}
