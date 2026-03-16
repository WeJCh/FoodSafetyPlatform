package com.mortal.complaint.domain.service;

import com.mortal.complaint.domain.enums.ComplaintStatus;
import java.util.EnumSet;
import java.util.Map;

/**
 * 投诉状态流转服务
 */
public final class ComplaintStatusFlow {

    private static final Map<ComplaintStatus, EnumSet<ComplaintStatus>> FLOW = Map.of(
        ComplaintStatus.SUBMITTED, EnumSet.of(ComplaintStatus.PENDING, ComplaintStatus.REJECTED),
        ComplaintStatus.PENDING, EnumSet.of(ComplaintStatus.ASSIGNED, ComplaintStatus.REJECTED),
        ComplaintStatus.ASSIGNED, EnumSet.of(ComplaintStatus.ASSIGNED, ComplaintStatus.PROCESSING, ComplaintStatus.REJECTED),
        ComplaintStatus.PROCESSING, EnumSet.of(ComplaintStatus.FEEDBACKED, ComplaintStatus.ASSIGNED),
        ComplaintStatus.FEEDBACKED, EnumSet.noneOf(ComplaintStatus.class),
        ComplaintStatus.REJECTED, EnumSet.noneOf(ComplaintStatus.class)
    );

    private ComplaintStatusFlow() {
    }

    public static void validateTransition(ComplaintStatus from, ComplaintStatus to) {
        if (from == null || to == null
            || !FLOW.getOrDefault(from, EnumSet.noneOf(ComplaintStatus.class)).contains(to)) {
            throw new IllegalArgumentException("invalid complaint status transition");
        }
    }
}
