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
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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
    @FXML private TextArea directAiRequestArea;
    @FXML private Label errorLabel;
    @FXML private Label directAiStatusLabel;
    @FXML private Button listRootButton;
    @FXML private Button readFileButton;
    @FXML private Button runCommandButton;
    @FXML private Button getLogsButton;
    @FXML private Button generateReportButton;
    @FXML private Button debugListDirButton;
    @FXML private Button debugGetLogsButton;
    @FXML private TextArea helpRequestsArea;
    @FXML private Label helpRequestReviewStatusLabel;
    @FXML private Button refreshHelpRequestsButton;
    @FXML private Button approveHelpRequestButton;
    @FXML private Button rejectHelpRequestButton;
    @FXML private Button relaunchAiButton;
    @FXML private Button launchDirectAiButton;
    @FXML private Button importSelectedHelpRequestButton;
    @FXML private Button executeNextAiTaskButton;
    @FXML private Button openAiRunFolderButton;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private AgentClientApplication application;
    private String sessionId;
    private JsonNode selectedHelpRequest;
    private final AiRunnerService aiRunnerService = new AiRunnerService();
    private AiRunnerService.AiLaunchSession activeAiLaunchSession;
    private final ScheduledExecutorService helpRequestPollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "airh-help-request-poller");
        thread.setDaemon(true);
        return thread;
    });
    private ScheduledFuture<?> helpRequestPollingTask;

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
        directAiStatusLabel.setText("连接远程设备后，可以直接在这里给 AI 下达远程协助目标。");
        refreshTaskButtons();
        refreshHelpRequestButtons();
        appendLog("控制台已启动。请输入对方连接码，通过 relay-server 建立手动调试会话。");
    }

    @FXML
    private void backToRoleSelect() {
        stopHelpRequestPolling();
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
    private void refreshHelpRequests() {
        if (sessionId == null || sessionId.isBlank()) {
            helpRequestsArea.setText("请先连接远程设备。");
            refreshHelpRequestButtons();
            return;
        }
        sendGet("/api/sessions/" + sessionId + "/help-requests")
                .thenAccept(body -> Platform.runLater(() -> renderHelpRequests(body)))
                .exceptionally(this::showAsyncError);
    }

    @FXML
    private void approveSelectedHelpRequest() {
        reviewSelectedHelpRequest("approve");
    }

    @FXML
    private void rejectSelectedHelpRequest() {
        reviewSelectedHelpRequest("reject");
    }

    @FXML
    private void relaunchAiForSelectedHelpRequest() {
        if (selectedHelpRequest == null) {
            showError("没有可拉起 AI 的需求");
            return;
        }
        launchAiForRequest(selectedHelpRequest);
    }

    @FXML
    private void launchDirectAiRequest() {
        clearError();
        if (sessionId == null || sessionId.isBlank()) {
            showError("请先连接远程设备，再拉起 AI");
            return;
        }
        String requestContent = directAiRequestArea.getText() == null ? "" : directAiRequestArea.getText().strip();
        if (requestContent.isBlank()) {
            showError("请输入要让 AI 执行的远程协助目标");
            return;
        }
        launchDirectAiButton.setDisable(true);
        directAiStatusLabel.setText("正在拉起可见 Codex 会话...");
        try {
            AiRunnerService.AiLaunchSession launchSession = aiRunnerService.launchDirectAssist(baseRelayUrl(), sessionId, requestContent);
            activeAiLaunchSession = launchSession;
            directAiStatusLabel.setText("Codex 已启动。让 AI 往任务队列写 JSON，然后点“执行下一条 AI 任务”。目录：" + launchSession.runDir());
            appendLog("已直接拉起 Codex 处理当前远程会话");
        } catch (IOException | RuntimeException exception) {
            String message = exception.getMessage() == null ? exception.toString() : exception.getMessage();
            directAiStatusLabel.setText("拉起 Codex 失败：" + message);
            showError("拉起 Codex 失败：" + message);
        } finally {
            launchDirectAiButton.setDisable(false);
            refreshHelpRequestButtons();
        }
    }

    @FXML
    private void executeNextAiTask() {
        clearError();
        if (sessionId == null || sessionId.isBlank()) {
            showError("请先连接远程设备");
            return;
        }
        if (activeAiLaunchSession == null) {
            showError("还没有已启动的 AI 会话");
            return;
        }
        Path requestFile;
        try {
            requestFile = findNextPendingAiTaskFile(activeAiLaunchSession.requestQueueDir());
        } catch (IOException exception) {
            showError("读取 AI 任务目录失败：" + exception.getMessage());
            return;
        }
        if (requestFile == null) {
            directAiStatusLabel.setText("当前没有待执行的 AI 任务文件。请让 AI 继续往队列写入 JSON。");
            refreshHelpRequestButtons();
            return;
        }
        try {
            QueuedAiTask queuedAiTask = parseQueuedAiTask(requestFile);
            directAiStatusLabel.setText("正在执行 AI 任务：" + queuedAiTask.summary());
            submitTask(queuedAiTask.taskType(), queuedAiTask.data(), queuedAiTask.timeoutSeconds(),
                    body -> finalizeAiTaskRequest(requestFile, queuedAiTask, body));
        } catch (IOException | IllegalArgumentException exception) {
            showError("解析 AI 任务文件失败：" + exception.getMessage());
            writeAiFailureResult(requestFile, exception.getMessage() == null ? exception.toString() : exception.getMessage());
        }
    }

    @FXML
    private void openAiRunFolder() {
        clearError();
        if (activeAiLaunchSession == null) {
            showError("还没有已启动的 AI 会话目录");
            return;
        }
        try {
            new ProcessBuilder("explorer.exe", activeAiLaunchSession.runDir().toString()).start();
        } catch (IOException exception) {
            showError("打开 AI 目录失败：" + exception.getMessage());
        }
    }

    @FXML
    private void importSelectedHelpRequest() {
        if (selectedHelpRequest == null) {
            showError("当前没有可导入的需求");
            return;
        }
        String content = selectedHelpRequest.path("content").asText("");
        if (content.isBlank()) {
            showError("选中的需求内容为空");
            return;
        }
        directAiRequestArea.setText(content);
        directAiStatusLabel.setText("已导入审核需求。你可以继续补充说明后再拉起 Codex。");
    }

    @FXML
    private void generateReport() {
        createTask(TaskType.GENERATE_REPORT, Map.of(), 30);
    }

    private void createTask(TaskType taskType, Map<String, Object> data, int timeoutSeconds) {
        submitTask(taskType, data, timeoutSeconds, null);
    }

    private void submitTask(TaskType taskType, Map<String, Object> data, int timeoutSeconds,
                            java.util.function.Consumer<String> successCallback) {
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
                .thenAccept(body -> Platform.runLater(() -> {
                    handleCreatedTask(taskType, body);
                    if (successCallback != null) {
                        successCallback.accept(body);
                    }
                }))
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
                    directAiStatusLabel.setText("已连接远程设备，可以直接输入 AI 协助目标。");
                    refreshTaskButtons();
                    appendLog("已匹配远程设备：" + deviceIdLabel.getText() + "，sessionId=" + sessionId);
                    startHelpRequestPolling();
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

    private void renderHelpRequests(String body) {
        try {
            JsonNode requests = objectMapper.readTree(body);
            selectedHelpRequest = null;
            if (!requests.isArray() || requests.isEmpty()) {
                helpRequestsArea.setText("暂无需求。等待被协助方在“提交需求”Tab 中提交。");
                helpRequestReviewStatusLabel.setText("暂无待审核需求");
                refreshHelpRequestButtons();
                return;
            }
            StringBuilder builder = new StringBuilder();
            JsonNode firstActionable = null;
            for (JsonNode request : requests) {
                String status = request.path("status").asText("-");
                if (firstActionable == null && ("PENDING".equals(status) || "APPROVED".equals(status)
                        || "AI_LAUNCH_FAILED".equals(status))) {
                    firstActionable = request;
                }
                builder.append("[").append(status).append("] ")
                        .append(request.path("createdAt").asText("-")).append(System.lineSeparator())
                        .append(request.path("content").asText("")).append(System.lineSeparator());
                String note = request.path("reviewerNote").asText("");
                if (!note.isBlank()) {
                    builder.append("备注：").append(note).append(System.lineSeparator());
                }
                builder.append("requestId: ").append(request.path("requestId").asText("-"))
                        .append(System.lineSeparator()).append(System.lineSeparator());
            }
            selectedHelpRequest = firstActionable == null ? requests.get(0) : firstActionable;
            helpRequestsArea.setText(builder.toString());
            helpRequestReviewStatusLabel.setText(selectedHelpRequest == null
                    ? "没有可操作需求"
                    : "当前操作目标：" + selectedHelpRequest.path("requestId").asText("-")
                    + " / " + selectedHelpRequest.path("status").asText("-"));
            refreshHelpRequestButtons();
        } catch (IOException exception) {
            showError("解析需求列表失败：" + exception.getMessage());
        }
    }

    private void reviewSelectedHelpRequest(String action) {
        if (selectedHelpRequest == null) {
            showError("没有选中的需求");
            return;
        }
        String requestId = selectedHelpRequest.path("requestId").asText("");
        if (requestId.isBlank()) {
            showError("需求缺少 requestId");
            return;
        }
        String note = "";
        if ("reject".equals(action)) {
            TextInputDialog dialog = new TextInputDialog("请补充需求细节后再提交");
            dialog.setTitle("拒绝需求");
            dialog.setHeaderText("填写拒绝原因");
            dialog.setContentText("备注：");
            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return;
            }
            note = result.get();
        }
        Map<String, Object> request = Map.of("reviewerNote", note);
        sendPost("/api/sessions/" + sessionId + "/help-requests/" + requestId + "/" + action, request)
                .thenAccept(body -> Platform.runLater(() -> {
                    if ("approve".equals(action)) {
                        try {
                            JsonNode approved = objectMapper.readTree(body);
                            helpRequestReviewStatusLabel.setText("已批准，正在拉起 Codex...");
                            launchAiForRequest(approved);
                        } catch (IOException exception) {
                            showError("解析批准响应失败：" + exception.getMessage());
                        }
                    } else {
                        helpRequestReviewStatusLabel.setText("已拒绝需求：" + requestId);
                        refreshHelpRequests();
                    }
                }))
                .exceptionally(this::showAsyncError);
    }

    private void launchAiForRequest(JsonNode request) {
        String requestId = request.path("requestId").asText("");
        String content = request.path("content").asText("");
        try {
            AiRunnerService.AiLaunchSession launchSession = aiRunnerService.launchCodex(requestId, baseRelayUrl(), sessionId, content);
            activeAiLaunchSession = launchSession;
            helpRequestReviewStatusLabel.setText("Codex 已启动。AI 可把任务写入队列，由客户端代为执行。");
            directAiStatusLabel.setText("当前 AI 工作目录：" + launchSession.runDir() + "。等待 AI 写入任务 JSON。");
            sendPost("/api/sessions/" + sessionId + "/help-requests/" + requestId + "/ai-launched",
                    Map.of("reviewerNote", "Codex prompt: " + launchSession.promptPath()))
                    .thenAccept(ignored -> Platform.runLater(this::refreshHelpRequests))
                    .exceptionally(this::showAsyncError);
            appendLog("已为需求拉起 Codex：" + requestId);
        } catch (IOException | RuntimeException exception) {
            helpRequestReviewStatusLabel.setText("拉起 Codex 失败：" + exception.getMessage());
            sendPost("/api/sessions/" + sessionId + "/help-requests/" + requestId + "/ai-launch-failed",
                    Map.of("reviewerNote", exception.getMessage() == null ? exception.toString() : exception.getMessage()))
                    .thenAccept(ignored -> Platform.runLater(this::refreshHelpRequests))
                    .exceptionally(this::showAsyncError);
        }
    }

    private void startHelpRequestPolling() {
        stopHelpRequestPolling();
        refreshHelpRequests();
        helpRequestPollingTask = helpRequestPollingExecutor.scheduleAtFixedRate(() ->
                Platform.runLater(this::refreshHelpRequests), 3, 3, TimeUnit.SECONDS);
    }

    private void stopHelpRequestPolling() {
        if (helpRequestPollingTask != null) {
            helpRequestPollingTask.cancel(false);
            helpRequestPollingTask = null;
        }
    }

    private String baseRelayUrl() {
        String base = relayUrlField.getText().strip();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
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
        refreshHelpRequestButtons();
    }

    private void refreshHelpRequestButtons() {
        boolean connected = sessionId != null && !sessionId.isBlank();
        boolean selected = selectedHelpRequest != null;
        String status = selected ? selectedHelpRequest.path("status").asText("") : "";
        if (refreshHelpRequestsButton != null) refreshHelpRequestsButton.setDisable(!connected);
        if (approveHelpRequestButton != null) {
            approveHelpRequestButton.setDisable(!connected || !selected
                    || !("PENDING".equals(status) || "AI_LAUNCH_FAILED".equals(status)));
        }
        if (rejectHelpRequestButton != null) {
            rejectHelpRequestButton.setDisable(!connected || !selected
                    || !("PENDING".equals(status) || "AI_LAUNCH_FAILED".equals(status)));
        }
        if (relaunchAiButton != null) {
            relaunchAiButton.setDisable(!connected || !selected
                    || !("APPROVED".equals(status) || "AI_LAUNCH_FAILED".equals(status) || "AI_LAUNCHED".equals(status)));
        }
        if (launchDirectAiButton != null) launchDirectAiButton.setDisable(!connected);
        if (importSelectedHelpRequestButton != null) importSelectedHelpRequestButton.setDisable(!connected || !selected);
        if (executeNextAiTaskButton != null) executeNextAiTaskButton.setDisable(!connected || activeAiLaunchSession == null);
        if (openAiRunFolderButton != null) openAiRunFolderButton.setDisable(activeAiLaunchSession == null);
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

    private Path findNextPendingAiTaskFile(Path requestQueueDir) throws IOException {
        if (requestQueueDir == null || !Files.isDirectory(requestQueueDir)) {
            return null;
        }
        try (Stream<Path> files = Files.list(requestQueueDir)) {
            return files
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .findFirst()
                    .orElse(null);
        }
    }

    private QueuedAiTask parseQueuedAiTask(Path requestFile) throws IOException {
        JsonNode rootNode = objectMapper.readTree(Files.readString(requestFile, StandardCharsets.UTF_8));
        String taskTypeText = rootNode.path("taskType").asText("").strip();
        if (taskTypeText.isBlank()) {
            throw new IllegalArgumentException("缺少 taskType");
        }
        TaskType taskType;
        try {
            taskType = TaskType.valueOf(taskTypeText);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("不支持的 taskType: " + taskTypeText, exception);
        }
        JsonNode dataNode = rootNode.path("payload").path("data");
        if (dataNode.isMissingNode() || dataNode.isNull()) {
            dataNode = rootNode.path("data");
        }
        if (!dataNode.isObject()) {
            throw new IllegalArgumentException("缺少 data/payload.data 对象");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> data = objectMapper.convertValue(dataNode, Map.class);
        String summary = rootNode.path("summary").asText(taskType.name());
        int timeoutSeconds = rootNode.path("timeoutSeconds").asInt(30);
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 30;
        }
        return new QueuedAiTask(taskType, data, timeoutSeconds, summary);
    }

    private void finalizeAiTaskRequest(Path requestFile, QueuedAiTask queuedAiTask, String taskResponseBody) {
        try {
            if (activeAiLaunchSession == null) {
                throw new IllegalStateException("AI 会话已丢失");
            }
            JsonNode responseNode = objectMapper.readTree(taskResponseBody);
            Path resultFile = resolveResultFile(activeAiLaunchSession.resultQueueDir(), requestFile);
            Map<String, Object> resultPayload = new LinkedHashMap<>();
            resultPayload.put("summary", queuedAiTask.summary());
            resultPayload.put("sourceRequest", requestFile.getFileName().toString());
            resultPayload.put("taskType", queuedAiTask.taskType().name());
            resultPayload.put("submittedAt", Instant.now().toString());
            resultPayload.put("relayResponse", objectMapper.convertValue(responseNode, Object.class));
            Files.writeString(resultFile, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultPayload), StandardCharsets.UTF_8);
            archiveAiRequestFile(requestFile, "processed");
            directAiStatusLabel.setText("已执行 AI 任务：" + queuedAiTask.summary() + "。结果已写回 " + resultFile.getFileName());
            appendLog("AI 任务已提交：" + queuedAiTask.summary() + "，来源文件=" + requestFile.getFileName());
        } catch (Exception exception) {
            showError("写回 AI 任务结果失败：" + exception.getMessage());
        } finally {
            refreshHelpRequestButtons();
        }
    }

    private void writeAiFailureResult(Path requestFile, String errorMessage) {
        if (activeAiLaunchSession == null || requestFile == null) {
            return;
        }
        try {
            Path resultFile = resolveResultFile(activeAiLaunchSession.resultQueueDir(), requestFile);
            Map<String, Object> resultPayload = new LinkedHashMap<>();
            resultPayload.put("sourceRequest", requestFile.getFileName().toString());
            resultPayload.put("failedAt", Instant.now().toString());
            resultPayload.put("error", errorMessage);
            Files.writeString(resultFile, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultPayload), StandardCharsets.UTF_8);
            archiveAiRequestFile(requestFile, "failed");
            directAiStatusLabel.setText("AI 任务文件执行失败，已把错误写回结果目录。");
            refreshHelpRequestButtons();
        } catch (Exception ignored) {
            // Keep the original request file in place if the feedback write also fails.
        }
    }

    private Path resolveResultFile(Path resultQueueDir, Path requestFile) {
        String baseName = requestFile.getFileName().toString();
        if (baseName.endsWith(".json")) {
            baseName = baseName.substring(0, baseName.length() - 5);
        }
        return resultQueueDir.resolve(baseName + ".result.json");
    }

    private void archiveAiRequestFile(Path requestFile, String bucket) throws IOException {
        Path archiveDir = requestFile.getParent().resolve(bucket);
        Files.createDirectories(archiveDir);
        Files.move(requestFile, archiveDir.resolve(requestFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    private record QueuedAiTask(TaskType taskType, Map<String, Object> data, int timeoutSeconds, String summary) {
    }
}
