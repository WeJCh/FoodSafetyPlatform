import { introspect, logout, verify } from "../api/auth";
import {
  clearStoredSession,
  getStoredSession,
  isAdminIdentity,
  isEnterpriseIdentity,
  isPublicIdentity,
  isRegulatorAdminIdentity,
  isRegulatorEnforcerIdentity,
  isRegulatorIdentity,
  isRememberedSession,
  persistSession
} from "./authSession";

let resolvedSession = null;
let bootstrapPromise = null;

export function getResolvedSession() {
  return resolvedSession;
}

export function getActiveSession() {
  return resolvedSession || getStoredSession();
}

export function commitResolvedSession(payload, remember = false) {
  resolvedSession = persistSession(payload, remember);
  bootstrapPromise = null;
  return resolvedSession;
}

export function dropResolvedSession() {
  resolvedSession = null;
  bootstrapPromise = null;
  clearStoredSession();
}

export function hasAuthenticatedSession(session = getActiveSession()) {
  return Boolean(session?.token);
}

export function hasAdminSession(session = getActiveSession()) {
  return hasAuthenticatedSession(session) && isAdminIdentity(session);
}

export function hasPublicSession(session = getActiveSession()) {
  return hasAuthenticatedSession(session) && isPublicIdentity(session);
}

export function hasEnterpriseSession(session = getActiveSession()) {
  return hasAuthenticatedSession(session) && isEnterpriseIdentity(session);
}

export function hasRegulatorSession(session = getActiveSession()) {
  return hasAuthenticatedSession(session) && isRegulatorIdentity(session);
}

export function hasRegulatorAdminSession(session = getActiveSession()) {
  return hasAuthenticatedSession(session) && isRegulatorAdminIdentity(session);
}

export function hasRegulatorEnforcerSession(session = getActiveSession()) {
  return hasAuthenticatedSession(session) && isRegulatorEnforcerIdentity(session);
}

export async function restoreResolvedSession() {
  if (resolvedSession) {
    return resolvedSession;
  }

  if (bootstrapPromise) {
    return bootstrapPromise;
  }

  const stored = getStoredSession();
  if (!stored?.token) {
    return null;
  }

  bootstrapPromise = (async () => {
    try {
      const verifyResult = await verify(stored.token);
      if (!verifyResult?.valid) {
        dropResolvedSession();
        return null;
      }

      const profile = await introspect(stored.token);
      if (profile?.valid === false) {
        dropResolvedSession();
        return null;
      }

      resolvedSession = persistSession(
        {
          token: stored.token,
          userId: profile?.userId ?? stored.userId ?? null,
          username: profile?.username || stored.username,
          userType: profile?.userType || stored.userType,
          roleType: stored.roleType || "",
          roles: Array.isArray(profile?.roles) ? profile.roles : stored.roles
        },
        isRememberedSession()
      );

      return resolvedSession;
    } catch {
      dropResolvedSession();
      return null;
    } finally {
      bootstrapPromise = null;
    }
  })();

  return bootstrapPromise;
}

export async function performLogout() {
  const session = getActiveSession();

  try {
    if (session?.token) {
      await logout(session.token);
    }
  } catch {
    // Ignore logout failures and always clear local session state.
  } finally {
    dropResolvedSession();
  }
}
