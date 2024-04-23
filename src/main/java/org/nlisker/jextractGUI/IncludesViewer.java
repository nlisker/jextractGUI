package org.nlisker.jextractGUI;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.stage.DirectoryChooser;

import org.nlisker.jextractGUI.model.CLOption;
import org.nlisker.jextractGUI.model.Displayable.Header;

/**
 * Viewer and controls for the <i>includes</i> directories.
 */
final class IncludesViewer extends FileListViewer {

	IncludesViewer() {
		String prompt = STR."\{File.separator}project\{File.separator}include";
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