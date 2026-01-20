package com.mortal.regulation.vo;

import java.util.List;
import lombok.Data;

@Data
public class BatchActionResult {

    private int successCount;
    private List<Long> failedIds;
}
