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

import javafx.geometry.Insets;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import org.nlisker.jextractGUI.NameViewer.ClassNameViewer;
import org.nlisker.jextractGUI.NameViewer.PackageNameViewer;

class MainView extends BorderPane {

	private static MainView INSTANCE;

	static MainView get() {
		return INSTANCE = INSTANCE == null ? new MainView() : INSTANCE;
	}

	final TextArea console = new TextArea();

	private MainView() {
		configureConsole(console);

		var vBox = new VBox(5,
				createTitle(SymbolsViewer.get()),
				new IncludesViewer(),
				new LibrariesViewer(),
				new MacroViewer(),
				new Separator(),
				new ClassNameViewer(),
				new PackageNameViewer(),
				new OutputPathViewer(),
				new Separator(),
				console);
		vBox.setPadding(new Insets(0, 0, 0, 2));

		setPadding(new Insets(2));
		setCenter(new SplitPane(SymbolsViewer.get(), vBox));
	}

	private static void configureConsole(TextArea console) {
		console.setWrapText(true);
		console.setEditable(false);
		VBox.setVgrow(console, Priority.ALWAYS);
	}

	private Text createTitle(SymbolsViewer symbolsViewer) {
		Text title = ControlUtils.createTitle("");
		title.textProperty().bind(symbolsViewer.focusedHeader().map(h -> "Options for " + h.simple()).orElse("Select a header"));
		Paint fill = title.getFill();
		title.fillProperty().bind(symbolsViewer.notFocused().map(b -> b ? Color.RED : fill));
		return title;
	}
}