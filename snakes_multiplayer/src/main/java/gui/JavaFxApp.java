package gui;

import gametree.GameTreeNode;
import gametree.communication.GameMessageDecoder;
import gametree.communication.GameMessageEncoder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import proto.ProtoGameMessageDecoder;
import proto.ProtoGameMessageEncoder;

import java.io.IOException;
import java.net.URL;

public class JavaFxApp extends Application {
    private static GameTreeNode gameTreeNode;

    public static void main(String[] args) {
        try {
            GameMessageEncoder gameMessageEncoder = new ProtoGameMessageEncoder();
            GameMessageDecoder gameMessageDecoder = new ProtoGameMessageDecoder();
            gameTreeNode = new GameTreeNode(gameMessageEncoder, gameMessageDecoder);
            Application.launch(args);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            if (gameTreeNode != null) {
                try {
                    gameTreeNode.shutdown();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        URL guiLocation = getClass().getClassLoader().getResource("gui.fxml");
        loader.setLocation(guiLocation);

        Parent root = loader.load();
        Scene scene = new Scene(root);

        MainViewController controller = loader.getController();
        controller.init(primaryStage, gameTreeNode);

        primaryStage.setTitle("Snake v1.0");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}