<template>
<section class="section-card">
    <h2 class="h5">Excel上传</h2>
    <div class="input-group mb-2">
      <input class="form-control" type="file" @change="onFileChange">
      <button class="btn btn-outline-primary" @click="parse" :disabled="!file">解析</button>
      <button class="btn btn-primary" @click="save" :disabled="!preview.length">保存</button>
      <button class="btn btn-secondary" @click="print" :disabled="!preview.length">打印</button>
      <div class="spinner-border ms-2" v-if="loading"></div>
    </div>
    <div v-if="preview.length" id="preview-table">
      <h2 class="h5">预览</h2>
      <table class="table table-bordered table-sm table-striped">
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
            <td class="barcode-cell">
              <div>{{ r.barcode }}</div>
              <img v-if="r.barcodeImage" :src="'data:image/png;base64,'+r.barcodeImage" />
            </td>
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
      loading: false,
      fileId: null
    }
  },
  methods: {
    onFileChange(e) { this.file = e.target.files[0] },
    async parse() {
      this.loading = true
      const data = new FormData()
      data.append('file', this.file)
      const res = await axios.post('http://localhost:8080/api/workrecords/parse', data, { headers: { 'Content-Type': 'multipart/form-data' } })
      this.fileId = res.data.fileId
      this.preview = res.data.records.map(r => ({ ...r, workerCodes:'', qualifiedQty:null, hourSubtotal:null }))
      this.loading = false
    },
    async save() {
      if(!confirm('请再次核查数据后确认提交')) return
      this.loading = true
      const valid = this.preview.filter(r => r.processCode && r.barcode)
      const res = await axios.post(`http://localhost:8080/api/workrecords?fileId=${this.fileId}`, valid)
      if (valid.length < this.preview.length) {
        alert('部分记录因缺少工序代码或条形码已被忽略')
      }
      const hasSupp = res.data.some(r => r.supplemental)
      this.preview = []
      this.file = null
      this.loading = false
      alert(hasSupp ? '保存成功，部分记录为补录，请核查。' : '保存成功')
      this.$emit('saved')
    },
    print() {
      window.print()
    }
  }
}
</script>
