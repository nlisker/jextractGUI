package org.nlisker.jextractGUI;

import javafx.geometry.Insets;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.PackagePrivate;

import org.nlisker.jextractGUI.NameViewer.ClassNameViewer;
import org.nlisker.jextractGUI.NameViewer.PackageNameViewer;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class MainView extends BorderPane {

	private static MainView INSTANCE;

	static MainView get() {
		return INSTANCE = INSTANCE == null ? new MainView() : INSTANCE;
	}

	@PackagePrivate
	TextArea console = new TextArea();

	private MainView() {
		var vBox = new VBox(5);

		var title = ControlUtils.createTitle("");
		title.textProperty().bind(SymbolsViewer.get().focusedHeader().map(h -> "Options for " + h.simple()).orElse("Select a header"));
		var fill = title.getFill();
		title.fillProperty().bind(SymbolsViewer.get().noFocus().map(b -> b ? Color.RED : fill));
		vBox.getChildren().add(title);
		vBox.getChildren().add(new IncludesViewer());
		vBox.getChildren().add(new LibrariesViewer());
		vBox.getChildren().add(new MacroViewer());
		vBox.getChildren().add(new Separator());
		vBox.getChildren().add(new ClassNameViewer());
		vBox.getChildren().add(new PackageNameViewer());
		vBox.getChildren().add(new OutputPathViewer());
		vBox.getChildren().add(new Separator());

		console.setWrapText(true);
		console.setEditable(false);
		VBox.setVgrow(console, Priority.ALWAYS);

		vBox.getChildren().add(console);
		vBox.setPadding(new Insets(0, 0, 0, 2));

		setPadding(new Insets(2));
		setCenter(new SplitPane(SymbolsViewer.get(), vBox));
	}
}