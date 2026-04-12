
<template>
  <div class="admin-shell regulator-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">监管中心</div>
      <div class="sidebar-meta">
        <span>账号：{{ regulatorUser.username }}</span>
        <span>角色：{{ regulatorUser.userType }}</span>
        <span>职责：执法人员</span>
      </div>
      <nav class="admin-nav">
        <button :class="{ active: section === 'enterprises' }" @click="section = 'enterprises'">企业监管</button>
        <button :class="{ active: section === 'tasks' }" @click="handleTaskEnter">我的任务</button>
        <button :class="{ active: section === 'sampling' }" @click="handleSamplingEnter">抽检任务</button>
        <button :class="{ active: section === 'inspections' }" @click="handleInspectionEnter">检查记录</button>
        <button :class="{ active: section === 'rectification' }" @click="handleRectificationEnter">整改跟进</button>
        <button :class="{ active: section === 'complaints' }" @click="handleComplaintEnter">投诉处理</button>
        <button :class="{ active: section === 'warnings' }" @click="handleWarningEnter">风险预警</button>
        <button :class="{ active: section === 'stats' }" @click="section = 'stats'">数据统计</button>
      </nav>
      <button class="ghost sidebar-ghost" type="button" @click="handleLogout">退出登录</button>
    </aside>

    <div class="admin-main">
      <div class="dashboard-topbar">
        <div class="dashboard-title">
          <strong>执法人员工作台</strong>
          <span>任务执行、抽检录入、检查记录与整改跟进</span>
        </div>
        <div class="user-chip">
          <span>{{ regulatorUser.username }}</span>
          <span>执法人员</span>
        </div>
      </div>

      <div class="dashboard-content">
        <div class="card dashboard-card">

          <div class="sub-nav">
            <button :class="{ active: section === 'enterprises' }" @click="section = 'enterprises'">企业列表</button>
            <button :class="{ active: section === 'tasks' }" @click="handleTaskEnter">我的任务</button>
            <button :class="{ active: section === 'sampling' }" @click="handleSamplingEnter">抽检任务</button>
            <button :class="{ active: section === 'inspections' }" @click="handleInspectionEnter">检查记录</button>
            <button :class="{ active: section === 'rectification' }" @click="handleRectificationEnter">整改跟进</button>
            <button :class="{ active: section === 'warnings' }" @click="handleWarningEnter">风险预警</button>
            <button :class="{ active: section === 'stats' }" @click="section = 'stats'">数据统计</button>
          </div>

          <div v-if="section === 'enterprises'">
            <div class="section-title">企业监管</div>
            <form class="filter-bar filter-bar--triple" @submit.prevent="handleSearch">
              <label>企业名称<input v-model.trim="filters.enterpriseName" placeholder="输入企业名称" /></label>
              <label>企业状态
                <select v-model="filters.status">
                  <option value="">全部</option>
                  <option value="NORMAL">正常</option>
                  <option value="KEY">重点监管</option>
                </select>
              </label>
              <label>审核状态
                <select v-model="filters.approvalStatus">
                  <option value="">全部</option>
                  <option value="PENDING">待审核</option>
                  <option value="APPROVED">已通过</option>
                  <option value="REJECTED">已驳回</option>
                </select>
              </label>
              <button class="primary" type="submit" :disabled="loading">{{ loading ? "查询中..." : "查询" }}</button>
            </form>

            <div class="list-table">
              <div class="list-row list-header">
                <span>企业名称</span><span>状态</span><span>审核</span><span>负责人</span><span>更新时间</span><span>操作</span>
              </div>
              <div v-if="!records.length" class="list-empty">暂无企业数据</div>
              <div v-for="item in records" :key="item.id" class="list-row">
                <span>{{ item.enterpriseName }}</span>
                <span>{{ formatStatus(item.status) }}</span>
                <span>{{ formatApprovalStatus(item.approvalStatus) }}</span>
                <span>{{ item.principal || "-" }}</span>
                <span>{{ formatTime(item.updateTime) }}</span>
                <button class="ghost" type="button" @click="handleViewDetail(item)">查看详情</button>
              </div>
            </div>

            <div class="pager">
              <span>共{{ total }} 条，{{ page }}/{{ pages }} 页</span>
              <div class="pager-actions">
                <button class="ghost" type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
                <button class="ghost" type="button" :disabled="page >= pages" @click="changePage(page + 1)">下一页</button>
            </div>
          </div>
        </div>
          <div v-else-if="section === 'tasks'">
            <div class="section-title">我的任务</div>
            <form class="filter-bar filter-bar--compact" @submit.prevent="handleTaskSearch">
              <label>任务状态
                <select v-model="taskFilters.status">
                  <option value="">全部</option>
                  <option value="ASSIGNED">待执行</option>
                  <option value="IN_PROGRESS">执行中</option>
                  <option value="COMPLETED">已完成</option>
                  <option value="CLOSED">已归档</option>
                </select>
              </label>
              <button class="primary" type="submit" :disabled="taskLoading">{{ taskLoading ? "查询中..." : "查询" }}</button>
            </form>

            <div class="list-table task-table">
              <div class="list-row list-header task-header">
                <span>任务编号</span><span>企业</span><span>优先级</span><span>状态</span><span>截止时间</span><span>操作</span>
              </div>
              <div v-if="!taskRecords.length" class="list-empty">暂无任务</div>
              <div v-for="task in taskRecords" :key="task.id" class="list-row task-row">
                <span>{{ task.taskNo }}</span>
                <span>{{ task.enterpriseName || "-" }}</span>
                <span>{{ formatTaskPriority(task.priority) }}</span>
                <span>{{ formatTaskStatus(task.status) }}</span>
                <span>{{ formatTime(task.deadline) }}</span>
                <div class="action-buttons">
                  <button class="ghost" type="button" :disabled="taskLoading" @click="openTaskDetail(task)">查看详情</button>
                  <button v-if="task.status === 'ASSIGNED'" class="ghost" type="button" :disabled="taskLoading" @click="handleStartTask(task)">开始执行</button>
                  <button v-if="task.status === 'IN_PROGRESS'" class="primary" type="button" @click="handleSelectTask(task)">提交检查</button>
                </div>
              </div>
            </div>

            <div class="pager">
              <span>共{{ taskTotal }} 条，{{ taskPage }}/{{ taskPages }} 页</span>
              <div class="pager-actions">
                <button class="ghost" type="button" :disabled="taskPage <= 1" @click="changeTaskPage(taskPage - 1)">上一页</button>
                <button class="ghost" type="button" :disabled="taskPage >= taskPages" @click="changeTaskPage(taskPage + 1)">下一页</button>
              </div>
            </div>

            <div v-if="activeTask" class="task-submit">
              <div class="section-subtitle">检查结果填报</div>
              <div class="task-meta">
                <span>任务：{{ activeTask.taskNo }}</span>
                <span>企业：{{ activeTask.enterpriseName || "-" }}</span>
              </div>
              <form class="task-form" @submit.prevent="handleSubmitTask">
                <label>检查日期<input v-model="taskForm.inspectionDate" type="date" required /></label>
                <label>检查结果
                  <select v-model="taskForm.result">
                    <option value="PASS">合格</option>
                    <option value="FAIL">不合格</option>
                  </select>
                </label>
                <label>总体问题描述<textarea v-model.trim="taskForm.problemDesc" rows="3" placeholder="如有问题请填写"></textarea></label>
                <div class="task-items">
                  <div class="task-item" v-for="(item, index) in taskForm.items" :key="index">
                    <input v-model.trim="item.itemName" placeholder="检查项" />
                    <select v-model="item.itemResult">
                      <option value="PASS">合格</option>
                      <option value="FAIL">不合格</option>
                    </select>
                    <input v-model.trim="item.problemDesc" placeholder="问题描述（可选）" />
                    <button class="ghost" type="button" @click="removeItem(index)">删除</button>
                  </div>
                  <button class="ghost" type="button" @click="addItem">添加检查项</button>
                </div>
                <div class="task-actions">
                  <button class="primary" type="submit" :disabled="taskLoading">{{ taskLoading ? "提交中..." : "提交结果" }}</button>
                  <button class="ghost" type="button" @click="clearActiveTask">取消</button>
                </div>
              </form>
            </div>

            <div v-if="detailTask" class="modal-mask" @click.self="closeTaskDetail">
              <div class="modal-card task-detail-modal">
                <div class="modal-title">任务详情</div>
                <div class="task-detail-header">
                  <span class="task-chip task-chip--status">{{ formatTaskStatus(detailTask.status) }}</span>
                  <span class="task-chip task-chip--priority">{{ formatTaskPriority(detailTask.priority) }}</span>
                  <span class="task-chip">{{ detailTask.taskNo || "-" }}</span>
                </div>
                <div class="task-detail-grid">
                  <section class="task-detail-section">
                    <div class="task-detail-section-title">企业信息</div>
                    <div v-if="detailTaskLoading" class="task-detail-loading">加载企业信息中...</div>
                    <div v-else class="task-detail-fields">
                      <div class="task-detail-field">
                        <span>企业名称</span>
                        <strong>{{ detailTaskEnterprise?.enterpriseName || detailTask.enterpriseName || "-" }}</strong>
                      </div>
                      <div class="task-detail-field">
                        <span>负责人姓名</span>
                        <strong>{{ detailTaskEnterprise?.principal || "-" }}</strong>
                      </div>
                      <div class="task-detail-field">
                        <span>所属区域</span>
                        <strong>{{ detailTaskRegionName || "-" }}</strong>
                      </div>
                      <div class="task-detail-field task-detail-field--full">
                        <span>详细地址</span>
                        <strong>{{ detailTaskEnterprise?.addressDetail || "-" }}</strong>
                      </div>
                    </div>
                  </section>
                  <section class="task-detail-section">
                    <div class="task-detail-section-title">任务信息</div>
                    <div class="task-detail-fields">
                      <div class="task-detail-field">
                        <span>任务标题</span>
                        <strong>{{ detailTask.taskTitle || "-" }}</strong>
                      </div>
                      <div class="task-detail-field task-detail-field--full">
                        <span>任务描述</span>
                        <strong>{{ detailTask.taskDesc || "暂无任务描述" }}</strong>
                      </div>
                      <div class="task-detail-field">
                        <span>截止时间</span>
                        <strong>{{ formatTime(detailTask.deadline) }}</strong>
                      </div>
                    </div>
                  </section>
                </div>
                <div class="modal-actions"><button class="ghost" type="button" @click="closeTaskDetail">关闭</button></div>
              </div>
            </div>
          </div>

          <div v-else-if="section === 'sampling'">
            <div class="section-title">抽检任务</div>
            <form class="filter-bar filter-bar--compact" @submit.prevent="handleSamplingSearch">
              <label>任务状态
                <select v-model="samplingFilters.status">
                  <option value="">全部</option>
                  <option value="ASSIGNED">待抽检</option>
                  <option value="COMPLETED">已完成</option>
                  <option value="CLOSED">已归档</option>
                </select>
              </label>
              <button class="primary" type="submit" :disabled="samplingLoading">{{ samplingLoading ? "查询中..." : "查询" }}</button>
            </form>

            <div class="list-table task-table">
              <div class="list-row list-header sampling-header">
                <span>任务编号</span><span>企业</span><span>产品</span><span>优先级</span><span>状态</span><span>截止时间</span><span>操作</span>
              </div>
              <div v-if="!samplingRecords.length" class="list-empty">暂无抽检任务</div>
              <div v-for="task in samplingRecords" :key="task.id" class="list-row sampling-row">
                <span>{{ task.taskNo }}</span>
                <span>{{ task.enterpriseName || "-" }}</span>
                <div>
                  <div class="primary-text">{{ task.productName || "-" }}</div>
                  <div class="secondary-text">{{ task.productSpecification || "暂无规格" }}</div>
                </div>
                <span>{{ formatTaskPriority(task.priority) }}</span>
                <span>{{ formatSamplingTaskStatus(task.status) }}</span>
                <span>{{ formatTime(task.deadline) }}</span>
                <div class="action-buttons">
                  <button class="ghost" type="button" :disabled="samplingLoading" @click="openSamplingTaskDetail(task)">查看详情</button>
                  <button v-if="task.status === 'ASSIGNED'" class="primary" type="button" @click="handleSelectSamplingTask(task)">提交结果</button>
                </div>
              </div>
            </div>

            <div class="pager">
              <span>共{{ samplingTotal }} 条，{{ samplingPage }}/{{ samplingPages }} 页</span>
              <div class="pager-actions">
                <button class="ghost" type="button" :disabled="samplingPage <= 1" @click="changeSamplingPage(samplingPage - 1)">上一页</button>
                <button class="ghost" type="button" :disabled="samplingPage >= samplingPages" @click="changeSamplingPage(samplingPage + 1)">下一页</button>
              </div>
            </div>

            <div v-if="activeSamplingTask" class="task-submit">
              <div class="section-subtitle">抽检结果填报</div>
              <div class="task-meta">
                <span>任务：{{ activeSamplingTask.taskNo }}</span>
                <span>企业：{{ activeSamplingTask.enterpriseName || "-" }}</span>
                <span>产品：{{ activeSamplingTask.productName || "-" }}</span>
              </div>
              <form class="task-form" @submit.prevent="handleSubmitSamplingResult">
                <label>采样时间<input v-model="samplingForm.sampledTime" type="datetime-local" required /></label>
                <label>抽检结果
                  <select v-model="samplingForm.result">
                    <option value="PASS">合格</option>
                    <option value="FAIL">不合格</option>
                  </select>
                </label>
                <label>抽检结论<textarea v-model.trim="samplingForm.conclusion" rows="3" placeholder="填写抽检结论"></textarea></label>
                <label>处置建议<textarea v-model.trim="samplingForm.disposalSuggestion" rows="3" placeholder="填写后续建议"></textarea></label>
                <div class="task-actions">
                  <button class="primary" type="submit" :disabled="samplingLoading">{{ samplingLoading ? "提交中..." : "提交结果" }}</button>
                  <button class="ghost" type="button" @click="clearActiveSamplingTask">取消</button>
                </div>
              </form>
            </div>

            <div v-if="samplingDetailTask" class="modal-mask" @click.self="closeSamplingTaskDetail">
              <div class="modal-card task-detail-modal">
                <div class="modal-title">抽检任务详情</div>
                <div class="task-detail-header">
                  <span class="task-chip task-chip--status">{{ formatSamplingTaskStatus(samplingDetailTask.status) }}</span>
                  <span class="task-chip task-chip--priority">{{ formatTaskPriority(samplingDetailTask.priority) }}</span>
                  <span class="task-chip">{{ samplingDetailTask.taskNo || "-" }}</span>
                </div>
                <div class="task-detail-grid">
                  <section class="task-detail-section">
                    <div class="task-detail-section-title">企业信息</div>
                    <div v-if="samplingDetailLoading" class="task-detail-loading">加载企业信息中...</div>
                    <div v-else class="task-detail-fields">
                      <div class="task-detail-field">
                        <span>企业名称</span>
                        <strong>{{ samplingDetailEnterprise?.enterpriseName || samplingDetailTask.enterpriseName || "-" }}</strong>
                      </div>
                      <div class="task-detail-field">
                        <span>负责人姓名</span>
                        <strong>{{ samplingDetailEnterprise?.principal || "-" }}</strong>
                      </div>
                      <div class="task-detail-field">
                        <span>所属区域</span>
                        <strong>{{ samplingDetailRegionName || "-" }}</strong>
                      </div>
                      <div class="task-detail-field task-detail-field--full">
                        <span>详细地址</span>
                        <strong>{{ samplingDetailEnterprise?.addressDetail || "-" }}</strong>
                      </div>
                    </div>
                  </section>
                  <section class="task-detail-section">
                    <div class="task-detail-section-title">抽检信息</div>
                    <div class="task-detail-fields">
                      <div class="task-detail-field">
                        <span>任务标题</span>
                        <strong>{{ samplingDetailTask.taskTitle || "-" }}</strong>
                      </div>
                      <div class="task-detail-field">
                        <span>抽检产品</span>
                        <strong>{{ samplingDetailTask.productName || "-" }}</strong>
                      </div>
                      <div class="task-detail-field">
                        <span>产品类别</span>
                        <strong>{{ samplingDetailTask.productCategory || "-" }}</strong>
                      </div>
                      <div class="task-detail-field">
                        <span>产品规格</span>
                        <strong>{{ samplingDetailTask.productSpecification || "-" }}</strong>
                      </div>
                      <div class="task-detail-field">
                        <span>截止时间</span>
                        <strong>{{ formatTime(samplingDetailTask.deadline) }}</strong>
                      </div>
                      <div class="task-detail-field task-detail-field--full">
                        <span>任务描述</span>
                        <strong>{{ samplingDetailTask.taskDesc || "暂无任务描述" }}</strong>
                      </div>
                    </div>
                  </section>
                </div>
                <div class="modal-actions"><button class="ghost" type="button" @click="closeSamplingTaskDetail">关闭</button></div>
              </div>
            </div>
          </div>

          <div v-else-if="section === 'inspections'">
            <div class="section-title">检查记录</div>
            <form class="filter-bar filter-bar--quad" @submit.prevent="handleInspectionSearch">
              <label>企业名称<input v-model.trim="inspectionFilters.enterpriseName" placeholder="输入企业名称" /></label>
              <label>检查结果
                <select v-model="inspectionFilters.result">
                  <option value="">全部</option>
                  <option value="PASS">合格</option>
                  <option value="FAIL">不合格</option>
                </select>
              </label>
              <label>起始日期<input v-model="inspectionFilters.startDate" type="date" /></label>
              <label>截止日期<input v-model="inspectionFilters.endDate" type="date" /></label>
              <button class="primary" type="submit" :disabled="inspectionLoading">{{ inspectionLoading ? "查询中..." : "查询" }}</button>
            </form>

            <div class="list-table inspection-table">
              <div class="list-row list-header inspection-header">
                <span>企业名称</span><span>检查日期</span><span>结果</span><span>更新时间</span><span>操作</span>
              </div>
              <div v-if="!inspectionRecords.length" class="list-empty">暂无检查记录</div>
              <div v-for="record in inspectionRecords" :key="record.id" class="list-row inspection-row">
                <span>{{ record.enterpriseName || "-" }}</span>
                <span>{{ record.inspectionDate || "-" }}</span>
                <span>{{ formatInspectionResult(record.result) }}</span>
                <span>{{ formatTime(record.updateTime) }}</span>
                <button class="ghost" type="button" @click="openInspectionDetail(record)">查看详情</button>
              </div>
            </div>

            <div class="pager">
              <span>共{{ inspectionTotal }} 条，{{ inspectionPage }}/{{ inspectionPages }} 页</span>
              <div class="pager-actions">
                <button class="ghost" type="button" :disabled="inspectionPage <= 1" @click="changeInspectionPage(inspectionPage - 1)">上一页</button>
                <button class="ghost" type="button" :disabled="inspectionPage >= inspectionPages" @click="changeInspectionPage(inspectionPage + 1)">下一页</button>
              </div>
            </div>

            <div v-if="inspectionDetail" class="modal-mask" @click.self="closeInspectionDetail">
              <div class="modal-card">
                <div class="modal-title">检查记录详情</div>
                <div class="modal-body">
                  <div class="modal-field"><span>企业名称</span><strong>{{ inspectionDetail.record.enterpriseName || "-" }}</strong></div>
                  <div class="modal-field"><span>检查日期</span><strong>{{ inspectionDetail.record.inspectionDate || "-" }}</strong></div>
                  <div class="modal-field"><span>检查结果</span><strong>{{ formatInspectionResult(inspectionDetail.record.result) }}</strong></div>
                  <div class="modal-field"><span>问题描述</span><strong>{{ inspectionDetail.record.problemDesc || "-" }}</strong></div>
                  <div class="modal-field">
                    <span>检查明细</span>
                    <div class="modal-list">
                      <div v-if="!inspectionDetail.items.length" class="modal-empty">暂无检查明细</div>
                      <div v-for="(item, index) in inspectionDetail.items" :key="index" class="modal-item">
                        <div class="modal-item-name">{{ item.itemName || "-" }}</div>
                        <div class="modal-item-meta">{{ formatInspectionResult(item.itemResult) }}</div>
                        <div class="modal-item-desc">{{ item.problemDesc || "-" }}</div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="modal-actions"><button class="ghost" type="button" @click="closeInspectionDetail">关闭</button></div>
              </div>
            </div>
          </div>

          <div v-else-if="section === 'rectification'">
            <div class="section-title">整改跟进</div>
            <form class="filter-bar filter-bar--triple" @submit.prevent="handleRectificationSearch">
              <label>整改状态
                <select v-model="rectificationFilters.status">
                  <option value="">全部</option>
                  <option value="ONGOING">整改中</option>
                  <option value="SUBMITTED">待复核</option>
                  <option value="REWORK">打回重做</option>
                  <option value="CONFIRMED">已确认</option>
                </select>
              </label>
              <label>企业名称<input v-model.trim="rectificationFilters.enterpriseName" placeholder="输入企业名称" /></label>
              <button class="primary" type="submit" :disabled="rectificationLoading">
                {{ rectificationLoading ? "查询中..." : "查询" }}
              </button>
            </form>

            <div class="list-table">
              <div class="list-row list-header rectification-header">
                <span>企业名称</span><span>状态</span><span>整改时限</span><span>整改说明</span><span>更新时间</span><span>操作</span>
              </div>
              <div v-if="!rectificationRecords.length" class="list-empty">暂无整改任务</div>
              <div v-for="item in rectificationRecords" :key="item.id" class="list-row rectification-row">
                <span>{{ item.enterpriseName || "-" }}</span>
                <span>{{ formatRectificationStatus(item.status) }}</span>
                <span :class="['rectification-sla', `rectification-sla--${rectificationSlaClass(item)}`]">
                  {{ formatRectificationSla(item) }}
                </span>
                <div class="rectification-text" :title="item.progress || '-'">
                  {{ item.progress || "企业暂未提交整改说明" }}
                </div>
                <span>{{ formatTime(item.updateTime) }}</span>
                <div class="action-buttons">
                  <button class="ghost" type="button" @click="openRectificationDetail(item)">查看详情</button>
                </div>
              </div>
            </div>

            <div class="pager">
              <span>共{{ rectificationTotal }} 条，{{ rectificationPage }}/{{ rectificationPages }} 页</span>
              <div class="pager-actions">
                <button class="ghost" type="button" :disabled="rectificationPage <= 1" @click="changeRectificationPage(rectificationPage - 1)">上一页</button>
                <button class="ghost" type="button" :disabled="rectificationPage >= rectificationPages" @click="changeRectificationPage(rectificationPage + 1)">下一页</button>
              </div>
            </div>
          </div>

          <div v-else-if="section === 'complaints'">
            <div class="section-title">投诉处理</div>
            <form class="filter-bar filter-bar--double" @submit.prevent="handleComplaintSearch">
              <label>投诉状态
                <select v-model="complaintFilters.status">
                  <option value="">全部</option>
                  <option value="ASSIGNED">已派发</option>
                  <option value="PROCESSING">处理中</option>
                  <option value="FEEDBACKED">已反馈</option>
                </select>
              </label>
              <label>企业名称<input v-model.trim="complaintFilters.enterpriseName" placeholder="输入企业名称" /></label>
              <button class="primary" type="submit" :disabled="complaintLoading">{{ complaintLoading ? "查询中..." : "查询" }}</button>
            </form>

            <div class="list-table complaint-table">
              <div class="list-row list-header complaint-header">
                <span>投诉编号</span><span>企业名称</span><span>状态</span><span>处理人</span><span>更新时间</span><span>操作</span>
              </div>
              <div v-if="!complaintRecords.length" class="list-empty">暂无投诉</div>
              <div v-for="item in complaintRecords" :key="item.id" class="list-row complaint-row">
                <span>{{ item.complaintNo }}</span>
                <span>{{ item.enterpriseName || "-" }}</span>
                <span>{{ formatComplaintStatus(item.status) }}</span>
                <span>{{ item.assignedToName || "-" }}</span>
                <span>{{ formatTime(item.updateTime) }}</span>
                <div class="action-buttons">
                  <button class="ghost" type="button" @click="handleViewComplaint(item)">查看详情</button>
                  <button
                    v-if="item.status === 'ASSIGNED'"
                    class="primary"
                    type="button"
                    :disabled="complaintLoading"
                    @click="handleStartComplaint(item)"
                  >
                    开始处理
                  </button>
                </div>
              </div>
            </div>

            <div class="pager">
              <span>共{{ complaintTotal }} 条，{{ complaintPage }}/{{ complaintPages }} 页</span>
              <div class="pager-actions">
                <button class="ghost" type="button" :disabled="complaintPage <= 1" @click="changeComplaintPage(complaintPage - 1)">上一页</button>
                <button class="ghost" type="button" :disabled="complaintPage >= complaintPages" @click="changeComplaintPage(complaintPage + 1)">下一页</button>
              </div>
            </div>

          </div>

          <div v-else-if="section === 'warnings'">
            <div class="section-title">风险预警</div>
            <div class="warning-quick-tools">
              <button
                class="ghost warning-quick-toggle"
                :class="{ active: warningOnlyPending }"
                type="button"
                @click="toggleWarningOnlyPending"
              >
                仅看待处理：{{ warningOnlyPending ? "开启" : "关闭" }}
              </button>
              <span v-if="warningOnlyPending" class="warning-quick-tip">已固定筛选状态为“待处理”</span>
            </div>
            <form class="filter-bar filter-bar--five" @submit.prevent="handleWarningSearch">
              <label>状态
                <select v-model="warningFilters.status" :disabled="warningOnlyPending">
                  <option value="">全部</option>
                  <option value="OPEN">待处理</option>
                  <option value="PROCESSING">处理中</option>
                  <option value="RESOLVED">已解决</option>
                  <option value="CLOSED">已归档</option>
                </select>
              </label>
              <label>等级
                <select v-model="warningFilters.level">
                  <option value="">全部</option>
                  <option value="L1">一级</option>
                  <option value="L2">二级</option>
                </select>
              </label>
              <label>预警类型<input v-model.trim="warningFilters.warningType" placeholder="例：SLA_OVERDUE_SUBMIT" /></label>
              <label>业务类型<input v-model.trim="warningFilters.bizType" placeholder="例：RECTIFICATION" /></label>
              <label>关键词<input v-model.trim="warningFilters.keyword" placeholder="标题或内容关键词" /></label>
              <button class="primary" type="submit" :disabled="warningLoading || warningActionLoading">
                {{ warningLoading ? "查询中..." : "查询" }}
              </button>
            </form>

            <div class="list-table warning-table">
              <div class="list-row list-header warning-header">
                <span>预警编号</span><span>预警标题</span><span>等级</span><span>状态</span><span>触发次数</span><span>最近触发</span><span>操作</span>
              </div>
              <div v-if="!warningRecords.length" class="list-empty">暂无预警记录</div>
              <div v-for="item in warningRecords" :key="item.id" class="list-row warning-row">
                <span>{{ item.warningNo || "-" }}</span>
                <span class="warning-title-cell" :title="item.title || '-'">{{ item.title || "-" }}</span>
                <span>{{ formatWarningLevel(item.level) }}</span>
                <span :class="['warning-status-chip', `warning-status-chip--${warningStatusClass(item.status)}`]">
                  {{ formatWarningStatus(item.status) }}
                </span>
                <span>{{ item.triggerCount || 0 }}</span>
                <span>{{ formatTime(item.lastOccurTime) }}</span>
                <div class="action-buttons">
                  <button class="ghost" type="button" @click="openWarningDetail(item)">查看详情</button>
                  <button
                    v-if="warningQuickAction(item.status)"
                    class="primary"
                    type="button"
                    :disabled="warningActionLoading"
                    @click="handleWarningAction(item, warningQuickAction(item.status).actionType)"
                  >
                    {{ warningQuickAction(item.status).label }}
                  </button>
                </div>
              </div>
            </div>

            <div class="pager">
              <span>共{{ warningTotal }} 条，{{ warningPage }}/{{ warningPages }} 页</span>
              <div class="pager-actions">
                <button
                  class="ghost"
                  type="button"
                  :disabled="warningPage <= 1 || warningLoading"
                  @click="changeWarningPage(warningPage - 1)"
                >
                  上一页
                </button>
                <button
                  class="ghost"
                  type="button"
                  :disabled="warningPage >= warningPages || warningLoading"
                  @click="changeWarningPage(warningPage + 1)"
                >
                  下一页
                </button>
              </div>
            </div>

            <div v-if="warningDetailVisible" class="modal-mask" @click.self="closeWarningDetail">
              <div class="modal-card warning-detail-modal">
                <div class="modal-title">预警详情</div>
                <div class="modal-body">
                  <div v-if="warningDetailLoading" class="modal-empty">详情加载中...</div>
                  <template v-else-if="warningDetail">
                    <div class="warning-summary-grid">
                      <div class="warning-summary-item">
                        <span>预警编号</span>
                        <strong>{{ warningDetail.warningNo || "-" }}</strong>
                      </div>
                      <div class="warning-summary-item">
                        <span>状态</span>
                        <strong>{{ formatWarningStatus(warningDetail.status) }}</strong>
                      </div>
                      <div class="warning-summary-item">
                        <span>等级</span>
                        <strong>{{ formatWarningLevel(warningDetail.level) }}</strong>
                      </div>
                      <div class="warning-summary-item">
                        <span>触发次数</span>
                        <strong>{{ warningDetail.triggerCount || 0 }}</strong>
                      </div>
                    </div>
                    <div class="modal-field"><span>预警标题</span><strong>{{ warningDetail.title || "-" }}</strong></div>
                    <div class="modal-field"><span>预警内容</span><strong>{{ warningDetail.content || "-" }}</strong></div>
                    <div class="modal-field">
                      <span>负载数据</span>
                      <pre class="warning-payload">{{ formatWarningPayload(warningDetail.payloadJson) }}</pre>
                    </div>
                    <div class="modal-field">
                      <span>处理记录</span>
                      <div class="warning-timeline-list">
                        <div v-if="!warningDetail.processLogs || !warningDetail.processLogs.length" class="modal-empty">
                          暂无处理记录
                        </div>
                        <div v-for="log in warningDetail.processLogs || []" :key="log.id" class="warning-timeline-item">
                          <span class="warning-timeline-dot" :class="warningTimelineDotClass(log.actionType)"></span>
                          <div class="warning-timeline-content">
                            <div class="warning-timeline-header">
                              <div class="warning-timeline-name">{{ formatWarningAction(log.actionType) }}</div>
                              <div class="warning-timeline-time">{{ formatTime(log.createTime) }}</div>
                            </div>
                            <div class="warning-timeline-meta">操作人：{{ log.operatorName || "-" }}</div>
                            <div class="warning-timeline-desc">{{ log.actionComment || "无说明" }}</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </template>
                </div>
                <div class="modal-actions warning-detail-actions">
                  <button
                    v-if="warningDetail && canJumpWarningComplaint(warningDetail)"
                    class="ghost"
                    type="button"
                    @click="jumpToWarningComplaint(warningDetail)"
                  >
                    跳转投诉详情
                  </button>
                  <button
                    v-if="warningDetail && canJumpWarningRectification(warningDetail)"
                    class="ghost"
                    type="button"
                    @click="jumpToWarningRectification(warningDetail)"
                  >
                    跳转整改详情
                  </button>
                  <button
                    v-if="warningDetail && warningQuickAction(warningDetail.status)"
                    class="primary"
                    type="button"
                    :disabled="warningActionLoading"
                    @click="handleWarningAction(warningDetail, warningQuickAction(warningDetail.status).actionType)"
                  >
                    {{ warningQuickAction(warningDetail.status).label }}
                  </button>
                  <button class="ghost" type="button" @click="closeWarningDetail">关闭</button>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="section === 'stats'" class="stats-dashboard">
            <SupervisionOverviewPanel :token="token" mode="enforcer" />
            <WarningStatsPanel :token="token" mode="enforcer" />
          </div>

          <div v-else class="placeholder">
            <strong>功能建设中</strong>
            <p>{{ sectionLabel }} 相关能力正在完善，请稍后再试。</p>
          </div>

          <RectificationDetailModal
            :visible="rectificationDetailVisible"
            :detail="rectificationDetail"
            :action-logs="rectificationActionLogs"
            :detail-loading="rectificationDetailLoading"
            :reviewable="false"
            :reviewing="false"
            @close="closeRectificationDetail"
          />

          <div class="status" :class="status.type" v-if="status.message">{{ status.message }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchComplaints, startComplaintProcess } from "../api/complaint";
import { getActiveSession, performLogout } from "../session/authRuntime";
import {
  fetchEnterpriseDetail,
  fetchEnterprises,
  fetchMyWarningDetail,
  fetchMyWarningRecords,
  fetchRegionPath,
  processMyWarning
} from "../api/regulation";
import {
  fetchInspectionRecordDetail,
  fetchMyInspectionRecords,
  fetchMySamplingTasks,
  fetchMyInspectionTasks,
  fetchMyRegulatorRectifications,
  fetchRectificationActions,
  fetchRectificationDetail,
  startInspectionTask,
  submitSamplingResult,
  submitInspectionTask
} from "../api/regulationOperation";
import RectificationDetailModal from "../components/RectificationDetailModal.vue";
import SupervisionOverviewPanel from "../components/SupervisionOverviewPanel.vue";
import WarningStatsPanel from "../components/WarningStatsPanel.vue";
import { formatByMap, formatTime } from "../utils/formatters";
import {
  approvalStatusMap,
  complaintStatusMap,
  enterpriseStatusMap,
  warningActionMap,
  warningLevelMap,
  warningStatusMap
} from "../utils/statusMaps";

const router = useRouter();
const route = useRoute();
const session = computed(() => getActiveSession() || {});
const regulatorUser = computed(() => session.value);
const token = computed(() => session.value.token || "");

function normalizeSection(value) {
  const sectionMap = new Set([
    "enterprises",
    "tasks",
    "sampling",
    "inspections",
    "rectification",
    "complaints",
    "warnings",
    "stats"
  ]);
  return sectionMap.has(value) ? value : "enterprises";
}

function getSectionRouteName(sectionValue) {
  const routeNameMap = {
    enterprises: "regulator-enforcer-enterprises",
    tasks: "regulator-enforcer-tasks",
    sampling: "regulator-enforcer-sampling",
    inspections: "regulator-enforcer-inspections",
    rectification: "regulator-enforcer-rectifications",
    complaints: "regulator-enforcer-complaints",
    warnings: "regulator-enforcer-warnings",
    stats: "regulator-enforcer-stats"
  };
  return routeNameMap[sectionValue] || "regulator-enforcer-enterprises";
}

function resolveCurrentSection() {
  return normalizeSection(route.meta?.initialSection);
}

const section = ref(resolveCurrentSection());

const filters = reactive({ enterpriseName: "", status: "", approvalStatus: "" });
const status = reactive({ message: "", type: "" });
const loading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);

const taskLoading = ref(false);
const taskRecords = ref([]);
const taskPage = ref(1);
const taskSize = ref(8);
const taskTotal = ref(0);
const taskPages = ref(1);
const taskFilters = reactive({ status: "" });
const activeTask = ref(null);
const detailTask = ref(null);
const detailTaskEnterprise = ref(null);
const detailTaskRegionName = ref("-");
const detailTaskLoading = ref(false);
const samplingLoading = ref(false);
const samplingRecords = ref([]);
const samplingPage = ref(1);
const samplingSize = ref(8);
const samplingTotal = ref(0);
const samplingPages = ref(1);
const samplingFilters = reactive({ status: "" });
const activeSamplingTask = ref(null);
const samplingDetailTask = ref(null);
const samplingDetailEnterprise = ref(null);
const samplingDetailRegionName = ref("-");
const samplingDetailLoading = ref(false);

const inspectionFilters = reactive({ enterpriseName: "", result: "", startDate: "", endDate: "" });
const inspectionRecords = ref([]);
const inspectionLoading = ref(false);
const inspectionPage = ref(1);
const inspectionSize = ref(8);
const inspectionTotal = ref(0);
const inspectionPages = ref(1);
const inspectionDetail = ref(null);

const complaintLoading = ref(false);
const complaintRecords = ref([]);
const complaintPage = ref(1);
const complaintSize = ref(8);
const complaintTotal = ref(0);
const complaintPages = ref(1);
const complaintFilters = reactive({ status: "", enterpriseName: "" });
const rectificationLoading = ref(false);
const rectificationRecords = ref([]);
const rectificationPage = ref(1);
const rectificationSize = ref(8);
const rectificationTotal = ref(0);
const rectificationPages = ref(1);
const rectificationFilters = reactive({ status: "", enterpriseName: "" });
const warningLoading = ref(false);
const warningActionLoading = ref(false);
const warningDetailLoading = ref(false);
const warningRecords = ref([]);
const warningPage = ref(1);
const warningSize = ref(8);
const warningTotal = ref(0);
const warningPages = ref(1);
const warningFilters = reactive({
  status: "",
  level: "",
  warningType: "",
  bizType: "",
  keyword: ""
});
const warningOnlyPending = ref(false);
const warningStatusBackup = ref("");
const warningDetailVisible = ref(false);
const warningDetail = ref(null);
const rectificationDetailVisible = ref(false);
const rectificationDetailLoading = ref(false);
const rectificationDetail = ref(null);
const rectificationActionLogs = ref([]);

const taskForm = reactive({
  inspectionDate: "",
  result: "PASS",
  problemDesc: "",
  items: [{ itemName: "", itemResult: "PASS", problemDesc: "" }]
});
const samplingForm = reactive({
  sampledTime: "",
  result: "PASS",
  conclusion: "",
  disposalSuggestion: ""
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

const sectionLabelMap = {
  tasks: "我的任务",
  sampling: "抽检任务",
  inspections: "检查记录",
  rectification: "整改跟进",
  complaints: "投诉处理",
  warnings: "风险预警",
  stats: "数据统计"
};

const sectionLabel = computed(() => sectionLabelMap[section.value] || "当前模块");

const taskStatusMap = { CREATED: "待派发", ASSIGNED: "待执行", IN_PROGRESS: "执行中", COMPLETED: "已完成", CLOSED: "已归档" };
const samplingTaskStatusMap = { CREATED: "待派发", ASSIGNED: "待抽检", COMPLETED: "已完成", CLOSED: "已归档" };
const taskPriorityMap = { LOW: "低", MEDIUM: "中", HIGH: "高" };
const inspectionResultMap = { PASS: "合格", FAIL: "不合格" };
const rectificationStatusMap = {
  ONGOING: "整改中",
  SUBMITTED: "待复核",
  REWORK: "打回重做",
  CONFIRMED: "已确认"
};
function formatStatus(value) { return formatByMap(value, enterpriseStatusMap); }
function formatApprovalStatus(value) { return formatByMap(value, approvalStatusMap); }
function formatTaskStatus(value) { return taskStatusMap[value] || value || "-"; }
function formatSamplingTaskStatus(value) { return samplingTaskStatusMap[value] || value || "-"; }
function formatTaskPriority(value) { return taskPriorityMap[value] || value || "-"; }
function formatInspectionResult(value) { return inspectionResultMap[value] || value || "-"; }
function formatRectificationStatus(value) { return rectificationStatusMap[value] || value || "-"; }
function formatComplaintStatus(value) { return formatByMap(value, complaintStatusMap); }
function formatWarningStatus(value) { return formatByMap(value, warningStatusMap); }
function formatWarningLevel(value) { return formatByMap(value, warningLevelMap); }
function formatWarningAction(value) { return formatByMap(value, warningActionMap); }

function warningStatusClass(value) {
  if (value === "OPEN") return "open";
  if (value === "PROCESSING") return "processing";
  if (value === "RESOLVED") return "resolved";
  if (value === "CLOSED") return "closed";
  return "unknown";
}

function warningQuickAction(statusValue) {
  if (statusValue === "OPEN") return { actionType: "PROCESS", label: "开始处理" };
  if (statusValue === "PROCESSING") return { actionType: "RESOLVE", label: "标记解决" };
  return null;
}

function warningTimelineDotClass(actionType) {
  const value = String(actionType || "").toUpperCase();
  if (value === "RESOLVE" || value === "AUTO_ARCHIVE") return "done";
  if (value === "PROCESS" || value === "ASSIGN" || value === "AUTO_LEVEL_UP") return "active";
  return "";
}

function formatWarningPayload(payloadJson) {
  if (!payloadJson) return "-";
  try {
    return JSON.stringify(JSON.parse(payloadJson), null, 2);
  } catch {
    return String(payloadJson);
  }
}

function canJumpWarningComplaint(warning) {
  return String(warning?.bizType || "").toUpperCase() === "COMPLAINT" && Number(warning?.bizId) > 0;
}

function canJumpWarningRectification(warning) {
  return String(warning?.bizType || "").toUpperCase() === "RECTIFICATION" && Number(warning?.bizId) > 0;
}

function jumpToWarningComplaint(warning) {
  if (!canJumpWarningComplaint(warning)) return;
  router.push({
    name: "regulator-enforcer-complaint-detail",
    params: { complaintId: Number(warning.bizId) },
    query: { from: "warnings" }
  }).catch(() => {});
  closeWarningDetail();
}

async function jumpToWarningRectification(warning) {
  if (!canJumpWarningRectification(warning)) return;
  const rectificationId = Number(warning.bizId);
  closeWarningDetail();
  section.value = "rectification";
  await loadRectifications();
  await openRectificationDetailById(rectificationId);
}

async function load() {
  loading.value = true; setStatus("");
  try {
    const data = await fetchEnterprises(token.value, { ...filters, page: page.value, size: size.value });
    records.value = data.records || [];
    total.value = data.total || 0;
    page.value = data.page || 1;
    size.value = data.size || size.value;
    pages.value = data.pages || 1;
  } catch (error) {
    setStatus(error.message || "加载企业列表失败", "error");
  } finally {
    loading.value = false;
  }
}

async function loadTasks() {
  taskLoading.value = true; setStatus("");
  try {
    const data = await fetchMyInspectionTasks(token.value, { ...taskFilters, page: taskPage.value, size: taskSize.value });
    taskRecords.value = data.records || [];
    taskTotal.value = data.total || 0;
    taskPage.value = data.page || 1;
    taskSize.value = data.size || taskSize.value;
    taskPages.value = data.pages || 1;
  } catch (error) {
    setStatus(error.message || "加载任务列表失败", "error");
  } finally {
    taskLoading.value = false;
  }
}

async function loadSamplingTasks() {
  samplingLoading.value = true; setStatus("");
  try {
    const data = await fetchMySamplingTasks(token.value, {
      ...samplingFilters,
      page: samplingPage.value,
      size: samplingSize.value
    });
    samplingRecords.value = data.records || [];
    samplingTotal.value = data.total || 0;
    samplingPage.value = data.page || 1;
    samplingSize.value = data.size || samplingSize.value;
    samplingPages.value = data.pages || 1;
  } catch (error) {
    setStatus(error.message || "加载抽检任务失败", "error");
  } finally {
    samplingLoading.value = false;
  }
}

async function handleTaskSearch() { taskPage.value = 1; await loadTasks(); }
async function changeTaskPage(nextPage) { taskPage.value = nextPage; await loadTasks(); }
async function handleSamplingSearch() { samplingPage.value = 1; await loadSamplingTasks(); }
async function changeSamplingPage(nextPage) { samplingPage.value = nextPage; await loadSamplingTasks(); }
async function handleSamplingEnter() { section.value = "sampling"; await loadSamplingTasks(); }
async function handleInspectionEnter() { section.value = "inspections"; await loadInspections(); }
async function handleRectificationEnter() { section.value = "rectification"; await loadRectifications(); }
async function handleComplaintEnter() { section.value = "complaints"; await loadComplaints(); }
async function handleWarningEnter() { section.value = "warnings"; await loadWarnings(); }
async function handleInspectionSearch() { inspectionPage.value = 1; await loadInspections(); }
async function changeInspectionPage(nextPage) { inspectionPage.value = nextPage; await loadInspections(); }

async function loadInspections() {
  inspectionLoading.value = true; setStatus("");
  try {
    const data = await fetchMyInspectionRecords(token.value, { ...inspectionFilters, page: inspectionPage.value, size: inspectionSize.value });
    inspectionRecords.value = data.records || [];
    inspectionTotal.value = data.total || 0;
    inspectionPage.value = data.page || 1;
    inspectionSize.value = data.size || inspectionSize.value;
    inspectionPages.value = data.pages || 1;
  } catch (error) {
    setStatus(error.message || "加载检查记录失败", "error");
  } finally {
    inspectionLoading.value = false;
  }
}

async function loadComplaints() {
  complaintLoading.value = true; setStatus("");
  try {
    const data = await fetchComplaints(token.value, { ...complaintFilters, page: complaintPage.value, size: complaintSize.value });
    complaintRecords.value = data.records || [];
    complaintTotal.value = data.total || 0;
    complaintPage.value = data.page || 1;
    complaintSize.value = data.size || complaintSize.value;
    complaintPages.value = data.pages || 1;
  } catch (error) {
    setStatus(error.message || "加载投诉列表失败", "error");
  } finally {
    complaintLoading.value = false;
  }
}

async function loadRectifications() {
  rectificationLoading.value = true; setStatus("");
  try {
    const data = await fetchMyRegulatorRectifications(token.value, {
      ...rectificationFilters,
      page: rectificationPage.value,
      size: rectificationSize.value
    });
    rectificationRecords.value = data.records || [];
    rectificationTotal.value = data.total || 0;
    rectificationPage.value = data.page || 1;
    rectificationSize.value = data.size || rectificationSize.value;
    rectificationPages.value = data.pages || 1;
    // 列表刷新时同步已打开详情，避免时间线与状态过期。
    if (rectificationDetailVisible.value && rectificationDetail.value?.id) {
      await openRectificationDetailById(rectificationDetail.value.id, true);
    }
  } catch (error) {
    setStatus(error.message || "加载整改任务失败", "error");
  } finally {
    rectificationLoading.value = false;
  }
}

async function loadWarnings() {
  warningLoading.value = true; setStatus("");
  try {
    const data = await fetchMyWarningRecords(token.value, {
      ...warningFilters,
      page: warningPage.value,
      size: warningSize.value
    });
    warningRecords.value = data.records || [];
    warningTotal.value = data.total || 0;
    warningPage.value = data.page || 1;
    warningSize.value = data.size || warningSize.value;
    warningPages.value = data.pages || 1;
  } catch (error) {
    setStatus(error.message || "加载预警列表失败", "error");
  } finally {
    warningLoading.value = false;
  }
}

async function handleComplaintSearch() { complaintPage.value = 1; await loadComplaints(); }
async function changeComplaintPage(nextPage) { complaintPage.value = nextPage; await loadComplaints(); }
async function handleRectificationSearch() { rectificationPage.value = 1; await loadRectifications(); }
async function changeRectificationPage(nextPage) { rectificationPage.value = nextPage; await loadRectifications(); }
async function handleWarningSearch() { warningPage.value = 1; await loadWarnings(); }
async function changeWarningPage(nextPage) { warningPage.value = nextPage; await loadWarnings(); }
async function toggleWarningOnlyPending() {
  warningOnlyPending.value = !warningOnlyPending.value;
  if (warningOnlyPending.value) {
    warningStatusBackup.value = warningFilters.status;
    warningFilters.status = "OPEN";
  } else {
    warningFilters.status = warningStatusBackup.value || "";
  }
  warningPage.value = 1;
  await loadWarnings();
}

async function handleStartComplaint(item) {
  if (!item?.id) return;
  complaintLoading.value = true;
  setStatus("", "info");
  try {
    await startComplaintProcess(token.value, item.id);
    setStatus("已开始处理投诉", "success");
    await loadComplaints();
  } catch (error) {
    setStatus(error.message || "开始处理失败", "error");
  } finally {
    complaintLoading.value = false;
  }
}

async function openWarningDetail(item) {
  if (!item?.id) return;
  warningDetailVisible.value = true;
  warningDetailLoading.value = true;
  warningDetail.value = null;
  try {
    warningDetail.value = await fetchMyWarningDetail(token.value, item.id);
  } catch (error) {
    setStatus(error.message || "加载预警详情失败", "error");
    warningDetailVisible.value = false;
  } finally {
    warningDetailLoading.value = false;
  }
}

function closeWarningDetail() {
  warningDetailVisible.value = false;
  warningDetail.value = null;
  warningDetailLoading.value = false;
}

async function openRectificationDetailById(rectificationId, silent = false) {
  if (!rectificationId) return;
  rectificationDetailVisible.value = true;
  if (!silent) {
    rectificationDetailLoading.value = true;
  }
  rectificationDetail.value = { id: rectificationId };
  rectificationActionLogs.value = [];
  try {
    const [detail, actions] = await Promise.all([
      fetchRectificationDetail(token.value, rectificationId),
      fetchRectificationActions(token.value, rectificationId)
    ]);
    rectificationDetail.value = detail || rectificationDetail.value;
    rectificationActionLogs.value = Array.isArray(actions) ? actions : [];
  } catch (error) {
    if (!silent) {
      setStatus(error.message || "加载整改详情失败", "error");
      closeRectificationDetail();
    }
  } finally {
    if (!silent) {
      rectificationDetailLoading.value = false;
    }
  }
}

async function openRectificationDetail(item) {
  if (!item?.id) return;
  await openRectificationDetailById(item.id);
}

function closeRectificationDetail() {
  rectificationDetailVisible.value = false;
  rectificationDetail.value = null;
  rectificationActionLogs.value = [];
  rectificationDetailLoading.value = false;
}

async function handleWarningAction(target, actionType) {
  const warningId = target?.id;
  if (!warningId || !actionType) return;
  warningActionLoading.value = true;
  setStatus("");
  try {
    const detail = await processMyWarning(token.value, warningId, { actionType });
    if (warningDetailVisible.value && warningDetail.value?.id === warningId) {
      warningDetail.value = detail;
    }
    setStatus(`预警已执行${formatWarningAction(actionType)}`, "success");
    await loadWarnings();
  } catch (error) {
    setStatus(error.message || "预警处理失败", "error");
  } finally {
    warningActionLoading.value = false;
  }
}

function handleViewComplaint(item) {
  if (!item?.id) return;
  router.push({
    name: "regulator-enforcer-complaint-detail",
    params: { complaintId: item.id },
    query: { from: section.value }
  }).catch(() => {});
}

async function handleStartTask(task) {
  taskLoading.value = true; setStatus("");
  try {
    await startInspectionTask(token.value, task.id);
    setStatus("任务已开始执行", "success");
    await loadTasks();
  } catch (error) {
    setStatus(error.message || "开始任务失败", "error");
  } finally {
    taskLoading.value = false;
  }
}

function handleSelectTask(task) {
  activeTask.value = task;
  taskForm.inspectionDate = new Date().toISOString().slice(0, 10);
  taskForm.result = "PASS";
  taskForm.problemDesc = "";
  taskForm.items = [{ itemName: "", itemResult: "PASS", problemDesc: "" }];
}

function handleSelectSamplingTask(task) {
  activeSamplingTask.value = task;
  samplingForm.sampledTime = new Date().toISOString().slice(0, 16);
  samplingForm.result = "PASS";
  samplingForm.conclusion = "";
  samplingForm.disposalSuggestion = "";
}

function clearActiveTask() { activeTask.value = null; }
function clearActiveSamplingTask() { activeSamplingTask.value = null; }
async function openTaskDetail(task) {
  if (!task) return;
  detailTask.value = task;
  detailTaskEnterprise.value = null;
  detailTaskRegionName.value = "-";
  if (!task.enterpriseId) return;

  detailTaskLoading.value = true;
  try {
    const enterprise = await fetchEnterpriseDetail(token.value, task.enterpriseId);
    detailTaskEnterprise.value = enterprise || null;
    if (enterprise?.regionId) {
      const path = await fetchRegionPath(token.value, enterprise.regionId).catch(() => []);
      detailTaskRegionName.value = Array.isArray(path) && path.length
        ? path.map((item) => item.name).join("/")
        : "-";
    }
  } catch (error) {
    setStatus(error.message || "加载企业信息失败", "error");
  } finally {
    detailTaskLoading.value = false;
  }
}

async function openSamplingTaskDetail(task) {
  if (!task) return;
  samplingDetailTask.value = task;
  samplingDetailEnterprise.value = null;
  samplingDetailRegionName.value = "-";
  if (!task.enterpriseId) return;

  samplingDetailLoading.value = true;
  try {
    const enterprise = await fetchEnterpriseDetail(token.value, task.enterpriseId);
    samplingDetailEnterprise.value = enterprise || null;
    if (enterprise?.regionId) {
      const path = await fetchRegionPath(token.value, enterprise.regionId).catch(() => []);
      samplingDetailRegionName.value = Array.isArray(path) && path.length
        ? path.map((item) => item.name).join("/")
        : "-";
    }
  } catch (error) {
    setStatus(error.message || "加载企业信息失败", "error");
  } finally {
    samplingDetailLoading.value = false;
  }
}

function closeTaskDetail() {
  detailTask.value = null;
  detailTaskEnterprise.value = null;
  detailTaskRegionName.value = "-";
  detailTaskLoading.value = false;
}

function closeSamplingTaskDetail() {
  samplingDetailTask.value = null;
  samplingDetailEnterprise.value = null;
  samplingDetailRegionName.value = "-";
  samplingDetailLoading.value = false;
}

async function openInspectionDetail(record) {
  if (!record?.id) return;
  inspectionLoading.value = true;
  try {
    inspectionDetail.value = await fetchInspectionRecordDetail(token.value, record.id);
  } catch (error) {
    setStatus(error.message || "加载检查记录失败", "error");
  } finally {
    inspectionLoading.value = false;
  }
}

function closeInspectionDetail() { inspectionDetail.value = null; }

function addItem() { taskForm.items.push({ itemName: "", itemResult: "PASS", problemDesc: "" }); }
function removeItem(index) { if (taskForm.items.length <= 1) return; taskForm.items.splice(index, 1); }

async function handleSubmitTask() {
  if (!activeTask.value) return;
  if (!taskForm.inspectionDate) { setStatus("请选择检查日期", "error"); return; }
  taskLoading.value = true; setStatus("");
  try {
    const items = taskForm.items.filter((item) => item.itemName && item.itemName.trim()).map((item) => ({
      itemName: item.itemName, itemResult: item.itemResult, problemDesc: item.problemDesc
    }));
    await submitInspectionTask(token.value, activeTask.value.id, {
      inspectionDate: taskForm.inspectionDate,
      result: taskForm.result,
      problemDesc: taskForm.problemDesc,
      items
    });
    setStatus("检查结果已提交", "success");
    clearActiveTask();
    await loadTasks();
  } catch (error) {
    setStatus(error.message || "提交结果失败", "error");
  } finally {
    taskLoading.value = false;
  }
}

async function handleSubmitSamplingResult() {
  if (!activeSamplingTask.value) return;
  if (!samplingForm.sampledTime) {
    setStatus("请选择采样时间", "error");
    return;
  }
  samplingLoading.value = true; setStatus("");
  try {
    await submitSamplingResult(token.value, activeSamplingTask.value.id, {
      sampledTime: normalizeDateTimeInput(samplingForm.sampledTime),
      result: samplingForm.result,
      conclusion: samplingForm.conclusion,
      disposalSuggestion: samplingForm.disposalSuggestion
    });
    setStatus("抽检结果已提交", "success");
    clearActiveSamplingTask();
    await loadSamplingTasks();
  } catch (error) {
    setStatus(error.message || "提交抽检结果失败", "error");
  } finally {
    samplingLoading.value = false;
  }
}

async function handleSearch() { page.value = 1; await load(); }
async function changePage(nextPage) { page.value = nextPage; await load(); }
async function handleTaskEnter() { section.value = "tasks"; await loadTasks(); }
async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
}
function handleViewDetail(item) {
  router.push({
    name: "regulator-enforcer-enterprise-detail",
    params: { enterpriseId: item.id },
    query: { from: section.value }
  }).catch(() => {});
}
function normalizeDateTimeInput(value) { return value && value.length === 16 ? `${value}:00` : value; }

function formatDurationMinutes(minutes) {
  const total = Math.max(0, Number(minutes) || 0);
  const days = Math.floor(total / (24 * 60));
  const hours = Math.floor((total % (24 * 60)) / 60);
  const mins = total % 60;
  if (days > 0) return `${days}天${hours}小时`;
  if (hours > 0) return `${hours}小时${mins}分钟`;
  return `${mins}分钟`;
}

function rectificationSlaClass(item) {
  if (!item) return "none";
  if (item.slaStatus === "OVERDUE") return "overdue";
  if (item.slaStatus === "DUE_SOON") return "warning";
  if (item.slaStatus === "NORMAL") return "normal";
  return "none";
}

function formatRectificationSla(item) {
  if (!item) return "-";
  const remaining = Number(item.remainingMinutes);
  if (item.slaStatus === "OVERDUE") {
    return `已超时 ${formatDurationMinutes(Math.abs(remaining))}`;
  }
  if (item.slaStatus === "DUE_SOON") {
    return `即将超时 ${formatDurationMinutes(remaining)}`;
  }
  if (item.slaStatus === "NORMAL") {
    return `剩余 ${formatDurationMinutes(remaining)}`;
  }
  if (item.currentDeadline) {
    return `截止 ${formatTime(item.currentDeadline)}`;
  }
  return "已完成";
}

async function loadSection(sectionValue) {
  const normalized = normalizeSection(sectionValue);

  if (normalized === "tasks") {
    await loadTasks();
    return;
  }
  if (normalized === "sampling") {
    await loadSamplingTasks();
    return;
  }
  if (normalized === "inspections") {
    await loadInspections();
    return;
  }
  if (normalized === "rectification") {
    await loadRectifications();
    return;
  }
  if (normalized === "complaints") {
    await loadComplaints();
    return;
  }
  if (normalized === "warnings" || normalized === "stats") {
    if (normalized === "warnings") {
      await loadWarnings();
    }
    return;
  }
  await load();
}

onMounted(() => {
  loadSection(section.value);
});

watch(
  () => route.name,
  async () => {
    const normalized = resolveCurrentSection();
    if (section.value !== normalized) {
      section.value = normalized;
    }
    await loadSection(normalized);
  }
);

watch(section, (nextSection) => {
  const targetName = getSectionRouteName(nextSection);
  if (route.name !== targetName) {
    router.push({ name: targetName }).catch(() => {});
  }
});
</script>

<style scoped>
.stats-dashboard { display: grid; gap: 22px; }

.regulator-shell { grid-template-columns: 260px 1fr; }
.regulator-shell .sub-nav { display: none; }
.regulator-shell .hero-panel { padding: 48px 80px 32px; }
.regulator-shell .hero-content h1 { font-size: 34px; }
.regulator-shell .hero-content p { max-width: 720px; font-size: 16px; }
.regulator-shell .hero-highlights { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.regulator-shell .form-panel { display: block; width: 100%; padding: 24px 36px 60px; align-items: stretch; justify-content: flex-start; }
.regulator-shell .card { width: 100%; padding: 28px; }
.sub-nav { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
.sub-nav button { padding: 8px 14px; border-radius: 999px; border: 1px solid var(--stroke); background: var(--card-strong); color: var(--ink); cursor: pointer; }
.sub-nav button.active { background: var(--nav); border-color: transparent; font-weight: 600; color: #fff; }
.placeholder { border-radius: 12px; border: 1px dashed var(--stroke); padding: 16px; color: var(--muted); font-size: 13px; }
.list-row { --row-columns: 1.6fr 0.9fr 0.9fr 1fr 1.2fr 0.8fr; }
.task-header, .task-row { --row-columns: 1.2fr 1.6fr 0.8fr 0.9fr 1fr 1.2fr; }
.sampling-header, .sampling-row { --row-columns: 1.2fr 1.4fr 1.5fr 0.8fr 0.9fr 1fr 1.4fr; }
.inspection-header, .inspection-row { --row-columns: 1.6fr 1fr 0.8fr 1.2fr 0.8fr; }
.rectification-header, .rectification-row { --row-columns: 1.3fr 0.8fr 1fr 1.6fr 1fr 0.9fr; }
.complaint-header, .complaint-row { --row-columns: 1.4fr 1.4fr 0.9fr 0.9fr 1.1fr 1.2fr; }
.warning-header, .warning-row {
  --row-columns: minmax(180px, 1.4fr) minmax(200px, 1.8fr) 0.7fr 0.9fr 0.8fr 1fr 1.2fr;
}
.rectification-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rectification-sla {
  font-size: 12px;
  font-weight: 600;
}
.rectification-sla--normal { color: #0d4f9b; }
.rectification-sla--warning { color: #b36b00; }
.rectification-sla--overdue { color: var(--danger); }
.rectification-sla--none { color: var(--muted); }
.warning-quick-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.warning-quick-toggle {
  height: 34px;
  padding: 0 14px;
}
.warning-quick-toggle.active {
  color: #1f4f89;
  border-color: #c7defc;
  background: #eef6ff;
  font-weight: 600;
}
.warning-quick-tip {
  font-size: 12px;
  color: var(--muted);
}
.warning-title-cell { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.warning-status-chip {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
}
.warning-status-chip--open { color: #9b3a0a; background: #fff4eb; border-color: #f8d5bf; }
.warning-status-chip--acked { color: #1f4f89; background: #eef6ff; border-color: #c7defc; }
.warning-status-chip--processing { color: #245d62; background: #ecfbfb; border-color: #c4ebec; }
.warning-status-chip--resolved { color: #1f6b4d; background: #ebf9f1; border-color: #c6e9d6; }
.warning-status-chip--closed { color: #5a6b7f; background: #f0f4f8; border-color: #d8e1ea; }
.warning-status-chip--unknown { color: var(--muted); background: #f5f8fb; border-color: var(--stroke); }
.action-buttons { display: flex; align-items: center; gap: 8px; }
.action-buttons button { height: 32px; padding: 0 14px; min-width: 88px; white-space: nowrap; }
.section-subtitle { font-weight: 600; margin-bottom: 8px; }
.task-meta { display: flex; gap: 12px; flex-wrap: wrap; font-size: 12px; color: var(--muted); margin-bottom: 12px; }
.task-form { display: grid; gap: 12px; }
.task-items { display: grid; gap: 10px; }
.task-item { display: grid; grid-template-columns: 1.4fr 0.8fr 1.2fr auto; gap: 8px; align-items: center; }
.task-actions { display: flex; gap: 10px; justify-content: flex-end; }
.task-detail-modal { width: min(760px, 94vw); }
.warning-detail-modal {
  width: min(920px, 96vw);
}
.warning-summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
.warning-summary-item {
  border: 1px solid var(--stroke);
  border-radius: 12px;
  background: var(--card-strong);
  padding: 10px;
}
.warning-summary-item span {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}
.warning-summary-item strong { font-size: 14px; color: var(--ink); }
.warning-payload {
  margin: 0;
  max-height: 180px;
  overflow: auto;
  border: 1px solid var(--stroke);
  border-radius: 12px;
  background: var(--card-strong);
  padding: 10px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--muted);
}
.warning-detail-actions { justify-content: flex-end; gap: 10px; }
.warning-timeline-list {
  display: grid;
  gap: 8px;
  max-height: 280px;
  overflow: auto;
  padding: 10px;
  border: 1px solid var(--stroke);
  border-radius: 12px;
  background: var(--card-strong);
}
.warning-timeline-item {
  display: grid;
  grid-template-columns: 14px 1fr;
  gap: 10px;
  align-items: start;
  border-radius: 8px;
  padding: 4px 6px;
}
.warning-timeline-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 5px;
  border: 2px solid #c9d6e5;
  background: #fff;
}
.warning-timeline-dot.done {
  border-color: rgba(31, 107, 77, 0.7);
  background: rgba(31, 107, 77, 0.2);
}
.warning-timeline-dot.active {
  border-color: var(--primary);
  background: var(--primary);
}
.warning-timeline-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.warning-timeline-name {
  font-weight: 600;
  font-size: 13px;
  color: var(--ink);
}
.warning-timeline-time {
  font-size: 12px;
  color: var(--muted);
  white-space: nowrap;
}
.warning-timeline-meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--muted);
}
.warning-timeline-desc {
  margin-top: 6px;
  font-size: 13px;
  color: var(--ink);
  line-height: 1.5;
  word-break: break-word;
}
.task-detail-header { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.task-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--stroke);
  background: var(--card-strong);
  font-size: 12px;
  color: var(--muted);
}
.task-chip--status {
  border-color: rgba(27, 99, 177, 0.25);
  background: rgba(27, 99, 177, 0.1);
  color: var(--nav);
}
.task-chip--priority {
  border-color: rgba(0, 132, 91, 0.2);
  background: rgba(0, 132, 91, 0.1);
  color: #0f6c53;
}
.task-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.task-detail-section {
  border: 1px solid var(--stroke);
  border-radius: 12px;
  background: var(--card-strong);
  padding: 12px;
}
.task-detail-section-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 8px;
}
.task-detail-loading {
  font-size: 13px;
  color: var(--muted);
}
.task-detail-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.task-detail-field span {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 4px;
}
.task-detail-field strong {
  display: block;
  font-size: 14px;
  color: var(--ink);
  line-height: 1.45;
  word-break: break-word;
}
.task-detail-field--full { grid-column: 1 / -1; }
.modal-list { display: grid; gap: 10px; }
.modal-item { padding: 10px 12px; border-radius: 12px; border: 1px solid var(--stroke); background: var(--card-strong); display: grid; gap: 4px; }
.modal-item-name { font-weight: 600; font-size: 14px; }
.modal-item-meta { font-size: 12px; color: var(--muted); }
.modal-item-desc { font-size: 13px; color: var(--ink); }
.modal-empty { font-size: 12px; color: var(--muted); }
@media (max-width: 1024px) { .regulator-shell .hero-panel { padding: 36px 40px 24px; } .regulator-shell .form-panel { padding: 10px 40px 60px; } .regulator-shell .hero-highlights { grid-template-columns: 1fr; } }
@media (max-width: 960px) { .regulator-shell { grid-template-columns: 1fr; } }
@media (max-width: 820px) {
  .task-item { grid-template-columns: 1fr; }
  .task-header, .task-row, .sampling-header, .sampling-row, .inspection-header, .inspection-row, .rectification-header, .rectification-row, .complaint-header, .complaint-row, .warning-header, .warning-row { --row-columns: 1fr; }
  .task-detail-grid { grid-template-columns: 1fr; }
  .task-detail-fields { grid-template-columns: 1fr; }
  .warning-summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .warning-summary-grid { grid-template-columns: 1fr; }
}
</style>
