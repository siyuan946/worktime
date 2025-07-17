<template>
  <section class="section-card">
    <h2 class="h5">扫码录入</h2>
    <div class="input-group mb-2" style="max-width:300px;">
      <input class="form-control form-control-sm" v-model="searchBarcode" placeholder="扫码条形码" />
      <button class="btn btn-outline-secondary btn-sm" @click="searchByBarcode">查询</button>
    </div>
    <div class="input-group mb-2" style="max-width:400px;">
      <select class="form-select form-select-sm" v-model="selectedFileId">
        <option value="" disabled>选择文件查看全部</option>
        <option v-for="f in files" :key="f.id" :value="f.id">{{ f.fileName }}</option>
      </select>
      <button class="btn btn-outline-secondary btn-sm" @click="loadFile" :disabled="!selectedFileId">加载</button>
      <button class="btn btn-outline-primary btn-sm" @click="exportFile" :disabled="!records.length">导出</button>
    </div>
    <div class="mb-2" v-if="records.length">
      产量: {{ planQty }} | 总合格数: {{ totalQualified }} | 已填写 {{ records.length }} 条
      <button v-if="!viewOnly" class="btn btn-sm btn-outline-secondary ms-2" @click="addRecord">新增记录</button>
    </div>
    <table class="table table-bordered table-sm table-striped">
      <thead>
        <tr>
          <th>ID</th>
          <th>通知单号</th>
          <th>产品名称</th>
          <th>图号</th>
          <th>批次号</th>
          <th>工序代码</th>
          <th>工时</th>
          <th>产量</th>
          <th>人员代码</th>
          <th>数量分配</th>
          <th>姓名</th>
          <th>车间</th>
          <th>班组</th>
          <th>合格数</th>
          <th>工时小计</th>
          <th>工时分配</th>
          <th v-if="!viewOnly"></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="rec in records" :key="rec.id">
          <td>{{ rec.id }}</td>
          <td>{{ rec.notificationNumber }}</td>
          <td>{{ rec.productName }}</td>
          <td>{{ rec.drawingNumber }}</td>
          <td>
            <span v-if="!rec.editing">{{ rec.batchNumber }}</span>
            <input v-else class="form-control form-control-sm" v-model="rec.batchNumber" style="width:80px" />
          </td>
          <td>{{ rec.processCode }}</td>
          <td>{{ rec.hours }}</td>
          <td>{{ rec.planQty }}</td>
          <td>
            <span v-if="!rec.editing">{{ rec.workerCodes }}</span>
            <input v-else class="form-control form-control-sm" v-model="rec.workerCodes" @blur="onWorkerCodesChange(rec)" style="width:80px" />
          </td>
          <td>
            <span v-if="!rec.editing">{{ rec.workerQtys }}</span>
            <input v-else class="form-control form-control-sm" v-model="rec.workerQtys" @input="computeWorkerHours(rec)" style="width:80px" />
          </td>
          <td>{{ rec.workerNames }}</td>
          <td>{{ rec.workshop }}</td>
          <td>{{ rec.team }}</td>
          <td>
            <span v-if="!rec.editing">{{ rec.qualifiedQty }}</span>
            <input v-else type="number" class="form-control form-control-sm" v-model.number="rec.qualifiedQty" @input="onQtyChange(rec)" style="width:80px" />
          </td>
          <td>{{ rec.hourSubtotal }}</td>
          <td>{{ rec.workerHours }}</td>
          <td v-if="!viewOnly">
            <template v-if="!rec.editing">
              <button class="btn btn-sm btn-outline-primary me-1" @click="rec.editing=true">编辑</button>
              <button class="btn btn-sm btn-outline-danger" @click="deleteRecord(rec)">删除</button>
            </template>
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
      searchBarcode: '',
      files: [],
      selectedFileId: '',
      viewOnly: false
    }
  },
  created() {
    this.fetchFiles()
  },
  computed: {
    planQty() {
      return this.records.length ? this.records[0].planQty : null
    },
    totalQualified() {
      return this.records.reduce((sum, r) => sum + (r.qualifiedQty || 0), 0)
    }
  },
  methods: {
    async fetchFiles() {
      const res = await axios.get('http://localhost:8080/api/files')
      this.files = res.data
    },
    async loadFile() {
      if (!this.selectedFileId) return
      try {
        const res = await axios.get(
          `http://localhost:8080/api/workrecords/file/${this.selectedFileId}/filled`
        )
        if (!Array.isArray(res.data) || !res.data.length) {
          alert('该文件暂无填写记录')
          this.records = []
        } else {
          await this.processRecords(res.data)
          this.viewOnly = true
        }
      } catch (e) {
        console.error(e)
        alert('加载失败')
      }
    },
    async exportFile() {
      if (!this.selectedFileId || !this.records.length) return
      const res = await axios.get(
        `http://localhost:8080/api/workrecords/file/${this.selectedFileId}/export`,
        { responseType: 'blob' }
      )
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const a = document.createElement('a')
      a.href = url
      a.download = 'records.xlsx'
      a.click()
      window.URL.revokeObjectURL(url)
    },
    async searchByBarcode() {
      const code = this.searchBarcode.trim()
      if (!code) { this.records = []; return }
      const url = `http://localhost:8080/api/workrecords/barcode/${encodeURIComponent(code)}`
      try {
        const res = await axios.get(url)
        await this.processRecords(res.data)
        this.viewOnly = false
      } catch (e) {
        console.error(e)
        alert('查询失败')
      }
    },
    async updateRecord(rec) {
      const total = this.records.reduce((sum, r) => sum + (r === rec ? (rec.qualifiedQty || 0) : (r.qualifiedQty || 0)), 0)
      if (this.planQty != null && total > this.planQty) {
        alert('总合格数已超过产量，请确认')
      }
      await axios.put(`http://localhost:8080/api/workrecords/${rec.id}`, rec)
      rec.editing = false
      this.searchByBarcode()
    },
    async addRecord() {
      if (this.records.some(r => r.editing)) {
        alert('请先保存当前编辑的记录后再新增')
        return
      }
      if (!this.records.length) return
      const id = this.records[0].id
      const res = await axios.post(`http://localhost:8080/api/workrecords/duplicate/${id}`)
      const rec = { ...res.data, editing: false, workshop:'', team:'', workerQtys:'', workerHours:'' }
      if (rec.qualifiedQty != null && rec.hours != null) {
        rec.hourSubtotal = rec.qualifiedQty * rec.hours
      }
      if (rec.workerCodes) await this.lookupWorker(rec)
      this.computeWorkerHours(rec)
      this.records.push(rec)
    },
    async deleteRecord(rec) {
      if (!confirm('确定删除这条记录?')) return
      const idx = this.records.indexOf(rec)
      if (idx !== -1) this.records.splice(idx, 1)
    },
    async processRecords(list) {
      this.records = list.map(r => ({ ...r, editing: false, workshop:'', team:'', workerQtys:'', workerHours:'' }))
      for (const rec of this.records) {
        if (rec.qualifiedQty != null && rec.hours != null) {
          rec.hourSubtotal = rec.qualifiedQty * rec.hours
        }
        if (rec.workerCodes) await this.lookupWorker(rec)
        this.computeWorkerHours(rec)
      }
    },
    computeSubtotal(row) {
      if (row.qualifiedQty != null && row.hours != null) row.hourSubtotal = row.qualifiedQty * row.hours
      else row.hourSubtotal = null
      const total = this.records.reduce((sum, r) => sum + (r.qualifiedQty || 0), 0)
      if (this.planQty != null && total > this.planQty) {
        alert('总合格数已超过产量，请确认')
      }
    },
    onQtyChange(rec) {
      this.computeSubtotal(rec)
      this.computeWorkerHours(rec)
    },
    onWorkerCodesChange(rec) {
      this.lookupWorker(rec)
      this.computeWorkerHours(rec)
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
          } else {
            alert(`未找到人员 ${c}`)
          }
        } catch (e) {
          console.error(e)
          alert(`未找到人员 ${c}`)
        }
      }
      rec.workerNames = names.join(',')
      rec.workshop = Array.from(workshops).join(',')
      rec.team = Array.from(teams).join(',')
    },
    computeWorkerHours(rec) {
      const codes = rec.workerCodes ? rec.workerCodes.trim().split(/[ ,\u3001]+/) : []
      if (!codes.length || rec.hours == null || rec.qualifiedQty == null) {
        rec.workerHours = ''
        if (!rec.workerQtys) rec.workerQtys = ''
        return
      }
      let qtys = []
      if (rec.workerQtys && rec.workerQtys.trim()) {
        qtys = rec.workerQtys.trim().split(/\s+/).map(n => parseFloat(n))
      }
      if (qtys.length !== codes.length) {
        const share = rec.qualifiedQty / codes.length
        qtys = codes.map(() => share)
        rec.workerQtys = qtys.join(' ')
      }
      rec.workerHours = codes.map((c,i)=> `${c}:${(qtys[i]||0)*rec.hours}` ).join(',')
    }
  }
}
</script>
