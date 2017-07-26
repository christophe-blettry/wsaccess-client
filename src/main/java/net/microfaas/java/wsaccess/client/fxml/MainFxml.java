/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.microfaas.java.wsaccess.client.fxml;

import java.io.IOException;
import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 *
 * @author christophe
 */
public class MainFxml extends Application {

	@Override
	public void start(Stage primaryStage) throws IOException {
		final URL fxmlURL = getClass().getResource("/fxml/WebsocketClientFXML.fxml");
		final FXMLLoader fxmlLoader = new FXMLLoader(fxmlURL);
		final Node node = fxmlLoader.load();
		final StackPane root = new StackPane(node);
		final Scene scene = new Scene(root);
		primaryStage.setTitle("FXML");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}

}
