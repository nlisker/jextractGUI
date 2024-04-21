package com.nlisker.jfextract;

import java.io.File;
import java.util.List;
import java.util.function.Function;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.nlisker.jfextract.model.CLOption;
import com.nlisker.jfextract.model.Displayable.Header;

/**
 * Parent class of viewers and controls for file lists. 
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
abstract class FileListViewer extends BorderPane implements TextInput<File>, FilesInput<File>, DnDInput<File> {

	SymbolsViewer symbolsViewer = SymbolsViewer.get();

	ListView<File> listView = new ListView<>();

	protected FileListViewer(String titleText, Function<Header, ObservableList<File>> property, CLOption option,
			String tooltipText, String promptText) {
		configureList(property);
		createControls(titleText, option, tooltipText, promptText);
	}

	private void configureList(Function<Header, ObservableList<File>> property) {
		listView.itemsProperty().bind(symbolsViewer.focusedHeader().map(property::apply));
		listView.disableProperty().bind(symbolsViewer.noFocus());
		listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		listView.setEditable(true);
		listView.setPrefHeight(150);
		listView.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.DELETE) removeSelected(); });
//		listView.setCellFactory(listView ->
//			new ListCell<File>() {
//				{
//					textProperty().bind(itemProperty().map(File::toString));
//					fontProperty().bind(textProperty().map(text -> {
//						var defFont = Font.getDefault();
//						return text.startsWith(":") ? defFont :  Font.font(defFont.getFamily(), FontPosture.ITALIC, defFont.getSize());
//					}));
//				}
//			}
//		);
	}

	private void createControls(String titleText, CLOption option, String tooltipText, String promptText) {
		var helpButton = ControlUtils.createHelpButton(option);
		Text title = ControlUtils.createTitle(titleText);

		var selectButton = ControlUtils.createSelectButton("Select path");
		selectButton.disableProperty().bind(symbolsViewer.noFocus());
		selectButton.setOnAction(e -> addValidFiles());

		var removeButton = ControlUtils.createRemoveButton();
		removeButton.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
		removeButton.setOnAction(e -> removeSelected());

		Node freeTextControls = ControlUtils.createFreeTextControl(tooltipText, promptText, 100, this::addValidText);
		freeTextControls.disableProperty().bind(symbolsViewer.noFocus());

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