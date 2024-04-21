package com.nlisker.jfextract;

import java.util.function.Function;
import java.util.function.Predicate;

import javax.lang.model.SourceVersion;

import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import com.nlisker.jfextract.model.CLOption;
import com.nlisker.jfextract.model.Displayable.Header;

/**
 * Viewer and controls for the package and class names generation.
 */
sealed abstract class NameViewer extends BorderPane {

	protected NameViewer(CLOption option, String name, String tooltipText, String prompt, int colCount,
			Function<Header, StringProperty> property, Predicate<String> validity) {
		var helpButton = ControlUtils.createHelpButton(option);
		var label = new Label(name);

		var textField = ControlUtils.createBoundTextField(tooltipText, prompt, colCount, property);
		textField.setTextFormatter(new TextFormatter<>(change -> validity.test(change.getControlNewText()) ? change : null));

		HBox controls = ControlUtils.createControls(helpButton, label, textField);
		setCenter(controls);
	}

	static final class ClassNameViewer extends NameViewer {

		ClassNameViewer() {
			super(CLOption.CLASS_NAME, "Class name", "Enter class name", "MyHeader", 15, Header::className,
					className -> (SourceVersion.isIdentifier(className) && !SourceVersion.isKeyword(className)) || "".equals(className));
		}
	}

	static final class PackageNameViewer extends NameViewer {

		PackageNameViewer() {
			super(CLOption.PACKAGE_NAME, "Target package", "Enter package name", "com.example.native", 25, Header::packageName,
					packageName -> SourceVersion.isName(packageName) || "".equals(packageName) || packageName.endsWith("."));
		}
	}
}