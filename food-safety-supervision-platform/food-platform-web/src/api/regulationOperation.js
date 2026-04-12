import { API_BASE_URL, requestWithBase } from "./client";

const REGULATION_OPERATION_BASE_URL = import.meta.env.VITE_REG_OP_API_BASE || API_BASE_URL;

export function fetchMyRectifications(token, params = {}) {
  const search = new URLSearchParams();
  if (params.status) search.append("status", params.status);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_OPERATION_BASE_URL,
    `/api/regulation-operation/rectifications/my${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function submitMyRectification(token, id, payload) {
  return requestWithBase(
    REGULATION_OPERATION_BASE_URL,
    `/api/regulation-operation/rectifications/my/${id}/submit`,
    {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify(payload)
    }
  );
}

export function fetchRectificationDetail(token, id) {
  return requestWithBase(REGULATION_OPERATION_BASE_URL, `/api/regulation-operation/rectifications/${id}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function fetchRectificationActions(token, id) {
  return requestWithBase(REGULATION_OPERATION_BASE_URL, `/api/regulation-operation/rectifications/${id}/actions`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function fetchEnterpriseInspectionRecords(token, params = {}) {
  const search = new URLSearchParams();
  if (params.result) search.append("result", params.result);
  if (params.startDate) search.append("startDate", params.startDate);
  if (params.endDate) search.append("endDate", params.endDate);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_OPERATION_BASE_URL,
    `/api/regulation-operation/inspections/enterprise${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function fetchEnterpriseInspectionRecordDetail(token, id) {
  return requestWithBase(
    REGULATION_OPERATION_BASE_URL,
    `/api/regulation-operation/inspections/enterprise/${id}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}
