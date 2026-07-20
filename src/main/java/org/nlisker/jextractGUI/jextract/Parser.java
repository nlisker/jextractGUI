package org.nlisker.jextractGUI.jextract;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBoxTreeItem;

import lombok.experimental.UtilityClass;

import org.nlisker.jextractGUI.model.CLOption;
import org.nlisker.jextractGUI.model.Displayable;
import org.nlisker.jextractGUI.model.Displayable.Header;
import org.nlisker.jextractGUI.model.Displayable.IncludeDeclaration;
import org.nlisker.jextractGUI.model.Displayable.IncludeKind;
import org.nlisker.jextractGUI.model.Displayable.MainHeader;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Declaration.Scoped;
import org.openjdk.jextract.JextractTool;

@UtilityClass
public class Parser {

	/// Parses the header and populates its symbols and the symbols of all included headers.
	public void populateHeaderItem(CheckBoxTreeItem<Displayable> mainHeaderItem) throws Exception {
		var mainHeader = (MainHeader) mainHeaderItem.getValue();

		List<Declaration> declarations = parse(mainHeader);

		Map<Path, CheckBoxTreeItem<Displayable>> headers = new HashMap<>();

		Map<Path, Map<IncludeKind, CheckBoxTreeItem<Displayable>>> headerIncludes = new HashMap<>();

		declarations.stream().forEach(decl -> {
			Path path = decl.pos().path();
			CheckBoxTreeItem<Displayable> headerItem = headers.computeIfAbsent(path, _ -> {
				var newHeaderItem = new CheckBoxTreeItem<Displayable>(new Header(path));
				if (path.equals(mainHeader.path())) {
					mainHeaderItem.getChildren().addFirst(newHeaderItem);
				} else {
					mainHeaderItem.getChildren().add(newHeaderItem);
				}
				return newHeaderItem;
			});

			Map<IncludeKind, CheckBoxTreeItem<Displayable>> includes = headerIncludes.computeIfAbsent(path, _ -> new HashMap<>());

			var includeKind = IncludeKind.fromDeclaration(decl);
			CheckBoxTreeItem<Displayable> groupItem = includes.computeIfAbsent(includeKind, _ -> {
				var kindGroupItem = new CheckBoxTreeItem<Displayable>(includeKind);
				kindGroupItem.setExpanded(true);
				headerItem.getChildren().add(kindGroupItem);
				return kindGroupItem;
			});

			// unlike other scoped declarations, enum declarations' members are symbols and not the enum itself
			if (decl instanceof Declaration.Scoped scoped && scoped.kind() == Declaration.Scoped.Kind.ENUM) {
				scoped.members().forEach(enumConst -> addDeclaration(enumConst, groupItem));
			} else {
				addDeclaration(decl, groupItem);
			}
		});
	}

	private void addDeclaration(Declaration decl, CheckBoxTreeItem<Displayable> groupItem) {
		var includeDeclaration = new IncludeDeclaration(decl);
		var declItem = new CheckBoxTreeItem<Displayable>(includeDeclaration);
		groupItem.getChildren().add(declItem);
	}

	/// Parses the header and populates its symbols.
	private List<Declaration> parse(MainHeader header) throws Exception {
		String[] includes = header.includes().stream()
				.flatMap(include -> List.of(CLOption.INCLUDES_PATH.commands().getLast(), include.toString()).stream())
				.toArray(String[]::new);

		try {
			Scoped headerScope = JextractTool.parse(List.of(header.path().toString()), includes);
			return headerScope.members();
		} catch (Exception e) {
			e.printStackTrace();
			Platform.runLater(() -> new Alert(AlertType.ERROR, header.simple() + "\n" + e.getMessage(), ButtonType.OK).show());
			throw e;
		}
	}
}