# 漂浮載具隱私政策

最後更新日期：2026 年 8 月 28 日

本隱私政策適用於「漂浮載具」（Taiwan Vehicle Barcode Floating）Android 應用程式。

## 1. 開發者資訊

- 開發者：rsps1008
- 專案：[GitHub Repository](https://github.com/rsps1008/Taiwan-Vehicle-Barcode-Floating)
- 聯絡方式：[GitHub Issues](https://github.com/rsps1008/Taiwan-Vehicle-Barcode-Floating/issues)

## 2. App 會處理哪些資料

App 只處理你主動輸入或操作所需的資料：

- 手機條碼載具號碼。
- 浮窗位置、展開透明度、關閉行為與小工具點擊行為等偏好設定。
- 當你選擇小工具的複製功能時，會將已儲存的載具號碼寫入 Android 剪貼簿。

## 3. 資料如何使用與儲存

上述資料只用於顯示條碼、維持浮窗狀態、更新桌面小工具及完成複製操作。資料以 Android `SharedPreferences` 儲存在你的裝置本機；本專案沒有由開發者營運的伺服器，也不會將資料上傳至我們的伺服器。

## 4. 權限與系統功能

- Overlay 權限只用於在其他 App 上方顯示可拖曳的浮動條碼視窗。
- 前景服務權限只用於維持浮窗服務運作。
- Android 13 以上的通知權限只用於前景服務通知；小工具複製結果以系統 Toast 回饋。
- 剪貼簿只在你啟用「複製載具號碼」時使用。

App 不要求網路、定位、相機、麥克風、聯絡人、簡訊、電話紀錄、相簿或廣告識別碼等權限，亦不包含廣告、分析或追蹤功能。

## 5. 第三方服務與資料分享

App 不使用登入、雲端備份或第三方資料服務。我們不販售、不出租，也不會為廣告或行銷目的分享你的資料。Android 作業系統與裝置上其他 App 對剪貼簿的處理，仍受其各自的系統行為與政策約束。

## 6. 資料刪除

你可以在 App 設定中修改載具號碼與各項偏好；解除安裝 App 或清除 App 資料，會依 Android 系統行為移除本機設定。已複製到剪貼簿的內容，需依你的 Android 版本與剪貼簿管理功能自行清除。

## 7. 兒童隱私

本 App 不以 13 歲以下兒童為主要對象，也不會主動蒐集兒童個人資料。

## 8. 政策變更與聯絡方式

若功能、法規或上架要求有所變更，我們會更新本頁的最後更新日期與內容。若你對隱私或資料處理有疑問，請透過 [GitHub Issues](https://github.com/rsps1008/Taiwan-Vehicle-Barcode-Floating/issues) 聯絡。

本專案採用 [MIT License](LICENSE)。
