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

export function logout(token) {
  return request("/api/auth/logout", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    }
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

export function introspect(token) {
  return request("/api/auth/introspect", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}