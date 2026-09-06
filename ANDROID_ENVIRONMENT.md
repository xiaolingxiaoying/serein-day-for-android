# Android 开发环境说明

更新时间：2026-09-05

## 结论

本机已经具备原生 Android（Kotlin/Java）开发的大部分工具。Android Studio、JDK、Android SDK、编译工具、ADB 和模拟器程序已经安装完成。当前需要完成的是：

1. 将已下载并校验通过的 Android 37.1 x86_64 系统镜像注册到 SDK；
2. 创建一个 AVD 模拟器；
3. 如需连接小米真机，再根据设备管理器提示安装或更新 USB/ADB 驱动。

## 主机信息

| 项目 | 状态 |
|---|---|
| 操作系统 | Windows 11 家庭版，64 位，Build 26200 |
| 处理器 | AMD Ryzen 7 7730U，16 逻辑处理器 |
| 内存 | 约 32 GB |
| C 盘可用空间 | 约 337 GB（检查时） |
| CPU 虚拟化 | 已启用；系统检测到 Hypervisor |
| WSL | WSL2，包含 Ubuntu 22.04、Debian |
| Docker | 已安装 |

## 已安装工具

### Android Studio

安装目录：

```text
C:\Program Files\Android\Android Studio
```

检查到的版本约为 Android Studio 2026.1.3。Studio 自带 JDK 25.0.2，可用于运行 IDE。

### JDK

通过 Scoop 安装 OpenJDK 21：

```text
C:\Users\ranly\scoop\apps\openjdk21\current
```

当前用户环境变量：

```text
JAVA_HOME=C:\Users\ranly\scoop\apps\openjdk21\current
```

### Android SDK

SDK 根目录：

```text
C:\Users\ranly\AppData\Local\Android\Sdk
```

已安装组件：

```text
cmdline-tools;latest
platform-tools 37.0.1
emulator 37.1.11
platforms;android-36.1
platforms;android-37.1
build-tools;36.1.0
build-tools;37.0.0
```

对应的关键程序：

```text
C:\Users\ranly\AppData\Local\Android\Sdk\platform-tools\adb.exe
C:\Users\ranly\AppData\Local\Android\Sdk\emulator\emulator.exe
C:\Users\ranly\AppData\Local\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat
C:\Users\ranly\AppData\Local\Android\Sdk\cmdline-tools\latest\bin\avdmanager.bat
```

当前用户已配置：

```text
ANDROID_HOME=C:\Users\ranly\AppData\Local\Android\Sdk
ANDROID_SDK_ROOT=C:\Users\ranly\AppData\Local\Android\Sdk
```

PATH 应包含：

```text
%JAVA_HOME%\bin
%ANDROID_HOME%\platform-tools
%ANDROID_HOME%\emulator
%ANDROID_HOME%\cmdline-tools\latest\bin
```

## 系统镜像

已下载并解压：

```text
C:\Users\ranly\Downloads\x86_64-ps16k-37.1_r09(1)\x86_64
```

镜像类型：

```text
Android 37.1
Google APIs
16 KB Page Size
x86_64
```

重要文件包括 `system.img`、`vendor.img`、`kernel-ranchu` 和 `source.properties`。

ZIP 校验结果：

```text
文件：x86_64-ps16k-37.1_r09(1).zip
大小：2,178,012,675 字节
SHA-256：11B9682183827379E8D415DB880A30B9EC84D4032650E08E6149B69B30D545A0
7-Zip：Everything is Ok，退出码 0
```

7-Zip 的 `Headers Error` 是格式兼容性警告，但文件 CRC 校验全部通过，且文件大小与官方直链一致。

目前系统镜像还没有正式注册到 SDK 的目标目录，也没有创建 AVD。推荐的 SDK 目标目录是：

```text
C:\Users\ranly\AppData\Local\Android\Sdk\system-images\android-37.1\google_apis_ps16k\x86_64
```

由于本机 CPU 是 AMD x86_64，使用 x86_64 模拟器镜像比 ARM64 镜像更适合硬件加速。手机本身是 ARM64 不影响使用 x86_64 模拟器。

## Platform-Tools 副本

另外下载了一份：

```text
D:\software\platform-tools
```

其中包含 `adb.exe` 和 `fastboot.exe`。但 SDK 目录中已经有一份 Platform-Tools，因此不建议同时把两个目录都加入 PATH，以免调用到不同版本的 `adb`。建议优先使用 SDK 目录中的版本。

## 小米 USB 驱动

已下载并校验官方小米下载中心提供的备用驱动包：

```text
C:\Users\ranly\Downloads\Xiaomi-USB-Driver-official-legacy.rar
```

文件信息：

```text
大小：19,266,468 字节
SHA-256：D6DC4CCC1165D7EACE8AF0D2340ACE319D0E6C46B08DDB377E70365DF8C86E25
7-Zip：Everything is Ok
```

来源：

- [小米官方资源下载中心](https://www.mi.com/c/service/download)
- [官方驱动直链](https://s1.mi.com/images/new816/Driver.rar)

这个驱动包来自旧版 MIUI V4/V5，建议只在 Windows 设备管理器识别异常时尝试安装，不要直接运行未知安装程序。小米官方说明，现代 Xiaomi/Redmi 手机通常由 Windows 10/11 自动提供 MTP 支持；ADB 调试还需要在手机上开启 USB 调试并确认 RSA 授权。

## 真机连接步骤

1. 使用支持数据传输的 USB 线连接手机；
2. 手机 USB 用途选择“文件传输 / Android Auto”；
3. 手机设置中连续点击 MIUI/HyperOS 版本以开启开发者选项；
4. 在开发者选项中开启“USB 调试”；
5. 在电脑终端执行：

```powershell
adb kill-server
adb start-server
adb devices
```

6. 手机出现 RSA 授权提示时选择允许；
7. `adb devices` 显示序列号和 `device` 即连接成功。

## 最终验证命令

重新打开 PowerShell 后执行：

```powershell
java -version
adb version
sdkmanager --version
emulator -version
avdmanager list avd
```

当系统镜像注册完成后，创建 AVD 的示例命令为：

```powershell
avdmanager create avd `
  -n Pixel_Android_37_1_x86_64 `
  -k "system-images;android-37.1;google_apis_ps16k;x86_64" `
  -d pixel_8
```

## 可选组件

- NDK、CMake：只有开发 C/C++、JNI 或依赖原生库时需要；
- Flutter SDK：只有开发 Flutter 时需要；
- 小米或其他厂商专用驱动：只有真机未被 Windows 正确识别时需要；
- 独立 Gradle：通常不需要，Android 项目一般使用项目自带的 `gradlew.bat`。
