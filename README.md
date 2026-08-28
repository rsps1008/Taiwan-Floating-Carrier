# 漂浮載具（Taiwan Vehicle Barcode Floating）

漂浮載具是一款開源、免費的 Android App，讓你把台灣手機條碼放在螢幕上方，需要結帳時快速展開出示。除了可拖曳的浮動視窗，也提供桌面小工具，支援直接開啟浮窗或複製載具號碼。

## 功能特色

- 以浮動視窗顯示手機條碼與載具號碼。
- 浮窗可在展開與收合狀態間切換，並可拖曳到方便的位置。
- 輸入 7 碼載具號碼後自動補上開頭的 `/`。
- 可調整展開狀態的透明度，並自訂關閉按鈕行為。
- 提供 3x1 至 5x1 可調整大小的桌面小工具。
- 小工具點擊可選擇開啟浮窗，或將完整載具號碼複製到剪貼簿並顯示 Toast。
- 條碼由 ZXing 產生，浮窗與小工具共用相同的條碼生成邏輯。

## 使用方式

1. 從 GitHub 下載或 clone 專案。
2. 使用 Android Studio 開啟專案。
3. 在 App 首頁輸入 7 碼手機條碼，並允許「在其他應用程式上層顯示」。
4. 點擊「啟動浮動視窗」，即可在其他 App 上方快速使用條碼。
5. 若要從桌面快速使用，可加入「漂浮載具」小工具，再於設定中選擇點擊動作。

## 權限說明

- `SYSTEM_ALERT_WINDOW`：顯示可拖曳的浮動條碼視窗。
- `FOREGROUND_SERVICE` 與 `FOREGROUND_SERVICE_SPECIAL_USE`：維持浮窗服務運作。
- `POST_NOTIFICATIONS`：Android 13 以上用於顯示前景服務通知；拒絕後不影響核心條碼功能。

App 不需要網路、定位、相機、麥克風、聯絡人或電話等權限。詳細內容請參閱[隱私政策](PRIVACY_POLICY.md)或[網站上的隱私政策](https://TaiwanFloatingCarrier.rsps1008.ru/privacy-policy/)。

## 技術架構

- Kotlin、Android Views/XML、AndroidX AppCompat、Material Components
- Android API 28 以上；compileSdk 35、targetSdk 35
- SharedPreferences 儲存載具號碼、浮窗位置、透明度與使用者偏好
- ZXing Core 與 JourneyApps ZXing Android Embedded 產生條碼

## 編譯與執行

需求：Android Studio、JDK 17、Android SDK API 35。

Windows PowerShell：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

## 授權

本專案採用 [MIT License](./LICENSE)。

## 相關連結

- [專案網站](https://TaiwanFloatingCarrier.rsps1008.ru/)
- [隱私政策](./PRIVACY_POLICY.md)
- [GitHub Issues](https://github.com/rsps1008/Taiwan-Vehicle-Barcode-Floating/issues)
