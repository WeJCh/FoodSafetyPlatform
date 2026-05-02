<template>
  <div class="public-complaint-success-page">
    <header class="public-complaint-success-page__topbar">
      <div class="public-complaint-success-page__topbar-inner">
        <div class="public-complaint-success-page__brand-nav">
          <span class="public-complaint-success-page__brand">食品安全监管平台</span>
          <nav class="public-complaint-success-page__nav" aria-label="公众导航">
            <button
              v-for="item in topNavItems"
              :key="item.key"
              type="button"
              class="public-complaint-success-page__nav-item"
              :class="{ 'is-active': item.key === 'complaints' }"
              @click="goTo(item.routeName)"
            >
              {{ item.label }}
            </button>
          </nav>
        </div>
        <div class="public-complaint-success-page__toolbar">
          <button type="button" class="ghost public-complaint-success-page__logout" @click="handleLogout">退出登录</button>
        </div>
      </div>
    </header>

    <main class="public-complaint-success-page__main">
      <section class="public-complaint-success-page__panel">
        <div class="public-complaint-success-page__badge">
          <span class="material-symbols-outlined" aria-hidden="true">check_circle</span>
        </div>
        <h1>投诉提交成功</h1>
        <p>您的投诉信息已进入受理流程，可在“我的投诉”中持续查看处理进度。</p>

        <div class="public-complaint-success-page__code-box">
          <span>投诉编号</span>
          <strong>{{ complaintNo }}</strong>
          <button type="button" class="ghost" @click="copyComplaintNo">复制</button>
        </div>

        <div class="public-complaint-success-page__meta">
          <span>当前状态：{{ formatStatus(status) }}</span>
          <span>预计 3 至 5 个工作日内完成受理分派</span>
        </div>

        <div class="public-complaint-success-page__actions">
          <button type="button" @click="goTrack">查看我的投诉</button>
          <button type="button" class="ghost" @click="goCreate">继续提交</button>
          <button type="button" class="ghost" @click="goTo('public-home')">返回首页</button>
        </div>
      </section>

      <section class="public-complaint-success-page__note">
        <h3>温馨提示</h3>
        <ul>
          <li>平台会通过投诉进度记录持续同步处理节点。</li>
          <li>如需补充凭证，可在后续投诉详情中追加说明。</li>
          <li>请确保提交材料真实有效，恶意投诉将依法处理。</li>
        </ul>
      </section>

      <AppStatusToast :message="statusMessage.message" :type="statusMessage.type" />
    </main>
  </div>
</template>

<script setup>
import { computed, reactive } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppStatusToast from "../../components/common/AppStatusToast.vue";
import { performLogout } from "../../session/authRuntime";
import { complaintStatusMap, formatStatusLabel } from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";

const route = useRoute();
const router = useRouter();
const statusMessage = reactive({ message: "", type: "" });

const topNavItems = [
  { key: "home", label: "首页", routeName: "public-home" },
  { key: "bulletins", label: "监管公告", routeName: "public-bulletins" },
  { key: "enterprises", label: "企业公示", routeName: "public-enterprises" },
  { key: "sampling", label: "抽检结果", routeName: "public-sampling-results" },
  { key: "complaint-create", label: "我要投诉", routeName: "public-complaint-create" },
  { key: "complaints", label: "我的投诉", routeName: "public-complaints" }
];

const complaintNo = computed(() => {
  const value = typeof route.query.complaintNo === "string" ? route.query.complaintNo : "";
  return value || "-";
});

const status = computed(() => {
  const value = typeof route.query.status === "string" ? route.query.status : "";
  return value || "SUBMITTED";
});

function setStatus(message, type = "info") {
  statusMessage.message = message;
  statusMessage.type = type;
}

function formatStatus(value) {
  return formatStatusLabel(value, complaintStatusMap);
}

function goTo(name) {
  router.push({ name }).catch(() => {});
}

function goTrack() {
  router.push({ name: "public-complaints" }).catch(() => {});
}

function goCreate() {
  router.push({ name: "public-complaint-create" }).catch(() => {});
}

async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
}

async function copyComplaintNo() {
  if (!complaintNo.value || complaintNo.value === "-") return;
  try {
    await navigator.clipboard.writeText(complaintNo.value);
    setStatus("投诉编号已复制。", "success");
  } catch (error) {
    setStatus(resolveErrorMessage(error, "复制失败，请手动复制投诉编号"), "error");
  }
}
</script>

<style scoped>
.public-complaint-success-page { min-height: 100vh; background: var(--surface); }
.public-complaint-success-page__topbar { position: sticky; top: 0; z-index: 40; border-bottom: 1px solid rgba(195,198,211,.4); background: rgba(248,250,253,.84); }
.public-complaint-success-page__topbar-inner { max-width: 1680px; margin: 0 auto; min-height: var(--public-topbar-min-h); padding: 0 16px; display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.public-complaint-success-page__brand-nav { display: flex; align-items: center; gap: 24px; }
.public-complaint-success-page__brand { font-family: var(--font-display); font-size: var(--public-brand-size); font-weight: 800; color: var(--primary); }
.public-complaint-success-page__nav { display: flex; gap: 18px; }
.public-complaint-success-page__nav-item { border: none; background: transparent; min-height: var(--public-topbar-min-h); color: var(--on-surface-variant); font-size: var(--public-nav-size); font-weight: 700; border-bottom: 2px solid transparent; cursor: pointer; }
.public-complaint-success-page__nav-item.is-active { color: var(--primary); border-bottom-color: var(--primary); }
.public-complaint-success-page__toolbar { display: flex; align-items: center; gap: 10px; }
.public-complaint-success-page__logout { min-height: var(--public-toolbar-min-h); font-size: var(--public-logout-font-size); margin: 0; }
.public-complaint-success-page__main { max-width: 1080px; margin: 0 auto; padding: 36px 16px 60px; display: grid; gap: 14px; }
.public-complaint-success-page__panel { border: 1px solid rgba(195,198,211,.32); border-radius: 12px; background: var(--surface-container-lowest); padding: 24px; display: grid; gap: 12px; justify-items: center; text-align: center; }
.public-complaint-success-page__badge { width: 58px; height: 58px; border-radius: 50%; display: grid; place-items: center; background: rgba(33,156,84,.14); color: #1f6e45; }
.public-complaint-success-page__badge .material-symbols-outlined { font-size: var(--public-stat-number); font-variation-settings: "FILL" 1; }
.public-complaint-success-page__panel h1 { margin: 0; font-family: var(--font-display); color: var(--primary); font-size: var(--public-form-title); line-height: 1; }
.public-complaint-success-page__panel p { margin: 0; color: var(--on-surface-variant); font-size: var(--public-body-secondary); }
.public-complaint-success-page__code-box { margin-top: 2px; width: min(560px, 100%); border-radius: 10px; border: 1px solid rgba(195,198,211,.45); background: var(--surface-container-low); padding: 10px 12px; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: center; }
.public-complaint-success-page__code-box span { font-size: var(--public-caption); color: var(--on-surface-variant); }
.public-complaint-success-page__code-box strong { font-size: var(--public-code-strong); color: var(--primary); letter-spacing: .04em; }
.public-complaint-success-page__meta { display: grid; gap: 2px; font-size: var(--public-caption); color: var(--on-surface-variant); }
.public-complaint-success-page__actions { display: flex; gap: 8px; flex-wrap: wrap; justify-content: center; }
.public-complaint-success-page__actions button { min-height: var(--public-btn-compact-min-h); border-radius: 8px; padding: 0 12px; border: 1px solid transparent; background: var(--primary); color: #fff; font-size: var(--public-btn); font-weight: 700; cursor: pointer; }
.public-complaint-success-page__actions .ghost { background: #fff; color: var(--on-surface-variant); border-color: rgba(195,198,211,.6); }
.public-complaint-success-page__note { border: 1px solid rgba(195,198,211,.32); border-radius: 12px; background: var(--surface-container-lowest); padding: 14px; }
.public-complaint-success-page__note h3 { margin: 0 0 8px; font-size: var(--public-caption); text-transform: uppercase; letter-spacing: .06em; color: var(--primary); }
.public-complaint-success-page__note ul { margin: 0; padding-left: 18px; display: grid; gap: 6px; color: var(--on-surface-variant); font-size: var(--public-caption); }
@media (max-width: 1100px) { .public-complaint-success-page__nav { display: none; } }
@media (max-width: 760px) { .public-complaint-success-page__toolbar { display: none; } .public-complaint-success-page__panel h1 { font-size: var(--public-form-title-mobile); } }
</style>
