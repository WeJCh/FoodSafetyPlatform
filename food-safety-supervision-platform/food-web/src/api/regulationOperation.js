import { API_BASE_URL, requestWithBase } from "./client";

const REGULATION_OPERATION_BASE_URL =
  import.meta.env.VITE_REG_OP_API_BASE || API_BASE_URL;

export function createInspectionTask(token, payload) {
  return requestWithBase(REGULATION_OPERATION_BASE_URL, "/api/regulation-operation/tasks", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function assignInspectionTask(token, id, payload) {
  return requestWithBase(REGULATION_OPERATION_BASE_URL, `/api/regulation-operation/tasks/${id}/assign`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function fetchInspectionTasks(token, params = {}) {
  const search = new URLSearchParams();
  if (params.enterpriseName) search.append("enterpriseName", params.enterpriseName);
  if (params.status) search.append("status", params.status);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_OPERATION_BASE_URL,
    `/api/regulation-operation/tasks${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function fetchMyInspectionTasks(token, params = {}) {
  const search = new URLSearchParams();
  if (params.status) search.append("status", params.status);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_OPERATION_BASE_URL,
    `/api/regulation-operation/tasks/my${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function startInspectionTask(token, id) {
  return requestWithBase(REGULATION_OPERATION_BASE_URL, `/api/regulation-operation/tasks/${id}/start`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function submitInspectionTask(token, id, payload) {
  return requestWithBase(REGULATION_OPERATION_BASE_URL, `/api/regulation-operation/tasks/${id}/submit`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function closeInspectionTask(token, id) {
  return requestWithBase(REGULATION_OPERATION_BASE_URL, `/api/regulation-operation/tasks/${id}/close`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

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

export function fetchRectifications(token, params = {}) {
  const search = new URLSearchParams();
  if (params.status) search.append("status", params.status);
  if (params.enterpriseName) search.append("enterpriseName", params.enterpriseName);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_OPERATION_BASE_URL,
    `/api/regulation-operation/rectifications${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function reviewRectification(token, id, payload) {
  return requestWithBase(REGULATION_OPERATION_BASE_URL, `/api/regulation-operation/rectifications/${id}/review`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function confirmRectification(token, id) {
  return reviewRectification(token, id, { action: "CONFIRM" });
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

export function fetchMyInspectionRecords(token, params = {}) {
  const search = new URLSearchParams();
  if (params.enterpriseName) search.append("enterpriseName", params.enterpriseName);
  if (params.result) search.append("result", params.result);
  if (params.startDate) search.append("startDate", params.startDate);
  if (params.endDate) search.append("endDate", params.endDate);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_OPERATION_BASE_URL,
    `/api/regulation-operation/inspections/my${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function fetchInspectionRecords(token, params = {}) {
  const search = new URLSearchParams();
  if (params.enterpriseName) search.append("enterpriseName", params.enterpriseName);
  if (params.result) search.append("result", params.result);
  if (params.startDate) search.append("startDate", params.startDate);
  if (params.endDate) search.append("endDate", params.endDate);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_OPERATION_BASE_URL,
    `/api/regulation-operation/inspections${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function fetchInspectionRecordDetail(token, id) {
  return requestWithBase(REGULATION_OPERATION_BASE_URL, `/api/regulation-operation/inspections/${id}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function fetchMyRegulatorRectifications(token, params = {}) {
  const search = new URLSearchParams();
  if (params.status) search.append("status", params.status);
  if (params.enterpriseName) search.append("enterpriseName", params.enterpriseName);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_OPERATION_BASE_URL,
    `/api/regulation-operation/rectifications/regulator/my${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}
