<template>
  <RegulatorAdminWorkspacePage
    active-key="bulletins"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="bulletin-edit-page">
      <header class="op-bar">
        <div class="op-left">
          <button class="back-btn" type="button" @click="goList">返回列表</button>
          <div class="divider"></div>
          <div class="title-wrap">
            <h1>{{ isCreateMode ? "新建公告" : "编辑公告" }}</h1>
            <span class="draft-chip">{{ formatStatus(detailStatus) }}</span>
          </div>
        </div>
        <div class="op-actions">
          <button class="ghost" type="button" :disabled="saving || loading" @click="handleSaveDraft">保存草稿</button>
          <button class="primary" type="button" :disabled="saving || loading" @click="handlePublish">
            正式发布
          </button>
          <button
            v-if="!isCreateMode && detailStatus === 'PUBLISHED'"
            class="danger-ghost"
            type="button"
            :disabled="saving || loading"
            @click="handleOffline"
          >
            下线公告
          </button>
        </div>
      </header>

      <div class="workspace-grid">
        <div class="left-col">
          <section class="panel title-panel">
            <label>公告标题</label>
            <input v-model.trim="form.title" maxlength="120" placeholder="请输入公告标题（建议 30 字以内）" />
          </section>

          <section class="panel editor-panel">
            <div class="editor-toolbar">
              <div class="toolbar-group">
                <button type="button" class="tool-btn" @click="execEditorCommand('bold')">B</button>
                <button type="button" class="tool-btn" @click="execEditorCommand('italic')"><i>I</i></button>
                <button type="button" class="tool-btn" @click="execEditorCommand('underline')"><u>U</u></button>
                <span class="tool-divider"></span>
                <button type="button" class="tool-btn" @click="setBlock('p')">正文</button>
                <button type="button" class="tool-btn" @click="setBlock('h2')">标题</button>
                <span class="tool-divider"></span>
                <button type="button" class="tool-btn" @click="execEditorCommand('insertUnorderedList')">• 列表</button>
                <button type="button" class="tool-btn" @click="execEditorCommand('insertOrderedList')">1. 列表</button>
                <button type="button" class="tool-btn" @click="insertIndentation">缩进</button>
                <span class="tool-divider"></span>
                <button type="button" class="tool-btn" @click="execEditorCommand('justifyLeft')">左对齐</button>
                <button type="button" class="tool-btn" @click="execEditorCommand('justifyCenter')">居中</button>
                <button type="button" class="tool-btn" @click="execEditorCommand('justifyRight')">右对齐</button>
                <span class="tool-divider"></span>
                <button type="button" class="tool-btn" @click="insertLink">链接</button>
              </div>
            </div>
            <div
              ref="editorRef"
              class="editor-content"
              contenteditable="true"
              data-placeholder="请在这里输入公告正文内容..."
              @input="handleEditorInput"
            ></div>
          </section>
        </div>

        <aside class="right-col">
          <section class="panel attr-panel">
            <h3>发布属性</h3>
            <label>
              分类（待确认）
              <select v-model="form.category">
                <option value="">请选择分类</option>
                <option v-for="item in categoryOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </label>
            <div>
              <label class="upload-label">附件列表（待确认）</label>
              <div class="upload-box">
                <span class="upload-icon">☁</span>
                <span>点击或拖拽上传附件 (PDF, DOCX)</span>
              </div>
            </div>
          </section>

          <section class="panel log-panel">
            <h3>编辑日志</h3>
            <div class="timeline-list">
              <article class="timeline-item">
                <span class="timeline-dot timeline-dot--active"></span>
                <div>
                  <strong>{{ isCreateMode ? "创建草稿" : "最近编辑" }}</strong>
                  <p>{{ detailUpdatedTime ? formatTime(detailUpdatedTime) : "--" }}</p>
                </div>
              </article>
              <article class="timeline-item">
                <span class="timeline-dot"></span>
                <div>
                  <strong>待发布</strong>
                  <p>{{ formatStatus(detailStatus) }}</p>
                </div>
              </article>
            </div>
          </section>
        </aside>
      </div>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  createBulletin,
  fetchBulletinDetail,
  offlineBulletin,
  publishBulletin,
  updateBulletin
} from "../../api/regulation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const route = useRoute();
const router = useRouter();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();

const categoryOptions = [
  { value: "POLICY", label: "政策法规" },
  { value: "INSPECTION", label: "监督检查" },
  { value: "NOTICE", label: "消费提示" },
  { value: "OTHER", label: "其他公告" }
];

const loading = ref(false);
const saving = ref(false);
const editorRef = ref(null);
const bulletinId = ref(0);
const detailStatus = ref("DRAFT");
const detailUpdatedTime = ref("");
const status = reactive({ message: "", type: "" });
const form = reactive({
  title: "",
  category: "",
  content: ""
});

const isCreateMode = computed(() => !bulletinId.value);

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatStatus(value) {
  const map = {
    DRAFT: "草稿",
    PUBLISHED: "已发布",
    OFFLINE: "已下线"
  };
  return map[value] || value || "-";
}

function goList() {
  router.push({ name: "regulator-admin-bulletins" }).catch(() => {});
}

function validateForm() {
  if (!form.title.trim()) {
    setStatus("公告标题不能为空", "error");
    return false;
  }
  if (!form.category) {
    setStatus("请选择公告类别", "error");
    return false;
  }
  if (!getPlainTextFromHtml(form.content).trim()) {
    setStatus("公告正文不能为空", "error");
    return false;
  }
  return true;
}

function buildPayload() {
  return {
    title: form.title,
    category: form.category,
    content: form.content
  };
}

function getPlainTextFromHtml(html) {
  const parser = new DOMParser();
  const doc = parser.parseFromString(html || "", "text/html");
  return doc.body.textContent || "";
}

function normalizeContentHtml(value) {
  const html = String(value || "").trim();
  if (!html) return "";
  return html;
}

function syncEditorFromModel() {
  if (!editorRef.value) return;
  editorRef.value.innerHTML = normalizeContentHtml(form.content);
}

function handleEditorInput() {
  if (!editorRef.value) return;
  form.content = editorRef.value.innerHTML;
}

function execEditorCommand(command, value = null) {
  if (!editorRef.value) return;
  editorRef.value.focus();
  if (value === null) document.execCommand(command);
  else document.execCommand(command, false, value);
  handleEditorInput();
}

function setBlock(tag) {
  execEditorCommand("formatBlock", tag.toUpperCase());
}

function insertIndentation() {
  execEditorCommand("insertHTML", "&emsp;&emsp;");
}

function insertLink() {
  const url = window.prompt("请输入链接地址（含 http/https）");
  if (!url) return;
  execEditorCommand("createLink", url);
}

async function loadDetail() {
  const id = Number(route.params.bulletinId || 0);
  bulletinId.value = id > 0 ? id : 0;
  if (!bulletinId.value) {
    detailStatus.value = "DRAFT";
    detailUpdatedTime.value = "";
    form.title = "";
    form.category = "";
    form.content = "";
    syncEditorFromModel();
    return;
  }
  loading.value = true;
  setStatus("");
  try {
    const detail = await fetchBulletinDetail(token.value, bulletinId.value);
    form.title = detail.title || "";
    form.category = detail.category || "";
    form.content = detail.content || "";
    syncEditorFromModel();
    detailStatus.value = detail.status || "DRAFT";
    detailUpdatedTime.value = detail.updateTime || detail.publishedTime || detail.createTime || "";
  } catch (error) {
    setStatus(error.message || "加载公告详情失败", "error");
  } finally {
    loading.value = false;
  }
}

async function handleSaveDraft() {
  if (!validateForm()) return;
  saving.value = true;
  setStatus("");
  try {
    const payload = buildPayload();
    if (isCreateMode.value) {
      const created = await createBulletin(token.value, payload);
      const nextId = Number(created?.id || 0);
      setStatus("公告草稿已创建", "success");
      if (nextId > 0) {
        await router.replace({ name: "regulator-admin-bulletin-edit", params: { bulletinId: nextId } });
      } else {
        await loadDetail();
      }
    } else {
      await updateBulletin(token.value, bulletinId.value, payload);
      setStatus("公告草稿已更新", "success");
      await loadDetail();
    }
  } catch (error) {
    setStatus(error.message || "保存草稿失败", "error");
  } finally {
    saving.value = false;
  }
}

async function handlePublish() {
  if (isCreateMode.value) {
    await handleSaveDraft();
    if (isCreateMode.value) return;
  } else if (!validateForm()) {
    return;
  } else {
    saving.value = true;
    setStatus("");
    try {
      await updateBulletin(token.value, bulletinId.value, buildPayload());
    } catch (error) {
      saving.value = false;
      setStatus(error.message || "更新公告失败", "error");
      return;
    } finally {
      saving.value = false;
    }
  }
  saving.value = true;
  setStatus("");
  try {
    await publishBulletin(token.value, bulletinId.value);
    setStatus("公告已正式发布", "success");
    await loadDetail();
  } catch (error) {
    setStatus(error.message || "发布公告失败", "error");
  } finally {
    saving.value = false;
  }
}

async function handleOffline() {
  if (isCreateMode.value) return;
  saving.value = true;
  setStatus("");
  try {
    await offlineBulletin(token.value, bulletinId.value);
    setStatus("公告已下线", "success");
    await loadDetail();
  } catch (error) {
    setStatus(error.message || "下线公告失败", "error");
  } finally {
    saving.value = false;
  }
}

onMounted(loadDetail);
watch(() => route.params.bulletinId, loadDetail);
</script>

<style scoped>
.bulletin-edit-page { display: grid; gap: 12px; }
.op-bar {
  background: #fff;
  border-radius: 4px;
  padding: 10px 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.op-left { display: flex; align-items: center; gap: 10px; }
.back-btn { border: 0; background: transparent; color: #475569; font-size: 14px; font-weight: 700; cursor: pointer; }
.divider { width: 1px; height: 16px; background: #cbd5e1; }
.title-wrap { display: flex; align-items: center; gap: 8px; }
.title-wrap h1 { margin: 0; font-size: 20px; color: #002660; font-weight: 900; }
.draft-chip { min-height: 18px; padding: 0 6px; border-radius: 2px; background: #e2e8f0; color: #475569; font-size: 10px; font-weight: 900; display: inline-flex; align-items: center; }
.op-actions { display: flex; gap: 8px; flex-wrap: wrap; }

.workspace-grid { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 14px; align-items: start; }
.left-col, .right-col { display: grid; gap: 12px; }
.panel { background: #fff; border: 1px solid #e9edf2; border-radius: 3px; padding: 14px; }
.title-panel label { display: block; font-size: 10px; color: #64748b; font-weight: 900; letter-spacing: 0.08em; text-transform: uppercase; margin-bottom: 6px; }
.title-panel input {
  width: 100%;
  min-height: 42px;
  border: 0;
  border-radius: 3px;
  padding: 0;
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  background: transparent;
  outline: none;
  box-shadow: none;
  appearance: none;
}
.title-panel input::placeholder {
  color: #94a3b8;
}
.title-panel input:focus {
  outline: none;
  box-shadow: none;
  border-color: transparent;
}

.editor-panel { min-height: 560px; display: grid; grid-template-rows: auto 1fr; padding: 0; overflow: hidden; }
.editor-toolbar { min-height: 40px; border-bottom: 1px solid #edf2f7; display: flex; align-items: center; padding: 0 8px; background: #fff; }
.toolbar-group { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.tool-btn {
  min-height: 26px;
  border: 1px solid #dbe2ea;
  border-radius: 3px;
  background: #fff;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  padding: 0 8px;
  cursor: pointer;
}
.tool-btn:hover { background: #f8fafc; }
.tool-divider { width: 1px; height: 14px; background: #dbe2ea; margin: 0 2px; }
.editor-content {
  width: 100%;
  height: 100%;
  border: 0;
  outline: none;
  padding: 16px;
  font-size: 13px;
  color: #334155;
  line-height: 1.8;
  background: #fbfcfd;
  box-shadow: none;
  appearance: none;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}
.editor-content:focus {
  outline: none;
  box-shadow: none;
  border-color: transparent;
}
.editor-content:empty::before {
  content: attr(data-placeholder);
  color: #94a3b8;
}
.editor-content :deep(p) { margin: 0 0 8px; }
.editor-content :deep(h2) { margin: 8px 0; font-size: 20px; color: #0f172a; font-weight: 800; }
.editor-content :deep(ul),
.editor-content :deep(ol) { margin: 8px 0 8px 24px; }
.editor-content :deep(a) { color: #1d4ed8; text-decoration: underline; }

.attr-panel, .log-panel { background: #f3f5f8; border-color: #e6ebf1; }
.attr-panel h3, .log-panel h3 {
  margin: 0 0 10px;
  color: #334155;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.attr-panel { display: grid; gap: 10px; }
.attr-panel label { display: grid; gap: 6px; color: #64748b; font-size: 11px; font-weight: 800; }
.attr-panel input, .attr-panel select {
  min-height: 34px;
  border: 1px solid #dde4ed;
  border-radius: 3px;
  padding: 0 10px;
  font-size: 12px;
  background: #e9edf2;
}
.upload-label { display: block; margin-bottom: 6px; color: #64748b; font-size: 11px; font-weight: 800; }
.upload-box {
  min-height: 96px;
  border: 1px dashed #cbd5e1;
  border-radius: 3px;
  display: grid;
  place-content: center;
  gap: 6px;
  color: #94a3b8;
  font-size: 11px;
  text-align: center;
  background: #f8fafc;
}
.upload-icon { font-size: 18px; line-height: 1; }
.timeline-list { display: grid; gap: 10px; }
.timeline-item { display: grid; grid-template-columns: 10px 1fr; gap: 8px; }
.timeline-dot { width: 6px; height: 6px; border-radius: 50%; margin-top: 5px; background: #cbd5e1; }
.timeline-dot--active { background: #335bae; }
.timeline-item strong { display: block; color: #334155; font-size: 12px; }
.timeline-item p { margin: 2px 0 0; color: #94a3b8; font-size: 10px; }

.primary, .ghost, .danger-ghost {
  min-height: 34px;
  border-radius: 3px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}
.primary { border: 0; background: #002660; color: #fff; }
.ghost { border: 1px solid #d1d5db; background: #fff; color: #334155; }
.danger-ghost { border: 1px solid #fecaca; background: #fff1f2; color: #991b1b; }

.status { position: fixed; right: 18px; bottom: 18px; border-radius: 3px; padding: 10px 12px; color: #fff; background: #0f172a; font-size: 13px; z-index: 1300; }
.status.error { background: #b91c1c; }
.status.success { background: #166534; }

@media (max-width: 1100px) {
  .workspace-grid { grid-template-columns: 1fr; }
}
</style>
