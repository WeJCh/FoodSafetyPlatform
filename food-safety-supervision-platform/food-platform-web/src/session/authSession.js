const LOCAL_KEY = "food-platform-session";
const SESSION_KEY = "food-platform-session-temp";

function parseStoredValue(raw) {
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function normalizeRoles(payload) {
  return Array.isArray(payload?.roles) ? payload.roles : [];
}

export function isEnterpriseIdentity(payload) {
  const roles = normalizeRoles(payload);
  return roles.includes("ENTERPRISE") || payload?.userType === "ENTERPRISE";
}

export function isRememberedSession() {
  return Boolean(localStorage.getItem(LOCAL_KEY));
}

export function persistSession(payload, remember = false) {
  const targetStorage = remember ? localStorage : sessionStorage;
  const otherStorage = remember ? sessionStorage : localStorage;
  const session = {
    token: payload.token || "",
    username: payload.username || "",
    userType: payload.userType || "",
    roles: normalizeRoles(payload)
  };
  targetStorage.setItem(remember ? LOCAL_KEY : SESSION_KEY, JSON.stringify(session));
  otherStorage.removeItem(remember ? SESSION_KEY : LOCAL_KEY);
  return session;
}

export function getStoredSession() {
  return parseStoredValue(localStorage.getItem(LOCAL_KEY))
    || parseStoredValue(sessionStorage.getItem(SESSION_KEY));
}

export function clearStoredSession() {
  localStorage.removeItem(LOCAL_KEY);
  sessionStorage.removeItem(SESSION_KEY);
}