<template>
  <EnterpriseWorkspacePage
    active-key="profile"
    title="企业备案"
    subtitle="将企业注册后的资料完善、证照附件和审核状态正式接入备案流程。"
    top-search-placeholder="搜索企业或档案..."
    :username="enterpriseUser.username"
    :user-type="enterpriseUser.userType"
    :status-label="statusLabel"
    :status-tone="statusTone"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <div class="enterprise-workspace-card">
      <nav class="enterprise-page-hero__crumb" style="margin-bottom: 18px" aria-label="面包屑">
        <span>企业工作台</span>
        <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
        <span class="is-current">企业备案</span>
      </nav>

      <section class="enterprise-profile-registrar">
        <div class="enterprise-profile-registrar__gradient">
          <div style="display: flex; align-items: center; gap: 20px; flex: 1; min-width: 0">
            <div class="enterprise-profile-registrar__icon">
              <span class="material-symbols-outlined is-filled" aria-hidden="true">verified</span>
            </div>
            <div>
              <h1 class="enterprise-profile-registrar__title">企业备案详情</h1>
              <p class="enterprise-profile-registrar__meta">
                统一社会信用代码：{{ form.creditCode || "待填写" }}
              </p>
            </div>
          </div>
          <div class="enterprise-profile-registrar__status">
            <EnterpriseStatusChip :label="statusLabel" :tone="statusTone" />
            <p v-if="profile.approvedTime" class="enterprise-profile-registrar__audit-meta">
              上次审核日期 {{ formatTime(profile.approvedTime) }}
            </p>
            <RouterLink class="enterprise-profile-registrar__cta" :to="{ name: 'enterprise-profile-detail' }">只读详情页</RouterLink>
          </div>
        </div>
      </section>

      <div class="enterprise-detail-layout">
        <form id="enterprise-profile-form-anchor" @submit.prevent="handleSubmit">
        <div class="enterprise-panel enterprise-panel--accent-top">
          <div class="enterprise-panel__head" style="justify-content: space-between">
            <div style="display: flex; align-items: center; gap: 10px">
              <div class="enterprise-panel__head-bar" />
              <h3>基础信息表单</h3>
            </div>
            <a class="ghost enterprise-inline-link" style="font-size: 12px" href="#enterprise-profile-form-anchor" @click.prevent="scrollToForm">在表单内修改</a>
          </div>
          <p v-if="profile.approvalComment" class="secondary-text" style="margin: 0 0 16px; font-size: 13px">审核意见：{{ profile.approvalComment }}</p>
          <p v-if="!profileLoaded" class="secondary-text" style="margin: 0 0 16px">暂无备案记录，请先提交企业资料。</p>
          <div class="enterprise-form-section" style="box-shadow: none; border: none; padding: 0; margin: 0">
          <div class="enterprise-form-section__title" style="display: none">基础备案信息</div>
          <div class="enterprise-detail-grid">
            <label>
              企业名称
              <input v-model.trim="form.enterpriseName" required placeholder="请输入企业名称" />
            </label>
            <label>
              食品经营许可证编号
              <input v-model.trim="form.licenseNo" placeholder="请输入食品经营许可证编号" />
            </label>
            <label>
              统一社会信用代码
              <input v-model.trim="form.creditCode" maxlength="18" placeholder="请输入 18 位统一社会信用代码（选填）" />
            </label>
            <label>
              法定代表人
              <input v-model.trim="stageB.legalRepresentative" placeholder="请输入法定代表人姓名" />
            </label>
            <label>
              负责人姓名
              <input v-model.trim="form.principal" placeholder="请输入负责人姓名" />
            </label>
            <label>
              负责人电话
              <input v-model.trim="form.principalPhone" placeholder="11 位手机号" />
            </label>
          </div>
          </div>
        </div>

        <div class="enterprise-panel">
          <div class="enterprise-panel__head">
            <div class="enterprise-panel__head-bar" />
            <h3>行政区与地址</h3>
          </div>
          <div class="enterprise-form-section" style="box-shadow: none; border: none; padding: 0; margin: 0">
          <div class="enterprise-form-section__title" style="display: none">行政区与地址</div>
          <div class="enterprise-detail-grid">
            <label>
              省份
              <select v-model="regionSelection.provinceId" @change="handleProvinceChange">
                <option value="">请选择省</option>
                <option v-for="item in regionOptions.provinces" :key="item.id" :value="item.id">{{ item.name }}</option>
              </select>
            </label>
            <label>
              城市
              <select v-model="regionSelection.cityId" :disabled="!regionSelection.provinceId" @change="handleCityChange">
                <option value="">请选择市</option>
                <option v-for="item in regionOptions.cities" :key="item.id" :value="item.id">{{ item.name }}</option>
              </select>
            </label>
            <label>
              区县
              <select v-model="regionSelection.countyId" :disabled="!regionSelection.cityId" @change="handleCountyChange">
                <option value="">请选择区县</option>
                <option v-for="item in regionOptions.counties" :key="item.id" :value="item.id">{{ item.name }}</option>
              </select>
            </label>
            <label>
              街道
              <select v-model="regionSelection.streetId" :disabled="!regionSelection.countyId">
                <option value="">请选择街道</option>
                <option v-for="item in regionOptions.streets" :key="item.id" :value="item.id">{{ item.name }}</option>
              </select>
            </label>
            <div v-if="existingRegionText && !regionSelection.provinceId" class="hint enterprise-detail-item--full">当前行政区：{{ existingRegionText }}</div>
            <label class="enterprise-detail-item--full">
              详细地址
              <input v-model.trim="form.addressDetail" required placeholder="请输入详细地址" />
            </label>
          </div>
          </div>
        </div>

        <div class="enterprise-panel">
          <div class="enterprise-panel__head">
            <div class="enterprise-panel__head-bar" />
            <h3>附件上传区</h3>
          </div>
          <div class="enterprise-form-section" style="box-shadow: none; border: none; padding: 0; margin: 0">
          <div class="enterprise-form-section__title" style="display: none">附件上传区</div>
          <p class="enterprise-attachment-intro">请按顺序上传以下材料原件扫描件或照片，清晰可辨认。上传成功后将随「更新并重新提交」一并送达审核。</p>
          <div class="enterprise-attachment-uploader-grid">
            <div
              v-for="field in attachmentFields"
              :key="field.type"
              class="enterprise-attachment-slot"
              :class="{ 'enterprise-attachment-slot--filled': !!findAttachment(field.type) }"
            >
              <div class="enterprise-attachment-slot__head">
                <span class="material-symbols-outlined enterprise-attachment-slot__icon" aria-hidden="true">{{ attachmentTypeIcon[field.type] || "attach_file" }}</span>
                <span class="enterprise-attachment-slot__title">{{ field.label }}</span>
              </div>

              <div v-if="findAttachment(field.type)" class="enterprise-attachment-file-card">
                <span class="material-symbols-outlined enterprise-attachment-file-card__icon" aria-hidden="true">draft</span>
                <div class="enterprise-attachment-file-card__body">
                  <span class="enterprise-attachment-file-card__name">{{ findAttachment(field.type)?.name }}</span>
                  <span class="enterprise-attachment-file-card__hint">已上传 · 提交备案时一并发送</span>
                </div>
                <div class="enterprise-attachment-file-card__actions">
                  <a class="enterprise-attachment-file-card__link" :href="findAttachment(field.type)?.url" target="_blank" rel="noreferrer">预览</a>
                  <button type="button" class="enterprise-attachment-file-card__link" @click="removeAttachment(field.type)">移除</button>
                  <label class="enterprise-attachment-file-card__replace">
                    更换
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      :disabled="loading || uploading"
                      @change="handleAttachmentSelect(field.type, field.label, $event)"
                    />
                  </label>
                </div>
              </div>

              <label
                v-else
                class="enterprise-attachment-dropzone"
                :class="{ 'is-dragover': dragOverType === field.type, 'is-disabled': loading || uploading }"
                @dragenter.prevent="dragOverType = field.type"
                @dragleave.prevent="handleAttachmentDragLeave($event, field.type)"
                @dragover.prevent="dragOverType = field.type"
                @drop.prevent="handleAttachmentDrop(field.type, field.label, $event)"
              >
                <input
                  type="file"
                  class="enterprise-attachment-dropzone__input"
                  accept="image/jpeg,image/png,image/webp"
                  :disabled="loading || uploading"
                  @change="handleAttachmentSelect(field.type, field.label, $event)"
                />
                <span class="material-symbols-outlined enterprise-attachment-dropzone__cloud" aria-hidden="true">cloud_upload</span>
                <span class="enterprise-attachment-dropzone__title">点击选择或拖放文件到此处</span>
                <span class="enterprise-attachment-dropzone__hint">JPG、PNG、WebP · 单文件 ≤ 5MB</span>
              </label>
            </div>
          </div>
          </div>
        </div>

        <div class="product-form__actions">
          <button class="primary" type="submit" :disabled="loading || uploading">{{ loading ? "提交中..." : submitLabel }}</button>
        </div>
      </form>

        <aside class="enterprise-side-stack">
          <div class="enterprise-side-card enterprise-audit-summary-card">
            <div class="enterprise-side-card__head enterprise-audit-summary-card__head">
              <span class="material-symbols-outlined enterprise-audit-summary-card__head-icon" aria-hidden="true">timeline</span>
              审核轨迹摘要
            </div>
            <div class="enterprise-side-card__body enterprise-audit-summary-card__body">
              <ul v-if="timelineSummaryRows.length" class="enterprise-audit-summary" role="list">
                <li
                  v-for="(row, index) in timelineSummaryRows"
                  :key="row.key"
                  class="enterprise-audit-summary__item"
                  :class="[`is-${row.variant}`, { 'is-last': index === timelineSummaryRows.length - 1 }]"
                >
                  <div class="enterprise-audit-summary__rail" aria-hidden="true">
                    <span class="enterprise-audit-summary__marker">
                      <span v-if="row.variant === 'complete'" class="material-symbols-outlined enterprise-audit-summary__marker-icon is-filled" aria-hidden="true">check</span>
                      <span v-else-if="row.variant === 'active'" class="enterprise-audit-summary__marker-pulse" />
                      <span v-else class="enterprise-audit-summary__marker-hollow" />
                    </span>
                  </div>
                  <div class="enterprise-audit-summary__content">
                    <span class="enterprise-audit-summary__title">{{ row.label }}</span>
                    <span class="enterprise-audit-summary__meta">{{ row.metaLine }}</span>
                    <p v-if="row.noteLine" class="enterprise-audit-summary__note">{{ row.noteLine }}</p>
                  </div>
                </li>
              </ul>
              <p v-else class="enterprise-audit-summary__empty">暂无审核轨迹，提交备案后将在此展示。</p>
              <RouterLink class="primary enterprise-link-button enterprise-audit-summary__cta" :to="{ name: 'enterprise-profile-detail' }">
                <span class="material-symbols-outlined" aria-hidden="true" style="font-size: 18px">open_in_new</span>
                查看完整轨迹
              </RouterLink>
            </div>
          </div>
          <div class="enterprise-side-card">
            <div class="enterprise-side-card__head">提示</div>
            <div class="enterprise-side-card__body">
              <p class="secondary-text" style="margin: 0; font-size: 13px; line-height: 1.55">{{ statusDescription }}</p>
            </div>
          </div>
        </aside>
      </div>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </div>
  </EnterpriseWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import { presignUpload } from "../../api/file";
import { fetchEnterpriseProfile, fetchRegions, submitEnterpriseProfile } from "../../api/regulation";
import EnterpriseStatusChip from "../../components/enterprise/EnterpriseStatusChip.vue";
import EnterpriseWorkspacePage from "../../components/enterprise/EnterpriseWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { getApprovalStatusLabel, getApprovalStatusTone, useEnterpriseShellSession } from "./enterpriseShared";
import {
  buildApprovalTimeline,
  createEmptyStageBData,
  ENTERPRISE_ATTACHMENT_FIELDS,
  loadStageBData,
  mergeProfileWithStageB,
  saveStageBData,
  upsertStageBHistory
} from "./enterpriseProfileStageB";

const { enterpriseUser, token, handleSidebarNavigate, handleLogout } = useEnterpriseShellSession();
const loading = ref(false);
const profileLoaded = ref(false);
const uploading = ref(false);
const dragOverType = ref(null);

const attachmentTypeIcon = {
  businessLicense: "badge",
  foodPermit: "restaurant",
  onsitePhoto: "photo_camera"
};
const status = reactive({ message: "", type: "" });
const existingRegionId = ref(null);
const existingRegionText = ref("");
const existingRegionPath = ref([]);
const profile = reactive({ approvalStatus: "", approvalComment: "", approvedTime: "" });
const regionOptions = reactive({ provinces: [], cities: [], counties: [], streets: [] });
const regionSelection = reactive({ provinceId: "", cityId: "", countyId: "", streetId: "" });
const form = reactive({ enterpriseName: "", licenseNo: "", creditCode: "", addressDetail: "", principal: "", principalPhone: "" });
const stageB = reactive(createEmptyStageBData());

const attachmentFields = ENTERPRISE_ATTACHMENT_FIELDS;
const statusLabel = computed(() => getApprovalStatusLabel(profileLoaded.value, profile.approvalStatus));
const statusTone = computed(() => getApprovalStatusTone(profileLoaded.value, profile.approvalStatus));
const statusDescription = computed(() => {
  if (!profileLoaded.value) return "当前还没有提交企业备案记录。";
  if (profile.approvalStatus === "APPROVED") return "企业主体资料已通过审核，可以继续使用企业端能力。";
  if (profile.approvalStatus === "REJECTED") return "请根据审核意见修改后重新提交。";
  return "企业资料已提交，等待监管审核。";
});
const submitLabel = computed(() => (profileLoaded.value ? "更新并重新提交" : "提交备案"));
const approvalTimeline = computed(() =>
  buildApprovalTimeline({
    profileLoaded: profileLoaded.value,
    approvalStatus: profile.approvalStatus,
    approvalComment: profile.approvalComment,
    approvedTime: profile.approvedTime,
    history: stageB.history
  })
);

const timelineSummaryRows = computed(() => {
  const list = approvalTimeline.value.slice(0, 4);
  let activeAssigned = false;
  return list.map((item) => {
    const hasTime = Boolean(item.time && String(item.time).trim());
    let variant = "complete";
    if (hasTime) {
      variant = "complete";
    } else if (!activeAssigned) {
      variant = "active";
      activeAssigned = true;
    } else {
      variant = "upcoming";
    }
    let metaLine = hasTime ? formatTime(item.time) : "";
    if (!hasTime) {
      metaLine = variant === "active" ? "当前阶段 · 处理中" : "待开始";
    }
    const noteLine = variant === "active" && item.note ? item.note : "";
    return {
      key: `${item.type}-${item.label}`,
      label: item.label,
      variant,
      metaLine,
      noteLine
    };
  });
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function scrollToForm() {
  document.getElementById("enterprise-profile-form-anchor")?.scrollIntoView({ behavior: "smooth", block: "start" });
}

function getUserStorageKey() {
  return enterpriseUser.value?.userId || enterpriseUser.value?.username || "anonymous";
}

function resetForm(payload = {}) {
  form.enterpriseName = payload.enterpriseName || "";
  form.licenseNo = payload.licenseNo || "";
  form.creditCode = payload.creditCode || "";
  form.addressDetail = payload.addressDetail || "";
  form.principal = payload.principal || "";
  form.principalPhone = payload.principalPhone || "";
}

function applyStageB(payload = {}) {
  const merged = mergeProfileWithStageB({}, payload);
  stageB.legalRepresentative = merged.legalRepresentative;
  stageB.attachments = merged.attachments;
  stageB.history = merged.history;
}

function persistStageB() {
  saveStageBData(getUserStorageKey(), { ...stageB });
}

async function loadProfile() {
  try {
    const data = await fetchEnterpriseProfile(token.value);
    const local = loadStageBData(getUserStorageKey());
    const merged = mergeProfileWithStageB(data, local);
    profile.approvalStatus = data.approvalStatus || "";
    profile.approvalComment = data.approvalComment || "";
    profile.approvedTime = data.approvedTime || "";
    existingRegionId.value = data.regionId || null;
    existingRegionText.value = data.regionPathText || "";
    existingRegionPath.value = Array.isArray(data.regionPath) ? data.regionPath : [];
    resetForm(data);
    applyStageB(merged);
    stageB.attachments = Array.isArray(data.attachments) ? data.attachments : [];
    saveStageBData(getUserStorageKey(), { ...merged, attachments: stageB.attachments, history: stageB.history });
    profileLoaded.value = true;
    if (existingRegionPath.value.length) {
      await applyRegionPath(existingRegionPath.value);
    }
  } catch (error) {
    if (String(error?.message).includes("not found")) {
      profileLoaded.value = false;
      existingRegionId.value = null;
      existingRegionText.value = "";
      existingRegionPath.value = [];
      resetForm();
      const local = loadStageBData(getUserStorageKey());
      applyStageB(local);
      form.creditCode = local.creditCode || "";
      return;
    }
    setStatus(error.message || "加载备案信息失败", "error");
  }
}

async function handleSubmit() {
  loading.value = true;
  setStatus("");
  try {
    const regionId = resolveEnterpriseRegionId();
    if (!regionId) {
      setStatus("请选择所属行政区", "error");
      return;
    }
    const payload = {
      ...form,
      regionId,
      legalRepresentative: stageB.legalRepresentative,
      attachments: stageB.attachments
    };
    const data = await submitEnterpriseProfile(token.value, payload);
    profile.approvalStatus = data.approvalStatus || "PENDING";
    profile.approvalComment = data.approvalComment || "";
    profile.approvedTime = data.approvedTime || "";
    existingRegionId.value = data.regionId || existingRegionId.value;
    existingRegionText.value = data.regionPathText || existingRegionText.value;
    existingRegionPath.value = Array.isArray(data.regionPath) ? data.regionPath : existingRegionPath.value;
    stageB.attachments = Array.isArray(data.attachments) ? data.attachments : stageB.attachments;
    profileLoaded.value = true;
    stageB.history = upsertStageBHistory(stageB.history, {
      type: "PROFILE_SUBMITTED",
      label: "资料已提交",
      time: new Date().toISOString(),
      note: "企业资料完善已提交，等待监管审核。"
    });
    persistStageB();
    setStatus("提交成功，已进入审核流程。", "success");
  } catch (error) {
    setStatus(error.message || "提交备案失败", "error");
  } finally {
    loading.value = false;
  }
}

async function loadRegions(parentId, targetKey) {
  try {
    regionOptions[targetKey] = await fetchRegions(token.value, parentId);
  } catch (error) {
    setStatus(error.message || "加载行政区失败", "error");
  }
}

function resetRegion(level) {
  if (level === "province") {
    regionSelection.cityId = "";
    regionSelection.countyId = "";
    regionSelection.streetId = "";
    regionOptions.cities = [];
    regionOptions.counties = [];
    regionOptions.streets = [];
  } else if (level === "city") {
    regionSelection.countyId = "";
    regionSelection.streetId = "";
    regionOptions.counties = [];
    regionOptions.streets = [];
  } else if (level === "county") {
    regionSelection.streetId = "";
    regionOptions.streets = [];
  }
}

async function applyRegionPath(path) {
  if (!path?.length) return;
  const province = path[0];
  regionSelection.provinceId = province?.id ? String(province.id) : "";
  resetRegion("province");
  if (province?.id) await loadRegions(province.id, "cities");
  const city = path[1];
  if (city?.id) {
    regionSelection.cityId = String(city.id);
    await loadRegions(city.id, "counties");
  }
  const county = path[2];
  if (county?.id) {
    regionSelection.countyId = String(county.id);
    await loadRegions(county.id, "streets");
  }
  const street = path[3];
  if (street?.id) regionSelection.streetId = String(street.id);
}

async function handleProvinceChange() {
  resetRegion("province");
  const provinceId = Number(regionSelection.provinceId || 0);
  if (provinceId) await loadRegions(provinceId, "cities");
}

async function handleCityChange() {
  resetRegion("city");
  const cityId = Number(regionSelection.cityId || 0);
  if (cityId) await loadRegions(cityId, "counties");
}

async function handleCountyChange() {
  resetRegion("county");
  const countyId = Number(regionSelection.countyId || 0);
  if (countyId) await loadRegions(countyId, "streets");
}

function resolveEnterpriseRegionId() {
  if (regionOptions.streets.length) return Number(regionSelection.streetId || 0) || null;
  if (regionOptions.counties.length) return Number(regionSelection.countyId || 0) || null;
  if (regionOptions.cities.length) return Number(regionSelection.cityId || 0) || null;
  return Number(regionSelection.provinceId || 0) || existingRegionId.value || null;
}

function findAttachment(type) {
  return stageB.attachments.find((item) => item.type === type) || null;
}

function isAllowedProfileAttachment(file) {
  if (!file?.name) return false;
  const t = (file.type || "").toLowerCase();
  if (["image/jpeg", "image/png", "image/webp"].includes(t)) return true;
  return /\.(jpe?g|png|webp)$/i.test(file.name);
}

function handleAttachmentDragLeave(event, type) {
  const related = event.relatedTarget;
  if (related && event.currentTarget.contains(related)) return;
  if (dragOverType.value === type) dragOverType.value = null;
}

async function uploadAttachment(file) {
  const payload = {
    filename: file.name,
    contentType: file.type || "application/octet-stream",
    size: file.size,
    bizType: "ENTERPRISE_PROFILE"
  };
  const presign = await presignUpload(token.value, payload);
  const response = await fetch(presign.uploadUrl, {
    method: "PUT",
    headers: { "Content-Type": payload.contentType },
    body: file
  });
  if (!response.ok) throw new Error(`上传失败 (${response.status})`);
  return presign.fileUrl;
}

async function processProfileAttachment(type, label, file) {
  if (!file) return;
  if (!isAllowedProfileAttachment(file)) {
    setStatus("仅支持 JPG、PNG、WebP 图片格式", "error");
    return;
  }
  const maxBytes = 5 * 1024 * 1024;
  if (file.size > maxBytes) {
    setStatus("单文件请控制在 5MB 以内", "error");
    return;
  }
  uploading.value = true;
  setStatus("");
  try {
    const url = await uploadAttachment(file);
    stageB.attachments = [
      ...stageB.attachments.filter((item) => item.type !== type),
      { type, label, name: file.name, url, uploadedAt: new Date().toISOString() }
    ];
    persistStageB();
    setStatus(`${label}上传成功`, "success");
  } catch (error) {
    setStatus(error.message || `${label}上传失败`, "error");
  } finally {
    uploading.value = false;
  }
}

async function handleAttachmentSelect(type, label, event) {
  const file = event?.target?.files?.[0];
  if (event?.target) event.target.value = "";
  await processProfileAttachment(type, label, file);
}

async function handleAttachmentDrop(type, label, event) {
  dragOverType.value = null;
  const file = event.dataTransfer?.files?.[0];
  await processProfileAttachment(type, label, file);
}

function removeAttachment(type) {
  stageB.attachments = stageB.attachments.filter((item) => item.type !== type);
  persistStageB();
}

onMounted(async () => {
  await loadRegions(null, "provinces");
  await loadProfile();
});
</script>
