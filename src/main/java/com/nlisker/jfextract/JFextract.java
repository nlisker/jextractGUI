package com.nlisker.jfextract;

import java.io.IOException;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class JFextract extends Application {

	static Stage stage;

	@Override
	public void start(Stage stage) throws IOException {
		JFextract.stage = stage;
		var scene = new Scene(MainView.get());
		stage.setScene(scene);
		stage.setTitle("JFextract");
		stage.setWidth(1200);
		try (var stream = JFextract.class.getResourceAsStream("/icon.png")) {
			stage.getIcons().add(new Image(stream));
		}
		stage.show();
	}
	
	public static void main(String[] args) {
		Application.launch(args);
	}
}