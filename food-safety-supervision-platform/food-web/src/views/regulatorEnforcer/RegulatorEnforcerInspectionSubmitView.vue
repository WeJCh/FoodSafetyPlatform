<template>
  <RegulatorEnforcerWorkspacePage
    active-key="tasks"
    :username="enforcerUser.username || enforcerUser.realName || ''"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
    @pending-feature="regulatorEnforcerFeaturePendingNotice"
  >
    <section class="inspection-submit-page">
      <header class="page-head">
        <div>
          <nav class="crumbs" aria-label="面包屑">
            <button type="button" class="crumb-link" @click="goList">我的检查任务</button>
            <span class="crumb-sep">/</span>
            <button v-if="task?.id" type="button" class="crumb-link" @click="goDetail">
              {{ task?.taskNo || "任务详情" }}
            </button>
            <template v-if="task?.id">
              <span class="crumb-sep">/</span>
            </template>
            <span class="crumb-current">检查结果提交</span>
          </nav>
          <h1>检查结果提交页</h1>
          <p>参考执法端原型结构，完成检查项填报、问题说明与最终结论提交。</p>
        </div>
        <div class="head-actions">
          <button class="ghost-btn" type="button" :disabled="actionLoading" @click="goDetail">
            返回详情
          </button>
          <button class="primary-btn" type="button" :disabled="actionLoading || pageLoading" @click="handleSubmit">
            {{ actionLoading ? "提交中..." : "确认并提交" }}
          </button>
        </div>
      </header>

      <div v-if="pageLoading" class="state-card">检查结果提交页加载中...</div>
      <div v-else-if="loadError" class="state-card state-card--error">{{ loadError }}</div>

      <template v-else-if="task">
        <div class="content-grid">
          <div class="main-column">
            <section class="panel">
              <div class="panel-head">
                <h2>任务基本信息</h2>
              </div>
              <div class="info-grid">
                <article>
                  <span>任务编号</span>
                  <strong>{{ task.taskNo || `#${task.id}` }}</strong>
                </article>
                <article>
                  <span>受检单位</span>
                  <strong>{{ enterprise?.enterpriseName || task.enterpriseName || "-" }}</strong>
                </article>
                <article>
                  <span>检查类型</span>
                  <strong>{{ task.taskType || task.taskTitle || "日常监督检查" }}</strong>
                </article>
                <article>
                  <span>计划日期</span>
                  <strong>{{ formatTime(task.deadline || task.startedTime || task.createTime) }}</strong>
                </article>
                <article class="is-wide">
                  <span>单位地址</span>
                  <strong>{{ enterprise?.addressDetail || "-" }}</strong>
                </article>
                <article>
                  <span>所属区域</span>
                  <strong>{{ regionLabel }}</strong>
                </article>
                <article>
                  <span>执行人员</span>
                  <strong>{{ enforcerUser.realName || enforcerUser.username || task.assignedToName || "-" }}</strong>
                </article>
              </div>
            </section>

            <section class="panel">
              <div class="panel-head panel-head--space">
                <h2>检查项填报</h2>
                <button class="text-btn" type="button" @click="addItem">新增检查项</button>
              </div>

              <div class="item-list">
                <article v-for="(item, index) in form.items" :key="item.key" class="item-card">
                  <div class="item-card__head">
                    <div class="item-card__title">
                      <span class="item-index">{{ String(index + 1).padStart(2, '0') }}</span>
                      <input
                        v-model.trim="item.itemName"
                        type="text"
                        maxlength="100"
                        placeholder="请输入检查项名称"
                      />
                    </div>
                    <button
                      v-if="form.items.length > 1"
                      class="icon-btn"
                      type="button"
                      aria-label="删除检查项"
                      @click="removeItem(index)"
                    >
                      删除
                    </button>
                  </div>

                  <textarea
                    v-model.trim="item.problemGuide"
                    rows="2"
                    maxlength="160"
                    placeholder="填写检查要点或现场核查重点，便于提交前复核。"
                  ></textarea>

                  <div class="item-card__foot">
                    <div class="item-result-group">
                      <button
                        type="button"
                        class="mini-choice"
                        :class="{ 'is-active is-pass': item.itemResult === 'PASS' }"
                        @click="item.itemResult = 'PASS'"
                      >
                        合格
                      </button>
                      <button
                        type="button"
                        class="mini-choice"
                        :class="{ 'is-active is-fail': item.itemResult === 'FAIL' }"
                        @click="item.itemResult = 'FAIL'"
                      >
                        不合格
                      </button>
                    </div>

                    <textarea
                      v-model.trim="item.problemDesc"
                      rows="2"
                      maxlength="500"
                      placeholder="如该项不合格，请填写问题描述；无问题可留空。"
                    ></textarea>
                  </div>
                </article>
              </div>
            </section>

            <section class="panel">
              <div class="panel-head">
                <h2>问题说明</h2>
              </div>
              <div class="notice-strip">
                当前接口未提供独立“备注信息”字段，页面将按后端语义把本区说明提交到“总体问题描述”。
              </div>
              <label class="field-block">
                <span>检查日期</span>
                <input v-model="form.inspectionDate" type="date" required />
              </label>
              <label class="field-block">
                <span>总体问题描述</span>
                <textarea
                  v-model.trim="form.problemDesc"
                  rows="5"
                  maxlength="1000"
                  placeholder="请汇总填写现场发现的问题、证据情况和处置要点。若最终结论不是合格，建议明确写出核心问题。"
                ></textarea>
              </label>
            </section>
          </div>

          <aside class="side-column">
            <section class="panel panel--primary">
              <div class="panel-head panel-head--light">
                <h2>最终检查结论</h2>
              </div>
              <div class="decision-list">
                <button
                  type="button"
                  class="decision-btn"
                  :class="{ 'is-active is-pass': form.decision === 'PASS' }"
                  @click="form.decision = 'PASS'"
                >
                  <strong>检查合格</strong>
                  <span>提交为 PASS，不触发整改流程</span>
                </button>
                <button
                  type="button"
                  class="decision-btn"
                  :class="{ 'is-active is-rectify': form.decision === 'RECTIFY' }"
                  @click="form.decision = 'RECTIFY'"
                >
                  <strong>限期整改</strong>
                  <span>按不合格提交，系统会自动生成整改任务</span>
                </button>
                <button
                  type="button"
                  class="decision-btn"
                  :class="{ 'is-active is-fail': form.decision === 'FAIL' }"
                  @click="form.decision = 'FAIL'"
                >
                  <strong>检查不合格</strong>
                  <span>按 FAIL 提交，并进入后续风险与整改联动</span>
                </button>
              </div>
            </section>

            <section class="panel">
              <div class="panel-head">
                <h2>提交校验提示</h2>
              </div>
              <ul class="tip-list">
                <li>至少保留 1 个检查项，且检查项名称不能为空。</li>
                <li>若任一检查项判定为不合格，则不能将最终结论提交为“检查合格”。</li>
                <li>选择“限期整改”或“检查不合格”时，建议同步补充总体问题描述。</li>
                <li>后端检查接口仅支持 `PASS/FAIL`；“限期整改”会映射为 `FAIL` 并自动触发整改任务。</li>
              </ul>
            </section>

            <section class="panel">
              <div class="panel-head">
                <h2>流转进度</h2>
              </div>
              <div class="timeline-list">
                <article v-for="(item, index) in timeline" :key="`${item.title}-${index}`" class="timeline-item">
                  <span class="timeline-dot" :class="item.dotClass"></span>
                  <div class="timeline-body">
                    <div class="timeline-top">
                      <strong>{{ item.title }}</strong>
                      <small>{{ item.time }}</small>
                    </div>
                    <p>{{ item.desc }}</p>
                  </div>
                </article>
              </div>
            </section>
          </aside>
        </div>

        <div v-if="status.message" class="status-banner" :class="`is-${status.type}`">{{ status.message }}</div>
      </template>
    </section>
  </RegulatorEnforcerWorkspacePage>
</template>

<script setup>
import { computed, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchEnterpriseDetail, fetchRegionPath } from "../../api/regulation";
import { findMyInspectionTaskById, submitInspectionTask } from "../../api/regulationOperation";
import RegulatorEnforcerWorkspacePage from "../../components/regulatorEnforcer/RegulatorEnforcerWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import {
  regulatorEnforcerFeaturePendingNotice,
  useRegulatorEnforcerShellSession
} from "./regulatorEnforcerShared";

const route = useRoute();
const router = useRouter();
const { enforcerUser, token, handleSidebarNavigate, handleLogout } = useRegulatorEnforcerShellSession();

const pageLoading = ref(true);
const actionLoading = ref(false);
const loadError = ref("");
const task = ref(null);
const enterprise = ref(null);
const regionLabel = ref("-");
const status = reactive({ message: "", type: "info" });
const itemSeed = ref(3);

const form = reactive({
  inspectionDate: "",
  decision: "PASS",
  problemDesc: "",
  items: []
});

const defaultItemTemplates = [
  {
    itemName: "生产许可效力",
    problemGuide: "核查许可证有效期、许可范围与现场经营范围是否一致。",
    itemResult: "PASS",
    problemDesc: ""
  },
  {
    itemName: "从业人员健康管理",
    problemGuide: "抽查从业人员健康证明、培训记录与晨检落实情况。",
    itemResult: "PASS",
    problemDesc: ""
  },
  {
    itemName: "经营环境卫生",
    problemGuide: "检查场所卫生、三防设施、冷藏温控与台账留存情况。",
    itemResult: "PASS",
    problemDesc: ""
  }
];

const timeline = computed(() => {
  const current = task.value;
  if (!current) return [];

  return [
    {
      title: "任务已下达",
      time: formatTime(current.assignedTime || current.createTime),
      desc: current.taskDesc || "监管任务已派发至当前执法人员，待现场执行。",
      dotClass: "is-on"
    },
    {
      title: current.startedTime ? "现场检查完成" : "等待现场检查",
      time: current.startedTime ? formatTime(current.startedTime) : "--",
      desc: current.startedTime
        ? `${current.assignedToName || "当前执法人员"} 已开始执行任务，可继续录入检查结果。`
        : "任务尚未开始执行，请先从任务列表或详情页启动。",
      dotClass: current.startedTime ? "is-on" : "is-muted"
    },
    {
      title: "等待结果提交",
      time: "当前环节",
      desc: "提交后任务状态将变为已完成，检查记录会写入检查记录列表。",
      dotClass: "is-alert"
    }
  ];
});

function createItem(template = {}) {
  itemSeed.value += 1;
  return {
    key: `${Date.now()}-${itemSeed.value}`,
    itemName: template.itemName || "",
    problemGuide: template.problemGuide || "",
    itemResult: template.itemResult || "PASS",
    problemDesc: template.problemDesc || ""
  };
}

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatTodayLocal() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function resolveSubmitResult() {
  return form.decision === "PASS" ? "PASS" : "FAIL";
}

function addItem() {
  form.items.push(createItem());
}

function removeItem(index) {
  if (form.items.length <= 1) return;
  form.items.splice(index, 1);
}

function goList() {
  router.push({ name: "regulator-enforcer-tasks" }).catch(() => {});
}

function goDetail() {
  if (!task.value?.id) {
    goList();
    return;
  }
  router.push({
    name: "regulator-enforcer-task-detail",
    params: { taskId: task.value.id }
  }).catch(() => {});
}

async function loadPage() {
  pageLoading.value = true;
  loadError.value = "";
  setStatus("");
  task.value = null;
  enterprise.value = null;
  regionLabel.value = "-";

  const taskId = route.params.taskId;
  if (!taskId) {
    loadError.value = "缺少检查任务参数";
    pageLoading.value = false;
    return;
  }

  try {
    const row = await findMyInspectionTaskById(token.value, taskId);
    if (!row) {
      loadError.value = "未找到该检查任务，请从列表重新进入。";
      return;
    }

    task.value = row;
    form.inspectionDate = formatTodayLocal();
    form.decision = "PASS";
    form.problemDesc = "";
    form.items = defaultItemTemplates.map((template) => createItem(template));

    if (row.enterpriseId) {
      try {
        const detail = await fetchEnterpriseDetail(token.value, row.enterpriseId);
        enterprise.value = detail || null;
        if (detail?.regionId) {
          const path = await fetchRegionPath(token.value, detail.regionId).catch(() => []);
          regionLabel.value = Array.isArray(path) && path.length ? path.map((item) => item.name).join(" / ") : "-";
        }
      } catch {
        enterprise.value = null;
      }
    }
  } catch (error) {
    loadError.value = error?.message || "检查结果提交页加载失败";
  } finally {
    pageLoading.value = false;
  }
}

function validateBeforeSubmit() {
  if (!task.value?.id) {
    setStatus("缺少任务信息，无法提交。", "error");
    return false;
  }
  if (!form.inspectionDate) {
    setStatus("请选择检查日期。", "error");
    return false;
  }

  const normalizedItems = form.items
    .map((item) => ({
      itemName: String(item.itemName || "").trim(),
      itemResult: item.itemResult || "PASS",
      problemDesc: String(item.problemDesc || "").trim()
    }))
    .filter((item) => item.itemName);

  if (!normalizedItems.length) {
    setStatus("请至少填写 1 个有效检查项。", "error");
    return false;
  }

  const hasFailedItems = normalizedItems.some((item) => item.itemResult === "FAIL");
  if (form.decision === "PASS" && hasFailedItems) {
    setStatus("存在不合格检查项时，最终结论不能提交为“检查合格”。", "error");
    return false;
  }
  if (form.decision !== "PASS" && !String(form.problemDesc || "").trim() && !hasFailedItems) {
    setStatus("选择“限期整改”或“检查不合格”时，请补充总体问题描述或至少标记 1 个不合格检查项。", "error");
    return false;
  }

  return normalizedItems;
}

async function handleSubmit() {
  const normalizedItems = validateBeforeSubmit();
  if (!normalizedItems) return;

  actionLoading.value = true;
  setStatus("");
  try {
    await submitInspectionTask(token.value, task.value.id, {
      inspectionDate: form.inspectionDate,
      result: resolveSubmitResult(),
      problemDesc: form.problemDesc.trim(),
      items: normalizedItems
    });
    await router.push({
      name: "regulator-enforcer-task-detail",
      params: { taskId: task.value.id },
      query: { submitted: "1" }
    });
  } catch (error) {
    setStatus(error?.message || "提交检查结果失败", "error");
  } finally {
    actionLoading.value = false;
  }
}

watch(() => route.params.taskId, loadPage, { immediate: true });
</script>

<style scoped>
.inspection-submit-page {
  display: grid;
  gap: 18px;
}
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
}
.crumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #64748b;
  font-size: 12px;
}
.crumb-link,
.crumb-current,
.crumb-sep {
  font: inherit;
}
.crumb-link {
  border: 0;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  padding: 0;
}
.crumb-current,
.crumb-link:hover,
.crumb-link:focus-visible {
  color: #002660;
}
.page-head h1 {
  margin: 0;
  color: #002660;
  font-size: 28px;
  font-weight: 900;
}
.page-head p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}
.head-actions {
  display: flex;
  gap: 12px;
}
.ghost-btn,
.primary-btn,
.decision-btn,
.mini-choice,
.text-btn,
.icon-btn {
  border-radius: 10px;
}
.ghost-btn,
.primary-btn {
  min-height: 42px;
  padding: 0 18px;
  border: 1px solid #dbe3ee;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}
.ghost-btn {
  background: #eef2f7;
  color: #334155;
}
.primary-btn {
  background: #002660;
  border-color: #002660;
  color: #fff;
}
.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(300px, 0.95fr);
  gap: 18px;
}
.main-column,
.side-column {
  display: grid;
  gap: 18px;
  align-content: start;
}
.panel {
  border: 1px solid #dbe3ee;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.06);
  overflow: hidden;
}
.panel--primary {
  background: linear-gradient(180deg, #002660 0%, #0c4a8a 100%);
  color: #fff;
  border-color: #002660;
}
.panel-head {
  padding: 18px 20px 0;
}
.panel-head--space {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.panel-head--light h2 {
  color: #fff;
}
.panel-head h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
  color: #002660;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 16px;
  padding: 20px;
}
.info-grid article {
  display: grid;
  gap: 6px;
}
.info-grid article.is-wide {
  grid-column: 1 / -1;
}
.info-grid span,
.field-block span {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}
.info-grid strong {
  color: #0f172a;
  font-size: 14px;
  line-height: 1.5;
}
.text-btn,
.icon-btn {
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}
.text-btn {
  min-height: 34px;
  padding: 0 14px;
}
.icon-btn {
  min-height: 32px;
  padding: 0 12px;
}
.item-list {
  display: grid;
  gap: 14px;
  padding: 20px;
}
.item-card {
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #f8fbff;
  padding: 16px;
  display: grid;
  gap: 12px;
}
.item-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}
.item-card__title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}
.item-index {
  min-width: 34px;
  height: 34px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 900;
}
.item-card input,
.item-card textarea,
.field-block input,
.field-block textarea {
  width: 100%;
  border: 1px solid #dbe3ee;
  border-radius: 12px;
  padding: 11px 12px;
  font-size: 14px;
  color: #0f172a;
  background: #fff;
  resize: vertical;
}
.item-card__foot {
  display: grid;
  gap: 12px;
}
.item-result-group {
  display: flex;
  gap: 10px;
}
.mini-choice {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid #dbe3ee;
  background: #fff;
  color: #334155;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}
.mini-choice.is-active.is-pass {
  border-color: #16a34a;
  background: #f0fdf4;
  color: #166534;
}
.mini-choice.is-active.is-fail {
  border-color: #dc2626;
  background: #fef2f2;
  color: #991b1b;
}
.notice-strip {
  margin: 14px 20px 0;
  border-radius: 12px;
  padding: 12px 14px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  line-height: 1.6;
}
.field-block {
  display: grid;
  gap: 8px;
  padding: 16px 20px 20px;
}
.decision-list {
  display: grid;
  gap: 12px;
  padding: 16px 20px 20px;
}
.decision-btn {
  border: 1px solid rgba(255, 255, 255, 0.28);
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  padding: 14px 16px;
  display: grid;
  gap: 6px;
  text-align: left;
  cursor: pointer;
}
.decision-btn strong {
  font-size: 15px;
}
.decision-btn span {
  font-size: 12px;
  line-height: 1.5;
  opacity: 0.84;
}
.decision-btn.is-active {
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.24) inset;
}
.decision-btn.is-active.is-pass {
  background: rgba(34, 197, 94, 0.22);
}
.decision-btn.is-active.is-rectify {
  background: rgba(251, 191, 36, 0.24);
}
.decision-btn.is-active.is-fail {
  background: rgba(248, 113, 113, 0.24);
}
.tip-list {
  margin: 0;
  padding: 12px 20px 20px 36px;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}
.timeline-list {
  display: grid;
  gap: 14px;
  padding: 14px 20px 20px;
}
.timeline-item {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}
.timeline-dot {
  width: 12px;
  height: 12px;
  margin-top: 4px;
  border-radius: 999px;
  background: #cbd5e1;
  box-shadow: 0 0 0 4px #eff6ff;
}
.timeline-dot.is-on {
  background: #2563eb;
}
.timeline-dot.is-alert {
  background: #f59e0b;
}
.timeline-body {
  display: grid;
  gap: 4px;
}
.timeline-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}
.timeline-top strong {
  color: #0f172a;
  font-size: 14px;
}
.timeline-top small,
.timeline-body p {
  color: #64748b;
  font-size: 12px;
}
.timeline-body p {
  margin: 0;
  line-height: 1.6;
}
.state-card,
.status-banner {
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 14px;
}
.state-card {
  border: 1px solid #dbe3ee;
  background: #fff;
  color: #334155;
}
.state-card--error,
.status-banner.is-error {
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}
.status-banner {
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}
.status-banner.is-success {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #166534;
}
@media (max-width: 1100px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 768px) {
  .page-head {
    flex-direction: column;
    align-items: stretch;
  }
  .head-actions {
    width: 100%;
  }
  .head-actions > button {
    flex: 1;
  }
  .info-grid {
    grid-template-columns: 1fr;
  }
  .item-card__head,
  .item-card__title,
  .timeline-top {
    flex-direction: column;
    align-items: stretch;
  }
  .item-result-group {
    flex-wrap: wrap;
  }
}
</style>
