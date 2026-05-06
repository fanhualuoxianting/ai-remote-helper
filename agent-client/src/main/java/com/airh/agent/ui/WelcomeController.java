package com.airh.agent.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class WelcomeController {
    @FXML private VBox openAssistedCardButton;
    @FXML private VBox openControllerCardButton;
    @FXML private Button openAssistedButton;
    @FXML private Button openControllerButton;

    private Runnable openAssistedAction = () -> { };
    private Runnable openControllerAction = () -> { };

    @FXML
    private void initialize() {
        openAssistedCardButton.setOnMouseClicked(event -> openAssistedAction.run());
        openControllerCardButton.setOnMouseClicked(event -> openControllerAction.run());
    }

    @FXML
    private void openAssisted() {
        openAssistedAction.run();
    }

    @FXML
    private void openController() {
        openControllerAction.run();
    }

    public void setOpenAssistedAction(Runnable openAssistedAction) {
        this.openAssistedAction = openAssistedAction == null ? () -> { } : openAssistedAction;
    }

    public void setOpenControllerAction(Runnable openControllerAction) {
        this.openControllerAction = openControllerAction == null ? () -> { } : openControllerAction;
    }
}
