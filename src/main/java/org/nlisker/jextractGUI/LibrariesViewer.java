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
		String prompt = STR.":\{File.separator}path\{File.separator}lib.dll OR knownLib";
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
			Optional.of(text).map(File::new).filter(file -> isValidFile(new File(text.substring(1)))) :
			Optional.of(text).map(File::new).filter(file -> file.toPath().getNameCount() == 1);
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
		return Optional.of(path.toFile()).filter(this::isValidFile);
	}

	private boolean isValidFile(File file) {
		return file.isFile() && EXTENTIONS.stream().anyMatch(file.toString()::endsWith);
	}
}