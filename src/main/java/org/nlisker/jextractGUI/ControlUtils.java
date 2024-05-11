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

import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import lombok.experimental.UtilityClass;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignK;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.nlisker.jextractGUI.model.CLOption;
import org.nlisker.jextractGUI.model.Displayable.Header;

/**
 * Utility class for creating common controls.
 */
@UtilityClass
class ControlUtils {

	private final Color TEXT_COLOR = Color.web("#333");
	private final int TITLE_FONT_SIZE = 18;
	private final int ICON_FONT_SIZE = 24;
	private final int HELP_ICON_FONT_SIZE = 18;

	HBox createControls(Node... children) {
		var hControls = new HBox(5, children);
		hControls.setAlignment(Pos.CENTER_LEFT);
		return hControls;
	}

	Text createTitle(String text) {
		var title = new Text(text);
		title.setFill(TEXT_COLOR);
		title.setFont(Font.font(TITLE_FONT_SIZE));
		return title;
	}

	Button createSelectButton(String tooltipText) {
		return createButton(MaterialDesignF.FOLDER_OPEN, Color.GOLDENROD, tooltipText);
	}

	Button createRemoveButton() {
		return createButton(MaterialDesignM.MINUS, Color.rgb(200, 0, 0), "Remove selected");
	}

	Button createHelpButton(CLOption option) {
		var button = createButton(MaterialDesignH.HELP, HELP_ICON_FONT_SIZE, Color.DARKBLUE, "Help");
		button.setOnAction(e -> {
			var popup = new Alert(AlertType.INFORMATION);
			popup.setTitle(option.toString());
			popup.setHeaderText("Specifies " + option.commands().toString() + " " + option.argument());
			popup.setContentText(option.description());
			popup.show();
		});
		return button;
	}

	Button createButton(Ikon icon, Color color, String tooltipText) {
		return createButton(icon, ICON_FONT_SIZE, color, tooltipText);
	}

	private Button createButton(Ikon icon, int size, Color color, String tooltipText) {
		var symbol = FontIcon.of(icon, size, color);
//		symbol.setBoundsType(TextBoundsType.VISUAL);
		var button = new Button("", symbol);
		button.setPadding(new Insets(0));
		button.setTooltip(new Tooltip(tooltipText));
		return button;
	}

	/**
	 * Creates a text field with an "enter" button that have the same action.
	 */
	Node createFreeTextControl(String tooltipText, String prompt, int colCount, Consumer<TextField> onAction) {
		var textField = createTextField(tooltipText, prompt, colCount);
		var button = createButton(MaterialDesignK.KEYBOARD_RETURN, TEXT_COLOR, "Add");
		textField.setOnAction(e -> onAction.accept(textField));
		button.setOnAction(e -> onAction.accept(textField));

		HBox controls = createControls(textField, button);
		controls.setBorder(Border.stroke(Color.DARKBLUE));
		controls.setPadding(new Insets(1, 2, 1, 1));
		return controls;
	}

	TextField createBoundTextField(String tooltipText, String prompt, int colCount, Function<Header, StringProperty> headerProperty) {
		var textField = createTextField(tooltipText, prompt, colCount);
		textField.disableProperty().bind(SymbolsViewer.get().noFocus());
		SymbolsViewer.get().noFocus().subscribe(noFocus -> { if (noFocus) textField.clear(); });
		bindFocusedHeader(textField.textProperty(), headerProperty);
		return textField;
	}

	private TextField createTextField(String tooltipText, String prompt, int colCount) {
		var textField = new TextField();
		textField.setPromptText(prompt);
		textField.setTooltip(new Tooltip(tooltipText));
		textField.setPrefColumnCount(colCount);
		return textField;
	}

	<U> void bindFocusedHeader(Property<U> uiProperty, Function<Header, ? extends Property<U>> headerProperty) {
		SymbolsViewer.get().focusedHeader().subscribe((oldHeader, newHeader) -> {
			if (oldHeader != null) {
				uiProperty.unbindBidirectional(headerProperty.apply(oldHeader));
			}
			if (newHeader != null) {
				uiProperty.bindBidirectional(headerProperty.apply(newHeader));
			}
		});
	}

	Node createAndAttachDropHint(Node viewer, ObservableValue<Boolean> visibleCondition) {
		var color = Color.LIGHTGRAY;
		var border = new Border(new BorderStroke(color, BorderStrokeStyle.DASHED, new CornerRadii(10), new BorderWidths(3)));

		var icon = FontIcon.of(MaterialDesignA.ARROW_COLLAPSE_DOWN, 90, color);
		var container = new StackPane(icon);
		container.setMaxHeight(Region.USE_PREF_SIZE);
		container.setMaxWidth(Region.USE_PREF_SIZE);
		container.setBorder(border);
		container.setMouseTransparent(true);
		container.visibleProperty().bind(visibleCondition);
	
		var stackPane = new StackPane(viewer, container);
		VBox.setVgrow(stackPane, Priority.ALWAYS);
		return stackPane;
	}

	ProgressIndicator createProgressIndicator(ObservableValue<Boolean> visibleCondition) {
		var progressIndicator = new ProgressIndicator();
		progressIndicator.setMaxHeight(ICON_FONT_SIZE);
		progressIndicator.setMaxWidth(ICON_FONT_SIZE);
		progressIndicator.visibleProperty().bind(visibleCondition);
		return progressIndicator;
	}
}