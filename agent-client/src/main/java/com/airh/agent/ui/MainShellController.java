package com.airh.agent.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class MainShellController {
    @FXML private StackPane root;
    @FXML private BorderPane shellRoot;
    @FXML private StackPane contentPane;
    @FXML private Button homeNavButton;
    @FXML private Button assistedNavButton;
    @FXML private Button controllerNavButton;
    @FXML private Button settingsNavButton;
    @FXML private Button aboutNavButton;

    private AgentClientApplication application;
    private AssistedController assistedController;
    private ControllerViewController controllerViewController;
    private WelcomeController welcomeController;
    private Parent welcomePage;
    private Parent assistedPage;
    private Parent controllerPage;
    private Parent settingsPage;
    private Parent aboutPage;

    public Parent getRoot() {
        return root;
    }

    public void setApplication(AgentClientApplication application) {
        this.application = application;
        showWelcomePage();
        showStartupEasterEggIfEnabled();
    }

    @FXML
    private void showHome() {
        showWelcomePage();
    }

    @FXML
    private void showAssisted() {
        showAssistedPage();
    }

    @FXML
    private void showController() {
        showControllerPage();
    }

    @FXML
    private void showSettings() {
        showNode(loadSettingsPage(), settingsNavButton);
    }

    @FXML
    private void showAbout() {
        showNode(loadAboutPage(), aboutNavButton);
    }

    public void showWelcomePage() {
        showNode(loadWelcomePage(), homeNavButton);
    }

    public void showAssistedPage() {
        showNode(loadAssistedPage(), assistedNavButton);
    }

    public void showControllerPage() {
        showNode(loadControllerPage(), controllerNavButton);
    }

    public void shutdown() {
        if (assistedController != null) {
            assistedController.shutdown();
        }
    }

    private Parent loadWelcomePage() {
        if (welcomePage == null) {
            FXMLLoader loader = createLoader("/fxml/WelcomeView.fxml");
            welcomePage = load(loader, "/fxml/WelcomeView.fxml");
            welcomeController = loader.getController();
            welcomeController.setOpenAssistedAction(this::showAssistedPage);
            welcomeController.setOpenControllerAction(this::showControllerPage);
        }
        return welcomePage;
    }

    private Parent loadAssistedPage() {
        if (assistedPage == null) {
            FXMLLoader loader = createLoader("/fxml/assisted.fxml");
            assistedPage = load(loader, "/fxml/assisted.fxml");
            assistedController = loader.getController();
            assistedController.setApplication(application);
            assistedController.configure(UUID.randomUUID().toString(), AgentClientApplication.resolveDeviceName());
        }
        return assistedPage;
    }

    private Parent loadControllerPage() {
        if (controllerPage == null) {
            FXMLLoader loader = createLoader("/fxml/controller.fxml");
            controllerPage = load(loader, "/fxml/controller.fxml");
            controllerViewController = loader.getController();
            controllerViewController.setApplication(application);
        }
        return controllerPage;
    }

    private Parent loadSettingsPage() {
        if (settingsPage == null) {
            settingsPage = loadPage("/fxml/SettingsView.fxml");
        }
        return settingsPage;
    }

    private Parent loadAboutPage() {
        if (aboutPage == null) {
            aboutPage = loadPage("/fxml/AboutView.fxml");
        }
        return aboutPage;
    }

    private void showNode(Parent page, Button selectedButton) {
        contentPane.getChildren().setAll(page);
        List<Button> buttons = List.of(homeNavButton, assistedNavButton, controllerNavButton, settingsNavButton, aboutNavButton);
        for (Button button : buttons) {
            button.getStyleClass().remove("nav-button-active");
        }
        if (!selectedButton.getStyleClass().contains("nav-button-active")) {
            selectedButton.getStyleClass().add("nav-button-active");
        }
    }

    private Parent loadPage(String resourcePath) {
        return load(createLoader(resourcePath), resourcePath);
    }

    private FXMLLoader createLoader(String resourcePath) {
        return new FXMLLoader(getClass().getResource(resourcePath));
    }

    private Parent load(FXMLLoader loader, String resourcePath) {
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("加载 UI 失败：" + resourcePath, exception);
        }
    }

    private void showStartupEasterEggIfEnabled() {
        if (!isMatrixEasterEggEnabled()) {
            return;
        }
        StartupMatrixOverlay overlay = new StartupMatrixOverlay(
                this::showAssistedPage,
                this::showControllerPage,
                resolveStartupEasterEggDuration()
        );
        root.getChildren().add(overlay);
        overlay.start(() -> root.getChildren().remove(overlay));
    }

    private boolean isMatrixEasterEggEnabled() {
        return "matrix".equalsIgnoreCase(System.getProperty("airh.startupEasterEgg"))
                || "matrix".equalsIgnoreCase(System.getenv("AIRH_STARTUP_EASTER_EGG"));
    }

    private Duration resolveStartupEasterEggDuration() {
        String rawValue = System.getProperty("airh.startupEasterEggDuration");
        if (rawValue == null || rawValue.isBlank()) {
            return StartupMatrixOverlay.DEFAULT_DURATION;
        }
        try {
            double seconds = Double.parseDouble(rawValue.trim());
            if (seconds > 0 && seconds <= 120) {
                return Duration.seconds(seconds);
            }
        } catch (NumberFormatException ignored) {
            // Fall back to the safe default if the development override is invalid.
        }
        return StartupMatrixOverlay.DEFAULT_DURATION;
    }
}
