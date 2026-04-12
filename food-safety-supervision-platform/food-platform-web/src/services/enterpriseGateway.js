import { presignUpload } from "../api/file";
import {
  createProduct,
  fetchEnterpriseProfile,
  fetchMyProducts,
  submitEnterpriseProfile,
  updateProduct
} from "../api/regulation";
import {
  fetchEnterpriseInspectionRecordDetail,
  fetchEnterpriseInspectionRecords,
  fetchMyRectifications,
  fetchRectificationActions,
  fetchRectificationDetail,
  submitMyRectification
} from "../api/regulationOperation";
import {
  enterpriseInspectionsSeed,
  enterpriseProductsSeed,
  enterpriseProfileSeed,
  enterpriseRectificationsSeed,
  findSeedInspection,
  findSeedProduct,
  findSeedRectification,
  inspectionItemsSeed,
  rectificationActionsSeed
} from "../data/enterpriseSeed";

function paginate(records, page = 1, size = 8) {
  const resolvedPage = Number(page) || 1;
  const resolvedSize = Number(size) || 8;
  const start = (resolvedPage - 1) * resolvedSize;
  const items = records.slice(start, start + resolvedSize);
  const pages = Math.max(1, Math.ceil(records.length / resolvedSize));
  return {
    records: items,
    total: records.length,
    page: resolvedPage,
    size: resolvedSize,
    pages
  };
}

function withFallback(asyncTask, fallback) {
  return async (...args) => {
    try {
      return await asyncTask(...args);
    } catch {
      return typeof fallback === "function" ? fallback(...args) : fallback;
    }
  };
}

export const loadEnterpriseProfile = withFallback(
  async (token) => {
    if (!token) throw new Error("missing token");
    return await fetchEnterpriseProfile(token);
  },
  () => enterpriseProfileSeed
);

export async function saveEnterpriseProfile(token, payload) {
  const adaptedPayload = {
    enterpriseName: payload.enterpriseName,
    licenseNo: payload.licenseNo,
    addressDetail: payload.addressDetail,
    principal: payload.principal,
    principalPhone: payload.principalPhone
  };
  if (!token) {
    return { ...enterpriseProfileSeed, ...payload };
  }
  return submitEnterpriseProfile(token, adaptedPayload);
}

export const loadProducts = withFallback(
  async (token) => {
    if (!token) throw new Error("missing token");
    return await fetchMyProducts(token);
  },
  () => enterpriseProductsSeed
);

export async function loadProductById(token, id) {
  const products = await loadProducts(token);
  return products.find((item) => String(item.id) === String(id)) || findSeedProduct(id);
}

export async function saveProduct(token, id, payload) {
  const adaptedPayload = {
    productName: payload.productName,
    category: payload.category,
    specification: payload.specification,
    status: payload.status,
    remark: payload.remark
  };
  if (!token) {
    return { id: id || `mock-${Date.now()}`, ...payload, updateTime: new Date().toISOString() };
  }
  return id ? updateProduct(token, id, adaptedPayload) : createProduct(token, adaptedPayload);
}

export const loadInspections = withFallback(
  async (token, params = {}) => {
    if (!token) throw new Error("missing token");
    return await fetchEnterpriseInspectionRecords(token, params);
  },
  (_, params = {}) => {
    let records = [...enterpriseInspectionsSeed];
    if (params.result) {
      records = records.filter((item) => item.result === params.result);
    }
    if (params.startDate) {
      records = records.filter((item) => item.inspectionDate >= params.startDate);
    }
    if (params.endDate) {
      records = records.filter((item) => item.inspectionDate <= params.endDate);
    }
    return paginate(records, params.page, params.size);
  }
);

export const loadInspectionDetail = withFallback(
  async (token, id) => {
    if (!token) throw new Error("missing token");
    return await fetchEnterpriseInspectionRecordDetail(token, id);
  },
  (_, id) => {
    const record = findSeedInspection(id);
    return record
      ? {
          record,
          items: inspectionItemsSeed[id] || []
        }
      : null;
  }
);

export const loadRectifications = withFallback(
  async (token, params = {}) => {
    if (!token) throw new Error("missing token");
    return await fetchMyRectifications(token, params);
  },
  (_, params = {}) => {
    let records = [...enterpriseRectificationsSeed];
    if (params.status) {
      records = records.filter((item) => item.status === params.status);
    }
    return paginate(records, params.page, params.size);
  }
);

export async function loadRectificationBundle(token, id) {
  if (!token) {
    const detail = findSeedRectification(id);
    return {
      detail,
      actions: rectificationActionsSeed[id] || []
    };
  }

  try {
    const [detail, actions] = await Promise.all([
      fetchRectificationDetail(token, id),
      fetchRectificationActions(token, id)
    ]);
    return {
      detail,
      actions: Array.isArray(actions) ? actions : []
    };
  } catch {
    return {
      detail: findSeedRectification(id),
      actions: rectificationActionsSeed[id] || []
    };
  }
}

export async function submitRectificationProgress(token, id, payload) {
  if (!token) {
    return {
      success: true,
      id,
      ...payload
    };
  }
  return submitMyRectification(token, id, payload);
}

export async function uploadRectificationEvidence(token, file) {
  if (!token) {
    return {
      fileUrl: `mock://${file.name}`
    };
  }

  const payload = {
    filename: file.name,
    contentType: file.type || "application/octet-stream",
    size: file.size,
    bizType: "RECTIFICATION"
  };
  const presign = await presignUpload(token, payload);
  const response = await fetch(presign.uploadUrl, {
    method: "PUT",
    headers: {
      "Content-Type": payload.contentType
    },
    body: file
  });

  if (!response.ok) {
    throw new Error(`上传失败 (${response.status})`);
  }

  return {
    fileUrl: presign.fileUrl
  };
}

export async function loadDashboardSnapshot(token) {
  const [profile, products, inspectionsPage, rectificationsPage] = await Promise.all([
    loadEnterpriseProfile(token),
    loadProducts(token),
    loadInspections(token, { page: 1, size: 3 }),
    loadRectifications(token, { page: 1, size: 3 })
  ]);

  return {
    profile,
    products,
    inspections: inspectionsPage.records || [],
    rectifications: rectificationsPage.records || []
  };
}

