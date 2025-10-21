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
        v-for="group in groups"
        :key="group.key"
        :class="['issue-panel__item', { 'issue-panel__item--active': activeGroup && activeGroup.key === group.key }]"
      >
        <div class="issue-panel__row">
          <span class="issue-panel__drawing">{{ group.drawingNumber || '（空图号）' }}</span>
          <span class="badge bg-danger-subtle text-danger">缺少{{ group.type }}</span>
        </div>
        <div class="issue-panel__row issue-panel__row--footer">
          <span class="issue-panel__count">{{ group.count }} 条</span>
          <div class="btn-group btn-group-sm">
            <button type="button" class="btn btn-outline-primary" @click="$emit('select', group)">定位</button>
            <button
              v-if="bulkableTypes.includes(group.type)"
              type="button"
              class="btn btn-outline-success"
              @click="$emit('bulk', group)"
            >批量填写</button>
          </div>
        </div>
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
  }
}
</script>
