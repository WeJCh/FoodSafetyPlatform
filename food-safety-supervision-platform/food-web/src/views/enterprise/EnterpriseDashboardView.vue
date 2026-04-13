<template>
  <EnterpriseWorkspacePage
    active-key="dashboard"
    title="企业工作台"
    subtitle="欢迎回来，今日待处理事项共计 3 项。"
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
        <p>欢迎回来，今日待处理事项共计 {{ pendingRectificationCount || recentInspectionAlertCount || 0 }} 项。</p>
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
              <button class="ghost" type="button" @click="onDownloadCertificate">下载电子证书</button>
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
                  <p>申报新的受监管条目</p>
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
            <p>2024 年度食品安全监管新规即将发布，请提前完成合规自查与资料复核。</p>
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
            <span>LAST SYNC: 2023-11-08 09:41 CST</span>
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
const rectificationRecords = ref([]);

const approvalLabel = computed(() => getApprovalStatusLabel(profileLoaded.value, profile.approvalStatus));
const approvalTone = computed(() => getApprovalStatusTone(profileLoaded.value, profile.approvalStatus));
const pendingRectificationCount = computed(() =>
  rectificationRecords.value.filter((item) => item.status === "ONGOING" || item.status === "REWORK").length
);
const recentInspectionAlertCount = computed(() =>
  inspectionRecords.value.slice(0, 3).filter((item) => item.result === "FAIL" || item.problemDesc).length
);
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
    return `您的企业备案信息已于 ${formatTime(profile.approvedTime)} 审核完成。建议继续维护产品档案与检查整改资料。`;
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
  return "当前备案正在审核中，请留意近期检查与整改任务，并准备补充可能需要的附件资料。";
});

const validUntilText = computed(() => {
  if (profile.approvedTime) return formatTime(profile.approvedTime).slice(0, 10);
  return "待核准";
});

const primaryAction = computed(() => {
  if (!profileLoaded.value || profile.approvalStatus !== "APPROVED") {
    return { label: "更新备案信息", to: { name: "enterprise-profile" } };
  }
  return { label: "更新备案信息", to: { name: "enterprise-profile" } };
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function getInspectionGlyph(item) {
  if (item.result === "FAIL") return "!";
  if (item.problemDesc) return "检";
  return "安";
}

function onDownloadCertificate() {
  // TODO: 接入电子证照/备案证书下载接口（签章 PDF 或链上凭证）
  enterpriseFeaturePendingNotice("下载电子证书");
}

function onHelpFab() {
  // TODO: 接入在线客服或帮助文档路由
  enterpriseFeaturePendingNotice("工作台帮助");
}

async function loadDashboard() {
  setStatus("");
  try {
    const [profileData, products, inspectionData, rectificationData] = await Promise.all([
      fetchEnterpriseProfile(token.value).catch((error) => {
        if (String(error?.message).includes("not found")) return null;
        throw error;
      }),
      fetchMyProducts(token.value).catch(() => []),
      fetchEnterpriseInspectionRecords(token.value, { page: 1, size: 6 }).catch(() => ({ records: [], total: 0 })),
      fetchMyRectifications(token.value, { page: 1, size: 6 }).catch(() => ({ records: [], total: 0 }))
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
    inspectionRecords.value = inspectionData.records || [];
    rectificationRecords.value = rectificationData.records || [];
  } catch (error) {
    setStatus(error.message || "加载企业工作台失败。", "error");
  }
}

onMounted(() => {
  loadDashboard();
});
</script>
