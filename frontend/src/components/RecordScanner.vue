<template>
  <section class="section-card">
    <h2 class="h5">扫码录入</h2>
    <div class="input-group mb-2" style="max-width:300px;">
      <input class="form-control form-control-sm" v-model="searchBarcode" placeholder="扫码条形码" />
      <button class="btn btn-outline-secondary btn-sm" @click="searchByBarcode">查询</button>
    </div>
    <table class="table table-bordered table-sm table-striped">
      <thead>
        <tr>
          <th>ID</th>
          <th>通知单号</th>
          <th>产品名称</th>
          <th>图号</th>
          <th>工序代码</th>
          <th>工时</th>
          <th>人员代码</th>
          <th>姓名</th>
          <th>车间</th>
          <th>班组</th>
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
          <td>
            <span v-if="!rec.editing">{{ rec.workerCodes }}</span>
            <input v-else class="form-control form-control-sm" v-model="rec.workerCodes" @blur="lookupWorker(rec)" style="width:80px" />
          </td>
          <td>{{ rec.workerNames }}</td>
          <td>{{ rec.workshop }}</td>
          <td>{{ rec.team }}</td>
          <td>
            <span v-if="!rec.editing">{{ rec.qualifiedQty }}</span>
            <input v-else type="number" class="form-control form-control-sm" v-model.number="rec.qualifiedQty" @input="computeSubtotal(rec)" style="width:80px" />
          </td>
          <td>{{ rec.hourSubtotal }}</td>
          <td>
            <button class="btn btn-sm btn-outline-primary" v-if="!rec.editing" @click="rec.editing=true">编辑</button>
            <button class="btn btn-sm btn-primary" v-else @click="updateRecord(rec)">保存</button>
          </td>
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
      records: [],
      searchBarcode: ''
    }
  },
  methods: {
    async searchByBarcode() {
      const code = this.searchBarcode.trim()
      if (!code) { this.records = []; return }
      const url = `http://localhost:8080/api/workrecords/barcode/${encodeURIComponent(code)}`
      try {
        const res = await axios.get(url)
        this.records = res.data.map(r => ({ ...r, editing: false, workshop:'', team:'' }))
        for (const rec of this.records) {
          if (rec.workerCodes) await this.lookupWorker(rec)
        }
      } catch (e) {
        console.error(e)
        alert('查询失败')
      }
    },
    async updateRecord(rec) {
      await axios.put(`http://localhost:8080/api/workrecords/${rec.id}`, rec)
      rec.editing = false
      this.searchByBarcode()
    },
    computeSubtotal(row) {
      if (row.qualifiedQty != null && row.hours != null) row.hourSubtotal = row.qualifiedQty * row.hours
      else row.hourSubtotal = null
    },
    async lookupWorker(rec) {
      const codes = rec.workerCodes ? rec.workerCodes.split(/[,\u3001\s]+/) : []
      const names = []
      const workshops = new Set()
      const teams = new Set()
      for (const c of codes) {
        if (!c) continue
        try {
          const res = await axios.get(`http://localhost:8080/api/workers/code/${encodeURIComponent(c)}`)
          const w = res.data
          if (w) {
            if (w.name) names.push(w.name)
            if (w.workshop) workshops.add(w.workshop)
            if (w.team) teams.add(w.team)
          }
        } catch (e) {}
      }
      rec.workerNames = names.join(',')
      rec.workshop = Array.from(workshops).join(',')
      rec.team = Array.from(teams).join(',')
    }
  }
}
</script>
