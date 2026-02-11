<template>
  <AuthView
    v-if="view === 'auth'"
    @admin-login="handleAdminLogin"
    @enterprise-login="handleEnterpriseLogin"
    @public-login="handlePublicLogin"
    @regulator-login="handleRegulatorLogin"
  />
  <PublicHomeView
    v-else-if="view === 'public'"
    :public-user="publicUser"
    @logout="handleLogout"
    @open-complaint="handleOpenPublicComplaint"
    @open-track="handleOpenPublicTrack"
  />
  <PublicComplaintView
    v-else-if="view === 'public-complaint'"
    :public-user="publicUser"
    :public-token="publicToken"
    @back="handlePublicHome"
    @logout="handleLogout"
    @open-track="handleOpenPublicTrack"
  />
  <PublicComplaintTrackView
    v-else-if="view === 'public-track'"
    :public-user="publicUser"
    :initial-complaint-no="publicTrackParams.complaintNo"
    :initial-contact="publicTrackParams.contact"
    @back="handlePublicHome"
    @logout="handleLogout"
  />
  <AdminView
    v-else-if="view === 'admin'"
    :admin-user="adminUser"
    :token="adminToken"
    @logout="handleLogout"
  />
  <RegulatorAdminView
    v-else-if="view === 'regulator-admin'"
    :regulator-user="regulatorUser"
    :token="regulatorToken"
    :initial-section="regulatorReturnSection"
    @logout="handleLogout"
    @view-enterprise="handleViewEnterprise"
    @view-complaint="handleViewComplaint"
  />
  <RegulatorAdminComplaintDetailView
    v-else-if="view === 'regulator-admin-complaint'"
    :token="regulatorToken"
    :complaint-id="complaintDetailId"
    @back="handleBackFromComplaint"
  />
  <RegulatorEnforcerView
    v-else-if="view === 'regulator-enforcer'"
    :regulator-user="regulatorUser"
    :token="regulatorToken"
    :initial-section="regulatorReturnSection"
    @logout="handleLogout"
    @view-enterprise="handleViewEnterprise"
    @view-complaint="handleViewComplaint"
  />
  <RegulatorEnforcerComplaintDetailView
    v-else-if="view === 'regulator-enforcer-complaint'"
    :token="regulatorToken"
    :complaint-id="complaintDetailId"
    @back="handleBackFromComplaint"
  />
  <EnterpriseDetailView
    v-else-if="view === 'enterprise-detail'"
    :token="regulatorToken"
    :enterprise-id="enterpriseDetailId"
    @back="handleBackFromDetail"
  />
  <EnterpriseProfileView
    v-else
    :enterprise-user="enterpriseUser"
    :token="enterpriseToken"
    @logout="handleLogout"
  />
</template>

<script setup>
import { reactive, ref } from "vue";
import AdminView from "./views/AdminView.vue";
import AuthView from "./views/AuthView.vue";
import EnterpriseDetailView from "./views/EnterpriseDetailView.vue";
import EnterpriseProfileView from "./views/EnterpriseProfileView.vue";
import PublicComplaintTrackView from "./views/PublicComplaintTrackView.vue";
import PublicComplaintView from "./views/PublicComplaintView.vue";
import PublicHomeView from "./views/PublicHomeView.vue";
import RegulatorAdminView from "./views/RegulatorAdminView.vue";
import RegulatorAdminComplaintDetailView from "./views/RegulatorAdminComplaintDetailView.vue";
import RegulatorEnforcerView from "./views/RegulatorEnforcerView.vue";
import RegulatorEnforcerComplaintDetailView from "./views/RegulatorEnforcerComplaintDetailView.vue";
import { fetchRegulatorProfile } from "./api/regulation";
import { logout as logoutRequest } from "./api/auth";

const view = ref("auth");
const adminToken = ref("");
const adminUser = reactive({ username: "", userType: "", roles: [] });
const enterpriseToken = ref("");
const enterpriseUser = reactive({ username: "", userType: "", roles: [] });
const publicToken = ref("");
const publicUser = reactive({ username: "", userType: "", roles: [] });
const publicTrackParams = reactive({ complaintNo: "", contact: "" });
const regulatorToken = ref("");
const regulatorUser = reactive({ username: "", userType: "", roleType: "", roles: [] });
const enterpriseDetailId = ref("");
const complaintDetailId = ref("");
const returnView = ref("");
const complaintReturnView = ref("");
const regulatorReturnSection = ref("");

function handleAdminLogin(payload) {
  adminToken.value = payload.token;
  adminUser.username = payload.username;
  adminUser.userType = payload.userType;
  adminUser.roles = Array.isArray(payload.roles) ? payload.roles : [];
  view.value = "admin";
}

function handleEnterpriseLogin(payload) {
  enterpriseToken.value = payload.token;
  enterpriseUser.username = payload.username;
  enterpriseUser.userType = payload.userType;
  enterpriseUser.roles = Array.isArray(payload.roles) ? payload.roles : [];
  view.value = "enterprise";
}

function handlePublicLogin(payload) {
  publicToken.value = payload.token;
  publicUser.username = payload.username;
  publicUser.userType = payload.userType;
  publicUser.roles = Array.isArray(payload.roles) ? payload.roles : [];
  view.value = "public";
}

function handlePublicHome() {
  view.value = "public";
}

function handleOpenPublicComplaint() {
  view.value = "public-complaint";
}

function handleOpenPublicTrack(payload = {}) {
  publicTrackParams.complaintNo = payload.complaintNo || "";
  publicTrackParams.contact = payload.contact || "";
  view.value = "public-track";
}

async function handleRegulatorLogin(payload) {
  regulatorToken.value = payload.token;
  regulatorUser.username = payload.username;
  regulatorUser.userType = payload.userType;
  regulatorUser.roles = Array.isArray(payload.roles) ? payload.roles : [];
  regulatorUser.roleType = "";
  // 关键注释：优先使用登录返回的 roles 进行页面分流
  if (regulatorUser.roles.includes("REGULATOR_ADMIN")) {
    regulatorUser.roleType = "REGULATOR_ADMIN";
    view.value = "regulator-admin";
    return;
  }
  if (regulatorUser.roles.includes("REGULATOR_ENFORCER")) {
    regulatorUser.roleType = "REGULATOR_ENFORCER";
    view.value = "regulator-enforcer";
    return;
  }
  // 关键注释：兜底逻辑，兼容旧数据（无 roles 时仍用监管档案判定）
  const profile = await fetchRegulatorProfile(payload.token).catch(() => null);
  regulatorUser.roleType = profile?.roleType || "";
  view.value = profile?.roleType === "REGULATOR_ADMIN" ? "regulator-admin" : "regulator-enforcer";
}

function handleViewEnterprise(payload) {
  const resolvedId = typeof payload === "object" ? payload?.id : payload;
  if (!resolvedId) return;
  enterpriseDetailId.value = resolvedId;
  returnView.value = view.value;
  regulatorReturnSection.value = payload?.fromSection || "";
  view.value = "enterprise-detail";
}

function handleViewComplaint(payload) {
  const resolvedId = typeof payload === "object" ? payload?.id : payload;
  if (!resolvedId) return;
  complaintDetailId.value = resolvedId;
  complaintReturnView.value = view.value;
  regulatorReturnSection.value = payload?.fromSection || "complaints";
  view.value = view.value === "regulator-admin"
    ? "regulator-admin-complaint"
    : "regulator-enforcer-complaint";
}

function handleBackFromDetail() {
  view.value = returnView.value || "regulator-admin";
  enterpriseDetailId.value = "";
  returnView.value = "";
}

function handleBackFromComplaint() {
  view.value = complaintReturnView.value || "regulator-admin";
  complaintDetailId.value = "";
  complaintReturnView.value = "";
}

async function handleLogout() {
  const tokens = [adminToken.value, enterpriseToken.value, publicToken.value, regulatorToken.value].filter(Boolean);
  if (tokens.length) {
    try {
      await Promise.all(tokens.map((token) => logoutRequest(token)));
    } catch (error) {
      // Ignore logout errors to avoid blocking UI reset.
      console.warn("Logout request failed", error);
    }
  }
  adminToken.value = "";
  adminUser.username = "";
  adminUser.userType = "";
  adminUser.roles = [];
  enterpriseToken.value = "";
  enterpriseUser.username = "";
  enterpriseUser.userType = "";
  enterpriseUser.roles = [];
  publicToken.value = "";
  publicUser.username = "";
  publicUser.userType = "";
  publicUser.roles = [];
  publicTrackParams.complaintNo = "";
  publicTrackParams.contact = "";
  regulatorToken.value = "";
  regulatorUser.username = "";
  regulatorUser.userType = "";
  regulatorUser.roleType = "";
  regulatorUser.roles = [];
  enterpriseDetailId.value = "";
  complaintDetailId.value = "";
  returnView.value = "";
  complaintReturnView.value = "";
  regulatorReturnSection.value = "";
  view.value = "auth";
}
</script>
