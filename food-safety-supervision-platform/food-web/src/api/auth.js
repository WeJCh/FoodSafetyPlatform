import { request } from "./client";

export function login(payload) {
  return request("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function register(payload) {
  return request("/api/users/register/public", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function registerEnterprise(payload) {
  return request("/api/users/register/enterprise", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function createRegulator(payload, token) {
  return request("/api/admin/users/regulators", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function verify(token) {
  return request("/api/auth/verify", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}
