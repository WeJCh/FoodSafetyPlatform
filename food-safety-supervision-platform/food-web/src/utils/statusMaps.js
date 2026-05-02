function normalizeKey(value) {
  return String(value || "").trim().toUpperCase();
}

function defineStatus(source) {
  return Object.freeze(source);
}

export const enterpriseStatusMap = defineStatus({
  NORMAL: "正常监管",
  KEY: "重点监管",
  RISK: "风险关注",
  A: "正常监管",
  B: "重点监管",
  C: "风险关注"
});

export const approvalStatusMap = defineStatus({
  PENDING: "待审核",
  APPROVED: "已通过",
  REJECTED: "已驳回"
});

export const complaintStatusMap = defineStatus({
  SUBMITTED: "待受理",
  PENDING: "待分派",
  ASSIGNED: "已分派",
  PROCESSING: "处理中",
  FEEDBACKED: "已反馈",
  REJECTED: "已驳回"
});

export const warningStatusMap = defineStatus({
  OPEN: "待处置",
  PROCESSING: "处理中",
  RESOLVED: "已解决",
  CLOSED: "已归档"
});

export const warningLevelMap = defineStatus({
  L1: "一级预警",
  L2: "二级预警"
});

export const warningActionMap = defineStatus({
  EVENT_UPSERT: "系统上报",
  ASSIGN: "派发处理",
  PROCESS: "进入处理",
  RESOLVE: "标记解决",
  AUTO_LEVEL_UP: "自动升级",
  AUTO_ARCHIVE: "系统归档"
});

export const rectificationStatusMap = defineStatus({
  ONGOING: "整改中",
  SUBMITTED: "待复核",
  REWORK: "退回整改",
  CONFIRMED: "已确认"
});

export const productStatusMap = defineStatus({
  ACTIVE: "启用",
  INACTIVE: "停用"
});

export const inspectionResultMap = defineStatus({
  PASS: "合格",
  FAIL: "不合格"
});

export const samplingPublicStatusMap = defineStatus({
  DRAFT: "草稿",
  PUBLISHED: "已公示",
  OFFLINE: "已下线"
});

export const inspectionTaskStatusMap = defineStatus({
  CREATED: "待启动",
  ASSIGNED: "已指派",
  IN_PROGRESS: "执行中",
  COMPLETED: "已完成",
  CLOSED: "已归档"
});

export const taskPriorityMap = defineStatus({
  LOW: "低优先级",
  MEDIUM: "中优先级",
  HIGH: "高优先级"
});

export const samplingTaskStatusMap = defineStatus({
  CREATED: "待派发",
  ASSIGNED: "待抽检",
  IN_PROGRESS: "抽检中",
  COMPLETED: "已完成",
  CLOSED: "已归档"
});

export const bulletinStatusMap = defineStatus({
  DRAFT: "草稿",
  PUBLISHED: "已发布",
  OFFLINE: "已下线"
});

export function formatStatusLabel(value, map, fallback = "-") {
  const key = normalizeKey(value);
  return map[key] || (value ? String(value) : fallback);
}

export function getStatusTone(value, kind = "") {
  const key = normalizeKey(value);
  const statusKind = normalizeKey(kind);

  if (statusKind === "APPROVAL") {
    if (key === "APPROVED") return "success";
    if (key === "REJECTED") return "danger";
    return "warning";
  }

  if (statusKind === "WARNING") {
    if (key === "RESOLVED" || key === "CLOSED") return "success";
    if (key === "PROCESSING") return "info";
    if (key === "OPEN") return "warning";
    return "neutral";
  }

  if (statusKind === "RECTIFICATION") {
    if (key === "CONFIRMED") return "success";
    if (key === "REWORK") return "danger";
    if (key === "SUBMITTED") return "warning";
    if (key === "ONGOING") return "info";
    return "neutral";
  }

  if (statusKind === "COMPLAINT") {
    if (key === "FEEDBACKED") return "success";
    if (key === "REJECTED") return "danger";
    if (key === "ASSIGNED" || key === "PROCESSING") return "info";
    return "warning";
  }

  if (statusKind === "ENTERPRISE") {
    if (key === "KEY" || key === "B") return "warning";
    if (key === "RISK" || key === "C") return "danger";
    return "success";
  }

  if (statusKind === "RESULT") {
    if (key === "FAIL") return "danger";
    if (key === "PASS") return "success";
    return "neutral";
  }

  if (statusKind === "PUBLICATION") {
    if (key === "PUBLISHED") return "success";
    if (key === "OFFLINE") return "neutral";
    return "warning";
  }

  if (statusKind === "TASK") {
    if (key === "COMPLETED" || key === "CLOSED") return "success";
    if (key === "IN_PROGRESS") return "info";
    if (key === "ASSIGNED" || key === "CREATED") return "warning";
    return "neutral";
  }

  return "neutral";
}
