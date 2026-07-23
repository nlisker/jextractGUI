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

import java.util.function.Function;
import java.util.function.Predicate;

import javax.lang.model.SourceVersion;

import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import org.nlisker.jextractGUI.model.CLOption;
import org.nlisker.jextractGUI.model.Displayable.MainHeader;

/// Viewer and controls for the package and class names generation.
sealed abstract class NameViewer extends BorderPane {

	protected NameViewer(CLOption option, String name, String tooltipText, String prompt, int colCount,
			Function<MainHeader, StringProperty> property, Predicate<String> validity) {
		var helpButton = ControlUtils.createHelpButton(option);
		var label = new Label(name);

		var textField = ControlUtils.createBoundTextField(tooltipText, prompt, colCount, property);
		textField.setTextFormatter(new TextFormatter<>(change -> validity.test(change.getControlNewText()) ? change : null));

		HBox controls = ControlUtils.createControls(helpButton, label, textField);
		setCenter(controls);
	}

	static final class ClassNameViewer extends NameViewer {

		ClassNameViewer() {
			super(CLOption.CLASS_NAME, "Class name", "Enter class name", "MyHeader", 15, MainHeader::className,
					className -> (SourceVersion.isIdentifier(className) && !SourceVersion.isKeyword(className)) || "".equals(className));
		}
	}

	static final class PackageNameViewer extends NameViewer {

		PackageNameViewer() {
			super(CLOption.PACKAGE_NAME, "Target package", "Enter package name", "com.example.native", 25, MainHeader::packageName,
					packageName -> SourceVersion.isName(packageName) || "".equals(packageName) || packageName.endsWith("."));
		}
	}
}