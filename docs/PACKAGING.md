# AI Remote Helper - Packaging Guide

## Windows 打包

### 前置条件

- Java 21+（JDK，不是 JRE）
- JAVA_HOME 环境变量已设置
- Maven 项目已构建过至少一次

### 打包步骤

```powershell
git clone https://github.com/<your-name>/ai-remote-helper.git
cd ai-remote-helper
scripts\package-windows.bat
```

### 打包产物

```
dist/
├── AI-Remote-Helper.bat          ← 双击启动
├── agent-client-all.jar          ← 20MB fat jar
└── javafx-sdk/
    └── lib/
        ├── javafx-base-21.0.5.jar
        ├── javafx-base-21.0.5-win.jar
        ├── javafx-controls-21.0.5.jar
        ├── javafx-controls-21.0.5-win.jar
        ├── javafx-graphics-21.0.5.jar
        └── javafx-graphics-21.0.5-win.jar
```

### 用户安装方式

1. 下载 dist 文件夹（或 zip 压缩包）
2. 解压到任意目录
3. 确保系统安装了 Java 21 且 JAVA_HOME 已设置
4. 双击 `AI-Remote-Helper.bat`

### 用户不需要

- git clone
- 安装 JDK（只要有 Java 21 runtime）
- 运行 Maven
- 配置域名或服务器

### 已知限制

- 当前版本需要 Java 21 已安装在目标机器上
- 未来版本将使用 jpackage 打包自包含 exe（含 Java runtime）
- 当前不支持开机自启（设计如此）
- 当前不支持静默安装

### 安全说明

- 不会开机自启
- 不会注册系统服务
- 不会请求管理员权限
- 不会修改防火墙
- 所有操作在用户授权目录内进行

### 后续计划

- jpackage 自包含 exe（不需要用户安装 Java）
- Inno Setup / NSIS 安装包
- GitHub Releases 发布
