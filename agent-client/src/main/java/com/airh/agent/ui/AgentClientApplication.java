package com.airh.agent.ui;

import com.airh.agent.connection.AgentConnectionClient;
import com.airh.agent.connection.AgentConnectionListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.UUID;

public class AgentClientApplication extends Application {
    private AgentConnectionClient connectionClient;
    private Path authorizedDirectoryPath;

    @Override
    public void start(Stage stage) {
        String deviceIdValue = UUID.randomUUID().toString();
        String deviceName = resolveDeviceName();
        connectionClient = new AgentConnectionClient(deviceIdValue, deviceName, new UiConnectionListener());

        Label title = new Label("AI Remote Helper");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        TextField serverUrl = new TextField("http://localhost:8080");
        serverUrl.setPrefColumnCount(36);

        Label status = new Label("当前状态：未连接");
        Label deviceId = new Label("当前 deviceId：" + deviceIdValue);
        Label connectionCode = new Label("当前连接码：未生成");
        Label authorizedDirectory = new Label("授权目录：未选择");
        Label currentTask = new Label("当前任务：无");

        TextArea logs = new TextArea();
        logs.setPromptText("实时日志");
        logs.setEditable(false);
        logs.setPrefRowCount(12);

        Button chooseDirectoryButton = new Button("选择授权目录");
        Button connectButton = new Button("连接");
        Button disconnectButton = new Button("断开");
        disconnectButton.setDisable(true);

        chooseDirectoryButton.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择授权工作目录");
            File directory = chooser.showDialog(stage);
            if (directory != null) {
                authorizedDirectoryPath = directory.toPath().toAbsolutePath().normalize();
                authorizedDirectory.setText("授权目录：" + authorizedDirectoryPath);
                appendLog(logs, "已选择授权目录：" + authorizedDirectoryPath);
            }
        });

        connectButton.setOnAction(event -> {
            if (authorizedDirectoryPath == null) {
                appendLog(logs, "连接失败：请先手动选择授权目录");
                return;
            }
            connectButton.setDisable(true);
            disconnectButton.setDisable(false);
            status.setText("当前状态：连接中");
            try {
                connectionClient.connect(serverUrl.getText(), authorizedDirectoryPath.toString());
            } catch (RuntimeException exception) {
                appendLog(logs, "连接失败：" + exception.getMessage());
                status.setText("当前状态：未连接");
                connectButton.setDisable(false);
                disconnectButton.setDisable(true);
            }
        });

        disconnectButton.setOnAction(event -> {
            connectionClient.disconnect();
            status.setText("当前状态：未连接");
            connectionCode.setText("当前连接码：未生成");
            connectButton.setDisable(false);
            disconnectButton.setDisable(true);
        });

        HBox serverRow = new HBox(8, new Label("服务器地址："), serverUrl);
        HBox actions = new HBox(8, chooseDirectoryButton, connectButton, disconnectButton);
        VBox root = new VBox(12, title, serverRow, status, deviceId, connectionCode, authorizedDirectory, currentTask, logs, actions);
        root.setPadding(new Insets(16));

        stage.setTitle("AI Remote Helper Agent");
        stage.setScene(new Scene(root, 640, 420));
        stage.setOnCloseRequest(event -> connectionClient.shutdown());
        stage.show();

        UiConnectionListener.bind(status, connectionCode, currentTask, logs, connectButton, disconnectButton);
        appendLog(logs, "Agent UI 已启动，等待用户选择授权目录并连接");
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static void appendLog(TextArea logs, String message) {
        logs.appendText("[" + java.time.LocalTime.now().withNano(0) + "] " + message + System.lineSeparator());
    }

    private static String resolveDeviceName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException exception) {
            return System.getProperty("user.name", "unknown-device");
        }
    }

    private static final class UiConnectionListener implements AgentConnectionListener {
        private static Label status;
        private static Label connectionCode;
        private static Label currentTask;
        private static TextArea logs;
        private static Button connectButton;
        private static Button disconnectButton;

        static void bind(Label statusLabel, Label connectionCodeLabel, Label currentTaskLabel, TextArea logsArea, Button connect, Button disconnect) {
            status = statusLabel;
            connectionCode = connectionCodeLabel;
            currentTask = currentTaskLabel;
            logs = logsArea;
            connectButton = connect;
            disconnectButton = disconnect;
        }

        @Override
        public void onConnecting(String serverUrl) {
            Platform.runLater(() -> appendLog(logs, "连接中：" + serverUrl));
        }

        @Override
        public void onConnected(String sessionId, String code) {
            Platform.runLater(() -> {
                status.setText("当前状态：已连接");
                connectionCode.setText("当前连接码：" + code);
                appendLog(logs, "连接成功，sessionId：" + sessionId + "，连接码：" + code);
            });
        }

        @Override
        public void onDisconnected(String reason) {
            Platform.runLater(() -> {
                status.setText("当前状态：未连接");
                connectionCode.setText("当前连接码：未生成");
                currentTask.setText("当前任务：无");
                connectButton.setDisable(false);
                disconnectButton.setDisable(true);
                appendLog(logs, reason);
            });
        }

        @Override
        public void onTaskStarted(String taskId, String taskType, String payloadSummary) {
            Platform.runLater(() -> {
                currentTask.setText("当前任务：" + taskId + " / " + taskType);
                appendLog(logs, "收到任务：" + taskId + "，类型：" + taskType);
                appendLog(logs, "任务 payload 摘要：" + payloadSummary);
                appendLog(logs, "任务开始：本阶段只返回模拟结果");
            });
        }

        @Override
        public void onTaskFinished(String taskId, String taskStatus, String summary) {
            Platform.runLater(() -> {
                currentTask.setText("当前任务：无");
                appendLog(logs, "任务结束：" + taskId + "，状态：" + taskStatus + "，" + summary);
            });
        }

        @Override
        public void onLog(String message) {
            Platform.runLater(() -> appendLog(logs, message));
        }
    }
}
