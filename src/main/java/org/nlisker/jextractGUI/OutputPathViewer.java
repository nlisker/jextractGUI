/*
 * Copyright 2024 Nir Lisker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nlisker.jextractGUI;

import java.io.File;
import java.util.Optional;

import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;

import org.nlisker.jextractGUI.model.CLOption;
import org.nlisker.jextractGUI.model.Displayable.Header;

/// Viewer and controls for the source files generation.
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

		String prompt = File.separator + "src" + File.separator + "main" + File.separator + "java";
		var textField = ControlUtils.createBoundTextField("Enter output path", prompt, 30, Header::outputPath);

		var selectButton = ControlUtils.createSelectButton("Select output path");
		selectButton.disableProperty().bind(symbolsViewer.notFocused());
		selectButton.setOnAction(_ -> Optional.ofNullable(new DirectoryChooser().showDialog(JextractGUI.stage))
				.map(File::getPath)
				.ifPresent(textField::setText));

		return ControlUtils.createControls(textField, selectButton);
	}
}