package org.nlisker.jextractGUI.jextract;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBoxTreeItem;

import lombok.experimental.UtilityClass;

import javafx.scene.control.Alert.AlertType;

import org.nlisker.jextractGUI.model.Displayable;
import org.nlisker.jextractGUI.model.Displayable.Header;
import org.nlisker.jextractGUI.model.Displayable.IncludeKind;
import org.nlisker.jextractGUI.model.Displayable.IncludeKindDeclaration;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.JextractTool;
import org.openjdk.jextract.Declaration.Scoped;

@UtilityClass
public class Parser {

	/// Parses the header and populates its symbols and the symbols of all included headers.
	public Collection<CheckBoxTreeItem<Displayable>> createHeaderItems(CheckBoxTreeItem<Displayable> mainHeaderItem) throws Exception {
		var mainHeader = (Header) mainHeaderItem.getValue();

		List<Declaration> declarations = parse(mainHeader);

		Map<Path, CheckBoxTreeItem<Displayable>> headers = new HashMap<>();
		headers.put(mainHeader.path(), mainHeaderItem);

		Map<Path, Map<IncludeKind, CheckBoxTreeItem<Displayable>>> headerIncludes = new HashMap<>();

		declarations.stream().forEach(decl -> {
			CheckBoxTreeItem<Displayable> headerItem = headers.computeIfAbsent(decl.pos().path(),
					path -> new CheckBoxTreeItem<>(new Header(path)));

			Map<IncludeKind, CheckBoxTreeItem<Displayable>> includes = headerIncludes.computeIfAbsent(decl.pos().path(),
					_ -> new HashMap<>());

			var includeKind = IncludeKind.fromDeclaration(decl);
			CheckBoxTreeItem<Displayable> groupItem = includes.computeIfAbsent(includeKind, _ -> {
				var kindGroupItem = new CheckBoxTreeItem<Displayable>(includeKind);
				kindGroupItem.setExpanded(true);
				headerItem.getChildren().add(kindGroupItem);
				return kindGroupItem;
			});

			var includeKindDeclaration = new IncludeKindDeclaration(decl);
			var declItem = new CheckBoxTreeItem<Displayable>(includeKindDeclaration);
			groupItem.getChildren().add(declItem);
		});

		return headers.values();
	}

	/// Parses the header and populates its symbols.
	private List<Declaration> parse(Header header) throws Exception {
		PrintStream oldErrorStream = System.err;
		try (var byteStream = new ByteArrayOutputStream();
				var newErrorStream = new PrintStream(byteStream)) {
			System.setErr(newErrorStream);

			Scoped headerScope = JextractTool.parse(List.of(header.path().toString()));

			System.err.flush();
			String errorMessage = byteStream.toString();
			if (!errorMessage.isBlank()) {
				throw new Exception(errorMessage);
			}

			return headerScope.members();
		} catch (Exception e) {
			e.printStackTrace();
			Platform.runLater(() -> new Alert(AlertType.ERROR, header.simple() + "\n" + e.getMessage(), ButtonType.OK).show());
			throw e;
		} finally {
			System.setErr(oldErrorStream);
		}
	}
}