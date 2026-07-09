/*
 * Copyright 2026 Nir Lisker
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

import javafx.stage.DirectoryChooser;

import org.nlisker.jextractGUI.model.CLOption;
import org.nlisker.jextractGUI.model.Displayable.Header;

/// Viewer and controls for the *includes* directories.
final class IncludesViewer extends FileListViewer {

	IncludesViewer() {
		String prompt = File.separator + "project" + File.separator + "include";
		super("Includes", Header::includes, CLOption.INCLUDES_PATH, "Enter include path", prompt);
	}

	@Override
	public Optional<File> parseText(String text) {
		return Optional.of(text).map(File::new).filter(File::isDirectory);
	}

	@Override
	public File parseSelectedFile(File file) {
		return file;
	}

	@Override
	public Optional<List<File>> filesSupplier() {
		var chooser = new DirectoryChooser();
		chooser.setTitle("Select include dirs");
		return Optional.ofNullable(chooser.showDialog(JextractGUI.stage)).map(List::of);
	}

	@Override
	public Optional<File> parseDnDPath(Path path) {
		return Optional.of(path.toFile()).filter(File::isDirectory);
	}
}