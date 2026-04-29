# AI Remote Helper 打包文档

## 前置条件

- Java 21+ (推荐使用 Temurin)
- JAVA_HOME 环境变量已设置
- Windows 操作系统

## 打包步骤

### 使用 PowerShell 脚本

```powershell
cd E:\openclaw-project\ai-remote-helper
.\scripts\package-agent.ps1
```

### 使用 Batch 脚本

```cmd
cd E:\openclaw-project\ai-remote-helper
.\scripts\package-agent.bat
```

### 手动打包

```powershell
# 1. 构建项目
.\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package -DskipTests

# 2. 使用 jpackage 打包
$env:JAVA_HOME = "D:\jdk-21.0.9.10-hotspot"
& "$env:JAVA_HOME\bin\jpackage.exe" `
    --type app-image `
    --name "AI-Remote-Helper-Agent" `
    --input "agent-client\target" `
    --main-jar "agent-client-0.1.0-SNAPSHOT.jar" `
    --main-class "com.airh.agent.AgentClientApplication" `
    --dest "dist" `
    --java-options "--add-opens javafx.graphics/com.sun.javafx.application=ALL-UNNAMED" `
    --java-options "--add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED" `
    --java-options "--add-opens javafx.fxml/com.sun.javafx.fxml=ALL-UNNAMED"
```

## 输出产物

打包完成后，在 `dist/AI-Remote-Helper-Agent` 目录下会生成：

- `AI-Remote-Helper-Agent.exe` - 可执行文件
- `runtime/` - Java 运行时
- `app/` - 应用程序 jar 文件

## 运行打包产物

```powershell
cd dist\AI-Remote-Helper-Agent
.\AI-Remote-Helper-Agent.exe
```

## 常见问题

### jpackage 找不到

确保 JAVA_HOME 指向 JDK 14+，而不是 JRE。

### JavaFX 模块错误

确保使用 `--add-opens` 参数打开 JavaFX 模块。

### 打包失败

检查是否所有依赖都已正确下载：

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd dependency:resolve
```

## 后续规划

- macOS 支持：使用 `--type dmg` 或 `--type pkg`
- Linux 支持：使用 `--type deb` 或 `--type rpm`
- 图标设置：使用 `--icon` 参数
- 安装程序：使用 `--type msi` 生成 Windows 安装程序
