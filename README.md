# Serein Day

一个简约的 Android 倒数日应用，采用 **Serene Forest Mint**（薄荷森林绿 · Material 3）视觉体系，交互向 Days Matter（倒数日）看齐：分类切换、大号天数、纯列表、极简模式。设计稿见 `stitch_serein_day_countdown(1)/`（`stitch_serein_day_countdown/` 为早期紫色风格参考稿）。

## 已实现（v1.2.0 极简版）

- **倒数本（Days Matter 式分类）**：首页标题下方横滑分类 chips（全部 / 各倒数本 / 管理）直接切换；「管理倒数本」支持新建、重命名、删除（删除时事件移入首个剩余倒数本）
- **首页 Home**：置顶事件自动成为里程碑大卡（封面图 / 渐变、进度环、实时天数、最新小记摘要，置顶带徽标）；其余事件按「今天 / 未来 / 已过去」分组展示；极简滚动指示条
- **排序方式**：设置页可选 按剩余天数 / 按目标日期 / 按添加时间，置顶始终最前
- **新建/编辑 Add Countdown**：名称（24 字限制）、**公历 / 农历切换 + 每年重复**（实时显示距今天数）、倒数本选择、提醒开关、**置顶开关**（替代旧的三档权重滑杆）、封面图片与详情页背景设置
- **图片来源三通道**：拍照（FileProvider）/ 系统相册（Photo Picker）/ 文件（DocumentsUI，绕开部分 ROM 相册的裁剪劫持），另有「恢复默认」移除图片；图片复制为应用私有副本，与原图脱钩
- **详情 Countdown Detail**：整页可设置背景壁纸（带压暗遮罩，右上角小圆钮随时更换/恢复）；里程碑卡 + 进度条；**小记**（随时可编辑可删除、最新在上）；分享卡片、封存、彻底删除（两步确认）
- **极简模式**：设置页开关。开启后首页变纯列表（名称 + 日期 + 大号天数，分组展示），编辑页只留 名称/日期/倒数本/置顶，详情页只留倒数卡与操作
- **归档 Archive**：从首页右上角进入；归档事件冻结只读，可恢复或彻底删除
- **设置 Settings**：8 套配色预设（柔紫/赭红/靛蓝/灰绿/蜜橙/玫红/青碧/石墨）+ **自定义主色**（色相/饱和度滑杆实时预览）、浅色 / 深色 / 跟随系统、极简模式、排序方式、倒数本管理、**常驻通知**（通知栏常驻显示置顶倒数卡，跨午夜自动刷新）、触感反馈、数据备份（JSON 分享）
- 底部导航仅「首页 / 设置」两个 tab；旧版数据自动迁移：旧分类 → 倒数本，旧「心愿与备忘」→ 第一条小记，旧 pinned/优先级 → 置顶


## 已知限制

- 「开启提醒」目前仅保存开关状态，尚未实现到点系统通知推送（后续版本计划）

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
