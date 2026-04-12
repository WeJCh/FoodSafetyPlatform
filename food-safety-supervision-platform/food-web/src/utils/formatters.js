export function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

export function formatByMap(value, map, fallback = "-") {
  return map[value] || value || fallback;
}
