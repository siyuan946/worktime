<template>
<section class="section-card">
    <h2 class="h5">Excel上传</h2>
    <div class="input-group mb-2">
      <input class="form-control" type="file" @change="onFileChange">
      <button class="btn btn-outline-primary" @click="parse" :disabled="!file">解析</button>
      <select class="form-select" style="max-width:180px" v-model="selectedFileId">
        <option value="" disabled>选择历史文件</option>
        <option v-for="f in files" :key="f.id" :value="f.id">{{ f.fileName }} ({{ f.uploadTime ? f.uploadTime.slice(0,10) : '' }})</option>
      </select>
      <button class="btn btn-outline-secondary" @click="load" :disabled="!selectedFileId">加载</button>
      <button class="btn btn-outline-danger" @click="remove" :disabled="!selectedFileId">删除</button>
      <button class="btn btn-outline-warning" @click="deleteZero" :disabled="!preview.length">清除0工序</button>
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
            <th class="no-print">产品名称</th>
            <th class="drawing-col">图号</th>
            <th class="no-print">名称</th>
            <th>计划数</th>
            <th>工序代码</th>
            <th class="process-col">工序</th>
            <th class="hours-col">工时</th>
            <th class="print-only">人员代码</th>
            <th class="print-only">合格件数</th>
            <th class="print-only">起始时间</th>
            <th class="print-only">结束时间</th>
            <th class="print-only">检验员</th>
            <th>条形码</th>
            <th class="no-print"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(r,i) in preview" :key="i" :class="{'table-danger': r.codeMissing || r.hoursMissing}">
            <td>{{ r.notificationNumber }}</td>
            <td class="no-print">{{ r.productName }}</td>
            <td class="drawing-col">{{ r.drawingNumber }}</td>
            <td class="no-print">{{ r.partName }}</td>
            <td>{{ r.planQty }}</td>
            <td>{{ r.processCode }}</td>
            <td class="process-col"><input class="form-control form-control-sm" v-model="r.processName" @blur="updateProcess(r)"/></td>
            <td class="hours-col"><input type="number" class="form-control form-control-sm" v-model.number="r.hours" @blur="checkHours(r)" style="width:80px"/></td>
            <td class="print-only"></td>
            <td class="print-only"></td>
            <td class="print-only"></td>
            <td class="print-only"></td>
            <td class="print-only"></td>
            <td class="barcode-cell">
              <div>{{ r.barcode }}</div>
              <img v-if="r.barcodeImage" :src="'data:image/png;base64,'+r.barcodeImage" />
            </td>
            <td class="no-print"><button class="btn btn-sm btn-outline-danger" @click="deleteRow(i)">删除</button></td>
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
      fileId: null,
      files: [],
      selectedFileId: ''
    }
  },
  created() {
    this.fetchFiles()
  },
  methods: {
    onFileChange(e) { this.file = e.target.files[0] },
    async fetchFiles() {
      const res = await axios.get('http://localhost:8080/api/files')
      this.files = res.data
    },
    async load() {
      if (!this.selectedFileId) return
      this.loading = true
      try {
        const res = await axios.get(
          `http://localhost:8080/api/workrecords/file/${this.selectedFileId}`
        )
        if (!Array.isArray(res.data) || !res.data.length) {
          alert('未找到该文件的记录')
          this.preview = []
        } else {
          this.preview = res.data.map(r => ({
            ...r,
            codeMissing: false,
            hoursMissing: r.hours == null
          }))
          await this.refreshProcesses()
        }
        this.fileId = this.selectedFileId
        this.file = null
      } catch (e) {
        console.error(e)
        alert('加载失败')
      }
      this.loading = false
    },
    async remove() {
      if (!this.selectedFileId) return
      if (!confirm('删除该文件及其所有记录，确定删除?')) return
      this.loading = true
      await axios.delete(`http://localhost:8080/api/files/${this.selectedFileId}`)
      this.loading = false
      this.selectedFileId = ''
      this.preview = []
      await this.fetchFiles()
      alert('已删除')
    },
    async parse() {
      if (this.file) {
        const dup = this.files.find(f => f.fileName === this.file.name)
        if (dup) {
          const cont = confirm('发现同名文件，若内容相同请勿重复上传。继续上传?')
          if (!cont) return
        }
      }
      this.loading = true
      const data = new FormData()
      data.append('file', this.file)
      const res = await axios.post('http://localhost:8080/api/workrecords/parse', data, { headers: { 'Content-Type': 'multipart/form-data' } })
      this.fileId = res.data.fileId
      this.preview = res.data.records.map(r => ({ ...r, workerCodes:'', qualifiedQty:null, hourSubtotal:null }))
      const warn = this.preview.filter(r => r.codeMissing || r.hoursMissing)
      if (warn.length) alert(`发现${warn.length}条记录缺少工时或工序码，请检查`)
      await this.fetchFiles()
      this.loading = false
    },
    async save() {
      if(!confirm('请再次核查数据后确认提交')) return
      this.loading = true
      await this.refreshProcesses()
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
      await this.fetchFiles()
      this.$emit('saved')
    },
    print() {
      window.print()
    },
    sanitize(text) {
      return text ? text.replace(/[^\x00-\x7F]/g, '') : ''
    },
    async updateProcess(r) {
      if (!r.processName) {
        r.processCode = ''
        r.codeMissing = true
        await this.updateBarcode(r)
        return
      }
      try {
        const res = await axios.get(`http://localhost:8080/api/processcodes/name/${encodeURIComponent(r.processName)}`)
        if (res.data) {
          r.processCode = res.data.code
          r.codeMissing = false
        } else {
          r.processCode = r.processName
          r.codeMissing = true
        }
      } catch (e) {
        r.processCode = r.processName
        r.codeMissing = true
      }
      await this.updateBarcode(r)
    },
    async checkHours(r) {
      r.hoursMissing = r.hours == null || r.hours === ''
    },
    deleteRow(index) {
      if (confirm('确定删除该行? 删除后不可恢复')) {
        this.preview.splice(index, 1)
      }
    },
    deleteZero() {
      if (!this.preview.length) return
      if (!confirm('确定删除所有工序为0的行?')) return
      const before = this.preview.length
      this.preview = this.preview.filter(r => {
        const code = r.processCode != null ? String(r.processCode).trim() : ''
        return code !== '0'
      })
      const removed = before - this.preview.length
      alert(`已删除${removed}行`)
    },
    async refreshProcesses() {
      for (const r of this.preview) {
        await this.updateProcess(r)
      }
    },
    async updateBarcode(r) {
      if (r.drawingNumber && r.notificationNumber && r.processCode) {
        const bar = `${r.drawingNumber}-${r.notificationNumber}-${r.processCode}`
        const res = await axios.get('http://localhost:8080/api/workrecords/generateBarcode', { params: { text: bar } })
        r.barcode = this.sanitize(bar)
        r.barcodeImage = res.data
      }
    }
  }
}
</script>
