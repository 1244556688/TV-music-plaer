# TV Minimalist Music Player (電視極簡音樂播放器 🌌)

這是一款專為 Android TV 平台量身打造的**極簡太空星空主題音樂播放器**。採用現代化的 **Jetpack Compose** 進行 UI 設計，並結合 **Room Database** 實現本機曲目持久化儲存。

為了保護電視螢幕並提供極致的視覺享受，本播放器內建了**星空動態背景**與**防 OLED 烙印 (Burn-In) 的螢幕保護程式**。

---

## 🌟 特色功能 (Features)

1. **📥 初始空白播放清單與靈活匯入 (Empty State & Audio Importing)**
   - 一開始播放清單為空白，讓使用者完全自主管理。
   - 支援一鍵**匯入電視本機音訊檔案**（自動解析歌曲名稱、歌手與時長資訊）。
   - 支援**手動新增歌曲**，自訂曲風、歌名、時長與演出者。
   - 內建「太空合成器音樂套件」，可一鍵加載預設的迷幻太空音軌。

2. **📺 Android TV 遙控器 100% 完美導航 (D-pad Focus Friendly)**
   - 所有按鈕、卡片與輸入框皆為電視遙控器（D-pad）量身定製。
   - 當焦點移至目標時，會呈現耀眼的**霓虹藍/霓虹紫呼吸外框**與動態微縮放視覺回饋。

3. **💤 OLED 螢幕防烙印保護程式 (Anti-Burn-In Screensaver)**
   - 在無操作 30 秒後，自動進入**漂浮星空大時鐘與歌曲資訊**保護介面。
   - 資訊卡片會以極其緩慢的 Sine 波形隨機漂浮移動，確保像素點不持續發光，防止高階 OLED 電視留下殘影。
   - 只要按下遙控器任意按鍵，即可立刻喚醒並回到主播放介面。

4. **🗄️ Room Database 本地資料庫持久化**
   - 採用 Android 官方推薦的 Room 元件進行資料存儲。
   - 匯入或手動建立的歌曲清單，即使電視關機、應用程式重啟也絕不遺失。

5. **🎛️ 進階播放控制控制台**
   - 支援隨機播放 (Shuffle)、循環播放模式切換 (無循環、單曲循環 `1`、全清單循環)。
   - 專為遙控器設計的分段式音量調整滑軌。
   - 播放時，左側清單會顯示絢麗的**即時動態音訊頻譜動畫**。

---

## 🛠️ 開發與建置環境 (Tech Stack)

- **語言：** Kotlin 100%
- **UI 架構：** Jetpack Compose (Material 3)
- **資料庫：** Room Database (KSP 支援)
- **架構模式：** MVVM (Model-View-ViewModel)
- **相容性：** Android 8.0 (API 26) 及以上，支援觸控螢幕、一般手機與 Android TV / Google TV。

---

## 🚀 如何在 GitHub 發布與本地打包 (How to Build & Release)

### 1. 複製本專案
```bash
git clone <您的 GitHub 專案網址>
```

### 2. 使用 Android Studio 開啟
- 下載並安裝最新版 **Android Studio (Ladybug 2024.2.1 或更新版本)**。
- 點擊 `File -> Open`，選擇本專案根目錄。
- Android Studio 將自動偵測 `build.gradle.kts` 並下載所需依賴。

### 3. 如何打包產生 APK 上傳至 GitHub Release
在 Android Studio 中，您可以輕鬆打包成 APK：

#### 🔹 方法 A：產生除錯版 APK (Debug APK)
1. 在 Android Studio 右側點擊 **Gradle** 面板。
2. 展開 `app -> Tasks -> build`。
3. 雙擊 **`assembleDebug`** 任務。
4. 編譯完成後，APK 檔案會儲存在：  
   `app/build/outputs/apk/debug/app-debug.apk`
5. 您可以直接將此 APK 上傳至您的 GitHub Release 頁面。

#### 🔹 方法 B：產生簽署的發行版 APK (Signed Release APK)
若要將程式發布給大眾，建議打包發行版：
1. 點擊頂部選單的 **`Build -> Generate Signed Bundle / APK...`**。
2. 選擇 **`APK`**，然後點擊 Next。
3. 建立或選擇您的金鑰庫儲存庫 (`Key store path`)、輸入密碼、別名。
4. 選擇 **`release`** 建置變體。
5. 點擊 Finish，編譯完後即可在 `app/release/` 資料夾下取得正式發行版 APK。

---

## 📂 專案核心目錄結構 (Project Structure)

```text
com.example/
├── data/
│   ├── Song.kt                  # 歌曲資料模型與預設曲目定義
│   ├── SongDao.kt               # Room 數據訪問接口 (DAO)
│   ├── MusicDatabase.kt         # Room 資料庫實體
│   └── SongRepository.kt        # 歌曲儲存庫資料調度中心
├── ui/
│   ├── theme/
│   │   ├── Color.kt             # 電視專用霓虹配色表
│   │   └── Theme.kt             # Material 3 主題設定
│   ├── components/
│   │   ├── TVFocusable.kt       # 遙控器焦點擴充 Modifiers
│   │   ├── TVPlayerLayout.kt    # 主播放器介面 (清單、操作盤、音樂對話框)
│   │   ├── TVScreensaverLayout.kt# OLED 防烙印保護程式與星空畫布
│   │   └── VinylRecord.kt       # 精美旋轉黑膠唱片元件
│   └── MainPlayerViewModel.kt   # 核心播放控制器與 Room 狀態訂閱 VM
└── MainActivity.kt              # 程式主入口及系統本機音訊選取器邏輯
```

---

## 📝 GitHub 專案發布小撇步
1. **GitHub Repository 說明：** 建議將此 README.md 內容直接放入您 GitHub 專案的 `README.md`。
2. **上傳 APK 說明：** 在 GitHub Releases 中，除了上傳原始碼壓縮包外，手動拖入打包好的 `app-debug.apk` 可以大大方便電視使用者直接透過電視瀏覽器（如 Downloader）安裝體驗！

🌌 *祝您在電視大螢幕上聆聽太空星空迷幻樂章愉快！*
