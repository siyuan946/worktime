<template>
  <section class="section-card upload-shell" :class="codeModeClass">
    <header class="upload-header no-print">
      <div>
        <h2 class="h5 mb-1">Excel上传</h2>
        <p class="text-muted small mb-0">从解析、校对到打印的完整录入流程</p>
      </div>
      <div class="upload-current-file" v-if="currentFileName">
        <div class="label">当前文件</div>
        <div class="value" :title="currentFileName">{{ currentFileName }}</div>
      </div>
    </header>

    <div class="upload-toolbar no-print">
      <div class="toolbar-row">
        <div class="toolbar-block flex-grow-1">
          <label class="form-label mb-1">选择或解析文件</label>
          <div class="d-flex flex-wrap gap-2 align-items-center">
            <input class="form-control" type="file" @change="onFileChange">
            <button class="btn btn-outline-primary" @click="parse" :disabled="!file">解析</button>
            <div class="d-flex align-items-center gap-2">
              <select class="form-select" style="max-width:200px" v-model="selectedFileId">
                <option value="" disabled>选择历史文件</option>
                <option v-for="f in files" :key="f.id" :value="f.id">{{ f.fileName }} ({{ f.uploadTime ? f.uploadTime.slice(0,10) : '' }})</option>
              </select>
              <button class="btn btn-outline-secondary" @click="load" :disabled="!selectedFileId">加载</button>
              <button class="btn btn-outline-danger" @click="remove" :disabled="!selectedFileId">删除</button>
            </div>
          </div>
        </div>
        <div class="toolbar-block flex-grow-1">
          <label class="form-label mb-1">数据清理与提交</label>
          <div class="d-flex flex-wrap gap-2 align-items-center">
            <button class="btn btn-outline-warning" @click="deleteZero" :disabled="!preview.length">清除0工序</button>
            <button class="btn btn-primary" @click="save" :disabled="!preview.length">保存</button>
            <button class="btn btn-secondary" @click="print" :disabled="!preview.length">打印</button>
            <div class="spinner-border" v-if="loading"></div>
          </div>
        </div>
      </div>

      <div class="toolbar-row">
        <div class="flex-grow-1">
          <div class="progress mb-2" v-if="showProgress" style="height: 0.75rem;">
            <div class="progress-bar" role="progressbar" :style="{ width: parseProgress + '%' }">
              {{ parseProgress }}%
            </div>
          </div>
          <div
            v-if="feedback.visible"
            class="alert alert-dismissible fade show mb-0"
            :class="'alert-' + feedback.variant"
            role="status"
          >
            <div>{{ feedback.message }}</div>
            <button type="button" class="btn-close" aria-label="关闭提示" @click="hideFeedback"></button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="preview.length" class="upload-body" :class="codeModeClass">
      <div class="preview-column" id="preview-table">
        <div class="preview-header no-print">
          <div>
            <h2 class="h5 mb-0">预览</h2>
            <div class="text-muted small">按图号分页展示，右侧缺陷面板随时校对</div>
          </div>
          <div class="preview-actions">
            <div>当前图号：{{ currentPageInfo?.drawingNumber || '—' }}（第 {{ currentPage + 1 }} / {{ pages.length }} 页）</div>
            <div class="input-group input-group-sm" style="width: 240px;">
              <input class="form-control" placeholder="搜索图号" v-model.trim="drawingSearch" @keyup.enter="jumpToDrawing">
              <button class="btn btn-outline-secondary" @click="jumpToDrawing">跳转</button>
            </div>
            <div class="btn-group btn-group-sm">
              <button class="btn btn-outline-secondary" @click="prevPage" :disabled="currentPage === 0">上一页</button>
              <button class="btn btn-outline-secondary" @click="nextPage" :disabled="currentPage >= pages.length - 1">下一页</button>
            </div>
          </div>
        </div>

        <div v-if="issueFilter" class="alert alert-warning py-2 px-3 no-print issue-filter-alert">
          <div>
            正在处理图号「{{ issueFilter.drawingNumber || '（空图号）' }}」缺少「{{ issueFilter.type }}」的 {{ issueFilter.indexes.length }} 条记录。
          </div>
          <button type="button" class="btn btn-sm btn-outline-secondary" @click="clearIssueFilter">退出筛选</button>
        </div>

        <div class="preview-pages-flow">
          <div
            v-for="item in visiblePages"
            :key="item.index"
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
                  <th class="barcode-cell">{{ codeLabel }}</th>
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
                      @blur="handlePlanBlur(entry.record)"
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
                      @blur="handleHoursBlur(entry.record)"
                    />
                    <span class="print-text">{{ entry.record.hours }}</span>
                  </td>
                  <td :class="['no-print', { 'missing-cell': entry.record.codeMissing }]">
                    <input
                      type="text"
                      class="form-control form-control-sm"
                      v-model="entry.record.processCode"
                      placeholder="填写工序代码"
                      @input="handleProcessCodeTyping(entry.record)"
                      @blur="handleProcessCodeBlur(entry.record)"
                    />
                    <span class="print-text">{{ entry.record.processCode }}</span>
                  </td>
                  <td :class="['process-col', { 'missing-cell': entry.record.processMissing || entry.record.codeMissing }]">
                    <input
                      class="form-control form-control-sm no-print"
                      v-model="entry.record.processName"
                      @blur="handleProcessBlur(entry.record)"
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
            <div class="print-page-footer" aria-hidden="true">
              {{ formatPrintFooter(item.page.drawingNumber) }}
            </div>
          </div>
        </div>
      </div>

      <aside class="issue-column no-print">
        <div class="issue-panel-container">
          <RecordIssuePanel
            :groups="issueGroups"
            :active-group="issueFilter"
            @select="selectIssueGroup"
            @clear="clearIssueFilter"
            @bulk="handleBulkFill"
          />
        </div>
      </aside>
    </div>

    <div
      v-if="preview.length"
      id="print-area"
      class="print-area"
      :class="[codeModeClass, layoutClass]"
      :style="printCssVars"
      aria-hidden="true"
    >
      <div
        v-for="(page, index) in printPages"
        :key="`print-vertical-${index}`"
        class="preview-page vertical-preview"
        :class="{ 'force-new-page': page.isFirstOfDrawing && index !== 0 }"
      >
        <div class="d-flex justify-content-between align-items-center mb-2 page-heading">
          <h3 class="h6 mb-0">图号：{{ page.drawingNumber || '（空）' }}</h3>
          <span class="text-muted">第 {{ index + 1 }} 页 / 共 {{ printPages.length }} 页</span>
        </div>
        <div class="vertical-grid" :style="verticalGridStyle">
          <div
            v-for="field in verticalFields"
            :key="field.key"
            class="vertical-row"
          >
            <div class="vertical-label">{{ field.label }}</div>
            <div
              v-for="entry in getPaddedPrintEntries(page)"
              :key="`${field.key}-${entry.index}`"
              :class="[{ 'barcode-cell': field.key === 'barcode' }, 'vertical-value-cell', field.className]"
            >
              <template v-if="field.key === 'barcode'">
                <div class="barcode-box">
                  <div class="barcode-text">{{ entry.record.barcode }}</div>
                  <img v-if="entry.record.barcodeImage" :src="'data:image/png;base64,' + entry.record.barcodeImage" />
                </div>
              </template>
              <template v-else>
                {{ getFieldValue(entry.record, field.key) }}
              </template>
            </div>
          </div>
        </div>
        <div class="print-page-footer">{{ formatPrintFooter(page.drawingNumber) }}</div>
      </div>
    </div>
    <BulkIssueModal
      :visible="bulkModal.visible"
      :type="bulkModal.type"
      :group="bulkModal.group"
      :records="bulkModal.records"
      @close="closeBulkModal"
      @apply="applyBulkUpdates"
    />
  </section>
</template>

<script>
import axios from 'axios'
import RecordIssuePanel from './RecordIssuePanel.vue'
import BulkIssueModal from './BulkIssueModal.vue'
export default {
  components: { RecordIssuePanel, BulkIssueModal },
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
      verticalColumnsPerPage: 8,
      processCache: {},
      processCacheLoaded: false,
      barcodeCache: { qr: {}, barcode: {} },
      showProgress: false,
      parseProgress: 0,
      renderAllPages: false,
      renderBuffer: 0,
      issueFilter: null,
      issueCompletionNotified: false,
      bulkModal: {
        visible: false,
        type: '',
        group: null,
        records: []
      },
      rememberedPairs: Object.create(null),
      feedback: {
        visible: false,
        message: '',
        variant: 'info'
      },
      feedbackTimer: null,
      codeMode: localStorage.getItem('codeMode') || 'qr',
      printLayout: 'vertical'
    }
  },
  created() {
    this.fetchFiles()
    this.ensureProcessCache()
    this.persistPrintLayout()
    this.applyCodeMode(localStorage.getItem('codeMode') || 'qr', { skipFetch: true })
  },
  mounted() {
    window.addEventListener('afterprint', this.handleAfterPrint)
    this.modeListener = (mode) => this.applyCodeMode(mode)
    if (this.$root && this.$root.$on) {
      this.$root.$on('code-mode-changed', this.modeListener)
    }
  },
  beforeDestroy() {
    window.removeEventListener('afterprint', this.handleAfterPrint)
    this.clearFeedbackTimer()
    if (this.$root && this.$root.$off && this.modeListener) {
      this.$root.$off('code-mode-changed', this.modeListener)
    }
  },
  computed: {
    currentFileName() {
      if (this.file) return this.file.name
      const id = this.fileId || this.selectedFileId
      const f = this.files.find(x => x.id === id)
      return f ? f.fileName : ''
    },
    pageSize() {
      return this.verticalColumnsPerPage
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
      const size = this.pageSize || 1
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
    printPages() { return this.allPages },
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
    },
    codeModeClass() { return `code-mode-${this.codeMode}` },
    layoutClass() { return 'layout-vertical' },
    codeLabel() { return this.codeMode === 'barcode' ? '条形码' : '二维码' },
    printColumnsPerPage() { return this.pageSize || 12 },
    verticalGridStyle() {
      return {
        '--vertical-column-count': this.printColumnsPerPage || 10,
        '--vertical-label-width': '90px',
        '--vertical-row-height': '44px',
        '--vertical-barcode-height': this.codeMode === 'qr' ? '100px' : '90px'
      }
    },
    verticalFields() {
      return [
        { key: 'notificationNumber', label: '通知单号' },
        { key: 'productName', label: '产品名称' },
        { key: 'drawingNumber', label: '图号' },
        { key: 'planQty', label: '计划数' },
        { key: 'partName', label: '名称' },
        { key: 'hours', label: '单件工时' },
        { key: 'processCode', label: '工序代码' },
        { key: 'processName', label: '工序' },
        { key: 'workerCodes', label: '人员代码' },
        { key: 'qualifiedQty', label: '合格件数' },
        { key: 'startTime', label: '起始时间' },
        { key: 'endTime', label: '结束时间' },
        { key: 'inspector', label: '检验员' },
        { key: 'barcode', label: this.codeLabel, className: 'barcode-column' }
      ]
    },
    activeCodeCache() { return this.barcodeCache[this.codeMode] || {} },
    printCssVars() {
        return {
          '--rows-per-page': this.rowsPerPage,
          '--vertical-column-count': this.printColumnsPerPage,
          '--print-column-count': this.printColumnsPerPage,
          '--vertical-row-count': this.verticalFields.length,
          '--vertical-barcode-height': this.codeMode === 'qr' ? '100px' : '90px',
          '--barcode-box-height': this.codeMode === 'qr' ? '90px' : '70px',
          '--barcode-box-height-qr': this.codeMode === 'qr' ? '100px' : '90px'
        }
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
            this.handleIssueGroupCleared(newGroups)
          } else {
            this.$nextTick(() => this.ensureCurrentPageContainsFilter())
          }
        } else {
          this.handleIssueGroupCleared(newGroups)
        }
      },
      deep: true
    }
  },
  methods: {
    clearFeedbackTimer() {
      if (this.feedbackTimer) {
        clearTimeout(this.feedbackTimer)
        this.feedbackTimer = null
      }
    },
    createEmptyRecord() {
      return {
        notificationNumber: '',
        productName: '',
        drawingNumber: '',
        planQty: '',
        partName: '',
        hours: '',
        processCode: '',
        processName: '',
        workerCodes: '',
        qualifiedQty: '',
        startTime: '',
        endTime: '',
        inspector: '',
        barcode: '',
        barcodeImage: ''
      }
    },
    getPaddedPrintEntries(page) {
      const target = this.printColumnsPerPage
      const entries = Array.isArray(page.entries) ? page.entries.slice() : []
      if (!Number.isFinite(target) || target <= 0) return entries
      const drawingKey = page && page.drawingNumber ? page.drawingNumber : 'blank'
      for (let i = entries.length; i < target; i += 1) {
        entries.push({ index: `placeholder-${drawingKey}-${i}`, record: this.createEmptyRecord(), placeholder: true })
      }
      return entries
    },
    getFieldValue(record, key) {
      if (!record || !key) return ''
      switch (key) {
        case 'notificationNumber': return record.notificationNumber || ''
        case 'productName': return record.productName || ''
        case 'drawingNumber': return record.drawingNumber || ''
        case 'planQty': return record.planQty
        case 'partName': return record.partName || ''
        case 'hours': return record.hours
        case 'processCode': return record.processCode || ''
        case 'processName': return record.processName || ''
        case 'workerCodes': return record.workerCodes || ''
        case 'qualifiedQty': return record.qualifiedQty
        case 'startTime': return record.startTime || ''
        case 'endTime': return record.endTime || ''
        case 'inspector': return record.inspector || ''
        default: return record[key] || ''
      }
    },
    hideFeedback() {
      this.clearFeedbackTimer()
      this.feedback.visible = false
      this.feedback.message = ''
    },
    showFeedback(message, variant = 'info', duration = 3000) {
      if (!message) {
        this.hideFeedback()
        return
      }
      this.clearFeedbackTimer()
      this.feedback.variant = variant || 'info'
      this.feedback.message = message
      this.feedback.visible = true
      if (duration > 0) {
        this.feedbackTimer = setTimeout(() => {
          this.hideFeedback()
        }, duration)
      }
    },
    persistPrintLayout() {
      this.printLayout = 'vertical'
      localStorage.setItem('printLayout', 'vertical')
    },
    hasText(value) {
      if (value === null || value === undefined) return false
      return String(value).trim().length > 0
    },
      normalizeProcessCode(value) {
        if (value === null || value === undefined) return ''
        const text = String(value).trim()
        if (!text || text === '0') return ''
        return text
      },
      isValidProcessCode(value) {
        return this.normalizeProcessCode(value) !== ''
      },
      decorateRecord(raw) {
        const record = { ...raw }
        record.notificationNumber = record.notificationNumber != null ? String(record.notificationNumber).trim() : ''
        record.productName = record.productName != null ? String(record.productName).trim() : ''
        record.drawingNumber = record.drawingNumber != null ? String(record.drawingNumber).trim() : ''
        record.partName = record.partName != null ? String(record.partName).trim() : ''
        record.processName = record.processName != null ? String(record.processName).trim() : ''
        const normalizedCode = this.normalizeProcessCode(record.processCode)
        record.processCode = normalizedCode
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
        const sourceCodeMissing = record.codeMissing === true || !normalizedCode
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
        record._lastAcceptedProcessCode = record.processCode
        record._lastAcceptedBarcode = record.barcode
        record._lastAcceptedBarcodeImage = record.barcodeImage
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

      const normalizedCode = this.normalizeProcessCode(record.processCode)
      if (normalizedCode !== record.processCode) {
        record.processCode = normalizedCode
      }
      const hasProcessCode = this.isValidProcessCode(normalizedCode)
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
      if (!message && response && response.status === 409) {
        const url = response.config && response.config.url ? String(response.config.url) : ''
        if (url.includes('/api/processcodes')) {
          message = '工序代码已存在，请使用其他代号'
        } else {
          message = '请求与服务器当前状态冲突，请检查数据后重试'
        }
      }
      if (!message && error && typeof error.message === 'string') {
        message = error.message
      }
      const final = message || fallback || '操作失败'
      if (typeof this.showFeedback === 'function') {
        this.showFeedback(final, 'danger', 5000)
      }
      alert(final)
    },
    requireUserHeaders() {
      const user = (localStorage.getItem('username') || '').trim()
      if (!user) {
        alert('请先登录后再进行此操作')
        throw new Error('missing-user')
      }
      return { 'X-User': user }
    },
    ensureCodeCache(mode = this.codeMode) {
      if (!this.barcodeCache[mode]) {
        if (this.$set) this.$set(this.barcodeCache, mode, {})
        else this.barcodeCache[mode] = {}
      }
      return this.barcodeCache[mode]
    },
    applyCodeMode(mode, options = {}) {
      const target = mode || 'qr'
      const skipFetch = options && options.skipFetch
      if (this.codeMode === target && !options.force) {
        if (!skipFetch) this.refreshCodeImagesForMode()
        return
      }
      this.codeMode = target
      localStorage.setItem('codeMode', target)
      this.ensureCodeCache(target)
      if (!skipFetch) {
        this.refreshCodeImagesForMode()
        this.prefetchAllBarcodes()
      }
    },
    refreshCodeImagesForMode() {
      this.preview.forEach(r => { r.barcodeImage = '' })
    },
    onFileChange(e) { this.file = e.target.files[0] },
    async fetchFiles() {
      const res = await axios.get('/api/files')
      this.files = res.data
      this.barcodeCache = { qr: {}, barcode: {} }
      this.ensureCodeCache()
    },
    async load() {
      if (!this.selectedFileId) return
      this.loading = true
      this.barcodeCache = { qr: {}, barcode: {} }
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
      this.loading = true; this.showProgress = true; this.parseProgress = 5; this.barcodeCache = { qr: {}, barcode: {} }; this.issueFilter = null; this.issueCompletionNotified = false
      try {
        const data = new FormData(); data.append('file', this.file)
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
      let partialNotice = ''
      try {
        for (let offset = 0; offset < valid.length; offset += chunkSize) {
          const chunk = valid.slice(offset, offset + chunkSize)
          // eslint-disable-next-line no-await-in-loop
          const res = await axios.post(url, chunk, { headers })
          if (Array.isArray(res.data)) responses.push(...res.data)
        }
        if (valid.length < this.preview.length) {
          partialNotice = '部分记录因缺少工序代码或条形码已被忽略'
        }
        const hasSupp = responses.some(r => r && r.supplemental)
        this.preview = []
        this.file = null
        this.issueFilter = null
        this.issueCompletionNotified = false
        const notices = []
        if (hasSupp) notices.push('部分记录为补录，请核查')
        if (partialNotice) notices.push(partialNotice)
        const baseMessage = '保存成功'
        const finalMessage = notices.length ? `${baseMessage}，${notices.join('，')}` : baseMessage
        const variant = notices.length ? 'warning' : 'success'
        const duration = notices.length ? 6000 : 4000
        this.showFeedback(finalMessage, variant, duration)
        await this.fetchFiles()
        this.$emit('saved')
      } catch (e) {
        console.error(e)
        this.showFeedback('保存失败，请稍后重试', 'danger', 5000)
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
    buildAutoSavePayload(record) {
      if (!record) return null
      const asText = (value) => {
        if (value === null || value === undefined) return null
        const str = String(value).trim()
        return str.length ? str : null
      }
      const asInteger = (value) => {
        if (value === null || value === undefined || value === '') return null
        const num = Number(value)
        if (Number.isNaN(num)) return null
        return Math.trunc(num)
      }
      const asNumber = (value) => {
        if (value === null || value === undefined || value === '') return null
        const num = Number(value)
        return Number.isNaN(num) ? null : num
      }
      const payload = {
        id: record.id != null ? record.id : null,
        notificationNumber: asText(record.notificationNumber),
        productName: asText(record.productName),
        drawingNumber: asText(record.drawingNumber),
        productCode: asText(record.productCode),
        partName: asText(record.partName),
        planQty: asInteger(record.planQty),
        sourceRowNumber: asInteger(record.sourceRowNumber),
        processName: asText(record.processName),
        processCode: asText(record.processCode),
        barcode: asText(this.sanitize(record.barcode)),
        barcodeImage: record.barcodeImage || null,
        batchNumber: asText(record.batchNumber),
        hours: asNumber(record.hours),
        workerCodes: asText(record.workerCodes),
        workerNames: asText(record.workerNames),
        workerQtys: asText(record.workerQtys),
        qualifiedQty: asNumber(record.qualifiedQty),
        hourSubtotal: asNumber(record.hourSubtotal),
        supplemental: record.supplemental,
        filled: record.filled,
        startTime: record.startTime || null,
        endTime: record.endTime || null,
        inspector: asText(record.inspector),
        remark1: asText(record.remark1),
        remark2: asText(record.remark2)
      }
      return payload
    },
    formatPrintFooter(drawingNumber) {
      const sanitized = drawingNumber == null ? '' : this.sanitize(drawingNumber)
      const drawing = sanitized && sanitized.length ? sanitized : '（空）'
      return `图号：${drawing}`
    },
    async autoSaveRecord(record, options = {}) {
      const silent = options && options.silent === true
      if (!record) return
      if (!this.fileId) return
      if (record._autoSaving) {
        record._autoSavePending = true
        return
      }
      let headers
      try { headers = this.requireUserHeaders() }
      catch (err) { return }
      const payload = this.buildAutoSavePayload(record)
      if (!payload) return
      record._autoSaving = true
      try {
        const res = await axios.post('/api/workrecords/autosave', payload, {
          headers,
          params: { fileId: this.fileId }
        })
        if (res && res.data) {
          const updated = this.decorateRecord({
            ...res.data,
            barcodeImage: res.data.barcodeImage || payload.barcodeImage
          })
          Object.keys(updated).forEach(key => {
            if (key === '_autoSaving' || key === '_autoSavePending') return
            this.$set(record, key, updated[key])
          })
        }
        if (!silent) {
          this.showFeedback('单条记录已自动保存', 'success', 2000)
        }
      } catch (error) {
        this.handleRequestError(error, '自动保存失败')
      } finally {
        record._autoSaving = false
        if (record._autoSavePending) {
          record._autoSavePending = false
          this.autoSaveRecord(record, options)
        }
      }
    },
    async rememberProcessCode(record) {
      if (!record) return false
      const name = record.processName != null ? String(record.processName).trim() : ''
      const code = this.normalizeProcessCode(record.processCode)
      if (!name || !code) return false
      const key = `${name}|||${code}`
      if (this.rememberedPairs[key]) {
        record._lastAcceptedProcessCode = code
        record._lastAcceptedBarcode = record.barcode
        record._lastAcceptedBarcodeImage = record.barcodeImage
        if (!this.processCache[name] || this.processCache[name] !== code) {
          this.$set(this.processCache, name, code)
        }
        return true
      }
      let headers
      try {
        headers = this.requireUserHeaders()
      } catch (err) {
        return false
      }
      const previousCode = this.normalizeProcessCode(record._lastAcceptedProcessCode)
      const previousBarcode = record._lastAcceptedBarcode || ''
      const previousImage = record._lastAcceptedBarcodeImage || ''
      try {
        await axios.post('/api/processcodes/remember', { name, code }, { headers })
        this.$set(this.rememberedPairs, key, true)
        if (!this.processCache[name] || this.processCache[name] !== code) {
          this.$set(this.processCache, name, code)
        }
        record._lastAcceptedProcessCode = code
        record._lastAcceptedBarcode = record.barcode
        record._lastAcceptedBarcodeImage = record.barcodeImage
        return true
      } catch (error) {
        this.handleRequestError(error, '保存工序代码失败')
        if (this.processCache[name] === code) {
          if (this.hasText(previousCode)) {
            this.$set(this.processCache, name, previousCode)
          } else if (this.$delete) {
            this.$delete(this.processCache, name)
          } else {
            delete this.processCache[name]
          }
        }
        if (previousCode !== code) {
          record.processCode = previousCode
        }
        if (this.hasText(previousCode)) {
          record.serverCodeMissing = false
          record.codeMissing = false
          record.barcode = previousBarcode
          record.barcodeImage = previousImage
        } else {
          record.serverCodeMissing = true
          record.codeMissing = true
          record.barcode = ''
          record.barcodeImage = ''
        }
        this.updateIssueFlags(record)
        return false
      }
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
            const code = this.normalizeProcessCode(item.code)
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
    handleProcessCodeTyping(record) {
      if (!record) return
      const value = record.processCode != null ? String(record.processCode) : ''
      const normalized = this.normalizeProcessCode(value)
      const hasValue = normalized !== ''
      record.serverCodeMissing = !hasValue
      record.codeMissing = !hasValue
      if (!hasValue) {
        record.barcode = ''
        record.barcodeImage = ''
      }
      this.updateIssueFlags(record)
    },
    async handleProcessCodeBlur(record) {
      if (!record) return
      const value = record.processCode != null ? String(record.processCode).trim() : ''
      const normalized = this.normalizeProcessCode(value)
      record.processCode = normalized
      if (!normalized) {
        record.serverCodeMissing = true
        record.codeMissing = true
        record.barcode = ''
        record.barcodeImage = ''
        this.updateIssueFlags(record)
        await this.autoSaveRecord(record)
        return
      }
      const previousBarcode = record.barcode
      const previousImage = record.barcodeImage
      record.serverCodeMissing = false
      record.codeMissing = false
      await this.updateBarcode(record)
      const success = await this.rememberProcessCode(record)
      if (!success) {
        record.barcode = previousBarcode
        record.barcodeImage = previousImage
        return
      }
      this.updateIssueFlags(record)
      await this.autoSaveRecord(record)
    },
    handlePlanInput(record) {
      if (!record) return
      this.updateIssueFlags(record)
    },
    async handlePlanBlur(record) {
      if (!record) return
      this.handlePlanInput(record)
      await this.autoSaveRecord(record)
    },
    async handleHoursBlur(record) {
      if (!record) return
      this.checkHours(record)
      await this.autoSaveRecord(record)
    },
    async handleProcessBlur(record) {
      if (!record) return
      await this.updateProcess(record)
      await this.autoSaveRecord(record)
    },
      async updateProcess(r, cacheReady = false, allowFetch = true, fetchBarcodeImage = true) {
        if (!cacheReady) await this.ensureProcessCache()
        const rawName = r.processName || ''
        const name = rawName.trim()
        const existingCode = r.processCode != null ? String(r.processCode).trim() : ''
        let shouldRemember = false
        if (!name) {
          r.processCode = ''
          r.serverCodeMissing = false
          r.codeMissing = true
          await this.updateBarcode(r, fetchBarcodeImage)
          this.updateIssueFlags(r)
          return
        }
        let code = this.normalizeProcessCode(this.processCache[name])
        if (!code && allowFetch) {
          try {
            const res = await axios.get(`/api/processcodes/name/${encodeURIComponent(name)}`)
            if (res.data && res.data.code) {
              const normalized = this.normalizeProcessCode(res.data.code)
              if (normalized) {
                code = normalized
                this.$set(this.processCache, name, normalized)
              }
            }
          } catch (e) { /* ignore */ }
        }
        const manualCode = this.normalizeProcessCode(existingCode)
        if (code) {
          r.processCode = code
          r.serverCodeMissing = false
        } else if (manualCode) {
          r.processCode = manualCode
          r.serverCodeMissing = false
          if (!this.processCache[name] || this.processCache[name] !== manualCode) {
            this.$set(this.processCache, name, manualCode)
          }
          shouldRemember = true
        } else {
          r.processCode = ''
          r.serverCodeMissing = true
        }
        r.codeMissing = !this.isValidProcessCode(r.processCode)
        await this.updateBarcode(r, fetchBarcodeImage)
        this.updateIssueFlags(r)
        if (shouldRemember && !r.codeMissing) {
          await this.rememberProcessCode(r)
        }
      },
    checkHours(r) {
      r.hoursMissing = r.hours == null || r.hours === ''
      this.updateIssueFlags(r)
    },
    async deleteRow(index) {
      const record = this.preview[index]
      if (!record) return
      if (!confirm('确定删除该行? 删除后不可恢复')) return
      if (record.id) {
        let headers
        try { headers = this.requireUserHeaders() }
        catch (err) { return }
        try {
          await axios.delete(`/api/workrecords/${record.id}`, { headers })
        } catch (error) {
          this.handleRequestError(error, '删除记录失败')
          return
        }
      }
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
    async deleteZero() {
      if (!this.preview.length) return
      if (!confirm('确定删除所有工序为0的行?')) return
      const before = this.preview.length
      const toRemove = this.preview.filter(r => {
        const code = r.processCode != null ? String(r.processCode).trim() : ''
        return code === '0'
      })
      if (!toRemove.length) {
        alert('未找到工序为0的记录')
        return
      }
      const idsToDelete = toRemove.map(r => r.id).filter(Boolean)
      let headers = null
      if (idsToDelete.length) {
        try { headers = this.requireUserHeaders() }
        catch (err) { return }
      }
      const failedIds = new Set()
      let firstError = null
      for (const id of idsToDelete) {
        try {
          // eslint-disable-next-line no-await-in-loop
          await axios.delete(`/api/workrecords/${id}`, { headers })
        } catch (error) {
          if (!firstError) firstError = error
          failedIds.add(id)
        }
      }
      this.preview = this.preview.filter(r => {
        const code = r.processCode != null ? String(r.processCode).trim() : ''
        if (code !== '0') return true
        if (r.id && failedIds.has(r.id)) return true
        return false
      })
      this.preview.forEach(r => this.updateIssueFlags(r))
      const removed = before - this.preview.length
      if (firstError) {
        this.handleRequestError(firstError, '部分记录删除失败')
      }
      const extra = failedIds.size ? '，部分记录删除失败' : ''
      alert(`已删除${removed}行${extra}`)
      this.$nextTick(() => this.ensurePageInRange())
    },
    async loadBarcodesForPage(pageIndex) {
      const page = this.pages[pageIndex]; if (!page) return
      if (!this._barcodeLoading) this._barcodeLoading = new Set()
      if (this._barcodeLoading.has(pageIndex)) return
      const cache = this.ensureCodeCache()
      const missing = []
      for (const entry of page.entries) {
        const code = this.sanitize(entry.record.barcode)
        if (code !== entry.record.barcode) {
          this.$set(entry.record, 'barcode', code)
          this.updateIssueFlags(entry.record)
        }
        if (!code) continue
        if (!cache[code] && !entry.record.barcodeImage) missing.push(code)
      }
      if (!missing.length) {
        for (const entry of page.entries) {
          const code = this.sanitize(entry.record.barcode)
          if (code && cache[code] && entry.record.barcodeImage !== cache[code]) {
            this.$set(entry.record, 'barcodeImage', cache[code])
          }
        }
        return
      }
      this._barcodeLoading.add(pageIndex)
      try {
        await this.fetchBarcodes(missing)
        const updatedCache = this.ensureCodeCache()
        for (const entry of page.entries) {
          const code = this.sanitize(entry.record.barcode)
          if (code && updatedCache[code]) {
            this.$set(entry.record, 'barcodeImage', updatedCache[code])
          }
        }
      } catch (e) { console.error('加载条码失败', e) }
      finally { this._barcodeLoading.delete(pageIndex) }
    },
    async fetchBarcodes(codes) {
      if (!Array.isArray(codes) || !codes.length) return
      const cache = this.ensureCodeCache()
      const unique = []
      const seen = new Set()
      for (const raw of codes) {
        const code = this.sanitize(raw)
        if (!code) continue
        if (cache[code]) continue
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
          const res = await axios.post(`/api/workrecords/generateBarcodes?type=${this.codeMode}`, chunk)
          const data = res && res.data ? res.data : {}
          Object.keys(data || {}).forEach(key => {
            if (!key) return
            const target = this.ensureCodeCache()
            if (this.$set) this.$set(target, key, data[key])
            else target[key] = data[key]
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
        const cache = this.ensureCodeCache()
        if (code && cache[code] && record.barcodeImage !== cache[code]) {
          this.$set(record, 'barcodeImage', cache[code])
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
            const code = this.normalizeProcessCode(value)
            if (code) {
              this.$set(this.processCache, key, code)
            } else if (this.$delete) {
              this.$delete(this.processCache, key)
            } else {
              delete this.processCache[key]
            }
          })
        } catch (error) {
          console.error('批量加载工序失败', error)
        }
      }
      const tasks = this.preview.map(r => this.updateProcess(r, true, false, false))
      await Promise.all(tasks)
    },
    async updateBarcode(r, fetchImage = true) {
      const normalizedCode = this.normalizeProcessCode(r.processCode)
      if (normalizedCode !== r.processCode) {
        r.processCode = normalizedCode
      }
      const canUseCode = normalizedCode && r.serverCodeMissing !== true
      if (r.drawingNumber && r.notificationNumber && canUseCode) {
        const bar = `${r.drawingNumber}-${r.notificationNumber}-${normalizedCode}`
        const clean = this.sanitize(bar)
        r.barcode = clean
        if (!clean) { r.barcodeImage = ''; return }
        const cache = this.ensureCodeCache()
        if (cache[clean]) { r.barcodeImage = cache[clean]; return }
        if (!fetchImage) { r.barcodeImage = ''; return }
        try {
          const res = await axios.get('/api/workrecords/generateBarcode', { params: { text: bar, type: this.codeMode } })
          if (res && res.data) { const target = this.ensureCodeCache(); this.$set(target, clean, res.data); r.barcodeImage = res.data }
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
    handleIssueGroupCleared(groups) {
      const remaining = Array.isArray(groups) ? groups.filter(group => group.count > 0) : []
      const previousPage = this.currentPage
      const hadFilter = !!this.issueFilter
      this.issueFilter = null
      this.$nextTick(() => {
        if (!this.pages.length) {
          this.currentPage = 0
          return
        }
        let target = previousPage
        if (target >= this.pages.length) target = this.pages.length - 1
        if (target < 0) target = 0
        this.currentPage = target
      })
      if (!hadFilter) return
      if (!this.issueCompletionNotified) {
        if (!remaining.length) {
          alert('所有缺陷已处理完毕！')
        } else {
          alert('当前缺陷已处理完毕，请检查页面后再选择其它缺陷。')
        }
        this.issueCompletionNotified = true
      }
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
                const normalized = this.normalizeProcessCode(input)
                if (!normalized) {
                  alert('请输入有效的工序代码（不可为空或为0）')
                  return undefined
                }
                return normalized
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
      if (group.type === '单件工时' || group.type === '工序代码') {
        this.openBulkModal(group)
        return
      }
      const config = this.getBulkConfig(group.type)
      if (!config) {
        alert('该缺陷暂不支持批量填写')
        return
      }
      const input = prompt(config.prompt, '')
      if (input === null) return
      const prepared = config.prepare ? config.prepare.call(this, input) : input
      if (prepared === undefined) return
      const tasks = group.indexes.slice().map(idx => (async () => {
        const record = this.preview[idx]
        if (!record) return
        const result = config.apply.call(this, record, prepared, idx)
        if (result && typeof result.then === 'function') {
          await result
        }
        await this.autoSaveRecord(record, { silent: true })
      })())
      Promise.all(tasks).finally(() => {
        this.ensureCurrentPageContainsFilter()
      })
    },
    openBulkModal(group) {
      if (!group) return
      const records = group.indexes
        .map(idx => {
          const record = this.preview[idx]
          if (!record) return null
          return {
            index: idx,
            notificationNumber: record.notificationNumber,
            drawingNumber: record.drawingNumber,
            processName: record.processName,
            processCode: record.processCode,
            hours: record.hours,
            planQty: record.planQty
          }
        })
        .filter(Boolean)
      this.bulkModal = {
        visible: true,
        type: group.type,
        group,
        records
      }
    },
    closeBulkModal() {
      this.bulkModal = {
        visible: false,
        type: '',
        group: null,
        records: []
      }
    },
    async applyBulkUpdates(payload) {
      if (!payload || !payload.type || !Array.isArray(payload.updates)) {
        this.closeBulkModal()
        return
      }
      const { type, updates } = payload
      const tasks = updates.map(item => (async () => {
        if (!item || typeof item.index !== 'number') return
        const record = this.preview[item.index]
        if (!record) return
        let shouldSave = true
        switch (type) {
          case '单件工时': {
            const value = item.hours
            record.hours = value === '' || value === null || Number.isNaN(Number(value)) ? null : Number(value)
            this.checkHours(record)
            break
          }
          case '工序代码': {
            const code = this.normalizeProcessCode(item.processCode)
            record.processCode = code
            record.serverCodeMissing = !code
            record.codeMissing = !code
            if (!code) {
              record.barcode = ''
              record.barcodeImage = ''
              this.updateIssueFlags(record)
            } else {
              const name = record.processName != null ? String(record.processName).trim() : ''
              if (name && (!this.processCache[name] || this.processCache[name] !== code)) this.$set(this.processCache, name, code)
              const previousBarcode = record.barcode
              const previousImage = record.barcodeImage
              await this.updateBarcode(record)
              const success = await this.rememberProcessCode(record)
              if (!success) {
                record.barcode = previousBarcode
                record.barcodeImage = previousImage
                shouldSave = false
              } else {
                record.serverCodeMissing = false
                record.codeMissing = false
              }
            }
            break
          }
          case '工序': {
            const name = item.processName != null ? String(item.processName).trim() : ''
            record.processName = name
            await this.updateProcess(record)
            break
          }
          case '计划数': {
            const plan = item.planQty
            record.planQty = plan === '' || plan === null || Number.isNaN(Number(plan)) ? null : Number(plan)
            this.handlePlanInput(record)
            break
          }
          default:
            break
        }
        if (shouldSave) {
          await this.autoSaveRecord(record, { silent: true })
        }
      })())
      try {
        await Promise.all(tasks)
      } finally {
        this.closeBulkModal()
        this.ensureCurrentPageContainsFilter()
      }
    }
  }
}
</script>
