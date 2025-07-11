<template>
  <section>
    <h2 class="h5">Excel Upload</h2>
    <div class="input-group mb-2">
      <input class="form-control" type="file" @change="onFileChange">
      <button class="btn btn-outline-primary" @click="parse" :disabled="!file">Parse</button>
      <button class="btn btn-primary" @click="save" :disabled="!preview.length">Save</button>
      <div class="spinner-border ms-2" v-if="loading"></div>
    </div>
    <div v-if="preview.length">
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
            <td><input class="form-control form-control-sm" v-model="r.workerCodes" placeholder="codes"/></td>
            <td><input class="form-control form-control-sm" v-model.number="r.qualifiedQty" @input="computeSubtotal(r)" type="number" style="width:80px"/></td>
            <td>{{ r.hourSubtotal }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script>
import axios from 'axios'
export default {
  data() {
    return {
      file: null,
      preview: [],
      loading: false
    }
  },
  methods: {
    onFileChange(e) { this.file = e.target.files[0] },
    async parse() {
      this.loading = true
      const data = new FormData()
      data.append('file', this.file)
      const res = await axios.post('http://localhost:8080/api/workrecords/parse', data, { headers: { 'Content-Type': 'multipart/form-data' } })
      this.preview = res.data.map(r => ({ ...r, workerCodes:'', qualifiedQty:null, hourSubtotal:null }))
      this.loading = false
    },
    async save() {
      this.loading = true
      await axios.post('http://localhost:8080/api/workrecords', this.preview)
      this.preview = []
      this.file = null
      this.loading = false
      this.$emit('saved')
    },
    computeSubtotal(row) {
      if (row.qualifiedQty != null && row.hours != null) row.hourSubtotal = row.qualifiedQty * row.hours
      else row.hourSubtotal = null
    }
  }
}
</script>
