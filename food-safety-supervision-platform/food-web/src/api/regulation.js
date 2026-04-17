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

export function fetchRegulatorProfileByUserId(token, userId) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/regulators/user/${userId}`, {
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

export function fetchPublicEnterpriseDetail(token, id) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/public/enterprises/${id}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function fetchBulletins(token, params = {}) {
  const search = new URLSearchParams();
  if (params.keyword) search.append("keyword", params.keyword);
  if (params.category) search.append("category", params.category);
  if (params.status) search.append("status", params.status);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_BASE_URL,
    `/api/regulation/bulletins${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function fetchBulletinDetail(token, id) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/bulletins/${id}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function createBulletin(token, payload) {
  return requestWithBase(REGULATION_BASE_URL, "/api/regulation/bulletins", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function updateBulletin(token, id, payload) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/bulletins/${id}`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function publishBulletin(token, id) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/bulletins/${id}/publish`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function offlineBulletin(token, id) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/bulletins/${id}/offline`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function fetchPublicBulletins(token, params = {}) {
  const search = new URLSearchParams();
  if (params.keyword) search.append("keyword", params.keyword);
  if (params.category) search.append("category", params.category);
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  const query = search.toString();
  return requestWithBase(
    REGULATION_BASE_URL,
    `/api/regulation/public/bulletins${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function fetchPublicBulletinDetail(token, id) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/public/bulletins/${id}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
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

export function fetchEnterpriseProducts(token, enterpriseId) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/enterprises/${enterpriseId}/products`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

/**
 * 分页查询预警记录
 * @param token 令牌
 * @param params 查询参数
 * @returns 请求结果
 */
export function fetchWarningRecords(token, params = {}) {
  const search = new URLSearchParams();
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  if (params.status) search.append("status", params.status);
  if (params.level) search.append("level", params.level);
  if (params.warningType) search.append("warningType", params.warningType);
  if (params.bizType) search.append("bizType", params.bizType);
  if (params.bizId) search.append("bizId", params.bizId);
  if (params.keyword) search.append("keyword", params.keyword);
  const query = search.toString();
  return requestWithBase(
    REGULATION_BASE_URL,
    `/api/regulation/warnings${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

/**
 * 查询预警详情
 * @param token 令牌
 * @param id 预警ID
 * @returns 请求结果
 */
export function fetchWarningRecordDetail(token, id) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/warnings/${id}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

/**
 * 处理预警动作
 * @param token 令牌
 * @param id 预警ID
 * @param payload 动作参数
 * @returns 请求结果
 */
export function processWarningRecord(token, id, payload) {
  const actionType = String(payload?.actionType || "").toUpperCase();
  const actionPathMap = {
    ASSIGN: "assign",
    PROCESS: "process",
    RESOLVE: "resolve"
  };
  const actionPath = actionPathMap[actionType];
  if (!actionPath) {
    throw new Error("unsupported warning actionType");
  }
  const body = actionType === "ASSIGN"
    ? {
        assignedTo: payload?.assignedTo,
        actionComment: payload?.actionComment
      }
    : {
        actionComment: payload?.actionComment
      };
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/warnings/${id}/${actionPath}`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(body)
  });
}

/**
 * 查询当前执法员可见预警列表
 * @param token 令牌
 * @param params 查询参数
 * @returns 请求结果
 */
export function fetchMyWarningRecords(token, params = {}) {
  const search = new URLSearchParams();
  if (params.page) search.append("page", params.page);
  if (params.size) search.append("size", params.size);
  if (params.status) search.append("status", params.status);
  if (params.level) search.append("level", params.level);
  if (params.warningType) search.append("warningType", params.warningType);
  if (params.bizType) search.append("bizType", params.bizType);
  if (params.bizId) search.append("bizId", params.bizId);
  if (params.keyword) search.append("keyword", params.keyword);
  const query = search.toString();
  return requestWithBase(
    REGULATION_BASE_URL,
    `/api/regulation/warnings/my${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

/**
 * 查询当前执法员可见预警详情
 * @param token 令牌
 * @param id 预警ID
 * @returns 请求结果
 */
export function fetchMyWarningDetail(token, id) {
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/warnings/my/${id}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

/**
 * 处理当前执法员可操作预警
 * @param token 令牌
 * @param id 预警ID
 * @param payload 动作参数
 * @returns 请求结果
 */
export function processMyWarning(token, id, payload) {
  const actionType = String(payload?.actionType || "").toUpperCase();
  const actionPathMap = {
    PROCESS: "process",
    RESOLVE: "resolve"
  };
  const actionPath = actionPathMap[actionType];
  if (!actionPath) {
    throw new Error("unsupported warning actionType");
  }
  return requestWithBase(REGULATION_BASE_URL, `/api/regulation/warnings/my/${id}/${actionPath}`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({
      actionComment: payload?.actionComment
    })
  });
}

function buildWarningStatsQuery(params = {}) {
  const search = new URLSearchParams();
  const keys = [
    "startTime",
    "endTime",
    "warningType",
    "bizType",
    "level",
    "status",
    "regionId",
    "regionIds",
    "ownerRegulatorId",
    "topN",
    "trendDays",
    "overdueHours"
  ];
  keys.forEach((key) => {
    const value = params[key];
    if (value !== undefined && value !== null && value !== "") {
      search.append(key, value);
    }
  });
  return search.toString();
}

export function fetchWarningOverview(token, params = {}) {
  const query = buildWarningStatsQuery(params);
  return requestWithBase(
    REGULATION_BASE_URL,
    `/api/query/warnings/overview${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function fetchWarningTrend(token, params = {}) {
  const query = buildWarningStatsQuery(params);
  return requestWithBase(
    REGULATION_BASE_URL,
    `/api/query/warnings/trend${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function fetchWarningTypes(token, params = {}) {
  const query = buildWarningStatsQuery(params);
  return requestWithBase(
    REGULATION_BASE_URL,
    `/api/query/warnings/types${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function fetchWarningEfficiency(token, params = {}) {
  const query = buildWarningStatsQuery(params);
  return requestWithBase(
    REGULATION_BASE_URL,
    `/api/query/warnings/efficiency${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

function buildSupervisionOverviewQuery(params = {}) {
  const search = new URLSearchParams();
  ["regionId", "regionIds", "ownerRegulatorId"].forEach((key) => {
    const value = params[key];
    if (value !== undefined && value !== null && value !== "") {
      search.append(key, value);
    }
  });
  return search.toString();
}

export function fetchSupervisionOverview(token, params = {}) {
  const query = buildSupervisionOverviewQuery(params);
  return requestWithBase(
    REGULATION_BASE_URL,
    `/api/query/supervision/overview${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}
