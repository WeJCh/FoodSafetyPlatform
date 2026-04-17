<template>
  <EnterpriseWorkspacePage
    active-key="dashboard"
    title="企业工作台"
    :subtitle="`欢迎回来，当前待处理整改任务共计 ${pendingRectificationCount} 项。`"
    top-search-placeholder="搜索监管记录、产品或任务..."
    :username="enterpriseUser.username"
    :user-type="enterpriseUser.userType"
    :status-label="approvalLabel"
    :status-tone="approvalTone"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="enterprise-dashboard-page">
      <div class="enterprise-dashboard-page__head">
        <h2>企业工作台</h2>
        <p>以下数字均按后端当前总量口径汇总，列表区域仅展示最近记录预览。</p>
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
            <h3>快速行动</h3>
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
                  <strong>查看整改回复</strong>
                  <p>提交或反馈整改落实情况</p>
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
        <p>© 2023 Sovereign Oversight Regulatory Authority. All Rights Reserved.</p>
      </footer>

      <button class="enterprise-dashboard-fab" type="button" title="帮助" @click="onHelpFab">
        <span class="material-symbols-outlined" aria-hidden="true">support_agent</span>
      </button>

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
import {
  enterpriseFeaturePendingNotice,
  formatInspectionResult,
  getApprovalStatusLabel,
  getApprovalStatusTone,
  useEnterpriseShellSession
} from "./enterpriseShared";

const { enterpriseUser, token, handleSidebarNavigate, handleLogout } = useEnterpriseShellSession();
const status = reactive({ message: "", type: "" });
const profileLoaded = ref(false);
const profile = reactive({ approvalStatus: "", approvalComment: "", approvedTime: "" });
const productCount = ref(0);
const inspectionRecords = ref([]);
const pendingRectificationCount = ref(0);
const recentInspectionAlertCount = ref(0);
const dashboardSyncTime = ref("-");

const approvalLabel = computed(() => getApprovalStatusLabel(profileLoaded.value, profile.approvalStatus));
const approvalTone = computed(() => getApprovalStatusTone(profileLoaded.value, profile.approvalStatus));
const recentInspections = computed(() => inspectionRecords.value.slice(0, 3));

const heroStatusTitle = computed(() => {
  if (!profileLoaded.value) return "未提交";
  if (profile.approvalStatus === "APPROVED") return "已核准";
  if (profile.approvalStatus === "REJECTED") return "已驳回";
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
  if (profile.approvalComment) {
    return profile.approvalComment;
  }
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
  return "待核准";
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

function onHelpFab() {
  enterpriseFeaturePendingNotice("工作台帮助");
}

async function loadDashboard() {
  setStatus("");
  try {
    const [profileData, products, inspectionPreviewData, inspectionAlertData, ongoingRectificationData, reworkRectificationData] = await Promise.all([
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
      profileLoaded.value = true;
    } else {
      profile.approvalStatus = "";
      profile.approvalComment = "";
      profile.approvedTime = "";
      profileLoaded.value = false;
    }

    productCount.value = Array.isArray(products) ? products.length : 0;
    inspectionRecords.value = inspectionPreviewData.records || [];
    recentInspectionAlertCount.value = Number(inspectionAlertData.total || 0);
    pendingRectificationCount.value = Number(ongoingRectificationData.total || 0) + Number(reworkRectificationData.total || 0);
    dashboardSyncTime.value = formatTime(new Date().toISOString());
  } catch (error) {
    setStatus(error.message || "加载企业工作台失败。", "error");
  }
}

onMounted(() => {
  loadDashboard();
});
</script>
