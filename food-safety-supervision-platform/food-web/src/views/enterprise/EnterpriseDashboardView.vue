<template>
  <EnterpriseWorkspacePage
    active-key="dashboard"
    title="企业工作台"
    :subtitle="`欢迎回来，当前待处理整改任务共 ${pendingRectificationCount} 项。`"
    top-search-placeholder="搜索检查记录、产品或任务..."
    :username="enterpriseUser.username"
    :user-type="enterpriseUser.userType"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="enterprise-dashboard-page">
      <div class="enterprise-dashboard-page__head">
        <h2>企业工作台</h2>
        <p>以下数据均按后端当前统计口径展示，列表区域仅显示最近记录预览。</p>
      </div>

      <div class="enterprise-dashboard-bento">
        <section class="enterprise-dashboard-status-panel">
          <div class="enterprise-dashboard-status-panel__main">
            <div>
              <div class="enterprise-dashboard-status-panel__eyebrow">当前备案状态</div>
              <div class="enterprise-dashboard-status-panel__headline">
                <h3>{{ heroStatusTitle }}</h3>
                <span class="enterprise-dashboard-status-badge" :class="`is-${approvalTone}`">
                  {{ approvalCode }}
                </span>
              </div>
              <p class="enterprise-dashboard-status-panel__desc">{{ heroDescription }}</p>
            </div>

            <div class="enterprise-dashboard-status-panel__actions">
              <RouterLink class="primary enterprise-link-button" :to="primaryAction.to">
                {{ primaryAction.label }}
              </RouterLink>
            </div>
          </div>

          <div class="enterprise-dashboard-status-panel__certificate">
            <div class="enterprise-dashboard-status-panel__certificate-overlay"></div>
            <div class="enterprise-dashboard-status-panel__certificate-body">
              <span class="enterprise-dashboard-status-panel__certificate-icon">G</span>
              <span class="enterprise-dashboard-status-panel__certificate-label">VALID UNTIL</span>
              <strong>{{ validUntilText }}</strong>
            </div>
          </div>
        </section>

        <aside class="enterprise-dashboard-side-stack">
          <article class="enterprise-dashboard-stat-card">
            <div>
              <p>在册产品数量</p>
              <strong>{{ productCount }}</strong>
            </div>
            <span class="enterprise-dashboard-stat-card__icon">产品</span>
          </article>

          <article class="enterprise-dashboard-stat-card enterprise-dashboard-stat-card--danger">
            <div>
              <p>待整改任务</p>
              <strong>{{ pendingRectificationCount.toString().padStart(2, "0") }}</strong>
            </div>
            <span class="enterprise-dashboard-stat-card__icon">风险</span>
          </article>

          <article class="enterprise-dashboard-stat-card">
            <div>
              <p>不合格检查记录</p>
              <strong>{{ recentInspectionAlertCount.toString().padStart(2, "0") }}</strong>
            </div>
            <span class="enterprise-dashboard-stat-card__icon">检查</span>
          </article>

          <RouterLink class="enterprise-dashboard-quick-create" :to="{ name: 'enterprise-product-create' }">
            <span>快速新增产品</span>
            <strong>立即录入</strong>
          </RouterLink>
        </aside>

        <section class="enterprise-dashboard-risk-panel">
          <div class="enterprise-dashboard-risk-panel__head">
            <div>
              <p class="enterprise-dashboard-risk-panel__eyebrow">重点监管状态</p>
              <h3>{{ keySupervisionTitle }}</h3>
              <p class="enterprise-dashboard-risk-panel__desc">{{ keySupervisionDescription }}</p>
            </div>
            <span
              class="enterprise-dashboard-risk-panel__badge"
              :class="{ 'is-key': isKeyEnterprise, 'is-normal': !isKeyEnterprise }"
            >
              {{ isKeyEnterprise ? "KEY" : "NORMAL" }}
            </span>
          </div>

          <div v-if="keyReasonItems.length" class="enterprise-dashboard-risk-panel__list">
            <article
              v-for="(item, index) in keyReasonItems"
              :key="`${item.reasonType || 'reason'}-${index}`"
              class="enterprise-dashboard-risk-panel__item"
            >
              <div class="enterprise-dashboard-risk-panel__item-head">
                <strong>{{ item.reasonLabel || formatKeyReasonType(item.reasonType) }}</strong>
                <span>{{ formatTime(item.createTime) }}</span>
              </div>
              <p>{{ item.reasonDetail || "已触发重点监管规则。" }}</p>
            </article>
          </div>
          <div v-else class="enterprise-dashboard-risk-panel__empty">
            当前暂无已记录的重点监管原因，若后续触发风险规则或被监管确认，将在这里展示。
          </div>

          <div class="enterprise-dashboard-risk-panel__actions">
            <RouterLink class="ghost enterprise-link-button" :to="{ name: 'enterprise-profile-detail' }">
              查看备案详情
            </RouterLink>
            <RouterLink
              v-if="isKeyEnterprise"
              class="primary enterprise-link-button"
              :to="{ name: 'enterprise-rectifications' }"
            >
              查看整改任务
            </RouterLink>
          </div>
        </section>

        <section class="enterprise-dashboard-list-panel">
          <div class="enterprise-dashboard-list-panel__head">
            <h3>近期检查记录</h3>
            <RouterLink class="enterprise-inline-link" :to="{ name: 'enterprise-inspections' }">查看全部</RouterLink>
          </div>

          <div v-if="!recentInspections.length" class="enterprise-dashboard-list-panel__empty">
            当前没有可展示的检查记录。
          </div>

          <div v-else class="enterprise-dashboard-inspection-list">
            <article
              v-for="item in recentInspections"
              :key="item.id"
              class="enterprise-dashboard-inspection-row"
            >
              <div class="enterprise-dashboard-inspection-row__main">
                <div class="enterprise-dashboard-inspection-row__icon">
                  {{ getInspectionGlyph(item) }}
                </div>
                <div>
                  <p class="enterprise-dashboard-inspection-row__title">
                    {{ item.problemDesc || "本次检查未记录问题描述" }}
                  </p>
                  <p class="enterprise-dashboard-inspection-row__meta">
                    {{ item.inspectionDate || formatTime(item.updateTime) }}
                  </p>
                </div>
              </div>

              <div class="enterprise-dashboard-inspection-row__side">
                <span
                  class="enterprise-dashboard-inspection-row__result"
                  :class="{ 'is-danger': item.result === 'FAIL' }"
                >
                  {{ formatInspectionResult(item.result) }}
                </span>
                <RouterLink
                  class="enterprise-dashboard-inspection-row__more"
                  :to="{ name: 'enterprise-inspection-detail', params: { inspectionId: item.id } }"
                >
                  详情
                </RouterLink>
              </div>
            </article>
          </div>
        </section>

        <aside class="enterprise-dashboard-actions-panel">
          <div class="enterprise-dashboard-actions-panel__block">
            <h3>快捷入口</h3>
            <div class="enterprise-dashboard-actions-list">
              <RouterLink class="enterprise-dashboard-action" :to="{ name: 'enterprise-profile' }">
                <span class="enterprise-dashboard-action__icon">档</span>
                <div>
                  <strong>更新备案信息</strong>
                  <p>维护企业基础档案</p>
                </div>
              </RouterLink>

              <RouterLink class="enterprise-dashboard-action" :to="{ name: 'enterprise-product-create' }">
                <span class="enterprise-dashboard-action__icon">新</span>
                <div>
                  <strong>新增监管产品</strong>
                  <p>申报新的受监管产品条目</p>
                </div>
              </RouterLink>

              <RouterLink class="enterprise-dashboard-action" :to="{ name: 'enterprise-rectifications' }">
                <span class="enterprise-dashboard-action__icon">整</span>
                <div>
                  <strong>查看整改进展</strong>
                  <p>提交或补充整改落实情况</p>
                </div>
              </RouterLink>
            </div>
          </div>

          <div class="enterprise-dashboard-actions-panel__notice">
            <div class="enterprise-dashboard-actions-panel__notice-dot"></div>
            <strong>监管动态提醒</strong>
            <p>请持续关注近期检查、整改与备案状态变化，及时补充监管要求的资料与附件。</p>
          </div>
        </aside>
      </div>

      <footer class="enterprise-dashboard-footer">
        <div class="enterprise-dashboard-footer__meta">
          <div class="enterprise-dashboard-footer__item">
            <span class="enterprise-dashboard-footer__dot"></span>
            <span>SYSTEM ID: SOV-99812</span>
          </div>
          <div class="enterprise-dashboard-footer__item">
            <span class="enterprise-dashboard-footer__dot"></span>
            <span>LAST SYNC: {{ dashboardSyncTime }}</span>
          </div>
        </div>
        <p>© 2023 企业端 All Rights Reserved.</p>
      </footer>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </section>
  </EnterpriseWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import { fetchEnterpriseProfile, fetchMyProducts } from "../../api/regulation";
import { fetchEnterpriseInspectionRecords, fetchMyRectifications } from "../../api/regulationOperation";
import EnterpriseWorkspacePage from "../../components/enterprise/EnterpriseWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import {
  formatInspectionResult,
  getApprovalStatusLabel,
  getApprovalStatusTone,
  useEnterpriseShellSession
} from "./enterpriseShared";

const { enterpriseUser, token, handleSidebarNavigate, handleLogout } = useEnterpriseShellSession();
const status = reactive({ message: "", type: "" });
const profileLoaded = ref(false);
const profile = reactive({
  approvalStatus: "",
  approvalComment: "",
  approvedTime: "",
  status: "",
  keyReasons: []
});
const productCount = ref(0);
const inspectionRecords = ref([]);
const pendingRectificationCount = ref(0);
const recentInspectionAlertCount = ref(0);
const dashboardSyncTime = ref("-");

const approvalLabel = computed(() => getApprovalStatusLabel(profileLoaded.value, profile.approvalStatus));
const approvalTone = computed(() => getApprovalStatusTone(profileLoaded.value, profile.approvalStatus));
const recentInspections = computed(() => inspectionRecords.value.slice(0, 3));
const isKeyEnterprise = computed(() => profileLoaded.value && profile.status === "KEY");
const keyReasonItems = computed(() => (
  Array.isArray(profile.keyReasons) ? profile.keyReasons.slice(0, 3) : []
));

const heroStatusTitle = computed(() => {
  if (!profileLoaded.value) return "未提交备案";
  if (profile.approvalStatus === "APPROVED") return "已审核通过";
  if (profile.approvalStatus === "REJECTED") return "审核未通过";
  return "审核中";
});

const approvalCode = computed(() => {
  if (!profileLoaded.value) return "UNFILED";
  if (profile.approvalStatus === "APPROVED") return "APPROVED";
  if (profile.approvalStatus === "REJECTED") return "REJECTED";
  return "PENDING";
});

const heroDescription = computed(() => {
  if (profile.approvedTime) {
    return `您的企业备案信息已于 ${formatTime(profile.approvedTime)} 审核完成。建议持续维护产品档案、检查记录与整改资料。`;
  }
  if (profile.approvalComment) return profile.approvalComment;
  if (!profileLoaded.value) {
    return "请先提交企业备案资料，备案通过后再完整启用产品建档、检查回看和整改提交流程。";
  }
  if (profile.approvalStatus === "REJECTED") {
    return "当前备案申请未通过，请根据审核意见尽快修正资料并重新提交。";
  }
  return "当前备案正在审核中，请留意近期检查与整改任务，并准备补充所需附件资料。";
});

const validUntilText = computed(() => {
  if (profile.approvedTime) return formatTime(profile.approvedTime).slice(0, 10);
  return "待核验";
});

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
    : "当前未发现已记录的重点监管原因，但仍需按时维护备案信息并配合监管检查。";
});

const primaryAction = computed(() => ({ label: "更新备案信息", to: { name: "enterprise-profile" } }));

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function getInspectionGlyph(item) {
  if (item.result === "FAIL") return "!";
  if (item.problemDesc) return "检";
  return "查";
}

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

async function loadDashboard() {
  setStatus("");
  try {
    const [
      profileData,
      products,
      inspectionPreviewData,
      inspectionAlertData,
      ongoingRectificationData,
      reworkRectificationData
    ] = await Promise.all([
      fetchEnterpriseProfile(token.value).catch((error) => {
        if (String(error?.message).includes("not found")) return null;
        throw error;
      }),
      fetchMyProducts(token.value).catch(() => []),
      fetchEnterpriseInspectionRecords(token.value, { page: 1, size: 3 }).catch(() => ({ records: [], total: 0 })),
      fetchEnterpriseInspectionRecords(token.value, { result: "FAIL", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchMyRectifications(token.value, { status: "ONGOING", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchMyRectifications(token.value, { status: "REWORK", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 }))
    ]);

    if (profileData) {
      profile.approvalStatus = profileData.approvalStatus || "";
      profile.approvalComment = profileData.approvalComment || "";
      profile.approvedTime = profileData.approvedTime || "";
      profile.status = profileData.status || "";
      profile.keyReasons = Array.isArray(profileData.keyReasons) ? profileData.keyReasons : [];
      profileLoaded.value = true;
    } else {
      profile.approvalStatus = "";
      profile.approvalComment = "";
      profile.approvedTime = "";
      profile.status = "";
      profile.keyReasons = [];
      profileLoaded.value = false;
    }

    productCount.value = Array.isArray(products) ? products.length : 0;
    inspectionRecords.value = inspectionPreviewData.records || [];
    recentInspectionAlertCount.value = Number(inspectionAlertData.total || 0);
    pendingRectificationCount.value = Number(ongoingRectificationData.total || 0)
      + Number(reworkRectificationData.total || 0);
    dashboardSyncTime.value = formatTime(new Date().toISOString());
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载企业工作台失败，请稍后重试。"), "error");
  }
}

onMounted(() => {
  loadDashboard();
});
</script>

<style scoped>
.enterprise-dashboard-risk-panel {
  grid-column: 9 / span 4;
  grid-row: 2;
  align-self: start;
  border: 1px solid #dbe3ee;
  background: #fff;
  padding: 18px;
  display: grid;
  gap: 14px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(25, 28, 30, 0.02);
}

.enterprise-dashboard-list-panel {
  grid-column: 1 / span 8;
  grid-row: 2;
}

.enterprise-dashboard-actions-panel {
  grid-column: 1 / span 12;
  grid-row: 3;
}

.enterprise-dashboard-risk-panel__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.enterprise-dashboard-risk-panel__eyebrow {
  margin: 0;
  font-size: 11px;
  color: #64748b;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.enterprise-dashboard-risk-panel h3 {
  margin: 6px 0 0;
  font-size: 28px;
  line-height: 1.15;
  color: #0f172a;
}

.enterprise-dashboard-risk-panel__desc {
  margin: 8px 0 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
  max-width: 760px;
}

.enterprise-dashboard-risk-panel__badge {
  min-width: 88px;
  min-height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.enterprise-dashboard-risk-panel__badge.is-key {
  background: #fff1f2;
  color: #be123c;
  border: 1px solid #fecdd3;
}

.enterprise-dashboard-risk-panel__badge.is-normal {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}

.enterprise-dashboard-risk-panel__list {
  display: grid;
  gap: 10px;
}

.enterprise-dashboard-risk-panel__item {
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 12px 14px;
}

.enterprise-dashboard-risk-panel__item-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.enterprise-dashboard-risk-panel__item-head strong {
  color: #0f172a;
  font-size: 14px;
}

.enterprise-dashboard-risk-panel__item-head span {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.enterprise-dashboard-risk-panel__item p,
.enterprise-dashboard-risk-panel__empty {
  margin: 8px 0 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}

.enterprise-dashboard-risk-panel__empty {
  border: 1px dashed #cbd5e1;
  background: #f8fafc;
  padding: 14px;
  margin: 0;
}

.enterprise-dashboard-risk-panel__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.enterprise-dashboard-actions-panel__block {
  display: grid;
  gap: 16px;
}

.enterprise-dashboard-actions-panel__notice {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  column-gap: 12px;
  row-gap: 4px;
  align-items: start;
}

.enterprise-dashboard-actions-panel__notice-dot {
  grid-column: 1;
  grid-row: 1 / span 2;
  margin-top: 5px;
}

.enterprise-dashboard-actions-panel__notice strong,
.enterprise-dashboard-actions-panel__notice p {
  grid-column: 2;
}

.enterprise-dashboard-actions-panel__notice p {
  margin-top: 0;
}

.enterprise-dashboard-actions-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 0;
}

.enterprise-dashboard-action {
  align-items: center;
  padding: 10px 14px;
}

.enterprise-dashboard-action > div {
  min-width: 0;
}

.enterprise-dashboard-action strong {
  font-size: 14px;
  line-height: 1.4;
}

.enterprise-dashboard-action p {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.7;
}

@media (max-width: 1180px) {
  .enterprise-dashboard-risk-panel {
    grid-column: span 12;
    grid-row: auto;
  }

  .enterprise-dashboard-list-panel,
  .enterprise-dashboard-actions-panel {
    grid-column: span 12;
    grid-row: auto;
  }

  .enterprise-dashboard-actions-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .enterprise-dashboard-risk-panel__head,
  .enterprise-dashboard-risk-panel__item-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
