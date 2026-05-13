package com.mortal.complaint.vo;

import lombok.Data;

@Data
public class ComplaintPublicStatsVO {

    private long totalCount;
    private long processingCount;
    private long finishedCount;
}
