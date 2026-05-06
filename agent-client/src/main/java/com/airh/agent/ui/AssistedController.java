package com.airh.agent.ui;

import com.airh.protocol.dto.HealthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.airh.agent.connection.AgentConnectionClient;
import com.airh.agent.connection.AgentConnectionListener;
import com.airh.agent.report.ReportGenerationService;
import com.airh.agent.safety.PathSandbox;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Properties;

public class AssistedController {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path CONFIG_PATH = Path.of(System.getProperty("user.home"), ".ai-remote-helper", "agent-client.properties");

    @FXML private BorderPane root;
    @FXML private TextField serverUrlField;
    @FXML private RadioButton lanModeRadio;
    @FXML private RadioButton officialModeRadio;
    @FXML private RadioButton customModeRadio;
    @FXML private TextField assistantIpField;
    @FXML private TextField assistantPortField;
    @FXML private Label localhostWarningLabel;
    @FXML private Label generatedWebsocketLabel;
    @FXML private Label connectionTestResultLabel;
    @FXML private Button testConnectionButton;
    @FXML private Label statusLabel;
    @FXML private Label deviceIdLabel;
    @FXML private Label connectionCodeLabel;
    @FXML private Label authorizedDirectoryLabel;
    @FXML private Label sessionExpiresLabel;
    @FXML private Label currentTaskLabel;
    @FXML private CheckBox readFilesCheck;
    @FXML private CheckBox writeFilesCheck;
    @FXML private CheckBox runCommandCheck;
    @FXML private CheckBox installDependencyCheck;
    @FXML private CheckBox highRiskCheck;
    @FXML private TextArea logsArea;
    @FXML private TextArea taskResultArea;
    @FXML private Button chooseDirectoryButton;
    @FXML private Button connectButton;
    @FXML private Button pauseButton;
    @FXML private Button disconnectButton;
    @FXML private Button reportButton;
    @FXML private Button copyCodeButton;
    @FXML private Label step1Circle;
    @FXML private Label step1Status;
    @FXML private Label step2Circle;
    @FXML private Label step2Status;
    @FXML private Label step3Circle;
    @FXML private Label step3Status;
    @FXML private Label authDirWarning;

    private AgentClientApplication application;
    private AgentConnectionClient connectionClient;
    private Path authorizedDirectoryPath;
    private PathSandbox pathSandbox;
    private boolean connectionTestPassed;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    public BorderPane getRoot() {
        return root;
    }

    public void setApplication(AgentClientApplication application) {
        this.application = application;
    }

    public void configure(String deviceId, String deviceName) {
        deviceIdLabel.setText(deviceId);
        connectionClient = new AgentConnectionClient(deviceId, deviceName, new UiConnectionListener());
        appendLog("Agent UI 已启动，请先选择授权目录，再连接 relay-server");
    }

    @FXML
    private void initialize() {
        statusLabel.setText("未连接");
        connectionCodeLabel.setText("未生成");
        authorizedDirectoryLabel.setText("未选择");
        sessionExpiresLabel.setText("连接后由 relay-server 控制");
        currentTaskLabel.setText("无");
        readFilesCheck.setSelected(true);
        writeFilesCheck.setSelected(true);
        runCommandCheck.setSelected(true);
        installDependencyCheck.setSelected(false);
        highRiskCheck.setSelected(false);
        highRiskCheck.setDisable(true);
        disconnectButton.setDisable(true);
        pauseButton.setDisable(true);
        connectButton.setDisable(true);
        copyCodeButton.setDisable(true);
        updateStepIndicators();
        configureConnectionModeControls();
        loadLanConfig();
        refreshGeneratedConnectionInfo();
    }

    @FXML
    private void backToRoleSelect() {
        shutdown();
        application.showRoleSelectView();
    }

    @FXML
    private void chooseDirectory() {
        AuthorizedDirectoryChooser chooser = new AuthorizedDirectoryChooser();
        chooser.choose(root.getScene().getWindow()).ifPresent(selection -> {
            authorizedDirectoryPath = selection.authorizedDirectory();
            pathSandbox = selection.sandbox();
            authorizedDirectoryLabel.setText(authorizedDirectoryPath.toString());
            if (authDirWarning != null) authDirWarning.setVisible(false);
            refreshConnectButtonState();
            updateStepIndicators();
            appendLog("已选择授权目录：" + authorizedDirectoryPath);
            appendLog("路径沙箱已启用，后续文件和命令 cwd 均限制在授权目录内");
        });
    }

    @FXML
    private void connect() {
        if (authorizedDirectoryPath == null || pathSandbox == null) {
            appendLog("连接失败：请先手动选择授权目录");
            return;
        }
        String serverUrl = resolveServerUrlForConnection();
        if (serverUrl == null) {
            return;
        }
        if (!connectionTestPassed) {
            connectionTestResultLabel.setText("请先点击“测试连接”，确认服务器在线后再连接。");
            appendLog("连接被阻止：尚未完成连接测试");
            refreshConnectButtonState();
            return;
        }
        connectButton.setDisable(true);
        disconnectButton.setDisable(false);
        pauseButton.setDisable(false);
        statusLabel.setText("连接中");
        try {
            connectionClient.connect(serverUrl, authorizedDirectoryPath.toString());
            saveLanConfigIfNeeded();
            appendLog("准备连接到：" + serverUrl + "，WebSocket：" + buildWebSocketUrl(serverUrl));
        } catch (RuntimeException exception) {
            appendLog("连接失败：" + exception.getMessage());
            statusLabel.setText("未连接");
            connectButton.setDisable(false);
            disconnectButton.setDisable(true);
            pauseButton.setDisable(true);
        }
    }

    @FXML
    private void testConnection() {
        String serverUrl = resolveServerUrlForTest();
        if (serverUrl == null) {
            return;
        }
        String healthUrl = serverUrl + "/api/health";
        testConnectionButton.setDisable(true);
        connectionTestResultLabel.setText("正在测试：" + healthUrl);
        HttpRequest request = HttpRequest.newBuilder(URI.create(healthUrl))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, throwable) -> Platform.runLater(() -> {
                    testConnectionButton.setDisable(false);
                    if (throwable != null) {
                        connectionTestPassed = false;
                        connectionTestResultLabel.setText("连接失败：请检查是否同一局域网、relay-server 是否启动、防火墙是否放行 8080、IP 是否正确。"
                                + throwable.getMessage());
                        appendLog("测试连接失败：" + throwable.getMessage());
                        refreshConnectButtonState();
                        return;
                    }
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        connectionTestPassed = false;
                        connectionTestResultLabel.setText("连接失败：HTTP " + response.statusCode()
                                + "。请检查 relay-server 是否正常启动。");
                        appendLog("测试连接失败：HTTP " + response.statusCode());
                        refreshConnectButtonState();
                        return;
                    }
                    try {
                        HealthResponse health = OBJECT_MAPPER.readValue(response.body(), HealthResponse.class);
                        connectionTestPassed = true;
                        connectionTestResultLabel.setText("服务器在线：" + health.appName()
                                + "，版本：" + health.version()
                                + "，状态：" + health.status()
                                + "，WebSocket：" + health.websocketEndpoint());
                        appendLog("测试连接成功：" + serverUrl + "，版本：" + health.version());
                        saveLanConfigIfNeeded();
                        refreshConnectButtonState();
                    } catch (IOException exception) {
                        connectionTestPassed = false;
                        connectionTestResultLabel.setText("连接成功，但健康检查响应解析失败：" + exception.getMessage());
                        appendLog("健康检查响应解析失败：" + exception.getMessage());
                        refreshConnectButtonState();
                    }
                }));
    }

    @FXML
    private void pauseExecution() {
        appendLog("用户暂停执行：断开当前连接并停止接收后续任务");
        disconnect();
        statusLabel.setText("已暂停");
    }

    @FXML
    private void disconnect() {
        if (connectionClient != null) {
            connectionClient.disconnect();
        }
        statusLabel.setText("已断开");
        connectionCodeLabel.setText("未生成");
        copyCodeButton.setText("复制连接码");
        copyCodeButton.setDisable(true);
        currentTaskLabel.setText("无");
        refreshConnectButtonState();
        disconnectButton.setDisable(true);
        pauseButton.setDisable(true);
        updateStepIndicators();
    }

    @FXML
    private void generateReport() {
        if (authorizedDirectoryPath == null) {
            appendLog("生成报告失败：请先选择授权目录");
            return;
        }
        try {
            ReportGenerationService service = new ReportGenerationService();
            Path reportPath = service.saveReport(authorizedDirectoryPath, service.generateReport(authorizedDirectoryPath));
            taskResultArea.setText("报告已生成：" + reportPath);
            appendLog("已生成中文 report.md 会话报告：" + reportPath);
        } catch (IOException exception) {
            appendLog("生成报告失败：" + exception.getMessage());
        }
    }

    public void shutdown() {
        if (connectionClient != null) {
            connectionClient.shutdown();
        }
    }

    private void appendLog(String message) {
        logsArea.appendText("[" + LocalTime.now().withNano(0) + "] " + message + System.lineSeparator());
    }

    private void configureConnectionModeControls() {
        ToggleGroup toggleGroup = new ToggleGroup();
        lanModeRadio.setToggleGroup(toggleGroup);
        officialModeRadio.setToggleGroup(toggleGroup);
        customModeRadio.setToggleGroup(toggleGroup);
        lanModeRadio.setSelected(true);
        localhostWarningLabel.setManaged(false);
        assistantIpField.textProperty().addListener((observable, oldValue, newValue) -> refreshGeneratedConnectionInfo());
        assistantPortField.textProperty().addListener((observable, oldValue, newValue) -> refreshGeneratedConnectionInfo());
        toggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> refreshConnectionMode());
        refreshConnectionMode();
    }

    private void refreshConnectionMode() {
        boolean lanMode = lanModeRadio.isSelected();
        assistantIpField.setDisable(!lanMode);
        assistantPortField.setDisable(!lanMode);
        serverUrlField.setEditable(!lanMode);
        serverUrlField.setDisable(false);
        serverUrlField.setPromptText(lanMode ? "根据协助者 IP 和端口自动生成" : "例如 http://192.168.1.8:8080");
        if (!lanMode && serverUrlField.getText() != null && serverUrlField.getText().contains("<协助者IP>")) {
            serverUrlField.clear();
        }
        connectionTestPassed = false;
        refreshConnectButtonState();
        refreshGeneratedConnectionInfo();
    }

    private void refreshGeneratedConnectionInfo() {
        if (lanModeRadio == null || !lanModeRadio.isSelected()) {
            if (generatedWebsocketLabel != null) generatedWebsocketLabel.setText("自定义服务器将按服务器地址自动生成 WebSocket 地址");
            localhostWarningLabel.setVisible(false);
            localhostWarningLabel.setManaged(false);
            return;
        }
        String host = normalizeHost(assistantIpField.getText());
        connectionTestPassed = false;
        refreshConnectButtonState();
        boolean localhost = isLocalhost(host);
        localhostWarningLabel.setVisible(localhost);
        localhostWarningLabel.setManaged(localhost);
        String port = normalizePort(assistantPortField.getText()).orElse("8080");
        String baseUrl = host.isBlank() ? "http://<协助者IP>:" + port : "http://" + host + ":" + port;
        serverUrlField.setText(baseUrl);
        if (generatedWebsocketLabel != null) generatedWebsocketLabel.setText(host.isBlank() ? "ws://<协助者IP>:" + port + "/ws" : "ws://" + host + ":" + port + "/ws");
    }

    private String resolveServerUrlForConnection() {
        String serverUrl = resolveServerUrlForTest();
        if (serverUrl == null) {
            return null;
        }
        if (customModeRadio.isSelected() && !confirmCustomServer(serverUrl)) {
            appendLog("用户取消连接自定义服务器：" + serverUrl);
            return null;
        }
        return serverUrl;
    }

    private String resolveServerUrlForTest() {
        if (lanModeRadio.isSelected()) {
            Optional<String> port = normalizePort(assistantPortField.getText());
            String host = normalizeHost(assistantIpField.getText());
            if (host.isBlank()) {
                connectionTestResultLabel.setText("请输入协助者电脑的局域网 IP，例如 192.168.1.8。");
                connectionTestPassed = false;
                refreshConnectButtonState();
                return null;
            }
            if (port.isEmpty()) {
                connectionTestResultLabel.setText("端口必须是 1-65535 之间的数字，默认可填写 8080。");
                connectionTestPassed = false;
                refreshConnectButtonState();
                return null;
            }
            if (isLocalhost(host)) {
                connectionTestResultLabel.setText("localhost 只表示本机。请确认这是本机开发连接；连接协助者电脑时应填写对方局域网 IP。");
            }
            return "http://" + host + ":" + port.get();
        }
        String serverUrl = serverUrlField.getText() == null ? "" : serverUrlField.getText().strip();
        if (serverUrl.isBlank()) {
            connectionTestResultLabel.setText("请输入服务器地址。");
            connectionTestPassed = false;
            refreshConnectButtonState();
            return null;
        }
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            connectionTestResultLabel.setText("自定义服务器地址必须以 http:// 或 https:// 开头。");
            connectionTestPassed = false;
            refreshConnectButtonState();
            return null;
        }
        return removeTrailingSlash(serverUrl);
    }

    private boolean confirmCustomServer(String serverUrl) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认自定义服务器");
        alert.setHeaderText("将连接到自定义服务器");
        alert.setContentText("请确认你信任该服务器，并且它属于本次远程协助会话：" + serverUrl);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private String buildWebSocketUrl(String serverUrl) {
        URI uri = URI.create(serverUrl);
        String scheme = "https".equals(uri.getScheme()) ? "wss" : "ws";
        return scheme + "://" + uri.getRawAuthority() + "/ws";
    }

    private String normalizeHost(String rawHost) {
        String host = rawHost == null ? "" : rawHost.strip();
        if (host.startsWith("http://") || host.startsWith("https://") || host.startsWith("ws://") || host.startsWith("wss://")) {
            URI uri = URI.create(host);
            host = uri.getHost() == null ? "" : uri.getHost();
        }
        int slashIndex = host.indexOf('/');
        if (slashIndex >= 0) {
            host = host.substring(0, slashIndex);
        }
        if (host.indexOf(':') > 0 && host.indexOf(':') == host.lastIndexOf(':')) {
            host = host.substring(0, host.indexOf(':'));
        }
        return host;
    }

    private Optional<String> normalizePort(String rawPort) {
        String portText = rawPort == null || rawPort.isBlank() ? "8080" : rawPort.strip();
        try {
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) {
                return Optional.empty();
            }
            return Optional.of(Integer.toString(port));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private boolean isLocalhost(String host) {
        String normalized = host.toLowerCase();
        return "localhost".equals(normalized) || "127.0.0.1".equals(normalized) || "::1".equals(normalized);
    }

    private String removeTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private void loadLanConfig() {
        Properties properties = new Properties();
        if (Files.exists(CONFIG_PATH)) {
            try (InputStream inputStream = Files.newInputStream(CONFIG_PATH)) {
                properties.load(inputStream);
            } catch (IOException exception) {
                appendLog("读取本地连接配置失败，将使用默认端口：" + exception.getMessage());
            }
        }
        assistantIpField.setText(properties.getProperty("lan.ip", ""));
        assistantPortField.setText(properties.getProperty("lan.port", "8080"));
    }

    private void saveLanConfigIfNeeded() {
        if (!lanModeRadio.isSelected()) {
            return;
        }
        String host = normalizeHost(assistantIpField.getText());
        Optional<String> port = normalizePort(assistantPortField.getText());
        if (host.isBlank() || port.isEmpty()) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("lan.ip", host);
        properties.setProperty("lan.port", port.get());
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream outputStream = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(outputStream, "AI Remote Helper Agent LAN connection config");
            }
        } catch (IOException exception) {
            appendLog("保存本地连接配置失败：" + exception.getMessage());
        }
    }

    @FXML
    private void copyConnectionCode() {
        String code = connectionCodeLabel.getText();
        if (code != null && !code.equals("未生成")) {
            ClipboardContent content = new ClipboardContent();
            content.putString(code);
            Clipboard.getSystemClipboard().setContent(content);
            copyCodeButton.setText("已复制");
            appendLog("连接码已复制到剪贴板：" + code);
        }
    }

    private void refreshConnectButtonState() {
        if (connectButton == null) {
            return;
        }
        boolean ready = authorizedDirectoryPath != null && pathSandbox != null && connectionTestPassed;
        connectButton.setDisable(!ready);
    }

    private void updateStepIndicators() {
        boolean hasDir = authorizedDirectoryPath != null;
        boolean connected = "已连接".equals(statusLabel.getText());
        boolean hasCode = connectionCodeLabel.getText() != null && !connectionCodeLabel.getText().equals("未生成");

        if (step1Circle != null) {
            step1Circle.setStyle(hasDir ? "-fx-background-color: #16A34A;" : "-fx-background-color: #D1D5DB;");
            step1Status.setText(hasDir ? "已完成" : "未完成");
            step1Status.setStyle(hasDir ? "-fx-text-fill: #16A34A; -fx-font-weight: 700;" : "");
        }
        if (step2Circle != null) {
            step2Circle.setStyle(connected ? "-fx-background-color: #16A34A;" : (hasDir ? "-fx-background-color: #2563EB;" : "-fx-background-color: #D1D5DB;"));
            step2Status.setText(connected ? "已完成" : (hasDir ? "进行中" : "未完成"));
            step2Status.setStyle(connected ? "-fx-text-fill: #16A34A; -fx-font-weight: 700;" : (hasDir ? "-fx-text-fill: #2563EB;" : ""));
        }
        if (step3Circle != null) {
            step3Circle.setStyle(hasCode ? "-fx-background-color: #16A34A;" : (connected ? "-fx-background-color: #2563EB;" : "-fx-background-color: #D1D5DB;"));
            step3Status.setText(hasCode ? "请发给协助者" : (connected ? "进行中" : "未完成"));
            step3Status.setStyle(hasCode ? "-fx-text-fill: #16A34A; -fx-font-weight: 700;" : (connected ? "-fx-text-fill: #2563EB;" : ""));
        }
    }

    private final class UiConnectionListener implements AgentConnectionListener {
        @Override
        public void onConnecting(String serverUrl) {
            Platform.runLater(() -> appendLog("连接中：" + serverUrl));
        }

        @Override
        public void onConnected(String sessionId, String connectionCode) {
            Platform.runLater(() -> {
                statusLabel.setText("已连接");
                connectionCodeLabel.setText(connectionCode);
                copyCodeButton.setDisable(false);
                copyCodeButton.setText("复制连接码");
                sessionExpiresLabel.setText("sessionId：" + sessionId);
                updateStepIndicators();
                appendLog("连接成功，sessionId：" + sessionId + "，连接码：" + connectionCode);
            });
        }

        @Override
        public void onDisconnected(String reason) {
            Platform.runLater(() -> {
                statusLabel.setText("已断开");
                connectionCodeLabel.setText("未生成");
                copyCodeButton.setText("复制连接码");
                copyCodeButton.setDisable(true);
                currentTaskLabel.setText("无");
                refreshConnectButtonState();
                disconnectButton.setDisable(true);
                pauseButton.setDisable(true);
                updateStepIndicators();
                appendLog(reason);
            });
        }

        @Override
        public void onTaskStarted(String taskId, String taskType, String payloadSummary) {
            Platform.runLater(() -> {
                currentTaskLabel.setText(taskId + " / " + taskType);
                appendLog("收到任务：" + taskId + "，类型：" + taskType);
                appendLog("任务 payload 摘要：" + payloadSummary);
            });
        }

        @Override
        public void onTaskFinished(String taskId, String status, String summary) {
            Platform.runLater(() -> {
                currentTaskLabel.setText("无");
                appendLog("任务结束：" + taskId + "，状态：" + status + "，" + summary);
            });
        }

        @Override
        public void onTaskOutput(String taskId, String output) {
            Platform.runLater(() -> taskResultArea.setText(summarizeOutput(taskId, output)));
        }

        @Override
        public void onLog(String message) {
            Platform.runLater(() -> appendLog(message));
        }

        private String summarizeOutput(String taskId, String output) {
            String normalized = output == null ? "" : output;
            int maxLength = 6000;
            String visible = normalized.length() <= maxLength
                    ? normalized
                    : normalized.substring(0, maxLength) + System.lineSeparator() + "... 已截断，仅显示前 " + maxLength + " 字符";
            return "任务：" + taskId + System.lineSeparator() + visible;
        }
    }
}
