# RecordUpload 组件逻辑概览

本文梳理 `RecordUpload.vue` 中和“解析表格、生成二维码、分页+懒加载”相关的核心流程，方便排查上传大文件时的行为。

## 1. 上传与解析流程

1. 选择文件后点击“解析”会执行 `parse()`：
   - 校验是否已登录（`requireUserHeaders()`）并展示进度条状态。 
   - 以 `FormData` 形式提交 `/api/api/workrecords/parse?store=true`，后端返回 `fileId` 与总数、缺失项统计。【F:frontend/src/components/RecordUpload.vue†L507-L549】
   - 解析成功后刷新文件列表、图号分桶，并加载首个图号第一页的分页数据；失败则经 `handleRequestError` 弹出“解析失败”。【F:frontend/src/components/RecordUpload.vue†L548-L567】
2. `fetchDrawings()` 请求 `/workrecords/file/{fileId}/drawings`，将后端统计的每个图号及起始行号映射到本地 `drawings` 队列，为分页准备导航信息。【F:frontend/src/components/RecordUpload.vue†L418-L455】
3. `fetchPage()` 基于当前图号与页码调用 `fetchPageData()`：
   - 请求 `/workrecords/file/{fileId}/page` 获得单页内容与总页数。
   - 使用 `decorateRecord()` 标记缺失字段、生成本地 `_localKey`，然后触发条码懒加载。【F:frontend/src/components/RecordUpload.vue†L365-L411】【F:frontend/src/components/RecordUpload.vue†L333-L344】

## 2. 表格数据与二维码生成

- `decorateRecord()` 会标准化每条记录：剔除条码中的不可见字符、初始化缺失标记、准备 `_dirty` 与 `_localKey`，确保新增行和接口返回的数据在表格中可追踪。【F:frontend/src/components/RecordUpload.vue†L333-L344】
- 更新工序时 (`updateProcess`) 会：
  1. 确保工序缓存已加载；
  2. 按名称匹配工序码，找不到时再向 `/processcodes/name/{name}` 请求；
  3. 设置 `processCode` 与缺失标记后调用 `updateBarcode()` 重新生成条码字符串。【F:frontend/src/components/RecordUpload.vue†L568-L617】
- `updateBarcode()` 会拼接“图号-通知单号-工序代码”，经 `sanitize()` 去除特殊字符后：
  - 若本地 `barcodeCache` 已有对应图片直接复用；
  - 否则调用 `/workrecords/generateBarcode` 获取单个条码图片并缓存；
  - 缺少必要字段时清空条码与图片。【F:frontend/src/components/RecordUpload.vue†L618-L656】
- `loadBarcodes()` 用于批量懒加载：
  - 收集当前记录中尚未缓存的条码值，去重后批量请求 `/workrecords/generateBarcodes`。
  - 将返回的 Base64 图片写入缓存并同步更新列表中的记录，避免重复网络请求。【F:frontend/src/components/RecordUpload.vue†L657-L688】

## 3. 分页与懒加载策略

- 组件的分页状态由 `drawings`（图号集合）、`currentDrawingIndex`、`pageNo` 与 `pageSize` 管理。顶部导航支持上一/下一图号与页码跳转，并可通过关键字查找图号后重新加载当前页。【F:frontend/src/components/RecordUpload.vue†L53-L124】【F:frontend/src/components/RecordUpload.vue†L730-L777】
- `fetchPage()` 每次只拉取当前页的数据，渲染完成后在 `nextTick` 中调用 `loadBarcodesForCurrentPage()`，确保条码图片按需加载而不是一次性拉取所有页。【F:frontend/src/components/RecordUpload.vue†L365-L411】【F:frontend/src/components/RecordUpload.vue†L689-L695】
- 打印时 `collectPrintPages()` 会按图号分页循环，通过与分页 API 相同的方式逐页取数、装饰记录，并在最后一页补齐空白行，确保打印内容铺满一页；条码图片同样在进入打印队列时懒加载。【F:frontend/src/components/RecordUpload.vue†L778-L852】

以上逻辑共同保证：
- 上传解析阶段只处理一份 `FormData`，成功后按图号分页加载数据；
- 条码图片使用缓存与懒加载策略降低并发压力；
- 大文件也只在前端保留当前页，避免一次性渲染几万行带来的性能瓶颈。
