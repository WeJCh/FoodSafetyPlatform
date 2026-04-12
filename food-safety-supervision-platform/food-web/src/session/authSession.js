const LOCAL_KEY = "food-web-session";
const SESSION_KEY = "food-web-session-temp";

function parseStoredValue(raw) {
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function normalizeRoles(payload) {
  return Array.isArray(payload?.roles) ? payload.roles : [];
}

export function createSessionSnapshot(payload = {}) {
  return {
    token: payload.token || "",
    userId: payload.userId ?? null,
    username: payload.username || "",
    userType: payload.userType || "",
    roleType: payload.roleType || "",
    roles: normalizeRoles(payload)
  };
}

export function hasRole(payload, roleCode) {
  return normalizeRoles(payload).includes(roleCode);
}

export function isAdminIdentity(payload) {
  return hasRole(payload, "ADMIN") || payload?.userType === "ADMIN";
}

export function isPublicIdentity(payload) {
  return hasRole(payload, "PUBLIC") || payload?.userType === "PUBLIC";
}

export function isEnterpriseIdentity(payload) {
  return hasRole(payload, "ENTERPRISE") || payload?.userType === "ENTERPRISE";
}

export function isRegulatorAdminIdentity(payload) {
  return hasRole(payload, "REGULATOR_ADMIN") || payload?.roleType === "REGULATOR_ADMIN";
}

export function isRegulatorEnforcerIdentity(payload) {
  return hasRole(payload, "REGULATOR_ENFORCER") || payload?.roleType === "REGULATOR_ENFORCER";
}

export function isRegulatorIdentity(payload) {
  return (
    isRegulatorAdminIdentity(payload)
    || isRegulatorEnforcerIdentity(payload)
    || payload?.userType === "REGULATOR"
  );
}

export function resolveRegulatorRoleType(payload) {
  if (isRegulatorAdminIdentity(payload)) {
    return "REGULATOR_ADMIN";
  }
  if (isRegulatorEnforcerIdentity(payload)) {
    return "REGULATOR_ENFORCER";
  }
  return payload?.roleType || "";
}

export function isRememberedSession() {
  return Boolean(localStorage.getItem(LOCAL_KEY));
}

export function persistSession(payload, remember = false) {
  const targetStorage = remember ? localStorage : sessionStorage;
  const otherStorage = remember ? sessionStorage : localStorage;
  const session = createSessionSnapshot(payload);

  targetStorage.setItem(remember ? LOCAL_KEY : SESSION_KEY, JSON.stringify(session));
  otherStorage.removeItem(remember ? SESSION_KEY : LOCAL_KEY);

  return session;
}

export function getStoredSession() {
  return (
    parseStoredValue(localStorage.getItem(LOCAL_KEY))
    || parseStoredValue(sessionStorage.getItem(SESSION_KEY))
  );
}

export function clearStoredSession() {
  localStorage.removeItem(LOCAL_KEY);
  sessionStorage.removeItem(SESSION_KEY);
}
