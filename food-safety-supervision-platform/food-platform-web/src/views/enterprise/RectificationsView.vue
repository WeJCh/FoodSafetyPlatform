<template>
  <section class="enterprise-page">
    <form class="enterprise-card filter-strip" @submit.prevent="handleSearch">
      <label class="enterprise-field">
        <span>整改状态</span>
        <select v-model="filters.status">
          <option value="">全部</option>
          <option value="ONGOING">整改中</option>
          <option value="SUBMITTED">待复核</option>
          <option value="REWORK">打回重做</option>
          <option value="CONFIRMED">已确认</option>
        </select>
      </label>
      <button class="enterprise-primary-button enterprise-primary-button--compact" type="submit">查询</button>
    </form>

    <div class="inspection-list">
      <article v-for="item in records" :key="item.id" class="enterprise-card inspection-row-card">
        <div>
          <p class="section-kicker">{{ item.taskNo }}</p>
          <h3>{{ item.title }}</h3>
          <p>{{ item.rectificationDesc }}</p>
        </div>
        <div class="inspection-row-card__meta">
          <span :class="['enterprise-chip', `enterprise-chip--${statusTone(item.status)}`]">{{ statusLabel(item.status) }}</span>
          <RouterLink class="enterprise-ghost-button" :to="{ name: 'enterprise-rectification-detail', params: { id: item.id } }">查看详情</RouterLink>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { loadRectifications } from "../../services/enterpriseGateway";
import { getStoredSession } from "../../session/authSession";

const filters = reactive({ status: "" });
const records = ref([]);

function statusLabel(status) {
  return {
    ONGOING: "整改中",
    SUBMITTED: "待复核",
    REWORK: "打回重做",
    CONFIRMED: "已确认"
  }[status] || status;
}

function statusTone(status) {
  return {
    ONGOING: "warning",
    SUBMITTED: "primary",
    REWORK: "danger",
    CONFIRMED: "success"
  }[status] || "neutral";
}

async function handleSearch() {
  const session = getStoredSession();
  const data = await loadRectifications(session?.token || "", { ...filters, page: 1, size: 10 });
  records.value = data.records || [];
}

onMounted(handleSearch);
</script>
