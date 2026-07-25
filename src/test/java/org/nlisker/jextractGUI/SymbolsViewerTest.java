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
package org.nlisker.jextractGUI;

import static com.google.common.truth.Truth.*;

import java.nio.file.Path;

import javafx.scene.control.CheckBoxTreeItem;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nlisker.jextractGUI.model.Displayable;
import org.nlisker.jextractGUI.model.Displayable.Header;
import org.nlisker.jextractGUI.model.Displayable.MainHeader;

@DisplayNameGeneration(ReplaceCamelCase.class)
class SymbolsViewerTest {

	@Nested
	class WhenHeaderIsSelfContained {

		private static final String HEADER_REL_PATH = "full.h";

		// level 1
		private CheckBoxTreeItem<Displayable> mainHeaderItem;

		// level 2 self-item
		private CheckBoxTreeItem<Displayable> headerItem;

		@BeforeAll
		void buildTree() throws Exception {
			var mainHeader = new MainHeader(Path.of(HEADER_REL_PATH));
			var header = new Header(Path.of(HEADER_REL_PATH));

			mainHeaderItem = new CheckBoxTreeItem<>(mainHeader);
			headerItem = new CheckBoxTreeItem<>(header);

			mainHeaderItem.getChildren().add(headerItem);

			SymbolsViewer.selectSelfHeader(mainHeaderItem);
		}

		@Test
		void selectMainHeaderAndSelfHeader() {
			assertThat(mainHeaderItem.isSelected()).isTrue();
			assertThat(headerItem.isSelected()).isTrue();
		}

		@Test
		void expandMainHeaderAndSelfHeader() {
			assertThat(mainHeaderItem.isExpanded()).isTrue();
			assertThat(headerItem.isExpanded()).isTrue();
		}
	}

	@Nested
	class WhenHeaderHasIncludes {

		private static final String INCLUDING = "including.h";
		private static final String INCLUDED = "included.h";

		// level 1
		private CheckBoxTreeItem<Displayable> mainHeaderItem;

		// level 2 self-item
		private CheckBoxTreeItem<Displayable> includingHeaderItem;

		// level 2 include
		private CheckBoxTreeItem<Displayable> includedHeaderItem;

		@BeforeAll
		void buildTree() throws Exception {
			var mainHeader = new MainHeader(Path.of(INCLUDING));
			var includingHeader = new Header(Path.of(INCLUDING));
			var includedHeader = new Header(Path.of(INCLUDED));

			mainHeaderItem = new CheckBoxTreeItem<>(mainHeader);
			includingHeaderItem = new CheckBoxTreeItem<>(includingHeader);
			includedHeaderItem = new CheckBoxTreeItem<>(includedHeader);

			mainHeaderItem.getChildren().add(includingHeaderItem);
			mainHeaderItem.getChildren().add(includedHeaderItem);

			SymbolsViewer.selectSelfHeader(mainHeaderItem);
		}

		@Test
		void selectOnlyTheSelfHeader() {
			assertThat(mainHeaderItem.isIndeterminate()).isTrue();
			assertThat(includingHeaderItem.isSelected()).isTrue();
			assertThat(includedHeaderItem.isSelected()).isFalse();
		}

		@Test
		void expandOnlyTheSelfHeader() {
			assertThat(mainHeaderItem.isExpanded()).isTrue();
			assertThat(includingHeaderItem.isExpanded()).isTrue();
			assertThat(includedHeaderItem.isExpanded()).isFalse();
		}
	}
}