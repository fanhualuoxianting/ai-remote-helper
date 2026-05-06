package com.airh.agent.ui;

import com.airh.protocol.enums.TaskType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ControllerViewController {
    @FXML private BorderPane root;
    @FXML private TextField relayUrlField;
    @FXML private TextField connectionCodeField;
    @FXML private Label sessionIdLabel;
    @FXML private Label topStatusLabel;
    @FXML private Label deviceStatusLabel;
    @FXML private Label deviceIdLabel;
    @FXML private Label remoteDirectoryLabel;
    @FXML private Label permissionsLabel;
    @FXML private Label notConnectedHint;
    @FXML private TextField listDirPathField;
    @FXML private TextField readFilePathField;
    @FXML private TextField commandCwdField;
    @FXML private TextField commandField;
    @FXML private TextField timeoutField;
    @FXML private TextField logsTaskIdField;
    @FXML private TextArea taskLogsArea;
    @FXML private TextArea taskResultArea;
    @FXML private Label errorLabel;
    @FXML private Button listRootButton;
    @FXML private Button readFileButton;
    @FXML private Button runCommandButton;
    @FXML private Button getLogsButton;
    @FXML private Button generateReportButton;
    @FXML private Button debugListDirButton;
    @FXML private Button debugGetLogsButton;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private AgentClientApplication application;
    private String sessionId;

    public BorderPane getRoot() {
        return root;
    }

    public void setApplication(AgentClientApplication application) {
        this.application = application;
    }

    @FXML
    private void initialize() {
        sessionIdLabel.setText("未连接");
        topStatusLabel.setText("未连接远程设备");
        deviceStatusLabel.setText("未连接");
        deviceIdLabel.setText("-");
        remoteDirectoryLabel.setText("-");
        permissionsLabel.setText("读取文件、修改文件、执行命令受 Agent 授权目录和 safety 模块限制");
        errorLabel.setText("");
        refreshTaskButtons();
        appendLog("控制台已启动。请输入对方连接码，通过 relay-server 建立手动调试会话。");
    }

    @FXML
    private void backToRoleSelect() {
        application.showRoleSelectView();
    }

    @FXML
    private void connectRemoteDevice() {
        clearError();
        String code = connectionCodeField.getText().strip();
        if (code.isBlank()) {
            showError("请输入连接码");
            return;
        }
        sendGet("/api/devices/online").thenAccept(body -> Platform.runLater(() -> matchDevice(code, body)))
                .exceptionally(this::showAsyncError);
    }

    @FXML
    private void listRootDir() {
        createTask(TaskType.LIST_DIR, Map.of("path", "."), 30);
    }

    @FXML
    private void listDir() {
        createTask(TaskType.LIST_DIR, Map.of("path", blankToDefault(listDirPathField.getText(), ".")), 30);
    }

    @FXML
    private void readFile() {
        createTask(TaskType.READ_FILE, Map.of("path", readFilePathField.getText().strip()), 30);
    }

    @FXML
    private void runCommand() {
        createTask(TaskType.RUN_COMMAND, Map.of(
                "cwd", blankToDefault(commandCwdField.getText(), "."),
                "command", commandField.getText().strip(),
                "timeoutSeconds", parseTimeout()
        ), parseTimeout());
    }

    @FXML
    private void getLogs() {
        clearError();
        String taskId = logsTaskIdField.getText().strip();
        if (taskId.isBlank()) {
            showError("请输入 taskId");
            return;
        }
        sendGet("/api/tasks/" + taskId + "/logs")
                .thenAccept(body -> Platform.runLater(() -> {
                    taskLogsArea.setText(pretty(body));
                    appendLog("已拉取任务日志：" + taskId);
                }))
                .exceptionally(this::showAsyncError);
    }

    @FXML
    private void generateReport() {
        createTask(TaskType.GENERATE_REPORT, Map.of(), 30);
    }

    private void createTask(TaskType taskType, Map<String, Object> data, int timeoutSeconds) {
        clearError();
        if (sessionId == null || sessionId.isBlank()) {
            showError("请先通过连接码连接远程设备");
            return;
        }
        if (taskType == TaskType.READ_FILE && blank(data.get("path")).isBlank()) {
            showError("read_file 需要输入路径");
            return;
        }
        if (taskType == TaskType.RUN_COMMAND && blank(data.get("command")).isBlank()) {
            showError("run_command 需要输入 command");
            return;
        }
        Map<String, Object> request = Map.of(
                "taskType", taskType.name(),
                "payload", Map.of("data", data),
                "timeoutSeconds", timeoutSeconds
        );
        sendPost("/api/sessions/" + sessionId + "/tasks", request)
                .thenAccept(body -> Platform.runLater(() -> handleCreatedTask(taskType, body)))
                .exceptionally(this::showAsyncError);
    }

    private void handleCreatedTask(TaskType taskType, String body) {
        taskResultArea.setText(pretty(body));
        try {
            JsonNode response = objectMapper.readTree(body);
            String taskId = response.path("taskId").asText();
            logsTaskIdField.setText(taskId);
            appendLog("已通过 relay-server 下发任务：" + taskType + "，taskId=" + taskId);
            refreshTaskLater(taskId);
        } catch (IOException exception) {
            showError("解析任务响应失败：" + exception.getMessage());
        }
    }

    private void refreshTaskLater(String taskId) {
        CompletableFuture.delayedExecutor(900, java.util.concurrent.TimeUnit.MILLISECONDS).execute(() ->
                sendGet("/api/tasks/" + taskId)
                        .thenAccept(body -> Platform.runLater(() -> taskResultArea.setText(pretty(body))))
                        .exceptionally(this::showAsyncError));
    }

    private void matchDevice(String code, String body) {
        try {
            JsonNode devices = objectMapper.readTree(body);
            for (JsonNode device : devices) {
                if (code.equals(device.path("connectionCode").asText())) {
                    sessionId = device.path("sessionId").asText();
                    sessionIdLabel.setText(sessionId);
                    deviceStatusLabel.setText(device.path("online").asBoolean() ? "在线" : "离线");
                    topStatusLabel.setText(device.path("online").asBoolean() ? "已连接远程设备" : "远程设备离线");
                    deviceIdLabel.setText(device.path("deviceId").asText("-"));
                    remoteDirectoryLabel.setText(device.path("authorizedDirectory").asText("-"));
                    permissionsLabel.setText("通过 relay-server 下发任务；实际文件、命令权限由 Agent 授权目录和 safety 模块执行");
                    notConnectedHint.setText("已连接远程设备。请只在对方授权目录内执行协助操作。");
                    refreshTaskButtons();
                    appendLog("已匹配远程设备：" + deviceIdLabel.getText() + "，sessionId=" + sessionId);
                    return;
                }
            }
            showError("未找到匹配连接码的在线设备");
        } catch (IOException exception) {
            showError("解析在线设备响应失败：" + exception.getMessage());
        }
    }

    private CompletableFuture<String> sendGet(String path) {
        HttpRequest request = HttpRequest.newBuilder(resolve(path))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
        return send(request);
    }

    private CompletableFuture<String> sendPost(String path, Object body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(resolve(path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            return send(request);
        } catch (IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private CompletableFuture<String> send(HttpRequest request) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new IllegalStateException("HTTP " + response.statusCode() + "：" + response.body());
                    }
                    return response.body();
                });
    }

    private URI resolve(String path) {
        String base = relayUrlField.getText().strip();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return URI.create(base + path);
    }

    private Void showAsyncError(Throwable throwable) {
        Platform.runLater(() -> showError(throwable.getCause() == null ? throwable.getMessage() : throwable.getCause().getMessage()));
        return null;
    }

    private void appendLog(String message) {
        taskLogsArea.appendText("[" + LocalTime.now().withNano(0) + "] " + message + System.lineSeparator());
    }

    private void showError(String message) {
        errorLabel.setText(message == null ? "未知错误" : message);
        appendLog("错误：" + errorLabel.getText());
    }

    private void refreshTaskButtons() {
        boolean connected = sessionId != null && !sessionId.isBlank();
        listRootButton.setDisable(!connected);
        readFileButton.setDisable(!connected);
        runCommandButton.setDisable(!connected);
        getLogsButton.setDisable(!connected);
        generateReportButton.setDisable(!connected);
        debugListDirButton.setDisable(!connected);
        debugGetLogsButton.setDisable(!connected);
    }

    private void clearError() {
        errorLabel.setText("");
    }

    private String pretty(String json) {
        try {
            Object value = objectMapper.readValue(json, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (IOException exception) {
            return json;
        }
    }

    private int parseTimeout() {
        try {
            return Integer.parseInt(blankToDefault(timeoutField.getText(), "30"));
        } catch (NumberFormatException exception) {
            return 30;
        }
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.strip();
    }

    private String blank(Object value) {
        return value == null ? "" : value.toString();
    }
}
