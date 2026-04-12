import { API_BASE_URL, requestWithBase } from "./client";

const FILE_BASE_URL = import.meta.env.VITE_REG_API_BASE || API_BASE_URL;

export function presignUpload(token, payload) {
  return requestWithBase(FILE_BASE_URL, "/api/files/presign", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  });
}
