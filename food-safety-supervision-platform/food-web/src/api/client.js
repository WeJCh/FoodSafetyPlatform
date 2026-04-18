import { clearStoredSession } from "../session/authSession";

const DEFAULT_BASE_URL = "http://localhost:8080";
export const UNAUTHORIZED_EVENT = "food-web:unauthorized";
export const API_BASE_URL = import.meta.env.VITE_API_BASE || DEFAULT_BASE_URL;

export async function request(path, options = {}) {
  const { baseUrl, headers, ...rest } = options;
  const response = await fetch(`${baseUrl || API_BASE_URL}${path}`, {
    ...rest,
    headers: {
      "Content-Type": "application/json",
      ...(headers || {})
    }
  });

  const text = await response.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }

  if (!response.ok) {
    if (response.status === 401) {
      clearStoredSession();
      if (typeof window !== "undefined") {
        window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT, {
          detail: {
            path,
            status: response.status
          }
        }));
      }
    }
    const message = data?.message || `Request failed (${response.status})`;
    throw new Error(message);
  }

  if (data && typeof data.code === "number" && data.code !== 0) {
    throw new Error(data.message || "Request failed");
  }

  if (response.ok && data == null) {
    throw new Error(`Empty or non-JSON response from ${path}`);
  }

  return data?.data ?? data;
}

export function requestWithBase(baseUrl, path, options = {}) {
  return request(path, { ...options, baseUrl });
}
