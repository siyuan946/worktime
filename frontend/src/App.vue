<template>
  <div>
    <login-page v-if="!loggedIn" @logged-in="onLogin"></login-page>
    <div v-else class="container py-4">
      <h1 class="mb-4 text-center">工时录入</h1>
      <record-upload @saved="reload"></record-upload>
      <record-list ref="recordList" class="mt-4"></record-list>
      <worker-manager class="mt-4"></worker-manager>
      <process-manager class="mt-4"></process-manager>
    </div>
  </div>
</template>

<script>
import RecordUpload from './components/RecordUpload.vue'
import RecordList from './components/RecordList.vue'
import WorkerManager from './components/WorkerManager.vue'
import ProcessManager from './components/ProcessManager.vue'
import LoginPage from './components/LoginPage.vue'

export default {
  components: { RecordUpload, RecordList, WorkerManager, ProcessManager, LoginPage },
  data() {
    return { loggedIn: false }
  },
  methods: {
    reload() { this.$refs.recordList.fetch() },
    onLogin() { this.loggedIn = true }
  }
}
</script>

<style>
.table input { margin: 2px; }
</style>
