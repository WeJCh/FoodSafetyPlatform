import { introspect, logout, verify } from "../api/auth";
import {
  clearStoredSession,
  getStoredSession,
  isEnterpriseIdentity,
  isRememberedSession,
  persistSession
} from "./authSession";

let resolvedSession = null;
let bootstrapPromise = null;

export function getResolvedSession() {
  return resolvedSession;
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

export function hasEnterpriseSession(session = resolvedSession || getStoredSession()) {
  return Boolean(session?.token) && isEnterpriseIdentity(session);
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
      if (profile && profile.valid === false) {
        dropResolvedSession();
        return null;
      }

      resolvedSession = persistSession(
        {
          token: stored.token,
          username: profile?.username || stored.username,
          userType: profile?.userType || stored.userType,
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
  const session = getStoredSession();
  try {
    if (session?.token) {
      await logout(session.token);
    }
  } catch {
    // 登出失败时仍然清理本地会话，避免前端保留脏状态。
  } finally {
    dropResolvedSession();
  }
}