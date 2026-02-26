package com.mortal.regulation.service;

import com.mortal.regulation.common.enums.ComplaintStatus;
import com.mortal.regulation.common.enums.RectificationStatus;
import java.util.EnumSet;
import java.util.Map;

/**
 * 统一状态流转校验器。
 */
public final class StatusTransitionValidator {

    private static final Map<ComplaintStatus, EnumSet<ComplaintStatus>> COMPLAINT_FLOW = Map.of(
        ComplaintStatus.SUBMITTED, EnumSet.of(ComplaintStatus.PENDING, ComplaintStatus.REJECTED),
        ComplaintStatus.PENDING, EnumSet.of(ComplaintStatus.ASSIGNED, ComplaintStatus.REJECTED),
        ComplaintStatus.ASSIGNED, EnumSet.of(ComplaintStatus.ASSIGNED, ComplaintStatus.PROCESSING, ComplaintStatus.REJECTED),
        ComplaintStatus.PROCESSING, EnumSet.of(ComplaintStatus.FEEDBACKED, ComplaintStatus.ASSIGNED),
        ComplaintStatus.FEEDBACKED, EnumSet.noneOf(ComplaintStatus.class),
        ComplaintStatus.REJECTED, EnumSet.noneOf(ComplaintStatus.class)
    );

    private static final Map<RectificationStatus, EnumSet<RectificationStatus>> RECTIFICATION_FLOW = Map.of(
        RectificationStatus.ONGOING, EnumSet.of(RectificationStatus.SUBMITTED),
        RectificationStatus.SUBMITTED, EnumSet.of(RectificationStatus.CONFIRMED, RectificationStatus.REWORK),
        RectificationStatus.REWORK, EnumSet.of(RectificationStatus.SUBMITTED),
        RectificationStatus.CONFIRMED, EnumSet.noneOf(RectificationStatus.class)
    );

    private StatusTransitionValidator() {
    }

    public static void validateComplaintTransition(ComplaintStatus from, ComplaintStatus to) {
        if (from == null || to == null
            || !COMPLAINT_FLOW.getOrDefault(from, EnumSet.noneOf(ComplaintStatus.class)).contains(to)) {
            throw new IllegalArgumentException("invalid complaint status transition");
        }
    }

    public static void validateRectificationTransition(RectificationStatus from, RectificationStatus to) {
        if (from == null || to == null
            || !RECTIFICATION_FLOW.getOrDefault(from, EnumSet.noneOf(RectificationStatus.class)).contains(to)) {
            throw new IllegalArgumentException("invalid rectification status transition");
        }
    }
}
