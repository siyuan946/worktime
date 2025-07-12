<template>
  <section class="section-card">
    <h2 class="h5">人员管理</h2>
    <h3 class="h6">已有人员</h3>
    <table class="table table-bordered table-sm table-striped mb-3">
      <thead>
        <tr>
          <th>工号</th><th>姓名</th><th>车间</th><th>班组</th><th>入厂</th><th>离厂</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="w in workers" :key="'view'+w.id">
          <td>{{ w.code }}</td>
          <td>{{ w.name }}</td>
          <td>{{ w.workshop }}</td>
          <td>{{ w.team }}</td>
          <td>{{ w.entryDate }}</td>
          <td>{{ w.leaveDate }}</td>
        </tr>
      </tbody>
    </table>
    <h3 class="h6">编辑/新增</h3>
    <table class="table table-bordered table-sm table-striped">
      <thead>
        <tr>
          <th>工号</th><th>姓名</th><th>车间</th><th>班组</th><th>入厂</th><th>离厂</th><th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="w in workers" :key="w.id">
          <td><input class="form-control form-control-sm" v-model="w.code" /></td>
          <td><input class="form-control form-control-sm" v-model="w.name" /></td>
          <td><input class="form-control form-control-sm" v-model="w.workshop" /></td>
          <td><input class="form-control form-control-sm" v-model="w.team" /></td>
          <td><input class="form-control form-control-sm" v-model="w.entryDate" type="date" /></td>
          <td><input class="form-control form-control-sm" v-model="w.leaveDate" type="date" /></td>
          <td>
            <button class="btn btn-sm btn-outline-primary" @click="updateWorker(w)">保存</button>
            <button class="btn btn-sm btn-outline-danger" @click="deleteWorker(w.id)">删除</button>
          </td>
        </tr>
        <tr>
          <td><input class="form-control form-control-sm" v-model="newWorker.code" /></td>
          <td><input class="form-control form-control-sm" v-model="newWorker.name" /></td>
          <td><input class="form-control form-control-sm" v-model="newWorker.workshop" /></td>
          <td><input class="form-control form-control-sm" v-model="newWorker.team" /></td>
          <td><input class="form-control form-control-sm" v-model="newWorker.entryDate" type="date" /></td>
          <td><input class="form-control form-control-sm" v-model="newWorker.leaveDate" type="date" /></td>
          <td><button class="btn btn-sm btn-primary" @click="createWorker" :disabled="!canAdd">新增</button></td>
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
      workers: [],
      newWorker: { code: '', name: '', workshop: '', team: '', entryDate: '', leaveDate: '' }
    }
  },
  computed: {
    canAdd() {
      return this.newWorker.code && this.newWorker.name
    }
  },
  created() {
    this.fetchWorkers()
  },
  methods: {
    async fetchWorkers() {
      const res = await axios.get('/api/workers')
      this.workers = res.data
    },
    async createWorker() {
      await axios.post('/api/workers', this.newWorker)
      this.newWorker = { code: '', name: '', workshop: '', team: '', entryDate: '', leaveDate: '' }
      this.fetchWorkers()
    },
    async updateWorker(w) {
      await axios.put(`/api/workers/${w.id}`, w)
      this.fetchWorkers()
    },
    async deleteWorker(id) {
      await axios.delete(`/api/workers/${id}`)
      this.fetchWorkers()
    }
  }
}
</script>
