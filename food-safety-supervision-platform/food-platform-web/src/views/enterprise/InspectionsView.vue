<template>
  <section class="enterprise-page">
    <form class="enterprise-card filter-strip" @submit.prevent="handleSearch">
      <label class="enterprise-field">
        <span>检查结果</span>
        <select v-model="filters.result">
          <option value="">全部</option>
          <option value="PASS">合格</option>
          <option value="FAIL">不合格</option>
        </select>
      </label>
      <label class="enterprise-field">
        <span>起始日期</span>
        <input v-model="filters.startDate" type="date" />
      </label>
      <label class="enterprise-field">
        <span>截止日期</span>
        <input v-model="filters.endDate" type="date" />
      </label>
      <button class="enterprise-primary-button enterprise-primary-button--compact" type="submit">查询</button>
    </form>

    <div class="inspection-list">
      <article v-for="record in records" :key="record.id" class="enterprise-card inspection-row-card">
        <div>
          <p class="section-kicker">{{ record.inspectionDate }}</p>
          <h3>{{ record.title }}</h3>
          <p>{{ record.problemDesc }}</p>
        </div>
        <div class="inspection-row-card__meta">
          <span :class="['enterprise-chip', `enterprise-chip--${record.result === 'PASS' ? 'success' : 'danger'}`]">
            {{ record.result === 'PASS' ? '合格' : '待整改' }}
          </span>
          <RouterLink class="enterprise-ghost-button" :to="{ name: 'enterprise-inspection-detail', params: { id: record.id } }">查看详情</RouterLink>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { loadInspections } from "../../services/enterpriseGateway";
import { getStoredSession } from "../../session/authSession";

const filters = reactive({ result: "", startDate: "", endDate: "" });
const records = ref([]);

async function handleSearch() {
  const session = getStoredSession();
  const data = await loadInspections(session?.token || "", { ...filters, page: 1, size: 10 });
  records.value = data.records || [];
}

onMounted(handleSearch);
</script>
