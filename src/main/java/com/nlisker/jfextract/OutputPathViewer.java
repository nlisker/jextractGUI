package com.nlisker.jfextract;

import java.io.File;
import java.util.Optional;

import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;

import com.nlisker.jfextract.model.CLOption;
import com.nlisker.jfextract.model.Displayable.Header;

/**
 * Viewer and controls for the source files generation.
 */
final class OutputPathViewer extends BorderPane {

	OutputPathViewer() {
		var helpButton = ControlUtils.createHelpButton(CLOption.OUTPUT_PATH);
		var label = new Label("Output path");
		Pane sourceControls = createFolderTextControl();
		HBox controls = ControlUtils.createControls(helpButton, label, sourceControls);
		setCenter(controls);
	}

	private Pane createFolderTextControl() {
		var symbolsViewer = SymbolsViewer.get();

		String prompt = STR."\{File.separator}src\{File.separator}main\{File.separator}java";
		var textField = ControlUtils.createBoundTextField("Enter output path", prompt, 30, Header::outputPath);

		var selectButton = ControlUtils.createSelectButton("Select output path");
		selectButton.disableProperty().bind(symbolsViewer.noFocus());
		selectButton.setOnAction(e -> Optional.ofNullable(new DirectoryChooser().showDialog(JFextract.stage))
			.map(File::getPath)
			.ifPresent(textField::setText));

		return ControlUtils.createControls(textField, selectButton);
	}
}