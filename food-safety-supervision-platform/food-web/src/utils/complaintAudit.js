import { formatByMap } from "./formatters";
import { complaintStatusMap } from "./statusMaps";

const complaintActionNameMap = {
  COMPLAINT_SUBMIT: "提交投诉",
  COMPLAINT_ACCEPT: "受理投诉",
  COMPLAINT_ASSIGN: "分派投诉",
  COMPLAINT_REASSIGN: "改派投诉",
  COMPLAINT_PROCESS_START: "开始处理投诉",
  COMPLAINT_HANDLE: "处理完成投诉",
  COMPLAINT_REJECT: "驳回投诉"
};

export function formatComplaintAuditAction(value, fallback = "投诉操作日志") {
  const key = String(value || "").trim().toUpperCase();
  return complaintActionNameMap[key] || String(value || "").trim() || fallback;
}

export function formatComplaintAuditSummary(item, fallback = "暂无日志摘要") {
  const summary = String(item?.summary || "").trim();
  if (summary) return summary;

  const status = formatByMap(item?.status, complaintStatusMap);
  const actionLabel = formatComplaintAuditAction(item?.actionType, "");
  if (status && status !== "-") {
    return actionLabel ? `${actionLabel}，当前状态：${status}` : `投诉当前状态：${status}`;
  }
  return actionLabel || fallback;
}

export function formatComplaintAuditOperatorName(value, fallback = "系统") {
  return String(value || "").trim() || fallback;
}
