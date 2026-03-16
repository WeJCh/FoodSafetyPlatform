package com.mortal.regulation.service;

import com.mortal.regulation.common.enums.RectificationStatus;
import java.util.EnumSet;
import java.util.Map;

/**
 * 统一状态流转校验器。
 */
public final class StatusTransitionValidator {

    private static final Map<RectificationStatus, EnumSet<RectificationStatus>> RECTIFICATION_FLOW = Map.of(
        RectificationStatus.ONGOING, EnumSet.of(RectificationStatus.SUBMITTED),
        RectificationStatus.SUBMITTED, EnumSet.of(RectificationStatus.CONFIRMED, RectificationStatus.REWORK),
        RectificationStatus.REWORK, EnumSet.of(RectificationStatus.SUBMITTED),
        RectificationStatus.CONFIRMED, EnumSet.noneOf(RectificationStatus.class)
    );

    private StatusTransitionValidator() {
    }

    public static void validateRectificationTransition(RectificationStatus from, RectificationStatus to) {
        if (from == null || to == null
            || !RECTIFICATION_FLOW.getOrDefault(from, EnumSet.noneOf(RectificationStatus.class)).contains(to)) {
            throw new IllegalArgumentException("invalid rectification status transition");
        }
    }
}
