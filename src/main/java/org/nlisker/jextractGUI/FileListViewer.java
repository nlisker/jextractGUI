/*
 * Copyright 2024-2026 Nir Lisker
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
import java.util.List;
import java.util.function.Function;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import org.nlisker.jextractGUI.model.CLOption;
import org.nlisker.jextractGUI.model.Displayable.MainHeader;

/// Parent class of viewers and controls for file lists.
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
abstract class FileListViewer extends BorderPane implements TextInput<File>, FilesInput<File>, DnDInput<File> {

	SymbolsViewer symbolsViewer = SymbolsViewer.get();

	ListView<File> listView = new ListView<>();

	protected FileListViewer(String titleText, Function<MainHeader, ObservableList<File>> property, CLOption option,
			String tooltipText, String promptText) {
		configureList(property);
		createControls(titleText, option, tooltipText, promptText);
	}

	private void configureList(Function<MainHeader, ObservableList<File>> property) {
		listView.itemsProperty().bind(symbolsViewer.focusedHeader().map(property::apply));
		listView.disableProperty().bind(symbolsViewer.notFocused());
		listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		listView.setEditable(true);
		listView.setPrefHeight(150);
		listView.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.DELETE) removeSelected(); });

		var defFont = Font.getDefault();
		var italicFont = Font.font(defFont.getFamily(), FontPosture.ITALIC, defFont.getSize());
		listView.setCellFactory(_ ->
			new ListCell<>() {
				{
					textProperty().bind(itemProperty().map(File::toString));
					fontProperty().bind(textProperty().map(text -> text.contains(File.separator) ? defFont : italicFont).orElse(defFont));
				}
			}
		);
	}

	private void createControls(String titleText, CLOption option, String tooltipText, String promptText) {
		var helpButton = ControlUtils.createHelpButton(option);
		Text title = ControlUtils.createTitle(titleText);

		var selectButton = ControlUtils.createSelectButton("Select path");
		selectButton.disableProperty().bind(symbolsViewer.notFocused());
		selectButton.setOnAction(_ -> addValidFiles());

		var removeButton = ControlUtils.createRemoveButton();
		removeButton.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
		removeButton.setOnAction(_ -> removeSelected());

		Node freeTextControls = ControlUtils.createFreeTextControl(tooltipText, promptText, 100, this::addValidText);
		freeTextControls.disableProperty().bind(symbolsViewer.notFocused());

		var hControls = ControlUtils.createControls(helpButton, title, selectButton, removeButton, freeTextControls);

		addDnD(listView);
		var stackPane = ControlUtils.createAndAttachDropHint(listView, listView.itemsProperty().flatMap(Bindings::isEmpty));

		var vControls = new VBox(hControls, stackPane);
		setCenter(vControls);
	}

	@Override
	public ObservableList<File> items() {
		return listView.getItems();
	}

	@Override
	public List<File> getSelection() {
		return listView.getSelectionModel().getSelectedItems();
	}
}