package org.nlisker.jextractGUI.jextract;

import static java.util.stream.Collectors.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Stream;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;

import lombok.experimental.UtilityClass;

import javafx.scene.control.Alert.AlertType;

import org.nlisker.jextractGUI.model.Displayable;
import org.nlisker.jextractGUI.model.Displayable.Header;
import org.openjdk.jextract.JextractTool.JextractToolProvider;

@UtilityClass
public class Extractor {

	private final JextractToolProvider JEXTRACT = new JextractToolProvider();

	/// Creates the jextract commands for all selected or indeterminate headers as text, ready to be passed to it. Spaced segments
	/// are wrapped in quotes. Each command is separated by new lines.
	public String createCommands(List<TreeItem<Displayable>> headerItems) {
		return streamRelevantHeaders(headerItems)
				.map(Extractor::createCommand)
				.reduce((c1, c2) -> c1 + "\n\n" + c2)
				.get();
	}

	/// Runs the commands for all selected or indeterminate headers to generate bindings.
	public void runCommands(List<TreeItem<Displayable>> headerItems) {
		streamRelevantHeaders(headerItems).forEach(Extractor::runCommand);
	}

	private Stream<CheckBoxTreeItem<Displayable>> streamRelevantHeaders(List<TreeItem<Displayable>> headerItems) {
		return headerItems.stream()
				.<CheckBoxTreeItem<Displayable>>map(CheckBoxTreeItem.class::cast)
				.filter(Extractor::isHeaderTreeItemRelevant);
	}

	private boolean isHeaderTreeItemRelevant(CheckBoxTreeItem<Displayable> headerItem) {
		return headerItem.isSelected() || headerItem.isIndeterminate();
	}

	/// Creates the jextract command as text, ready to be passed to it. Spaced segments are wrapped in quotes.
	public String createCommand(CheckBoxTreeItem<Displayable> headerItem) {
		return createCommandArgs(headerItem).stream()
				.map(s -> s.contains(" ") ? "\"" + s + "\"" : s)
				.collect(joining(" "));
	}

	/// Runs the command for the header to generate bindings.
	public void runCommand(CheckBoxTreeItem<Displayable> headerItem) {
		var header = (Header) headerItem.getValue();
		List<String> commandArgs = createCommandArgs(headerItem);

		PrintStream oldErrorStream = System.err;
		try (var byteStream = new ByteArrayOutputStream();
				var newErrorStream = new PrintStream(byteStream)) {
			System.setErr(newErrorStream);

			JEXTRACT.run(System.out, System.err, commandArgs.toArray(new String[0]));

			System.err.flush();
			var errorMessage = byteStream.toString();
			if (!errorMessage.isBlank()) {
				throw new Exception(errorMessage);
			}
		} catch (Exception e) {
			e.printStackTrace();
			new Alert(AlertType.ERROR, header.simple() + "\n" + e.getMessage(), ButtonType.OK).show();
		} finally {
			System.setErr(oldErrorStream);
		}
		String output = header.outputPath().get();
		output = output.isBlank() ? "current directory" : output;
		new Alert(AlertType.INFORMATION, "Generated bindings at " + output + ".", ButtonType.OK).show();
	}

	private List<String> createCommandArgs(CheckBoxTreeItem<Displayable> headerItem) {
		List<String> args = ((Header) headerItem.getValue()).createCommandOptions();
		addIncludesArgs(headerItem, args);
		return args;
	}

	/// Adds the [#IncludeKind] arguments based on the tree selections.
	private void addIncludesArgs(CheckBoxTreeItem<Displayable> headerItem, List<String> args) {
		if (headerItem.isSelected() && !headerItem.isIndeterminate()) {
			return ;
		}

		for (var kindGroupItem : headerItem.getChildren()) {
			var cbGroupItem = (CheckBoxTreeItem<Displayable>) kindGroupItem;
			if (cbGroupItem.isSelected() && !cbGroupItem.isIndeterminate()) {
				addAllGroupDeclarations(kindGroupItem, args);
				continue;
			}
			if (cbGroupItem.isIndeterminate()) {
				for (var declItem : kindGroupItem.getChildren()) {
					var cbDeclItem = (CheckBoxTreeItem<Displayable>) declItem;
					if (cbDeclItem.isSelected()) {
						args.add(kindGroupItem.getValue().asOption());
						args.add(declItem.getValue().asOption());
					}
				}
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