package com.airh.agent.ui;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;

public class RoleSelectController {
    @FXML
    private BorderPane root;

    private AgentClientApplication application;

    public BorderPane getRoot() {
        return root;
    }

    public void setApplication(AgentClientApplication application) {
        this.application = application;
    }

    @FXML
    private void openAssistedView() {
        application.showAssistedView();
    }

    @FXML
    private void openControllerView() {
        application.showControllerView();
    }
}
