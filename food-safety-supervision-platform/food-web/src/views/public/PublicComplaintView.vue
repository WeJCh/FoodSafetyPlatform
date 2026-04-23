<template>
  <div class="public-complaint-page">
    <header class="public-complaint-page__topbar">
      <div class="public-complaint-page__topbar-inner">
        <div class="public-complaint-page__brand-nav">
          <span class="public-complaint-page__brand">食品安全监管平台</span>
          <nav class="public-complaint-page__nav" aria-label="公众导航">
            <button
              v-for="item in topNavItems"
              :key="item.key"
              type="button"
              class="public-complaint-page__nav-item"
              :class="{ 'is-active': item.key === 'complaint-create' }"
              @click="goTo(item.routeName)"
            >
              {{ item.label }}
            </button>
          </nav>
        </div>
        <div class="public-complaint-page__toolbar">
          <button type="button" class="public-complaint-page__icon-btn" @click="onFeaturePending('通知中心')">
            <span class="material-symbols-outlined" aria-hidden="true">notifications</span>
          </button>
          <button type="button" class="public-complaint-page__icon-btn" @click="onFeaturePending('个人中心')">
            <span class="material-symbols-outlined" aria-hidden="true">account_circle</span>
          </button>
          <button type="button" class="ghost public-complaint-page__logout" @click="handleLogout">退出登录</button>
        </div>
      </div>
    </header>

    <main class="public-complaint-page__main">
      <section class="public-complaint-page__head">
        <p>便民服务 / 我要投诉</p>
        <h1>我要投诉</h1>
        <span>请如实填写信息，平台将依法流转并反馈处理结果。</span>
      </section>

      <section class="public-complaint-page__layout">
        <article class="public-complaint-page__card">
          <h3>投诉信息填报</h3>
          <form class="public-complaint-page__form" @submit.prevent="handleSubmit">
            <section class="public-complaint-page__step">
              <header class="public-complaint-page__step-head">
                <i>1</i>
                <div>
                  <strong>第一步：选择投诉对象</strong>
                  <span>支持按企业名称搜索并自动回填基础信息。</span>
                </div>
              </header>
              <div class="public-complaint-page__step-body public-complaint-page__grid">
                <label class="public-complaint-page__combo-field">
                  <span>投诉企业</span>
                  <input
                    v-model.trim="enterpriseQuery"
                    placeholder="输入企业名称关键词"
                    @focus="openEnterpriseDropdown"
                    @input="handleEnterpriseInput"
                    @blur="scheduleCloseEnterpriseDropdown"
                  />
                  <div v-if="enterpriseDropdownOpen" class="public-complaint-page__combo-panel">
                    <div v-if="enterpriseLoading" class="public-complaint-page__combo-state">加载中...</div>
                    <button
                      v-for="item in enterpriseOptions"
                      :key="item.id"
                      type="button"
                      class="public-complaint-page__combo-item"
                      @mousedown.prevent="selectEnterprise(item)"
                    >
                      <strong>{{ item.enterpriseName }}</strong>
                      <small>{{ [item.regionPathText, item.addressDetail].filter(Boolean).join(" · ") || "-" }}</small>
                    </button>
                    <div v-if="!enterpriseLoading && !enterpriseOptions.length" class="public-complaint-page__combo-state">
                      暂无匹配企业
                    </div>
                    <button
                      v-if="enterpriseHasMore"
                      type="button"
                      class="ghost public-complaint-page__combo-more"
                      @mousedown.prevent="loadMoreEnterprises"
                    >
                      加载更多
                    </button>
                  </div>
                </label>
                <label>
                  <span>企业编号</span>
                  <input v-model="form.enterpriseId" readonly placeholder="选择企业后自动填充" />
                </label>
                <label>
                  <span>所在区域</span>
                  <input v-model.trim="form.region" placeholder="例：浙江省/杭州市/西湖区" @input="regionEdited = true" />
                </label>
                <label>
                  <span>详细地址</span>
                  <input v-model.trim="form.addressDetail" placeholder="街道、门牌号" @input="addressEdited = true" />
                </label>
              </div>
            </section>

            <section class="public-complaint-page__step">
              <header class="public-complaint-page__step-head">
                <i>2</i>
                <div>
                  <strong>第二步：投诉详情</strong>
                  <span>请尽量描述清楚时间、地点和问题现象。</span>
                </div>
              </header>
              <div class="public-complaint-page__step-body">
                <div class="public-complaint-page__grid public-complaint-page__grid--compact">
                  <label>
                    <span>投诉类型</span>
                    <select v-model="form.complaintType">
                      <option value="">请选择投诉类型</option>
                      <option value="食品过期">食品过期</option>
                      <option value="卫生不达标">卫生不达标</option>
                      <option value="无证经营">无证经营</option>
                      <option value="其他">其他</option>
                    </select>
                  </label>
                </div>
                <label>
                  <span>问题描述</span>
                  <textarea v-model.trim="form.content" rows="5" placeholder="请详细描述投诉内容" required />
                </label>
              </div>
            </section>

            <section class="public-complaint-page__step">
              <header class="public-complaint-page__step-head">
                <i>3</i>
                <div>
                  <strong>第三步：隐私设置</strong>
                  <span>可选择匿名投诉并隐藏联系方式。</span>
                </div>
              </header>
              <div class="public-complaint-page__step-body">
                <div class="public-complaint-page__privacy-row">
                  <div class="public-complaint-page__privacy-tip">
                    <span class="material-symbols-outlined" aria-hidden="true">visibility_off</span>
                    <div>
                      <strong>匿名投诉</strong>
                      <small>启用后，联系方式等个人身份信息仅监管方可见。</small>
                    </div>
                  </div>
                  <label class="public-complaint-page__checkbox">
                    <input type="checkbox" v-model="form.anonymous" @change="handleAnonymousToggle" />
                    匿名投诉（隐藏联系方式）
                  </label>
                </div>
              </div>
            </section>

            <section class="public-complaint-page__step">
              <header class="public-complaint-page__step-head">
                <i>4</i>
                <div>
                  <strong>第四步：证据上传</strong>
                  <span>上传现场图片可帮助监管部门快速核实。</span>
                </div>
              </header>
              <div class="public-complaint-page__step-body">
                <div class="public-complaint-page__upload">
                  <div class="public-complaint-page__upload-head">
                    <strong>现场图片</strong>
                    <small>最多 5 张，支持 JPG/PNG/WebP，单张不超过 5MB</small>
                  </div>
                  <label class="public-complaint-page__file-picker">
                    <input type="file" multiple accept="image/*" @change="handleFileChange" />
                    <span class="public-complaint-page__file-picker-text">
                      <strong>上传证据图片</strong>
                      <small>{{ uploadPickerSubText }}</small>
                    </span>
                  </label>
                  <div v-if="!uploadItems.length" class="public-complaint-page__upload-empty">
                    <span class="material-symbols-outlined" aria-hidden="true">add_photo_alternate</span>
                    <p>点击上方按钮上传证据图片</p>
                  </div>
                  <div v-if="uploadItems.length" class="public-complaint-page__preview-grid">
                    <article v-for="item in uploadItems" :key="item.id" class="public-complaint-page__preview-item">
                      <img :src="item.previewUrl" alt="投诉图片" />
                      <div class="public-complaint-page__preview-meta" :class="{ 'is-error': item.error }">
                        <span v-if="item.uploading">上传中...</span>
                        <span v-else-if="item.error">上传失败</span>
                        <span v-else>已上传</span>
                      </div>
                      <button v-if="item.error" type="button" class="ghost" @click="retryUpload(item)">重试</button>
                      <button type="button" class="ghost" @click="removeImage(item.id)">移除</button>
                    </article>
                  </div>
                </div>
              </div>
            </section>

            <div v-if="!form.anonymous" class="public-complaint-page__grid">
              <label>
                <span>真实姓名</span>
                <input v-model.trim="form.complainantName" placeholder="可选填写" />
              </label>
              <label>
                <span>联系方式</span>
                <input v-model.trim="form.contact" placeholder="请填写手机号" />
              </label>
            </div>
            <div class="public-complaint-page__actions">
              <button type="submit" :disabled="loading">{{ loading ? "提交中..." : "提交投诉" }}</button>
              <button type="button" class="ghost" @click="resetForm">重置内容</button>
            </div>
          </form>
        </article>

        <aside class="public-complaint-page__aside">
          <section>
            <h4>投诉须知</h4>
            <ul>
              <li>请确保投诉内容真实、客观，避免夸大或捏造事实。</li>
              <li>平台将依法保护投诉人隐私信息，可选择匿名投诉。</li>
              <li>提交后会生成投诉编号，可在“我的投诉”中持续追踪。</li>
            </ul>
          </section>
          <section>
            <h4>处理时效</h4>
            <p>一般将在 3-5 个工作日内完成受理与分派，复杂案件会同步更新进度说明。</p>
          </section>
        </aside>
      </section>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { submitPublicComplaint } from "../../api/complaint";
import { presignUpload } from "../../api/file";
import { fetchPublicEnterprises } from "../../api/regulation";
import { getActiveSession, performLogout } from "../../session/authRuntime";

const router = useRouter();
const publicToken = getActiveSession()?.token || "";

const topNavItems = [
  { key: "home", label: "首页", routeName: "public-home" },
  { key: "bulletins", label: "监管公告", routeName: "public-bulletins" },
  { key: "enterprises", label: "企业公示", routeName: "public-enterprises" },
  { key: "sampling", label: "抽检结果", routeName: "public-sampling-results" },
  { key: "complaint-create", label: "我要投诉", routeName: "public-complaint-create" },
  { key: "complaints", label: "我的投诉", routeName: "public-complaints" }
];

const form = reactive({
  enterpriseName: "",
  enterpriseId: "",
  region: "",
  addressDetail: "",
  complaintType: "",
  content: "",
  complainantName: "",
  contact: "",
  anonymous: false
});

const MAX_IMAGE_COUNT = 5;
const MAX_FILE_SIZE = 5 * 1024 * 1024;
const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/webp"];

const uploadItems = ref([]);
const loading = ref(false);
const status = reactive({ message: "", type: "" });
const enterpriseQuery = ref("");
const enterpriseOptions = ref([]);
const enterpriseLoading = ref(false);
const enterpriseDropdownOpen = ref(false);
const enterprisePage = ref(1);
const enterpriseSize = 10;
const enterpriseHasMore = ref(false);
const regionEdited = ref(false);
const addressEdited = ref(false);
const isUploading = computed(() => uploadItems.value.some((item) => item.uploading));
const uploadPickerSubText = computed(() => {
  if (!uploadItems.value.length) return "支持 JPG/PNG/WebP，最多 5 张";
  const uploadingCount = uploadItems.value.filter((item) => item.uploading).length;
  if (uploadingCount) return `已选 ${uploadItems.value.length} 张，上传中 ${uploadingCount} 张`;
  return `已选择 ${uploadItems.value.length} 张图片`;
});
let enterpriseSearchTimer = null;

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}
function goTo(name) {
  router.push({ name }).catch(() => {});
}
function onFeaturePending(name) {
  window.alert(`${name} 功能待后续完善`);
}
async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
}
function handleAnonymousToggle() {
  if (form.anonymous) {
    form.complainantName = "";
    form.contact = "";
  }
}
function openEnterpriseDropdown() {
  enterpriseDropdownOpen.value = true;
  if (!enterpriseOptions.value.length) loadEnterprises(true);
}
function scheduleCloseEnterpriseDropdown() {
  window.setTimeout(() => {
    enterpriseDropdownOpen.value = false;
  }, 150);
}
function handleEnterpriseInput() {
  enterpriseDropdownOpen.value = true;
  if (enterpriseQuery.value !== form.enterpriseName) {
    form.enterpriseName = "";
    form.enterpriseId = "";
  }
  if (enterpriseSearchTimer) window.clearTimeout(enterpriseSearchTimer);
  enterpriseSearchTimer = window.setTimeout(() => loadEnterprises(true), 300);
}
async function loadEnterprises(reset) {
  if (enterpriseLoading.value) return;
  if (!publicToken) {
    setStatus("请先登录后再查询企业", "error");
    return;
  }
  enterpriseLoading.value = true;
  if (reset) enterprisePage.value = 1;
  try {
    const data = await fetchPublicEnterprises(publicToken, {
      enterpriseName: enterpriseQuery.value,
      page: enterprisePage.value,
      size: enterpriseSize
    });
    const records = data?.records || [];
    enterpriseOptions.value = reset ? records : [...enterpriseOptions.value, ...records];
    const total = data?.total ?? enterpriseOptions.value.length;
    enterpriseHasMore.value = enterpriseOptions.value.length < total;
  } catch (error) {
    setStatus(error.message || "企业列表加载失败", "error");
  } finally {
    enterpriseLoading.value = false;
  }
}
function loadMoreEnterprises() {
  if (enterpriseLoading.value || !enterpriseHasMore.value) return;
  enterprisePage.value += 1;
  loadEnterprises(false);
}
function selectEnterprise(item) {
  form.enterpriseId = String(item.id || "");
  form.enterpriseName = item.enterpriseName || "";
  enterpriseQuery.value = form.enterpriseName;
  if (!regionEdited.value) form.region = item.regionPathText || "";
  if (!addressEdited.value) form.addressDetail = item.addressDetail || "";
  enterpriseDropdownOpen.value = false;
}
function validateFile(file) {
  if (!ALLOWED_TYPES.includes(file.type)) return "仅支持 JPG/PNG/WebP 图片";
  if (file.size > MAX_FILE_SIZE) return "单张图片不能超过 5MB";
  return "";
}
function createUploadItem(file) {
  return {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    name: file.name,
    file,
    previewUrl: URL.createObjectURL(file),
    fileUrl: "",
    objectKey: "",
    uploading: true,
    error: ""
  };
}
async function uploadFile(item) {
  try {
    const payload = {
      filename: item.name,
      contentType: item.file.type || "application/octet-stream",
      size: item.file.size,
      bizType: "COMPLAINT"
    };
    const presign = await presignUpload(publicToken, payload);
    const response = await fetch(presign.uploadUrl, {
      method: "PUT",
      headers: { "Content-Type": payload.contentType },
      body: item.file
    });
    if (!response.ok) throw new Error(`上传失败 (${response.status})`);
    item.fileUrl = presign.fileUrl;
    item.objectKey = presign.objectKey;
    item.uploading = false;
  } catch (error) {
    item.error = error.message || "上传失败";
    item.uploading = false;
  }
}
function handleFileChange(event) {
  if (!publicToken) {
    setStatus("请先登录后再上传图片", "error");
    return;
  }
  const files = Array.from(event.target.files || []);
  if (!files.length) return;
  const remaining = MAX_IMAGE_COUNT - uploadItems.value.length;
  if (remaining <= 0) {
    setStatus(`最多上传 ${MAX_IMAGE_COUNT} 张图片`, "error");
    return;
  }
  files.slice(0, remaining).forEach((file) => {
    const error = validateFile(file);
    if (error) {
      setStatus(error, "error");
      return;
    }
    const item = createUploadItem(file);
    uploadItems.value = [...uploadItems.value, item];
    uploadFile(item);
  });
  event.target.value = "";
}
function removeImage(id) {
  const target = uploadItems.value.find((item) => item.id === id);
  if (target?.previewUrl) URL.revokeObjectURL(target.previewUrl);
  uploadItems.value = uploadItems.value.filter((item) => item.id !== id);
}
function retryUpload(item) {
  if (!item || !item.file) return;
  item.error = "";
  item.uploading = true;
  uploadFile(item);
}
async function handleSubmit() {
  if (!form.enterpriseId.trim()) return setStatus("请先选择投诉企业", "error");
  const enterpriseId = Number(form.enterpriseId);
  if (!Number.isFinite(enterpriseId)) return setStatus("企业编号格式不正确", "error");
  if (!form.content.trim()) return setStatus("请填写投诉内容", "error");
  if (!form.anonymous && !form.contact.trim()) return setStatus("请填写联系方式或选择匿名投诉", "error");
  if (isUploading.value) return setStatus("图片上传中，请稍后提交", "error");
  if (uploadItems.value.some((item) => item.error)) return setStatus("存在上传失败图片，请处理后重试", "error");
  loading.value = true;
  setStatus("");
  try {
    const imageUrls = uploadItems.value.map((item) => item.fileUrl).filter(Boolean);
    const payload = {
      enterpriseId,
      complaintType: form.complaintType || undefined,
      content: form.content,
      contact: form.anonymous ? undefined : form.contact,
      complainantName: form.anonymous ? undefined : form.complainantName,
      imageUrls: imageUrls.length ? imageUrls : undefined
    };
    const result = await submitPublicComplaint(publicToken, payload);
    setStatus("投诉提交成功", "success");
    router.push({
      name: "public-complaint-submit-success",
      query: {
        complaintNo: result?.complaintNo || "",
        status: result?.status || "SUBMITTED"
      }
    }).catch(() => {});
  } catch (error) {
    setStatus(error.message || "投诉提交失败", "error");
  } finally {
    loading.value = false;
  }
}
function resetForm() {
  form.enterpriseName = "";
  form.enterpriseId = "";
  form.region = "";
  form.addressDetail = "";
  form.complaintType = "";
  form.content = "";
  form.complainantName = "";
  form.contact = "";
  form.anonymous = false;
  uploadItems.value.forEach((item) => item.previewUrl && URL.revokeObjectURL(item.previewUrl));
  uploadItems.value = [];
  enterpriseQuery.value = "";
  regionEdited.value = false;
  addressEdited.value = false;
  setStatus("");
}

onMounted(() => {
  if (publicToken) loadEnterprises(true);
});
</script>

<style scoped>
.public-complaint-page { min-height: 100vh; background: var(--surface); }
.public-complaint-page__topbar { position: sticky; top: 0; z-index: 40; border-bottom: 1px solid rgba(195,198,211,.4); background: rgba(248,250,253,.84); }
.public-complaint-page__topbar-inner { max-width: 1680px; margin: 0 auto; min-height: var(--public-topbar-min-h); padding: 0 16px; display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.public-complaint-page__brand-nav { display: flex; align-items: center; gap: 24px; }
.public-complaint-page__brand { font-family: var(--font-display); font-size: var(--public-brand-size); font-weight: 800; color: var(--primary); }
.public-complaint-page__nav { display: flex; gap: 18px; }
.public-complaint-page__nav-item { border: none; background: transparent; min-height: var(--public-topbar-min-h); color: var(--on-surface-variant); font-size: var(--public-nav-size); font-weight: 700; border-bottom: 2px solid transparent; cursor: pointer; }
.public-complaint-page__nav-item.is-active { color: var(--primary); border-bottom-color: var(--primary); }
.public-complaint-page__toolbar { display: flex; align-items: center; gap: 10px; }
.public-complaint-page__icon-btn { width: var(--public-btn-compact-min-h); height: var(--public-btn-compact-min-h); border-radius: 8px; border: 1px solid transparent; background: transparent; color: var(--on-surface-variant); cursor: pointer; }
.public-complaint-page__logout { min-height: var(--public-toolbar-min-h); font-size: var(--public-logout-font-size); margin: 0; }
.public-complaint-page__main { max-width: 1680px; margin: 0 auto; padding: 24px 16px 48px; display: grid; gap: 14px; }
.public-complaint-page__head h1 { margin: 4px 0 6px; font-size: var(--public-hero-title-alt); line-height: 1; color: var(--primary); font-family: var(--font-display); }
.public-complaint-page__head p { margin: 0; font-size: var(--public-caption); color: var(--on-surface-variant); }
.public-complaint-page__head span { font-size: var(--public-caption); color: var(--on-surface-variant); }
.public-complaint-page__layout { display: grid; grid-template-columns: minmax(0, 1.3fr) minmax(260px, .7fr); gap: 14px; }
.public-complaint-page__card,
.public-complaint-page__aside section,
.public-complaint-page__aside section { border: 1px solid rgba(195,198,211,.3); border-radius: 10px; background: var(--surface-container-lowest); padding: 14px; }
.public-complaint-page__card h3,
.public-complaint-page__card h3 { margin: 0 0 12px; font-size: var(--public-text-md); color: var(--primary); letter-spacing: .05em; text-transform: uppercase; }
.public-complaint-page__form { display: grid; gap: 12px; }
.public-complaint-page__grid { display: grid; gap: 10px; grid-template-columns: 1fr 1fr; }
.public-complaint-page__grid--compact { grid-template-columns: minmax(260px, 420px); }
.public-complaint-page__form label { display: grid; gap: 6px; }
.public-complaint-page__form label > span { font-size: var(--public-overline); color: var(--on-surface-variant); }
.public-complaint-page__form input,
.public-complaint-page__form select,
.public-complaint-page__form textarea { border: 1px solid rgba(195,198,211,.6); border-radius: 8px; min-height: var(--public-btn-compact-min-h); padding: 0 10px; background: #fff; font-size: var(--public-control); }
.public-complaint-page__form textarea { min-height: 110px; padding-top: 10px; resize: vertical; }
.public-complaint-page__step { border: 1px solid rgba(195,198,211,.45); border-radius: 10px; background: var(--surface-container-low); }
.public-complaint-page__step-head { display: flex; align-items: flex-start; gap: 10px; padding: 10px 12px; border-bottom: 1px solid rgba(195,198,211,.38); }
.public-complaint-page__step-head i { width: 22px; height: 22px; border-radius: 50%; background: rgba(70,89,231,.12); color: var(--primary); font-style: normal; font-size: var(--public-overline); font-weight: 800; display: inline-flex; align-items: center; justify-content: center; margin-top: 1px; }
.public-complaint-page__step-head strong { display: block; font-size: var(--public-caption); color: var(--primary); }
.public-complaint-page__step-head span { display: block; margin-top: 2px; font-size: var(--public-overline); color: var(--on-surface-variant); }
.public-complaint-page__step-body { padding: 12px; display: grid; gap: 10px; }
.public-complaint-page__combo-field { position: relative; }
.public-complaint-page__combo-panel { position: absolute; top: calc(100% + 4px); left: 0; right: 0; z-index: 10; border: 1px solid rgba(195,198,211,.52); border-radius: 8px; background: #fff; max-height: 240px; overflow: auto; display: grid; gap: 4px; padding: 6px; }
.public-complaint-page__combo-item { border: 1px solid transparent; background: var(--surface-container-low); border-radius: 6px; padding: 8px; text-align: left; display: grid; gap: 2px; cursor: pointer; }
.public-complaint-page__combo-item:hover { border-color: rgba(70, 89, 231, .3); }
.public-complaint-page__combo-item strong { font-size: var(--public-caption); }
.public-complaint-page__combo-item small { color: var(--on-surface-variant); font-size: var(--public-overline); }
.public-complaint-page__combo-state { font-size: var(--public-caption); color: var(--on-surface-variant); padding: 6px; }
.public-complaint-page__combo-more { margin-top: 2px; min-height: var(--public-chip-min-h); }
.public-complaint-page__upload {
  border: 1px dashed rgba(195,198,211,.7);
  border-radius: 10px;
  padding: 12px;
  display: grid;
  gap: 10px;
  background: rgba(248, 250, 253, .55);
}
.public-complaint-page__upload-head { display: grid; gap: 2px; }
.public-complaint-page__upload-head strong { font-size: var(--public-caption); }
.public-complaint-page__upload-head small { font-size: var(--public-overline); color: var(--on-surface-variant); }
.public-complaint-page__file-picker {
  position: relative;
  min-height: 64px;
  border: 1px solid rgba(70,89,231,.24);
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  padding: 0 12px 0 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  cursor: pointer;
  transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease;
}
.public-complaint-page__file-picker:hover {
  border-color: rgba(70,89,231,.5);
  box-shadow: 0 6px 16px rgba(30,55,120,.1);
  transform: translateY(-1px);
}
.public-complaint-page__file-picker:focus-within {
  border-color: rgba(70,89,231,.62);
  box-shadow: 0 0 0 3px rgba(70,89,231,.14);
}
.public-complaint-page__file-picker input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}
.public-complaint-page__file-picker-text {
  display: grid;
  gap: 2px;
  min-width: 0;
  text-align: center;
}
.public-complaint-page__file-picker-text strong {
  font-size: var(--public-caption);
  color: var(--on-surface);
  font-weight: 700;
  line-height: 1.2;
}
.public-complaint-page__file-picker-text small {
  font-size: var(--public-overline);
  color: var(--on-surface-variant);
  line-height: 1.2;
}
.public-complaint-page__upload-empty { min-height: 92px; border: 1px dashed rgba(195,198,211,.6); border-radius: 8px; display: grid; place-items: center; gap: 4px; color: var(--on-surface-variant); background: rgba(248,250,253,.75); }
.public-complaint-page__upload-empty .material-symbols-outlined { font-size: var(--public-icon-lg); color: #5f6f9b; }
.public-complaint-page__upload-empty p { margin: 0; font-size: var(--public-overline); }
.public-complaint-page__preview-grid { display: grid; gap: 8px; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); }
.public-complaint-page__preview-item { border: 1px solid rgba(195,198,211,.46); border-radius: 8px; padding: 6px; display: grid; gap: 4px; background: var(--surface-container-low); }
.public-complaint-page__preview-item img { width: 100%; height: 90px; object-fit: cover; border-radius: 6px; }
.public-complaint-page__preview-meta { font-size: var(--public-overline); color: var(--on-surface-variant); }
.public-complaint-page__preview-meta.is-error { color: #ba1a1a; }
.public-complaint-page__checkbox { display: inline-flex !important; align-items: center; gap: 8px; font-size: var(--public-caption); color: var(--on-surface-variant); }
.public-complaint-page__privacy-row { border: 1px solid rgba(195,198,211,.45); border-radius: 8px; background: #fff; padding: 10px; display: grid; gap: 8px; }
.public-complaint-page__privacy-tip { display: flex; align-items: flex-start; gap: 8px; }
.public-complaint-page__privacy-tip .material-symbols-outlined { font-size: var(--public-body-em); color: var(--primary); margin-top: 1px; }
.public-complaint-page__privacy-tip strong { display: block; font-size: var(--public-caption); color: var(--on-surface); }
.public-complaint-page__privacy-tip small { display: block; font-size: var(--public-overline); color: var(--on-surface-variant); margin-top: 2px; }
.public-complaint-page__actions { display: flex; gap: 8px; flex-wrap: wrap; }
.public-complaint-page__actions button { min-height: var(--public-btn-compact-min-h); border-radius: 8px; padding: 0 12px; border: 1px solid transparent; background: var(--primary); color: #fff; font-size: var(--public-btn); font-weight: 700; cursor: pointer; }
.public-complaint-page__actions button.ghost { background: #fff; color: var(--on-surface-variant); border-color: rgba(195,198,211,.6); }
.public-complaint-page__aside { display: grid; gap: 12px; align-content: start; }
.public-complaint-page__aside h4 { margin: 0 0 8px; font-size: var(--public-caption); color: var(--primary); letter-spacing: .05em; text-transform: uppercase; }
.public-complaint-page__aside ul { margin: 0; padding-left: 18px; display: grid; gap: 6px; color: var(--on-surface-variant); font-size: var(--public-caption); }
.public-complaint-page__aside p { margin: 0; color: var(--on-surface-variant); font-size: var(--public-caption); line-height: 1.6; }
@media (max-width: 1100px) { .public-complaint-page__nav { display: none; } .public-complaint-page__layout { grid-template-columns: 1fr; } }
@media (max-width: 760px) { .public-complaint-page__toolbar { display: none; } .public-complaint-page__grid { grid-template-columns: 1fr; } .public-complaint-page__head h1 { font-size: var(--public-page-title-xs); } }
@media (max-width: 560px) {
  .public-complaint-page__file-picker {
    min-height: 72px;
    align-items: center;
    padding-top: 10px;
    padding-bottom: 10px;
    justify-content: flex-start;
  }
  .public-complaint-page__file-picker-text {
    text-align: left;
  }
}
</style>
