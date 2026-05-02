import { authApi, clientApi, rulesApi, adminClientApi } from "./alisApi";

async function loginAndLoadMyProfile() {
  await authApi.login({
    email: "client@example.com",
    password: "password123",
  });

  return clientApi.getProfile();
}

async function updateMyProfile() {
  return clientApi.updateProfile({
    fullName: "Updated Name",
    username: "updated_username",
  });
}

async function uploadMyDocument(file) {
  const upload = await clientApi.uploadDocument(file);
  return clientApi.getDocumentReports(upload.documentId);
}

async function legalPractitionerCreatesRule() {
  await authApi.login({
    email: "legal@example.com",
    password: "password123",
  });

  return rulesApi.createRule({
    title: "POPIA Privacy Clause",
    description: "Personal information must be processed lawfully.",
    category: "PRIVACY",
    keyword: "personal information",
  });
}

async function adminLoadsClients() {
  await authApi.login({
    email: "admin@example.com",
    password: "password123",
  });

  return adminClientApi.getClients({ page: 0, size: 20 });
}

export {
  loginAndLoadMyProfile,
  updateMyProfile,
  uploadMyDocument,
  legalPractitionerCreatesRule,
  adminLoadsClients,
};
