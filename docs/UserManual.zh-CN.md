# 工时管理系统使用手册

## 1. 登录与主界面导航
- 打开系统后首先进入登录页，填写用户名与密码并点击“登录”，表单会在提交时禁止重复点击并在失败时展示服务端返回的错误信息，登录成功后会把登录状态与用户名写入浏览器本地存储。登录成功会触发父组件切换到主界面。【F:frontend/src/components/LoginPage.vue†L1-L65】
- 登录后顶部导航条提供 Excel 上传、扫码录入、人员管理、工序代码和操作记录五个功能入口，右侧的“退出登录”会清理本地存储并返回登录页。主容器通过 `<router-view>` 承载各业务页面，并在子页面保存成功时触发刷新。【F:frontend/src/App.vue†L1-L52】

## 2. Excel 上传与预览
- “Excel 上传”页顶部可选择本地文件进行解析，也可以从下拉框加载历史文件或删除文件，提供“清除0工序”“保存”“打印”等批量操作按钮，并在解析大文件时显示进度条反馈。【F:frontend/src/components/RecordUpload.vue†L4-L24】【F:frontend/src/components/RecordUpload.vue†L171-L178】
- 解析接口会将 Excel 转换后的记录分批写入数据库并返回对应的文件 ID、缺陷统计和记录列表，保证预览的每一行都已经持久化；上传记录同时会生成 `UploadedFile` 记录并在服务端统计缺少工序代码/工时的行数。【F:backend/src/main/java/com/example/worktime/controller/WorkRecordController.java†L587-L650】
- 历史文件加载后会按图号分组渲染，支持快速跳页、图号搜索和“清除0工序”过滤；加载时会调用缺陷检测并为每条记录构建分页结构。【F:frontend/src/components/RecordUpload.vue†L34-L168】【F:frontend/src/components/RecordUpload.vue†L528-L555】
- 表格内可直接编辑计划数、单件工时、工序代码/名称等字段，行会根据缺失字段标红并展示缺陷摘要；缺陷检测会同步维护“缺少通知单号/工序代码/条形码”等状态以提示补录项。【F:frontend/src/components/RecordUpload.vue†L90-L157】【F:frontend/src/components/RecordUpload.vue†L439-L488】
- 任何单元格修改都会触发自动保存，组件会构造精简载荷调用 `/api/workrecords/autosave`，并在服务端补齐条码、补录标记与缺陷状态后回写最新字段值，避免重复提交或丢失修改。【F:frontend/src/components/RecordUpload.vue†L705-L793】【F:backend/src/main/java/com/example/worktime/controller/WorkRecordController.java†L409-L478】
- 点击“保存”会筛出已填写工序代码和条形码的记录，按 1000 条分批提交给后端，后端根据条码去重标记补录状态并写库；保存完成后会刷新文件列表并发出提示。【F:frontend/src/components/RecordUpload.vue†L631-L665】【F:backend/src/main/java/com/example/worktime/controller/WorkRecordController.java†L549-L585】
- “打印”按钮会预先加载全部条形码，将所有分页渲染到 DOM 后调用 `window.print()`，同时配合打印样式把页脚显示成“图号：XXX”，对齐 A4 页边距并在页面底部左侧覆盖浏览器默认 URL。【F:frontend/src/components/RecordUpload.vue†L666-L687】【F:frontend/src/components/RecordUpload.vue†L753-L757】【F:frontend/src/assets/style.css†L325-L368】

## 3. 缺陷追踪与批量处理
- 左侧“缺陷追踪”面板会将所有缺陷按照图号与缺陷类型聚合，显示每组缺陷数量，点击可在主表格中过滤出对应行，也可以一键退出筛选。【F:frontend/src/components/RecordUpload.vue†L25-L55】【F:frontend/src/components/RecordUpload.vue†L309-L338】【F:frontend/src/components/RecordIssuePanel.vue†L1-L48】
- 针对缺少工序代码、工序名称、单件工时、计划数的缺陷，可通过“批量填写”打开模态框，集中输入并一次性应用到所有筛选行，便于快速补齐数据。【F:frontend/src/components/RecordIssuePanel.vue†L13-L38】【F:frontend/src/components/BulkIssueModal.vue†L1-L81】
- 服务端在解析、加载和保存时会调用 `flagIssues`，对通知单号、产品、工序、工时、条形码等字段逐项检查，并把“仅填写了工序名称或 0 的工序代码”视为缺失，同时清除无效条码，确保缺陷面板与持久化状态保持一致。【F:backend/src/main/java/com/example/worktime/controller/WorkRecordController.java†L671-L734】

## 4. 打印版式与图号页脚
- 打印专用样式在 `@media print` 中启用，取消左右外边距、禁止分页内断行，并为每页底部预留 36px 的空间，用绝对定位的 `.print-page-footer` 显示“图号：XXX”，颜色置灰以模拟打印脚注。【F:frontend/src/assets/style.css†L325-L348】
- 页脚文本来自 `formatPrintFooter`，会先对图号做字符清洗，确保在缺省图号时仍然输出“图号：（空）”，避免打印空白或出现非法字符。【F:frontend/src/components/RecordUpload.vue†L753-L757】

## 5. 扫码录入与补录
- “扫码录入”页支持直接输入/扫码条形码查询，也可加载某个文件的已填记录；工具栏还提供按自然月或图号导出 Excel 的功能键。【F:frontend/src/components/RecordScanner.vue†L3-L47】
- 表格列出了计划数、人员分配、起止日期、合格数等字段，支持在非只读模式下就地编辑并实时计算工时小计、分配值，必要时可新增行或删除行。【F:frontend/src/components/RecordScanner.vue†L49-L160】
- 查询或加载记录时会调用 `processRecords`，对每条数据执行补充（例如拆分人员代码、回填班组、计算合计），并在需要时访问人员库补足姓名信息。【F:frontend/src/components/RecordScanner.vue†L205-L355】
- “保存”“保存全部”“删除”“复制”均走后台接口：单条更新使用 `PUT /api/workrecords/{id}`，批量保存用 `PUT /api/workrecords/bulk`，复制行调用 `/api/workrecords/duplicate/{id}`；保存前会校验数量与工时分配并提示缺失字段。【F:frontend/src/components/RecordScanner.vue†L275-L334】【F:backend/src/main/java/com/example/worktime/controller/WorkRecordController.java†L376-L535】
- 导出功能对应 `/api/workrecords/natural-month/{year}/{month}/export` 与 `/api/workrecords/drawing/{drawing}/export`，后端会自动根据自然月（26 日跨月）或图号筛选记录并生成 Excel。【F:frontend/src/components/RecordScanner.vue†L228-L255】【F:backend/src/main/java/com/example/worktime/controller/WorkRecordController.java†L135-L185】

## 6. 人员管理
- 人员列表支持按工号/姓名搜索、在线编辑字段以及一键保存或删除；“新增人员”按钮会弹出模态框，自动聚焦工号输入并锁定页面滚动，表单仅在工号、姓名填写后允许提交。【F:frontend/src/components/WorkerManager.vue†L1-L150】
- 后端提供 `/api/workers` 及 `/api/workers/search` 查询接口，新增/更新时会校验工号与姓名必填，并把操作记录到日志中。【F:backend/src/main/java/com/example/worktime/controller/WorkerController.java†L17-L73】【F:backend/src/main/java/com/example/worktime/controller/WorkerController.java†L74-L107】

## 7. 工序代码管理
- 页面结构与人员管理类似，可按代号或名称搜索，支持在线编辑大类、内容等字段，并通过模态框新增工序代码，内置冲突提示以便发现重复代号。【F:frontend/src/components/ProcessManager.vue†L1-L188】
- 服务端在新增或更新时会确保代号、名称非空且全局唯一，`ProcessCodeService` 会缓存名称到代号的映射，并建立代号索引供缺陷检测和自动补全使用；`/api/processcodes/remember` 还允许从上传页直接记忆名称-代号配对。【F:backend/src/main/java/com/example/worktime/controller/ProcessCodeController.java†L17-L118】【F:backend/src/main/java/com/example/worktime/service/ProcessCodeService.java†L19-L120】【F:backend/src/main/java/com/example/worktime/service/ProcessCodeService.java†L121-L205】

## 8. 操作记录中心
- 操作记录页提供用户、关键字、日期范围、模块、实体类型/ID、客户端 IP、Trace ID、状态码及耗时区间等筛选项，支持修改每页条数、重置、手动刷新及自动每分钟刷新。【F:frontend/src/components/OperationLog.vue†L3-L135】
- 列表点击可展开详情查看请求方式、路径、实体标识及响应详情，支持复制到剪贴板；相同 Trace ID 的记录会自动分组展示主从关系，便于追踪批量操作。【F:frontend/src/components/OperationLog.vue†L96-L133】【F:frontend/src/components/OperationLog.vue†L220-L236】【F:frontend/src/components/OperationLog.vue†L305-L332】
- 后端分页接口 `/api/logs` 支持上述筛选条件，所有条件都会在查询时构建 JPA Specification；过滤器会在每次请求时记录用户名、模块、摘要、状态码、耗时、客户端、请求/响应体，并附带 Trace ID 与线程上下文中收集到的业务实体信息。【F:backend/src/main/java/com/example/worktime/controller/OperationLogController.java†L27-L90】【F:backend/src/main/java/com/example/worktime/filter/OperationLogFilter.java†L24-L87】【F:backend/src/main/java/com/example/worktime/filter/OperationLogFilter.java†L88-L147】
- 业务代码在执行时可通过 `OperationLogContext` 注入模块、实体、摘要和补充详情，配合 `OperationLogService.log` 写入数据库并阻止重复记录，保证日志详情清晰完整。【F:backend/src/main/java/com/example/worktime/service/OperationLogContext.java†L13-L71】【F:backend/src/main/java/com/example/worktime/service/OperationLogService.java†L17-L36】

## 9. 上传文件管理
- 历史文件下拉列表来自 `/api/files` 接口，它会连同记录数量一起返回；删除文件时会级联清除关联工时记录，并把文件名、删除数量写入操作日志。【F:backend/src/main/java/com/example/worktime/controller/UploadedFileController.java†L30-L48】
- 上传接口允许保留原始 Excel 数据（`store` 参数默认为 true），方便后续下载或审计；文件 ID 会回传给前端用于再次加载或打印。【F:backend/src/main/java/com/example/worktime/controller/WorkRecordController.java†L587-L644】

## 10. 条形码与补录规则
- 系统提供 `/api/workrecords/generateBarcode(s)` 接口生成 Code128 条码图片，上传与自动保存时会对条码进行 Unicode 归一化与空白清理，并在缺少工序代码时自动清空条码，防止错误数据混入。【F:backend/src/main/java/com/example/worktime/controller/WorkRecordController.java†L350-L374】【F:backend/src/main/java/com/example/worktime/controller/WorkRecordController.java†L450-L734】
- 保存或自动保存时若检测到条码在其它记录中已存在，会把该条目标记为补录（supplemental），方便后续统计与打印时区分。【F:backend/src/main/java/com/example/worktime/controller/WorkRecordController.java†L450-L573】

## 11. 业务日志与回溯
- 所有与工时记录、人员、工序、文件相关的新增/修改/删除操作都会在对应控制器内设置模块、实体 ID 与摘要，并借助过滤器追加状态码、耗时、请求体，方便在操作记录中按图号、Trace ID 或实体快速回溯业务动作。【F:backend/src/main/java/com/example/worktime/controller/WorkRecordController.java†L470-L584】【F:backend/src/main/java/com/example/worktime/controller/WorkerController.java†L74-L107】【F:backend/src/main/java/com/example/worktime/controller/ProcessCodeController.java†L62-L118】【F:backend/src/main/java/com/example/worktime/controller/UploadedFileController.java†L35-L48】【F:backend/src/main/java/com/example/worktime/filter/OperationLogFilter.java†L24-L147】
