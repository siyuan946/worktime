<template>
  <teleport to="body">
    <div
      v-if="visible"
      class="modal fade show d-block bulk-issue-modal"
      tabindex="-1"
      role="dialog"
      aria-modal="true"
    >
      <div class="modal-dialog modal-lg modal-dialog-scrollable" role="document">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">批量处理：{{ title }}</h5>
            <button type="button" class="btn-close" @click="$emit('close')" aria-label="关闭"></button>
          </div>
          <div class="modal-body">
            <p class="text-muted small mb-3">
              一次性快速填写当前筛选出的记录，可逐条调整后统一保存。
            </p>
            <div class="table-responsive">
              <table class="table table-sm bulk-issue-modal__table align-middle mb-0">
                <thead>
                  <tr>
                    <th style="min-width: 120px;">通知单号</th>
                    <th style="min-width: 140px;">图号</th>
                    <th style="min-width: 140px;">工序</th>
                    <th v-if="type === '工序代码'" style="min-width: 140px;">工序代码</th>
                    <th v-else-if="type === '单件工时'" style="min-width: 120px;">单件工时</th>
                    <th v-else-if="type === '工序'" style="min-width: 140px;">工序名称</th>
                    <th v-else-if="type === '计划数'" style="min-width: 120px;">计划数</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in drafts" :key="item.index">
                    <td>{{ item.notificationNumber || '—' }}</td>
                    <td>{{ item.drawingNumber || '—' }}</td>
                    <td>{{ item.processName || '—' }}</td>
                    <td v-if="type === '工序代码'">
                      <input
                        type="text"
                        class="form-control form-control-sm"
                        v-model.trim="item.processCode"
                        placeholder="填写工序代码"
                      >
                    </td>
                    <td v-else-if="type === '单件工时'">
                      <input
                        type="number"
                        step="0.0001"
                        class="form-control form-control-sm"
                        v-model.number="item.hours"
                        placeholder="输入工时"
                      >
                    </td>
                    <td v-else-if="type === '工序'">
                      <input
                        type="text"
                        class="form-control form-control-sm"
                        v-model.trim="item.processName"
                        placeholder="输入工序名称"
                      >
                    </td>
                    <td v-else-if="type === '计划数'">
                      <input
                        type="number"
                        class="form-control form-control-sm"
                        v-model.number="item.planQty"
                        placeholder="输入计划数"
                      >
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-outline-secondary" @click="$emit('close')">取消</button>
            <button type="button" class="btn btn-primary" @click="submit">应用修改</button>
          </div>
        </div>
      </div>
    </div>
    <div v-if="visible" class="modal-backdrop fade show"></div>
  </teleport>
</template>

<script>
export default {
  name: 'BulkIssueModal',
  props: {
    visible: { type: Boolean, default: false },
    type: { type: String, default: '' },
    group: { type: Object, default: null },
    records: { type: Array, default: () => [] }
  },
  data() {
    return { drafts: [] }
  },
  computed: {
    title() {
      const drawing = this.group && this.group.drawingNumber
      const drawingLabel = drawing ? `图号「${drawing}」` : '无图号'
      return `${drawingLabel}缺少「${this.type}」`
    }
  },
  watch: {
    visible: {
      handler(val) {
        if (val) {
          this.initialiseDrafts()
        } else {
          this.drafts = []
        }
      },
      immediate: true
    },
    records: {
      handler() {
        if (this.visible) this.initialiseDrafts()
      },
      deep: true
    }
  },
  methods: {
    initialiseDrafts() {
      const payload = Array.isArray(this.records) ? this.records : []
      this.drafts = payload.map(item => ({
        index: item.index,
        notificationNumber: item.notificationNumber,
        drawingNumber: item.drawingNumber,
        processName: item.processName,
        processCode: item.processCode,
        hours: item.hours,
        planQty: item.planQty
      }))
    },
    submit() {
      const updates = this.drafts.map(item => ({ ...item }))
      this.$emit('apply', { type: this.type, updates })
    }
  }
}
</script>
