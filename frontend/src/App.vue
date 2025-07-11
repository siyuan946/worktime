<template>
  <div class="container py-4">
    <h1 class="mb-4 text-center">Work Time Entry</h1>
    <section class="mb-4">
      <h2 class="h5">Excel Upload</h2>
      <div class="input-group mb-2">
        <input class="form-control" type="file" @change="onFileChange">
        <button class="btn btn-outline-primary" @click="parse" :disabled="!file">Parse</button>
        <button class="btn btn-primary" @click="save" :disabled="preview.length===0">Save</button>
      </div>
    </section>

    <section v-if="preview.length">
      <h2 class="h5">Preview</h2>
      <table class="table table-bordered table-sm">
        <thead>
          <tr>
            <th>通知单号</th>
            <th>产品名称</th>
            <th>图号</th>
            <th>名称</th>
            <th>计划数</th>
            <th>工序代码</th>
            <th>工序</th>
            <th>工时</th>
            <th>条形码</th>
            <th>人员代码</th>
            <th>合格数</th>
            <th>工时小计</th>
            <th>开始时间</th>
            <th>结束时间</th>
            <th>检验员</th>
            <th>备注1</th>
            <th>备注2</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(r,i) in preview" :key="i">
            <td>{{ r.notificationNumber }}</td>
            <td>{{ r.productName }}</td>
            <td>{{ r.drawingNumber }}</td>
            <td>{{ r.partName }}</td>
            <td>{{ r.planQty }}</td>
            <td>{{ r.processCode }}</td>
            <td>{{ r.processName }}</td>
            <td>{{ r.hours }}</td>
            <td>{{ r.barcode }}</td>
            <td><input class="form-control form-control-sm" v-model="r.workerCodes" placeholder="工号,空格分隔"/></td>
            <td><input class="form-control form-control-sm" v-model.number="r.qualifiedQty" @input="computeSubtotal(r)" type="number" style="width:80px"/></td>
            <td>{{ r.hourSubtotal }}</td>
            <td><input class="form-control form-control-sm" v-model="r.startTime" type="datetime-local"/></td>
            <td><input class="form-control form-control-sm" v-model="r.endTime" type="datetime-local"/></td>
            <td><input class="form-control form-control-sm" v-model="r.inspector" /></td>
            <td><input class="form-control form-control-sm" v-model="r.remark1" /></td>
            <td><input class="form-control form-control-sm" v-model="r.remark2" /></td>
          </tr>
        </tbody>
      </table>
    </section>

    <section>
      <h2 class="h5">Saved Records</h2>
      <div class="input-group mb-2" style="max-width:300px;">
        <input class="form-control form-control-sm" v-model="searchBarcode" placeholder="Search barcode" />
        <button class="btn btn-outline-secondary btn-sm" @click="searchByBarcode">Search</button>
        <button class="btn btn-outline-secondary btn-sm" @click="fetch">All</button>
      </div>
      <table class="table table-bordered table-sm">
        <thead>
          <tr>
            <th>ID</th>
            <th>通知单号</th>
            <th>产品名称</th>
            <th>图号</th>
            <th>工序代码</th>
            <th>工时</th>
            <th>条形码</th>
            <th>人员代码</th>
            <th>合格数</th>
            <th>工时小计</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="rec in records" :key="rec.id">
            <td>{{ rec.id }}</td>
            <td>{{ rec.notificationNumber }}</td>
            <td>{{ rec.productName }}</td>
            <td>{{ rec.drawingNumber }}</td>
            <td>{{ rec.processCode }}</td>
            <td>{{ rec.hours }}</td>
            <td>{{ rec.barcode }}</td>
            <td>
              <span v-if="!rec.editing">{{ rec.workerCodes }}</span>
              <input v-else class="form-control form-control-sm" v-model="rec.workerCodes" style="width:80px" />
            </td>
            <td>
              <span v-if="!rec.editing">{{ rec.qualifiedQty }}</span>
              <input v-else type="number" class="form-control form-control-sm" v-model.number="rec.qualifiedQty" @input="computeSubtotal(rec)" style="width:80px" />
            </td>
            <td>{{ rec.hourSubtotal }}</td>
            <td>
              <button class="btn btn-sm btn-outline-primary" v-if="!rec.editing" @click="rec.editing=true">Edit</button>
              <button class="btn btn-sm btn-primary" v-else @click="updateRecord(rec)">Save</button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <section>
      <h2 class="h5">Workers</h2>
      <table class="table table-bordered table-sm">
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
              <button class="btn btn-sm btn-outline-primary" @click="updateWorker(w)">Save</button>
              <button class="btn btn-sm btn-outline-danger" @click="deleteWorker(w.id)">Del</button>
            </td>
          </tr>
          <tr>
            <td><input class="form-control form-control-sm" v-model="newWorker.code" /></td>
            <td><input class="form-control form-control-sm" v-model="newWorker.name" /></td>
            <td><input class="form-control form-control-sm" v-model="newWorker.workshop" /></td>
            <td><input class="form-control form-control-sm" v-model="newWorker.team" /></td>
            <td><input class="form-control form-control-sm" v-model="newWorker.entryDate" type="date" /></td>
            <td><input class="form-control form-control-sm" v-model="newWorker.leaveDate" type="date" /></td>
            <td><button class="btn btn-sm btn-primary" @click="createWorker">Add</button></td>
          </tr>
        </tbody>
      </table>
    </section>

    <section>
      <h2 class="h5">Process Codes</h2>
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
              <button class="btn btn-sm btn-outline-primary" @click="updateProcess(p)">Save</button>
              <button class="btn btn-sm btn-outline-danger" @click="deleteProcess(p.id)">Del</button>
            </td>
          </tr>
          <tr>
            <td><input class="form-control form-control-sm" v-model="newProcess.code" /></td>
            <td><input class="form-control form-control-sm" v-model="newProcess.name" /></td>
            <td><input class="form-control form-control-sm" v-model="newProcess.category" /></td>
            <td><input class="form-control form-control-sm" v-model="newProcess.content" /></td>
            <td><button class="btn btn-sm btn-primary" @click="createProcess">Add</button></td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script>
import axios from 'axios'
export default {
  data() {
    return {
      file: null,
      preview: [],
      records: [],
      searchBarcode: '',
      workers: [],
      newWorker: { code: '', name: '', workshop: '', team: '', entryDate: '', leaveDate: '' },
      processCodes: [],
      newProcess: { code: '', name: '', category: '', content: '' }
    }
  },
  created() {
    this.fetch()
    this.fetchWorkers()
    this.fetchProcesses()
  },
  methods: {
    onFileChange(e) { this.file = e.target.files[0] },
    async parse() {
      const data = new FormData()
      data.append('file', this.file)
      const res = await axios.post('http://localhost:8080/api/workrecords/parse', data, { headers: { 'Content-Type': 'multipart/form-data' } })
      this.preview = res.data.map(r => ({
        ...r,
        workerCodes: '',
        qualifiedQty: null,
        hourSubtotal: null,
        startTime: '',
        endTime: '',
        inspector: '',
        remark1: '',
        remark2: ''
      }))
    },
    async save() {
      try {
        await axios.post('http://localhost:8080/api/workrecords', this.preview)
        this.preview = []
        this.file = null
        this.fetch()
      } catch (e) {
        alert(e.response?.data?.message || 'Save failed')
      }
    },
    async fetch() {
      const res = await axios.get('http://localhost:8080/api/workrecords')
      this.records = res.data.map(r => ({...r, editing:false}))
    },

    async fetchWorkers() {
      const res = await axios.get('http://localhost:8080/api/workers')
      this.workers = res.data
    },

    async createWorker() {
      try {
        await axios.post('http://localhost:8080/api/workers', this.newWorker)
        this.newWorker = { code: '', name: '', workshop: '', team: '', entryDate: '', leaveDate: '' }
        this.fetchWorkers()
      } catch (e) {
        alert(e.response?.data?.message || 'Create worker failed')
      }
    },

    async updateWorker(w) {
      try {
        await axios.put(`http://localhost:8080/api/workers/${w.id}`, w)
        this.fetchWorkers()
      } catch (e) {
        alert(e.response?.data?.message || 'Update worker failed')
      }
    },

    async deleteWorker(id) {
      await axios.delete(`http://localhost:8080/api/workers/${id}`)
      this.fetchWorkers()
    },

    async fetchProcesses() {
      const res = await axios.get('http://localhost:8080/api/processcodes')
      this.processCodes = res.data
    },

    async createProcess() {
      try {
        await axios.post('http://localhost:8080/api/processcodes', this.newProcess)
        this.newProcess = { code: '', name: '', category: '', content: '' }
        this.fetchProcesses()
      } catch (e) {
        alert(e.response?.data?.message || 'Create process failed')
      }
    },

    async updateProcess(p) {
      try {
        await axios.put(`http://localhost:8080/api/processcodes/${p.id}`, p)
        this.fetchProcesses()
      } catch (e) {
        alert(e.response?.data?.message || 'Update process failed')
      }
    },

    async deleteProcess(id) {
      await axios.delete(`http://localhost:8080/api/processcodes/${id}`)
      this.fetchProcesses()
    },

    async searchByBarcode() {
      if (!this.searchBarcode) {
        this.fetch();
        return;
      }
      const res = await axios.get(`http://localhost:8080/api/workrecords/barcode/${this.searchBarcode}`)
      this.records = res.data.map(r => ({...r, editing:false}))
    },

    async updateRecord(rec) {
      try {
        await axios.put(`http://localhost:8080/api/workrecords/${rec.id}`, rec)
        rec.editing = false
        this.fetch()
      } catch (e) {
        alert(e.response?.data?.message || 'Update failed')
      }
    },

    computeSubtotal(row) {
      if (row.qualifiedQty != null && row.hours != null) {
        row.hourSubtotal = row.qualifiedQty * row.hours
      } else {
        row.hourSubtotal = null
      }
    }
  }
}
</script>

<style>
.table input {
  margin: 2px;
}
</style>
