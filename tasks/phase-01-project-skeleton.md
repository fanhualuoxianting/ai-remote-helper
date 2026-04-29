# Phase 01：创建 Java 版项目骨架

## 目标

创建 AI Remote Helper 的 Java 版 Maven 多模块项目骨架。

本阶段只做项目结构、基础 pom、基础文档、协议枚举和安全规则框架，不实现真实远程命令执行。

## 必须创建的模块

根目录：
- pom.xml
- README.md
- SECURITY.md
- DEPLOY.md
- DEVELOPMENT_REPORT.md

Maven 子模块：
- common-protocol
- common-safety
- relay-server
- agent-client
- mcp-bridge

非 Maven 或预留目录：
- web-console
- docs
- tasks

## 技术要求

父 pom：
- Java 21
- Maven 多模块
- 统一 dependencyManagement
- 统一 pluginManagement
- 子模块能被 mvn clean package 构建

common-protocol：
- 创建基础 package
- 定义 MessageType 枚举
- 定义 TaskType 枚举
- 定义 TaskStatus 枚举
- 定义 PermissionType 枚举
- 定义 RiskLevel 枚举
- 定义基础 DTO：
  - RemoteMessage
  - RemoteTask
  - TaskResult
  - TaskLog

common-safety：
- 创建基础 package
- 创建 CommandRiskDetector 类框架
- 创建 PathGuard 类框架
- 创建 SensitiveFileGuard 类框架
- 本阶段只做基础规则，不实现复杂逻辑

relay-server：
- 创建 Spring Boot 3 项目结构
- 能启动一个空服务
- 预留 websocket、session、device、task、audit 包
- 暂时不需要真实连接 Agent

agent-client：
- 创建 JavaFX 项目结构
- 能启动一个基础窗口
- 窗口显示：
  - AI Remote Helper
  - 当前状态：未连接
  - 授权目录：未选择
  - 日志区域占位
  - 连接按钮占位
  - 断开按钮占位

mcp-bridge：
- 创建 Java 模块结构
- 预留 tools、client、config 包
- 暂时不需要真实 MCP 功能

web-console：
- 只创建 README.md，说明后期用于管理后台

docs：
- 创建 ARCHITECTURE.md
- 创建 ROADMAP.md

## 安全要求

本阶段不要实现任何远程命令执行。
不要实现隐藏运行。
不要实现开机自启。
不要实现提权。
不要访问授权目录外文件。

## 文档要求

README.md：
- 说明项目是什么
- 说明不是隐藏远控
- 说明模块结构
- 说明本地开发启动方式

SECURITY.md：
- 说明安全边界
- 说明禁止能力
- 说明授权目录机制
- 说明审计机制规划

DEPLOY.md：
- 说明后续如何部署 relay-server
- 说明后续如何打包 agent-client

DEVELOPMENT_REPORT.md：
- 记录 Phase 01 完成内容
- 记录修改文件
- 记录运行命令
- 记录测试结果
- 记录下一阶段计划

## 验收标准

1. Maven 多模块结构完整。
2. 根目录执行 mvn clean package 不应因为项目结构错误失败。
3. relay-server 能启动空 Spring Boot 服务。
4. agent-client 能启动 JavaFX 基础窗口。
5. README.md、SECURITY.md、DEPLOY.md、DEVELOPMENT_REPORT.md 存在且为中文。
6. 没有实现任何危险远控能力。
