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
  </div>
</template>

<script>
import axios from 'axios'
export default {
  data() {
    return {
      file: null,
      preview: [],
      records: []
    }
  },
  created() {
    this.fetch()
  },
  methods: {
    onFileChange(e) { this.file = e.target.files[0] },
    async parse() {
      const data = new FormData()
      data.append('file', this.file)
      const res = await axios.post('http://localhost:8080/api/workrecords/parse', data, { headers: { 'Content-Type': 'multipart/form-data' } })
      this.preview = res.data
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
    }
  }
}
</script>

<style>
input { margin: 4px; }
</style>
