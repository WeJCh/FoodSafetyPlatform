<template>
  <RegulatorAdminWorkspacePage
    active-key="approvals"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="approval-page">
      <header class="approval-page__head">
        <div>
          <h1>企业备案审核</h1>
          <p>当前共有 {{ pendingRecords.length }} 条待处理申请，请及时完成审核闭环。</p>
        </div>
        <div class="approval-page__head-actions">
          <button type="button" class="ghost" :disabled="approvalLoading" @click="openBatchAuditModal('reject')">批量驳回</button>
          <button type="button" class="primary" :disabled="approvalLoading" @click="openBatchAuditModal('approve')">批量通过</button>
        </div>
      </header>

      <section class="approval-table">
        <div class="approval-table__head">
          <div class="title"><span class="material-symbols-outlined">list_alt</span>待审核列表</div>
          <span>最近刷新：{{ refreshedAt }}</span>
        </div>
        <div class="approval-table__content">
          <table>
            <thead>
              <tr>
                <th class="check-cell"><input type="checkbox" :checked="allSelected" @change="toggleSelectAll" /></th>
                <th>申请时间</th>
                <th>企业信息</th>
                <th>备案类型</th>
                <th>申请备注</th>
                <th>联系人</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody v-if="pendingRecords.length">
              <tr v-for="item in pendingRecords" :key="item.id">
                <td class="check-cell"><input v-model="selectedIds" type="checkbox" :value="item.id" /></td>
                <td>{{ formatTime(item.updateTime) }}</td>
                <td>
                  <strong>{{ item.enterpriseName || "-" }}</strong>
                  <p>统一社会信用代码：{{ item.creditCode || "-" }}</p>
                  <p>所属区域：{{ formatRegionName(item.regionId) }}</p>
                </td>
                <td>{{ item.enterpriseType || "备案申请" }}</td>
                <td>{{ item.approvalComment || item.description || "暂无备注" }}</td>
                <td>
                  <strong>{{ item.principal || "-" }}</strong>
                  <p>{{ item.principalPhone || "-" }}</p>
                </td>
                <td>
                  <div class="actions">
                    <button type="button" class="link" @click="handleViewDetail(item)">详情</button>
                    <button type="button" class="reject" :disabled="approvalLoading" @click="openSingleAuditModal(item, 'reject')">驳回</button>
                    <button type="button" class="pass" :disabled="approvalLoading" @click="openSingleAuditModal(item, 'approve')">通过</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <AppEmptyState
            v-if="!pendingRecords.length"
            title="暂无待审核企业"
            description="新的企业备案申请会显示在这里。"
            class="approval-table__empty-state"
          />
        </div>
      </section>

      <section class="approval-bottom">
        <article class="guideline-panel">
          <h3>最近审核日志</h3>
          <ul v-if="auditLogs.length">
            <li v-for="item in auditLogs" :key="item.id">
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
              <p>{{ item.meta }}</p>
            </li>
          </ul>
          <div v-else class="status info">当前暂无企业审核日志。</div>
        </article>
      </section>

      <AppStatusToast :message="status.message" :type="status.type" />

      <div v-if="auditModal.visible" class="audit-modal-mask" @click.self="closeAuditModal">
        <div class="audit-modal">
          <h3>{{ auditModal.title }}</h3>
          <p>{{ auditModal.subtitle }}</p>
          <div class="audit-modal__form">
            <label v-if="auditModal.actionType === 'approve'">
              包保责任人
              <select v-model="approvalForm.regulatorId" :disabled="eligibleRegulatorsLoading">
                <option value="">{{ eligibleRegulatorsLoading ? "加载中..." : "请选择包保责任人" }}</option>
                <option v-for="item in eligibleRegulators" :key="item.id" :value="String(item.id)">
                  {{ item.name }}{{ formatRegulatorRegions(item.regionIds) ? `（${formatRegulatorRegions(item.regionIds)}）` : "" }}
                </option>
              </select>
            </label>
            <label>
              审核意见
              <textarea v-model.trim="approvalForm.comment" rows="4" placeholder="请输入审核意见（必填）" />
            </label>
          </div>
          <div class="audit-modal__actions">
            <button type="button" class="ghost" :disabled="approvalLoading" @click="closeAuditModal">取消</button>
            <button type="button" class="primary" :disabled="approvalLoading" @click="submitAuditAction">
              {{ approvalLoading ? "提交中..." : "确认提交" }}
            </button>
          </div>
        </div>
      </div>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import AppEmptyState from "../../components/common/AppEmptyState.vue";
import AppStatusToast from "../../components/common/AppStatusToast.vue";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import {
  approveEnterprise,
  approveEnterpriseBatch,
  fetchEligibleRegulators,
  fetchPendingEnterprises,
  fetchRecentEnterpriseAuditLogs,
  fetchRegionPath,
  rejectEnterprise,
  rejectEnterpriseBatch
} from "../../api/regulation";
import { formatTime } from "../../utils/formatters";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const router = useRouter();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();

const pendingRecords = ref([]);
const approvalLoading = ref(false);
const eligibleRegulatorsLoading = ref(false);
const eligibleRegulators = ref([]);
const status = reactive({ message: "", type: "" });
const approvalForm = reactive({ regulatorId: "", comment: "" });
const selectedIds = ref([]);
const regionNameMap = reactive({});
const refreshedAt = ref("-");
const auditLogs = ref([]);
const auditModal = reactive({
  visible: false,
  actionType: "approve",
  mode: "single",
  enterpriseId: null,
  title: "",
  subtitle: "",
  regionId: null
});

const allSelected = computed(() => {
  if (!pendingRecords.value.length) return false;
  return selectedIds.value.length === pendingRecords.value.length;
});

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatRegionName(regionId) {
  if (!regionId) return "-";
  return regionNameMap[regionId] || `区域 ${regionId}`;
}

function formatRegulatorRegions(regionIds = []) {
  if (!Array.isArray(regionIds) || !regionIds.length) return "";
  return regionIds.map((id) => formatRegionName(id)).filter(Boolean).join("、");
}

function findPendingById(id) {
  return pendingRecords.value.find((item) => String(item.id) === String(id)) || null;
}

function getBatchRegionId() {
  const regionIds = selectedIds.value
    .map((id) => findPendingById(id)?.regionId)
    .filter((id) => id !== null && id !== undefined);
  return new Set(regionIds).size === 1 ? regionIds[0] : null;
}

async function ensureRegionName(regionId) {
  if (!regionId || regionNameMap[regionId]) return;
  try {
    const path = await fetchRegionPath(token.value, regionId);
    regionNameMap[regionId] = Array.isArray(path) && path.length
      ? path.map((item) => item.name).join(" / ")
      : `区域 ${regionId}`;
  } catch {
    regionNameMap[regionId] = `区域 ${regionId}`;
  }
}

async function loadEligibleRegulators(regionId) {
  eligibleRegulatorsLoading.value = true;
  try {
    const data = await fetchEligibleRegulators(token.value, regionId || undefined);
    const list = Array.isArray(data) ? data : [];
    eligibleRegulators.value = list.filter((item) => String(item.roleType) === "REGULATOR_ENFORCER");
    await Promise.all(
      eligibleRegulators.value.flatMap((item) =>
        Array.isArray(item.regionIds) ? item.regionIds.map((region) => ensureRegionName(region)) : []
      )
    );
  } catch (error) {
    eligibleRegulators.value = [];
    setStatus(resolveErrorMessage(error, "执法人员列表加载失败，请稍后重试"), "error");
  } finally {
    eligibleRegulatorsLoading.value = false;
  }
}

async function loadPending() {
  approvalLoading.value = true;
  setStatus("");
  try {
    const [data, logs] = await Promise.all([
      fetchPendingEnterprises(token.value),
      fetchRecentEnterpriseAuditLogs(token.value, 6).catch(() => [])
    ]);
    pendingRecords.value = Array.isArray(data) ? data : [];
    auditLogs.value = (Array.isArray(logs) ? logs : []).map((item, index) => ({
      id: item.id || `audit-${index}`,
      title: item.actionName || item.actionType || "企业审核日志",
      desc: item.summary || `${item.targetName || "企业"}发生了一条新的审核操作`,
      meta: `${item.operatorName || "系统"} | ${formatTime(item.createTime)}`
    }));
    selectedIds.value = [];
    refreshedAt.value = formatTime(new Date().toISOString());
    await Promise.all(pendingRecords.value.map((item) => ensureRegionName(item.regionId)));
  } catch (error) {
    setStatus(resolveErrorMessage(error, "待审核企业加载失败，请稍后重试"), "error");
  } finally {
    approvalLoading.value = false;
  }
}

async function openSingleAuditModal(item, actionType) {
  auditModal.visible = true;
  auditModal.mode = "single";
  auditModal.actionType = actionType;
  auditModal.enterpriseId = item.id;
  auditModal.regionId = item.regionId || null;
  auditModal.title = actionType === "approve" ? "确认通过企业备案" : "确认驳回企业备案";
  auditModal.subtitle = item.enterpriseName || "当前企业";
  approvalForm.comment = "";
  approvalForm.regulatorId = item.regulatorId ? String(item.regulatorId) : "";
  eligibleRegulators.value = [];
  if (actionType === "approve") {
    await loadEligibleRegulators(item.regionId || null);
  }
}

async function openBatchAuditModal(actionType) {
  if (!selectedIds.value.length) {
    setStatus("请先选择需要审核的企业。", "error");
    return;
  }
  const regionId = getBatchRegionId();
  if (actionType === "approve" && !regionId) {
    setStatus("批量通过时请选择同一街道辖区的企业，以便统一指定包保责任人。", "error");
    return;
  }
  auditModal.visible = true;
  auditModal.mode = "batch";
  auditModal.actionType = actionType;
  auditModal.enterpriseId = null;
  auditModal.regionId = regionId;
  auditModal.title = actionType === "approve" ? "批量通过企业备案" : "批量驳回企业备案";
  auditModal.subtitle = `已选择 ${selectedIds.value.length} 家企业`;
  approvalForm.comment = "";
  approvalForm.regulatorId = "";
  eligibleRegulators.value = [];
  if (actionType === "approve") {
    await loadEligibleRegulators(regionId);
  }
}

function closeAuditModal() {
  auditModal.visible = false;
  auditModal.regionId = null;
  eligibleRegulators.value = [];
}

async function submitAuditAction() {
  if (!approvalForm.comment.trim()) {
    setStatus("请填写审核意见。", "error");
    return;
  }
  if (auditModal.actionType === "approve" && !approvalForm.regulatorId) {
    setStatus("请选择包保责任人。", "error");
    return;
  }

  approvalLoading.value = true;
  setStatus("");
  try {
    if (auditModal.mode === "single") {
      if (auditModal.actionType === "approve") {
        await approveEnterprise(token.value, auditModal.enterpriseId, {
          comment: approvalForm.comment,
          regulatorId: Number(approvalForm.regulatorId)
        });
        setStatus("审核通过成功。", "success");
      } else {
        await rejectEnterprise(token.value, auditModal.enterpriseId, {
          comment: approvalForm.comment
        });
        setStatus("驳回成功。", "success");
      }
    } else if (auditModal.actionType === "approve") {
      await approveEnterpriseBatch(token.value, {
        ids: selectedIds.value,
        comment: approvalForm.comment,
        regulatorId: Number(approvalForm.regulatorId)
      });
      setStatus("批量通过成功。", "success");
    } else {
      await rejectEnterpriseBatch(token.value, {
        ids: selectedIds.value,
        comment: approvalForm.comment
      });
      setStatus("批量驳回成功。", "success");
    }

    closeAuditModal();
    await loadPending();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "审核提交失败，请稍后重试"), "error");
  } finally {
    approvalLoading.value = false;
  }
}

function toggleSelectAll(event) {
  if (event.target.checked) {
    selectedIds.value = pendingRecords.value.map((item) => item.id);
    return;
  }
  selectedIds.value = [];
}

function handleViewDetail(item) {
  router.push({
    name: "regulator-admin-enterprise-detail",
    params: { enterpriseId: item.id },
    query: { from: "approvals" }
  }).catch(() => {});
}

onMounted(() => {
  loadPending();
});
</script>

<style scoped>
.approval-page { display: grid; gap: 16px; }
.approval-page__head { display: flex; align-items: end; justify-content: space-between; gap: 10px; }
.approval-page__head h1 { margin: 0; color: #002660; font-size: 30px; font-weight: 800; }
.approval-page__head p { margin: 6px 0 0; color: #64748b; }
.approval-page__head-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 6px;
}
.approval-page__head-actions button, .actions button, .guideline-panel button { border: 0; border-radius: 8px; padding: 8px 12px; cursor: pointer; font-size: 12px; font-weight: 700; }
.approval-page__head-actions button {
  min-width: 80px;
  min-height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}
.approval-page__head-actions .ghost { background: #eef2f7; color: #334155; }
.approval-page__head-actions .primary { background: #002660; color: #fff; box-shadow: 0 6px 14px rgba(0, 38, 96, 0.22); }
.approval-page__head-actions button:disabled { opacity: 0.55; cursor: not-allowed; box-shadow: none; }
.guideline-panel h3 { margin: 0 0 10px; font-size: 16px; color: #0f172a; }
.approval-table { background: #fff; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0; }
.approval-table__head { display: flex; align-items: center; justify-content: space-between; background: #eef2f7; padding: 12px; font-size: 12px; color: #64748b; }
.approval-table__head .title { display: inline-flex; align-items: center; gap: 6px; color: #002660; font-weight: 800; }
.approval-table__content { overflow: auto; }
table { width: 100%; min-width: 1080px; border-collapse: collapse; }
th { text-align: left; padding: 12px; background: #f8fafc; border-top: 1px solid #e2e8f0; color: #64748b; font-size: 11px; text-transform: uppercase; }
tbody tr { transition: background-color 0.2s ease; }
tbody tr:hover { background: #f8fafc; }
td { padding: 12px; border-top: 1px solid #edf2f7; font-size: 13px; color: #1e293b; vertical-align: middle; }
td strong { display: block; color: #002660; }
td p { margin: 2px 0 0; color: #64748b; font-size: 12px; }
.check-cell { width: 56px; text-align: center; }
.check-cell input { display: block; margin: 0 auto; }
.actions { display: flex; gap: 6px; align-items: center; justify-content: flex-start; }
.actions button { min-height: 28px; line-height: 1; display: inline-flex; align-items: center; justify-content: center; }
.actions .link { background: #eff6ff; color: #1d4ed8; }
.actions .reject { background: #fee2e2; color: #b91c1c; }
.actions .pass { background: #dcfce7; color: #166534; }
.approval-bottom { display: grid; grid-template-columns: 1fr; gap: 12px; }
.guideline-panel {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
}
.guideline-panel ul { margin: 0; padding-left: 18px; color: #475569; display: grid; gap: 10px; }
.guideline-panel li strong { display: block; color: #0f172a; font-size: 13px; }
.guideline-panel li p { margin: 4px 0 0; color: #64748b; font-size: 12px; }
.audit-modal-mask { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.45); display: grid; place-items: center; z-index: 1000; }
.audit-modal { width: min(520px, 92vw); background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 20px 50px rgba(2, 6, 23, 0.25); }
.audit-modal h3 { margin: 0; color: #0f172a; font-size: 18px; }
.audit-modal p { margin: 6px 0 0; color: #64748b; font-size: 13px; }
.audit-modal__form { margin-top: 12px; display: grid; gap: 10px; }
.audit-modal__form label { display: grid; gap: 6px; color: #64748b; font-size: 12px; font-weight: 700; }
.audit-modal__form input,
.audit-modal__form textarea,
.audit-modal__form select {
  border: 0;
  background: #f1f5f9;
  border-radius: 8px;
  padding: 10px;
  font-size: 13px;
  color: #0f172a;
}
.audit-modal__form textarea { resize: vertical; min-height: 96px; }
.audit-modal__actions { margin-top: 14px; display: flex; justify-content: flex-end; gap: 8px; }
.audit-modal__actions .ghost, .audit-modal__actions .primary { border: 0; border-radius: 8px; padding: 8px 14px; font-size: 13px; cursor: pointer; }
.audit-modal__actions .ghost { background: #e2e8f0; color: #334155; }
.audit-modal__actions .primary { background: #002660; color: #fff; }
@media (max-width: 960px) {
  .approval-page__head { flex-direction: column; align-items: stretch; }
}
</style>
