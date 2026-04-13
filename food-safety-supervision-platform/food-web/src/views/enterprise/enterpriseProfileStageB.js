const STORAGE_PREFIX = "enterprise-profile-stage-b";

export const ENTERPRISE_ATTACHMENT_FIELDS = [
  { type: "businessLicense", label: "营业执照" },
  { type: "foodPermit", label: "食品经营许可证" },
  { type: "onsitePhoto", label: "经营场所照片" }
];

export function createEmptyStageBData() {
  return {
    creditCode: "",
    legalRepresentative: "",
    attachments: [],
    history: []
  };
}

function getStorageKey(userId) {
  return `${STORAGE_PREFIX}:${userId || "anonymous"}`;
}

export function loadStageBData(userId) {
  try {
    const raw = window.localStorage.getItem(getStorageKey(userId));
    if (!raw) return createEmptyStageBData();
    const parsed = JSON.parse(raw);
    return {
      ...createEmptyStageBData(),
      ...parsed,
      attachments: Array.isArray(parsed?.attachments) ? parsed.attachments : [],
      history: Array.isArray(parsed?.history) ? parsed.history : []
    };
  } catch {
    return createEmptyStageBData();
  }
}

export function saveStageBData(userId, payload) {
  const normalized = {
    ...createEmptyStageBData(),
    ...payload,
    attachments: Array.isArray(payload?.attachments) ? payload.attachments : [],
    history: Array.isArray(payload?.history) ? payload.history : []
  };
  window.localStorage.setItem(getStorageKey(userId), JSON.stringify(normalized));
  return normalized;
}

export function mergeProfileWithStageB(profilePayload = {}, stageBPayload = {}) {
  return {
    ...createEmptyStageBData(),
    ...stageBPayload,
    creditCode: profilePayload.creditCode || stageBPayload.creditCode || "",
    legalRepresentative: profilePayload.legalRepresentative || stageBPayload.legalRepresentative || "",
    attachments: Array.isArray(profilePayload.attachments)
      ? profilePayload.attachments
      : Array.isArray(stageBPayload.attachments)
        ? stageBPayload.attachments
        : [],
    history: Array.isArray(stageBPayload.history) ? stageBPayload.history : []
  };
}

export function upsertStageBHistory(history, entry) {
  const next = Array.isArray(history) ? [...history] : [];
  const exists = next.some((item) => item.type === entry.type && item.time === entry.time);
  if (!exists) next.push(entry);
  return next.sort((a, b) => String(a.time || "").localeCompare(String(b.time || "")));
}

export function buildApprovalTimeline({ profileLoaded, approvalStatus, approvalComment, approvedTime, history }) {
  const items = Array.isArray(history) ? [...history] : [];
  if (profileLoaded) {
    items.push({
      type: "PROFILE_SUBMITTED",
      label: "资料已提交",
      time: items.find((item) => item.type === "PROFILE_SUBMITTED")?.time || "",
      note: "企业备案资料已进入审核流程。"
    });
  }
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
  const deduped = [];
  items.forEach((item) => {
    if (!deduped.some((target) => target.type === item.type && target.label === item.label)) {
      deduped.push(item);
    }
  });
  return deduped;
}
