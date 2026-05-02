export function resolveErrorMessage(error, fallback = "操作失败，请稍后重试。") {
  if (typeof error === "string" && error.trim()) {
    return error.trim();
  }
  if (error && typeof error.message === "string" && error.message.trim()) {
    return error.message.trim();
  }
  return fallback;
}

export function getEmptyStateText(subject = "数据", hasFilters = false) {
  return hasFilters ? `暂无符合条件的${subject}` : `暂无${subject}`;
}

export function getEmptyStateDescription(
  subject = "数据",
  hasFilters = false,
  defaultDescription = "",
  filteredDescription = ""
) {
  if (hasFilters) {
    return filteredDescription || `请调整筛选条件后重新查看${subject}。`;
  }
  return defaultDescription || `新的${subject}将在这里展示。`;
}
