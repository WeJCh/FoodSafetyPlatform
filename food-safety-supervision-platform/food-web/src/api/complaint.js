import { API_BASE_URL, requestWithBase } from "./client";

const COMPLAINT_BASE_URL =
  import.meta.env.VITE_COMPLAINT_API_BASE || API_BASE_URL;

export function submitPublicComplaint(token, payload) {
  return requestWithBase(COMPLAINT_BASE_URL, "/api/complaints/public", {
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
    COMPLAINT_BASE_URL,
    `/api/complaints/my${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function fetchMyComplaintDetail(token, id) {
  return requestWithBase(COMPLAINT_BASE_URL, `/api/complaints/my/${id}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
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
    COMPLAINT_BASE_URL,
    `/api/complaints${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export function fetchComplaintDetail(token, id) {
  return requestWithBase(COMPLAINT_BASE_URL, `/api/complaints/${id}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function acceptComplaint(token, id) {
  return requestWithBase(COMPLAINT_BASE_URL, `/api/complaints/${id}/accept`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function assignComplaint(token, id, payload) {
  return requestWithBase(COMPLAINT_BASE_URL, `/api/complaints/${id}/assign`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function startComplaintProcess(token, id) {
  return requestWithBase(COMPLAINT_BASE_URL, `/api/complaints/${id}/process`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export function handleComplaint(token, id, payload) {
  return requestWithBase(COMPLAINT_BASE_URL, `/api/complaints/${id}/handle`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}

export function rejectComplaint(token, id, payload) {
  return requestWithBase(COMPLAINT_BASE_URL, `/api/complaints/${id}/reject`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}
