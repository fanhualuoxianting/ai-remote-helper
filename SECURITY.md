# AI Remote Helper 安全说明

## 授权模型

AI Remote Helper 是一个授权远程开发协助工具，不是隐蔽远控工具。

### 核心安全原则

1. **授权目录限制**：所有操作必须在用户选择的授权目录内
2. **路径沙箱**：防止路径穿越攻击（`../`、绝对路径等）
3. **敏感文件保护**：阻止访问 SSH 密钥、云凭证、浏览器数据等
4. **危险命令拦截**：阻止 `rm -rf`、`format`、`powershell -enc` 等危险命令
5. **操作审计**：所有操作都有审计日志

### 授权流程

1. 用户启动 Agent Client
2. 用户选择授权工作目录
3. Agent 连接到 Relay Server
4. 所有后续操作限制在授权目录内

## 禁止能力

AI Remote Helper **不会**实现以下功能：

- ❌ 隐藏运行
- ❌ 绕过授权
- ❌ 访问授权目录外文件
- ❌ 读取浏览器数据
- ❌ 读取 SSH 私钥
- ❌ 提权
- ❌ 修改注册表
- ❌ 规避审计日志
- ❌ 开机自启
- ❌ 静默安装

## 风险分级

### LOW（低风险）

可执行命令：
- `pwd`, `ls`, `dir`
- `git status`
- `npm run dev`, `npm test`
- `mvn test`, `gradle test`
- `python app.py`

### MEDIUM（中风险）

记录警告后可执行：
- `npm install`, `pip install`
- `mvn package`, `gradle build`
- `docker compose up`
- `git pull`

### HIGH（高风险）

默认阻止：
- `sudo`, `su -`
- `chmod 777`
- `rm -rf`（非根目录）
- `netsh`, `taskkill`

### BLOCKED（阻断）

绝对不允许：
- `rm -rf /`
- `format`
- `powershell -enc`
- `curl xxx | sh`
- `reg delete`
- `shutdown`
- 访问系统目录
- 访问用户隐私目录

## 路径沙箱

### 实现

- 使用 Java NIO Path 进行路径解析
- 使用 `normalize()` 和 `toAbsolutePath()` 标准化
- 防止 `../` 路径穿越
- 防止绝对路径访问
- Windows 路径考虑盘符和大小写

### 检查点

- `PathGuard.resolveSafePath()` - 安全解析路径
- `PathGuard.isInsideWorkspace()` - 检查是否在工作空间内
- `PathGuard.containsPathTraversal()` - 检测路径穿越

## 敏感文件保护

### 阻止的文件

- SSH 密钥：`id_rsa`, `id_ed25519`, `id_ecdsa`
- 云凭证：`.aws`, `.kube`, `.gnupg`
- 浏览器数据：Chrome, Firefox, Edge 配置文件
- 系统文件：`/etc/passwd`, `/etc/shadow`
- 证书文件：`.pem`, `.key`, `.p12`, `.pfx`, `.jks`

### 警告的文件

- `.env` 文件（可能包含凭证）

## 审计日志

### 记录内容

- 任务类型
- 文件路径
- 命令内容
- 风险等级
- 执行结果
- 时间戳

### 存储

- PostgreSQL：任务记录、审计事件、文件修改记录
- Redis：在线设备状态、会话映射

## 报告隐私处理

### 报告中不包含

- 敏感文件全文
- 设备 token
- 用户系统凭证
- 被阻止操作的敏感 payload

### 报告中包含

- 操作摘要
- 文件修改记录
- 命令执行记录
- 错误日志摘要
- 复现步骤
