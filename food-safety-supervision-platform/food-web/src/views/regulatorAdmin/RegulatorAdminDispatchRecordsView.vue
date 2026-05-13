<template>
  <RegulatorAdminWorkspacePage
    active-key="dispatch"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="records-page">
      <nav class="dispatch-subnav">
        <button type="button" @click="goToDispatchTasks">任务列表</button>
        <button type="button" class="active">检查记录</button>
      </nav>

      <header class="records-head">
        <div>
          <h1>检查记录</h1>
          <p>查看检查结果台账，支持按企业、结果和日期范围检索。</p>
        </div>
      </header>

      <section class="panel">
        <form class="filter-bar filter-bar--quad" @submit.prevent="handleInspectionSearch">
          <label>
            企业名称
            <input v-model.trim="inspectionFilters.enterpriseName" placeholder="输入企业名称" />
          </label>
          <label>
            检查结果
            <select v-model="inspectionFilters.result">
              <option value="">全部</option>
              <option value="PASS">合格</option>
              <option value="FAIL">不合格</option>
            </select>
          </label>
          <label>
            起始日期
            <input v-model="inspectionFilters.startDate" type="date" />
          </label>
          <label>
            截止日期
            <input v-model="inspectionFilters.endDate" type="date" />
          </label>
          <button class="primary" type="submit" :disabled="inspectionLoading">
            {{ inspectionLoading ? "查询中..." : "查询" }}
          </button>
        </form>

        <div class="records-list">
          <div class="records-list__head">
            <h3>记录列表</h3>
            <span>当前 {{ inspectionRecords.length }} 条</span>
          </div>
          <div class="list-table inspection-table">
            <div class="list-row list-header inspection-header">
              <span>企业名称</span>
              <span>检查日期</span>
              <span>结果</span>
              <span>更新时间</span>
              <span>操作</span>
            </div>
            <div v-if="!inspectionRecords.length" class="list-empty records-empty">
              <strong>暂无检查记录</strong>
              <span>请调整筛选条件后重试</span>
            </div>
            <div v-for="record in inspectionRecords" :key="record.id" class="list-row inspection-row">
              <span class="inspection-row__enterprise">{{ record.enterpriseName || "-" }}</span>
              <span>{{ record.inspectionDate || "-" }}</span>
              <span>
                <em class="result-pill" :class="resultClass(record.result)">{{ formatInspectionResult(record.result) }}</em>
              </span>
              <span class="inspection-row__time">{{ formatTime(record.updateTime) }}</span>
              <button class="ghost" type="button" @click="openInspectionDetail(record)">查看详情</button>
            </div>
          </div>
        </div>

        <div class="pager">
          <span>共 {{ inspectionTotal }} 条，{{ inspectionPage }}/{{ inspectionPages }} 页</span>
          <div class="pager-actions">
            <button
              class="ghost"
              type="button"
              :disabled="inspectionPage <= 1 || inspectionLoading"
              @click="changeInspectionPage(inspectionPage - 1)"
            >
              上一页
            </button>
            <button
              class="ghost"
              type="button"
              :disabled="inspectionPage >= inspectionPages || inspectionLoading"
              @click="changeInspectionPage(inspectionPage + 1)"
            >
              下一页
            </button>
          </div>
        </div>
      </section>

      <div class="status" :class="status.type" v-if="status.message">
        {{ status.message }}
      </div>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchInspectionRecords } from "../../api/regulationOperation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { formatStatusLabel, inspectionResultMap } from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const router = useRouter();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();

const status = reactive({ message: "", type: "" });
const inspectionFilters = reactive({
  enterpriseName: "",
  result: "",
  startDate: "",
  endDate: ""
});
const inspectionRecords = ref([]);
const inspectionLoading = ref(false);
const inspectionPage = ref(1);
const inspectionSize = ref(8);
const inspectionTotal = ref(0);
const inspectionPages = ref(1);

function goToDispatchTasks() {
  router.push({ name: "regulator-admin-dispatch" }).catch(() => {});
}

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatInspectionResult(value) {
  return formatStatusLabel(value, inspectionResultMap);
}

function resultClass(value) {
  if (value === "PASS") return "is-pass";
  if (value === "FAIL") return "is-fail";
  return "is-default";
}

async function loadInspections() {
  inspectionLoading.value = true;
  setStatus("");
  try {
    const data = await fetchInspectionRecords(token.value, {
      ...inspectionFilters,
      page: inspectionPage.value,
      size: inspectionSize.value
    });
    inspectionRecords.value = data.records || [];
    inspectionTotal.value = data.total || 0;
    inspectionPage.value = data.page || 1;
    inspectionSize.value = data.size || inspectionSize.value;
    inspectionPages.value = data.pages || 1;
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载检查记录失败"), "error");
  } finally {
    inspectionLoading.value = false;
  }
}

async function handleInspectionSearch() {
  inspectionPage.value = 1;
  await loadInspections();
}

async function changeInspectionPage(nextPage) {
  inspectionPage.value = nextPage;
  await loadInspections();
}

function openInspectionDetail(record) {
  if (!record?.id) return;
  router.push({
    name: "regulator-admin-inspection-detail",
    params: { inspectionId: record.id }
  }).catch(() => {});
}

onMounted(loadInspections);
</script>

<style scoped>
.records-page { display: grid; gap: 14px; }
.dispatch-subnav { display: inline-flex; align-items: center; gap: 8px; }
.dispatch-subnav button {
  min-height: 30px;
  border-radius: 4px;
  border: 1px solid #d7e1ec;
  background: #fff;
  color: #516377;
  font-size: 12px;
  font-weight: 700;
  padding: 0 12px;
  cursor: pointer;
}
.dispatch-subnav button.active { border-color: #bfd2ea; background: #eaf2fd; color: #0f3a72; }
.records-head h1 { margin: 0; font-size: 28px; color: #002660; font-weight: 900; letter-spacing: -0.02em; }
.records-head p { margin: 6px 0 0; color: #64748b; font-size: 13px; }
.panel { border: 1px solid #dbe2ea; border-radius: 4px; background: #fff; padding: 12px; }
.filter-bar { display: grid; grid-template-columns: 1.3fr 1fr 1fr 1fr auto; gap: 10px; align-items: end; margin-bottom: 12px; }
.filter-bar label { display: grid; gap: 6px; color: #64748b; font-size: 12px; font-weight: 700; }
.filter-bar input, .filter-bar select { border: 1px solid #d9e3ee; border-radius: 4px; min-height: 34px; padding: 0 10px; color: #0f172a; background: #fff; }
.primary { border: none; border-radius: 4px; min-height: 34px; background: #002660; color: #fff; padding: 0 12px; font-size: 12px; font-weight: 700; cursor: pointer; }
.records-list { border: 1px solid #dbe2ea; border-radius: 6px; overflow: hidden; background: #fff; }
.records-list__head {
  min-height: 40px;
  padding: 0 12px;
  border-bottom: 1px solid #e6edf5;
  background: #f8fbff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.records-list__head h3 { margin: 0; font-size: 13px; color: #0f172a; font-weight: 800; }
.records-list__head span { font-size: 12px; color: #64748b; }
.list-row { --row-columns: 1.6fr 1fr 0.8fr 1.2fr 0.8fr; }
.inspection-header, .inspection-row { --row-columns: 1.6fr 1fr 0.8fr 1.2fr 0.8fr; }
.inspection-table .list-header {
  background: #f2f7fd;
  color: #52667e;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.inspection-row { min-height: 48px; border-top: 1px solid #edf2f7; }
.inspection-row:hover { background: #f8fbff; }
.inspection-row__enterprise { color: #0f3a72; font-weight: 700; }
.inspection-row__time { color: #475569; }
.result-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  font-style: normal;
  font-size: 12px;
  font-weight: 700;
  border: 1px solid transparent;
}
.result-pill.is-pass { color: #166534; background: #dcfce7; border-color: #86efac; }
.result-pill.is-fail { color: #991b1b; background: #fee2e2; border-color: #fecaca; }
.result-pill.is-default { color: #334155; background: #e2e8f0; border-color: #cbd5e1; }
.records-empty {
  min-height: 140px;
  display: grid;
  place-items: center;
  gap: 4px;
  color: #64748b;
}
.records-empty strong { color: #334155; font-size: 14px; }
.records-empty span { font-size: 12px; }
.ghost { border: 1px solid #d1d5db; background: #fff; color: #334155; border-radius: 4px; padding: 6px 10px; font-size: 12px; cursor: pointer; }
.ghost:hover:not(:disabled) { background: #f8fafc; border-color: #cbd5e1; }
.pager { margin-top: 12px; display: flex; justify-content: space-between; align-items: center; color: #64748b; font-size: 12px; }
.pager-actions { display: flex; gap: 8px; }
@media (max-width: 1024px) {
  .filter-bar { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 720px) {
  .filter-bar { grid-template-columns: 1fr; }
}
</style>
