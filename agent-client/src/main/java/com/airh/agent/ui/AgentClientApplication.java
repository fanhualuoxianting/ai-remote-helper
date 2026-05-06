package com.airh.agent.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

public class AgentClientApplication extends Application {
    private Stage primaryStage;
    private MainShellController mainShellController;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        stage.setTitle("AI Remote Helper");
        stage.setMinWidth(1200);
        stage.setMinHeight(760);
        try (InputStream iconStream = getClass().getResourceAsStream("/icons/app.png")) {
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            }
        }
        showMainShell();
        stage.show();
    }

    private void showMainShell() {
        MainShellController controller = loadView("/fxml/MainShell.fxml");
        mainShellController = controller;
        controller.setApplication(this);
        primaryStage.setOnCloseRequest(event -> controller.shutdown());
        primaryStage.setScene(createScene(controller.getRoot()));
    }

    public void showRoleSelectView() {
        if (mainShellController != null) {
            mainShellController.showWelcomePage();
        }
    }

    public void showAssistedView() {
        if (mainShellController != null) {
            mainShellController.showAssistedPage();
        }
    }

    public void showControllerView() {
        if (mainShellController != null) {
            mainShellController.showControllerPage();
        }
    }

    private Scene createScene(Parent root) {
        Scene scene = new Scene(root, 1400, 900);
        URL stylesheet = getClass().getResource("/styles/app.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        return scene;
    }

    <T> T loadView(String resourcePath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
            loader.load();
            return loader.getController();
        } catch (IOException exception) {
            throw new IllegalStateException("加载 UI 失败：" + resourcePath, exception);
        }
    }

    static String resolveDeviceName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException exception) {
            return System.getProperty("user.name", "unknown-device");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
