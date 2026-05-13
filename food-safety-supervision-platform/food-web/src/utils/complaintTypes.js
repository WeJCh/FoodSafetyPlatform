export const complaintTypeOptions = [
  { value: "FOOD_SAFETY", label: "食品安全" },
  { value: "HYGIENE", label: "卫生环境" },
  { value: "PRICE", label: "价格收费" },
  { value: "FALSE_AD", label: "虚假宣传" },
  { value: "LICENSE", label: "资质证照" },
  { value: "SERVICE", label: "服务纠纷" },
  { value: "PACKAGING", label: "包装配送" },
  { value: "OTHER", label: "其他" }
];

export const complaintTypeLabelMap = complaintTypeOptions.reduce((acc, item) => {
  acc[item.value] = item.label;
  return acc;
}, {});

export function formatComplaintType(value, fallback = "其他") {
  const key = String(value || "").trim().toUpperCase();
  return complaintTypeLabelMap[key] || fallback;
}

export function resolveComplaintTypeCode(value) {
  const key = String(value || "").trim().toUpperCase();
  return complaintTypeLabelMap[key] ? key : "";
}
