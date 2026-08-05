# 生活台 · LifeHub（原生 Android 版）

把 `life-hub.html` 单文件网页重构为**纯原生 Android 应用**（Kotlin + Jetpack Compose + Room），不使用任何 WebView。覆盖原版 7 大模块，并通过 GitHub Actions 流水线打包 APK。

## 技术栈

- **语言**：Kotlin 1.9.24
- **UI**：Jetpack Compose（Material3）+ Material Icons Extended
- **导航**：Navigation-Compose
- **持久化**：Room 2.6.1（8 张表）+ DataStore（应用配置）
- **架构**：`AndroidViewModel` + `Repository` + `AppContainer` 依赖持有
- **序列化**：kotlinx-serialization-json（全量备份/导入）
- **构建**：Gradle 8.7 + AGP 8.5.2 + KSP
- **最低 SDK**：26（Android 8.0，适配自适应图标）／ 目标 SDK：34

## 模块

| 模块 | 路由 | 说明 |
|---|---|---|
| 首页 Home | `home` | 生活指数环形图、今日待办、快捷入口、备份导入导出 |
| 记账 Ledger | `ledger` | 支出/收入/返利、分类统计、月度柱图、环形占比 |
| 习惯 Habit | `habit` | 习惯打卡、30 日热力图、连续天数 |
| 健身 Fitness | `fitness` | 体重曲线（含 7 日均线）、训练周计划、热量缺口 |
| 日程 Schedule | `schedule` | 今日/本周/待办/已完成、逾期提醒、优先级 |
| 待买 Wishlist | `wishlist` | 优先级 P0/P1/P2、「已买并记账」联动账本 |
| 书影音 Media | `media` | 书/影/音收藏、封面墙/列表、星级评分、年度分布 |

## 项目结构

```
life-hub-native/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/lifehub/
│       │   ├── MainActivity.kt
│       │   ├── LifeHubApplication.kt        # AppContainer（依赖持有）
│       │   ├── charts/DrawCharts.kt         # 环形/折线/柱状/热力图
│       │   ├── data/                         # Room: entity/dao/repository
│       │   ├── ui/                           # theme/components/<module>Screen/navigation
│       │   ├── util/                         # DateUtils/Money/JsonBackupUtil
│       │   └── viewmodel/                    # 每个 ViewModel + Factory
│       └── res/                              # colors/strings/themes/launcher
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── .github/workflows/build.yml               # CI：构建并签名 APK
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew / gradlew.bat
└── .gitignore
```

## 本地构建

需 JDK 17。

```bash
# Debug APK
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk

# Release APK（未配置签名时产出 unsigned）
./gradlew assembleRelease
```

## CI 流水线（GitHub Actions）

`.github/workflows/build.yml` 在 push 到 `main`/`master` 或 PR 时自动运行：

1. 检出代码 → JDK 17 → Gradle 8.7
2. 若配置了签名 Secrets → 构建**已签名 release APK**；否则构建 **debug APK**
3. 上传为 Artifact（`lifehub-apk`，保留 30 天）

### 签名配置（可选，让 CI 产出可安装的 release 包）

1. 本地生成密钥库：
   ```bash
   keytool -genkey -v -keystore release.keystore -alias lifehub \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Base64 编码：
   ```bash
   base64 -i release.keystore -o keystore.b64   # macOS
   # Linux: base64 -w 0 release.keystore > keystore.b64
   ```
3. 在 GitHub 仓库 → Settings → Secrets and variables → Actions 添加 4 个 Secret：
   - `KEYSTORE_BASE64`：上一步的 base64 内容
   - `KEYSTORE_PASSWORD`：密钥库口令
   - `KEY_ALIAS`：别名（如 `lifehub`）
   - `KEY_PASSWORD`：别名口令
4. 触发流水线（push 或手动 `workflow_dispatch`），从 Artifacts 下载签名 APK。

> 密钥库文件本身**绝不提交**（`.gitignore` 已排除 `*.jks` / `*.keystore` / `keystore.properties`）。

## 数据备份

首页支持全量 JSON 备份/导入（基于 Android SAF 选文件），覆盖全部 8 张表，可跨设备迁移。
