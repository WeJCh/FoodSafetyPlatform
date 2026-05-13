package com.mortal.regulation.common.enums;

public enum EnterpriseKeyReasonType {
    WARNING_TRIGGERED,//预警触发
    COMPLAINT_OVERFLOW,//投诉过多
    CONSECUTIVE_INSPECTION_FAIL,//连续检查不合格
    SAMPLING_FAIL,//抽检不合格
    RECTIFICATION_OVERDUE,//整改逾期
    MANUAL_SET //人工设定
}
