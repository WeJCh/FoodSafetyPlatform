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

export function createSamplingTask(token, payload) {
  return requestWithBase(REGULATION_OPERATION_BASE_URL, "/api/regulation-operation/sampling/tasks", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function assignSamplingTask(token, id, payload) {
  return requestWithBase(REGULATION_OPERATION_BASE_URL, `/api/regulation-operation/sampling/tasks/${id}/assign`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function fetchSamplingTasks(token, params = {}) {
  const search = new URLSearchParams();
  if (params.enterpriseName) search.append("enterpriseName", params.enterpriseName);
  if (params.status) search.append("status", params.status);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_OPERATION_BASE_URL,
    `/api/regulation-operation/sampling/tasks${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

/**
 * 后端暂无单条任务查询接口：分页拉取直至找到目标 ID（管理端列表数据量可控）。
 */
export async function findSamplingTaskById(token, id) {
  const target = String(id);
  let page = 1;
  const size = 50;
  const maxPages = 40;
  while (page <= maxPages) {
    const data = await fetchSamplingTasks(token, { page, size });
    const records = data?.records || [];
    const found = records.find((t) => String(t.id) === target);
    if (found) return found;
    const pages = data?.pages || 1;
    if (page >= pages || !records.length) break;
    page += 1;
  }
  return null;
}

export function fetchMySamplingTasks(token, params = {}) {
  const search = new URLSearchParams();
  if (params.status) search.append("status", params.status);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_OPERATION_BASE_URL,
    `/api/regulation-operation/sampling/tasks/my${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function submitSamplingResult(token, id, payload) {
  return requestWithBase(REGULATION_OPERATION_BASE_URL, `/api/regulation-operation/sampling/tasks/${id}/result`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function closeSamplingTask(token, id) {
  return requestWithBase(REGULATION_OPERATION_BASE_URL, `/api/regulation-operation/sampling/tasks/${id}/close`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function publishSamplingResult(token, id) {
  return requestWithBase(REGULATION_OPERATION_BASE_URL, `/api/regulation-operation/sampling/results/${id}/publish`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function offlineSamplingResult(token, id) {
  return requestWithBase(REGULATION_OPERATION_BASE_URL, `/api/regulation-operation/sampling/results/${id}/offline`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function fetchPublicSamplingResults(token, params = {}) {
  const search = new URLSearchParams();
  if (params.enterpriseName) search.append("enterpriseName", params.enterpriseName);
  if (params.result) search.append("result", params.result);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_OPERATION_BASE_URL,
    `/api/regulation-operation/public/sampling/results${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function fetchPublicSamplingResultDetail(token, id) {
  return requestWithBase(REGULATION_OPERATION_BASE_URL, `/api/regulation-operation/public/sampling/results/${id}`, {
    method: "GET",
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
