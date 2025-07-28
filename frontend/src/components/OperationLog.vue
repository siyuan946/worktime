<template>
  <section class="section-card">
    <h2 class="h5">操作记录</h2>
    <table class="table table-bordered table-sm table-striped">
      <thead>
        <tr><th>时间</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="log in logs" :key="log.id">
          <td>{{ log.timestamp.replace('T',' ').slice(0,19) }}</td>
          <td class="wrap-text">{{ log.action }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script>
import axios from 'axios'
export default {
  data() {
    return { logs: [] }
  },
  created() { this.fetchLogs() },
  methods: {
    async fetchLogs() {
      const user = localStorage.getItem('username')
      if (!user) {
        this.logs = []
        return
      }
      const res = await axios.get('http://localhost:8080/api/logs', {
        headers: { 'X-User': user }
      })
      this.logs = res.data
    }
  }
}
</script>
