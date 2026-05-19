<template>
  <EnterpriseWorkspacePage
    active-key="inspections"
    title="检查记录详情"
    subtitle="查看检查结果、任务信息与对应整改任务。"
    top-search-placeholder="搜索记录、任务编号..."
    :username="enterpriseUser.username"
    :user-type="enterpriseUser.userType"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <div class="enterprise-workspace-card enterprise-inspection-detail-page">
      <nav class="enterprise-page-hero__crumb enterprise-inspection-detail-page__crumb" aria-label="面包屑">
        <span>检查记录</span>
        <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
        <span class="is-current">详情</span>
      </nav>

      <div v-if="loading" class="status info">加载中...</div>
      <div v-else-if="!detail" class="status error">未找到该检查记录。</div>
      <template v-else>
        <header class="enterprise-inspection-detail-hero">
          <div>
            <h1 class="enterprise-inspection-detail-hero__title">检查记录详情</h1>
            <p class="enterprise-inspection-detail-hero__subtitle">
              检查日期 {{ detail.record.inspectionDate || "-" }} · 更新时间 {{ formatTime(detail.record.updateTime) }}
            </p>
          </div>
          <RouterLink class="enterprise-toolbar-button enterprise-inspection-detail-hero__back" :to="{ name: 'enterprise-inspections' }">
            <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>
            返回列表
          </RouterLink>
        </header>

        <div class="enterprise-inspection-bento">
          <div class="enterprise-inspection-status-card" :class="{ 'is-pass': detail.record.result === 'PASS' }">
            <p class="enterprise-inspection-status-card__label">当前状态</p>
            <div class="enterprise-inspection-status-card__value">
              <span :class="{ 'is-danger': detail.record.result === 'FAIL' }">{{ formatInspectionResult(detail.record.result) }}</span>
              <span class="material-symbols-outlined" aria-hidden="true">{{ detail.record.result === "FAIL" ? "warning" : "check_circle" }}</span>
            </div>
            <p class="enterprise-inspection-status-card__meta">最后更新：{{ formatTime(detail.record.updateTime) }}</p>
          </div>

          <div class="enterprise-inspection-entity-card">
            <span class="material-symbols-outlined enterprise-inspection-entity-card__bg" aria-hidden="true">corporate_fare</span>
            <div class="enterprise-inspection-entity-card__body">
              <p class="enterprise-inspection-entity-card__label">受检单位</p>
              <h2 class="enterprise-inspection-entity-card__name">{{ detail.record.enterpriseName || "本企业" }}</h2>
              <div class="enterprise-inspection-entity-card__meta">
                <span>
                  <span class="material-symbols-outlined" aria-hidden="true">summarize</span>
                  任务编号 {{ detail.record.taskNo || `INS-${inspectionId}` }}
                </span>
                <span v-if="detail.record.taskTitle">
                  <span class="material-symbols-outlined" aria-hidden="true">assignment</span>
                  {{ detail.record.taskTitle }}
                </span>
                <span v-if="detail.record.enterpriseAddress">
                  <span class="material-symbols-outlined" aria-hidden="true">location_on</span>
                  {{ detail.record.enterpriseAddress }}
                </span>
                <span v-if="detail.record.creditCode">
                  <span class="material-symbols-outlined" aria-hidden="true">badge</span>
                  统一社会信用代码：{{ detail.record.creditCode }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="enterprise-detail-layout">
          <div>
            <section class="enterprise-panel enterprise-inspection-detail-panel">
              <div class="enterprise-inspection-detail-panel__head">
                <h3>检查明细</h3>
                <span>共 {{ (detail.items || []).length }} 项</span>
              </div>
              <div class="enterprise-data-table-wrap enterprise-inspection-detail-panel__table">
                <table v-if="(detail.items || []).length" class="enterprise-data-table">
                  <thead>
                    <tr>
                      <th>检查项</th>
                      <th>结果</th>
                      <th>问题</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(item, index) in detail.items" :key="index">
                      <td style="font-weight: 600">{{ item.itemName || "-" }}</td>
                      <td>
                        <EnterpriseStatusChip :label="formatInspectionResult(item.itemResult)" :tone="item.itemResult === 'FAIL' ? 'danger' : 'success'" small />
                      </td>
                      <td>{{ item.problemDesc || "-" }}</td>
                    </tr>
                  </tbody>
                </table>
                <div v-else class="secondary-text" style="padding: 24px 20px">暂无分项检查明细。</div>
              </div>
            </section>

            <div v-if="detail.record.result === 'FAIL' && detail.record.problemDesc" class="enterprise-findings-card">
              <div class="enterprise-findings-card__head">
                <span class="material-symbols-outlined" aria-hidden="true">gavel</span>
                <div>
                  <h3 class="enterprise-findings-card__title">重点整改说明</h3>
                  <p class="enterprise-findings-card__subtitle">请结合整改任务持续跟进处理结果。</p>
                </div>
              </div>
              <p class="enterprise-findings-card__body">{{ detail.record.problemDesc }}</p>
              <div class="enterprise-findings-card__actions">
                <RouterLink class="primary enterprise-link-button" :to="rectificationRoute">
                  {{ detail.record.rectificationId ? "查看对应整改任务" : "查看整改任务列表" }}
                </RouterLink>
              </div>
            </div>
          </div>

          <aside class="enterprise-side-stack">
            <div class="enterprise-side-card">
              <div class="enterprise-side-card__head">基础信息</div>
              <div class="enterprise-side-card__body">
                <div class="enterprise-meta-row">
                  <span>任务编号</span>
                  <span style="font-family: ui-monospace, monospace">{{ detail.record.taskNo || `INS-${inspectionId}` }}</span>
                </div>
                <div class="enterprise-meta-row">
                  <span>检查日期</span>
                  <span>{{ detail.record.inspectionDate || "-" }}</span>
                </div>
                <div class="enterprise-meta-row">
                  <span>检查结果</span>
                  <span>{{ formatInspectionResult(detail.record.result) }}</span>
                </div>
                <div v-if="detail.record.rectificationStatus" class="enterprise-meta-row">
                  <span>整改状态</span>
                  <span>{{ formatRectificationStatus(detail.record.rectificationStatus) }}</span>
                </div>
              </div>
            </div>

            <div class="enterprise-side-card">
              <div class="enterprise-side-card__head">流程提示</div>
              <div class="enterprise-side-card__body">
                <p class="enterprise-inspection-detail-tip">
                  检查明细由监管端录入。如对结果有异议，请按监管渠道提交说明材料。
                </p>
              </div>
            </div>
          </aside>
        </div>
      </template>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </div>
  </EnterpriseWorkspacePage>
</template>

<script setup>
import { computed, reactive, ref, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { fetchEnterpriseInspectionRecordDetail } from "../../api/regulationOperation";
import EnterpriseStatusChip from "../../components/enterprise/EnterpriseStatusChip.vue";
import EnterpriseWorkspacePage from "../../components/enterprise/EnterpriseWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { formatInspectionResult, formatRectificationStatus, useEnterpriseShellSession } from "./enterpriseShared";

const route = useRoute();
const { enterpriseUser, token, handleSidebarNavigate, handleLogout } = useEnterpriseShellSession();
const inspectionId = computed(() => String(route.params.inspectionId || ""));
const detail = ref(null);
const loading = ref(false);
const status = reactive({ message: "", type: "" });

const rectificationRoute = computed(() => {
  if (detail.value?.record?.rectificationId) {
    return {
      name: "enterprise-rectification-detail",
      params: { rectificationId: detail.value.record.rectificationId }
    };
  }
  return { name: "enterprise-rectifications" };
});

async function loadDetail() {
  loading.value = true;
  try {
    if (!inspectionId.value) {
      detail.value = null;
      return;
    }
    detail.value = await fetchEnterpriseInspectionRecordDetail(token.value, inspectionId.value);
  } catch (error) {
    status.message = error.message || "加载检查记录详情失败";
    status.type = "error";
  } finally {
    loading.value = false;
  }
}

watch(
  () => route.params.inspectionId,
  () => {
    loadDetail();
  },
  { immediate: true }
);
</script>

