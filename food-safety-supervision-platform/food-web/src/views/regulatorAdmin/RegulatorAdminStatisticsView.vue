<template>
  <RegulatorAdminWorkspacePage
    active-key="stats"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="stats-page">
      <header class="stats-hero">
        <div>
          <p class="stats-hero__eyebrow">Statistics Dashboard</p>
          <h1>监管数据统计</h1>
          <p>汇总辖区企业、检查、投诉与预警数据，支持每日监管决策。</p>
        </div>
        <div class="stats-hero__meta">
          <div><span>统计口径</span><strong>区域管理员</strong></div>
          <div><span>刷新频率</span><strong>实时</strong></div>
        </div>
      </header>

      <section class="quick-grid" aria-label="统计概览入口">
        <article class="quick-card">
          <span>核心指标</span>
          <strong>监管全景</strong>
          <p>企业、检查、抽检、投诉、预警一图联动</p>
        </article>
        <article class="quick-card">
          <span>风险导向</span>
          <strong>预警统计</strong>
          <p>支持按时间范围、等级、状态查看趋势与效率</p>
        </article>
      </section>

      <section class="stats-switch">
        <button
          type="button"
          class="switch-btn"
          :class="{ active: activePanel === 'overview' }"
          @click="activePanel = 'overview'"
        >
          监管全景
        </button>
        <button
          type="button"
          class="switch-btn"
          :class="{ active: activePanel === 'warning' }"
          @click="activePanel = 'warning'"
        >
          预警统计
        </button>
      </section>

      <section class="stats-panel">
        <SupervisionOverviewPanel v-if="activePanel === 'overview'" :token="token" mode="admin" />
        <WarningStatsPanel v-else :token="token" mode="admin" />
      </section>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { ref } from "vue";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import SupervisionOverviewPanel from "../../components/SupervisionOverviewPanel.vue";
import WarningStatsPanel from "../../components/WarningStatsPanel.vue";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();
const activePanel = ref("overview");
</script>

<style scoped>
.stats-page { display: grid; gap: 14px; }
.stats-hero {
  border-radius: 4px;
  border: 1px solid #d7e1ec;
  background: linear-gradient(180deg, #f8fbff 0%, #f2f7fd 100%);
  color: #0f172a;
  padding: 18px;
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
}
.stats-hero__eyebrow {
  margin: 0;
  color: #38699c;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.stats-hero h1 { margin: 6px 0 0; font-size: 30px; line-height: 1.1; font-weight: 900; color: #0f172a; }
.stats-hero p { margin: 8px 0 0; color: #5b7088; font-size: 13px; max-width: 620px; }
.stats-hero__meta { display: grid; gap: 8px; min-width: 210px; }
.stats-hero__meta div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #d8e3ef;
  background: #ffffff;
  border-radius: 4px;
  min-height: 34px;
  padding: 0 10px;
  font-size: 12px;
}
.stats-hero__meta span { color: #64748b; }
.stats-hero__meta strong { color: #0f172a; font-size: 13px; }
.quick-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.quick-card { border-radius: 4px; border: 1px solid #dbe2ea; background: #fff; padding: 12px 14px; }
.quick-card span { display: block; color: #64748b; font-size: 10px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.08em; }
.quick-card strong { display: block; margin-top: 6px; color: #0f172a; font-size: 18px; line-height: 1.15; font-weight: 800; }
.quick-card p { margin: 4px 0 0; color: #64748b; font-size: 12px; }

.stats-switch {
  display: inline-flex;
  align-items: center;
  border: 1px solid #dbe2ea;
  border-radius: 4px;
  background: #f8fbff;
  padding: 3px;
  gap: 4px;
  width: fit-content;
}
.switch-btn {
  border: none;
  border-radius: 2px;
  background: transparent;
  color: #64748b;
  min-height: 30px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}
.switch-btn.active {
  background: #ffffff;
  color: #0f3a72;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
}

.stats-panel {
  border: 1px solid #dbe2ea;
  border-radius: 4px;
  background: #ffffff;
  padding: 12px;
}

@media (max-width: 980px) {
  .stats-hero { flex-direction: column; align-items: flex-start; }
  .stats-hero__meta { width: 100%; min-width: 0; }
  .quick-grid { grid-template-columns: 1fr; }
}
</style>
