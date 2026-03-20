# Legado 无障碍适配版

## 项目简介

本项目为 [Legado 阅读器](https://github.com/gedoor/Legado) 的无障碍服务适配版本，旨在帮助视障用户更好地使用阅读器功能。

## 主要功能

- ✅ **无障碍服务支持** - 完美兼容屏幕阅读器 (TalkBack)
- ✅ **朗读菜单优化** - 朗读模式下任意位置点击即可呼出朗读控制菜单
- ✅ **完整保留原版功能** - 继承 Legado 所有原有特性

## 下载安装

### 最新版本
[![Download APK](https://img.shields.io/badge/Download-APK-blue?style=for-the-badge)](https://github.com/baich110/Legado-Accessibility/releases/latest/download/Legado-Accessibility-v3.24.0320.apk)

**版本**: v3.24.0320  
**APK 大小**: ~28 MB  
**下载链接**: [点击下载 Legado-Accessibility-v3.24.0320.apk](https://github.com/baich110/Legado-Accessibility/releases/download/v3.24.0320/Legado-Accessibility-v3.24.0320.apk)

### 历史版本
所有版本请访问 [Releases 页面](https://github.com/baich110/Legado-Accessibility/releases)

## 使用说明

1. 下载并安装 APK
2. 首次打开应用时，系统会提示开启无障碍服务
3. 进入 **设置 → 无障碍 → Legado 无障碍服务** 开启
4. 开始阅读，享受无障碍体验！

## 朗读功能

在朗读模式下：
- 打开一本书并点击「朗读」按钮开始朗读
- 点击阅读区域任意位置即可呼出**朗读控制菜单**
- 支持暂停、继续、调整语速等操作

## 项目结构

```
Legado-Accessibility/
├── app/                          # Android 应用模块
│   └── src/main/
│       ├── java/io/legado/app/   # 主要代码
│       └── AndroidManifest.xml   # 应用配置
├── .github/workflows/            # CI/CD 配置
└── README.md                     # 项目说明
```

## 构建说明

本项目使用 GitHub Actions 自动构建：
- 每次 push 到 master 分支自动触发构建
- 构建产物为 APK 文件
- 可在 [Actions](https://github.com/baich110/Legado-Accessibility/actions) 页面查看构建历史

## 技术栈

- **语言**: Kotlin, Java
- **构建工具**: Gradle
- **CI/CD**: GitHub Actions
- **目标平台**: Android 8.0+ (API 26+)

## 更新日志

### v3.24.0320 (2024-03-20)
- 🔧 **修复**: 朗读模式下点击阅读区域可正确切换到朗读控制菜单
- 优化触摸交互逻辑

## 致谢

- 原始项目: [Legado](https://github.com/gedoor/Legado) by gedooor
- 适配开发: Baich110

## 许可证

本项目继承 Legado 的开源许可证，遵循 GPL-3.0 协议。

---

⭐ 如果这个项目对你有帮助，请点个 Star！
