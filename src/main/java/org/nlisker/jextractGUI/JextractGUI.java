package org.nlisker.jextractGUI;

import java.io.IOException;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class JextractGUI extends Application {

	static Stage stage;

	@Override
	public void start(Stage stage) throws IOException {
		JextractGUI.stage = stage;
		var scene = new Scene(MainView.get());
		stage.setScene(scene);
		stage.setTitle("jextractGUI");
		stage.setWidth(1200);
		try (var stream = JextractGUI.class.getResourceAsStream("/icon.png")) {
			stage.getIcons().add(new Image(stream));
		}
		stage.show();
	}
	
	public static void main(String[] args) {
		Application.launch(args);
	}
}