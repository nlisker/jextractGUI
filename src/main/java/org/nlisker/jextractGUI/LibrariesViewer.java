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
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import org.nlisker.jextractGUI.model.CLOption;
import org.nlisker.jextractGUI.model.Displayable.Header;

/**
 * Viewer and controls for the <i>libraries</i> files.
 */
final class LibrariesViewer extends FileListViewer {

	private static final List<String> EXTENTIONS = List.of(".dll", ".so", ".dylib");

	LibrariesViewer() {
		String prompt = ":" + File.separator + "path" + File.separator + "lib.dll OR knownLib";
		super("Libraries", Header::libraries, CLOption.LIBRARY_PATH, "Enter library name or path", prompt);

		setTop(createUseSystemLoadControls());
	}

	private HBox createUseSystemLoadControls() {
		var symbolsViewer = SymbolsViewer.get();

		var helpButton = ControlUtils.createHelpButton(CLOption.USE_SYSTEM_LOAD_LIBRARIES);

		var useSystemCheckBox = new CheckBox("Use System to load libraries");
		useSystemCheckBox.disableProperty().bind(symbolsViewer.noFocus());
		symbolsViewer.noFocus().subscribe(noFocus -> { if (noFocus) useSystemCheckBox.setSelected(false); });
		ControlUtils.bindFocusedHeader(useSystemCheckBox.selectedProperty(), Header::useSystemLoadLibraries);

		return ControlUtils.createControls(helpButton, useSystemCheckBox);
	}

	/**
	 * Library entries are valid if either the entry starts with ':' followed by a path, of it's a name.
	 */
	@Override
	public Optional<File> parseText(String text) {
		return text.startsWith(":") ?
			Optional.of(new File(text)).filter(_ -> isValidFile(new File(text.substring(1)))) :
			Optional.of(new File(text)).filter(file -> file.toPath().getNameCount() == 1).map(file -> new File(file.getName()));
	}

	@Override
	public File parseSelectedFile(File file) {
		return new File(":" + file.toString());
	}

	@Override
	public Optional<List<File>> filesSupplier() {
		var fileChooser = new FileChooser();
		fileChooser.setTitle("Select Libraries");
		var filter = EXTENTIONS.stream().map(ext -> "*" + ext).toList();
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Libraries", filter));
		return Optional.ofNullable(fileChooser.showOpenMultipleDialog(JextractGUI.stage));
	}

	@Override
	public Optional<File> parseDnDPath(Path path) {
		return Optional.of(path.toFile()).filter(this::isValidFile).map(this::parseSelectedFile);
	}

	private boolean isValidFile(File file) {
		return file.isFile() && EXTENTIONS.stream().anyMatch(file.toString()::endsWith);
	}
}