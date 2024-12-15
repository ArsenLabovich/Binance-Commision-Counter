package org.example.earsexample.binancecommissioncounter;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.example.earsexample.binancecommissioncounter.configuration.SpringFXMLLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Objects;

public class JavaFXApp extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = SpringApplication.run(BinanceCommissionCounterApplication.class);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        SpringFXMLLoader loader = context.getBean(SpringFXMLLoader.class);
        Parent root = loader.load("/scenes/MainView.fxml").getRoot();
        Scene scene = new Scene(root);

        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm());

        primaryStage.setTitle("API Key Input");
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/pictures/binance.png"))));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        context.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}