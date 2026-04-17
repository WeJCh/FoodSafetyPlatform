<template>
  <RegulatorEnforcerPageShell
    active-key="stats"
    title="数据统计"
    subtitle="围绕执法任务、风险预警与日常监管闭环提供统计视图，页面结构参考执法端原型的指标头图与分区展示。"
  >
    <section class="stats-hero">
      <div>
        <p class="stats-hero__eyebrow">ENFORCER DASHBOARD</p>
        <h3>执法数据看板</h3>
        <p class="stats-hero__copy">
          默认按当前执法人员权限范围聚合，支持从监管全景与预警统计两个角度切换查看。
        </p>
      </div>
      <div class="stats-hero__meta">
        <article>
          <span>统计对象</span>
          <strong>执法人员</strong>
        </article>
        <article>
          <span>刷新方式</span>
          <strong>实时接口</strong>
        </article>
        <article>
          <span>图表覆盖</span>
          <strong>总览 + 预警</strong>
        </article>
      </div>
    </section>

    <section class="quick-grid">
      <article class="quick-card quick-card--primary">
        <span>监管全景</span>
        <strong>企业、检查、抽检、投诉统一视角</strong>
        <p>适合日常巡查前快速了解当前辖区或个人口径下的监管概况。</p>
      </article>
      <article class="quick-card">
        <span>风险预警</span>
        <strong>趋势、类型、等级、处置效率</strong>
        <p>适合在处置高风险预警、查看超时与闭环效率时集中分析。</p>
      </article>
    </section>

    <section class="switch-bar">
      <button type="button" class="switch-btn" :class="{ active: activePanel === 'overview' }" @click="activePanel = 'overview'">
        监管全景
      </button>
      <button type="button" class="switch-btn" :class="{ active: activePanel === 'warning' }" @click="activePanel = 'warning'">
        预警统计
      </button>
    </section>

    <section class="stats-panel">
      <SupervisionOverviewPanel v-if="activePanel === 'overview'" :token="token" mode="enforcer" />
      <WarningStatsPanel v-else :token="token" mode="enforcer" />
    </section>
  </RegulatorEnforcerPageShell>
</template>

<script setup>
import { ref } from "vue";
import SupervisionOverviewPanel from "../../components/SupervisionOverviewPanel.vue";
import WarningStatsPanel from "../../components/WarningStatsPanel.vue";
import RegulatorEnforcerPageShell from "./RegulatorEnforcerPageShell.vue";
import { useRegulatorEnforcerShellSession } from "./regulatorEnforcerShared";

const { token } = useRegulatorEnforcerShellSession();
const activePanel = ref("overview");
</script>

<style scoped>
.stats-hero {
  border: 1px solid #dbe3ee;
  background: linear-gradient(135deg, #f7fbff 0%, #edf4ff 100%);
  padding: 18px;
  margin-bottom: 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
}
.stats-hero__eyebrow {
  margin: 0;
  color: #2d5d99;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
.stats-hero h3 {
  margin: 8px 0 0;
  color: #0f172a;
  font-size: 30px;
  font-weight: 900;
}
.stats-hero__copy {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
  max-width: 680px;
}
.stats-hero__meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  min-width: 420px;
}
.stats-hero__meta article {
  border: 1px solid #dbe3ee;
  background: rgba(255, 255, 255, 0.9);
  padding: 12px;
}
.stats-hero__meta span,
.quick-card span {
  display: block;
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
}
.stats-hero__meta strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 14px;
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}
.quick-card {
  border: 1px solid #dbe3ee;
  background: #fff;
  padding: 14px;
}
.quick-card--primary {
  background: linear-gradient(135deg, #0f3a72 0%, #1d4f91 100%);
}
.quick-card--primary span,
.quick-card--primary strong,
.quick-card--primary p {
  color: #fff;
}
.quick-card strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}
.quick-card p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}
.switch-bar {
  display: inline-flex;
  gap: 6px;
  padding: 4px;
  margin-bottom: 14px;
  border: 1px solid #dbe3ee;
  background: #f8fbff;
}
.switch-btn {
  min-height: 34px;
  padding: 0 14px;
  border: 0;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}
.switch-btn.active {
  background: #fff;
  color: #0f3a72;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
}
.stats-panel {
  border: 1px solid #dbe3ee;
  background: #fff;
  padding: 12px;
}
@media (max-width: 1080px) {
  .stats-hero,
  .quick-grid {
    grid-template-columns: 1fr;
  }
  .stats-hero {
    display: grid;
  }
  .stats-hero__meta {
    min-width: 0;
    grid-template-columns: 1fr;
  }
}
@media (max-width: 720px) {
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
