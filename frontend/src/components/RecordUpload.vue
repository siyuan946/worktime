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

    <div class="progress mb-2" v-if="showProgress" style="height: 0.75rem;">
      <div class="progress-bar" role="progressbar" :style="{ width: parseProgress + '%' }">
        {{ parseProgress }}%
      </div>
    </div>

    <div v-if="preview.length" id="preview-table">
      <div class="d-flex flex-column flex-lg-row justify-content-between align-items-lg-center gap-2 mb-2 no-print">
        <h2 class="h5 mb-0">预览</h2>
        <div class="d-flex flex-wrap align-items-center gap-2">
          <div>当前图号：{{ currentPageInfo?.drawingNumber || '—' }}（第 {{ currentPage + 1 }} / {{ pages.length }} 页）</div>
          <div class="input-group input-group-sm" style="width: 220px;">
            <input class="form-control" placeholder="搜索图号" v-model.trim="drawingSearch" @keyup.enter="jumpToDrawing">
            <button class="btn btn-outline-secondary" @click="jumpToDrawing">跳转</button>
          </div>
          <div class="btn-group btn-group-sm">
            <button class="btn btn-outline-secondary" @click="prevPage" :disabled="currentPage === 0">上一页</button>
            <button class="btn btn-outline-secondary" @click="nextPage" :disabled="currentPage >= pages.length - 1">下一页</button>
          </div>
        </div>
      </div>

      <!-- 保持不变：force-new-page 确保新图号必换页 -->
      <div
        v-for="(page, pageIndex) in pages"
        :key="pageIndex"
        class="preview-page"
        :class="{ 'active-page': pageIndex === currentPage, 'force-new-page': page.isFirstOfDrawing && pageIndex !== 0 }"
      >
        <div class="d-flex justify-content-between align-items-center mb-2 page-heading">
          <h3 class="h6 mb-0">图号：{{ page.drawingNumber || '（空）' }}</h3>
          <span class="text-muted">第 {{ pageIndex + 1 }} 页 / 共 {{ pages.length }} 页</span>
        </div>

        <table class="table table-bordered table-sm table-striped mb-0">
          <thead>
            <tr>
              <th class="notification-col">通知单号</th>
              <th class="no-print">产品名称</th>
              <th class="drawing-col">图号</th>
              <th class="print-only plan-col">计划数</th>
              <th class="no-print">名称</th>
              <th class="plan-col no-print">计划数</th>
              <th class="hours-col">单件工时</th>
              <th class="no-print">工序代码</th>
              <th class="process-col">工序</th>
              <th class="print-only">人员代码</th>
              <th class="print-only">合格件数</th>
              <th class="print-only">起始时间</th>
              <th class="print-only">结束时间</th>
              <th class="print-only">检验员</th>
              <th class="barcode-cell">条形码</th>
              <th class="no-print"></th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="entry in page.entries" :key="entry.index" :class="{'table-danger': entry.record.codeMissing || entry.record.hoursMissing}">
              <td class="notification-col">{{ entry.record.notificationNumber }}</td>
              <td class="no-print">{{ entry.record.productName }}</td>
              <td class="drawing-col">{{ entry.record.drawingNumber }}</td>
              <td class="print-only plan-col">{{ entry.record.planQty }}</td>
              <td class="no-print">{{ entry.record.partName }}</td>
              <td class="plan-col no-print">
                <input type="number" class="form-control form-control-sm" v-model.number="entry.record.planQty" />
                <span class="print-text">{{ entry.record.planQty }}</span>
              </td>
              <td class="hours-col">
                <input type="number" class="form-control form-control-sm no-print" style="width:80px" v-model.number="entry.record.hours" @blur="checkHours(entry.record)" />
                <span class="print-text">{{ entry.record.hours }}</span>
              </td>
              <td class="no-print">{{ entry.record.processCode }}</td>
              <td class="process-col">
                <input class="form-control form-control-sm no-print" v-model="entry.record.processName" @blur="updateProcess(entry.record)" />
                <span class="print-text">{{ entry.record.processName }}</span>
              </td>
              <td class="print-only"></td>
              <td class="print-only"></td>
              <td class="print-only"></td>
              <td class="print-only"></td>
              <td class="print-only"></td>
              <td class="barcode-cell">
                <div>{{ entry.record.barcode }}</div>
                <img v-if="entry.record.barcodeImage" :src="'data:image/png;base64,'+entry.record.barcodeImage" />
              </td>
              <td class="no-print">
                <button class="btn btn-sm btn-outline-primary me-1" @click="addRow(entry.index)">新增</button>
                <button class="btn btn-sm btn-outline-danger" @click="deleteRow(entry.index)">删除</button>
              </td>
            </tr>

            <!-- 仍然渲染补白行，但屏幕隐藏、打印显示（见 style.css） -->
            <tr v-for="n in page.blankCount" :key="'blank-'+pageIndex+'-'+n" class="blank-row">
              <td class="notification-col">&nbsp;</td>
              <td class="no-print">&nbsp;</td>
              <td class="drawing-col">&nbsp;</td>
              <td class="print-only plan-col">&nbsp;</td>
              <td class="no-print">&nbsp;</td>
              <td class="plan-col no-print">&nbsp;</td>
              <td class="hours-col">&nbsp;</td>
              <td class="no-print">&nbsp;</td>
              <td class="process-col">&nbsp;</td>
              <td class="print-only">&nbsp;</td>
              <td class="print-only">&nbsp;</td>
              <td class="print-only">&nbsp;</td>
              <td class="print-only">&nbsp;</td>
              <td class="print-only">&nbsp;</td>
              <td class="barcode-cell"><div>&nbsp;</div></td>
              <td class="no-print"></td>
            </tr>
          </tbody>
        </table>
      </div>
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
      selectedFileId: '',
      currentPage: 0,
      drawingSearch: '',
      rowsPerPage: 12,
      processCache: {},
      processCacheLoaded: false,
      barcodeCache: {},
      showProgress: false,
      parseProgress: 0
    }
  },
  created() {
    this.fetchFiles()
    this.ensureProcessCache()
  },
  computed: {
    currentFileName() {
      if (this.file) return this.file.name
      const id = this.fileId || this.selectedFileId
      const f = this.files.find(x => x.id === id)
      return f ? f.fileName : ''
    },
    pages() {
      if (!this.preview.length) return []
      // 按图号分段
      const grouped = []
      let current = null
      this.preview.forEach((record, index) => {
        const drawing = record.drawingNumber || ''
        if (!current || current.drawingNumber !== drawing) {
          current = { drawingNumber: drawing, entries: [] }
          grouped.push(current)
        }
        current.entries.push({ record, index })
      })
      // 按 rowsPerPage 分页，标记每个图号的第一页
      const pages = []
      const size = this.rowsPerPage
      grouped.forEach(group => {
        if (!group.entries.length) {
          pages.push({
            drawingNumber: group.drawingNumber,
            entries: [],
            blankCount: size,
            isFirstOfDrawing: true
          })
          return
        }
        for (let offset = 0; offset < group.entries.length; offset += size) {
          const slice = group.entries.slice(offset, offset + size)
          const blanks = size > slice.length ? size - slice.length : 0
          pages.push({
            drawingNumber: group.drawingNumber,
            entries: slice,
            blankCount: blanks,
            isFirstOfDrawing: offset === 0
          })
        }
      })
      return pages
    },
    currentPageInfo() { return this.pages[this.currentPage] || null }
  },
  watch: {
    currentPage(val) { this.$nextTick(() => this.loadBarcodesForPage(val)) }
  },
  methods: {
    onFileChange(e) { this.file = e.target.files[0] },
    async fetchFiles() { const res = await axios.get('/api/api/files'); this.files = res.data },
    async load() {
      if (!this.selectedFileId) return
      this.loading = true
      this.barcodeCache = {}
      try {
        const res = await axios.get(`/api/api/workrecords/file/${this.selectedFileId}`)
        if (!Array.isArray(res.data) || !res.data.length) {
          alert('未找到该文件的记录'); this.preview = []
        } else {
          this.preview = res.data.map(r => ({
            ...r,
            barcode: this.sanitize(r.barcode),
            barcodeImage: '',
            codeMissing: !r.processCode,
            hoursMissing: r.hours == null
          }))
        }
        this.fileId = this.selectedFileId
        this.file = null
        this.currentPage = 0
        this.$nextTick(() => { this.ensurePageInRange(); this.loadBarcodesForPage(this.currentPage) })
      } catch (e) { console.error(e); alert('加载失败') }
      this.loading = false
    },
    async remove() {
      if (!this.selectedFileId) return
      if (!confirm('删除该文件及其所有记录，确定删除?')) return
      this.loading = true
      await axios.delete(`/api/api/files/${this.selectedFileId}`)
      this.loading = false
      this.selectedFileId = ''
      this.preview = []
      this.currentPage = 0
      await this.fetchFiles()
      alert('已删除')
    },
    async parse() {
      if (this.file) {
        const dup = this.files.find(f => f.fileName === this.file.name)
        if (dup) { const cont = confirm('发现同名文件，若内容相同请勿重复上传。继续上传?'); if (!cont) return }
      }
      this.loading = true; this.showProgress = true; this.parseProgress = 5; this.barcodeCache = {}
      try {
        const data = new FormData(); data.append('file', this.file)
        const user = localStorage.getItem('username')
        const headers = user ? { 'X-User': user } : {}
        const res = await axios.post('/api/api/workrecords/parse', data, { headers })
        this.fileId = res.data.fileId
        const records = Array.isArray(res.data.records) ? res.data.records : []
        const processed = []; const total = records.length
        if (!total) this.parseProgress = 100
        for (let i = 0; i < records.length; i++) {
          const r = records[i] || {}
          processed.push({
            ...r,
            workerCodes: '',
            qualifiedQty: null,
            hourSubtotal: null,
            barcode: this.sanitize(r.barcode),
            barcodeImage: '',
            codeMissing: !!r.codeMissing,
            hoursMissing: r.hours == null
          })
          if (total) {
            const percent = Math.min(100, Math.round(((i + 1) / total) * 100))
            if (percent > this.parseProgress) this.parseProgress = percent
          }
          if ((i + 1) % 50 === 0) { await new Promise(resolve => setTimeout(resolve, 0)) }
        }
        const warn = processed.filter(r => r.codeMissing || r.hoursMissing)
        this.preview = processed
        if (warn.length) alert(`发现${warn.length}条记录缺少单件工时或工序码，请检查`)
        await this.fetchFiles()
        this.currentPage = 0
        this.$nextTick(() => { this.ensurePageInRange(); this.loadBarcodesForPage(this.currentPage) })
      } catch (e) { console.error(e); alert('解析失败'); this.showProgress = false }
      this.loading = false
      if (this.showProgress) { this.parseProgress = 100; setTimeout(() => { this.showProgress = false }, 600) }
    },
    async save() {
      if(!confirm('请再次核查数据后确认提交')) return
      this.loading = true
      await this.refreshProcesses()
      const valid = this.preview.filter(r => r.processCode && r.barcode)
      const res = await axios.post(`/api/api/workrecords?fileId=${this.fileId}`, valid)
      if (valid.length < this.preview.length) alert('部分记录因缺少工序代码或条形码已被忽略')
      const hasSupp = res.data.some(r => r.supplemental)
      this.preview = []; this.file = null; this.loading = false
      alert(hasSupp ? '保存成功，部分记录为补录，请核查。' : '保存成功')
      await this.fetchFiles(); this.$emit('saved')
    },
    async print() {
      if (!this.preview.length) return
      this.loading = true
      try {
        for (let i = 0; i < this.pages.length; i++) {
          // eslint-disable-next-line no-await-in-loop
          await this.loadBarcodesForPage(i)
        }
      } finally { this.loading = false }
      const title = document.title
      if (this.currentFileName) document.title = this.currentFileName
      await this.$nextTick()
      window.print()
      document.title = title
    },
    sanitize(text) { return text ? text.replace(/[^\x00-\x7F]/g, '') : '' },
    async ensureProcessCache(force = false) {
      if (this.processCacheLoaded && !force) return
      try {
        const res = await axios.get('/api/api/processcodes')
        const map = {}
        if (Array.isArray(res.data)) {
          for (const item of res.data) {
            if (!item || !item.name || !item.code) continue
            const name = String(item.name).trim()
            const code = String(item.code).trim()
            if (!name || !code) continue
            map[name] = code
          }
        }
        this.processCache = map; this.processCacheLoaded = true
      } catch (e) { console.error('加载工序缓存失败', e) }
    },
    async updateProcess(r, cacheReady = false) {
      if (!cacheReady) await this.ensureProcessCache()
      const rawName = r.processName || ''; const name = rawName.trim()
      if (!name) { r.processCode = ''; r.codeMissing = true; await this.updateBarcode(r); return }
      let code = this.processCache[name]
      if (!code) {
        try {
          const res = await axios.get(`/api/api/processcodes/name/${encodeURIComponent(name)}`)
          if (res.data && res.data.code) {
            code = String(res.data.code).trim()
            if (code) this.$set(this.processCache, name, code)
          }
        } catch (e) { /* ignore */ }
      }
      if (code) { r.processCode = code; r.codeMissing = false } else { r.processCode = rawName; r.codeMissing = true }
      await this.updateBarcode(r)
    },
    async checkHours(r) { r.hoursMissing = r.hours == null || r.hours === '' },
    deleteRow(index) {
      if (!confirm('确定删除该行? 删除后不可恢复')) return
      this.preview.splice(index, 1)
      this.$nextTick(() => this.ensurePageInRange())
    },
    addRow(index) {
      const base = this.preview[index]
      const blank = {
        notificationNumber: base.notificationNumber,
        productName: base.productName,
        drawingNumber: base.drawingNumber,
        partName: base.partName,
        planQty: null,
        processCode: '',
        processName: '',
        hours: null,
        workerCodes: '',
        qualifiedQty: null,
        startTime: '',
        endTime: '',
        inspector: '',
        barcode: '',
        barcodeImage: '',
        codeMissing: true,
        hoursMissing: true
      }
      this.preview.splice(index + 1, 0, blank)
      this.$nextTick(() => this.ensurePageInRange())
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
    async loadBarcodesForPage(pageIndex) {
      const page = this.pages[pageIndex]; if (!page) return
      if (!this._barcodeLoading) this._barcodeLoading = new Set()
      if (this._barcodeLoading.has(pageIndex)) return
      const missing = []
      for (const entry of page.entries) {
        const code = this.sanitize(entry.record.barcode)
        if (!code) continue
        if (this.barcodeCache[code]) {
          if (entry.record.barcodeImage !== this.barcodeCache[code]) {
            this.$set(entry.record, 'barcodeImage', this.barcodeCache[code])
          }
        } else if (!entry.record.barcodeImage) {
          missing.push(code)
        }
      }
      if (!missing.length) return
      const unique = Array.from(new Set(missing))
      this._barcodeLoading.add(pageIndex)
      try {
        const res = await axios.post('/api/api/workrecords/generateBarcodes', unique)
        const data = res && res.data ? res.data : {}
        Object.keys(data || {}).forEach(key => {
          if (!key) return
          this.$set(this.barcodeCache, key, data[key])
        })
        for (const entry of page.entries) {
          const code = this.sanitize(entry.record.barcode)
          if (code && this.barcodeCache[code]) {
            this.$set(entry.record, 'barcodeImage', this.barcodeCache[code])
          }
        }
      } catch (e) { console.error('加载条码失败', e) }
      finally { this._barcodeLoading.delete(pageIndex) }
    },
    async refreshProcesses() {
      await this.ensureProcessCache()
      for (const r of this.preview) { // 串行
        // eslint-disable-next-line no-await-in-loop
        await this.updateProcess(r, true)
      }
    },
    async updateBarcode(r) {
      if (r.drawingNumber && r.notificationNumber && r.processCode) {
        const bar = `${r.drawingNumber}-${r.notificationNumber}-${r.processCode}`
        const clean = this.sanitize(bar)
        r.barcode = clean
        if (!clean) { r.barcodeImage = ''; return }
        if (this.barcodeCache[clean]) { r.barcodeImage = this.barcodeCache[clean]; return }
        try {
          const res = await axios.get('/api/api/workrecords/generateBarcode', { params: { text: bar } })
          if (res && res.data) { this.$set(this.barcodeCache, clean, res.data); r.barcodeImage = res.data }
          else { r.barcodeImage = '' }
        } catch (e) { console.error('获取条码失败', e); r.barcodeImage = '' }
      } else { r.barcode = ''; r.barcodeImage = '' }
    },
    prevPage() { if (this.currentPage > 0) this.currentPage -= 1 },
    nextPage() { if (this.currentPage < this.pages.length - 1) this.currentPage += 1 },
    jumpToDrawing() {
      const term = (this.drawingSearch || '').trim().toLowerCase()
      if (!term) return
      const index = this.pages.findIndex(p => (p.drawingNumber || '').toLowerCase().includes(term))
      if (index >= 0) this.currentPage = index
      else alert('未找到对应图号')
    },
    ensurePageInRange() {
      if (!this.pages.length) { this.currentPage = 0; return }
      if (this.currentPage >= this.pages.length) this.currentPage = this.pages.length - 1
      if (this.currentPage < 0) this.currentPage = 0
    }
  }
}
</script>
