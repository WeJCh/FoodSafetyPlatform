<template>
  <RegulatorAdminWorkspacePage
    active-key="approvals"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
    @pending-feature="onPendingFeature"
  >
    <section class="approval-page">
      <header class="approval-page__head">
        <div>
          <h1>企业备案审核</h1>
          <p>当前有 {{ pendingRecords.length }} 个待处理备案申请，请及时审核。</p>
        </div>
        <div class="approval-page__head-actions">
          <button type="button" class="ghost" :disabled="approvalLoading" @click="openBatchAuditModal('reject')">批量驳回</button>
          <button type="button" class="primary" :disabled="approvalLoading" @click="openBatchAuditModal('approve')">批量通过</button>
        </div>
      </header>

      <section class="approval-table">
        <div class="approval-table__head">
          <div class="title"><span class="material-symbols-outlined">list_alt</span>待审核名录</div>
          <span>更新于 {{ refreshedAt }}</span>
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
                <th>联系信息</th>
                <th>操作动作</th>
              </tr>
            </thead>
            <tbody v-if="pendingRecords.length">
              <tr v-for="item in pendingRecords" :key="item.id">
                <td class="check-cell"><input type="checkbox" :value="item.id" v-model="selectedIds" /></td>
                <td>{{ formatTime(item.updateTime) }}</td>
                <td>
                  <strong>{{ item.enterpriseName || "-" }}</strong>
                  <p>信用代码：{{ item.creditCode || "-" }}</p>
                  <p>区域：{{ formatRegionName(item.regionId) }}</p>
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
          <div v-if="!pendingRecords.length" class="empty">暂无待审核企业</div>
        </div>
      </section>

      <section class="approval-bottom">
        <article class="guideline-panel">
          <h3>审核准则</h3>
          <ul>
            <li>证照图像必须清晰，公章完整。</li>
            <li>信用代码必须与全国企业信息库一致。</li>
            <li>经营范围需符合申请备案类型。</li>
          </ul>
          <button type="button" @click="onPendingFeature('查看完整审核条例')">查看完整审核条例</button>
        </article>
      </section>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>

      <div v-if="auditModal.visible" class="audit-modal-mask" @click.self="closeAuditModal">
        <div class="audit-modal">
          <h3>{{ auditModal.title }}</h3>
          <p>{{ auditModal.subtitle }}</p>
          <div class="audit-modal__form">
            <label>
              审核人姓名
              <input v-model.trim="approvalForm.regulatorName" placeholder="可选填写" />
            </label>
            <label>
              审批意见
              <textarea
                v-model.trim="approvalForm.comment"
                rows="4"
                placeholder="请输入审批意见（必填）"
              ></textarea>
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
import {
  approveEnterprise,
  approveEnterpriseBatch,
  fetchPendingEnterprises,
  fetchRegionPath,
  rejectEnterprise,
  rejectEnterpriseBatch
} from "../../api/regulation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import {
  regulatorFeaturePendingNotice,
  useRegulatorAdminShellSession
} from "./regulatorAdminShared";

const router = useRouter();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();
const pendingRecords = ref([]);
const approvalLoading = ref(false);
const status = reactive({ message: "", type: "" });
const approvalForm = reactive({ regulatorName: "", comment: "" });
const selectedIds = ref([]);
const regionNameMap = reactive({});
const refreshedAt = ref("-");
const auditModal = reactive({
  visible: false,
  actionType: "approve",
  mode: "single",
  enterpriseId: null,
  title: "",
  subtitle: ""
});

const allSelected = computed(() => {
  if (!pendingRecords.value.length) return false;
  return selectedIds.value.length === pendingRecords.value.length;
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function onPendingFeature(title) {
  regulatorFeaturePendingNotice(title);
}

function formatRegionName(regionId) {
  if (!regionId) return "-";
  return regionNameMap[regionId] || `区域 ${regionId}`;
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

async function loadPending() {
  approvalLoading.value = true;
  setStatus("");
  try {
    const data = await fetchPendingEnterprises(token.value);
    pendingRecords.value = data || [];
    selectedIds.value = [];
    refreshedAt.value = formatTime(new Date().toISOString());
    await Promise.all((pendingRecords.value || []).map((item) => ensureRegionName(item.regionId)));
  } catch (error) {
    setStatus(error.message || "加载待审核企业失败", "error");
  } finally {
    approvalLoading.value = false;
  }
}

function openSingleAuditModal(item, actionType) {
  auditModal.visible = true;
  auditModal.mode = "single";
  auditModal.actionType = actionType;
  auditModal.enterpriseId = item.id;
  auditModal.title = actionType === "approve" ? "确认通过企业备案" : "确认驳回企业备案";
  auditModal.subtitle = item.enterpriseName || "当前企业";
  approvalForm.comment = "";
}

function openBatchAuditModal(actionType) {
  if (!selectedIds.value.length) {
    setStatus("请选择需要审批的企业", "error");
    return;
  }
  auditModal.visible = true;
  auditModal.mode = "batch";
  auditModal.actionType = actionType;
  auditModal.enterpriseId = null;
  auditModal.title = actionType === "approve" ? "批量通过企业备案" : "批量驳回企业备案";
  auditModal.subtitle = `已选择 ${selectedIds.value.length} 家企业`;
  approvalForm.comment = "";
}

function closeAuditModal() {
  auditModal.visible = false;
}

async function submitAuditAction() {
  if (!approvalForm.comment.trim()) {
    setStatus("审批意见必填", "error");
    return;
  }
  approvalLoading.value = true;
  setStatus("");
  try {
    if (auditModal.mode === "single") {
      if (auditModal.actionType === "approve") {
        await approveEnterprise(token.value, auditModal.enterpriseId, approvalForm);
        setStatus("审批通过成功", "success");
      } else {
        await rejectEnterprise(token.value, auditModal.enterpriseId, approvalForm);
        setStatus("驳回成功", "success");
      }
    } else if (auditModal.actionType === "approve") {
      await approveEnterpriseBatch(token.value, {
        ids: selectedIds.value,
        comment: approvalForm.comment,
        regulatorName: approvalForm.regulatorName
      });
      setStatus("批量通过成功", "success");
    } else {
      await rejectEnterpriseBatch(token.value, {
        ids: selectedIds.value,
        comment: approvalForm.comment,
        regulatorName: approvalForm.regulatorName
      });
      setStatus("批量驳回成功", "success");
    }
    closeAuditModal();
    await loadPending();
  } catch (error) {
    setStatus(error.message || "审核提交失败", "error");
  } finally {
    approvalLoading.value = false;
  }
}

function toggleSelectAll(event) {
  if (event.target.checked) {
    selectedIds.value = pendingRecords.value.map((item) => item.id);
  } else {
    selectedIds.value = [];
  }
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
.approval-page__head-actions .ghost {
  background: #eef2f7;
  color: #334155;
}
.approval-page__head-actions .ghost:hover:not(:disabled) {
  background: #e2e8f0;
}
.approval-page__head-actions .primary {
  background: #002660;
  color: #fff;
  box-shadow: 0 6px 14px rgba(0, 38, 96, 0.22);
}
.approval-page__head-actions .primary:hover:not(:disabled) {
  background: #003a8c;
}
.approval-page__head-actions button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}
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
.actions .link { background: #eef2ff; color: #1d4ed8; }
.actions .reject { background: #fee2e2; color: #991b1b; }
.actions .pass { background: #dcfce7; color: #166534; }
.empty { padding: 16px; color: #64748b; font-size: 13px; }
.approval-bottom { display: grid; grid-template-columns: 1fr; gap: 12px; }
.guideline-panel { background: #fff; border-radius: 12px; padding: 16px; }
.guideline-panel ul { margin: 0; padding-left: 16px; color: #475569; display: grid; gap: 8px; font-size: 13px; }
.guideline-panel button { margin-top: 12px; width: 100%; background: #eff6ff; color: #1d4ed8; }
.status { position: fixed; right: 18px; bottom: 18px; background: #0f172a; color: #fff; border-radius: 8px; padding: 10px 12px; font-size: 13px; }
.status.error { background: #b91c1c; }
.audit-modal-mask { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.45); display: grid; place-items: center; z-index: 1000; }
.audit-modal { width: min(520px, 92vw); background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 20px 50px rgba(2, 6, 23, 0.25); }
.audit-modal h3 { margin: 0; color: #0f172a; font-size: 18px; }
.audit-modal p { margin: 6px 0 0; color: #64748b; font-size: 13px; }
.audit-modal__form { margin-top: 12px; display: grid; gap: 10px; }
.audit-modal__form label { display: grid; gap: 6px; color: #64748b; font-size: 12px; font-weight: 700; }
.audit-modal__form input, .audit-modal__form textarea { border: 0; background: #f1f5f9; border-radius: 8px; padding: 10px; font-size: 13px; color: #0f172a; }
.audit-modal__form textarea { resize: vertical; min-height: 96px; }
.audit-modal__actions { margin-top: 14px; display: flex; justify-content: flex-end; gap: 8px; }
.audit-modal__actions .ghost, .audit-modal__actions .primary { border: 0; border-radius: 8px; padding: 8px 14px; font-size: 13px; cursor: pointer; }
.audit-modal__actions .ghost { background: #e2e8f0; color: #334155; }
.audit-modal__actions .primary { background: #002660; color: #fff; }
@media (max-width: 1200px) {
  .approval-bottom { grid-template-columns: 1fr; }
}
</style>
