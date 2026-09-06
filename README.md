# Serein Day

一个温柔、纯净的 Android 倒数日应用，采用 **Serene Forest Mint**（薄荷森林绿 · Material 3 Expressive）视觉体系，整体保持简约的 Apple 风格。设计稿见 `stitch_serein_day_countdown(1)/`（`stitch_serein_day_countdown/` 为早期紫色风格参考稿）。

## 已实现

- **倒数本**：事件归属倒数本管理（默认：纪念日 / 生日 / 旅行 / 考试）；Home 左上角菜单切换倒数本，「管理倒数本」支持新建、重命名、删除（删除时事件移入首个剩余倒数本）
- **首页 Home**：置顶里程碑大卡（封面图 / 渐变、进度环、实时天数、最新小记摘要）、「即将到来」聚合卡、日程时光列表（农历 · 每年 / 小记数徽标）、日辰心绪语录卡、极简滚动指示条
- **新建/编辑 Add Countdown**：实时卡片预览、封面图片（系统相册选择，复制副本与原图脱钩）、名称（24 字限制）、**公历 / 农历切换 + 每年重复**、提醒开关、置顶优先级滑杆（普通归档 / 次要关注 / 首页大卡置顶）、所属倒数本
- **详情 Countdown Detail**：默认只显示倒数卡片，点「编辑」展开全部；里程碑卡、进度条、提醒状态 / 所属倒数本 / 创建时间信息卡、**小记**（时间流：当天内可编辑、跨天仅可删，发布/编辑/删除、最新在上/编年体排序，与桌面版 serein-day-for-rust 完全一致）、分享卡片、封存、彻底删除（两步确认）
- **归档 Archive**：归档事件冻结只读，可恢复或彻底删除
- **设置 Settings**：8 套配色预设（柔紫/赭红/靛蓝/灰绿/蜜橙/玫红/青碧/石墨）+ **自定义主色**（色相/饱和度滑杆实时预览）、浅色 / 深色 / 跟随系统、**常驻通知**（通知栏常驻显示置顶倒数卡，跨午夜自动刷新）、倒数本管理入口、数据备份（JSON 分享）、周起始日选择器、触感反馈
- 旧版数据自动迁移：旧分类 → 倒数本，旧「心愿与备忘」→ 第一条小记，旧 pinned → 置顶

## 已知限制

- 「开启提醒」目前仅保存开关状态并在详情展示，尚未实现到点系统通知推送（后续版本计划）

## 构建与运行

```bash
set JAVA_HOME=C:\Users\ranly\scoop\apps\openjdk21\current
gradlew.bat assembleDebug
```

农历转换基于系统 ICU4J（`android.icu.util.ChineseCalendar`），锚点测试在 `app/src/androidTest`，可执行 `gradlew.bat connectedDebugAndroidTest` 验证（需已启动的模拟器）。

或用 Android Studio 打开本文件夹直接运行。编译 SDK 为 Android 36，最低支持 Android 8.0（API 26）。

### 本机测试模拟器

系统镜像（Android 37.1 x86_64）已注册到 SDK，并创建了名为 `SereinTest` 的 AVD（Pixel 6 规格），可用下面命令启动：

```bash
emulator -avd SereinTest
```

安装调试包：`adb install app\build\outputs\apk\debug\app-debug.apk`。

注意：该模拟器镜像存在平台缺陷，`adb shell pm grant` 无法授予 `POST_NOTIFICATIONS`，验证常驻通知需在系统设置里手动允许。
