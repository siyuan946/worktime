<template>
  <section class="section-card">
    <h2 class="h5 no-print">Excel上传</h2>
    <div class="input-group mb-2 no-print">
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

    <div class="progress mb-2 no-print" v-if="showProgress" style="height: 0.75rem;">
      <div class="progress-bar" role="progressbar" :style="{ width: parseProgress + '%' }">
        {{ parseProgress }}%
      </div>
    </div>

    <div v-if="preview.length" id="preview-table" class="upload-preview-layout">
      <RecordIssuePanel
        class="no-print issue-panel-container"
        :groups="issueGroups"
        :active-group="issueFilter"
        @select="selectIssueGroup"
        @clear="clearIssueFilter"
        @bulk="handleBulkFill"
      />
      <div class="preview-main">
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

        <div v-if="issueFilter" class="alert alert-warning py-2 px-3 mb-3 no-print issue-filter-alert">
          <div>
            正在处理图号「{{ issueFilter.drawingNumber || '（空图号）' }}」缺少「{{ issueFilter.type }}」的 {{ issueFilter.indexes.length }} 条记录。
          </div>
          <button type="button" class="btn btn-sm btn-outline-secondary" @click="clearIssueFilter">退出筛选</button>
        </div>

        <!-- 保持不变：force-new-page 确保新图号必换页 -->
        <template v-for="item in visiblePages" :key="item.index">
          <div
            class="preview-page"
            :class="{ 'active-page': item.index === currentPage, 'force-new-page': item.page.isFirstOfDrawing && item.index !== 0 }"
          >
            <div class="d-flex justify-content-between align-items-center mb-2 page-heading">
              <h3 class="h6 mb-0">图号：{{ item.page.drawingNumber || '（空）' }}</h3>
              <span class="text-muted">第 {{ item.index + 1 }} 页 / 共 {{ pages.length }} 页</span>
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
                  <th class="print-only worker-code-col">人员代码</th>
                  <th class="print-only qualified-col">合格件数</th>
                  <th class="print-only time-col">起始时间</th>
                  <th class="print-only time-col">结束时间</th>
                  <th class="print-only inspector-col">检验员</th>
                  <th class="barcode-cell">条形码</th>
                  <th class="no-print"></th>
                </tr>
              </thead>

              <tbody>
                <tr
                  v-for="entry in item.page.entries"
                  :key="entry.index"
                  :class="{ 'table-danger': entry.record.hasIssue }"
                  :title="entry.record.issueSummary || ''"
                >
                  <td :class="['notification-col', { 'missing-cell': entry.record.notificationMissing }]"><span>{{ entry.record.notificationNumber }}</span></td>
                  <td :class="['no-print', { 'missing-cell': entry.record.productMissing }]">{{ entry.record.productName }}</td>
                  <td :class="['drawing-col', { 'missing-cell': entry.record.drawingMissing }]">{{ entry.record.drawingNumber }}</td>
                  <td class="print-only plan-col">{{ entry.record.planQty }}</td>
                  <td :class="['no-print', { 'missing-cell': entry.record.partMissing }]">{{ entry.record.partName }}</td>
                  <td :class="['plan-col', 'no-print', { 'missing-cell': entry.record.planMissing }]">
                    <input
                      type="number"
                      class="form-control form-control-sm"
                      :class="{ 'is-invalid': entry.record.planMissing }"
                      v-model.number="entry.record.planQty"
                      @input="handlePlanInput(entry.record)"
                      @blur="handlePlanInput(entry.record)"
                    />
                    <span class="print-text">{{ entry.record.planQty }}</span>
                  </td>
                  <td :class="['hours-col', { 'missing-cell': entry.record.hoursMissing }]">
                    <input
                      type="number"
                      class="form-control form-control-sm no-print"
                      style="width:80px"
                      v-model.number="entry.record.hours"
                      :class="{ 'is-invalid': entry.record.hoursMissing }"
                      @input="checkHours(entry.record)"
                      @blur="checkHours(entry.record)"
                    />
                    <span class="print-text">{{ entry.record.hours }}</span>
                  </td>
                  <td :class="['no-print', { 'missing-cell': entry.record.codeMissing }]">{{ entry.record.processCode }}</td>
                  <td :class="['process-col', { 'missing-cell': entry.record.processMissing || entry.record.codeMissing }]">
                    <input
                      class="form-control form-control-sm no-print"
                      v-model="entry.record.processName"
                      @blur="updateProcess(entry.record)"
                      @input="handleProcessInput(entry.record)"
                      :class="{ 'is-invalid': entry.record.processMissing || entry.record.codeMissing }"
                    />
                    <span class="print-text">{{ entry.record.processName }}</span>
                  </td>
                  <td class="print-only worker-code-col"></td>
                  <td class="print-only qualified-col"></td>
                  <td class="print-only time-col"></td>
                  <td class="print-only time-col"></td>
                  <td class="print-only inspector-col"></td>
                  <td :class="['barcode-cell', { 'missing-cell': entry.record.barcodeMissing }]">
                    <div>{{ entry.record.barcode }}</div>
                    <img v-if="entry.record.barcodeImage" :src="'data:image/png;base64,'+entry.record.barcodeImage" />
                  </td>
                  <td class="no-print">
                    <div v-if="entry.record.issueSummary" class="issue-tag mb-1">⚠️ {{ entry.record.issueSummary }}</div>
                    <button class="btn btn-sm btn-outline-primary me-1" @click="addRow(entry.index)">新增</button>
                    <button class="btn btn-sm btn-outline-danger" @click="deleteRow(entry.index)">删除</button>
                  </td>
                </tr>

              </tbody>
            </table>
          </div>
        </template>
      </div>
    </div>
  </section>
</template>

<script>
import axios from 'axios'
import RecordIssuePanel from './RecordIssuePanel.vue'
export default {
  components: { RecordIssuePanel },
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
      parseProgress: 0,
      renderAllPages: false,
      renderBuffer: 0,
      issueFilter: null,
      issueCompletionNotified: false
    }
  },
  created() {
    this.fetchFiles()
    this.ensureProcessCache()
  },
  mounted() {
    window.addEventListener('afterprint', this.handleAfterPrint)
  },
  beforeDestroy() {
    window.removeEventListener('afterprint', this.handleAfterPrint)
  },
  computed: {
    currentFileName() {
      if (this.file) return this.file.name
      const id = this.fileId || this.selectedFileId
      const f = this.files.find(x => x.id === id)
      return f ? f.fileName : ''
    },
    allPages() {
      if (!this.preview.length) return []
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
      const pages = []
      const size = this.rowsPerPage
      grouped.forEach(group => {
        if (!group.entries.length) {
          pages.push({
            drawingNumber: group.drawingNumber,
            entries: [],
            isFirstOfDrawing: true
          })
          return
        }
        for (let offset = 0; offset < group.entries.length; offset += size) {
          const slice = group.entries.slice(offset, offset + size)
          pages.push({
            drawingNumber: group.drawingNumber,
            entries: slice,
            isFirstOfDrawing: offset === 0
          })
        }
      })
      return pages
    },
    pages() {
      const base = this.allPages
      if (!this.issueFilter) return base
      const filterSet = new Set(this.issueFilter.indexes)
      const filtered = []
      let lastDrawing = null
      base.forEach(page => {
        const entries = page.entries.filter(entry => filterSet.has(entry.index))
        if (!entries.length) return
        const drawing = page.drawingNumber
        const isFirst = drawing !== lastDrawing
        filtered.push({
          drawingNumber: drawing,
          entries,
          isFirstOfDrawing: isFirst
        })
        lastDrawing = drawing
      })
      return filtered
    },
    currentPageInfo() { return this.pages[this.currentPage] || null },
    visiblePages() {
      if (!this.pages.length) return []
      if (this.renderAllPages) {
        return this.pages.map((page, index) => ({ page, index }))
      }
      const indices = new Set()
      indices.add(this.currentPage)
      if (this.renderBuffer > 0) {
        for (let offset = 1; offset <= this.renderBuffer; offset += 1) {
          const prev = this.currentPage - offset
          const next = this.currentPage + offset
          if (prev >= 0) indices.add(prev)
          if (next < this.pages.length) indices.add(next)
        }
      }
      return Array.from(indices)
        .filter(index => index >= 0 && index < this.pages.length)
        .sort((a, b) => a - b)
        .map(index => ({ page: this.pages[index], index }))
    },
    issueGroups() {
      if (!this.preview.length) return []
      const groups = new Map()
      this.preview.forEach((record, index) => {
        if (!record || !Array.isArray(record.issueTypes) || !record.issueTypes.length) return
        const drawing = record.drawingNumber || ''
        record.issueTypes.forEach(type => {
          const key = `${drawing}|||${type}`
          if (!groups.has(key)) {
            groups.set(key, {
              key,
              drawingNumber: drawing,
              type,
              count: 0,
              indexes: []
            })
          }
          const group = groups.get(key)
          group.count += 1
          group.indexes.push(index)
        })
      })
      return Array.from(groups.values()).sort((a, b) => {
        if (b.count !== a.count) return b.count - a.count
        const aDrawing = a.drawingNumber || ''
        const bDrawing = b.drawingNumber || ''
        if (aDrawing !== bDrawing) return aDrawing.localeCompare(bDrawing, 'zh-Hans')
        return a.type.localeCompare(b.type, 'zh-Hans')
      })
    }
  },
  watch: {
    pages(newPages) {
      if (!newPages.length) {
        this.currentPage = 0
        return
      }
      if (this.currentPage >= newPages.length) {
        this.currentPage = newPages.length - 1
      }
    },
    currentPage(val) {
      this.$nextTick(() => {
        this.loadBarcodesForPage(val)
        this.preloadAdjacentBarcodes()
      })
    },
    issueGroups: {
      handler(newGroups) {
        if (!this.issueFilter) {
          if (newGroups.length) this.issueCompletionNotified = false
          return
        }
        const currentKey = this.issueFilter.key
        const match = newGroups.find(group => group.key === currentKey)
        if (match) {
          this.issueFilter.indexes = match.indexes.slice()
          if (!match.count) {
            this.advanceToNextIssueGroup(newGroups)
          } else {
            this.$nextTick(() => this.ensureCurrentPageContainsFilter())
          }
        } else {
          this.advanceToNextIssueGroup(newGroups)
        }
      },
      deep: true
    }
  },
  methods: {
    hasText(value) {
      if (value === null || value === undefined) return false
      return String(value).trim().length > 0
    },
    decorateRecord(raw) {
      const record = { ...raw }
      record.notificationNumber = record.notificationNumber != null ? String(record.notificationNumber).trim() : ''
      record.productName = record.productName != null ? String(record.productName).trim() : ''
      record.drawingNumber = record.drawingNumber != null ? String(record.drawingNumber).trim() : ''
      record.partName = record.partName != null ? String(record.partName).trim() : ''
      record.processName = record.processName != null ? String(record.processName).trim() : ''
      record.processCode = record.processCode != null ? String(record.processCode).trim() : ''
      if (record.planQty !== null && record.planQty !== undefined && record.planQty !== '') {
        const num = Number(record.planQty)
        record.planQty = Number.isNaN(num) ? null : num
      } else {
        record.planQty = null
      }
      if (record.hours !== null && record.hours !== undefined && record.hours !== '') {
        const num = Number(record.hours)
        record.hours = Number.isNaN(num) ? null : num
      } else {
        record.hours = null
      }
      const rawBarcode = record.barcode != null ? String(record.barcode).trim() : ''
      record.barcode = this.sanitize(rawBarcode)
      record.barcodeImage = record.barcodeImage || ''
      record.workerCodes = record.workerCodes || ''
      record.qualifiedQty = record.qualifiedQty != null ? Number(record.qualifiedQty) : null
      record.hourSubtotal = record.hourSubtotal != null ? Number(record.hourSubtotal) : null
      const sourceCodeMissing = record.codeMissing === true
      record.codeMissing = sourceCodeMissing
      record.serverCodeMissing = sourceCodeMissing
      record.hoursMissing = record.hoursMissing === true
      record.notificationMissing = record.notificationMissing === true
      record.productMissing = record.productMissing === true
      record.partMissing = record.partMissing === true
      record.drawingMissing = record.drawingMissing === true
      record.planMissing = record.planMissing === true
      record.processMissing = record.processMissing === true
      record.barcodeMissing = record.barcodeMissing === true
      record.issueSummary = ''
      record.hasIssue = false
      record.issueTypes = []
      this.updateIssueFlags(record)
      return record
    },
    updateIssueFlags(record) {
      if (!record) return
      const issues = []
      record.notificationMissing = !this.hasText(record.notificationNumber)
      if (record.notificationMissing) issues.push('通知单号')

      record.productMissing = !this.hasText(record.productName)
      if (record.productMissing) issues.push('产品名称')

      record.partMissing = !this.hasText(record.partName)
      if (record.partMissing) issues.push('名称')

      record.drawingMissing = !this.hasText(record.drawingNumber)
      if (record.drawingMissing) issues.push('图号')

      record.planMissing = record.planQty === null || record.planQty === undefined || Number.isNaN(record.planQty)
      if (record.planMissing) issues.push('计划数')

      record.processMissing = !this.hasText(record.processName)
      if (record.processMissing) issues.push('工序')

      record.hoursMissing = record.hours === null || record.hours === undefined || record.hours === '' || Number.isNaN(record.hours)
      if (record.hoursMissing) issues.push('单件工时')

      const hasProcessCode = this.hasText(record.processCode)
      const codeMissingFlag = record.serverCodeMissing === true || !hasProcessCode
      if (!codeMissingFlag) {
        record.serverCodeMissing = false
      }
      record.codeMissing = codeMissingFlag
      if (record.codeMissing) issues.push('工序代码')

      const sanitizedBarcode = this.hasText(record.barcode) ? this.sanitize(String(record.barcode)) : ''
      if (sanitizedBarcode !== record.barcode) {
        record.barcode = sanitizedBarcode
      }
      record.barcodeMissing = !this.hasText(sanitizedBarcode)
      if (record.barcodeMissing) issues.push('条形码')

      record.hasIssue = issues.length > 0
      record.issueSummary = record.hasIssue ? `缺少：${issues.join('、')}` : ''
      record.issueTypes = issues.slice()
      if (record.hasIssue) {
        this.issueCompletionNotified = false
      }
    },
    handleRequestError(error, fallback) {
      if (error instanceof Error && error.message === 'missing-user') return
      console.error(error)
      const response = error && error.response
      let message = null
      if (response && response.data != null) {
        const data = response.data
        if (typeof data === 'string') {
          message = data
        } else if (typeof data === 'object') {
          message = data.message || data.error || data.detail || null
          if (!message && Array.isArray(data.errors) && data.errors.length) {
            const first = data.errors[0]
            message = typeof first === 'string' ? first : (first && first.message) || null
          }
        }
      }
      if (!message && error && typeof error.message === 'string') {
        message = error.message
      }
      alert(message || fallback || '操作失败')
    },
    requireUserHeaders() {
      const user = (localStorage.getItem('username') || '').trim()
      if (!user) {
        alert('请先登录后再进行此操作')
        throw new Error('missing-user')
      }
      return { 'X-User': user }
    },
    onFileChange(e) { this.file = e.target.files[0] },
    async fetchFiles() { const res = await axios.get('/api/files'); this.files = res.data },
    async load() {
      if (!this.selectedFileId) return
      this.loading = true
      this.barcodeCache = {}
      try {
        const res = await axios.get(`/api/workrecords/file/${this.selectedFileId}`)
        if (!Array.isArray(res.data) || !res.data.length) {
          alert('未找到该文件的记录'); this.preview = []
        } else {
          this.preview = res.data.map(r => this.decorateRecord({
            ...r,
            barcodeImage: '',
            codeMissing: r.codeMissing,
            hoursMissing: r.hoursMissing
          }))
        }
        this.fileId = this.selectedFileId
        this.file = null
        this.currentPage = 0
        this.issueFilter = null
        this.issueCompletionNotified = false
        this.$nextTick(() => {
          this.ensurePageInRange()
          this.loadBarcodesForPage(this.currentPage)
          this.preloadAdjacentBarcodes()
        })
      } catch (e) { console.error(e); alert('加载失败') }
      this.loading = false
    },
    async remove() {
      if (!this.selectedFileId) return
      if (!confirm('删除该文件及其所有记录，确定删除?')) return
      let headers
      try { headers = this.requireUserHeaders() }
      catch (err) { return }
      this.loading = true
      try {
        await axios.delete(`/api/files/${this.selectedFileId}`, { headers })
        this.selectedFileId = ''
        this.preview = []
        this.currentPage = 0
        this.issueFilter = null
        await this.fetchFiles()
        alert('已删除')
      } catch (e) {
        console.error(e)
        alert('删除失败')
      }
      this.loading = false
    },
    async parse() {
      if (this.file) {
        const dup = this.files.find(f => f.fileName === this.file.name)
        if (dup) { const cont = confirm('发现同名文件，若内容相同请勿重复上传。继续上传?'); if (!cont) return }
      }
      let headers
      try { headers = this.requireUserHeaders() }
      catch (err) { return }
      this.loading = true; this.showProgress = true; this.parseProgress = 5; this.barcodeCache = {}; this.issueFilter = null; this.issueCompletionNotified = false
      try {
        const data = new FormData(); data.append('file', this.file); data.append('store', 'false')
        const res = await axios.post('/api/workrecords/parse', data, { headers })
        const payload = res && res.data ? res.data : {}
        this.fileId = payload.fileId
        this.selectedFileId = payload.fileId || ''
        const records = Array.isArray(payload.records) ? payload.records : []
        const processed = []; const total = records.length
        if (!total) this.parseProgress = 100
        for (let i = 0; i < records.length; i++) {
          const r = records[i] || {}
          processed.push(this.decorateRecord({
            ...r,
            workerCodes: '',
            qualifiedQty: null,
            hourSubtotal: null,
            barcodeImage: '',
            codeMissing: !!r.codeMissing,
            hoursMissing: r.hours == null
          }))
          if (total) {
            const percent = Math.min(100, Math.round(((i + 1) / total) * 100))
            if (percent > this.parseProgress) this.parseProgress = percent
          }
          if ((i + 1) % 50 === 0) { await new Promise(resolve => setTimeout(resolve, 0)) }
        }
        const warn = processed.filter(r => r.hasIssue)
        this.preview = processed
        if (warn.length) alert(`发现${warn.length}条记录缺少必填信息，请检查`)
        await this.fetchFiles()
        this.currentPage = 0
        this.$nextTick(() => {
          this.ensurePageInRange()
          this.loadBarcodesForPage(this.currentPage)
          this.preloadAdjacentBarcodes()
        })
      } catch (e) {
        this.handleRequestError(e, '解析失败')
        this.showProgress = false
      }
      this.loading = false
      if (this.showProgress) { this.parseProgress = 100; setTimeout(() => { this.showProgress = false }, 600) }
    },
    async save() {
      if(!confirm('请再次核查数据后确认提交')) return
      let headers
      try { headers = this.requireUserHeaders() }
      catch (err) { return }
      this.loading = true
      await this.refreshProcesses()
      const valid = this.preview.filter(r => r.processCode && r.barcode)
      const chunkSize = 1000
      const url = `/api/workrecords?fileId=${this.fileId}`
      const responses = []
      try {
        for (let offset = 0; offset < valid.length; offset += chunkSize) {
          const chunk = valid.slice(offset, offset + chunkSize)
          // eslint-disable-next-line no-await-in-loop
          const res = await axios.post(url, chunk, { headers })
          if (Array.isArray(res.data)) responses.push(...res.data)
        }
        if (valid.length < this.preview.length) alert('部分记录因缺少工序代码或条形码已被忽略')
        const hasSupp = responses.some(r => r && r.supplemental)
        this.preview = []
        this.file = null
        this.issueFilter = null
        this.issueCompletionNotified = false
        alert(hasSupp ? '保存成功，部分记录为补录，请核查。' : '保存成功')
        await this.fetchFiles()
        this.$emit('saved')
      } catch (e) {
        console.error(e)
        alert('保存失败')
        return
      } finally {
        this.loading = false
      }
    },
    async print() {
      if (!this.preview.length) return
      this.loading = true
      try {
        await this.prefetchAllBarcodes()
      } finally { this.loading = false }
      const title = document.title
      if (this.currentFileName) document.title = this.currentFileName
      const originalBuffer = this.renderBuffer
      this.renderAllPages = true
      this.renderBuffer = Math.max(originalBuffer, 2)
      try {
        await this.$nextTick()
        await this.$nextTick()
        await new Promise(resolve => requestAnimationFrame(() => resolve()))
        window.print()
      } finally {
        this.renderAllPages = false
        this.renderBuffer = originalBuffer
        document.title = title
      }
    },
    handleAfterPrint() {
      this.renderAllPages = false
    },
    sanitize(text) {
      if (text === null || text === undefined) return ''
      const sourceText = String(text)
      const source = typeof sourceText.normalize === 'function' ? sourceText.normalize('NFKC') : sourceText
      const invis = /\p{C}/u
      const whitespace = /\s/u
      let result = ''
      for (const ch of Array.from(source)) {
        if (whitespace.test(ch)) continue
        if (invis.test(ch)) continue
        result += ch
      }
      return result
    },
    async ensureProcessCache(force = false) {
      if (this.processCacheLoaded && !force) return
      try {
        const res = await axios.get('/api/processcodes')
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
    handleProcessInput(record) {
      if (!record) return
      record.processCode = ''
      record.codeMissing = true
      record.serverCodeMissing = false
      record.barcode = ''
      record.barcodeImage = ''
      this.updateIssueFlags(record)
    },
    handlePlanInput(record) {
      if (!record) return
      this.updateIssueFlags(record)
    },
    async updateProcess(r, cacheReady = false, allowFetch = true, fetchBarcodeImage = true) {
      if (!cacheReady) await this.ensureProcessCache()
      const rawName = r.processName || ''
      const name = rawName.trim()
      const existingCode = r.processCode != null ? String(r.processCode).trim() : ''
      if (!name) {
        r.processCode = ''
        r.serverCodeMissing = false
        r.codeMissing = true
        await this.updateBarcode(r, fetchBarcodeImage)
        this.updateIssueFlags(r)
        return
      }
      let code = this.processCache[name]
      if (!code && allowFetch) {
        try {
          const res = await axios.get(`/api/processcodes/name/${encodeURIComponent(name)}`)
          if (res.data && res.data.code) {
            code = String(res.data.code).trim()
            if (code) this.$set(this.processCache, name, code)
          }
        } catch (e) { /* ignore */ }
      }
      if (code) {
        r.processCode = code
        r.serverCodeMissing = false
      } else if (existingCode && r.serverCodeMissing !== true) {
        r.processCode = existingCode
      } else {
        r.processCode = ''
        r.serverCodeMissing = true
      }
      r.codeMissing = !this.hasText(r.processCode) || r.serverCodeMissing === true
      await this.updateBarcode(r, fetchBarcodeImage)
      this.updateIssueFlags(r)
    },
    checkHours(r) {
      r.hoursMissing = r.hours == null || r.hours === ''
      this.updateIssueFlags(r)
    },
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
      const decorated = this.decorateRecord(blank)
      this.preview.splice(index + 1, 0, decorated)
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
      this.preview.forEach(r => this.updateIssueFlags(r))
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
        if (code !== entry.record.barcode) {
          this.$set(entry.record, 'barcode', code)
          this.updateIssueFlags(entry.record)
        }
        if (!code) continue
        if (!this.barcodeCache[code] && !entry.record.barcodeImage) missing.push(code)
      }
      if (!missing.length) {
        for (const entry of page.entries) {
          const code = this.sanitize(entry.record.barcode)
          if (code && this.barcodeCache[code] && entry.record.barcodeImage !== this.barcodeCache[code]) {
            this.$set(entry.record, 'barcodeImage', this.barcodeCache[code])
          }
        }
        return
      }
      this._barcodeLoading.add(pageIndex)
      try {
        await this.fetchBarcodes(missing)
        for (const entry of page.entries) {
          const code = this.sanitize(entry.record.barcode)
          if (code && this.barcodeCache[code]) {
            this.$set(entry.record, 'barcodeImage', this.barcodeCache[code])
          }
        }
      } catch (e) { console.error('加载条码失败', e) }
      finally { this._barcodeLoading.delete(pageIndex) }
    },
    async fetchBarcodes(codes) {
      if (!Array.isArray(codes) || !codes.length) return
      const unique = []
      const seen = new Set()
      for (const raw of codes) {
        const code = this.sanitize(raw)
        if (!code) continue
        if (this.barcodeCache[code]) continue
        if (seen.has(code)) continue
        seen.add(code)
        unique.push(code)
      }
      if (!unique.length) return
      const chunkSize = 250
      for (let offset = 0; offset < unique.length; offset += chunkSize) {
        const chunk = unique.slice(offset, offset + chunkSize)
        try {
          // eslint-disable-next-line no-await-in-loop
          const res = await axios.post('/api/workrecords/generateBarcodes', chunk)
          const data = res && res.data ? res.data : {}
          Object.keys(data || {}).forEach(key => {
            if (!key) return
            this.$set(this.barcodeCache, key, data[key])
          })
        } catch (error) {
          console.error('批量生成条码失败', error)
          break
        }
      }
    },
    async prefetchAllBarcodes() {
      const codes = []
      for (const record of this.preview) {
        const code = this.sanitize(record.barcode)
        if (!code) continue
        codes.push(code)
      }
      await this.fetchBarcodes(codes)
      for (const record of this.preview) {
        const code = this.sanitize(record.barcode)
        if (code && this.barcodeCache[code] && record.barcodeImage !== this.barcodeCache[code]) {
          this.$set(record, 'barcodeImage', this.barcodeCache[code])
        }
        this.updateIssueFlags(record)
      }
    },
    async refreshProcesses() {
      await this.ensureProcessCache()
      const missing = new Set()
      for (const record of this.preview) {
        const name = (record.processName || '').trim()
        if (!name) continue
        if (!this.processCache[name]) missing.add(name)
      }
      if (missing.size) {
        try {
          const res = await axios.post('/api/processcodes/lookup', Array.from(missing))
          const data = res && res.data ? res.data : {}
          Object.keys(data || {}).forEach(key => {
            if (!key) return
            const value = data[key]
            if (value == null) return
            const code = String(value).trim()
            if (code) this.$set(this.processCache, key, code)
          })
        } catch (error) {
          console.error('批量加载工序失败', error)
        }
      }
      const tasks = this.preview.map(r => this.updateProcess(r, true, false, false))
      await Promise.all(tasks)
    },
    async updateBarcode(r, fetchImage = true) {
      if (r.drawingNumber && r.notificationNumber && r.processCode) {
        const bar = `${r.drawingNumber}-${r.notificationNumber}-${r.processCode}`
        const clean = this.sanitize(bar)
        r.barcode = clean
        if (!clean) { r.barcodeImage = ''; return }
        if (this.barcodeCache[clean]) { r.barcodeImage = this.barcodeCache[clean]; return }
        if (!fetchImage) { r.barcodeImage = ''; return }
        try {
          const res = await axios.get('/api/workrecords/generateBarcode', { params: { text: bar } })
          if (res && res.data) { this.$set(this.barcodeCache, clean, res.data); r.barcodeImage = res.data }
          else { r.barcodeImage = '' }
        } catch (e) { console.error('获取条码失败', e); r.barcodeImage = '' }
      } else { r.barcode = ''; r.barcodeImage = '' }
      this.updateIssueFlags(r)
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
    },
    preloadAdjacentBarcodes() {
      if (!this.pages.length) return
      const neighbors = []
      if (this.currentPage > 0) neighbors.push(this.currentPage - 1)
      if (this.currentPage < this.pages.length - 1) neighbors.push(this.currentPage + 1)
      neighbors.forEach(pageIndex => {
        this.loadBarcodesForPage(pageIndex)
      })
    },
    selectIssueGroup(group) {
      if (!group) {
        this.clearIssueFilter()
        return
      }
      this.issueFilter = {
        key: group.key,
        drawingNumber: group.drawingNumber,
        type: group.type,
        indexes: group.indexes.slice()
      }
      this.issueCompletionNotified = false
      this.$nextTick(() => {
        const filteredPages = this.pages
        const targetIndex = this.issueFilter && this.issueFilter.indexes.length ? this.issueFilter.indexes[0] : null
        let pageIndex = filteredPages.findIndex(page => page.entries.some(entry => entry.index === targetIndex))
        if (pageIndex < 0) pageIndex = 0
        if (filteredPages.length) {
          if (pageIndex >= filteredPages.length) pageIndex = filteredPages.length - 1
          this.currentPage = Math.max(0, pageIndex)
        } else {
          this.currentPage = 0
        }
      })
    },
    clearIssueFilter() {
      this.issueFilter = null
      this.issueCompletionNotified = false
      this.currentPage = 0
    },
    advanceToNextIssueGroup(groups) {
      const remaining = groups.filter(group => group.count > 0)
      if (!remaining.length) {
        const hadFilter = !!this.issueFilter
        this.issueFilter = null
        if (hadFilter && !this.issueCompletionNotified) {
          alert('所有缺陷已处理完毕！')
          this.issueCompletionNotified = true
        }
        return
      }
      this.issueCompletionNotified = false
      const currentKey = this.issueFilter ? this.issueFilter.key : null
      let next = remaining[0]
      if (currentKey) {
        const idx = remaining.findIndex(group => group.key === currentKey)
        if (idx >= 0) {
          next = remaining[(idx + 1) % remaining.length]
        }
      }
      this.selectIssueGroup(next)
    },
    ensureCurrentPageContainsFilter() {
      if (!this.issueFilter) return
      const filteredPages = this.pages
      if (!filteredPages.length) {
        this.currentPage = 0
        return
      }
      if (this.currentPage >= filteredPages.length) {
        this.currentPage = filteredPages.length - 1
      }
      const activeIndexes = new Set(this.issueFilter.indexes)
      const current = filteredPages[this.currentPage]
      if (current && current.entries.some(entry => activeIndexes.has(entry.index))) {
        return
      }
      const fallback = filteredPages.findIndex(page => page.entries.some(entry => activeIndexes.has(entry.index)))
      if (fallback >= 0) {
        this.currentPage = fallback
      } else {
        this.currentPage = 0
      }
    },
    getBulkConfig(type) {
      switch (type) {
        case '工序代码':
          return {
            prompt: '请输入工序代码（将应用到当前筛选的所有记录）',
            prepare(input) {
              const value = String(input || '').trim()
              if (!value) {
                alert('请输入有效的工序代码')
                return undefined
              }
              return value
            },
            apply(record, value) {
              record.processCode = value
              record.serverCodeMissing = false
              record.codeMissing = false
              return this.updateBarcode(record)
            }
          }
        case '工序':
          return {
            prompt: '请输入工序名称（将应用到当前筛选的所有记录）',
            prepare(input) {
              const value = String(input || '').trim()
              if (!value) {
                alert('请输入有效的工序名称')
                return undefined
              }
              return value
            },
            apply(record, value) {
              record.processName = value
              return this.updateProcess(record)
            }
          }
        case '单件工时':
          return {
            prompt: '请输入单件工时（数字，将应用到当前筛选的所有记录）',
            prepare(input) {
              const value = Number(input)
              if (Number.isNaN(value)) {
                alert('请输入有效的数字')
                return undefined
              }
              return value
            },
            apply(record, value) {
              record.hours = value
              record.hoursMissing = false
              this.updateIssueFlags(record)
            }
          }
        case '计划数':
          return {
            prompt: '请输入计划数（数字，将应用到当前筛选的所有记录）',
            prepare(input) {
              const value = Number(input)
              if (Number.isNaN(value)) {
                alert('请输入有效的数字')
                return undefined
              }
              return value
            },
            apply(record, value) {
              record.planQty = value
              record.planMissing = false
              this.updateIssueFlags(record)
            }
          }
        default:
          return null
      }
    },
    handleBulkFill(group) {
      if (!group) return
      const config = this.getBulkConfig(group.type)
      if (!config) {
        alert('该缺陷暂不支持批量填写')
        return
      }
      const input = prompt(config.prompt, '')
      if (input === null) return
      const prepared = config.prepare ? config.prepare.call(this, input) : input
      if (prepared === undefined) return
      const tasks = []
      group.indexes.slice().forEach(idx => {
        const record = this.preview[idx]
        if (!record) return
        const result = config.apply.call(this, record, prepared, idx)
        if (result && typeof result.then === 'function') {
          tasks.push(result)
        }
      })
      Promise.all(tasks).finally(() => {
        this.ensureCurrentPageContainsFilter()
      })
    }
  }
}
</script>
