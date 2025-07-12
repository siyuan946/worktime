<template>
  <section class="section-card">
    <h2 class="h5">已保存记录</h2>
    <div class="input-group mb-2" style="max-width:300px;">
      <input class="form-control form-control-sm" v-model="searchBarcode" placeholder="扫码条形码" />
      <button class="btn btn-outline-secondary btn-sm" @click="searchByBarcode">查询</button>
      <button class="btn btn-outline-secondary btn-sm" @click="fetch">全部</button>
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
  created() {
    this.fetch()
  },
  methods: {
    async fetch() {
      const res = await axios.get('http://localhost:8080/api/workrecords')
      this.records = res.data.map(r => ({...r, editing:false}))
    },
    async searchByBarcode() {
      if (!this.searchBarcode) { this.fetch(); return }
      const res = await axios.get(`http://localhost:8080/api/workrecords/barcode/${this.searchBarcode}`)
      this.records = res.data.map(r => ({...r, editing:false}))
    },
    async updateRecord(rec) {
      await axios.put(`http://localhost:8080/api/workrecords/${rec.id}`, rec)
      rec.editing = false
      this.fetch()
    },
    computeSubtotal(row) {
      if (row.qualifiedQty != null && row.hours != null) row.hourSubtotal = row.qualifiedQty * row.hours
      else row.hourSubtotal = null
    }
  }
}
</script>
