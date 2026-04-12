<template>
  <section class="enterprise-page">
    <div class="enterprise-grid enterprise-grid--profile">
      <article class="enterprise-card enterprise-card--summary">
        <div class="section-head">
          <h3>备案总览</h3>
          <RouterLink :to="{ name: 'enterprise-profile-detail' }">查看详情</RouterLink>
        </div>
        <div class="summary-hero">
          <div>
            <p class="section-kicker">企业名称</p>
            <h2>{{ profile.enterpriseName }}</h2>
            <p>{{ profile.regionText }} · {{ profile.addressDetail }}</p>
          </div>
          <span class="enterprise-chip enterprise-chip--success">{{ profile.archiveStatus || '已核准' }}</span>
        </div>
        <dl class="data-pairs data-pairs--compact">
          <div><dt>许可证编号</dt><dd>{{ profile.licenseNo }}</dd></div>
          <div><dt>负责人</dt><dd>{{ profile.principal }}</dd></div>
          <div><dt>联系电话</dt><dd>{{ profile.principalPhone }}</dd></div>
          <div><dt>电子邮箱</dt><dd>{{ profile.email }}</dd></div>
        </dl>
      </article>

      <article class="enterprise-card">
        <div class="section-head">
          <h3>审核状态</h3>
          <span class="enterprise-chip enterprise-chip--success">APPROVED</span>
        </div>
        <ul class="detail-list">
          <li>
            <strong>审核意见</strong>
            <span>{{ profile.approvalComment }}</span>
          </li>
          <li>
            <strong>审核时间</strong>
            <span>{{ profile.approvedTime }}</span>
          </li>
          <li>
            <strong>下次例检</strong>
            <span>{{ profile.nextReviewAt }}</span>
          </li>
        </ul>
      </article>

      <article class="enterprise-card">
        <div class="section-head">
          <h3>备案附件</h3>
          <span>{{ profile.attachments?.length || 0 }} 份</span>
        </div>
        <div class="attachment-list">
          <div v-for="attachment in profile.attachments || []" :key="attachment.id" class="attachment-item">
            <span class="material-symbols-outlined">description</span>
            <div>
              <strong>{{ attachment.name }}</strong>
              <p>{{ attachment.type }}</p>
            </div>
          </div>
        </div>
        <div class="hero-actions">
          <RouterLink class="enterprise-primary-button" :to="{ name: 'enterprise-profile-detail' }">编辑备案信息</RouterLink>
          <RouterLink class="enterprise-ghost-button" :to="{ name: 'enterprise-products' }">前往维护产品档案</RouterLink>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive } from "vue";
import { loadEnterpriseProfile } from "../../services/enterpriseGateway";
import { getStoredSession } from "../../session/authSession";

const profile = reactive({ attachments: [] });

onMounted(async () => {
  const session = getStoredSession();
  Object.assign(profile, await loadEnterpriseProfile(session?.token || ""));
});
</script>
