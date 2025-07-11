<template>
  <div style="max-width: 800px; margin: auto;">
    <h1>Work Time Entry</h1>
    <section style="margin-bottom:20px;">
      <h2>Excel Upload</h2>
      <input type="file" @change="onFileChange">
      <button @click="parse" :disabled="!file">Parse</button>
      <button @click="save" :disabled="preview.length===0">Save</button>
    </section>

    <section v-if="preview.length">
      <h2>Preview</h2>
      <table border="1" cellpadding="4" cellspacing="0" style="width:100%;">
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
            <th>人员代码</th>
            <th>合格数</th>
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
            <td><input v-model="r.workerCodes" placeholder="工号,空格分隔"/></td>
            <td><input v-model.number="r.qualifiedQty" type="number" style="width:60px"/></td>
            <td><input v-model="r.startTime" type="datetime-local"/></td>
            <td><input v-model="r.endTime" type="datetime-local"/></td>
            <td><input v-model="r.inspector" /></td>
            <td><input v-model="r.remark1" /></td>
            <td><input v-model="r.remark2" /></td>
          </tr>
        </tbody>
      </table>
    </section>

    <section>
      <h2>Saved Records</h2>
      <table border="1" cellpadding="4" cellspacing="0" style="width:100%;">
        <thead>
          <tr>
            <th>ID</th>
            <th>通知单号</th>
            <th>产品名称</th>
            <th>图号</th>
            <th>工序代码</th>
            <th>工时</th>
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
          </tr>
        </tbody>
      </table>
    </section>

    <section>
      <h2>Workers</h2>
      <table border="1" cellpadding="4" cellspacing="0" style="width:100%;">
        <thead>
          <tr>
            <th>工号</th><th>姓名</th><th>车间</th><th>班组</th><th>入厂</th><th>离厂</th><th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="w in workers" :key="w.id">
            <td><input v-model="w.code" /></td>
            <td><input v-model="w.name" /></td>
            <td><input v-model="w.workshop" /></td>
            <td><input v-model="w.team" /></td>
            <td><input v-model="w.entryDate" type="date" /></td>
            <td><input v-model="w.leaveDate" type="date" /></td>
            <td>
              <button @click="updateWorker(w)">Save</button>
              <button @click="deleteWorker(w.id)">Del</button>
            </td>
          </tr>
          <tr>
            <td><input v-model="newWorker.code" /></td>
            <td><input v-model="newWorker.name" /></td>
            <td><input v-model="newWorker.workshop" /></td>
            <td><input v-model="newWorker.team" /></td>
            <td><input v-model="newWorker.entryDate" type="date" /></td>
            <td><input v-model="newWorker.leaveDate" type="date" /></td>
            <td><button @click="createWorker">Add</button></td>
          </tr>
        </tbody>
      </table>
    </section>

    <section>
      <h2>Process Codes</h2>
      <table border="1" cellpadding="4" cellspacing="0" style="width:100%;">
        <thead>
          <tr>
            <th>代号</th><th>工序名称</th><th>大类</th><th>内容</th><th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in processCodes" :key="p.id">
            <td><input v-model="p.code" /></td>
            <td><input v-model="p.name" /></td>
            <td><input v-model="p.category" /></td>
            <td><input v-model="p.content" /></td>
            <td>
              <button @click="updateProcess(p)">Save</button>
              <button @click="deleteProcess(p.id)">Del</button>
            </td>
          </tr>
          <tr>
            <td><input v-model="newProcess.code" /></td>
            <td><input v-model="newProcess.name" /></td>
            <td><input v-model="newProcess.category" /></td>
            <td><input v-model="newProcess.content" /></td>
            <td><button @click="createProcess">Add</button></td>
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
        startTime: '',
        endTime: '',
        inspector: '',
        remark1: '',
        remark2: ''
      }))
    },
    async save() {
      await axios.post('http://localhost:8080/api/workrecords', this.preview)
      this.preview = []
      this.file = null
      this.fetch()
    },
    async fetch() {
      const res = await axios.get('http://localhost:8080/api/workrecords')
      this.records = res.data
    },

    async fetchWorkers() {
      const res = await axios.get('http://localhost:8080/api/workers')
      this.workers = res.data
    },

    async createWorker() {
      await axios.post('http://localhost:8080/api/workers', this.newWorker)
      this.newWorker = { code: '', name: '', workshop: '', team: '', entryDate: '', leaveDate: '' }
      this.fetchWorkers()
    },

    async updateWorker(w) {
      await axios.put(`http://localhost:8080/api/workers/${w.id}`, w)
      this.fetchWorkers()
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

<style>
input { margin: 4px; }
</style>
