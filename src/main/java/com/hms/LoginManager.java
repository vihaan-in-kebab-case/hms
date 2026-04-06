package com.hms;

import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class LoginManager {

    private final Stage stage;
    private final Runnable onLoginSuccess;

    // Loaded from credentials.txt
    private final Map<String, String> credentials = new HashMap<>();

    // UI refs for shake animation
    private VBox card;
    private Label errorLabel;

    public LoginManager(Stage stage, Runnable onLoginSuccess) {
        this.stage          = stage;
        this.onLoginSuccess = onLoginSuccess;
        loadCredentials();
    }

    // ── Read credentials.txt from classpath ──────────────────────────
    private void loadCredentials() {
        try (InputStream is = getClass().getResourceAsStream("/credentials.txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("=", 2);
                if (parts.length == 2) credentials.put(parts[0].trim(), parts[1].trim());
            }
        } catch (Exception e) {
            System.err.println("Could not load credentials: " + e.getMessage());
        }
    }

    // ── Show login screen ─────────────────────────────────────────────
    public void show() {
        // ── Background ────────────────────────────────────────────────
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0F1729;");

        // Decorative gold accent bar at top
        Rectangle topBar = new Rectangle();
        topBar.setHeight(4);
        topBar.setFill(Color.web("#D4AF37"));
        topBar.widthProperty().bind(root.widthProperty());
        StackPane.setAlignment(topBar, Pos.TOP_CENTER);

        // ── Card ──────────────────────────────────────────────────────
        card = new VBox(18);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(44, 48, 44, 48));
        card.setMaxWidth(400);
        card.setStyle(
            "-fx-background-color: #1A2540;" +
            "-fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 32, 0, 0, 8);"
        );

        // Hotel icon label
        Label icon = new Label("🏨");
        icon.setStyle("-fx-font-size: 42px;");

        Label appName = new Label("THE GRAND HOTEL");
        appName.setStyle(
            "-fx-text-fill: #D4AF37;" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: 800;" +
            "-fx-letter-spacing: 2;"
        );

        Label subtitle = new Label("Management System");
        subtitle.setStyle("-fx-text-fill: #8899BB; -fx-font-size: 12px;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2E3F62;");
        sep.setPadding(new Insets(4, 0, 4, 0));

        // Username
        Label userLbl = new Label("Username");
        userLbl.setStyle("-fx-text-fill: #8899BB; -fx-font-size: 11px; -fx-font-weight: 600;");
        userLbl.setAlignment(Pos.CENTER_LEFT);

        TextField userField = new TextField();
        userField.setPromptText("Enter username");
        userField.getStyleClass().add("hms-field");
        userField.setMaxWidth(Double.MAX_VALUE);

        // Password
        Label passLbl = new Label("Password");
        passLbl.setStyle("-fx-text-fill: #8899BB; -fx-font-size: 11px; -fx-font-weight: 600;");
        passLbl.setAlignment(Pos.CENTER_LEFT);

        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter password");
        passField.getStyleClass().add("hms-field");
        passField.setMaxWidth(Double.MAX_VALUE);

        // Error label
        errorLabel = new Label("");
        errorLabel.setStyle(
            "-fx-text-fill: #E74C3C;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: 600;"
        );
        errorLabel.setVisible(false);

        // Login button
        Button loginBtn = new Button("Sign In");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.getStyleClass().addAll("btn", "btn-primary");
        loginBtn.setStyle(loginBtn.getStyle() + "-fx-font-size: 14px; -fx-padding: 10 0 10 0;");

        Label hint = new Label("Default: admin / hotel@123");
        hint.setStyle("-fx-text-fill: #4A5568; -fx-font-size: 10px; -fx-font-style: italic;");

        card.getChildren().addAll(
                icon, appName, subtitle, sep,
                userLbl, userField,
                passLbl, passField,
                errorLabel, loginBtn, hint
        );

        // ── Login logic ───────────────────────────────────────────────
        Runnable attempt = () -> {
            String user = userField.getText().trim();
            String pass = passField.getText();
            if (validate(user, pass)) {
                onLoginSuccess.run();
            } else {
                showError("Invalid username or password.");
                passField.clear();
                shake();
            }
        };

        loginBtn.setOnAction(e -> attempt.run());
        passField.setOnAction(e -> attempt.run());  // Enter key in password field
        userField.setOnAction(e -> passField.requestFocus());

        // ── Assemble ──────────────────────────────────────────────────
        root.getChildren().addAll(topBar, card);

        Scene scene = new Scene(root, 1080, 720);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        stage.setTitle("HMS — Login");
        stage.setScene(scene);
        stage.setMinWidth(400);
        stage.setMinHeight(500);
        stage.show();

        // Fade card in
        card.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(500), card);
        ft.setFromValue(0); ft.setToValue(1);
        ft.setDelay(Duration.millis(100));
        ft.play();
    }

    // ── Credential check ──────────────────────────────────────────────
    private boolean validate(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) return false;
        String stored = credentials.get(username);
        return stored != null && stored.equals(password);
    }

    // ── Error display ─────────────────────────────────────────────────
    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
    }

    // ── Shake animation on wrong credentials ──────────────────────────
    private void shake() {
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), card);
        shake.setFromX(0); shake.setByX(14);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> card.setTranslateX(0));
        shake.play();
    }
}
