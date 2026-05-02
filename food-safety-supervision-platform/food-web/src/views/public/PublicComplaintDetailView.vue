<template>
  <div class="public-complaint-detail-page">
    <header class="public-complaint-detail-page__topbar">
      <div class="public-complaint-detail-page__topbar-inner">
        <div class="public-complaint-detail-page__brand-nav">
          <span class="public-complaint-detail-page__brand">食品安全监管平台</span>
          <nav class="public-complaint-detail-page__nav" aria-label="公众导航">
            <button
              v-for="item in topNavItems"
              :key="item.key"
              type="button"
              class="public-complaint-detail-page__nav-item"
              :class="{ 'is-active': item.key === 'complaints' }"
              @click="goTo(item.routeName)"
            >
              {{ item.label }}
            </button>
          </nav>
        </div>
        <div class="public-complaint-detail-page__toolbar">
          <button type="button" class="ghost public-complaint-detail-page__logout" @click="handleLogout">退出登录</button>
        </div>
      </div>
    </header>

    <main class="public-complaint-detail-page__main">
      <AppStatusToast v-if="loading" message="详情加载中..." type="info" />
      <AppStatusToast v-else-if="!detail" :message="status.message || '未找到对应的投诉记录。'" type="error" />

      <template v-else>
        <div class="public-complaint-detail-page__breadcrumb">
          <button type="button" @click="goBack">
            <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>
            返回我的投诉
          </button>
        </div>

        <div class="public-complaint-detail-page__layout">
          <section class="public-complaint-detail-page__content">
            <article class="public-complaint-detail-page__hero">
              <div class="public-complaint-detail-page__id-block">
                <span>Complaint ID</span>
                <h1>{{ detail.complaintNo || "-" }}</h1>
              </div>
              <AppStatusTag :label="formatStatus(detail.status)" :tone="statusTone(detail.status)" />
            </article>

            <article v-if="isCompleted" class="public-complaint-detail-page__feedback">
              <span class="material-symbols-outlined" aria-hidden="true">chat_bubble</span>
              <div>
                <h3>处理结果反馈</h3>
                <p>{{ resolveFeedbackSummary(detail) || "暂无反馈摘要" }}</p>
                <small>反馈时间：{{ formatTime(detail.updateTime) }}</small>
              </div>
            </article>

            <article class="public-complaint-detail-page__card">
              <h3>投诉详情信息</h3>
              <div class="public-complaint-detail-page__grid">
                <div><span>企业名称</span><strong>{{ detail.enterpriseName || "-" }}</strong></div>
                <div><span>投诉类别</span><strong>{{ detail.complaintType || "-" }}</strong></div>
                <div><span>提交时间</span><strong>{{ formatTime(detail.createTime) }}</strong></div>
                <div><span>更新时间</span><strong>{{ formatTime(detail.updateTime) }}</strong></div>
                <div><span>办理时限</span><strong>{{ formatTime(detail.deadlineTime) }}</strong></div>
                <div><span>联系方式</span><strong>{{ detail.contactMasked || detail.contact || "-" }}</strong></div>
              </div>
              <div class="public-complaint-detail-page__desc">
                <span>投诉描述</span>
                <p>{{ detail.content || "-" }}</p>
              </div>
              <div class="public-complaint-detail-page__attachments">
                <span>凭证附件（{{ attachments.length }}）</span>
                <AppEmptyState
                  v-if="!attachments.length"
                  title="暂无图片凭证"
                  description="投诉人未上传图片附件。"
                  class="public-complaint-detail-page__attachment-empty"
                />
                <div v-else class="public-complaint-detail-page__attachment-list">
                  <a
                    v-for="(url, index) in attachments"
                    :key="`${url}-${index}`"
                    :href="url"
                    target="_blank"
                    rel="noreferrer"
                  >
                    <img :src="url" alt="投诉凭证" />
                  </a>
                </div>
              </div>
            </article>
          </section>

          <aside class="public-complaint-detail-page__timeline-card">
            <h3>
              <span class="material-symbols-outlined" aria-hidden="true">history</span>
              处理进度追踪
            </h3>
            <div class="public-complaint-detail-page__timeline">
              <article v-for="item in timelineItems" :key="item.key">
                <i :class="{ 'is-done': item.done }"><span class="material-symbols-outlined" aria-hidden="true">{{ item.icon }}</span></i>
                <div>
                  <strong>{{ item.title }}</strong>
                  <small>{{ item.time }}</small>
                  <p>{{ item.desc }}</p>
                </div>
              </article>
            </div>
            <p class="public-complaint-detail-page__support">
              如对处理结果有异议，可拨打 <strong>12315</strong> 或联系属地监管部门。
            </p>
          </aside>
        </div>

        <section class="public-complaint-detail-page__bottom">
          <article>
            <h3>办理说明</h3>
            <p>投诉办理通常需要 3 至 5 个工作日。复杂案件会根据核查难度适当延长，并同步更新进度节点。</p>
          </article>
          <article>
            <h3>结果异议与复核</h3>
            <p>若对处理结果存在异议，可在“我的投诉”中补充说明材料，或拨打 <strong>12315</strong> 联系属地监管部门发起复核咨询。</p>
          </article>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchMyComplaintDetail } from "../../api/complaint";
import AppEmptyState from "../../components/common/AppEmptyState.vue";
import AppStatusTag from "../../components/common/AppStatusTag.vue";
import AppStatusToast from "../../components/common/AppStatusToast.vue";
import { getActiveSession, performLogout } from "../../session/authRuntime";
import { formatTime } from "../../utils/formatters";
import { complaintStatusMap, formatStatusLabel, getStatusTone } from "../../utils/statusMaps";
import { resolveErrorMessage } from "../../utils/uiFeedback";

const route = useRoute();
const router = useRouter();
const publicToken = getActiveSession()?.token || "";
const loading = ref(false);
const detail = ref(null);
const status = reactive({ message: "", type: "" });

const topNavItems = [
  { key: "home", label: "首页", routeName: "public-home" },
  { key: "bulletins", label: "监管公告", routeName: "public-bulletins" },
  { key: "enterprises", label: "企业公示", routeName: "public-enterprises" },
  { key: "sampling", label: "抽检结果", routeName: "public-sampling-results" },
  { key: "complaint-create", label: "我要投诉", routeName: "public-complaint-create" },
  { key: "complaints", label: "我的投诉", routeName: "public-complaints" }
];

const attachments = computed(() => {
  const raw = detail.value?.imageUrls;
  if (!Array.isArray(raw)) return [];
  return raw.filter(Boolean).map((item) => String(item));
});

const isCompleted = computed(() => {
  const current = String(detail.value?.status || "").toUpperCase();
  return current === "FEEDBACKED";
});

const timelineItems = computed(() => {
  const current = String(detail.value?.status || "").toUpperCase();
  const createTime = formatTime(detail.value?.createTime);
  const updateTime = formatTime(detail.value?.updateTime);
  const steps = [
    { key: "SUBMITTED", title: "已提交", desc: "用户已提交投诉，当前等待平台受理。", icon: "send" },
    { key: "PENDING", title: "已受理", desc: "平台已完成受理，等待分派给对应监管人员。", icon: "schedule" },
    { key: "ASSIGNED", title: "已分派", desc: "投诉已分派至对应监管人员。", icon: "forward" },
    { key: "PROCESSING", title: "处理中", desc: "监管人员正在核查办理。", icon: "sync" },
    { key: "FEEDBACKED", title: "已反馈", desc: "处理结果已反馈给投诉人。", icon: "check" }
  ];
  const currentIndex = steps.findIndex((item) => item.key === current);
  return steps.map((item, index) => ({
    ...item,
    done: current === "REJECTED" ? index <= 1 : currentIndex >= 0 ? index <= currentIndex : index === 0,
    time: index === 0 ? createTime : updateTime
  }));
});

function formatStatus(value) {
  return formatStatusLabel(value, complaintStatusMap);
}

function statusTone(value) {
  return getStatusTone(value, "COMPLAINT");
}

function resolveFeedbackSummary(item) {
  return item?.feedbackSummary || item?.handleResult || "";
}

function goTo(name) {
  router.push({ name }).catch(() => {});
}

function goBack() {
  router.push({ name: "public-complaints", query: { ...route.query } }).catch(() => {});
}

async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
}

async function loadDetail() {
  const complaintId = route.params.complaintId;
  if (!complaintId) {
    detail.value = null;
    status.message = "缺少投诉编号。";
    status.type = "error";
    return;
  }
  loading.value = true;
  status.message = "";
  try {
    detail.value = await fetchMyComplaintDetail(publicToken, complaintId);
  } catch (error) {
    detail.value = null;
    status.message = resolveErrorMessage(error, "投诉详情加载失败，请稍后重试");
    status.type = "error";
  } finally {
    loading.value = false;
  }
}

onMounted(loadDetail);
</script>

<style scoped>
.public-complaint-detail-page { min-height: 100vh; background: var(--surface); }
.public-complaint-detail-page__topbar { position: sticky; top: 0; z-index: 40; border-bottom: 1px solid rgba(195,198,211,.4); background: rgba(248,250,253,.84); }
.public-complaint-detail-page__topbar-inner { max-width: 1680px; margin: 0 auto; min-height: var(--public-topbar-min-h); padding: 0 16px; display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.public-complaint-detail-page__brand-nav { display: flex; align-items: center; gap: 24px; }
.public-complaint-detail-page__brand { font-family: var(--font-display); font-size: var(--public-brand-size); font-weight: 800; color: var(--primary); }
.public-complaint-detail-page__nav { display: flex; gap: 18px; }
.public-complaint-detail-page__nav-item { border: none; background: transparent; min-height: var(--public-topbar-min-h); color: var(--on-surface-variant); font-size: var(--public-nav-size); font-weight: 700; border-bottom: 2px solid transparent; cursor: pointer; }
.public-complaint-detail-page__nav-item.is-active { color: var(--primary); border-bottom-color: var(--primary); }
.public-complaint-detail-page__toolbar { display: flex; align-items: center; gap: 10px; }
.public-complaint-detail-page__logout { min-height: var(--public-toolbar-min-h); font-size: var(--public-logout-font-size); margin: 0; }
.public-complaint-detail-page__main { max-width: 1680px; margin: 0 auto; padding: 24px 16px 24px; }
.public-complaint-detail-page__breadcrumb { margin-bottom: 10px; }
.public-complaint-detail-page__breadcrumb button { border: 1px solid rgba(195,198,211,.5); background: #fff; border-radius: 8px; min-height: var(--public-btn-compact-min-h); font-size: var(--public-crumb-size); padding: 0 10px; display: inline-flex; align-items: center; gap: 4px; color: var(--on-surface-variant); cursor: pointer; }
.public-complaint-detail-page__layout { display: grid; grid-template-columns: minmax(0, 1fr) minmax(300px, .42fr); gap: 12px; }
.public-complaint-detail-page__content { display: grid; gap: 12px; }
.public-complaint-detail-page__hero { border: 1px solid rgba(195,198,211,.32); border-radius: 10px; background: var(--surface-container-lowest); border-bottom: 4px solid var(--primary); padding: 14px; display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.public-complaint-detail-page__id-block span { font-size: var(--public-overline); color: var(--on-surface-variant); letter-spacing: .06em; text-transform: uppercase; }
.public-complaint-detail-page__id-block h1 { margin: 4px 0 0; font-family: var(--font-display); color: var(--primary); font-size: var(--public-detail-title); line-height: 1; }
.public-complaint-detail-page__feedback { border-radius: 10px; border: 1px solid rgba(70,89,231,.22); background: rgba(70,89,231,.09); padding: 12px; display: flex; gap: 10px; }
.public-complaint-detail-page__feedback .material-symbols-outlined { font-size: var(--public-icon-lg); color: var(--primary); margin-top: 2px; }
.public-complaint-detail-page__feedback h3 { margin: 0 0 6px; font-size: var(--public-meta-strong); color: var(--primary); }
.public-complaint-detail-page__feedback p { margin: 0; font-size: var(--public-caption); line-height: 1.6; color: var(--on-surface); }
.public-complaint-detail-page__feedback small { display: block; margin-top: 6px; font-size: var(--public-overline); color: var(--on-surface-variant); }
.public-complaint-detail-page__card,
.public-complaint-detail-page__timeline-card { border: 1px solid rgba(195,198,211,.32); border-radius: 10px; background: var(--surface-container-lowest); padding: 14px; }
.public-complaint-detail-page__card h3 { margin: 0 0 10px; font-size: var(--public-caption); text-transform: uppercase; letter-spacing: .06em; color: var(--primary); }
.public-complaint-detail-page__grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.public-complaint-detail-page__grid div { display: grid; gap: 3px; }
.public-complaint-detail-page__grid span { font-size: var(--public-overline); color: var(--on-surface-variant); }
.public-complaint-detail-page__grid strong { font-size: var(--public-caption); }
.public-complaint-detail-page__desc { margin-top: 12px; }
.public-complaint-detail-page__desc span { display: block; font-size: var(--public-overline); color: var(--on-surface-variant); margin-bottom: 4px; }
.public-complaint-detail-page__desc p { margin: 0; font-size: var(--public-caption); line-height: 1.65; color: var(--on-surface); }
.public-complaint-detail-page__attachments { margin-top: 12px; }
.public-complaint-detail-page__attachments > span { display: block; font-size: var(--public-overline); color: var(--on-surface-variant); margin-bottom: 6px; }
.public-complaint-detail-page__attachment-empty { min-height: 96px; }
.public-complaint-detail-page__attachment-list { display: flex; gap: 8px; flex-wrap: wrap; }
.public-complaint-detail-page__attachment-list a { width: 108px; height: 108px; border-radius: 8px; overflow: hidden; border: 1px solid rgba(195,198,211,.45); background: var(--surface-container-low); }
.public-complaint-detail-page__attachment-list img { width: 100%; height: 100%; object-fit: cover; }
.public-complaint-detail-page__timeline-card h3 { margin: 0 0 10px; font-size: var(--public-caption); text-transform: uppercase; letter-spacing: .06em; color: var(--primary); display: inline-flex; align-items: center; gap: 4px; }
.public-complaint-detail-page__timeline { display: grid; gap: 10px; position: relative; }
.public-complaint-detail-page__timeline::before { content: ""; position: absolute; left: 10px; top: 8px; bottom: 8px; width: 1px; background: rgba(195,198,211,.8); }
.public-complaint-detail-page__timeline article { display: flex; gap: 8px; position: relative; z-index: 1; }
.public-complaint-detail-page__timeline article i { width: 22px; height: 22px; border-radius: 50%; background: rgba(70,89,231,.12); color: var(--primary); display: inline-flex; align-items: center; justify-content: center; font-style: normal; }
.public-complaint-detail-page__timeline article i.is-done { background: var(--primary); color: #fff; }
.public-complaint-detail-page__timeline article i .material-symbols-outlined { font-size: var(--public-text-md); }
.public-complaint-detail-page__timeline article strong { display: block; font-size: var(--public-caption); color: var(--on-surface); }
.public-complaint-detail-page__timeline article small { display: block; font-size: var(--public-table-head-overline); color: var(--on-surface-variant); margin: 1px 0 2px; }
.public-complaint-detail-page__timeline article p { margin: 0; font-size: var(--public-timeline-body); color: var(--on-surface-variant); line-height: 1.45; }
.public-complaint-detail-page__support { margin: 12px 0 0; font-size: var(--public-overline); line-height: 1.55; color: var(--on-surface-variant); border-top: 1px dashed rgba(195,198,211,.5); padding-top: 10px; }
.public-complaint-detail-page__bottom { margin-top: 12px; display: grid; gap: 12px; grid-template-columns: 1fr 1fr; }
.public-complaint-detail-page__bottom article { border: 1px solid rgba(195,198,211,.32); border-radius: 10px; background: var(--surface-container-lowest); padding: 12px; }
.public-complaint-detail-page__bottom h3 { margin: 0 0 6px; font-size: var(--public-caption); text-transform: uppercase; letter-spacing: .06em; color: var(--primary); }
.public-complaint-detail-page__bottom p { margin: 0; font-size: var(--public-caption); line-height: 1.65; color: var(--on-surface-variant); }
@media (max-width: 1100px) { .public-complaint-detail-page__nav { display: none; } .public-complaint-detail-page__layout { grid-template-columns: 1fr; } }
@media (max-width: 760px) { .public-complaint-detail-page__toolbar { display: none; } .public-complaint-detail-page__id-block h1 { font-size: var(--public-detail-title-sm); } .public-complaint-detail-page__grid { grid-template-columns: 1fr; } .public-complaint-detail-page__bottom { grid-template-columns: 1fr; } }
</style>
