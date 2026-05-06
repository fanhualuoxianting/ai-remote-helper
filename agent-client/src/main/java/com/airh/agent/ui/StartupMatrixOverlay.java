package com.airh.agent.ui;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.Random;

final class StartupMatrixOverlay extends StackPane {
    static final Duration DEFAULT_DURATION = Duration.seconds(30);

    private static final String MATRIX_CHARS =
            "アイウエオカキクケコサシスセソタチツテト0123456789AIRHREMOTEHELPER";
    private static final String[] TERMINAL_LINES = {
            "booting local-only assist shell...",
            "loading authorized-directory guard...",
            "binding relay session workspace...",
            "warming visible operation log...",
            "verifying no hidden background mode...",
            "arming manual disconnect route...",
            "awaiting operator target selection..."
    };

    private final Runnable assistedAction;
    private final Runnable controllerAction;
    private final Duration duration;
    private final Canvas matrixCanvas = new Canvas();
    private final Label terminalLine = new Label();
    private final VBox targetPanel = new VBox(14);
    private final Random random = new Random();

    private AnimationTimer animationTimer;
    private Timeline terminalTicker;
    private PauseTransition finishTimer;
    private PauseTransition mouseSkipArmTimer;
    private Runnable onClose;
    private double[] drops = new double[0];
    private boolean targetVisible;
    private boolean mouseSkipArmed;
    private boolean closed;

    StartupMatrixOverlay(Runnable assistedAction, Runnable controllerAction, Duration duration) {
        this.assistedAction = assistedAction;
        this.controllerAction = controllerAction;
        this.duration = duration == null ? DEFAULT_DURATION : duration;
        buildView();
        configureInput();
    }

    void start(Runnable onClose) {
        this.onClose = onClose;
        startMatrixAnimation();
        startTerminalTicker();
        startFinishTimer();
        armMouseSkipAfterShortDelay();
        Platform.runLater(this::requestFocus);
    }

    private void buildView() {
        getStyleClass().add("matrix-overlay");
        setFocusTraversable(true);
        matrixCanvas.widthProperty().bind(widthProperty());
        matrixCanvas.heightProperty().bind(heightProperty());
        widthProperty().addListener((observable, oldValue, newValue) -> resetDrops());
        heightProperty().addListener((observable, oldValue, newValue) -> resetDrops());

        VBox terminalPanel = createTerminalPanel();
        targetPanel.getStyleClass().add("matrix-target-panel");
        targetPanel.setId("matrixTargetPanel");
        targetPanel.setAccessibleText("选择你的目标");
        targetPanel.setAlignment(Pos.CENTER_LEFT);
        targetPanel.setMaxWidth(520);
        targetPanel.setVisible(false);
        targetPanel.setManaged(false);
        buildTargetPanelContent();

        StackPane.setAlignment(terminalPanel, Pos.BOTTOM_LEFT);
        StackPane.setMargin(terminalPanel, new Insets(0, 0, 34, 34));
        getChildren().addAll(matrixCanvas, terminalPanel, targetPanel);
    }

    private VBox createTerminalPanel() {
        Label title = new Label("AI REMOTE HELPER // DEV MATRIX BOOT");
        title.getStyleClass().add("matrix-terminal-title");

        terminalLine.getStyleClass().add("matrix-terminal-line");
        terminalLine.setText(TERMINAL_LINES[0]);

        Label hint = new Label("Esc / click / mouse move: skip animation");
        hint.getStyleClass().add("matrix-terminal-hint");

        Button skipButton = new Button("跳过启动动画");
        skipButton.getStyleClass().add("matrix-skip-button");
        skipButton.setId("matrixSkipButton");
        skipButton.setAccessibleText("跳过启动动画");
        skipButton.setOnAction(event -> {
            event.consume();
            revealTargetPanel();
        });

        VBox terminalPanel = new VBox(8, title, terminalLine, hint, skipButton);
        terminalPanel.getStyleClass().add("matrix-terminal");
        terminalPanel.setMaxWidth(420);
        return terminalPanel;
    }

    private void buildTargetPanelContent() {
        Label eyebrow = new Label("TARGET SELECTION");
        eyebrow.getStyleClass().add("matrix-target-eyebrow");

        Label title = new Label("选择你的目标");
        title.getStyleClass().add("matrix-target-title");

        Label copy = new Label("启动彩蛋已结束。请选择进入被协助模式，或进入协助者工作台。");
        copy.getStyleClass().add("matrix-target-copy");
        copy.setWrapText(true);

        Button assistedButton = new Button("我需要别人帮忙");
        assistedButton.getStyleClass().add("matrix-target-primary");
        assistedButton.setId("matrixAssistedTargetButton");
        assistedButton.setAccessibleText("我需要别人帮忙");
        assistedButton.setMaxWidth(Double.MAX_VALUE);
        assistedButton.setOnAction(event -> {
            event.consume();
            closeThenRun(assistedAction);
        });

        Button controllerButton = new Button("我要帮别人处理项目");
        controllerButton.getStyleClass().add("matrix-target-secondary");
        controllerButton.setId("matrixControllerTargetButton");
        controllerButton.setAccessibleText("我要帮别人处理项目");
        controllerButton.setMaxWidth(Double.MAX_VALUE);
        controllerButton.setOnAction(event -> {
            event.consume();
            closeThenRun(controllerAction);
        });

        HBox buttons = new HBox(12, assistedButton, controllerButton);
        buttons.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(assistedButton, Priority.ALWAYS);
        HBox.setHgrow(controllerButton, Priority.ALWAYS);

        Region divider = new Region();
        divider.getStyleClass().add("matrix-target-divider");

        targetPanel.getChildren().setAll(eyebrow, title, copy, divider, buttons);
    }

    private void configureInput() {
        setOnMouseClicked(event -> revealTargetPanel());
        setOnMouseMoved(event -> {
            if (mouseSkipArmed) {
                revealTargetPanel();
            }
        });
        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                revealTargetPanel();
            }
        });
    }

    private void startMatrixAnimation() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                drawFrame();
            }
        };
        animationTimer.start();
    }

    private void startTerminalTicker() {
        terminalTicker = new Timeline(new KeyFrame(Duration.millis(520), event -> {
            int index = random.nextInt(TERMINAL_LINES.length);
            terminalLine.setText("> " + TERMINAL_LINES[index]);
        }));
        terminalTicker.setCycleCount(Timeline.INDEFINITE);
        terminalTicker.play();
    }

    private void startFinishTimer() {
        finishTimer = new PauseTransition(duration);
        finishTimer.setOnFinished(event -> revealTargetPanel());
        finishTimer.playFromStart();
    }

    private void armMouseSkipAfterShortDelay() {
        mouseSkipArmTimer = new PauseTransition(Duration.millis(700));
        mouseSkipArmTimer.setOnFinished(event -> mouseSkipArmed = true);
        mouseSkipArmTimer.playFromStart();
    }

    private void revealTargetPanel() {
        if (targetVisible) {
            return;
        }
        targetVisible = true;
        stopTimers();
        targetPanel.setManaged(true);
        targetPanel.setVisible(true);
        drawDimmedFrame();
        Platform.runLater(targetPanel::requestFocus);
    }

    private void closeThenRun(Runnable action) {
        if (closed) {
            return;
        }
        closed = true;
        stopTimers();
        if (onClose != null) {
            onClose.run();
        }
        if (action != null) {
            action.run();
        }
    }

    private void stopTimers() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        if (terminalTicker != null) {
            terminalTicker.stop();
        }
        if (finishTimer != null) {
            finishTimer.stop();
        }
        if (mouseSkipArmTimer != null) {
            mouseSkipArmTimer.stop();
        }
    }

    private void resetDrops() {
        int columns = Math.max(1, (int) Math.ceil(getWidth() / 16.0));
        drops = new double[columns];
        for (int i = 0; i < drops.length; i++) {
            drops[i] = random.nextDouble() * Math.max(1, getHeight());
        }
    }

    private void drawFrame() {
        if (drops.length == 0) {
            resetDrops();
        }
        GraphicsContext graphics = matrixCanvas.getGraphicsContext2D();
        double width = matrixCanvas.getWidth();
        double height = matrixCanvas.getHeight();

        graphics.setFill(Color.rgb(2, 8, 5, 0.16));
        graphics.fillRect(0, 0, width, height);
        graphics.setFont(Font.font("Cascadia Mono", FontWeight.BOLD, 15));

        for (int column = 0; column < drops.length; column++) {
            double x = column * 16.0;
            double y = drops[column];
            graphics.setFill(Color.rgb(178, 255, 196, 0.92));
            graphics.fillText(randomChar(), x, y);
            graphics.setFill(Color.rgb(34, 197, 94, 0.62));
            graphics.fillText(randomChar(), x, y - 18);
            drops[column] += 16.0 + random.nextDouble() * 11.0;
            if (drops[column] > height + 32 && random.nextDouble() > 0.965) {
                drops[column] = -random.nextDouble() * 180.0;
            }
        }

        drawScanLines(graphics, width, height);
    }

    private void drawDimmedFrame() {
        GraphicsContext graphics = matrixCanvas.getGraphicsContext2D();
        graphics.setFill(Color.rgb(0, 0, 0, 0.72));
        graphics.fillRect(0, 0, matrixCanvas.getWidth(), matrixCanvas.getHeight());
        drawScanLines(graphics, matrixCanvas.getWidth(), matrixCanvas.getHeight());
    }

    private void drawScanLines(GraphicsContext graphics, double width, double height) {
        graphics.setStroke(Color.rgb(34, 197, 94, 0.13));
        graphics.setLineWidth(1);
        for (int y = 0; y < height; y += 22) {
            graphics.strokeLine(0, y, width, y);
        }
        graphics.setFill(Color.rgb(12, 18, 14, 0.30));
        graphics.fillRect(0, 0, width, height);
    }

    private String randomChar() {
        int index = random.nextInt(MATRIX_CHARS.length());
        return String.valueOf(MATRIX_CHARS.charAt(index));
    }
}
