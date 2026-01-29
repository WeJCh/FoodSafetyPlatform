<template>
  <AuthView
    v-if="view === 'auth'"
    @admin-login="handleAdminLogin"
    @enterprise-login="handleEnterpriseLogin"
    @regulator-login="handleRegulatorLogin"
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
  />
  <RegulatorEnforcerView
    v-else-if="view === 'regulator-enforcer'"
    :regulator-user="regulatorUser"
    :token="regulatorToken"
    @logout="handleLogout"
    @view-enterprise="handleViewEnterprise"
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
import RegulatorAdminView from "./views/RegulatorAdminView.vue";
import RegulatorEnforcerView from "./views/RegulatorEnforcerView.vue";
import { fetchRegulatorProfile } from "./api/regulation";
import { logout as logoutRequest } from "./api/auth";

const view = ref("auth");
const adminToken = ref("");
const adminUser = reactive({ username: "", userType: "" });
const enterpriseToken = ref("");
const enterpriseUser = reactive({ username: "", userType: "" });
const regulatorToken = ref("");
const regulatorUser = reactive({ username: "", userType: "", roleType: "" });
const enterpriseDetailId = ref("");
const returnView = ref("");
const regulatorReturnSection = ref("");

function handleAdminLogin(payload) {
  adminToken.value = payload.token;
  adminUser.username = payload.username;
  adminUser.userType = payload.userType;
  view.value = "admin";
}

function handleEnterpriseLogin(payload) {
  enterpriseToken.value = payload.token;
  enterpriseUser.username = payload.username;
  enterpriseUser.userType = payload.userType;
  view.value = "enterprise";
}

async function handleRegulatorLogin(payload) {
  regulatorToken.value = payload.token;
  regulatorUser.username = payload.username;
  regulatorUser.userType = payload.userType;
  regulatorUser.roleType = "";
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

function handleBackFromDetail() {
  view.value = returnView.value || "regulator-admin";
  enterpriseDetailId.value = "";
  returnView.value = "";
}

async function handleLogout() {
  const tokens = [adminToken.value, enterpriseToken.value, regulatorToken.value].filter(Boolean);
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
  enterpriseToken.value = "";
  enterpriseUser.username = "";
  enterpriseUser.userType = "";
  regulatorToken.value = "";
  regulatorUser.username = "";
  regulatorUser.userType = "";
  regulatorUser.roleType = "";
  enterpriseDetailId.value = "";
  returnView.value = "";
  regulatorReturnSection.value = "";
  view.value = "auth";
}
</script>
