// Frontend API client for ALIS backend.
// Drop this file into your frontend, for example: src/api/alisApi.js

export const API_BASE_URL =
  import.meta.env?.VITE_API_BASE_URL || "https://alis-backend-1.onrender.com";

const TOKEN_KEY = "alis_token";

export function setAuthToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  }
}

export function getAuthToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function clearAuthToken() {
  localStorage.removeItem(TOKEN_KEY);
}

function buildUrl(path, query) {
  const url = new URL(path, API_BASE_URL);

  if (query) {
    Object.entries(query).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, value);
      }
    });
  }

  return url.toString();
}

async function parseResponse(response) {
  const contentType = response.headers.get("content-type") || "";

  if (contentType.includes("application/json")) {
    return response.json();
  }

  const text = await response.text();
  return text || null;
}

async function request(path, options = {}) {
  const token = getAuthToken();
  const headers = new Headers(options.headers || {});

  if (!(options.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(buildUrl(path, options.query), {
    ...options,
    headers,
    body:
      options.body && !(options.body instanceof FormData)
        ? JSON.stringify(options.body)
        : options.body,
  });

  const data = await parseResponse(response);

  if (!response.ok) {
    const message =
      data?.message ||
      data?.error ||
      `Request failed with status ${response.status}`;
    throw new Error(message);
  }

  return data;
}

async function download(path, fileName, query) {
  const token = getAuthToken();
  const headers = new Headers();

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(buildUrl(path, query), { headers });

  if (!response.ok) {
    const data = await parseResponse(response);
    throw new Error(data?.message || `Download failed with status ${response.status}`);
  }

  const blob = await response.blob();
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(objectUrl);
}

export const authApi = {
  async register(payload) {
    const data = await request("/api/auth/register", {
      method: "POST",
      body: payload,
    });
    if (data.token) setAuthToken(data.token);
    return data;
  },

  async login({ email, password }) {
    const data = await request("/api/auth/login", {
      method: "POST",
      body: { email, password },
    });
    if (data.token) setAuthToken(data.token);
    return data;
  },

  logout() {
    clearAuthToken();
  },
};

export const clientApi = {
  getProfile() {
    return request("/api/client/profile");
  },

  updateProfile(payload) {
    return request("/api/client/profile", {
      method: "PUT",
      body: payload,
    });
  },

  deactivateProfile() {
    return request("/api/client/profile/deactivate", {
      method: "PATCH",
    });
  },

  deleteProfile() {
    return request("/api/client/profile", {
      method: "DELETE",
    });
  },

  uploadDocument(file) {
    const formData = new FormData();
    formData.append("file", file);

    return request("/api/client/documents/upload", {
      method: "POST",
      body: formData,
    });
  },

  getDocuments() {
    return request("/api/client/documents");
  },

  getDocument(documentId) {
    return request(`/api/client/documents/${documentId}`);
  },

  getDocumentReports(documentId) {
    return request(`/api/client/documents/${documentId}/reports`);
  },

  downloadReport(reportId) {
    return download(
      `/api/client/reports/${reportId}/download`,
      `Compliance_Report_${reportId}.pdf`
    );
  },
};

export const complianceApi = {
  analyzeDocument(documentId) {
    return request(`/api/compliance/analyze/${documentId}`, {
      method: "POST",
    });
  },

  getStatus(documentId) {
    return request(`/api/compliance/status/${documentId}`);
  },

  getResult(documentId) {
    return request(`/api/compliance/result/${documentId}`);
  },
};

export const rulesApi = {
  getRules() {
    return request("/api/rules");
  },

  getRule(ruleId) {
    return request(`/api/rules/${ruleId}`);
  },

  createRule(payload) {
    return request("/api/rules", {
      method: "POST",
      body: payload,
    });
  },

  updateRule(ruleId, payload) {
    return request(`/api/rules/${ruleId}`, {
      method: "PUT",
      body: payload,
    });
  },

  deleteRule(ruleId) {
    return request(`/api/rules/${ruleId}`, {
      method: "DELETE",
    });
  },
};

export const adminClientApi = {
  getClients({ page = 0, size = 20, sort = "createdAt", dir = "desc" } = {}) {
    return request("/api/admin/clients", {
      query: { page, size, sort, dir },
    });
  },

  getClient(clientId) {
    return request(`/api/admin/clients/${clientId}`);
  },

  filterClients(filter, { page = 0, size = 20 } = {}) {
    return request("/api/admin/clients/filter", {
      method: "POST",
      query: { page, size },
      body: filter,
    });
  },

  getClientsByRole(role, { page = 0, size = 20 } = {}) {
    return request("/api/admin/clients/by-role", {
      query: { role, page, size },
    });
  },

  getClientsByDateRange(from, to, { page = 0, size = 20 } = {}) {
    return request("/api/admin/clients/by-date", {
      query: { from, to, page, size },
    });
  },

  getDocumentCount(clientId) {
    return request(`/api/admin/clients/${clientId}/document-count`);
  },

  updateClient(clientId, payload) {
    return request(`/api/admin/clients/${clientId}`, {
      method: "PUT",
      body: payload,
    });
  },

  deleteClient(clientId) {
    return request(`/api/admin/clients/${clientId}`, {
      method: "DELETE",
    });
  },

  getSummaryStats() {
    return request("/api/admin/clients/reports/summary");
  },

  getRoleDistribution() {
    return request("/api/admin/clients/reports/role-distribution");
  },

  getRegistrationTrend(months = 12) {
    return request("/api/admin/clients/reports/registration-trend", {
      query: { months },
    });
  },

  getTopUploaders({ page = 0, size = 10 } = {}) {
    return request("/api/admin/clients/reports/top-uploaders", {
      query: { page, size },
    });
  },

  getInactiveClients() {
    return request("/api/admin/clients/reports/inactive");
  },
};
