import { API_BASE_URL, requestWithBase } from "./client";

const REGULATION_BASE_URL = import.meta.env.VITE_REG_API_BASE || API_BASE_URL;

export function fetchEnterpriseProfile(token) {
  return requestWithBase(REGULATION_BASE_URL, "/api/regulation/enterprise/profile", {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function submitEnterpriseProfile(token, payload) {
  return requestWithBase(REGULATION_BASE_URL, "/api/regulation/enterprise/profile", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function fetchMyProducts(token) {
  return requestWithBase(REGULATION_BASE_URL, "/api/regulation/products/my", {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function createProduct(token, payload) {
  return requestWithBase(REGULATION_BASE_URL, "/api/regulation/products", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function updateProduct(token, id, payload) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/products/${id}`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function fetchRegions(token, parentId = null) {
  const query = parentId === null ? "" : `?parentId=${parentId}`;
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/regions${query}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}
