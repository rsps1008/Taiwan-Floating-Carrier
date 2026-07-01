# AGENTS.md

## 專案摘要
- 專案名稱：`TaiwanBarcodeFloating`
- 這是一個 Android 單一 `app` 模組專案。
- 主要功能：
  - 以前景服務顯示可拖曳的懸浮視窗
  - 產生並顯示載具條碼
  - 提供 widget 顯示條碼與複製載具號碼
  - 透過主畫面調整載具號碼、透明度、關閉行為與 widget 點擊行為

## 主要檔案位置
- `app/src/main/java/com/rsps1008/floatingcarrier/MainActivity.kt`
  - 主設定畫面、Overlay 權限提示、啟動浮窗服務
- `app/src/main/java/com/rsps1008/floatingcarrier/FloatingViewService.kt`
  - 懸浮視窗的顯示、拖曳、收合、條碼刷新與關閉行為
- `app/src/main/java/com/rsps1008/floatingcarrier/CarrierBarcodeGenerator.kt`
  - 載具條碼生成邏輯
- `app/src/main/java/com/rsps1008/floatingcarrier/CarrierWidgetProvider.kt`
  - App Widget 更新、點擊行為、複製載具號碼
- `app/src/main/AndroidManifest.xml`
  - 權限、Activity、Service、Widget 註冊
- `app/src/main/res/`
  - 版面、樣式、字串、圖片與 widget 資源

## 修改原則
- 先維持現有行為，再做局部調整，避免不必要的大改動。
- 若需求只指向特定畫面或元件，優先只改那個區域，不要順手改無關模組。
- 涉及儲存設定時，優先沿用既有的 `SharedPreferences` 與現有 key 命名。
- 改動浮窗、widget 或權限流程時，要特別注意：
  - Overlay 權限是否仍能順利進入
  - 前景服務是否仍能啟動與停止
  - widget 點擊與複製流程是否正常
- 新增或調整 UI 時，盡量保持現有專案的 Android View/XML 寫法與視覺語言一致。

## 驗證方式
- 主要驗證命令：
  - `.\gradlew.bat assembleDebug`
- 若修改 Kotlin/Android 核心邏輯，建議再確認：
  - 專案可正常編譯
  - App 啟動後可顯示主畫面
  - 允許 Overlay 後可正常啟動浮窗
  - Widget 更新、點擊、複製行為正常
- 若改到資源檔或版面，還要留意：
  - `AndroidManifest.xml` 的宣告是否同步
  - `res/layout` 與 `res/values` 的引用是否對應正確

## 後續維護規則
- 只要之後有人要求「修改專案」或「詢問這個專案的問題」，都應先閱讀這份 `AGENTS.md`，並在必要時同步更新。
- 當下列內容有變更時，請一併更新本檔：
  - 專案結構
  - 主要功能流程
  - 建置或驗證指令
  - 權限、服務、widget 或儲存規則
  - 命名慣例或重要行為
- 若新增新模組、新流程或新的操作限制，請補充到對應段落，避免文件落後於程式碼。

## 協作備註
- 回答問題時，優先根據這個專案的實際程式碼與目前檔案內容，不要用泛用模板猜測。
- 若發現這份文件與程式碼不一致，請以程式碼為準，並回頭更新此檔。
