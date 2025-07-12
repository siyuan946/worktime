<template>
  <section>
    <h2 class="h5">工序代码管理</h2>
    <table class="table table-bordered table-sm">
      <thead>
        <tr>
          <th>代号</th><th>工序名称</th><th>大类</th><th>内容</th><th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in processCodes" :key="p.id">
          <td><input class="form-control form-control-sm" v-model="p.code" /></td>
          <td><input class="form-control form-control-sm" v-model="p.name" /></td>
          <td><input class="form-control form-control-sm" v-model="p.category" /></td>
          <td><input class="form-control form-control-sm" v-model="p.content" /></td>
          <td>
            <button class="btn btn-sm btn-outline-primary" @click="updateProcess(p)">保存</button>
            <button class="btn btn-sm btn-outline-danger" @click="deleteProcess(p.id)">删除</button>
          </td>
        </tr>
        <tr>
          <td><input class="form-control form-control-sm" v-model="newProcess.code" /></td>
          <td><input class="form-control form-control-sm" v-model="newProcess.name" /></td>
          <td><input class="form-control form-control-sm" v-model="newProcess.category" /></td>
          <td><input class="form-control form-control-sm" v-model="newProcess.content" /></td>
          <td><button class="btn btn-sm btn-primary" @click="createProcess" :disabled="!canAdd">新增</button></td>
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
      processCodes: [],
      newProcess: { code: '', name: '', category: '', content: '' }
    }
  },
  computed: {
    canAdd() {
      return this.newProcess.code && this.newProcess.name
    }
  },
  created() {
    this.fetchProcesses()
  },
  methods: {
    async fetchProcesses() {
      const res = await axios.get('http://localhost:8080/api/processcodes')
      this.processCodes = res.data
    },
    async createProcess() {
      await axios.post('http://localhost:8080/api/processcodes', this.newProcess)
      this.newProcess = { code: '', name: '', category: '', content: '' }
      this.fetchProcesses()
    },
    async updateProcess(p) {
      await axios.put(`http://localhost:8080/api/processcodes/${p.id}`, p)
      this.fetchProcesses()
    },
    async deleteProcess(id) {
      await axios.delete(`http://localhost:8080/api/processcodes/${id}`)
      this.fetchProcesses()
    }
  }
}
</script>
