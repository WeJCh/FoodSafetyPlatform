package com.mortal.regulation.service;

import com.mortal.regulation.common.enums.ComplaintStatus;
import com.mortal.regulation.common.enums.RectificationStatus;
import java.util.EnumSet;
import java.util.Map;

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
        RectificationStatus.SUBMITTED, EnumSet.of(RectificationStatus.CONFIRMED),
        RectificationStatus.CONFIRMED, EnumSet.noneOf(RectificationStatus.class)
    );

    private StatusTransitionValidator() {
    }

    /**
     * 验证投诉状态流转是否合法
     * 投诉状态流转
     * SUBMITTED → PENDING → ASSIGNED → PROCESSING → FEEDBACKED
     * 允许“重新指派”：ASSIGNED → ASSIGNED（仅修改 assignedTo）
     * 禁止跨级跳转（如 SUBMITTED 直接 PROCESSING）
     * @param from 起始状态
     * @param to 目标状态
     */
    public static void validateComplaintTransition(ComplaintStatus from, ComplaintStatus to) {
        // 关键校验：投诉状态必须按既定流程流转，禁止跳跃
        if (from == null || to == null || !COMPLAINT_FLOW.getOrDefault(from, EnumSet.noneOf(ComplaintStatus.class))
            .contains(to)) {
            throw new IllegalArgumentException("invalid complaint status transition");
        }
    }

    /**
     * 验证整改状态流转是否合法
     * 整改状态流转
     * ONGOING → SUBMITTED → CONFIRMED
     * 禁止回退（如 CONFIRMED → SUBMITTED）
     * @param from 起始状态
     * @param to 目标状态
     */
    public static void validateRectificationTransition(RectificationStatus from, RectificationStatus to) {
        // 关键校验：整改状态只能逐级流转，禁止回退
        if (from == null || to == null || !RECTIFICATION_FLOW.getOrDefault(from, EnumSet.noneOf(RectificationStatus.class))
            .contains(to)) {
            throw new IllegalArgumentException("invalid rectification status transition");
        }
    }
}
