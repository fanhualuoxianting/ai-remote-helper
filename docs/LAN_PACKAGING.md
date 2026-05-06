# AI Remote Helper LAN 版 Windows 打包说明

## 目标

LAN 版用于同一局域网内的授权远程协助。协助者电脑运行 `relay-server`，被协助者电脑运行打包后的 Agent 客户端。被协助者必须手动选择授权目录，Agent 始终可见，不会隐藏运行。

## 环境要求

- Windows 10/11
- Java 21 JDK，PATH 中必须能找到 `java` 和 `jpackage`
- Maven 3.9+
- 可用的 Maven 依赖缓存；离线打包时请先在本机完成依赖下载

## 如何打包 LAN 版

推荐使用 Agent 模块下的脚本：

```powershell
git clone https://github.com/<your-name>/ai-remote-helper.git
cd ai-remote-helper
agent-client\scripts\package-lan-windows.bat -Offline
```

也可以直接运行 PowerShell 脚本：

```powershell
cd ai-remote-helper
powershell -NoProfile -ExecutionPolicy Bypass -File agent-client\scripts\package-lan-windows.ps1 -Offline
```

根目录兼容入口仍可使用：

```powershell
cd ai-remote-helper
scripts\package-lan-windows.bat -Offline
```

如果需要联网解析依赖，可去掉 `-Offline`：

```powershell
cd ai-remote-helper
agent-client\scripts\package-lan-windows.bat
```

## 打包产物在哪里

脚本会生成：

```text
dist\AI-Remote-Helper-LAN\
├── AI-Remote-Helper-LAN.exe
├── README-USER.txt
├── app\
└── runtime\
```

其中 `runtime\` 由 `jpackage` 自动生成，普通用户不需要额外安装 JDK 或 Maven。

## 如何发给别人

```powershell
cd ai-remote-helper
Compress-Archive -Path dist\AI-Remote-Helper-LAN -DestinationPath dist\AI-Remote-Helper-LAN.zip -Force
```

把 `dist\AI-Remote-Helper-LAN.zip` 发给被协助者。对方解压后双击：

```text
AI-Remote-Helper-LAN.exe
```

## 别人如何运行

1. 双击 `AI-Remote-Helper-LAN.exe`。
2. 选择“我需要远程协助”。
3. 选择一个项目文件夹作为授权目录。
4. 让协助者告诉你他的局域网 IP，例如 `192.168.1.8`。
5. 输入协助者 IP 和端口 `8080`。
6. 点击“测试连接”。
7. 测试成功后点击“连接”。
8. 把连接码发给协助者。
9. 协助过程中可以随时断开。

## Windows 防火墙提示怎么办

通常需要在协助者电脑放行 `relay-server` 的 `8080` 端口。管理员 PowerShell 可执行：

```powershell
netsh advfirewall firewall add rule name="AI Remote Helper Relay" dir=in action=allow protocol=TCP localport=8080
```

如果是校园网、公司网或酒店 Wi-Fi，可能启用了设备隔离，局域网模式会连接失败。这不是 Agent 问题，需要换同一路由器网络、手机热点或后续公网/隧道方案。

## 如何卸载

LAN 版当前是 app-image 目录，不写注册表、不注册服务。删除整个目录即可：

```powershell
Remove-Item -LiteralPath ".\AI-Remote-Helper-LAN" -Recurse -Force
```

上面是本机开发产物删除命令；发给别人后，对方删除自己解压出来的 `AI-Remote-Helper-LAN` 文件夹即可。

## 安全边界

- 当前版本不会开机自启。
- 当前版本不会注册系统服务。
- 当前版本不会请求管理员权限。
- 当前版本不会隐藏运行。
- 当前版本不会绕过授权目录。
- 当前版本不会关闭防火墙或杀毒软件。
- 当前版本不会读取浏览器数据、SSH 私钥或系统凭证。

## 验证命令

本阶段已实际执行：

```powershell
mvn clean package
```

结果：`BUILD SUCCESS`。

本阶段已实际执行：

```powershell
agent-client\scripts\package-lan-windows.bat -Offline
```

结果：成功生成 `dist\AI-Remote-Helper-LAN\AI-Remote-Helper-LAN.exe` 和 `README-USER.txt`。
