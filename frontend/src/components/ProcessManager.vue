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
    <div v-if="showModal">
      <div class="modal-backdrop fade show"></div>
      <div class="modal fade show d-block" tabindex="-1" role="dialog" aria-modal="true" @click.self="closeModal">
        <div class="modal-dialog modal-dialog-centered" role="document">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">新增工序代码</h5>
              <button type="button" class="btn-close" @click="closeModal"></button>
            </div>
            <div class="modal-body">
              <div class="mb-2"><input class="form-control form-control-sm" v-model="newProcess.code" placeholder="代号" /></div>
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
    </div>
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
      showModal: false
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
    }
  },
  created() {
    this.fetchProcesses('')
  },
  beforeDestroy() {
    if (typeof document !== 'undefined') {
      document.body.classList.remove('modal-open')
    }
  },
  methods: {
    openModal() {
      this.newProcess = { code: '', name: '', category: '', content: '' }
      this.showModal = true
      if (typeof document !== 'undefined') {
        document.body.classList.add('modal-open')
      }
    },
    closeModal() {
      this.showModal = false
      if (typeof document !== 'undefined') {
        document.body.classList.remove('modal-open')
      }
    },
    async fetchProcesses(term) {
      const url = term
        ? `http://localhost:8080/api/processcodes/search?term=${encodeURIComponent(term)}`
        : 'http://localhost:8080/api/processcodes'
      const res = await axios.get(url)
      this.processCodes = res.data
    },
    async createProcess() {
      await axios.post('http://localhost:8080/api/processcodes', this.newProcess)
      this.closeModal()
      this.fetchProcesses(this.search)
    },
    async updateProcess(p) {
      await axios.put(`http://localhost:8080/api/processcodes/${p.id}`, p)
      this.fetchProcesses(this.search)
    },
    async deleteProcess(id) {
      await axios.delete(`http://localhost:8080/api/processcodes/${id}`)
      this.fetchProcesses(this.search)
    }
  }
}
</script>

<style scoped>
.modal-backdrop {
  z-index: 1050;
}
.modal {
  z-index: 1055;
}
</style>
