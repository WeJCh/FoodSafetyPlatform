export const ENTERPRISE_ATTACHMENT_FIELDS = [
  { type: "businessLicense", label: "营业执照" },
  { type: "foodPermit", label: "食品经营许可证" },
  { type: "onsitePhoto", label: "经营场所照片" }
];

export function createEmptyStageBData() {
  return {
    legalRepresentative: "",
    attachments: []
  };
}

export function buildApprovalTimeline({ profileLoaded, approvalStatus, approvalComment, approvedTime }) {
  if (!profileLoaded) return [];

  const items = [
    {
      type: "PROFILE_SUBMITTED",
      label: "资料已提交",
      time: "",
      note: "企业备案资料已进入审核流程。"
    }
  ];

  if (approvalStatus === "PENDING") {
    items.push({
      type: "PROFILE_REVIEWING",
      label: "审核中",
      time: "",
      note: "监管侧正在审核企业备案资料。"
    });
  }

  if (approvalStatus === "APPROVED") {
    items.push({
      type: "PROFILE_APPROVED",
      label: "审核通过",
      time: approvedTime || "",
      note: approvalComment || "企业资料已审核通过。"
    });
  }

  if (approvalStatus === "REJECTED") {
    items.push({
      type: "PROFILE_REJECTED",
      label: "审核驳回",
      time: approvedTime || "",
      note: approvalComment || "请根据审核意见修改后重新提交。"
    });
  }

  return items;
}
