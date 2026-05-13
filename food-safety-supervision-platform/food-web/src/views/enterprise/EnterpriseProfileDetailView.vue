<template>
  <EnterpriseWorkspacePage
    active-key="profile"
    title="备案详情"
    subtitle="只读查看备案资料、附件与审核状态。"
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
          <p class="enterprise-page-hero__desc" style="margin: 0">集中查看主体资料、附件与审核流转。</p>
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
              <p class="enterprise-profile-registrar__meta">统一社会信用代码 {{ form.creditCode || "-" }}</p>
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

      <section class="enterprise-panel enterprise-key-supervision-panel">
        <div class="enterprise-panel__head">
          <div class="enterprise-panel__head-bar" />
          <h3>重点监管状态</h3>
        </div>
        <div class="enterprise-key-supervision-panel__summary">
          <div>
            <strong>{{ keySupervisionTitle }}</strong>
            <p>{{ keySupervisionDescription }}</p>
          </div>
          <span
            class="enterprise-key-supervision-panel__badge"
            :class="{ 'is-key': isKeyEnterprise, 'is-normal': !isKeyEnterprise }"
          >
            {{ isKeyEnterprise ? "KEY" : "NORMAL" }}
          </span>
        </div>
        <div v-if="keyReasonItems.length" class="enterprise-key-supervision-panel__list">
          <article
            v-for="(item, index) in keyReasonItems"
            :key="`${item.reasonType || 'reason'}-${index}`"
            class="enterprise-key-supervision-panel__item"
          >
            <div class="enterprise-key-supervision-panel__item-head">
              <strong>{{ item.reasonLabel || formatKeyReasonType(item.reasonType) }}</strong>
              <span>{{ formatTime(item.createTime) }}</span>
            </div>
            <p>{{ item.reasonDetail || "已触发重点监管规则。" }}</p>
          </article>
        </div>
        <div v-else class="enterprise-key-supervision-panel__empty">
          当前暂无已记录的重点监管原因；若后续触发风险规则或被监管确认，将在这里展示。
        </div>
      </section>

      <div class="enterprise-detail-layout">
        <div>
          <section class="enterprise-panel">
            <div class="enterprise-panel__head">
              <div class="enterprise-panel__head-bar" />
              <h3>审核信息</h3>
            </div>
            <div class="enterprise-detail-grid" style="margin-bottom: 16px">
              <div class="enterprise-readonly-field">
                <span>审核人</span>
                <div>{{ profile.approvedByName || "-" }}</div>
              </div>
              <div class="enterprise-readonly-field">
                <span>审核时间</span>
                <div>{{ formatTime(profile.approvedTime) }}</div>
              </div>
              <div class="enterprise-readonly-field">
                <span>包保责任人</span>
                <div>{{ profile.regulatorName || "-" }}</div>
              </div>
              <div class="enterprise-readonly-field enterprise-detail-item--full">
                <span>审核意见</span>
                <div>{{ profile.approvalComment || "-" }}</div>
              </div>
            </div>
          </section>

          <section class="enterprise-panel enterprise-panel--accent-top">
            <div class="enterprise-panel__head">
              <div class="enterprise-panel__head-bar" />
              <h3>基础信息</h3>
            </div>
            <div class="enterprise-detail-grid">
              <div class="enterprise-readonly-field">
                <span>企业全称</span>
                <div>{{ form.enterpriseName || "-" }}</div>
              </div>
              <div class="enterprise-readonly-field">
                <span>食品经营许可证编号</span>
                <div>{{ form.licenseNo || "-" }}</div>
              </div>
              <div class="enterprise-readonly-field">
                <span>统一社会信用代码</span>
                <div>{{ form.creditCode || "-" }}</div>
              </div>
              <div class="enterprise-readonly-field">
                <span>法定代表人</span>
                <div>{{ stageB.legalRepresentative || "-" }}</div>
              </div>
              <div class="enterprise-readonly-field enterprise-detail-item--full">
                <span>注册地址 / 行政区</span>
                <div>{{ existingRegionText || "-" }} {{ form.addressDetail || "" }}</div>
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
                  <span style="font-size: 11px; color: var(--muted)">{{ item.time ? formatTime(item.time) : "待更新" }}</span>
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
                <strong>{{ item.label || item.type }}</strong>
                <span>{{ item.name || "未命名附件" }}</span>
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
                <span>{{ form.principal || "-" }}</span>
              </div>
              <div class="enterprise-meta-row">
                <span>电话</span>
                <span>{{ form.principalPhone || "-" }}</span>
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
import { buildApprovalTimeline, createEmptyStageBData } from "./enterpriseProfileStageB";

const { enterpriseUser, token, handleSidebarNavigate, handleLogout } = useEnterpriseShellSession();
const profileLoaded = ref(false);
const existingRegionText = ref("");
const profile = reactive({
  approvalStatus: "",
  approvalComment: "",
  approvedTime: "",
  approvedByName: "",
  regulatorName: "",
  status: "",
  keyReasons: []
});
const form = reactive({ enterpriseName: "", licenseNo: "", creditCode: "", addressDetail: "", principal: "", principalPhone: "" });
const stageB = reactive(createEmptyStageBData());

const statusLabel = computed(() => getApprovalStatusLabel(profileLoaded.value, profile.approvalStatus));
const statusTone = computed(() => getApprovalStatusTone(profileLoaded.value, profile.approvalStatus));
const isKeyEnterprise = computed(() => profileLoaded.value && profile.status === "KEY");
const keyReasonItems = computed(() => (
  Array.isArray(profile.keyReasons) ? profile.keyReasons.slice(0, 5) : []
));
const approvalTimeline = computed(() =>
  buildApprovalTimeline({
    profileLoaded: profileLoaded.value,
    approvalStatus: profile.approvalStatus,
    approvalComment: profile.approvalComment,
    approvedTime: profile.approvedTime
  })
);

const keySupervisionTitle = computed(() => (
  isKeyEnterprise.value ? "当前已纳入重点监管" : "当前未纳入重点监管"
));

const keySupervisionDescription = computed(() => {
  if (keyReasonItems.value.length) {
    const latest = keyReasonItems.value[0];
    const label = latest.reasonLabel || formatKeyReasonType(latest.reasonType);
    const timeText = latest.createTime ? formatTime(latest.createTime) : "-";
    return `最近一次触发原因：${label}，记录时间 ${timeText}。`;
  }
  return isKeyEnterprise.value
    ? "当前企业已被纳入重点监管，请持续关注整改、检查和投诉等监管要求。"
    : "当前暂无已记录的重点监管原因，请持续维护备案信息并配合日常监管。";
});

function formatKeyReasonType(value) {
  const map = {
    COMPLAINT_OVERFLOW: "投诉过量",
    CONSECUTIVE_INSPECTION_FAIL: "连续检查不合格",
    SAMPLING_FAIL: "抽检不合格",
    RECTIFICATION_OVERDUE: "整改逾期",
    WARNING_TRIGGERED: "预警触发",
    MANUAL_SET: "人工设定"
  };
  return map[value] || value || "-";
}

function applyStageB(payload = {}) {
  stageB.legalRepresentative = payload.legalRepresentative || "";
  stageB.attachments = Array.isArray(payload.attachments) ? payload.attachments : [];
}

async function loadProfile() {
  try {
    const data = await fetchEnterpriseProfile(token.value);
    profile.approvalStatus = data.approvalStatus || "";
    profile.approvalComment = data.approvalComment || "";
    profile.approvedTime = data.approvedTime || "";
    profile.approvedByName = data.approvedByName || "";
    profile.regulatorName = data.regulatorName || "";
    profile.status = data.status || "";
    profile.keyReasons = Array.isArray(data.keyReasons) ? data.keyReasons : [];
    existingRegionText.value = data.regionPathText || "";
    form.enterpriseName = data.enterpriseName || "";
    form.licenseNo = data.licenseNo || "";
    form.creditCode = data.creditCode || "";
    form.addressDetail = data.addressDetail || "";
    form.principal = data.principal || "";
    form.principalPhone = data.principalPhone || "";
    applyStageB(data);
    profileLoaded.value = true;
  } catch {
    profileLoaded.value = false;
    existingRegionText.value = "";
    profile.approvalStatus = "";
    profile.approvalComment = "";
    profile.approvedTime = "";
    profile.approvedByName = "";
    profile.regulatorName = "";
    profile.status = "";
    profile.keyReasons = [];
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

<style scoped>
.enterprise-key-supervision-panel {
  margin-bottom: 20px;
}

.enterprise-key-supervision-panel__summary {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.enterprise-key-supervision-panel__summary strong {
  display: block;
  font-size: 18px;
  line-height: 1.4;
  color: var(--on-surface);
}

.enterprise-key-supervision-panel__summary p {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--on-surface-variant);
}

.enterprise-key-supervision-panel__badge {
  flex-shrink: 0;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.enterprise-key-supervision-panel__badge.is-key {
  color: #9f1239;
  background: rgba(244, 63, 94, 0.12);
}

.enterprise-key-supervision-panel__badge.is-normal {
  color: #166534;
  background: rgba(34, 197, 94, 0.12);
}

.enterprise-key-supervision-panel__list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.enterprise-key-supervision-panel__item {
  padding: 14px 16px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.03);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.enterprise-key-supervision-panel__item-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.enterprise-key-supervision-panel__item-head strong {
  font-size: 14px;
  color: var(--on-surface);
}

.enterprise-key-supervision-panel__item-head span {
  font-size: 12px;
  color: var(--muted);
  white-space: nowrap;
}

.enterprise-key-supervision-panel__item p,
.enterprise-key-supervision-panel__empty {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--on-surface-variant);
}

.enterprise-key-supervision-panel__empty {
  padding: 14px 16px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.03);
  border: 1px dashed rgba(148, 163, 184, 0.3);
}

@media (max-width: 720px) {
  .enterprise-key-supervision-panel__summary,
  .enterprise-key-supervision-panel__item-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
