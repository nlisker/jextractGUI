package org.nlisker.jextractGUI.jextract;

import static java.util.stream.Collectors.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;

import lombok.experimental.UtilityClass;

import javafx.scene.control.Alert.AlertType;

import org.nlisker.jextractGUI.model.Displayable;
import org.nlisker.jextractGUI.model.Displayable.MainHeader;
import org.openjdk.jextract.JextractTool.JextractToolProvider;

@UtilityClass
public class Extractor {

	private final JextractToolProvider JEXTRACT = new JextractToolProvider();

	/// Creates the jextract commands for all selected or indeterminate headers as text, ready to be passed to it. Spaced segments
	/// are wrapped in quotes. Each command is separated by new lines.
	public String createCommands(List<TreeItem<Displayable>> mainHeaderItems) {
		return streamRelevantHeaders(mainHeaderItems)
				.map(Extractor::createCommand)
				.reduce((c1, c2) -> c1 + "\n\n" + c2)
				.get();
	}

	/// Runs the commands for all selected or indeterminate headers to generate bindings.
	public void runCommands(List<TreeItem<Displayable>> mainHeaderItems) {
		streamRelevantHeaders(mainHeaderItems).forEach(Extractor::runCommand);
	}

	private Stream<CheckBoxTreeItem<Displayable>> streamRelevantHeaders(List<TreeItem<Displayable>> mainHeaderItems) {
		return mainHeaderItems.stream()
				.<CheckBoxTreeItem<Displayable>>map(CheckBoxTreeItem.class::cast)
				.filter(Extractor::isHeaderTreeItemRelevant);
	}

	private boolean isHeaderTreeItemRelevant(CheckBoxTreeItem<Displayable> mainHeaderItem) {
		return mainHeaderItem.isSelected() || mainHeaderItem.isIndeterminate();
	}

	/// Creates the jextract command as text, ready to be passed to it. Spaced segments are wrapped in quotes.
	public String createCommand(CheckBoxTreeItem<Displayable> mainHeaderItem) {
		return createCommandArgs(mainHeaderItem).stream()
				.map(s -> s.contains(" ") ? "\"" + s + "\"" : s)
				.collect(joining(" "));
	}

	/// Runs the command for the header to generate bindings.
	public void runCommand(CheckBoxTreeItem<Displayable> mainHeaderItem) {
		var header = (MainHeader) mainHeaderItem.getValue();
		List<String> commandArgs = createCommandArgs(mainHeaderItem);

		var errorBuffer = new ByteArrayOutputStream();
		var errorStream = new PrintStream(errorBuffer);

		try {
			JEXTRACT.run(System.out, errorStream, commandArgs.toArray(new String[0]));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		errorStream.flush();
		var errorMessage = errorBuffer.toString();
		System.err.append(errorMessage);
	    try {
	        parseErrorStream(errorMessage);
	    } catch (Exception e) {
	        e.printStackTrace();
	        return;
	    }

		String outputPath = header.outputPath().get();
		String output = outputPath.isBlank() ? "current directory" : outputPath;
		Platform.runLater(() -> new Alert(AlertType.INFORMATION, "Generated bindings at " + output + ".", ButtonType.OK).show());
	}

	private void parseErrorStream(String stream) throws Exception {
		if (stream.contains("error")) {
			Platform.runLater(() -> new Alert(AlertType.ERROR, stream, ButtonType.OK).show());
			throw new Exception(stream);
		}
		if (stream.contains("warning")) {
			Platform.runLater(() -> new Alert(AlertType.WARNING, stream, ButtonType.OK).show());
		}
	}

	private List<String> createCommandArgs(CheckBoxTreeItem<Displayable> mainHeaderItem) {
		List<String> args = ((MainHeader) mainHeaderItem.getValue()).createCommandOptions();
		addIncludesArgs(mainHeaderItem, args);
		return args;
	}

	/// Adds the [#IncludeKind] arguments based on the tree selections.
	private void addIncludesArgs(CheckBoxTreeItem<Displayable> mainHeaderItem, List<String> args) {
		if (mainHeaderItem.isSelected() && !mainHeaderItem.isIndeterminate()) {
			return;
		}

		for (var headerItem : mainHeaderItem.getChildren()) {
			var cbHeaderItem = (CheckBoxTreeItem<Displayable>) headerItem;
			if (cbHeaderItem.isSelected() && !cbHeaderItem.isIndeterminate()) {
				for (var kindGroupItem : headerItem.getChildren()) {
					addAllGroupDeclarations(kindGroupItem, args);
				}
				continue;
			}
			if (cbHeaderItem.isIndeterminate()) {
				addHeaderArgs(cbHeaderItem, args);
			}
		}
	}

	private void addHeaderArgs(CheckBoxTreeItem<Displayable> headerItem, List<String> args) {
		for (var kindGroupItem : headerItem.getChildren()) {
			var cbGroupItem = (CheckBoxTreeItem<Displayable>) kindGroupItem;
			if (cbGroupItem.isSelected() && !cbGroupItem.isIndeterminate()) {
				addAllGroupDeclarations(kindGroupItem, args);
				continue;
			}
			if (cbGroupItem.isIndeterminate()) {
				addKindGroupArgs(kindGroupItem, args);
			}
		}
	}

	private void addKindGroupArgs(TreeItem<Displayable> kindGroupItem, List<String> args) {
		for (var declItem : kindGroupItem.getChildren()) {
			var cbDeclItem = (CheckBoxTreeItem<Displayable>) declItem;
			if (cbDeclItem.isSelected()) {
				args.add(kindGroupItem.getValue().asOption());
				args.add(declItem.getValue().asOption());
			}
		}
	}

	private void addAllGroupDeclarations(TreeItem<Displayable> kindGroupItem, List<String> args) {
		for (var declItem : kindGroupItem.getChildren()) {
			args.add(kindGroupItem.getValue().asOption());
			args.add(declItem.getValue().asOption());
		}
	}
}