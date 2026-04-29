package com.airh.agent.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AgentClientApplication extends Application {
    @Override
    public void start(Stage stage) {
        Label title = new Label("AI Remote Helper");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label status = new Label("当前状态：未连接");
        Label authorizedDirectory = new Label("授权目录：未选择");

        TextArea logs = new TextArea();
        logs.setPromptText("日志区域占位");
        logs.setEditable(false);
        logs.setPrefRowCount(12);

        Button connectButton = new Button("连接");
        Button disconnectButton = new Button("断开");

        HBox actions = new HBox(8, connectButton, disconnectButton);
        VBox root = new VBox(12, title, status, authorizedDirectory, logs, actions);
        root.setPadding(new Insets(16));

        stage.setTitle("AI Remote Helper Agent");
        stage.setScene(new Scene(root, 640, 420));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
