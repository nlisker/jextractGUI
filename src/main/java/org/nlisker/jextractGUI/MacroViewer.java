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

import java.util.List;
import java.util.Optional;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import org.nlisker.jextractGUI.model.CLOption;
import org.nlisker.jextractGUI.model.Macro;
import org.nlisker.jextractGUI.model.Displayable.Header;

/// Viewer and controls for the *macros*.
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
final class MacroViewer extends BorderPane implements TextInput<Macro> {

	SymbolsViewer symbolsViewer = SymbolsViewer.get();

	TableView<Macro> table = new TableView<>(null);

	MacroViewer() {
		createTableColumns();
		configureTable();
		createControls();
	}

	private void createTableColumns() {
		var nameCol = new TableColumn<Macro, String>("Name");
		nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
		nameCol.setCellValueFactory(cell -> cell.getValue().name());
		table.getColumns().add(nameCol);

		var valueCol = new TableColumn<Macro, String>("Value");
		valueCol.setCellFactory(TextFieldTableCell.forTableColumn());
		valueCol.setCellValueFactory(cell -> cell.getValue().value());
		table.getColumns().add(valueCol);
	}

	private void configureTable() {
		table.itemsProperty().bind(symbolsViewer.focusedHeader().map(Header::macros));
		table.disableProperty().bind(symbolsViewer.notFocused());
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		table.setEditable(true);
		table.setPrefHeight(150);
		table.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.DELETE) removeSelected(); });
	}

	private void createControls() {
		var helpButton = ControlUtils.createHelpButton(CLOption.MACRO);
		Text title = ControlUtils.createTitle("Macros");

		var removeButton = ControlUtils.createRemoveButton();
		removeButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
		removeButton.setOnAction(_ -> removeSelected());

		Node freeTextControls = ControlUtils.createFreeTextControl("Enter macro", "A=42", 100, this::addValidText);
		freeTextControls.disableProperty().bind(symbolsViewer.notFocused());

		var hControls = ControlUtils.createControls(helpButton, title, removeButton, freeTextControls);

		var vControls = new VBox(hControls, table);
		setCenter(vControls);
	}

	@Override
	public ObservableList<Macro> items() {
		return table.getItems();
	}

	@Override
	public List<Macro> getSelection() {
		return table.getSelectionModel().getSelectedItems();
	}

	@Override
	public Optional<Macro> parseText(String text) {
		return Macro.fromString(text);
	}
}