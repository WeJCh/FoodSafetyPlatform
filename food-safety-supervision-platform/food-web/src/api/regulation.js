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

export function fetchEligibleRegulators(token, regionId) {
  const query = regionId ? `?regionId=${regionId}` : "";
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/regulators/eligible${query}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function fetchRegulatorProfile(token) {
  return requestWithBase(REGULATION_BASE_URL, "/api/regulation/regulators/me", {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
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

export function fetchPublicEnterprises(token, params = {}) {
  const search = new URLSearchParams();
  if (params.enterpriseName) search.append("enterpriseName", params.enterpriseName);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_BASE_URL,
    `/api/regulation/public/enterprises${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

/**
 * 生成上传预签名地址
 * @param token 令牌
 * @param payload 请求体
 * @returns 请求结果
 */
export function presignUpload(token, payload) {
  return requestWithBase(REGULATION_BASE_URL, "/api/files/presign", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

/**
 * 获取行政区划列表
 * @param token 令牌
 * @param parentId 父级ID
 * @returns 请求结果
 */
export function fetchRegions(token, parentId = null) {
  const query = parentId === null ? "" : `?parentId=${parentId}`;
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/regions${query}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

/**
 * 获取行政区划路径
 * @param token 令牌
 * @param id 行政区划ID
 * @returns 请求结果
 */
export function fetchRegionPath(token, id) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/regions/${id}/path`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

/**
 * 获取待审批企业列表
 * @param token 令牌
 * @returns 请求结果
 */
export function fetchPendingEnterprises(token) {
  return requestWithBase(REGULATION_BASE_URL, "/api/regulation/enterprise/pending", {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

/**
 * 审批企业
 * @param token 令牌
 * @param id 企业ID
 * @param payload 请求体
 * @returns 请求结果
 */
export function approveEnterprise(token, id, payload = {}) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/enterprise/${id}/approve`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

/**
 * 拒绝企业
 * @param token 令牌
 * @param id 企业ID
 * @param payload 请求体
 * @returns 请求结果
 */
export function rejectEnterprise(token, id, payload = {}) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/enterprise/${id}/reject`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

/**
 * 批量审批企业
 * @param token 令牌
 * @param payload 请求体
 * @returns 请求结果
 */
export function approveEnterpriseBatch(token, payload = {}) {
  return requestWithBase(REGULATION_BASE_URL, "/api/regulation/enterprise/approve-batch", {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

/**
 * 批量拒绝企业
 * @param token 令牌
 * @param payload 请求体
 * @returns 请求结果
 */
export function rejectEnterpriseBatch(token, payload = {}) {
  return requestWithBase(REGULATION_BASE_URL, "/api/regulation/enterprise/reject-batch", {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

/**
 * 获取企业详情
 * @param token 令牌
 * @param id 企业ID
 * @returns 请求结果
 */
export function fetchEnterpriseDetail(token, id) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/enterprises/${id}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function createInspectionTask(token, payload) {
  return requestWithBase(REGULATION_BASE_URL, "/api/regulation/tasks", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function assignInspectionTask(token, id, payload) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/tasks/${id}/assign`, {
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
    REGULATION_BASE_URL,
    `/api/regulation/tasks${query ? `?${query}` : ""}`,
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
    REGULATION_BASE_URL,
    `/api/regulation/tasks/my${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function startInspectionTask(token, id) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/tasks/${id}/start`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function submitInspectionTask(token, id, payload) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/tasks/${id}/submit`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function closeInspectionTask(token, id) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/tasks/${id}/close`, {
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
    REGULATION_BASE_URL,
    `/api/regulation/rectifications/my${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function submitMyRectification(token, id, payload) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/rectifications/my/${id}/submit`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function fetchRectifications(token, params = {}) {
  const search = new URLSearchParams();
  if (params.status) search.append("status", params.status);
  if (params.enterpriseName) search.append("enterpriseName", params.enterpriseName);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_BASE_URL,
    `/api/regulation/rectifications${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function reviewRectification(token, id, payload) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/rectifications/${id}/review`, {
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
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/rectifications/${id}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function fetchRectificationActions(token, id) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/rectifications/${id}/actions`, {
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
    REGULATION_BASE_URL,
    `/api/regulation/inspections/my${query ? `?${query}` : ""}`,
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
    REGULATION_BASE_URL,
    `/api/regulation/inspections${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function fetchInspectionRecordDetail(token, id) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/inspections/${id}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function submitPublicComplaint(token, payload) {
  return requestWithBase(REGULATION_BASE_URL, "/api/regulation/complaints/public", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function fetchMyComplaints(token, params = {}) {
  const search = new URLSearchParams();
  if (params.status) search.append("status", params.status);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_BASE_URL,
    `/api/regulation/complaints/my${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function fetchMyComplaintDetail(token, id) {
  return requestWithBase(
    REGULATION_BASE_URL,
    `/api/regulation/complaints/my/${id}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function fetchComplaints(token, params = {}) {
  const search = new URLSearchParams();
  if (params.status) search.append("status", params.status);
  if (params.enterpriseName) search.append("enterpriseName", params.enterpriseName);
  if (params.assignedToName) search.append("assignedToName", params.assignedToName);
  if (params.assignedByName) search.append("assignedByName", params.assignedByName);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_BASE_URL,
    `/api/regulation/complaints${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function fetchComplaintDetail(token, id) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/complaints/${id}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function acceptComplaint(token, id) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/complaints/${id}/accept`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function assignComplaint(token, id, payload) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/complaints/${id}/assign`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function startComplaintProcess(token, id) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/complaints/${id}/process`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function handleComplaint(token, id, payload) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/complaints/${id}/handle`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function rejectComplaint(token, id, payload) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/complaints/${id}/reject`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}
