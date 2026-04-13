<template>
  <EnterpriseWorkspacePage
    active-key="profile"
    title="备案详情"
    subtitle="只读查看备案资料、附件与审核轨迹。"
    top-search-placeholder="搜索企业或档案..."
    :username="enterpriseUser.username"
    :user-type="enterpriseUser.userType"
    :status-label="statusLabel"
    :status-tone="statusTone"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <div class="enterprise-workspace-card">
      <nav class="enterprise-page-hero__crumb" style="margin-bottom: 18px" aria-label="面包屑">
        <span>企业工作台</span>
        <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
        <span class="is-current">备案详情</span>
      </nav>

      <header class="enterprise-page-hero" style="border-bottom: none; padding-bottom: 0; margin-bottom: 20px">
        <div>
          <h1 class="enterprise-page-hero__title" style="margin: 0 0 8px">企业备案详情</h1>
          <p class="enterprise-page-hero__desc" style="margin: 0">集中查看主体资料、附件与审核流转（只读）。</p>
        </div>
        <RouterLink class="primary enterprise-link-button" :to="{ name: 'enterprise-profile' }">返回资料编辑</RouterLink>
      </header>

      <section class="enterprise-profile-registrar">
        <div class="enterprise-profile-registrar__gradient">
          <div style="display: flex; align-items: center; gap: 18px; flex: 1; min-width: 0">
            <div class="enterprise-profile-registrar__icon">
              <span class="material-symbols-outlined is-filled" aria-hidden="true">{{ profile.approvalStatus === "REJECTED" ? "report_problem" : "verified" }}</span>
            </div>
            <div>
              <h2 class="enterprise-profile-registrar__title" style="font-size: 1.45rem">{{ form.enterpriseName || "企业名称待完善" }}</h2>
              <p class="enterprise-profile-registrar__meta">统一社会信用代码 {{ form.creditCode || "—" }}</p>
            </div>
          </div>
          <div class="enterprise-profile-registrar__status">
            <EnterpriseStatusChip :label="statusLabel" :tone="statusTone" />
            <p v-if="profile.approvedTime" class="enterprise-profile-registrar__audit-meta">审核时间 {{ formatTime(profile.approvedTime) }}</p>
          </div>
        </div>
      </section>

      <div v-if="profile.approvalStatus === 'REJECTED' && profile.approvalComment" class="enterprise-alert-rework">
        <span class="material-symbols-outlined is-filled" aria-hidden="true">report_problem</span>
        <div>
          <h4 style="margin: 0 0 6px; font-size: 15px">驳回原因</h4>
          <p style="margin: 0; font-size: 14px; line-height: 1.55">{{ profile.approvalComment }}</p>
        </div>
      </div>

      <div class="enterprise-detail-layout">
        <div>
          <section class="enterprise-panel enterprise-panel--accent-top">
            <div class="enterprise-panel__head">
              <div class="enterprise-panel__head-bar" />
              <h3>基础信息</h3>
            </div>
            <div class="enterprise-detail-grid">
              <div class="enterprise-readonly-field">
                <span>企业全称</span>
                <div>{{ form.enterpriseName || "—" }}</div>
              </div>
              <div class="enterprise-readonly-field">
                <span>食品经营许可证编号</span>
                <div>{{ form.licenseNo || "—" }}</div>
              </div>
              <div class="enterprise-readonly-field">
                <span>统一社会信用代码</span>
                <div>{{ form.creditCode || "—" }}</div>
              </div>
              <div class="enterprise-readonly-field">
                <span>法定代表人</span>
                <div>{{ stageB.legalRepresentative || "—" }}</div>
              </div>
              <div class="enterprise-readonly-field enterprise-detail-item--full">
                <span>注册地址 / 行政区</span>
                <div>{{ existingRegionText || "—" }} {{ form.addressDetail || "" }}</div>
              </div>
            </div>
          </section>

          <section class="enterprise-panel">
            <div class="enterprise-panel__head">
              <div class="enterprise-panel__head-bar" />
              <h3>审核状态流转</h3>
            </div>
            <div class="enterprise-audit-trail-v">
              <div v-for="item in approvalTimeline" :key="`${item.type}-${item.label}`" class="enterprise-audit-node">
                <span class="enterprise-audit-node__dot" :class="{ 'is-error': item.label?.includes('驳回') }" />
                <div style="display: flex; justify-content: space-between; gap: 10px; flex-wrap: wrap">
                  <strong style="font-size: 13px">{{ item.label }}</strong>
                  <span style="font-size: 11px; color: var(--muted)">{{ item.time || "待更新" }}</span>
                </div>
                <p v-if="item.note" style="margin: 6px 0 0; font-size: 12px; color: var(--on-surface-variant)">{{ item.note }}</p>
              </div>
            </div>
          </section>

          <section class="enterprise-panel">
            <div class="enterprise-panel__head">
              <div class="enterprise-panel__head-bar" />
              <h3>附件清单</h3>
            </div>
            <div v-if="!stageB.attachments.length" class="status info">当前还没有上传备案附件。</div>
            <div v-else class="enterprise-attachment-grid">
              <div v-for="item in stageB.attachments" :key="item.type" class="enterprise-attachment-card">
                <strong>{{ item.label }}</strong>
                <span>{{ item.name }}</span>
                <small>{{ item.uploadedAt ? formatTime(item.uploadedAt) : "已上传" }}</small>
                <a class="ghost enterprise-inline-link" :href="item.url" target="_blank" rel="noreferrer">查看附件</a>
              </div>
            </div>
          </section>
        </div>

        <aside class="enterprise-side-stack">
          <div class="enterprise-side-card">
            <div class="enterprise-side-card__head">联系人</div>
            <div class="enterprise-side-card__body">
              <div class="enterprise-meta-row">
                <span>负责人</span>
                <span>{{ form.principal || "—" }}</span>
              </div>
              <div class="enterprise-meta-row">
                <span>电话</span>
                <span>{{ form.principalPhone || "—" }}</span>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </EnterpriseWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import { fetchEnterpriseProfile } from "../../api/regulation";
import EnterpriseStatusChip from "../../components/enterprise/EnterpriseStatusChip.vue";
import EnterpriseWorkspacePage from "../../components/enterprise/EnterpriseWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { getApprovalStatusLabel, getApprovalStatusTone, useEnterpriseShellSession } from "./enterpriseShared";
import { buildApprovalTimeline, createEmptyStageBData, loadStageBData, mergeProfileWithStageB } from "./enterpriseProfileStageB";

const { enterpriseUser, token, handleSidebarNavigate, handleLogout } = useEnterpriseShellSession();
const profileLoaded = ref(false);
const existingRegionText = ref("");
const profile = reactive({ approvalStatus: "", approvalComment: "", approvedTime: "" });
const form = reactive({ enterpriseName: "", licenseNo: "", creditCode: "", addressDetail: "", principal: "", principalPhone: "" });
const stageB = reactive(createEmptyStageBData());

const statusLabel = computed(() => getApprovalStatusLabel(profileLoaded.value, profile.approvalStatus));
const statusTone = computed(() => getApprovalStatusTone(profileLoaded.value, profile.approvalStatus));
const approvalTimeline = computed(() =>
  buildApprovalTimeline({
    profileLoaded: profileLoaded.value,
    approvalStatus: profile.approvalStatus,
    approvalComment: profile.approvalComment,
    approvedTime: profile.approvedTime,
    history: stageB.history
  })
);

function getUserStorageKey() {
  return enterpriseUser.value?.userId || enterpriseUser.value?.username || "anonymous";
}

function applyStageB(payload = {}) {
  const merged = mergeProfileWithStageB({}, payload);
  stageB.legalRepresentative = merged.legalRepresentative;
  stageB.attachments = merged.attachments;
  stageB.history = merged.history;
}

async function loadProfile() {
  try {
    const data = await fetchEnterpriseProfile(token.value);
    const local = loadStageBData(getUserStorageKey());
    const merged = mergeProfileWithStageB(data, local);
    profile.approvalStatus = data.approvalStatus || "";
    profile.approvalComment = data.approvalComment || "";
    profile.approvedTime = data.approvedTime || "";
    existingRegionText.value = data.regionPathText || "";
    form.enterpriseName = data.enterpriseName || "";
    form.licenseNo = data.licenseNo || "";
    form.creditCode = data.creditCode || "";
    form.addressDetail = data.addressDetail || "";
    form.principal = data.principal || "";
    form.principalPhone = data.principalPhone || "";
    applyStageB(merged);
    stageB.attachments = Array.isArray(data.attachments) ? data.attachments : [];
    profileLoaded.value = true;
  } catch {
    profileLoaded.value = false;
    existingRegionText.value = "";
    profile.approvalStatus = "";
    profile.approvalComment = "";
    profile.approvedTime = "";
    form.enterpriseName = "";
    form.licenseNo = "";
    form.creditCode = "";
    form.addressDetail = "";
    form.principal = "";
    form.principalPhone = "";
    applyStageB(createEmptyStageBData());
  }
}

onMounted(() => {
  loadProfile();
});
</script>
