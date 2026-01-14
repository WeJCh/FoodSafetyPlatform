const DEFAULT_BASE_URL = "http://localhost:8080";

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
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const message = data?.message || `Request failed (${response.status})`;
    throw new Error(message);
  }

  if (data && typeof data.code === "number" && data.code !== 0) {
    throw new Error(data.message || "Request failed");
  }

  return data?.data ?? data;
}

export function requestWithBase(baseUrl, path, options = {}) {
  return request(path, { ...options, baseUrl });
}
