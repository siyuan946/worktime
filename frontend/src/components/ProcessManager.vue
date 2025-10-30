<template>
  <section class="section-card">
    <h2 class="h5">工序代码管理</h2>
    <div class="mb-2 text-end">
      <button class="btn btn-sm btn-primary" @click="openModal">新增工序代码</button>
    </div>
    <div class="row mb-2">
      <div class="col-sm-4">
        <input class="form-control form-control-sm" v-model="search" placeholder="搜索代号或名称">
      </div>
    </div>
    <table class="table table-bordered table-sm table-striped">
      <thead>
        <tr>
          <th>代号</th><th>工序名称</th><th>大类</th><th>内容</th><th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in filteredProcesses" :key="p.id">
          <td><input class="form-control form-control-sm" v-model="p.code" /></td>
          <td><input class="form-control form-control-sm" v-model="p.name" /></td>
          <td><input class="form-control form-control-sm" v-model="p.category" /></td>
          <td><input class="form-control form-control-sm" v-model="p.content" /></td>
          <td>
            <button class="btn btn-sm btn-outline-primary" @click="updateProcess(p)">保存</button>
            <button class="btn btn-sm btn-outline-danger" @click="deleteProcess(p.id)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
    <!-- 新增工序代码模态框 -->
    <div
      class="modal fade show"
      tabindex="-1"
      role="dialog"
      v-if="showAddModal"
      style="display: block"
      aria-modal="true"
      @click.self="closeModal"
    >
      <div class="modal-dialog modal-dialog-centered" role="document">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">新增工序代码</h5>
            <button type="button" class="btn-close" @click="closeModal"></button>
          </div>
          <div class="modal-body">
            <div class="mb-2"><input class="form-control form-control-sm" v-model="newProcess.code" placeholder="代号" ref="codeInput" /></div>
            <div class="mb-2"><input class="form-control form-control-sm" v-model="newProcess.name" placeholder="工序名称" /></div>
            <div class="mb-2"><input class="form-control form-control-sm" v-model="newProcess.category" placeholder="大类" /></div>
            <div><input class="form-control form-control-sm" v-model="newProcess.content" placeholder="内容" /></div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" @click="closeModal">取消</button>
            <button class="btn btn-primary" @click="createProcess" :disabled="!canAdd">保存</button>
          </div>
        </div>
      </div>
    </div>
    <div v-if="showAddModal" class="modal-backdrop fade show"></div>
  </section>
</template>

<script>
import axios from 'axios'
export default {
  data() {
    return {
      processCodes: [],
      search: '',
      newProcess: { code: '', name: '', category: '', content: '' },
      showAddModal: false
    }
  },
  computed: {
    canAdd() {
      return this.newProcess.code && this.newProcess.name
    },
    filteredProcesses() {
      return this.processCodes
    }
  },
  watch: {
    search(val) {
      this.fetchProcesses(val)
    },
    showAddModal(val) {
      if (typeof document !== 'undefined' && document.body) {
        if (val) {
          document.body.classList.add('modal-open')
        } else {
          this.$nextTick(() => {
            const remaining = document.querySelectorAll('.modal.fade.show')
            if (!remaining.length) {
              document.body.classList.remove('modal-open')
            }
          })
        }
      }
    }
  },
  created() {
    this.fetchProcesses('')
  },
  methods: {
    openModal() {
      this.newProcess = { code: '', name: '', category: '', content: '' }
      this.showAddModal = true
      this.$nextTick(() => {
        const input = this.$refs.codeInput
        if (input && typeof input.focus === 'function') {
          input.focus()
        }
      })
    },
    closeModal() {
      this.showAddModal = false
    },
    async fetchProcesses(term) {
      const url = term
        ? `/api/processcodes/search?term=${encodeURIComponent(term)}`
        : '/api/processcodes'
      try {
        const res = await axios.get(url)
        this.processCodes = res.data
      } catch (error) {
        this.showError(error, '加载工序代码失败')
      }
    },
    async createProcess() {
      try {
        await axios.post('/api/processcodes', this.newProcess)
        this.closeModal()
        this.fetchProcesses(this.search)
      } catch (error) {
        this.showError(error, '新增工序代码失败')
      }
    },
    async updateProcess(p) {
      try {
        await axios.put(`/api/processcodes/${p.id}`, p)
        this.fetchProcesses(this.search)
      } catch (error) {
        this.showError(error, '更新工序代码失败')
      }
    },
    async deleteProcess(id) {
      try {
        await axios.delete(`/api/processcodes/${id}`)
        this.fetchProcesses(this.search)
      } catch (error) {
        this.showError(error, '删除工序代码失败')
      }
    },
    showError(error, fallback) {
      console.error(error)
      const response = error && error.response
      let message = null
      if (response && response.data != null) {
        const data = response.data
        if (typeof data === 'string') {
          message = data
        } else if (typeof data === 'object') {
          message = data.message || data.error || data.detail || null
          if (!message && Array.isArray(data.errors) && data.errors.length) {
            const first = data.errors[0]
            message = typeof first === 'string' ? first : (first && first.message) || null
          }
        }
      }
      if (!message && response && response.status === 409) {
        const url = response.config && response.config.url ? String(response.config.url) : ''
        if (url.includes('/api/processcodes')) {
          message = '工序代码已存在，请使用其他代号'
        } else {
          message = '请求冲突，请检查数据后重试'
        }
      }
      if (!message && response && typeof response.statusText === 'string' && response.statusText) {
        message = response.statusText
      }
      if (!message && error && typeof error.message === 'string') {
        message = error.message
      }
      alert(message || fallback || '操作失败')
    }
  },
  beforeUnmount() {
    if (typeof document !== 'undefined' && document.body) {
      this.$nextTick(() => {
        const remaining = document.querySelectorAll('.modal.fade.show')
        if (!remaining.length) {
          document.body.classList.remove('modal-open')
        }
      })
    }
  }
}
</script>
