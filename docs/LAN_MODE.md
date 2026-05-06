# 局域网连接模式使用说明

## 适用场景

局域网连接模式适合同一 Wi-Fi、同一路由器或同一办公局域网内的远程协助。协助者电脑运行 `relay-server`，被协助者电脑运行 JavaFX Agent 客户端，并在 Agent 中输入协助者电脑的局域网 IP 和端口。

本模式不包含公网服务器、Cloudflare、域名部署或穿透能力。

## 协助者启动 relay-server

Windows 局域网演示推荐直接使用脚本启动，默认端口是 `18080`：

```powershell
scripts\start-relay-lan.bat
```

如果你在离线环境使用本机缓存，可以执行：

```powershell
scripts\start-relay-lan.bat -Offline
```

脚本会优先使用项目内 `.tools\apache-maven-3.9.9\bin\mvn.cmd`，如果不存在则使用系统 `mvn`。如果本机已有 `airh-postgres` 和 `airh-redis` Docker 容器，脚本会尝试启动它们。

如果需要改端口，例如使用 `19080`：

```powershell
scripts\start-relay-lan.bat -Port 19080
```

如果你不使用脚本，也可以手动启动：

```powershell
git clone https://github.com/<your-name>/ai-remote-helper.git
cd ai-remote-helper
mvn -f relay-server/pom.xml spring-boot:run
```

启动后控制台会打印本机访问地址和局域网访问候选，例如：

```text
AI Remote Helper Relay Server 已启动
本机访问：http://localhost:8080
局域网访问候选：
- http://192.168.1.8:8080
请让被协助者在 Agent 中输入同一局域网下的 IP。
```

也可以访问以下接口确认服务状态：

```powershell
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod http://localhost:8080/api/network/addresses
```

如果使用推荐脚本，端口默认是 `18080`，验证命令为：

```powershell
Invoke-RestMethod http://localhost:18080/api/health
Invoke-RestMethod http://localhost:18080/api/network/addresses
```

## 查看协助者电脑 IP

推荐优先使用 relay-server 控制台打印的局域网访问候选，也可以在协助者电脑执行：

```powershell
ipconfig
```

选择 Wi-Fi 或以太网适配器下的 IPv4 地址，常见格式为 `192.168.x.x`、`10.x.x.x` 或 `172.16.x.x` 到 `172.31.x.x`。

## 被协助者连接方式

1. 启动 Agent Client。
2. 在左侧导航栏或首页入口选择“我需要别人帮忙”。
3. 先在三步流程中选择授权目录。未选择授权目录时，“连接”按钮保持禁用。
4. 保持默认“局域网连接（推荐）”。
5. 在“协助者 IP”中输入协助者电脑的局域网 IP，例如 `192.168.1.8`。
6. 如果协助者用 Maven 默认方式启动，端口填写 `8080`；如果协助者用 `scripts\start-relay-lan.bat` 启动，端口填写 `18080`。
7. 点击“测试连接”，确认 `/api/health` 返回服务器在线。
8. 点击“连接”。
9. 连接成功后，界面会大号显示连接码，例如 `738-291`。
10. 点击“复制连接码”，按钮会显示“已复制”，然后把连接码发给协助者。

Agent 会根据输入生成：

```text
HTTP Base URL: http://{ip}:{port}
WebSocket URL: ws://{ip}:{port}/ws
```

## Windows 防火墙放行端口

如果被协助者无法连接协助者电脑，协助者电脑需要允许 relay-server 端口入站。推荐脚本默认端口是 `18080`：

```powershell
New-NetFirewallRule -DisplayName "AI Remote Helper Relay 18080" -Direction Inbound -Protocol TCP -LocalPort 18080 -Action Allow
```

如果使用 Maven 默认端口 `8080`：

```powershell
New-NetFirewallRule -DisplayName "AI Remote Helper Relay 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
```

如果不想保留规则，可以删除：

```powershell
Remove-NetFirewallRule -DisplayName "AI Remote Helper Relay 18080"
```

## 为什么不能填 localhost

`localhost` 只表示当前这台电脑本机。

被协助者在自己的电脑上填写 `localhost:8080` 时，Agent 会尝试连接被协助者自己的电脑，而不是协助者电脑。因此局域网协助时必须填写协助者电脑的局域网 IP，例如 `192.168.1.8`。

Agent 客户端检测到 `localhost` 或 `127.0.0.1` 时会显示明显警告。

## 协助者控制模式

协助者可使用 Agent Client 的“我要帮别人处理项目”页面做手动验证和调试：

1. 在 `relay-server` 输入框中填写服务地址，例如 `http://localhost:8080`。
2. 输入被协助者发来的连接码。
3. 点击“连接远程设备”。
4. 页面顶部状态条显示“已连接远程设备”后，快捷操作按钮会启用。
5. “文件浏览”分组可查看授权根目录、读取相对路径文件，例如 `pom.xml`。
6. “命令执行”分组可输入命令、工作目录和超时时间，例如命令 `mvn test`、工作目录 `.`、超时 `30` 秒。
7. “会话输出”分组可查看任务日志或生成中文报告。
8. “高级调试”默认折叠，仅保留 `list_dir`、`get_logs` 这类 API 风格入口供开发排障。

正式 AI 调用仍应通过 `mcp-bridge`。控制端不会直接连接对方电脑执行命令，所有任务仍通过 `relay-server` 转发，并由 Agent 在授权目录和 safety 模块限制下执行。

## 桌面客户端布局

当前 Agent Client 默认打开 `1400x900` 的桌面客户端主窗口：

- 左侧固定导航栏宽约 `220px`，包含产品名、`LAN Mode` 标签、首页、我需要帮忙、我要帮别人、设置、关于和版本号 `0.1.0`。
- 右侧为内容区，点击导航项切换页面，不会新增后端协议或公网模式。
- 被协助模式采用主区域和右侧安全栏布局，连接码以 `52px` 等宽蓝色字体突出显示。
- 协助者模式采用三栏工作台，未连接远程设备前文件、命令、日志和报告按钮保持禁用。
- 高级调试入口默认折叠，只用于开发排障。

## 常见问题排查

- 测试连接失败：确认两台电脑连接同一个 Wi-Fi 或同一个局域网，并确认端口填写的是协助者实际启动的端口。
- HTTP 超时或拒绝连接：确认协助者电脑的 relay-server 已启动。
- 只能本机访问：检查 Windows 防火墙是否放行 relay-server 端口，例如 TCP 18080。
- IP 填错：在协助者电脑查看 relay-server 启动日志或执行 `ipconfig`。
- 使用了虚拟网卡 IP：不要选择 Docker、VMware、VirtualBox、WSL、Hyper-V 虚拟网卡地址。
- “连接”按钮不可点击：先选择授权目录，再测试连接，测试成功后才允许连接。
- 连接码没有生成：确认被协助端已经连接成功，状态不是“未连接”或“已断开”。
- 复制连接码没有反应：只有连接码不为“未生成”时才允许复制。
- Agent 已连接但无法操作文件：被协助者必须先手动选择授权目录，所有文件和命令都限制在授权目录内。

## 后续升级到公网模式

后续公网模式应在官方服务器或受控网关上实现认证、会话有效期、审计日志、权限确认和安全策略。公网模式不应通过隐藏运行、开机自启、提权、关闭防火墙或绕过用户授权来实现。
