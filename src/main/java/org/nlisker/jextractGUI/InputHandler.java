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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;

/// Parent interface for input handlers responsible to add and remove items in the control.
///
/// @param <T> the type of item handled by the control
interface InputHandler<T> {

	ObservableList<T> items();

	default void add(T item) {
		items().add(item);
	}

	default BooleanBinding isEmpty() {
		return Bindings.isEmpty(items());
	}

	List<? extends T> getSelection();

	default void removeSelected() {
		items().removeAll(getSelection());
	}

	default boolean notContains(T item) {
		return !items().contains(item);
	}
}

/// Input handler for items added as a text from a text field.
///
/// @param <T> the type to which to convert the text
interface TextInput<T> extends InputHandler<T> {

	/// Converts the text field's text to the specified type if it's a valid input. Otherwise, an empty optional is returned.
	Optional<? extends T> parseText(String text);

	/// Parses the text in the text field and adds it if valid.
	default void addValidText(TextField tf) {
		parseText(tf.getText()).filter(this::notContains).ifPresent(item -> addAndClear(item, tf));
	}

	private void addAndClear(T item, TextField textField) {
		add(item);
		textField.clear();
	}
}

/// Input handler for items added from a file/directory chooser.
///
/// @param <T> the type to which to convert the text
interface FilesInput<T> extends InputHandler<T> {

	Optional<List<File>> filesSupplier();

	T parseSelectedFile(File file);

	/// Adds the selected files if valid.
	default void addValidFiles() {
		filesSupplier().stream()
				.flatMap(List::stream)
				.map(this::parseSelectedFile)
				.filter(this::notContains)
				.forEach(this::add);
	}
}

/// Input handler for files added by drag-and-drop.
///
/// @param <T> the type to which to convert the text
interface DnDInput<T> extends InputHandler<T> {

	/// Converts the file's path to the specified type if it's a valid input. Otherwise, an empty optional is returned.
	Optional<? extends T> parseDnDPath(Path path);

	default boolean findValidFiles(Stream<Path> paths) {
		return paths.map(this::parseDnDPath)
				.flatMap(Optional::stream)
				.anyMatch(this::notContains);
	}

	default void addValidFiles(Stream<Path> paths) {
		paths.map(this::parseDnDPath)
				.flatMap(Optional::stream)
				.filter(this::notContains)
				.forEach(this::add);
	}

	default void addDnD(Node target) {
		target.setOnDragOver(event -> {
			if (findValidFiles(paths(event))) {
				event.acceptTransferModes(TransferMode.COPY);
			}
		});
		target.setOnDragDropped(event -> addValidFiles(paths(event)));
	}

	private static Stream<Path> paths(DragEvent event) {
		return event.getDragboard().getFiles().stream()
				.map(File::toPath)
				.flatMap(DnDInput::walk);
	}

	private static Stream<Path> walk(Path path) {
		try {
			return Files.walk(path);
		} catch (IOException e) {
			e.printStackTrace();
			new Alert(AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
			return Stream.empty();
		}
	}
}