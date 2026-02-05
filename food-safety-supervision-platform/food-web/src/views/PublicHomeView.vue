<template>
  <div class="public-shell">
    <header class="public-hero">
      <div class="hero-copy">
        <span class="badge badge--public">公众服务</span>
        <h1>食品安全公众服务平台</h1>
        <p>
          投诉提交、进度查询、风险公示与科普提示一站式入口。
          让信息更透明，让处理更可追踪。
        </p>
        <div class="hero-actions">
          <button class="primary" type="button" @click="$emit('open-complaint')">我要投诉</button>
          <button class="ghost" type="button" @click="$emit('open-track')">查询进度</button>
          <button class="link" type="button" @click="$emit('back')">返回登录</button>
        </div>
      </div>
      <div class="hero-cards">
        <div class="hero-card">
          <strong>闭环处理</strong>
          <span>受理 → 派发 → 处理 → 反馈</span>
        </div>
        <div class="hero-card">
          <strong>信息公示</strong>
          <span>重点监管、抽检结果可查询</span>
        </div>
        <div class="hero-card">
          <strong>便民服务</strong>
          <span>投诉指引与食品安全提示</span>
        </div>
      </div>
    </header>

    <section class="public-grid">
      <article class="service-card highlight">
        <div class="card-head">
          <span class="chip">核心服务</span>
          <h3>投诉服务</h3>
          <p>企业名称、内容、联系方式一键提交，实时可追踪。</p>
        </div>
        <div class="card-actions">
          <button class="primary" type="button" @click="$emit('open-complaint')">提交投诉</button>
          <button class="ghost" type="button" @click="$emit('open-track')">查询进度</button>
        </div>
      </article>

      <article class="service-card">
        <div class="card-head">
          <span class="chip">公开信息</span>
          <h3>企业公示</h3>
          <p>查看企业状态、负责人信息与监管记录。</p>
        </div>
        <button class="ghost" type="button" @click="$emit('open-enterprise')">进入公示</button>
      </article>

      <article class="service-card">
        <div class="card-head">
          <span class="chip">风险提示</span>
          <h3>预警与公告</h3>
          <p>风险预警、抽检结果、重要通知集中展示。</p>
        </div>
        <button class="ghost" type="button" @click="$emit('open-bulletin')">查看公告</button>
      </article>
    </section>

    <section class="public-flow">
      <div>
        <h3>三步完成投诉</h3>
        <p>填写信息 → 获取单号 → 查询进度，全流程可追踪。</p>
      </div>
      <div class="flow-steps">
        <div class="flow-step">
          <span>01</span>
          <strong>提交投诉</strong>
          <p>填写企业名称、投诉类型与内容。</p>
        </div>
        <div class="flow-step">
          <span>02</span>
          <strong>保存单号</strong>
          <p>投诉成功后系统返回单号。</p>
        </div>
        <div class="flow-step">
          <span>03</span>
          <strong>查询进度</strong>
          <p>输入单号与联系方式获取处理结果。</p>
        </div>
      </div>
    </section>

    <section class="public-notice">
      <div class="notice-head">
        <h3>最新公告</h3>
        <span>演示数据</span>
      </div>
      <div class="notice-list">
        <div class="notice-item">
          <strong>重点监管企业名单更新</strong>
          <span>2026-02-04</span>
        </div>
        <div class="notice-item">
          <strong>节日期间食品安全提示</strong>
          <span>2026-02-02</span>
        </div>
        <div class="notice-item">
          <strong>抽检不合格产品公示</strong>
          <span>2026-01-28</span>
        </div>
      </div>
    </section>

    <footer class="public-footer">
      <div>食品安全监管热线：12315</div>
      <div>本页面为演示版，数据仅供参考</div>
    </footer>
  </div>
</template>

<script setup>
defineEmits(["back", "open-complaint", "open-track", "open-enterprise", "open-bulletin"]);
</script>

<style scoped>
.public-shell {
  min-height: 100vh;
  padding: 36px 48px 48px;
  background:
    radial-gradient(circle at 10% 10%, rgba(13, 94, 166, 0.18), transparent 45%),
    radial-gradient(circle at 85% 18%, rgba(15, 139, 141, 0.14), transparent 40%),
    var(--bg);
  display: grid;
  gap: 28px;
}

.public-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr);
  gap: 24px;
  align-items: center;
  padding: 26px 30px;
  border-radius: 20px;
  background: linear-gradient(140deg, #ffffff 0%, #f1f7ff 100%);
  border: 1px solid var(--stroke);
  box-shadow: var(--shadow);
}

.hero-copy h1 {
  margin: 10px 0 10px;
  font-size: 30px;
}

.hero-copy p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.hero-actions {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.hero-actions .link {
  background: none;
  border: none;
  color: var(--muted);
  cursor: pointer;
  font-size: 13px;
}

.badge--public {
  background: var(--accent);
}

.hero-cards {
  display: grid;
  gap: 12px;
}

.hero-card {
  padding: 14px 16px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid var(--stroke);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.hero-card strong {
  display: block;
  font-size: 14px;
  margin-bottom: 4px;
}

.hero-card span {
  font-size: 12px;
  color: var(--muted);
}

.public-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.service-card {
  padding: 18px;
  border-radius: 16px;
  border: 1px solid var(--stroke);
  background: #ffffff;
  display: grid;
  gap: 14px;
  box-shadow: var(--shadow);
}

.service-card.highlight {
  background: linear-gradient(135deg, #ffffff 0%, #eef6ff 100%);
  border-color: rgba(13, 94, 166, 0.35);
}

.card-head h3 {
  margin: 8px 0;
  font-size: 18px;
}

.card-head p {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
  font-size: 13px;
}

.chip {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: #e9f1f8;
  font-size: 11px;
  color: var(--muted);
}

.card-actions {
  display: flex;
  gap: 10px;
}

.public-flow {
  padding: 20px 24px;
  border-radius: 18px;
  border: 1px solid var(--stroke);
  background: #ffffff;
  display: grid;
  gap: 14px;
}

.public-flow h3 {
  margin: 0 0 4px;
}

.public-flow p {
  margin: 0;
  color: var(--muted);
}

.flow-steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.flow-step {
  border-radius: 14px;
  padding: 12px 14px;
  border: 1px solid var(--stroke);
  background: #f7fbff;
  display: grid;
  gap: 6px;
}

.flow-step span {
  font-weight: 700;
  color: var(--primary);
}

.flow-step p {
  margin: 0;
  font-size: 12px;
  color: var(--muted);
}

.public-notice {
  padding: 18px 22px;
  border-radius: 16px;
  border: 1px solid var(--stroke);
  background: #ffffff;
  display: grid;
  gap: 12px;
}

.notice-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: var(--muted);
}

.notice-list {
  display: grid;
  gap: 10px;
}

.notice-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px dashed rgba(215, 226, 238, 0.8);
  background: #f9fbff;
  font-size: 13px;
}

.public-footer {
  display: flex;
  justify-content: space-between;
  color: var(--muted);
  font-size: 12px;
  padding: 0 4px;
}

@media (max-width: 1100px) {
  .public-hero {
    grid-template-columns: 1fr;
  }

  .public-grid {
    grid-template-columns: 1fr;
  }

  .flow-steps {
    grid-template-columns: 1fr;
  }

  .public-footer {
    flex-direction: column;
    gap: 6px;
  }
}

@media (max-width: 720px) {
  .public-shell {
    padding: 24px 18px 36px;
  }

  .hero-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .card-actions {
    flex-direction: column;
  }
}
</style>
