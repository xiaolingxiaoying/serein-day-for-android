# Android / Xiaomi 小组件尺寸调研

## 结论

Redmi K70 上组件看起来高于宽，主要不是 `widget_countdown.xml` 的布局错误，而是桌面启动器将 `2x2` 解释为两个横向网格单元和两个纵向网格单元。网格单元的宽高可以不同，因此 2x2 不等于正方形。

小米官方 FAQ 明确说明：`appwidget-provider` 中的 `minHeight` 只用于计算 Y 轴需要占用的格子数，并不等于最终展示高度；例如桌面格子为 70x80dp 时，110dp 会按 `ceil(110 / 80) * 80` 计算为两行，最终高度还会受到桌面 padding 影响。

## 项目现状

`app/src/main/res/xml/countdown_widget_info.xml` 当前声明：

```xml
android:minWidth="110dp"
android:minHeight="110dp"
android:minResizeWidth="110dp"
android:minResizeHeight="110dp"
android:targetCellWidth="2"
android:targetCellHeight="2"
android:resizeMode="none"
```

Android 12 及以上优先使用 `targetCellWidth` / `targetCellHeight`；旧版本或不完整支持该属性的宿主会使用 `minWidth` / `minHeight`。这些属性定义的是默认占用约束，不是像素级外框尺寸。

`widget_countdown.xml` 的根布局和主要内容均为 `match_parent`，所以它会填满 Xiaomi Launcher 分配的整个 2x2 区域。当前代码没有按实际宽高做响应式布局。

## 官方依据

- Android 官方说明：小组件添加到桌面后，通常会占用比声明的最小宽高更多的空间，并会按启动器网格向上取整。<https://developer.android.com/develop/ui/views/appwidgets/layouts>
- Android 官方说明：Android 12+ 使用 `targetCellWidth` / `targetCellHeight` 作为默认网格尺寸，`minWidth` / `minHeight` 是兼容和约束值。<https://developer.android.com/develop/ui/compose/glance/create-app-widget>
- Xiaomi HyperOS 官方设计规范：手机小部件支持 2x2、4x2、4x4，且不同屏幕、桌面布局下尺寸会自适应变化。<https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1664>
- Xiaomi HyperOS 官方 FAQ：`minHeight` 只用于计算 Y 轴格子数；70x80dp 网格下，110dp 会按 `ceil(110 / 80) * 80` 计算。<https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1591>

## 修复建议

### 推荐方案：保留 2x2，按 Xiaomi 的非正方形 2x2 区域适配内容

1. 保留 `targetCellWidth="2"` 和 `targetCellHeight="2"`。
2. 不再把 2x2 误认为正方形，允许根卡片充满实际区域。
3. 缩小或按实际高度调整内部垂直间距、46sp 大数字和上下 padding。
4. 使用 `AppWidgetManager.getAppWidgetOptions()` 读取宿主提供的 dp 尺寸，在 `onAppWidgetOptionsChanged()` 和更新逻辑中按尺寸选择小、中布局。

这是最符合 Android 和 Xiaomi 官方规范的方案，因为外层小组件大小由宿主网格决定，应用只能适配内容，不能强制 Launcher 把网格单元变成正方形。

### 如果产品必须显示为正方形卡片

可以保留外层 2x2 占位，但把黑色背景、图片和内容放进一个居中的正方形内层容器；外层剩余区域使用桌面背景色或透明。这个方案只能让视觉卡片接近正方形，不能改变小组件实际占用的 2x2 矩形空间。

### 不推荐方案：只把 `minHeight` 改小

例如改为 80dp 可能让某些宿主只计算为一行，导致组件变成 2x1；在 Android 12+，`targetCellHeight="2"` 还可能继续优先生效。因此单独修改 `minHeight` 不能稳定修复 Redmi K70 上的比例问题。
