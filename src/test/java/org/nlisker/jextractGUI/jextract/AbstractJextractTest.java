/*
 * Copyright 2026 Nir Lisker
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
package org.nlisker.jextractGUI.jextract;

import java.net.URL;
import java.nio.file.Path;

import javafx.application.Platform;
import javafx.scene.control.CheckBoxTreeItem;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.nlisker.jextractGUI.ReplaceCamelCase;
import org.nlisker.jextractGUI.model.Displayable;
import org.nlisker.jextractGUI.model.Displayable.MainHeader;

@DisplayNameGeneration(ReplaceCamelCase.class)
abstract class AbstractJextractTest {

	/// Initialize the JavaFX toolkit for tests that use JavaFX via `Platform.runLater()` (for `Alert`s).
	@BeforeAll
	void initFX() {
		try {
			Platform.startup(() -> {});
		} catch (IllegalStateException _) {
			// ignore - JavaFX toolkit already started by another test class.
		}
	}

	protected final CheckBoxTreeItem<Displayable> createMainHeaderItem(String relativePath) {
		return new CheckBoxTreeItem<>(new MainHeader(resourcePath(relativePath)));
	}

	protected final Path resourcePath(String relativePath) {
		URL url = getClass().getResource("/" + relativePath);
		try {
			return Path.of(url.toURI());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}