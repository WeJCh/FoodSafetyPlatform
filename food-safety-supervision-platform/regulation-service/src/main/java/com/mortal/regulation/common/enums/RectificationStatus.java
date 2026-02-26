package com.mortal.regulation.common.enums;

/**
 * 整改任务状态。
 */
public enum RectificationStatus {
    /** 企业整改中。 */
    ONGOING,
    /** 企业已提交，等待监管复核。 */
    SUBMITTED,
    /** 已打回重做。 */
    REWORK,
    /** 监管复核通过。 */
    CONFIRMED
}
