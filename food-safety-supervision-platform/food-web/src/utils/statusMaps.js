export const enterpriseStatusMap = {
  NORMAL: "正常",
  KEY: "重点监管"
};

export const approvalStatusMap = {
  PENDING: "待审核",
  APPROVED: "已通过",
  REJECTED: "已驳回"
};

export const complaintStatusMap = {
  SUBMITTED: "已提交",
  PENDING: "已受理",
  ASSIGNED: "已派发",
  PROCESSING: "处理中",
  FEEDBACKED: "已反馈",
  REJECTED: "已驳回"
};

export const warningStatusMap = {
  OPEN: "待处理",
  PROCESSING: "处理中",
  RESOLVED: "已解决",
  CLOSED: "已归档"
};

export const warningLevelMap = {
  L1: "一级",
  L2: "二级"
};

export const warningActionMap = {
  EVENT_UPSERT: "系统上报",
  ASSIGN: "派发处理",
  PROCESS: "进入处理中",
  RESOLVE: "标记已解决",
  AUTO_LEVEL_UP: "自动升级",
  AUTO_ARCHIVE: "系统归档"
};
