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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
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
import javafx.util.StringConverter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.nlisker.jextractGUI.model.CLOption;
import org.nlisker.jextractGUI.model.Displayable;
import org.nlisker.jextractGUI.model.Displayable.Header;
import org.openjdk.jextract.Declaration.Scoped;
import org.openjdk.jextract.JextractTool;
import org.openjdk.jextract.JextractTool.JextractToolProvider;

/// Viewer and controls for the header files and their content (*symbols*).
@Accessors(fluent = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
final class SymbolsViewer extends BorderPane implements TextInput<TreeItem<Displayable>>, FilesInput<TreeItem<Displayable>>,
			DnDInput<TreeItem<Displayable>> {

	private static SymbolsViewer INSTANCE;

	static SymbolsViewer get() {
		return INSTANCE = INSTANCE == null ? new SymbolsViewer() : INSTANCE;
	}

	private static final List<String> EXTENTIONS = List.of(".h", ".c");

	HeaderParser parser = new HeaderParser();
	Extractor extractor = new Extractor();

	TreeView<Displayable> tree = new TreeView<>(new CheckBoxTreeItem<>());
	ObservableList<Task<Void>> runningTasks = FXCollections.observableArrayList();
	BooleanProperty detailed;
	BooleanBinding noItems = isEmpty();

	@Getter(value = AccessLevel.PACKAGE)
	ObjectProperty<Header> focusedHeader = new SimpleObjectProperty<>();

	@Getter(value = AccessLevel.PACKAGE)
	BooleanBinding noFocus = focusedHeader.isNull();

	private CheckBoxTreeItem<Displayable> root() {
		return (CheckBoxTreeItem<Displayable>) tree.getRoot();
	}

	private SymbolsViewer() {
		ObservableValue<Header> parentHeader = selectedItem().map(item -> {
			while (!(item.getValue() instanceof Header header)) {
				item = item.getParent();
			}
			return header;
		});
		// focus/selection doesn't update on deletion https://bugs.openjdk.org/browse/JDK-8248217
		// so need to explicitly null the selection when there are no items after deletion
		focusedHeader.bind(noItems.flatMap(noItems -> noItems ? null : parentHeader));

		var detailedViewCB = new CheckBox("Detailed view");
		detailed = detailedViewCB.selectedProperty();

		createControl(detailedViewCB);
		configureTree();
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

		var runAllButton = ControlUtils.createButton(MaterialDesignA.ANIMATION_PLAY, Color.GREEN, "Generate files for all headers");
		runAllButton.disableProperty().bind(noItems);
		runAllButton.setOnAction(_ -> extractor.runCommandForAllHeaders());

		var writeAllButton = ControlUtils.createButton(MaterialDesignP.PENCIL_BOX_MULTIPLE, Color.DARKBLUE, "Print command for all headers");
		writeAllButton.disableProperty().bind(noItems);
		writeAllButton.setOnAction(_ -> extractor.writeCommandForAllHeaders());

		var progressIndicator = ControlUtils.createProgressIndicator(Bindings.isNotEmpty(runningTasks));

		HBox batchControls = ControlUtils.createControls(detailedViewCB, expandButton, collapseButton,
				new Separator(Orientation.VERTICAL), runAllButton, writeAllButton,
				new Separator(Orientation.VERTICAL), progressIndicator);
		batchControls.setPadding(new Insets(2, 0, 0, 2));
		return batchControls;
	}

	private HBox createHeaderControls() {
		var helpButton = ControlUtils.createHelpButton(CLOption.INCLUDE);
		Text title = ControlUtils.createTitle("Headers");

		var selectButton = ControlUtils.createSelectButton("Select header path");
		selectButton.setOnAction(_ -> addValidFiles());

		var removeButton = ControlUtils.createRemoveButton();
		ObservableValue<Boolean> isSelectedHeader = selectedItem().map(item -> !(item.getValue() instanceof Header));
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
		var button = ControlUtils.createButton(icon, Color.DARKBLUE, (expand ? "Expand" : "Collapse") + " all");
		button.setOnAction(_ -> root().getChildren().forEach(typeItem -> {
			typeItem.setExpanded(expand);
			typeItem.getChildren().forEach(symbolItem -> symbolItem.setExpanded(expand));
		}));
		return button;
	}

	@Override
	public ObservableList<TreeItem<Displayable>> items() {
		return root().getChildren();
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
				.map(Displayable::detailed)
				.noneMatch(item.getValue().detailed()::equals);
	}

	@Override
	public Optional<CheckBoxTreeItem<Displayable>> parseText(String text) {
		return Optional.of(text)
				.map(File::new)
				.filter(SymbolsViewer::isValidFile)
				.map(this::createHeaderItem);
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
		return createHeaderItem(file);
	}

	@Override
	public Optional<CheckBoxTreeItem<Displayable>> parseDnDPath(Path path) {
		return Optional.of(path.toFile())
				.filter(SymbolsViewer::isValidFile)
				.map(this::createHeaderItem);
	}

	private static boolean isValidFile(File file) {
		return file.isFile() && EXTENTIONS.stream().anyMatch(file.toString()::endsWith);
	}

	private CheckBoxTreeItem<Displayable> createHeaderItem(File headerFile) {
		var header = new Header(headerFile);
		var headerItem = new CheckBoxTreeItem<Displayable>(header, null, true);
		header.requiresIncludeArgs(headerItem.indeterminateProperty());
		return headerItem;
	}


	@Override
	public void add(TreeItem<Displayable> treeHeaderItem) {
		var headerItem = (CheckBoxTreeItem<Displayable>) treeHeaderItem;
		var task = new Task<Void>() {

			@Override
			protected Void call() throws IOException {
				parser.populateHeaderTree(headerItem);
				addOperationsButtonsForHeader(headerItem);
				addHeaderItem(headerItem);
				return null;
			}
		};
		task.runningProperty().subscribe(running -> {
			if (running) {
				runningTasks.add(task);
			} else {
				runningTasks.remove(task);
			}
		});
		task.exceptionProperty().subscribe((_, ex) -> {
			ex.printStackTrace();
			String message = ex.getMessage();
			if (message == null) {
				return;
			}
			new Alert(AlertType.ERROR, message, ButtonType.OK).show();
		});

		var thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();
	}

	private void addOperationsButtonsForHeader(CheckBoxTreeItem<Displayable> headerItem) {
		BooleanBinding notSelected = (headerItem.selectedProperty().or(headerItem.indeterminateProperty())).not();

		var runButton = ControlUtils.createButton(MaterialDesignP.PLAY_BOX, Color.GREEN, "Generate files");
		runButton.disableProperty().bind(notSelected);
		runButton.setOnAction(_ -> extractor.runCommandForHeader(headerOf(headerItem)));

		var writeButton = ControlUtils.createButton(MaterialDesignP.PENCIL_BOX, Color.DARKBLUE, "Print command");
		writeButton.disableProperty().bind(notSelected);
		writeButton.setOnAction(_ -> Extractor.writeCommandForHeader(headerItem));

		var buttons = new HBox(5, runButton, writeButton);
		buttons.setPadding(new Insets(0, 0, 0, 5));
		headerItem.setGraphic(buttons);
		headerItem.setExpanded(true);
	}

	private void addHeaderItem(CheckBoxTreeItem<Displayable> headerItem) {
		Platform.runLater(() -> {
			root().getChildren().add(headerItem);
			tree.getSelectionModel().clearSelection();
			tree.getSelectionModel().select(headerItem);
		});
	}

	/// {@return the `Header` of the header tree item}
	private static Header headerOf(CheckBoxTreeItem<Displayable> headerItem) {
		return (Header) headerItem.getValue();
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

	private class HeaderParser {

		private void populateHeaderTree(CheckBoxTreeItem<Displayable> headerItem) {
			var header = headerOf(headerItem);
			// 1st pass to populate the header model
			popoulateHeader(header);

			// TODO 2nd pass to populate the tree, consider doing it in one pass for both the model and the controls
			header.includeKindGroups().forEach(kindGroup -> {
				var kindGroupItem = new CheckBoxTreeItem<Displayable>(kindGroup, null, true);
				kindGroup.included(kindGroupItem.selectedProperty().or(kindGroupItem.indeterminateProperty()));
				kindGroupItem.setExpanded(true);

				kindGroup.includeDeclarations().forEach(decl -> {
					var declItem = new CheckBoxTreeItem<Displayable>(decl, null, true);
					decl.included(declItem.selectedProperty());
					kindGroupItem.getChildren().add(declItem);
				});

				headerItem.getChildren().add(kindGroupItem);
			});
		}

		private void popoulateHeader(Header header) {
			// Always empty because the user can't put includes before parsing
			String[] options = header.includes().stream()
					.flatMap(include -> List.of(CLOption.INCLUDES_PATH.commands().getLast(), include.toString()).stream())
					.toArray(String[]::new);
			System.out.println("options = " + Arrays.toString(options));

			PrintStream oldErrorStream = System.err;
			try (var byteStream = new ByteArrayOutputStream();
				 var newErrorStream = new PrintStream(byteStream)) {
				System.setErr(newErrorStream);

				Scoped headerScope = JextractTool.parse(List.of(header.file().toString()), options);
				header.populate(headerScope);

				System.err.flush();
				String errorMessage = byteStream.toString();
				if (!errorMessage.isBlank()) {
					Platform.runLater(() -> new Alert(AlertType.WARNING, header.detailed() + "\n" + errorMessage, ButtonType.OK).show());
				}
			} catch (IOException e) {
				e.printStackTrace();
				Platform.runLater(() -> new Alert(AlertType.ERROR, e.getMessage(), ButtonType.OK).show());
			} finally {
				System.setErr(oldErrorStream);
			}
		}
	}

	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	private class Extractor {

		JextractToolProvider jextract = new JextractToolProvider();

		/// Writes the commands for all selected or indeterminate headers to the console. Each command is separated by new lines.
		private void writeCommandForAllHeaders() {
			root().getChildren().stream()
				.map(CheckBoxTreeItem.class::cast)
				.filter(Extractor::isHeaderTreeItemRelevant)
				.map(SymbolsViewer::headerOf)
				.map(Header::createCommandText)
				.reduce((c1, c2) -> c1 + "\n\n" + c2)
				.ifPresent(Extractor::writeCommand);
		}

		/// Runs the commands for all selected or indeterminate headers.
		private void runCommandForAllHeaders() {
			root().getChildren().stream()
				.map(CheckBoxTreeItem.class::cast)
				.filter(Extractor::isHeaderTreeItemRelevant)
				.map(SymbolsViewer::headerOf)
				.forEach(this::runCommandForHeader);
		}

		private static boolean isHeaderTreeItemRelevant(CheckBoxTreeItem<Displayable> headerItem) {
			return headerItem.isSelected() || headerItem.isIndeterminate();
		}

		/// Writes the command for the requested header to the console.
		private static void writeCommandForHeader(CheckBoxTreeItem<Displayable> headerItem) {
			String commandText = headerOf(headerItem).createCommandText();
			writeCommand(commandText);
		}

		private static void writeCommand(String command) {
			MainView.get().console.setText(command);
		}

		/// Runs the command for the requested header.
		void runCommandForHeader(Header header) {
			var oldErrorStream = System.err;
			try (var byteStream = new ByteArrayOutputStream();
				 var newErrorStream = new PrintStream(byteStream)) {
				System.setErr(newErrorStream);

				List<String> commandForHeader = header.createCommandSegments();
				jextract.run(System.out, System.err, commandForHeader.toArray(new String[0]));

				System.err.flush();
				var errorMessage = byteStream.toString();
				if (!errorMessage.isBlank()) {
					new Alert(AlertType.WARNING, header.simple() + "\n" + errorMessage, ButtonType.OK).show();
					return;
				}
			} catch (IOException e) {
				e.printStackTrace();
				new Alert(AlertType.ERROR, e.getMessage(), ButtonType.OK).show();
			} finally {
				System.setErr(oldErrorStream);
			}
			String output = header.outputPath().get();
			output = output.isBlank() ? "current directory" : output;
			new Alert(AlertType.INFORMATION, "Generated bindings at " + output + ".", ButtonType.OK).show();
		}
	}
}