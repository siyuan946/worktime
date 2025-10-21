<template>
  <aside class="issue-panel">
    <header class="issue-panel__header">
      <h3 class="issue-panel__title">缺陷追踪</h3>
      <button
        v-if="activeGroup"
        type="button"
        class="btn btn-sm btn-outline-secondary"
        @click="$emit('clear')"
      >退出筛选</button>
    </header>
    <p v-if="!groups.length" class="issue-panel__empty text-muted small mb-0">当前没有缺陷记录。</p>
    <ul v-else class="issue-panel__list list-unstyled mb-0">
      <li
        v-for="bucket in buckets"
        :key="bucket.key"
        :class="['issue-panel__item', { 'issue-panel__item--active': bucket.hasActive }]"
      >
        <div class="issue-panel__row issue-panel__row--header">
          <span class="issue-panel__drawing" :title="bucket.title">{{ bucket.title }}</span>
          <span class="issue-panel__count">共 {{ bucket.total }} 条缺陷</span>
        </div>
        <ul class="issue-panel__type-list list-unstyled mb-0">
          <li
            v-for="item in bucket.items"
            :key="item.group.key"
            class="issue-panel__type-row"
          >
            <button
              type="button"
              class="issue-panel__type-button btn btn-sm"
              :class="item.isActive ? 'btn-primary' : 'btn-outline-primary'"
              @click="$emit('select', item.group)"
            >
              缺少{{ item.group.type }}（{{ item.group.count }}）
            </button>
            <button
              v-if="bulkableTypes.includes(item.group.type)"
              type="button"
              class="btn btn-sm btn-outline-success"
              @click="$emit('bulk', item.group)"
            >批量填写</button>
          </li>
        </ul>
      </li>
    </ul>
  </aside>
</template>

<script>
export default {
  name: 'RecordIssuePanel',
  props: {
    groups: {
      type: Array,
      default: () => []
    },
    activeGroup: {
      type: Object,
      default: null
    },
    bulkableTypes: {
      type: Array,
      default: () => ['工序代码', '工序', '单件工时', '计划数']
    }
  },
  computed: {
    buckets() {
      if (!this.groups || !this.groups.length) {
        return []
      }
      const byDrawing = new Map()
      const order = []
      this.groups.forEach(group => {
        const drawingKey = group.drawingNumber || ''
        let bucket = byDrawing.get(drawingKey)
        if (!bucket) {
          bucket = {
            key: `${drawingKey}|||bucket`,
            drawingNumber: group.drawingNumber || '',
            title: group.drawingNumber || '（空图号）',
            total: 0,
            hasActive: false,
            items: []
          }
          byDrawing.set(drawingKey, bucket)
          order.push(bucket)
        }
        bucket.total += group.count
        const isActive = this.activeGroup && this.activeGroup.key === group.key
        if (isActive) {
          bucket.hasActive = true
        }
        bucket.items.push({
          group,
          isActive
        })
      })
      return order
    }
  }
}
</script>
