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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.StringConverter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.nlisker.jextractGUI.jextract.Extractor;
import org.nlisker.jextractGUI.jextract.Parser;
import org.nlisker.jextractGUI.model.CLOption;
import org.nlisker.jextractGUI.model.Displayable;
import org.nlisker.jextractGUI.model.Displayable.MainHeader;

/// Viewer and controls for the header files and their content (*symbols*).
@Accessors(fluent = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
final class SymbolsViewer extends BorderPane implements TextInput<TreeItem<Displayable>>, FilesInput<TreeItem<Displayable>>,
			DnDInput<TreeItem<Displayable>> {

	private static SymbolsViewer INSTANCE;

	static SymbolsViewer get() {
		return INSTANCE = INSTANCE == null ? new SymbolsViewer() : INSTANCE;
	}

	private static final Color EXEC_COLOR =  Color.GREEN;
	private static final Color CONF_COLOR =  Color.DARKBLUE;

	private static final List<String> EXTENTIONS = List.of(".h", ".c");

	TreeView<Displayable> tree = new TreeView<>(new TreeItem<>());
	BooleanProperty detailed;
	BooleanBinding noItems = isEmpty();

	BlockingQueue<CheckBoxTreeItem<Displayable>> parseRequests = new LinkedBlockingQueue<>();

	@Getter(value = AccessLevel.PACKAGE)
	ObjectProperty<MainHeader> focusedHeader = new SimpleObjectProperty<>();

	@Getter(value = AccessLevel.PACKAGE)
	BooleanBinding notFocused = focusedHeader.isNull();

	private ObservableList<TreeItem<Displayable>> mainHeaders() {
		return tree.getRoot().getChildren();
	}

	private SymbolsViewer() {
		ObservableValue<MainHeader> parentHeader = selectedItem().map(item -> {
			while (!(item.getValue() instanceof MainHeader mainHeader)) {
				item = item.getParent();
			}
			return mainHeader;
		});
		// focus/selection doesn't update on deletion https://bugs.openjdk.org/browse/JDK-8248217
		// so need to explicitly null the selection when there are no items after deletion
		focusedHeader.bind(noItems.flatMap(noItems -> noItems ? null : parentHeader));

		var detailedViewCB = new CheckBox("Detailed view");
		detailed = detailedViewCB.selectedProperty();

		createControl(detailedViewCB);
		configureTree();

		listenToParseReuqests();
	}

	private void createControl(CheckBox detailedViewCB) {
		HBox batchControls = createBatchControls(detailedViewCB);
		HBox headerControls = createHeaderControls();

		addDnD(tree);
		Node dropHint = ControlUtils.createAndAttachDropHint(tree, noItems);

		var vControls = new VBox(2, batchControls, headerControls, dropHint);
		setCenter(vControls);
	}

	private HBox createBatchControls(CheckBox detailedViewCB) {
		var expandButton = createCollapseExpandButton(true);
		var collapseButton = createCollapseExpandButton(false);

		var runAllButton = ControlUtils.createButton(MaterialDesignA.ANIMATION_PLAY, EXEC_COLOR, "Generate bindings for all headers");
		runAllButton.disableProperty().bind(noItems);
		runAllButton.setOnAction(_ -> Extractor.runCommands(mainHeaders()));

		var writeAllButton = ControlUtils.createButton(MaterialDesignP.PENCIL_BOX_MULTIPLE, CONF_COLOR, "Print command for all headers");
		writeAllButton.disableProperty().bind(noItems);
		writeAllButton.setOnAction(_ -> MainView.get().console.setText(Extractor.createCommands(mainHeaders())));

		HBox batchControls = ControlUtils.createControls(detailedViewCB, expandButton, collapseButton,
				new Separator(Orientation.VERTICAL), runAllButton, writeAllButton);
		batchControls.setPadding(new Insets(2, 0, 0, 2));
		return batchControls;
	}

	private HBox createHeaderControls() {
		var helpButton = ControlUtils.createHelpButton(CLOption.INCLUDE);
		Text title = ControlUtils.createTitle("Headers");

		var selectButton = ControlUtils.createSelectButton("Select header path");
		selectButton.setOnAction(_ -> addValidFiles());

		var removeButton = ControlUtils.createRemoveButton();
		ObservableValue<Boolean> isSelectedHeader = selectedItem().map(item -> !(item.getValue() instanceof MainHeader));
		removeButton.disableProperty().bind(selectedItem().isNull().or(BooleanExpression.booleanExpression(isSelectedHeader)));
		removeButton.setOnAction(_ -> {
			var alert = new Alert(AlertType.CONFIRMATION);
			alert.setContentText("Are you sure you want to remove the selected headers?");
			alert.showAndWait().filter(ButtonType.OK::equals).ifPresent(_ -> removeSelected());
		});

		String prompt = File.separator + "path" + File.separator + "header.h";
		Node freeTextControls = ControlUtils.createFreeTextControl("Enter header path", prompt, 100, this::addValidText);

		HBox headerControls = ControlUtils.createControls(helpButton, title, selectButton, removeButton, freeTextControls);
		headerControls.setPadding(new Insets(0, 0, 0, 2));
		return headerControls;
	}

	private void configureTree() {
		var simpleStringConverter = new DeclarationSimpleStringConverter();
		var detailedStringConverter = new DeclarationDetailedStringConverter();
		ObservableValue<StringConverter<TreeItem<Displayable>>> stringConverterBinding = detailed
				.map(detailed -> detailed ? detailedStringConverter : simpleStringConverter);
		stringConverterBinding.addListener(_ -> tree.refresh());

		Callback<TreeView<Displayable>, TreeCell<Displayable>> cellFactory = _ -> {
			var checkBoxTreeCell = new CheckBoxTreeCell<Displayable>();
			checkBoxTreeCell.converterProperty().bind(stringConverterBinding);
			return checkBoxTreeCell;
		};
		tree.setCellFactory(cellFactory);

		tree.setShowRoot(false);
		tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		tree.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.DELETE) removeSelected(); });
	}

	private Button createCollapseExpandButton(boolean expand) {
		Ikon icon = expand ? MaterialDesignE.EXPAND_ALL : MaterialDesignC.COLLAPSE_ALL;
		var button = ControlUtils.createButton(icon, CONF_COLOR, (expand ? "Expand" : "Collapse") + " all");
		button.setOnAction(_ -> mainHeaders().forEach(typeItem -> {
			typeItem.setExpanded(expand);
			typeItem.getChildren().forEach(symbolItem -> symbolItem.setExpanded(expand));
		}));
		return button;
	}

	@Override
	public ObservableList<TreeItem<Displayable>> items() {
		return mainHeaders();
	}

	@Override
	public List<TreeItem<Displayable>> getSelection() {
		return tree.getSelectionModel().getSelectedItems();
	}

	private ReadOnlyObjectProperty<TreeItem<Displayable>> selectedItem() {
		return tree.getSelectionModel().selectedItemProperty();
	}

	@Override
	public boolean notContains(TreeItem<Displayable> item) {
		return items().stream()
				.map(TreeItem::getValue)
				.noneMatch(item.getValue()::equals);
	}

	@Override
	public Optional<CheckBoxTreeItem<Displayable>> parseText(String text) {
		return Optional.of(text)
				.map(Path::of)
				.filter(SymbolsViewer::isValidFile)
				.map(this::createMainHeaderItem);
	}

	@Override
	public Optional<List<File>> filesSupplier() {
		var fileChooser = new FileChooser();
		fileChooser.setTitle("Select Headers");
		List<String> filter = EXTENTIONS.stream()
				.map(ext -> "*" + ext)
				.toList();
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Clang C", filter));
		return Optional.ofNullable(fileChooser.showOpenMultipleDialog(JextractGUI.stage));
	}

	@Override
	public CheckBoxTreeItem<Displayable> parseSelectedFile(File file) {
		return createMainHeaderItem(file.toPath());
	}

	@Override
	public Optional<CheckBoxTreeItem<Displayable>> parseDnDPath(Path path) {
		return Optional.of(path)
				.filter(SymbolsViewer::isValidFile)
				.map(this::createMainHeaderItem);
	}

	private static boolean isValidFile(Path path) {
		return Files.isRegularFile(path) && EXTENTIONS.stream().anyMatch(path.toString()::endsWith);
	}

	private CheckBoxTreeItem<Displayable> createMainHeaderItem(Path headerPath) {
		return new CheckBoxTreeItem<>(new MainHeader(headerPath));
	}


	@Override
	public void add(TreeItem<Displayable> mainHeaderItem) {
		var parseButton = ControlUtils.createButton(MaterialDesignM.MAGNIFY, CONF_COLOR, "Parse header");
		parseButton.setOnAction(_ -> {
			animateGraphic(parseButton);
			parseRequests.add((CheckBoxTreeItem<Displayable>) mainHeaderItem);
		});

		var container = new HBox(parseButton);
		container.setPadding(new Insets(0, 0, 0, 5));
		mainHeaderItem.setGraphic(container);

		addHeaderItem(mainHeaderItem);
	}

	private static void animateGraphic(Button parseButton) {
		var rotateTransition = new RotateTransition(Duration.seconds(1), parseButton.getGraphic());
		rotateTransition.setToAngle(360);
		rotateTransition.setCycleCount(Animation.INDEFINITE);
		rotateTransition.setInterpolator(Interpolator.LINEAR);
		rotateTransition.play();
	}

	private void addHeaderItem(TreeItem<Displayable> mainHeaderItem) {
		mainHeaders().add(mainHeaderItem);
		// select the new header to show its controls to avoid clearing text fields of the currently selected header
		tree.getSelectionModel().select(mainHeaderItem);
		tree.getSelectionModel().clearSelection();
		tree.getSelectionModel().select(mainHeaderItem);
	}

	private void listenToParseReuqests() {
		var task = new Task<Void>() {

			@Override
			protected Void call() throws InterruptedException  {
				while (true) {
					CheckBoxTreeItem<Displayable> mainHeaderItem = parseRequests.take();
					try {
						Parser.populateHeaderItem(mainHeaderItem);
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}
					for (var headerItem : mainHeaderItem.getChildren()) {
						var cbHeaderItem = (CheckBoxTreeItem<Displayable>) headerItem;
						if (headerItem.getValue().detailed().equals(mainHeaderItem.getValue().detailed())) {
							headerItem.setExpanded(true);
							cbHeaderItem.setSelected(true);
						}
					}
					mainHeaderItem.setExpanded(true);
					addOperationsButtonsForHeader(mainHeaderItem);
				}
			}
		};

		var thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();
	}

	private static void addOperationsButtonsForHeader(CheckBoxTreeItem<Displayable> mainHeaderItem) {
		BooleanBinding notSelected = (mainHeaderItem.selectedProperty().or(mainHeaderItem.indeterminateProperty())).not();

		var runButton = ControlUtils.createButton(MaterialDesignP.PLAY_BOX, EXEC_COLOR, "Generate bindings");
		runButton.disableProperty().bind(notSelected);
		runButton.setOnAction(_ -> Extractor.runCommand(mainHeaderItem));

		var writeButton = ControlUtils.createButton(MaterialDesignP.PENCIL_BOX, CONF_COLOR, "Print command");
		writeButton.disableProperty().bind(notSelected);
		writeButton.setOnAction(_ -> MainView.get().console.setText(Extractor.createCommand(mainHeaderItem)));

		var buttons = new HBox(5, runButton, writeButton);
		buttons.setPadding(new Insets(0, 0, 0, 5));
		mainHeaderItem.setGraphic(buttons);
	}

	private static class DeclarationDetailedStringConverter extends StringConverter<TreeItem<Displayable>> {

		@Override
		public String toString(TreeItem<Displayable> item) {
			return item.getValue().detailed();
		}

		@Override
		public TreeItem<Displayable> fromString(String string) {
			return null; // tree is not editable
		}
	}

	private static class DeclarationSimpleStringConverter extends StringConverter<TreeItem<Displayable>> {

		@Override
		public String toString(TreeItem<Displayable> item) {
			return item.getValue().simple();
		}

		@Override
		public TreeItem<Displayable> fromString(String string) {
			return null; // tree is not editable
		}
	}
}