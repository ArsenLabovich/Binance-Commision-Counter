package org.example.earsexample.binancecommissioncounter.javafxcontroller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.earsexample.binancecommissioncounter.service.BinanceApiCaller;
import org.example.earsexample.binancecommissioncounter.service.UserCredentialsService;
import org.springframework.stereotype.Controller;

import java.util.Objects;

@Controller
public class MainViewController {

    private final UserCredentialsService userCredentialsService;
    private final BinanceApiCaller binanceApiCaller;


    public MainViewController(UserCredentialsService userCredentialsService, BinanceApiCaller binanceApiCaller) {
        this.userCredentialsService = userCredentialsService;
        this.binanceApiCaller = binanceApiCaller;
    }

    @FXML
    public HBox progressBarBox;

    @FXML
    public HBox checkButtonBox;

    @FXML
    private TextField apiKeyField;

    @FXML
    private TextField secretKeyField;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label progressLabel;

    @FXML
    private Button seeCommissionsButton;

    @FXML
    private void handleCheckConnectivity() {
        String apiKey = apiKeyField.getText();
        String secretKey = secretKeyField.getText();
        userCredentialsService.setApiKey(apiKey);
        userCredentialsService.setSecretKey(secretKey);

        new Thread(() -> {
            boolean isConnected = binanceApiCaller.checkConnection();
            Platform.runLater(() -> {
                if (isConnected) {
                    seeCommissionsButton.setVisible(true);
                    secretKeyField.setVisible(true);
                    apiKeyField.setVisible(true);
                    checkButtonBox.setVisible(false);
                    showAlert(Alert.AlertType.INFORMATION, "Connection Successful", "Successfully connected to Binance API.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Connection Error", "Failed to connect to Binance API. Please check your API key and secret key.");
                }
            });
        }).start();
    }

    @FXML
    private void handleSubmit() {
        if (!validateInput()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter valid API Key and Secret Key.");
            return;
        }
        userCredentialsService.setApiKey(apiKeyField.getText());
        userCredentialsService.setSecretKey(secretKeyField.getText());

        // Show connecting indicator
        Platform.runLater(() -> {
            progressLabel.setText("Connecting...");
            progressBarBox.setVisible(true);
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        });

        new Thread(() -> {
            try {
                binanceApiCaller.runWithProgress(progress -> Platform.runLater(() -> {
                    progressBar.setProgress(progress);
                    progressLabel.setText(String.format("%.2f%%", progress * 100));
                    int remainingTime = (int) ((1 - progress) * 100);
                    progressLabel.setText(String.format("%.2f%% ~ %d s.", progress * 100, remainingTime));
                }), commissionsInUSDT -> Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "Commissions in USDT", "Commissions in USDT: " + commissionsInUSDT)));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressBarBox.setVisible(false);
                    showAlert(Alert.AlertType.ERROR, "Error", "An error occurred: " + e.getMessage());
                });
            }
        }).start();
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/pictures/binance.png"))));

        // Apply the stylesheet to the alert dialog
        alert.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm()
        );

        alert.showAndWait();
    }
    private boolean validateInput() {
        String apiKey = apiKeyField.getText();
        String secretKey = secretKeyField.getText();
        return apiKey != null && !apiKey.trim().isEmpty() &&
                secretKey != null && secretKey.matches("[a-zA-Z0-9]{64}");
    }
}