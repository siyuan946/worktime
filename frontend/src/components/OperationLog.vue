<template>
  <section class="section-card">
    <h2 class="h5">操作记录</h2>
    <form class="log-filter" @submit.prevent="applyFilters">
      <div class="log-filter__row">
        <label class="log-filter__field">
          <span>用户</span>
          <input v-model.trim="filters.username" type="text" class="form-control form-control-sm" placeholder="用户名" />
        </label>
        <label class="log-filter__field">
          <span>关键字</span>
          <input v-model.trim="filters.keyword" type="text" class="form-control form-control-sm" placeholder="动作或详情关键词" />
        </label>
        <label class="log-filter__field">
          <span>开始日期</span>
          <input v-model="filters.start" type="date" class="form-control form-control-sm" />
        </label>
        <label class="log-filter__field">
          <span>结束日期</span>
          <input v-model="filters.end" type="date" class="form-control form-control-sm" />
        </label>
        <label class="log-filter__field log-filter__field--size">
          <span>每页条数</span>
          <select v-model.number="size" class="form-select form-select-sm">
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </label>
      </div>
      <div class="log-filter__actions">
        <div class="log-filter__buttons">
          <button type="submit" class="btn btn-primary btn-sm" :disabled="loading">筛选</button>
          <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="loading" @click="resetFilters">重置</button>
          <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="loading" @click="refresh">刷新</button>
        </div>
        <label class="log-filter__auto">
          <input type="checkbox" v-model="autoRefresh" /> 自动刷新
        </label>
      </div>
    </form>
    <div class="log-pagination">
      <div>共 {{ total }} 条记录，当前第 {{ page + 1 }} / {{ pageCount || 1 }} 页</div>
      <div class="log-pagination__actions">
        <button class="btn btn-outline-secondary btn-sm" :disabled="loading || page === 0" @click="changePage(page - 1)">上一页</button>
        <button class="btn btn-outline-secondary btn-sm" :disabled="loading || page + 1 >= pageCount" @click="changePage(page + 1)">下一页</button>
      </div>
    </div>
    <table class="table table-bordered table-sm table-striped">
      <thead>
        <tr><th>时间</th><th>操作者</th><th>操作</th></tr>
      </thead>
      <tbody>
        <template v-for="log in logs" :key="log.id">
          <tr @click="toggleDetails(log)" style="cursor:pointer">
            <td>{{ formatTimestamp(log.timestamp) }}</td>
            <td>{{ log.username }}</td>
            <td class="wrap-text">{{ log.action }}</td>
          </tr>
          <tr v-if="log.show" :key="log.id + '-details'">
            <td colspan="3" class="pre-wrap">
              <div class="log-details__header">
                <span>详细信息</span>
                <button class="btn btn-outline-secondary btn-sm" @click.stop="copyDetails(log)">复制详情</button>
              </div>
              <pre v-if="log.details" class="log-details__body">{{ log.details }}</pre>
              <div v-if="!log.details" class="text-muted">暂无详情</div>
              <div v-if="log.sub && log.sub.length" class="log-details__children">
                <div class="log-details__child" v-for="s in log.sub" :key="s.id">
                  <div class="log-details__child-time">{{ formatTimestamp(s.timestamp) }}</div>
                  <div class="log-details__child-text">{{ s.action }}</div>
                </div>
              </div>
            </td>
          </tr>
        </template>
        <tr v-if="!logs.length && !loading">
          <td colspan="3" class="text-center text-muted">暂无符合条件的记录</td>
        </tr>
        <tr v-if="loading">
          <td colspan="3" class="text-center text-muted">加载中...</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script>
import axios from 'axios'

export default {
  data() {
    return {
      logs: [],
      total: 0,
      page: 0,
      size: 20,
      pageCount: 0,
      loading: false,
      autoRefresh: false,
      refreshTimer: null,
      filters: {
        username: '',
        keyword: '',
        start: '',
        end: ''
      }
    }
  },
  created() {
    this.fetchLogs()
  },
  watch: {
    size() {
      this.fetchLogs(true)
    },
    autoRefresh(val) {
      this.setupAutoRefresh(val)
    }
  },
  beforeDestroy() {
    this.setupAutoRefresh(false)
  },
  methods: {
    async fetchLogs(resetPage = false) {
      if (resetPage) this.page = 0
      this.loading = true
      const user = localStorage.getItem('username')
      const headers = user ? { 'X-User': user } : {}
      const params = new URLSearchParams()
      params.set('page', this.page)
      params.set('size', this.size)
      if (this.filters.username) params.set('username', this.filters.username)
      if (this.filters.keyword) params.set('keyword', this.filters.keyword)
      if (this.filters.start) params.set('startDate', this.filters.start)
      if (this.filters.end) params.set('endDate', this.filters.end)
      try {
        const res = await axios.get('/api/logs', { headers, params })
        const payload = res.data || {}
        const content = payload.content || []
        this.total = payload.totalElements ?? content.length
        this.pageCount = payload.totalPages ?? (content.length ? 1 : 0)
        if (this.pageCount > 0 && this.page >= this.pageCount) {
          this.page = this.pageCount - 1
          return this.fetchLogs()
        }
        this.logs = this.groupLogs(content)
      } finally {
        this.loading = false
      }
    },
    groupLogs(raw) {
      const grouped = []
      let current = null
      for (const log of raw) {
        log.show = false
        log.sub = log.sub || []
        const isApi = /^(GET|POST|PUT|DELETE|PATCH) /.test(log.action) && !log.details
        if (isApi && current) {
          current.sub.push(log)
        } else {
          grouped.push(log)
          current = log
        }
      }
      return grouped
    },
    formatTimestamp(value) {
      if (!value) return ''
      return value.replace('T', ' ').slice(0, 19)
    },
    toggleDetails(log) {
      log.show = !log.show
    },
    changePage(next) {
      if (next < 0 || (this.pageCount && next >= this.pageCount)) return
      this.page = next
      this.fetchLogs()
    },
    applyFilters() {
      this.fetchLogs(true)
    },
    resetFilters() {
      this.filters = { username: '', keyword: '', start: '', end: '' }
      this.fetchLogs(true)
    },
    refresh() {
      this.fetchLogs()
    },
    setupAutoRefresh(enable) {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer)
        this.refreshTimer = null
      }
      if (enable) {
        this.refreshTimer = setInterval(() => {
          if (!this.loading) {
            this.fetchLogs()
          }
        }, 60000)
      }
    },
    async copyDetails(log) {
      const text = [log.action, log.details].filter(Boolean).join('\n')
      if (!text) return
      try {
        await navigator.clipboard.writeText(text)
        if (this.$bvToast) {
          this.$bvToast.toast('已复制到剪贴板', { variant: 'success', autoHideDelay: 1500 })
        }
      } catch (e) {
        const textarea = document.createElement('textarea')
        textarea.value = text
        textarea.style.position = 'fixed'
        textarea.style.opacity = '0'
        document.body.appendChild(textarea)
        textarea.focus()
        textarea.select()
        document.execCommand('copy')
        document.body.removeChild(textarea)
      }
    }
  }
}
</script>

<style>
.pre-wrap { white-space: pre-wrap; }
.log-filter {
  display: flex;
  flex-direction: column;
  gap: .75rem;
  margin-bottom: 1rem;
}
.log-filter__row {
  display: flex;
  flex-wrap: wrap;
  gap: .75rem;
}
.log-filter__field {
  display: flex;
  flex-direction: column;
  gap: .25rem;
  min-width: 140px;
}
.log-filter__field--size {
  width: 120px;
}
.log-filter__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}
.log-filter__buttons {
  display: flex;
  gap: .5rem;
}
.log-filter__auto {
  display: flex;
  align-items: center;
  gap: .25rem;
  font-size: .875rem;
  color: #6c757d;
}
.log-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: .5rem;
  font-size: .875rem;
}
.log-pagination__actions {
  display: flex;
  gap: .5rem;
}
.log-details__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: .5rem;
}
.log-details__body {
  background: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: .25rem;
  padding: .5rem .75rem;
  white-space: pre-wrap;
}
.log-details__children {
  margin-top: .5rem;
  border-top: 1px dashed #dee2e6;
  padding-top: .5rem;
  display: grid;
  gap: .35rem;
}
.log-details__child {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: .75rem;
  font-size: .875rem;
}
.log-details__child-time {
  color: #6c757d;
}
.log-details__child-text {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
