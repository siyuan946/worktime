<template>
  <div class="container py-4">
    <h1 class="mb-4 text-center">Work Time Entry</h1>
    <record-upload @saved="reload"></record-upload>
    <record-list ref="recordList" class="mt-4"></record-list>
    <worker-manager class="mt-4"></worker-manager>
    <process-manager class="mt-4"></process-manager>
  </div>
</template>

<script>
import RecordUpload from './components/RecordUpload.vue'
import RecordList from './components/RecordList.vue'
import WorkerManager from './components/WorkerManager.vue'
import ProcessManager from './components/ProcessManager.vue'

export default {
  components: { RecordUpload, RecordList, WorkerManager, ProcessManager },
  methods: {
    reload() { this.$refs.recordList.fetch() }
  }
}
</script>

<style>
.table input { margin: 2px; }
</style>
