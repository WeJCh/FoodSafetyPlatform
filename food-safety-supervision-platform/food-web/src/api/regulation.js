import { API_BASE_URL, requestWithBase } from "./client";

const REGULATION_BASE_URL =
  import.meta.env.VITE_REG_API_BASE || API_BASE_URL;

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

export function createRegulatorProfile(token, payload) {
  return requestWithBase(REGULATION_BASE_URL, "/api/regulation/regulators", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function fetchRegulatorProfiles(token, params = {}) {
  const search = new URLSearchParams();
  if (params.roleType) search.append("roleType", params.roleType);
  if (params.regionId) search.append("regionId", params.regionId);
  const query = search.toString();
  return requestWithBase(
    REGULATION_BASE_URL,
    `/api/regulation/regulators${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function updateRegulatorStatus(token, id, status) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/regulators/${id}/status`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ status })
  });
}

export function fetchEnterprises(token, params = {}) {
  const search = new URLSearchParams();
  if (params.enterpriseName) search.append("enterpriseName", params.enterpriseName);
  if (params.status) search.append("status", params.status);
  if (params.approvalStatus) search.append("approvalStatus", params.approvalStatus);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_BASE_URL,
    `/api/regulation/enterprises${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
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
